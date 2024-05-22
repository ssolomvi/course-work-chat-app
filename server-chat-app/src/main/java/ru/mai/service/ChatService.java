package ru.mai.service;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import ru.mai.*;
import ru.mai.model.ChatRoom;
import ru.mai.model.KafkaMessage;
import ru.mai.serialization.KafkaMessageDeserializer;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ChatService extends ChatServiceGrpc.ChatServiceImplBase {
    private static final String DIFFIE_HELLMAN_G = "88005553535";

    private final ConnectResponse connectResponse;

    private final Set<String> activeUsers = ConcurrentHashMap.newKeySet();
    private final Set<ChatRoom> rooms = ConcurrentHashMap.newKeySet(); // login1, login2, topic1, topic2
    private final Map<String, Map<String, Optional<KafkaConsumer<String, KafkaMessage>>>> consumers = new ConcurrentHashMap<>();

    private final Map<String, HashSet<String>> notifyForConnection = new ConcurrentHashMap<>(); // key is who waited, value is who was waited
    private final Map<String, HashSet<String>> notifyForDisconnection = new ConcurrentHashMap<>(); // key is to notify, value is disconnected

    private final Map<String, Map<String, InitRoomResponse>> initRoomRequests = new ConcurrentHashMap<>(); // key is who should check, value is who initiated
    private final Map<String, Map<String, String>> diffieHellmanNumbers = new ConcurrentHashMap<>(); // key is destination, value is sender
    private final Map<String, Set<String>> deleteRoomRequests = new ConcurrentHashMap<>(); // key is to notify, value is who deleted

    private String getTopic(String client1, String client2) {
        return String.format("%s_%s", client1, client2);
    }

    public ChatService() {
        this.connectResponse = ConnectResponse.newBuilder().setDiffieHellmanG(DIFFIE_HELLMAN_G).build();
    }

    @Override
    public void connect(Login request, StreamObserver<ConnectResponse> responseObserver) {
        log.debug("{}: connected", request.getLogin());
        activeUsers.add(request.getLogin());
        responseObserver.onNext(connectResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void disconnect(Login request, StreamObserver<Empty> responseObserver) {
        log.debug("{}: disconnected", request.getLogin());
        activeUsers.remove(request.getLogin());
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    private KafkaConsumer<String, KafkaMessage> createKafkaConsumer(String topic) {
        // todo: configs
        KafkaConsumer<String, KafkaMessage> consumer = new KafkaConsumer<>(
                Map.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
                        ConsumerConfig.GROUP_ID_CONFIG, "test_group_consumer",
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"),
                new StringDeserializer(),
                new KafkaMessageDeserializer());
        consumer.subscribe(Collections.singletonList(topic));
        return consumer;
    }

    private void addConsumer(String login1, String login2) {
        if (consumers.containsKey(login1)) {
            var consumerMap = consumers.get(login1);
            consumerMap.put(login2, Optional.of(createKafkaConsumer(getTopic(login1, login2))));
            consumers.put(login1, consumerMap);
        } else {
            Map<String, Optional<KafkaConsumer<String, KafkaMessage>>> map = new HashMap<>();
            map.put(login2, Optional.of(createKafkaConsumer(getTopic(login1, login2))));
            consumers.put(login1, map);
        }
    }

    private Optional<KafkaConsumer<String, KafkaMessage>> peekKafkaConsumer(String login1, String login2) {
        if (!consumers.containsKey(login1)) {
            return Optional.empty();
        }

        var consumerMap = consumers.get(login1);
        return consumerMap.get(login2);
    }

    private void removeConsumer(String login1, String login2) {
        if (!consumers.containsKey(login1)) {
            return;
        }

        var consumerMap = consumers.get(login1);

        if (!consumerMap.isEmpty()) {
            consumers.remove(login2);
        }

        if (consumerMap.isEmpty()) {
            consumers.remove(login1); // optional? do agree on
        }
        // works? or put() is needed?
    }

    @Override
    public StreamObserver<ChatRoomLogins> connectWithRooms(StreamObserver<CompanionStatus> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(ChatRoomLogins room) {
                boolean companionIsActive = false;

                String ownLogin = room.getOwnLogin();
                String companionLogin = room.getCompanionLogin();

                if (activeUsers.contains(room.getCompanionLogin())) {
                    companionIsActive = true;

                    // add chat room
                    String topicInOwnLoginCompanionLogin = getTopic(ownLogin, companionLogin);
                    String topicInCompanionLoginOwnLogin = getTopic(companionLogin, ownLogin);
                    rooms.add(new ChatRoom(ownLogin, companionLogin, topicInOwnLoginCompanionLogin, topicInCompanionLoginOwnLogin));

                    // init consumers for client1 room and client2 room
                    addConsumer(ownLogin, companionLogin);
                    addConsumer(companionLogin, ownLogin);

                    // add to collection to notify this client's companion
                    HashSet<String> connected;
                    if (notifyForConnection.containsKey(companionLogin)) {
                        connected = notifyForConnection.get(companionLogin);
                    } else {
                        connected = new HashSet<>();
                    }
                    connected.add(ownLogin);
                    notifyForConnection.put(companionLogin, connected);
                }

                log.debug("{} -> {}: {}", ownLogin, companionLogin, companionIsActive ? "connected with" : "waiting for");

                responseObserver.onNext(CompanionStatus.newBuilder()
                        .setCompanionLogin(companionLogin)
                        .setStatus(companionIsActive)
                        .build());
            }

            @Override
            public void onError(Throwable t) {
                log.error("connectWithRooms: Error happened while connecting: ", t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };

    }

    @Override
    public StreamObserver<ChatRoomLogins> disconnectWithRooms(StreamObserver<Empty> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(ChatRoomLogins room) {
                String ownLogin = room.getOwnLogin();
                String companionLogin = room.getCompanionLogin();

                ChatRoom dummy = new ChatRoom(ownLogin, companionLogin, "", "");

                // delete chat room
                rooms.remove(dummy);
                // delete both consumers
                removeConsumer(ownLogin, companionLogin);
                removeConsumer(companionLogin, ownLogin);

                if (activeUsers.contains(companionLogin)) {
                    // put own login in notification sets for companions (notify companions)
                    HashSet<String> value;
                    if (notifyForDisconnection.containsKey(companionLogin)) {
                        value = notifyForDisconnection.get(companionLogin);
                    } else {
                        value = new HashSet<>();
                    }

                    value.add(ownLogin);
                    notifyForDisconnection.put(companionLogin, value);
                }

                log.debug("{} -> {}: disconnected", ownLogin, companionLogin);
            }

            @Override
            public void onError(Throwable t) {
                log.error("disconnectWithRooms: Error happened while disconnecting: ", t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(Empty.getDefaultInstance());
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void checkCompanionStatus(ChatRoomLogins request, StreamObserver<CompanionStatus> responseObserver) {
        if (activeUsers.contains(request.getCompanionLogin())) {
            log.debug("{} -> {}: companion is online", request.getOwnLogin(), request.getCompanionLogin());
            responseObserver.onNext(CompanionStatus.newBuilder()
                    .setCompanionLogin(request.getCompanionLogin())
                    .setStatus(true)
                    .build());
        } else {
            log.debug("{} -> {}: companion is offline", request.getOwnLogin(), request.getCompanionLogin());
            responseObserver.onNext(CompanionStatus.newBuilder()
                    .setCompanionLogin(request.getCompanionLogin())
                    .setStatus(false)
                    .build());
        }
    }

    private InitRoomResponse buildInitRoomResponse(String companion, String algorithm) {
        return InitRoomResponse.newBuilder()
                .setCompanionLogin(companion)
                .setAlgorithm(algorithm)
                .setEncryptionMode("ECB") // todo
                .setPaddingMode("Zeroes") // todo
                .setInitVector("") // todo
                .setDiffieHellmanP(BigInteger.TWO.pow(8 * 7).toString()) // todo: depends on ley length. Overall = 2^{8 * bytesNeeded}
                .build();
    }

    @Override
    public void initRoom(InitRoomRequest request, StreamObserver<InitRoomResponse> responseObserver) {
        String ownLogin = request.getOwnLogin();
        String companionLogin = request.getCompanionLogin();

        if (!activeUsers.contains(companionLogin)) {
            responseObserver.onNext(InitRoomResponse.getDefaultInstance());
            responseObserver.onCompleted();
            return;
        }

        if (initRoomRequests.containsKey(ownLogin)) {
            var whoRequested = initRoomRequests.get(ownLogin);
            if (whoRequested.containsKey(companionLogin)) {
                // request already has been initiated. Now ownLogin must check for it
                responseObserver.onNext(InitRoomResponse.getDefaultInstance());
                responseObserver.onCompleted();
                return;
            }
        }

        Map<String, InitRoomResponse> value;
        if (initRoomRequests.containsKey(companionLogin)) {
            value = initRoomRequests.get(companionLogin);
        } else {
            value = new HashMap<>();
        }

        InitRoomResponse forOwnLogin = buildInitRoomResponse(companionLogin, request.getAlgorithm());
        InitRoomResponse forCompanionLogin = buildInitRoomResponse(ownLogin, request.getAlgorithm());

        value.put(ownLogin, forCompanionLogin);
        initRoomRequests.put(companionLogin, value);

        rooms.add(new ChatRoom(ownLogin, companionLogin, getTopic(ownLogin, companionLogin), getTopic(companionLogin, ownLogin)));

        log.debug("{} -> {}: initiated room creation", ownLogin, companionLogin);

        responseObserver.onNext(forOwnLogin);
        responseObserver.onCompleted();
    }

    @Override
    public void checkForInitRoomRequest(Login request, StreamObserver<InitRoomResponse> responseObserver) {
        if (!initRoomRequests.containsKey(request.getLogin())) {
            responseObserver.onNext(InitRoomResponse.getDefaultInstance());
            responseObserver.onCompleted();
            return;
        }

        var requesters = initRoomRequests.remove(request.getLogin());

        for (var requestedInit : requesters.entrySet()) {
            log.debug("{} -> {}: got invited to chat", request.getLogin(), requestedInit.getKey());
            responseObserver.onNext(requestedInit.getValue());
        }

        responseObserver.onCompleted();
    }

    @Override
    public void passDiffieHellmanNumber(DiffieHellmanNumber request, StreamObserver<Empty> responseObserver) {
        Map<String, String> senders;
        if (diffieHellmanNumbers.containsKey(request.getCompanionLogin())) {
            senders = diffieHellmanNumbers.get(request.getCompanionLogin());
        } else {
            senders = new HashMap<>();
        }

        senders.put(request.getOwnLogin(), request.getNumber());
        diffieHellmanNumbers.put(request.getCompanionLogin(), senders);

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void anyDiffieHellmanNumber(Login request, StreamObserver<DiffieHellmanNumber> responseObserver) {
        if (!diffieHellmanNumbers.containsKey(request.getLogin())) {
            responseObserver.onNext(DiffieHellmanNumber.getDefaultInstance());
            responseObserver.onCompleted();
            return;
        }

        var senders = diffieHellmanNumbers.remove(request.getLogin());

        for (var sender : senders.entrySet()) {
            log.debug("{} <- {}: got diffie-hellman number {}", request.getLogin(), sender.getKey(), sender.getValue());
            responseObserver.onNext(DiffieHellmanNumber.newBuilder()
                    .setOwnLogin(request.getLogin())
                    .setCompanionLogin(sender.getKey())
                    .setNumber(sender.getValue())
                    .build());
        }

        responseObserver.onCompleted();
    }

    @Override
    public void deleteRoom(ChatRoomLogins request, StreamObserver<CompanionStatus> responseObserver) {
        if (!activeUsers.contains(request.getCompanionLogin())) {
            responseObserver.onNext(CompanionStatus.newBuilder()
                    .setCompanionLogin(request.getCompanionLogin())
                    .setStatus(false)
                    .build());
            responseObserver.onCompleted();
            return;
        }

        if (deleteRoomRequests.containsKey(request.getOwnLogin())) {
            var deletionInitiators = deleteRoomRequests.get(request.getOwnLogin());
            if (deletionInitiators.contains(request.getCompanionLogin())) {
                responseObserver.onNext(CompanionStatus.newBuilder()
                        .setCompanionLogin(request.getCompanionLogin())
                        .setStatus(false)
                        .build());
                responseObserver.onCompleted();
                return;
            }
        }


        Set<String> deletionInitiators;
        if (deleteRoomRequests.containsKey(request.getCompanionLogin())) {
            deletionInitiators = deleteRoomRequests.get(request.getCompanionLogin());
        } else {
            deletionInitiators = new HashSet<>();
        }

        deletionInitiators.add(request.getOwnLogin());
        deleteRoomRequests.put(request.getCompanionLogin(), deletionInitiators);

        log.debug("{} -> {}: initiated deletion of chat", request.getOwnLogin(), request.getCompanionLogin());
        ChatRoom dummyForDeletion = new ChatRoom(request.getOwnLogin(), request.getCompanionLogin(), "", "");
        rooms.remove(dummyForDeletion);

        responseObserver.onNext(CompanionStatus.newBuilder()
                .setCompanionLogin(request.getCompanionLogin())
                .setStatus(true)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void checkForDeletedRoom(Login request, StreamObserver<CompanionStatus> responseObserver) {
        if (!deleteRoomRequests.containsKey(request.getLogin())) {
            responseObserver.onNext(CompanionStatus.getDefaultInstance());
            responseObserver.onCompleted();
            return;
        }

        var requesters = deleteRoomRequests.get(request.getLogin());
        deleteRoomRequests.remove(request.getLogin());

        for (var deletionInitiator : requesters) {
            log.debug("{} -> {}: companion deleted chat", request.getLogin(), deletionInitiator);
            responseObserver.onNext(CompanionStatus.newBuilder().setCompanionLogin(deletionInitiator).setStatus(true).build());
        }

        responseObserver.onCompleted();
    }

    @Override
    public void sendMessage(MessageWithLogins request, StreamObserver<Empty> responseObserver) {
        super.sendMessage(request, responseObserver);
    }

    @Override
    public void anyMessages(ChatRoomLogins request, StreamObserver<MessageWithLogins> responseObserver) {
        super.anyMessages(request, responseObserver);
    }
}
