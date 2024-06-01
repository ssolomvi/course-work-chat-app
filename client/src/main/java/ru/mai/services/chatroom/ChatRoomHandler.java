package ru.mai.services.chatroom;

import com.vaadin.flow.spring.annotation.SpringComponent;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import ru.mai.*;
import ru.mai.encryption_context.EncryptionContext;
import ru.mai.encryption_mode.EncryptionModeEnum;
import ru.mai.encryption_padding_mode.PaddingModeEnum;
import ru.mai.utils.Pair;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@SpringComponent
@Scope("prototype")
public class ChatRoomHandler {
    private final ChatServiceGrpc.ChatServiceBlockingStub blockingStub;
    private final InitMetadataRepository metadataRepository;

    public ChatRoomHandler(@Autowired ChatServiceGrpc.ChatServiceBlockingStub blockingStub,
                           @Autowired InitMetadataRepository metadataRepository) {
        this.blockingStub = blockingStub;
        this.metadataRepository = metadataRepository;
    }

    /**
     * Invokes room creation
     *
     * @param own       own login
     * @param companion companion login
     * @param algorithm chosen algorithm
     * @return Returns {@code true} if room was created, {@code false} if companion is offline or already sent a request
     */
    public boolean initRoom(String own, String companion, String algorithm, String encryptionMode, String paddingMode) {
        if (own.equals(companion)) {
            return false;
        }
        InitRoomRequest request = InitRoomRequest.newBuilder()
                .setOwnLogin(own)
                .setCompanionLogin(companion)
                .setAlgorithm(ContextBuilder.getAlgorithmForGrpc(algorithm))
                .setEncryptionMode(ContextBuilder.getEncryptionModeForGrpc(encryptionMode))
                .setPaddingMode(ContextBuilder.getPaddingModeForGrpc(paddingMode))
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
     *
     * @param login own login
     * @return Returns list of initiators, if any
     */
    public List<String> checkForInitRoomRequests(Login login) {
        List<String> initiators = new LinkedList<>();
        try {
            Iterator<InitRoomResponse> responses = blockingStub.checkForInitRoomRequest(login);

            if (!responses.hasNext()) {
                return Collections.emptyList();
            }

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
            if (blockingStub.passDiffieHellmanNumber(DiffieHellmanNumber.newBuilder()
                    .setOwnLogin(own)
                    .setCompanionLogin(companion)
                    .setNumber(majorNumber)
                    .build()).getEnumStatus().equals(EnumStatus.ENUM_STATUS_OK)) {
                log.debug("{} -> {}: Passed diffie-hellman number: {}", own, metadata.getKey().getCompanionLogin(), majorNumber);
            } else {
                log.debug("{} -> {}: Error passing diffie-hellman number", own, metadata.getKey().getCompanionLogin());
            }
        } catch (StatusRuntimeException e) {
            log.error("{}: passDiffieHellmanNumber: Error happened, cause: ", own, e);
        }
    }

    public static Pair<Integer, Integer> getKeyAndBlockLength(Algorithm algorithm) {
        return switch (algorithm) {
            case ALGORITHM_DEAL -> new Pair<>(AlgorithmsConfigs.DEAL_KEY_LENGTH, AlgorithmsConfigs.DEAL_BLOCK_LENGTH);
            case ALGORITHM_RIJNDAEL -> new Pair<>(AlgorithmsConfigs.RIJNDAEL_KEY_LENGTH, AlgorithmsConfigs.RIJNDAEL_BLOCK_LENGTH);
            case ALGORITHM_RC6 -> new Pair<>(AlgorithmsConfigs.RC6_KEY_LENGTH, AlgorithmsConfigs.RC6_BLOCK_LENGTH);
            case ALGORITHM_LOKI97 -> new Pair<>(AlgorithmsConfigs.LOKI97_KEY_LENGTH, AlgorithmsConfigs.LOKI97_BLOCK_LENGTH);
            // todo:
//            case ALGORITHM_MARS -> new Pair<>(MARS.KEY_LENGTH16, MARS.BLOCK_LENGTH);
            default -> new Pair<>(AlgorithmsConfigs.DES_KEY_LENGTH, AlgorithmsConfigs.DES_BLOCK_LENGTH);
        };
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

        final Map<String, EncryptionContext> newCompanions = new HashMap<>();
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
            metadataRepository.remove(companionNumber.getCompanionLogin());

            EncryptionModeEnum encryptionMode = ContextBuilder.getEncryptionModeEnum(metadata.getKey().getEncryptionMode());
            PaddingModeEnum paddingMode = ContextBuilder.getPaddingModeEnum(metadata.getKey().getPaddingMode());
            Algorithm algorithm = metadata.getKey().getAlgorithm();
            byte[] initVector = metadata.getKey().getInitVector().getBytes(StandardCharsets.UTF_8);

            Pair<Integer, Integer> keyAndBlockLength = getKeyAndBlockLength(algorithm);
            if (initVector.length != keyAndBlockLength.getValue()) {
                byte[] tmp = new byte[keyAndBlockLength.getValue()];
                System.arraycopy(initVector, 0, tmp, 0, keyAndBlockLength.getValue());
                initVector = tmp;
            }

            byte[] key = DiffieHellmanNumbersHandler.getKey(new BigInteger(companionNumber.getNumber()), metadata.getValue(), new BigInteger(metadata.getKey().getDiffieHellmanP()));

            var context = ContextBuilder.createEncryptionContext(encryptionMode, paddingMode, algorithm, initVector, key);

            newCompanions.put(companionNumber.getCompanionLogin(), context);

            log.debug("{} -> {}: created context", login.getLogin(), companionNumber.getCompanionLogin());
        }

        return newCompanions;
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

    public void close() {
        metadataRepository.clear();
    }
}
