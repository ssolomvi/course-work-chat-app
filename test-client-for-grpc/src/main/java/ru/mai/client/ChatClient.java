package ru.mai.client;

import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import ru.mai.*;
import ru.mai.client.room.ChatRoomHandler;
import ru.mai.encryption_context.EncryptionContext;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ChatClient {
    private final String login;
    private final ConnectionsHandler connectionsHandler;
//    private final ChatRoomHandler chatRoomHandler;
//    private final Map<String, Boolean> companionsStatuses; // offline or online
    private Map<String, EncryptionContext> companionsContexts;

    // todo: load all companions from database
    List<String> companions = List.of("Boba", "Sasha", "Ilya");

    public ChatClient(ManagedChannel channel, String login) throws InterruptedException {
        ChatServiceGrpc.ChatServiceBlockingStub stub = ChatServiceGrpc.newBlockingStub(channel);
        log.info("Client started, listening with channel {}", channel);
        channel.getState(true);

        this.login = login;
        this.connectionsHandler = new ConnectionsHandler(login, stub);

        // todo: load all companions from database

        BigInteger diffieHellmanG = connectionsHandler.connectToServer();

//        this.companionsStatuses = connectionsHandler.connectRooms(companions);
//        this.chatRoomHandler = new ChatRoomHandler(login, asyncStub, diffieHellmanG);

//        pingServer();
    }

//    public void connect() {
//        if (!companions.isEmpty()) {
//            this.companionsStatuses.putAll(connectionsHandler.connectRooms(companions));
//        }
//    }

    // region Ping server
//    private void pingServer() {
//        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
//                () -> {
//                    checkForDeleteRoomRequest();
//                    checkForInitRoomRequests();
//                    checkForDiffieHellmanNumbers();
//                    checkCompanionsStatuses();
//                },
//                0, 5, TimeUnit.SECONDS
//        );
//    }

//    private void checkCompanionsStatuses() {
//        for (Map.Entry<String, Boolean> companionStatus : companionsStatuses.entrySet()) {
//            boolean isOnline = connectionsHandler.checkCompanionStatus(companionStatus.getKey());
//            if (isOnline != Boolean.TRUE.equals(companionStatus.getValue())) {
//                companionsStatuses.put(companionStatus.getKey(), isOnline);
//            }
//        }
//    }

//    private void checkForInitRoomRequests() {
//        boolean thereAreRequests = chatRoomHandler.checkForInitRoomRequests();
//        if (thereAreRequests) {
//            chatRoomHandler.passDiffieHellmanNumber();
//        }
//    }

//    private void checkForDiffieHellmanNumbers() {
//        var passedDiffieHellmanNumbers = chatRoomHandler.anyDiffieHellmanNumbers();
//
//        this.companionsContexts.putAll(passedDiffieHellmanNumbers);
//    }

//    private void checkForDeleteRoomRequest() {
//        var requestedDeletion = chatRoomHandler.checkForDeleteRoomRequests();
//
//        for (Map.Entry<String, Boolean> requester : requestedDeletion.entrySet()) {
//            if (Boolean.TRUE.equals(requester.getValue())) {
//                companionsContexts.remove(requester.getKey());
//            }
//        }
//    }
    // endregion

    // or better close()?
    public void disconnect() {
//        connectionsHandler.disconnectRooms(companions);
        connectionsHandler.disconnectFromServer();
    }

//    public void addRoom(String companion, String algorithm) {
//        if (chatRoomHandler.initRoom(companion, algorithm)) {
//            chatRoomHandler.passDiffieHellmanNumber();
//        }
//    }

//    public void deleteRoom(String companion) {
//        if (chatRoomHandler.deleteRoom(companion)) {
//            log.info("Deleted chat room with {}", companion);
//            companionsContexts.remove(companion);
//        } else {
//            log.info("Error deleting chat room, 'cause {} is offline or already sent a delete request", companion);
//        }
//    }

    public boolean sendMessage(String companion) {
        return false;
    }

    public void checkForMessages(String companion) {
        //
    }
}
