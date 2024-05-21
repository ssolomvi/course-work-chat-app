package ru.mai.client.room;

import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import ru.mai.*;
import ru.mai.encryption_context.EncryptionContext;
import ru.mai.utils.MathOperationsBigInteger;
import ru.mai.utils.Operations;
import ru.mai.utils.Pair;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ChatRoomHandler {
    private static final int DIFFIE_HELLMAN_NUMBER_SIZE = 50;
    private final String userLogin;
    private final Login login;
    private final ChatServiceGrpc.ChatServiceStub asyncStub;
    private final ChatServiceGrpc.ChatServiceBlockingStub blockingStub;
    private final BigInteger diffieHellmanG;

    /**
     * Collection, updated in methods checkForInitRoomRequests,
     * quarried in methods anyDiffieHellmanNumbers
     * <p>
     * Key is companion login, value is a pair of config for chatting with companion and minor number for dh
     */
    private final Map<String, Pair<InitRoomResponse, BigInteger>> metadataAfterInit = new ConcurrentHashMap<>();
    private final EncryptionContextBuilderOfInitRoomResponse contextBuilder = new EncryptionContextBuilderOfInitRoomResponse();

    public ChatRoomHandler(String userLogin, ChatServiceGrpc.ChatServiceBlockingStub blockingStub, ChatServiceGrpc.ChatServiceStub asyncStub, BigInteger diffieHellmanG) {
        this.userLogin = userLogin;
        this.asyncStub = asyncStub;
        this.blockingStub = blockingStub;
        this.login = Login.newBuilder().setLogin(userLogin).build();
        this.diffieHellmanG = diffieHellmanG;
    }


    /**
     * Initiates creating a single chat room. Passes metadata for companion
     *
     * @param companion companion's login
     * @param algorithm chosen algorithm
     */
    public boolean initRoom(String companion, String algorithm) {
        InitRoomRequest request = InitRoomRequest.newBuilder()
                .setOwnLogin(userLogin)
                .setCompanionLogin(companion)
                .setAlgorithm(algorithm)
                .build();

        var response = blockingStub.initRoom(request);

        if (response.equals(InitRoomResponse.getDefaultInstance())) {
            return false;
        }

        // put metadata for initiator
        metadataAfterInit.put(companion, new Pair<>(response, generateDiffieHellmanMinorNumber()));

        return true;
    }

    /**
     * Checks if any requests for initiating chat room exist.
     * <p>
     * If any, adds correspond data for passing diffie-hellman numbers
     *
     * @return {@code true}, if any requests for initiating chat room exist
     */
    public boolean checkForInitRoomRequests() {
        boolean atLeastOneIsNotDummy = false;
        try {
            Iterator<InitRoomResponse> responses = blockingStub.checkForInitRoomRequest(login);
            final InitRoomResponse dummy = InitRoomResponse.getDefaultInstance();

            if (!responses.hasNext()) {
                return false;
            }


            while (responses.hasNext()) {
                InitRoomResponse response = responses.next();
                if (response.equals(dummy)) {
                    continue;
                }
                // put metadata for to chat with
                metadataAfterInit.put(response.getCompanionLogin(),
                        new Pair<>(response, generateDiffieHellmanMinorNumber()));
                atLeastOneIsNotDummy = true;
            }

        } catch (StatusRuntimeException e) {
            log.error("{} checkForInitRoomRequests: Error happened, cause: ", login, e);
        }
        return atLeastOneIsNotDummy;
    }

    /**
     * If there are initiated requests for chat room creation, passes Diffie-Hellman number
     */
    public void passDiffieHellmanNumber() {
        if (metadataAfterInit.isEmpty()) {
            return;
        }

        try {
            for (var metadata: metadataAfterInit.entrySet()) {
                // A = g^a mod p
                BigInteger numberToPass = getDiffieHellmanNumber(metadata.getValue().getValue(),
                        new BigInteger(metadata.getValue().getKey().getDiffieHellmanP()));

                blockingStub.passDiffieHellmanNumber(
                        DiffieHellmanNumber.newBuilder()
                                .setOwnLogin(userLogin)
                                .setCompanionLogin(metadata.getKey())
                                .setNumber(numberToPass.toString())
                                .build());

                log.debug("{} -> {}: Passed diffie-hellman number: {}", userLogin, metadata.getKey(), numberToPass);
            }
        } catch (StatusRuntimeException e) {
            log.error("{}: passDiffieHellmanNumber: Error happened, cause: ", userLogin, e);
        }
    }

    public Map<String, EncryptionContext> anyDiffieHellmanNumbers() {
        try {
            var numbers = blockingStub.anyDiffieHellmanNumber(login);

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
                log.debug("{} <- {}: got number {}", userLogin, companionNumber.getCompanionLogin(), companionNumber.getNumber());
                // todo: insert into db new value (companion_login, encryption_mode, padding_mode, algorithm, init_vector, key)
                Pair<InitRoomResponse, BigInteger> metadata = metadataAfterInit.get(companionNumber.getCompanionLogin());
                // key = B ^ a mod P
                byte[] key = getKey(
                        new BigInteger(companionNumber.getNumber()), // companion number, B
                        metadata.getValue(), // minor number, a
                        new BigInteger(metadata.getKey().getDiffieHellmanP())); // d-h P number

                companionContexts.put(companionNumber.getCompanionLogin(),
                        contextBuilder.buildEncryptionContext(metadata.getKey(), key));

                metadataAfterInit.remove(companionNumber.getCompanionLogin());
                log.debug("{} -> {}: created context", userLogin, companionNumber.getCompanionLogin());
            }

            return companionContexts;
        } catch (StatusRuntimeException e) {
            log.debug("{}: anyDiffieHellmanNumbers: Error occurred, cause:, ", userLogin, e);
        }
        return Collections.emptyMap();
    }

    private BigInteger generateDiffieHellmanMinorNumber() {
        return new BigInteger(Operations.generateBytes(DIFFIE_HELLMAN_NUMBER_SIZE));
    }

    private BigInteger getDiffieHellmanNumber(BigInteger minorNumber, BigInteger diffieHellmanP) {
        return MathOperationsBigInteger.fastPowMod(diffieHellmanG, minorNumber, diffieHellmanP);
    }

    private byte[] getKey(BigInteger companionNumber, BigInteger minorNumber, BigInteger diffieHellmanP) {
        return MathOperationsBigInteger.fastPowMod(companionNumber, minorNumber, diffieHellmanP).toByteArray();
    }

    /**
     * Deletes chat room with {@code companion}
     *
     * @param companion to delete a chat with
     * @return {@code true}, if deletion succeeded, {@code false}, if {@code companion} is offline
     */
    public boolean deleteRoom(String companion) {
        ChatRoomLogins chatRoomLogins = ChatRoomLogins.newBuilder()
                .setOwnLogin(userLogin)
                .setCompanionLogin(companion)
                .build();

        var response = blockingStub.deleteRoom(chatRoomLogins);

        return response.getStatus();
    }

    public Map<String, Boolean> checkForDeleteRoomRequests() {
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
            log.error("{}: checkForDeleteRoomRequests: Error occurred, cause: ", userLogin, e);
        }
        return Collections.emptyMap();
    }
}
