package ru.mai.client;

import io.grpc.stub.StreamObserver;
import ru.mai.ChatRoomLogins;
import ru.mai.ChatServiceGrpc;
import ru.mai.CompanionStatus;
import ru.mai.Login;
import ru.mai.observers.ConnectResponseObserver;
import ru.mai.observers.CompanionStatusObserver;
import ru.mai.observers.EmptyResponseObserver;

import java.math.BigInteger;
import java.util.Map;

/**
 * Used for connecting / disconnecting from server with or without chat rooms
 */
public class ConnectionsHandler {
    private final String userLogin;
    private final Login login;
    private final ChatServiceGrpc.ChatServiceStub asyncStub;

    public ConnectionsHandler(String userLogin,
                              ChatServiceGrpc.ChatServiceStub asyncStub) {
        this.userLogin = userLogin;
        this.login = Login.newBuilder().setLogin(userLogin).build();
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
     * @return number g, needed for diffie-hellman key exchange
     */
    public BigInteger connectToServer() {
        ConnectResponseObserver connectResponseObserver = new ConnectResponseObserver();

        asyncStub.connect(login, connectResponseObserver);

        return new BigInteger(connectResponseObserver.getDiffieHellmanG());
    }

    /**
     * Invoked for connection to server, if any rooms present from db.
     * @param companions companions logins
     * @return map of companions logins and their status (online = true, offline = false)
     */
    public Map<String, Boolean> connectRooms(Iterable<String> companions) {
        CompanionStatusObserver companionStatusObserver = new CompanionStatusObserver();

        StreamObserver<ChatRoomLogins> requestObserver = asyncStub.connectWithRooms(companionStatusObserver);

        for (String companion : companions) {
            requestObserver.onNext(buildChatRoomLogins(companion));
        }

        requestObserver.onCompleted();

        return companionStatusObserver.getCompanionsAndStatus();
    }

    /**
     * Invoked for disconnecting from server, after disconnecting all chat rooms
     */
    public void disconnectFromServer() {
        asyncStub.disconnect(login, new EmptyResponseObserver());
    }

    /**
     * Invoked for disconnecting from companions
     * @param companions list of companions with whom chat room exists
     */
    public void disconnectRooms(Iterable<String> companions) {
        StreamObserver<ChatRoomLogins> requestObserver = asyncStub.disconnectWithRooms(new EmptyResponseObserver());

        for (String companion : companions) {
            requestObserver.onNext(buildChatRoomLogins(companion));
        }

        requestObserver.onCompleted();
    }

    public boolean checkCompanionStatus(String companion) {
        ChatRoomLogins chatRoomLogins = ChatRoomLogins.newBuilder().setOwnLogin(userLogin).setCompanionLogin(companion).build();
        CompanionStatusObserver observer = new CompanionStatusObserver();

        asyncStub.checkCompanionStatus(chatRoomLogins, observer);

        return observer.getCompanionsAndStatus().get(companion);
    }
}