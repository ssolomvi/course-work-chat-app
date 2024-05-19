package ru.mai.service;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import ru.mai.*;
import ru.mai.model.ChatRoom;
import ru.mai.model.KafkaMessage;
import ru.mai.serialization.KafkaMessageDeserializer;

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
        log.debug("User {} connected", request.getLogin());
        activeUsers.add(request.getLogin());
        responseObserver.onNext(connectResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void disconnect(Login request, StreamObserver<Empty> responseObserver) {
        log.debug("User {} disconnected", request.getLogin());
        activeUsers.remove(request.getLogin());
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    private KafkaConsumer<String, KafkaMessage> createKafkaConsumer(String topic) {
        // todo: configs
        KafkaConsumer<String, KafkaMessage> consumer = new KafkaConsumer<>(
                Map.of(),
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

                log.debug("User {} connected with companion {}", ownLogin, companionLogin);

                responseObserver.onNext(CompanionStatus.newBuilder()
                        .setCompanionLogin(companionLogin)
                        .setStatus(companionIsActive)
                        .build());
            }

            @Override
            public void onError(Throwable t) {
                log.error("Error happened while connecting: ", t);
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

                var map = consumers.get(ownLogin);
                if (map.isEmpty()) {
                    consumers.remove(ownLogin);
                }

                log.debug("User {} disconnected with companion {}", ownLogin, companionLogin);

                responseObserver.onNext(Empty.getDefaultInstance());
            }

            @Override
            public void onError(Throwable t) {
                log.error("Error happened while disconnecting: ", t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void checkCompanionStatus(ChatRoomLogins request, StreamObserver<CompanionStatus> responseObserver) {
        if (activeUsers.contains(request.getCompanionLogin())) {
            responseObserver.onNext(CompanionStatus.newBuilder()
                    .setCompanionLogin(request.getCompanionLogin())
                    .setStatus(true)
                    .build());
        } else {
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
                .setEncryptionMode("") // todo
                .setPaddingMode("") // todo
                .setInitVector("") // todo
                .setDiffieHellmanP("") // todo
                .build();
    }

    @Override
    public void initRoom(InitRoomRequest request, StreamObserver<InitRoomResponse> responseObserver) {
        String ownLogin = request.getOwnLogin();
        String companionLogin = request.getCompanionLogin();

        if (initRoomRequests.containsKey(ownLogin) || !activeUsers.contains(companionLogin)) {
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

        log.debug("User {} initiated room creation with {}", ownLogin, companionLogin);

        responseObserver.onNext(forOwnLogin);
        responseObserver.onCompleted();
    }

    @Override
    public void checkForInitRoomRequest(Login request, StreamObserver<InitRoomResponse> responseObserver) {
        // make a chat room
        // make consumers
        if (!initRoomRequests.containsKey(request.getLogin())) {
            responseObserver.onNext(InitRoomResponse.getDefaultInstance());
            responseObserver.onCompleted();
        }

        for (var requestedInit : initRoomRequests.get(request.getLogin()).entrySet()) {
            log.debug("User {} got invited to chat with {}", request.getLogin(), requestedInit.getKey());
            responseObserver.onNext(requestedInit.getValue());
        }

        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<DiffieHellmanNumber> passDiffieHellmanNumber(StreamObserver<Empty> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(DiffieHellmanNumber value) {
                Map<String, String> senders;
                if (diffieHellmanNumbers.containsKey(value.getCompanionLogin())) {
                    senders = diffieHellmanNumbers.get(value.getCompanionLogin());
                } else {
                    senders = new HashMap<>();
                }

                senders.put(value.getOwnLogin(), value.getNumber());

                diffieHellmanNumbers.put(value.getCompanionLogin(), senders);
                log.debug("User {} passed diffie-hellman number to {}", value.getOwnLogin(), value.getCompanionLogin());

                responseObserver.onNext(Empty.getDefaultInstance());
            }

            @Override
            public void onError(Throwable t) {
                log.error("Error happened while connecting: ", t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void anyDiffieHellmanNumber(Login request, StreamObserver<DiffieHellmanNumber> responseObserver) {
        if (!diffieHellmanNumbers.containsKey(request.getLogin())) {
            responseObserver.onNext(DiffieHellmanNumber.getDefaultInstance());
            responseObserver.onCompleted();
            return;
        }

        for (var senders : diffieHellmanNumbers.get(request.getLogin()).entrySet()) {
            log.debug("User {} got diffie-hellman number from {}", request.getLogin(), senders.getKey());
            responseObserver.onNext(DiffieHellmanNumber.newBuilder()
                    .setOwnLogin(request.getLogin())
                    .setCompanionLogin(senders.getKey())
                    .setNumber(senders.getValue())
                    .build());
        }

        responseObserver.onCompleted();
    }

    @Override
    public void deleteRoom(ChatRoomLogins request, StreamObserver<CompanionStatus> responseObserver) {
        if (deleteRoomRequests.containsKey(request.getOwnLogin()) || !activeUsers.contains(request.getCompanionLogin())) {
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

        log.debug("User {} initiated deletion of chat with {}", request.getOwnLogin(), request.getCompanionLogin());

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

        for (var deletionInitiator : deleteRoomRequests.get(request.getLogin())) {
            log.debug("User {} has chat deleted with {}", request.getLogin(), deletionInitiator);
            responseObserver.onNext(CompanionStatus.newBuilder().setCompanionLogin(deletionInitiator).build());
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
