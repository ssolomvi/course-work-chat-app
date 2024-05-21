package ru.mai.client;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import ru.mai.*;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Used for connecting / disconnecting from server with or without chat rooms
 */
@Slf4j
public class ConnectionsHandler {
    private final String userLogin;
    private final Login login;
    private final ChatServiceGrpc.ChatServiceStub asyncStub;
    private final ChatServiceGrpc.ChatServiceBlockingStub blockingStub;

    public ConnectionsHandler(String userLogin,
                              ChatServiceGrpc.ChatServiceBlockingStub blockingStub,
                              ChatServiceGrpc.ChatServiceStub asyncStub) {
        this.userLogin = userLogin;
        this.login = Login.newBuilder().setLogin(userLogin).build();
        this.blockingStub = blockingStub;
        this.asyncStub = asyncStub;
    }

    private ChatRoomLogins buildChatRoomLogins(String companion) {
        return ChatRoomLogins.newBuilder()
                .setOwnLogin(userLogin)
                .setCompanionLogin(companion)
                .build();
    }

    /**
     * Must be invoked once, for beginning of client-server communication
     * <p>
     * Connects to server (server add client to active users)
     *
     * @return number g, needed for diffie-hellman key exchange
     */
    public BigInteger connectToServer() {
        return new BigInteger(blockingStub.connect(login).getDiffieHellmanG());
    }

    /**
     * Invoked for connection to server, if any rooms present from db.
     *
     * @param companions companions logins
     * @return map of companions logins and their status (online = true, offline = false)
     */
    public Map<String, Boolean> connectRooms(Iterable<String> companions) {
        final CountDownLatch finishLatch = new CountDownLatch(1);
        final Map<String, Boolean> companionsAndStatus = new HashMap<>();

        StreamObserver<CompanionStatus> responseObserver = new StreamObserver<>() {
            private final CompanionStatus dummy = CompanionStatus.getDefaultInstance();

            @Override
            public void onNext(CompanionStatus value) {
                if (value.equals(dummy)) {
                    return;
                }

                companionsAndStatus.put(value.getCompanionLogin(), value.getStatus());
                log.debug("Companion {} is {}", value.getCompanionLogin(), value.getStatus() ? "online" : "offline");
            }

            @Override
            public void onError(Throwable t) {
                log.error("Error occurred, cause: ", t);
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                log.debug("Connection Rooms stream ended");
                finishLatch.countDown();
            }
        };

        StreamObserver<ChatRoomLogins> requestObserver = asyncStub.connectWithRooms(responseObserver);

        try {
            for (String companion : companions) {
                requestObserver.onNext(buildChatRoomLogins(companion));
            }
        } catch (RuntimeException e) {
            // Cancel RPC
            requestObserver.onError(e);
            throw e;
        }

        requestObserver.onCompleted();

        if (finishLatch.getCount() == 0) {
            return companionsAndStatus;
        }
        return Collections.emptyMap();
    }

    /**
     * Invoked for disconnecting from server, after disconnecting all chat rooms
     */
    public void disconnectFromServer() {
        blockingStub.disconnect(login);
    }

    /**
     * Invoked for disconnecting from companions
     *
     * @param companions list of companions with whom chat room exists
     */
    public void disconnectRooms(Iterable<String> companions) throws InterruptedException {
        final CountDownLatch finishLatch = new CountDownLatch(1);

        StreamObserver<ChatRoomLogins> requestObserver = asyncStub.disconnectWithRooms(new StreamObserver<>() {
            @Override
            public void onNext(Empty value) {
                //
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

        try {
            for (String companion : companions) {
                requestObserver.onNext(buildChatRoomLogins(companion));
            }
        } catch (RuntimeException e) {
            requestObserver.onError(e);
            throw e;
        }

        requestObserver.onCompleted();

        if (!finishLatch.await(30, TimeUnit.SECONDS)) {
            log.warn("disconnectRooms can not finish within 30s!");
        }
    }

    public boolean checkCompanionStatus(String companion) {
        ChatRoomLogins chatRoomLogins = ChatRoomLogins.newBuilder().setOwnLogin(userLogin).setCompanionLogin(companion).build();

        return blockingStub.checkCompanionStatus(chatRoomLogins).getStatus();
    }
}
