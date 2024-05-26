package ru.mai.services.chatroom;

import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mai.*;
import ru.mai.db.model.ChatMetadataEntity;
import ru.mai.db.repositories.ChatMetadataEntityRepository;
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
    private final ChatMetadataEntityRepository chatMetadataRepository;

    public ChatRoomHandler(@Autowired ChatServiceGrpc.ChatServiceBlockingStub blockingStub,
                           @Autowired InitMetadataRepository metadataRepository,
                           @Autowired ContextsRepository contextsRepository,
                           @Autowired ChatMetadataEntityRepository chatMetadataRepository) {
        this.blockingStub = blockingStub;
        this.metadataRepository = metadataRepository;
        this.contextsRepository = contextsRepository;
        this.chatMetadataRepository = chatMetadataRepository;
    }

    public void createContexts() {
        for (var room : chatMetadataRepository.findAll()) {
            contextsRepository.put(room.getCompanion(), room.getEncryptionMode(), room.getPaddingMode(), room.getAlgorithm(), room.getInitVector(), room.getKey());
        }
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
            if (blockingStub.passDiffieHellmanNumber(DiffieHellmanNumber.newBuilder()
                    .setOwnLogin(own)
                    .setCompanionLogin(companion)
                    .setNumber(majorNumber)
                    .build()).getEnumStatus().equals(EnumStatus.ENUM_STATUS_OK)) {
                log.debug("{} -> {}: Passed diffie-hellman number: {}", own, metadata.getKey(), majorNumber);
            } else {
                log.debug("{} -> {}: Error passing diffie-hellman number", own, metadata.getKey());
            }
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

            // save to db
            chatMetadataRepository.save(new ChatMetadataEntity(companionNumber.getCompanionLogin(),
                    metadata.getKey().getEncryptionMode().toString(),
                    metadata.getKey().getPaddingMode().toString(),
                    metadata.getKey().getAlgorithm().toString(),
                    initVector, key));


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
