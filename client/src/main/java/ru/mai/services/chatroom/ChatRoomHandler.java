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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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

    public boolean initRoom(String own, String companion, String algorithm) {
        InitRoomRequest request = InitRoomRequest.newBuilder()
                .setOwnLogin(own)
                .setCompanionLogin(companion)
                .setAlgorithm(algorithm)
                .build();

        var response = blockingStub.initRoom(request);

        if (response.equals(InitRoomResponse.getDefaultInstance())) {
            return false;
        }

        // put metadata for initiator
        metadataRepository.put(companion, response);
        return true;
    }

    public boolean checkForInitRoomRequests(Login login) {
        boolean atLeastOneIsNotDummy = false;
        try {
            Iterator<InitRoomResponse> responses = blockingStub.checkForInitRoomRequest(login);

            if (!responses.hasNext()) {
                return false;
            }

            final InitRoomResponse dummy = InitRoomResponse.getDefaultInstance();
            while (responses.hasNext()) {
                InitRoomResponse response = responses.next();
                if (response.equals(dummy)) {
                    continue;
                }
                // put metadata for to chat with
                metadataRepository.put(response.getCompanionLogin(), response);
                atLeastOneIsNotDummy = true;
            }

        } catch (StatusRuntimeException e) {
            log.error("{} checkForInitRoomRequests: Error happened, cause: ", login, e);
        }
        return atLeastOneIsNotDummy;
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
                continue;
            }
            log.debug("{} <- {}: got number {}", login.getLogin(), companionNumber.getCompanionLogin(), companionNumber.getNumber());

            var op = metadataRepository.get(companionNumber.getCompanionLogin());
            if (op.isEmpty()) {
                log.debug("No metadata for companion: {}", companionNumber.getCompanionLogin());
                continue;
            }

            Pair<InitRoomResponse, BigInteger> metadata = op.get();

            EncryptionModeEnum encryptionMode = ContextBuilder.getEncryptionModeEnum(metadata.getKey().getEncryptionMode());
            PaddingModeEnum paddingMode = ContextBuilder.getPaddingModeEnum(metadata.getKey().getPaddingMode());
            String algorithm = metadata.getKey().getAlgorithm();
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
        contextsRepository.remove(companion);
        // todo: remove from db companion_login, encryption_mode, padding_mode, algorithm, init; remove from db all messages with companion_login

        return response.getStatus();
    }

    public Map<String, Boolean> checkForDeleteRoomRequests(Login login) {
        try {

            var requests = blockingStub.checkForDeletedRoom(login);

            if (!requests.hasNext()) {
                return Collections.emptyMap();
            }

            final Map<String, Boolean> deleted = new HashMap<>();
            final CompanionStatus dummy = CompanionStatus.getDefaultInstance();

            while (requests.hasNext()) {
                CompanionStatus request = requests.next();
                if (request.equals(dummy)) {
                    continue;
                }
                deleted.put(request.getCompanionLogin(), request.getStatus());
            }

            return deleted;
        } catch (StatusRuntimeException e) {
            log.error("{}: checkForDeleteRoomRequests: Error occurred, cause: ", login.getLogin(), e);
        }
        return Collections.emptyMap();
    }

}
