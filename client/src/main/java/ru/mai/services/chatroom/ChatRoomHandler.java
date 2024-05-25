package ru.mai.services.chatroom;

import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mai.*;
import ru.mai.encryption_context.EncryptionContext;
import ru.mai.encryption_mode.EncryptionModeEnum;
import ru.mai.encryption_padding_mode.PaddingModeEnum;
import ru.mai.services.ContextsRepository;
import ru.mai.utils.Pair;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
public class ChatRoomHandler {
    private final ChatServiceGrpc.ChatServiceBlockingStub blockingStub;
    private final InitMetadataRepository metadataRepository;
    private final ContextsRepository contextsRepository;

    public ChatRoomHandler(@Autowired ChatServiceGrpc.ChatServiceBlockingStub blockingStub,
                           @Autowired InitMetadataRepository metadataRepository,
                           @Autowired ContextsRepository contextsRepository) {
        this.blockingStub = blockingStub;
        this.metadataRepository = metadataRepository;
        this.contextsRepository = contextsRepository;
    }

    private static EncryptionMode getEncryptionModeForGrpc(String mode) {
        return switch (mode) {
            case "CBC" -> EncryptionMode.ENCRYPTION_MODE_CBC;
            case "PCBC" -> EncryptionMode.ENCRYPTION_MODE_PCBC;
            case "CFB" -> EncryptionMode.ENCRYPTION_MODE_CFB;
            case "OFB" -> EncryptionMode.ENCRYPTION_MODE_OFB;
            case "CTR" -> EncryptionMode.ENCRYPTION_MODE_CTR;
            case "RANDOM_DELTA" -> EncryptionMode.ENCRYPTION_MODE_RANDOM_DELTA;
            default -> EncryptionMode.ENCRYPTION_MODE_ECB;
        };
    }

    private static PaddingMode getPaddingModeForGrpc(String mode) {
        return switch (mode) {
            case "ANSI_X_923" -> PaddingMode.PADDING_MODE_ANSI_X_923;
            case "PKCS7" -> PaddingMode.PADDING_MODE_PKCS7;
            case "ISO10126" -> PaddingMode.PADDING_MODE_ISO10126;
            default -> PaddingMode.PADDING_MODE_ZEROES;
        };
    }

    private static Algorithm getAlgorithmForGrpc(String algorithm) {
        return switch (algorithm) {
            case "DEAL" -> Algorithm.ALGORITHM_DEAL;
            case "Rijndael" -> Algorithm.ALGORITHM_RIJNDAEL;
            case "LOKI97" -> Algorithm.ALGORITHM_LOKI97;
            case "RC6" -> Algorithm.ALGORITHM_RC6;
            case "MARS" -> Algorithm.ALGORITHM_MARS;
            default -> Algorithm.ALGORITHM_DES;
        };
    }

    /**
     * Invokes room creation
     * @param own own login
     * @param companion companion login
     * @param algorithm chosen algorithm
     * @return Returns {@code true} if room was created, {@code false} if companion is offline or already sent a request
     */
    public boolean initRoom(String own, String companion, String algorithm, String encryptionMode, String paddingMode) {
        InitRoomRequest request = InitRoomRequest.newBuilder()
                .setOwnLogin(own)
                .setCompanionLogin(companion)
                .setAlgorithm(getAlgorithmForGrpc(algorithm))
                .setEncryptionMode(getEncryptionModeForGrpc(encryptionMode))
                .setPaddingMode(getPaddingModeForGrpc(paddingMode))
                .build();

        var response = blockingStub.initRoom(request);

        if (response.equals(InitRoomResponse.getDefaultInstance())) {
            return false;
        }

        // put metadata for initiator
        metadataRepository.put(companion, response);
        return true;
    }

    /**
     * Checks for init room requests
     * @param login own login
     * @return Returns list of initiators, if any
     */
    public List<String> checkForInitRoomRequests(Login login) {
        List<String> initiators;
        try {
            Iterator<InitRoomResponse> responses = blockingStub.checkForInitRoomRequest(login);

            if (!responses.hasNext()) {
                return Collections.emptyList();
            }

            initiators = new LinkedList<>();

            final InitRoomResponse dummy = InitRoomResponse.getDefaultInstance();
            while (responses.hasNext()) {
                InitRoomResponse response = responses.next();
                if (response.equals(dummy)) {
                    // happens in case there are no requests
                    continue;
                }
                // put metadata for to chat with
                metadataRepository.put(response.getCompanionLogin(), response);
                initiators.add(response.getCompanionLogin());
            }

        } catch (StatusRuntimeException e) {
            log.error("{} checkForInitRoomRequests: Error happened, cause: ", login, e);
            return Collections.emptyList();
        }
        return initiators;
    }

    public void passDiffieHellmanNumber(String own, String companion, BigInteger g) {
        var op = metadataRepository.get(companion);

        if (op.isEmpty()) {
            log.debug("No metadata for companion: {}", companion);
            return;
        }

        var metadata = op.get();

        String majorNumber = DiffieHellmanNumbersHandler.generateMajorNumber(g, new BigInteger(metadata.getKey().getDiffieHellmanP()), metadata.getValue()).toString();

        try {
            blockingStub.passDiffieHellmanNumber(DiffieHellmanNumber.newBuilder()
                    .setOwnLogin(own)
                    .setCompanionLogin(companion)
                    .setNumber(majorNumber)
                    .build());

            log.debug("{} -> {}: Passed diffie-hellman number: {}", own, metadata.getKey(), majorNumber);
        } catch (StatusRuntimeException e) {
            log.error("{}: passDiffieHellmanNumber: Error happened, cause: ", own, e);
        }
    }

    public Map<String, EncryptionContext> anyDiffieHellmanNumbers(Login login) {
        Iterator<DiffieHellmanNumber> numbers;
        try {
            numbers = blockingStub.anyDiffieHellmanNumber(login);
        } catch (StatusRuntimeException e) {
            log.debug("{}: anyDiffieHellmanNumbers: Error occurred, cause:, ", login.getLogin(), e);
            return Collections.emptyMap();
        }

        if (!numbers.hasNext()) {
            return Collections.emptyMap();
        }

        final Map<String, EncryptionContext> companionContexts = new HashMap<>();
        final DiffieHellmanNumber dummy = DiffieHellmanNumber.getDefaultInstance();

        while (numbers.hasNext()) {
            DiffieHellmanNumber companionNumber = numbers.next();
            if (companionNumber.equals(dummy)) {
                // happens in case there are no dh numbers
                continue;
            }
            log.debug("{} <- {}: got number {}", login.getLogin(), companionNumber.getCompanionLogin(), companionNumber.getNumber());

            var op = metadataRepository.get(companionNumber.getCompanionLogin());
            if (op.isEmpty()) {
                log.debug("No metadata for companion: {}", companionNumber.getCompanionLogin());
                continue;
            }

            Pair<InitRoomResponse, BigInteger> metadata = op.get(); // metadata for encryption context creation and minor number

            EncryptionModeEnum encryptionMode = ContextBuilder.getEncryptionModeEnum(metadata.getKey().getEncryptionMode());
            PaddingModeEnum paddingMode = ContextBuilder.getPaddingModeEnum(metadata.getKey().getPaddingMode());
            Algorithm algorithm = metadata.getKey().getAlgorithm();
            byte[] initVector = metadata.getKey().getInitVector().getBytes(StandardCharsets.UTF_8);
            byte[] key = DiffieHellmanNumbersHandler.getKey(new BigInteger(companionNumber.getNumber()), metadata.getValue(), new BigInteger(metadata.getKey().getDiffieHellmanP()));
            // todo: insert into db new value (companion_login, encryption_mode, padding_mode, algorithm, init_vector, key)

            var context = ContextBuilder.createEncryptionContext(encryptionMode, paddingMode, algorithm, initVector, key);

            contextsRepository.put(companionNumber.getCompanionLogin(), context);

            log.debug("{} -> {}: created context", login.getLogin(), companionNumber.getCompanionLogin());
        }

        return companionContexts;
    }

    public boolean deleteRoom(String own, String companion) {
        ChatRoomLogins chatRoomLogins = ChatRoomLogins.newBuilder()
                .setOwnLogin(own)
                .setCompanionLogin(companion)
                .build();

        var response = blockingStub.deleteRoom(chatRoomLogins);

        return response.getStatus();
    }


    public List<String> checkForDeleteRoomRequests(Login login) {
        try {

            var requests = blockingStub.checkForDeletedRoom(login);

            if (!requests.hasNext()) {
                return Collections.emptyList();
            }

            final List<String> deleted = new LinkedList<>();
            final CompanionAndStatus dummy = CompanionAndStatus.getDefaultInstance();

            while (requests.hasNext()) {
                CompanionAndStatus request = requests.next();
                if (request.equals(dummy)) {
                    // happens in case there are no delete requests
                    continue;
                }
                deleted.add(request.getCompanionLogin());
            }

            return deleted;
        } catch (StatusRuntimeException e) {
            log.error("{}: checkForDeleteRoomRequests: Error occurred, cause: ", login.getLogin(), e);
        }
        return Collections.emptyList();
    }

}
