package ru.mai.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mai.ChatRoomLogins;
import ru.mai.ChatServiceGrpc;
import ru.mai.Login;

import java.math.BigInteger;

/**
 * Used for connecting / disconnecting from server with or without chat rooms
 */
@Slf4j
@Component
public class ConnectionHandler {
    private final ChatServiceGrpc.ChatServiceBlockingStub blockingStub;

    public ConnectionHandler(@Autowired ChatServiceGrpc.ChatServiceBlockingStub blockingStub) {
        this.blockingStub = blockingStub;
    }

    /**
     * Must be invoked once, for beginning of client-server communication
     * <p>
     * Connects to server (server add client to active users)
     *
     * @return number g, needed for diffie-hellman key exchange
     */
    public BigInteger connect(Login login) {
        return new BigInteger(blockingStub.connect(login).getDiffieHellmanG());
    }

    /**
     * Invoked for disconnecting from server
     */
    public void disconnect(Login login) {
        blockingStub.disconnect(login);
    }


    /**
     * Invoked for checking on companion's status
     *
     * @param own       invoker login
     * @param companion companion login
     * @return {@code true}, if companion is online, {@code false} otherwise
     */
    public boolean checkCompanionStatus(String own, String companion) {
        ChatRoomLogins chatRoomLogins = ChatRoomLogins.newBuilder().setOwnLogin(own).setCompanionLogin(companion).build();

        return blockingStub.checkCompanionStatus(chatRoomLogins).getStatus();
    }
}
