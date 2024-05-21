package ru.mai.client.room;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import ru.mai.*;
import ru.mai.encryption_context.EncryptionContext;
import ru.mai.observers.CompanionStatusObserver;
import ru.mai.observers.DiffieHellmanNumberObserver;
import ru.mai.observers.EmptyResponseObserver;
import ru.mai.observers.InitRoomObserver;
import ru.mai.utils.MathOperationsBigInteger;
import ru.mai.utils.Operations;
import ru.mai.utils.Pair;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class ChatRoomHandler {
    private static final int DIFFIE_HELLMAN_NUMBER_SIZE = 50;
    private final String userLogin;
    private final Login login;
    private final ChatServiceGrpc.ChatServiceStub asyncStub;
    private final ChatServiceGrpc.ChatServiceBlockingStub blockingStub;
    private final BigInteger diffieHellmanG;

    private final Map<String, Pair<InitRoomResponse, BigInteger>> companionsBeforeChatRoomCreated = new ConcurrentHashMap<>();
    private final EncryptionContextBuilderOfInitRoomResponse contextBuilder = new EncryptionContextBuilderOfInitRoomResponse();

    public ChatRoomHandler(String userLogin, ChatServiceGrpc.ChatServiceBlockingStub blockingStub, ChatServiceGrpc.ChatServiceStub asyncStub, BigInteger diffieHellmanG) {
        this.userLogin = userLogin;
        this.asyncStub = asyncStub;
        this.blockingStub = blockingStub;
        this.login = Login.newBuilder().setLogin(userLogin).build();
        this.diffieHellmanG = diffieHellmanG;
    }


    /**
     * Initiates creating a single room
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

        companionsBeforeChatRoomCreated.put(companion,
                new Pair<>(response, generateDiffieHellmanMinorNumber()));

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
        final CountDownLatch finishLatch = new CountDownLatch(1);
        final List<InitRoomResponse> responses = new LinkedList<>();

        asyncStub.checkForInitRoomRequest(login, new StreamObserver<>() {
            private final InitRoomResponse dummy = InitRoomResponse.getDefaultInstance();

            @Override
            public void onNext(InitRoomResponse value) {
                if (!value.equals(dummy)) {
                    responses.add(value);
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("Error happened, cause: ", t);
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                finishLatch.countDown();
            }
        });

        if (responses.isEmpty()) {
            return false;
        }

        for (InitRoomResponse response : responses) {
            companionsBeforeChatRoomCreated.put(response.getCompanionLogin(),
                    new Pair<>(response, generateDiffieHellmanMinorNumber()));
        }

        return true;
    }

    /**
     * If there are initiated requests for chat room creation, passes Diffie-Hellman number
     */
    public void passDiffieHellmanNumber() {
        if (companionsBeforeChatRoomCreated.isEmpty()) {
            return;
        }
        final CountDownLatch finalLatch = new CountDownLatch(1);

        StreamObserver<DiffieHellmanNumber> requestObserver = asyncStub.passDiffieHellmanNumber(new StreamObserver<Empty>() {
            @Override
            public void onNext(Empty value) {
                //
            }

            @Override
            public void onError(Throwable t) {
                log.error("Error happened, cause: ", t);
                finalLatch.countDown();
            }

            @Override
            public void onCompleted() {
                finalLatch.countDown();
            }
        });

        for (Map.Entry<String, Pair<InitRoomResponse, BigInteger>> companionInfo : companionsBeforeChatRoomCreated.entrySet()) {
            BigInteger numberToPass = getDiffieHellmanNumber(companionInfo.getValue().getValue(),
                    new BigInteger(companionInfo.getValue().getKey().getDiffieHellmanP()));

            DiffieHellmanNumber request = DiffieHellmanNumber.newBuilder()
                    .setOwnLogin(userLogin)
                    .setCompanionLogin(companionInfo.getKey())
                    .setNumber(numberToPass.toString())
                    .build();

            requestObserver.onNext(request);
        }

    }

    public Map<String, EncryptionContext> anyDiffieHellmanNumbers() {
        final CountDownLatch finalLatch = new CountDownLatch(1);
        final Map<String, String> numbers = new HashMap<>();

        asyncStub.anyDiffieHellmanNumber(login, new StreamObserver<>() {
            private final DiffieHellmanNumber dummy = DiffieHellmanNumber.getDefaultInstance();

            @Override
            public void onNext(DiffieHellmanNumber value) {
                if (!value.equals(dummy)) {
                    numbers.put(value.getCompanionLogin(), value.getNumber());
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("Error occurred, cause: ", t);
                finalLatch.countDown();
            }

            @Override
            public void onCompleted() {
                finalLatch.countDown();
            }
        });

        if (numbers.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, EncryptionContext> companionContexts = new HashMap<>();

        for (Map.Entry<String, String> number : numbers.entrySet()) {
            // todo: insert into db new value (companion_login, encryption_mode, padding_mode, algorithm, init_vector, key)
            Pair<InitRoomResponse, BigInteger> companionInfo = companionsBeforeChatRoomCreated.get(number.getKey());
            byte[] key = getKey(new BigInteger(number.getValue()), companionInfo.getValue(), new BigInteger(companionInfo.getKey().getDiffieHellmanP()));

            companionContexts.put(number.getKey(), contextBuilder.buildEncryptionContext(companionInfo.getKey(), key));

            companionsBeforeChatRoomCreated.remove(number.getKey());
        }

        return companionContexts;
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
        final CountDownLatch finalLatch = new CountDownLatch(1);
        final Map<String, Boolean> deleted = new HashMap<>();

        asyncStub.checkForDeletedRoom(login, new StreamObserver<>() {
            private final CompanionStatus dummy = CompanionStatus.getDefaultInstance();

            @Override
            public void onNext(CompanionStatus value) {
                if (value.equals(dummy)) {
                    return;
                }

                deleted.put(value.getCompanionLogin(), value.getStatus());
                log.debug("Companion {} deleted chat", value.getCompanionLogin());
            }

            @Override
            public void onError(Throwable t) {
                log.error("Error occurred, cause: ", t);
                finalLatch.countDown();
            }

            @Override
            public void onCompleted() {
                finalLatch.countDown();
            }
        });

        if (deleted.isEmpty()) {
            return Collections.emptyMap();
        }
        return deleted;
    }
}
