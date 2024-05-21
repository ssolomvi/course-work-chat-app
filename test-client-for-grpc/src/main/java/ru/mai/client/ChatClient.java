package ru.mai.client;

import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import ru.mai.*;
import ru.mai.client.room.ChatRoomHandler;
import ru.mai.encryption_context.EncryptionContext;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ChatClient implements AutoCloseable {
    private final ScheduledExecutorService scheduled = Executors.newSingleThreadScheduledExecutor();
    private final String login;
    private final ConnectionsHandler connectionsHandler;
    private final ChatRoomHandler chatRoomHandler;
    private final Map<String, Boolean> companionsStatuses; // offline or online
    private final Map<String, EncryptionContext> companionsContexts = new ConcurrentHashMap<>();

    private int checkForDiffieHellmanNumbers = 0;

    // todo: load all companions from database
    List<String> companions = List.of("Boba", "Alexandr", "Ilya");

    public ChatClient(ManagedChannel channel, String login) {
        ChatServiceGrpc.ChatServiceBlockingStub stub = ChatServiceGrpc.newBlockingStub(channel);
        ChatServiceGrpc.ChatServiceStub asyncStub = ChatServiceGrpc.newStub(channel);

        log.info("{} started, listening with channel {}", login, channel);

        this.login = login;
        this.connectionsHandler = new ConnectionsHandler(login, stub, asyncStub);

        // todo: load all companions from database

        BigInteger diffieHellmanG = connectionsHandler.connectToServer();

        this.companionsStatuses = connectionsHandler.connectRooms(companions);
        this.chatRoomHandler = new ChatRoomHandler(login, stub, asyncStub, diffieHellmanG);

        pingServer();
    }

    public void connect() {
        if (!companions.isEmpty()) {
            this.companionsStatuses.putAll(connectionsHandler.connectRooms(companions));
        }
    }

    // region Ping server
    private void pingServer() {
        log.debug("{}: pinging server", login);
        scheduled.scheduleAtFixedRate(
                () -> {
                    checkCompanionsStatuses();
                    checkForDeleteRoomRequest();
                    checkForInitRoomRequests();
                    if (checkForDiffieHellmanNumbers != 0) {
                        checkForDiffieHellmanNumbers();
                    }
                },
                0, 5, TimeUnit.SECONDS
        );
    }

    public void checkCompanionsStatuses() {
        for (Map.Entry<String, Boolean> companionStatus : companionsStatuses.entrySet()) {
            boolean isOnline = connectionsHandler.checkCompanionStatus(companionStatus.getKey());
            if (isOnline != Boolean.TRUE.equals(companionStatus.getValue())) {
                companionsStatuses.put(companionStatus.getKey(), isOnline);
            }
        }
    }

    public void checkForInitRoomRequests() {
        boolean thereAreRequests = chatRoomHandler.checkForInitRoomRequests();
        if (thereAreRequests) {
            chatRoomHandler.passDiffieHellmanNumber();
            ++checkForDiffieHellmanNumbers;
        }
    }

    public void checkForDiffieHellmanNumbers() {
        var response = chatRoomHandler.anyDiffieHellmanNumbers();

        if (!response.isEmpty()) {
            --checkForDiffieHellmanNumbers;
        }

        this.companionsContexts.putAll(response);
    }

    public void checkForDeleteRoomRequest() {
//        log.debug("checking for delete room request");
        var requestedDeletion = chatRoomHandler.checkForDeleteRoomRequests();

        for (Map.Entry<String, Boolean> requester : requestedDeletion.entrySet()) {
            if (Boolean.TRUE.equals(requester.getValue())) {
                companionsContexts.remove(requester.getKey());
            }
        }
    }
//     endregion

    // or better close()?
    public void disconnect() throws InterruptedException {
        connectionsHandler.disconnectRooms(companions);
        connectionsHandler.disconnectFromServer();
        log.info("{}: disconnected from server", login);
        // flush messages if using cash
        // close executor
    }

    public void addRoom(String companion, String algorithm) {
        log.debug("{}: initiated adding room with {}", login, companion);
        if (chatRoomHandler.initRoom(companion, algorithm)) {
            chatRoomHandler.passDiffieHellmanNumber();
        }
    }

    public void deleteRoom(String companion) {
        if (chatRoomHandler.deleteRoom(companion)) {
            log.info("{}: deleted chat room with {}", login, companion);
            companionsContexts.remove(companion);
        } else {
            log.info("{}: Error deleting chat room, 'cause {} is offline or already sent a delete request", login, companion);
        }
    }

    public boolean sendMessage(String companion) {
        return false;
    }

    public void checkForMessages(String companion) {
        //
    }

    @Override
    public void close() throws Exception {
        scheduled.shutdown();
        try {
            if (!scheduled.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                scheduled.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduled.shutdownNow();
        }
    }
}
