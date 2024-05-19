package ru.mai.client.room;

import io.grpc.stub.StreamObserver;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatRoomHandler {
    private static final int DIFFIE_HELLMAN_NUMBER_SIZE = 50;
    private final String userLogin;
    private final Login login;
    private final ChatServiceGrpc.ChatServiceStub asyncStub;
    private final BigInteger diffieHellmanG;

    private final Map<String, Pair<InitRoomResponse, BigInteger>> companionsBeforeChatRoomCreated = new ConcurrentHashMap<>();
    private final EncryptionContextBuilderOfInitRoomResponse contextBuilder = new EncryptionContextBuilderOfInitRoomResponse();

    public ChatRoomHandler(String userLogin, ChatServiceGrpc.ChatServiceStub asyncStub, BigInteger diffieHellmanG) {
        this.userLogin = userLogin;
        this.asyncStub = asyncStub;
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

        InitRoomObserver initRoomObserver = new InitRoomObserver();

        asyncStub.initRoom(request, initRoomObserver);

        var responses = initRoomObserver.getResponses();

        if (responses.isEmpty()) {
            return false;
        }

        InitRoomResponse response = initRoomObserver.getResponses().get(0);

        companionsBeforeChatRoomCreated.put(companion,
                new Pair<>(response, generateDiffieHellmanMinorNumber()));

        return true;
    }

    /**
     * Checks if any requests for initiating chat room exist.
     * <p>
     * If any, adds correspond data for passing diffie-hellman numbers
     * @return {@code true}, if any requests for initiating chat room exist
     */
    public boolean checkForInitRoomRequests() {
        InitRoomObserver initRoomObserver = new InitRoomObserver();

        asyncStub.checkForInitRoomRequest(login, initRoomObserver);

        Iterable<InitRoomResponse> responses = initRoomObserver.getResponses();

        boolean responsesIsNotEmpty = false;
        for (InitRoomResponse response : responses) {
            responsesIsNotEmpty = true;
            companionsBeforeChatRoomCreated.put(response.getCompanionLogin(),
                    new Pair<>(response, generateDiffieHellmanMinorNumber()));
        }

        return responsesIsNotEmpty;
    }

    /**
     * If there are initiated requests for chat room creation, passes Diffie-Hellman number
     */
    public void passDiffieHellmanNumber() {
        if (companionsBeforeChatRoomCreated.isEmpty()) {
            return;
        }

        EmptyResponseObserver responseObserver = new EmptyResponseObserver();

        StreamObserver<DiffieHellmanNumber> requestObserver = asyncStub.passDiffieHellmanNumber(responseObserver);

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
        DiffieHellmanNumberObserver diffieHellmanNumberObserver = new DiffieHellmanNumberObserver();

        asyncStub.anyDiffieHellmanNumber(login, diffieHellmanNumberObserver);

        Map<String, String> numbers = diffieHellmanNumberObserver.getNumbers();

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

        CompanionStatusObserver observer = new CompanionStatusObserver();

        asyncStub.deleteRoom(chatRoomLogins, observer);

        return observer.getCompanionsAndStatus().get(companion);
    }

    public Map<String, Boolean> checkForDeleteRoomRequests() {
        CompanionStatusObserver companionStatusObserver = new CompanionStatusObserver();

        asyncStub.checkForDeletedRoom(login, companionStatusObserver);

        return companionStatusObserver.getCompanionsAndStatus();
    }
}
