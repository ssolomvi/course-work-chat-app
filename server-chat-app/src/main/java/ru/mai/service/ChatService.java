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
    private final Set<String> activeUsers = ConcurrentHashMap.newKeySet();
    private final Set<ChatRoom> rooms = ConcurrentHashMap.newKeySet(); // login1, login2, topic1, topic2
    private final Map<String, Map<String, Optional<KafkaConsumer<String, KafkaMessage>>>> consumers = new ConcurrentHashMap<>();

    private final Map<String, HashSet<String>> areWaitedForConnection = new ConcurrentHashMap<>(); // key is who is being awaited, value is waiters
    private final Map<String, HashSet<String>> connectedAfterBeingWaited = new ConcurrentHashMap<>(); // key is who waited, value is who was waited

    private final Map<String, HashSet<String>> disconnected = new ConcurrentHashMap<>(); // key is a disconnected client, value is to notify


    private String getTopic(String client1, String client2) {
        return String.format("%s_%s", client1, client2);
    }

    @Override
    public void connect(Login request, StreamObserver<ConnectResponse> responseObserver) {
        super.connect(request, responseObserver);
    }

    @Override
    public void disconnect(Login request, StreamObserver<Empty> responseObserver) {
        super.disconnect(request, responseObserver);
    }

    private KafkaConsumer<String, KafkaMessage> createKafkaConsumer(String topic) {
        // todo: configs
        return new KafkaConsumer<>(Map.of(), new StringDeserializer(), new KafkaMessageDeserializer());
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
    }

    @Override
    public StreamObserver<ChatRoomLogins> connectWithRooms(StreamObserver<ConnectResponseRoom> responseObserver) {
        return new StreamObserver<>() {
            private Boolean isActive = false;

            @Override
            public void onNext(ChatRoomLogins room) {
                activeUsers.add(room.getOwnLogin());

                String ownLogin = room.getOwnLogin();
                String companionLogin = room.getCompanionLogin();

                if (activeUsers.contains(room.getCompanionLogin())) {
                    isActive = true;

                    // add chat room
                    String topicInOwnLoginCompanionLogin = getTopic(ownLogin, companionLogin);
                    String topicInCompanionLoginOwnLogin = getTopic(companionLogin, ownLogin);
                    rooms.add(new ChatRoom(ownLogin, companionLogin, topicInOwnLoginCompanionLogin, topicInCompanionLoginOwnLogin));

                    // init consumers for client1 room and client2 room
                    addConsumer(ownLogin, companionLogin);
                    addConsumer(companionLogin, ownLogin);

                    // add to collection to notify this client's companion
                    HashSet<String> connected; // users who connect after being waited for
                    if (connectedAfterBeingWaited.containsKey(companionLogin)) {
                        connected = connectedAfterBeingWaited.get(companionLogin);
                    } else {
                        connected = new HashSet<>();
                    }
                    connected.add(ownLogin);
                    connectedAfterBeingWaited.put(companionLogin, connected);

                } else {
                    // add to collection to await for companion
                    HashSet<String> waiters;
                    if (areWaitedForConnection.containsKey(companionLogin)) {
                        waiters = areWaitedForConnection.get(companionLogin);
                    } else {
                        waiters = new HashSet<>();
                    }
                    waiters.add(ownLogin);
                    areWaitedForConnection.put(companionLogin, waiters);
                }

                responseObserver.onNext(ConnectResponseRoom.newBuilder()
                        .setDiffieHellmanG(DIFFIE_HELLMAN_G)
                        .setCompanionConnected(isActive)
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

                if (!activeUsers.contains(companionLogin)) {
                    // no need to tell client is disconnecting
                    ChatRoom dummy = new ChatRoom(ownLogin, companionLogin, "", "");

                    rooms.remove(dummy);
                    // за себя и за Сашку
                    removeConsumer(ownLogin, companionLogin);
                    removeConsumer(companionLogin, ownLogin);
                } else if (disconnected.containsKey(ownLogin)) {
                    var value = disconnected.get(ownLogin);
                    value.remove(companionLogin);
                    disconnected.put(ownLogin, value);
                }
                
                var map = consumers.get(ownLogin);
                if (map.isEmpty()) {
                    consumers.remove(ownLogin);
                }

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
    public void initRoom(InitRoomRequest request, StreamObserver<InitRoomResponse> responseObserver) {
        super.initRoom(request, responseObserver);
    }

    @Override
    public void checkForInitRoomRequest(Empty request, StreamObserver<CompanionLogin> responseObserver) {
        super.checkForInitRoomRequest(request, responseObserver);
    }

    @Override
    public void passDiffieHellmanNumber(DiffieHellmanNumber request, StreamObserver<Empty> responseObserver) {
        super.passDiffieHellmanNumber(request, responseObserver);
    }

    @Override
    public void anyDiffieHellmanNumber(Empty request, StreamObserver<DiffieHellmanNumber> responseObserver) {
        super.anyDiffieHellmanNumber(request, responseObserver);
    }

    @Override
    public void deleteRoom(ChatRoomLogins request, StreamObserver<Empty> responseObserver) {
        super.deleteRoom(request, responseObserver);
    }

    @Override
    public void checkForDeletedRoom(ChatRoomLogins request, StreamObserver<CompanionLogin> responseObserver) {
        super.checkForDeletedRoom(request, responseObserver);
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
