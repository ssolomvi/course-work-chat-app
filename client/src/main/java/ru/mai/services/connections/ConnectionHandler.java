package ru.mai.services.connections;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mai.ChatRoomLogins;
import ru.mai.ChatServiceGrpc;
import ru.mai.EnumStatus;
import ru.mai.Login;
import ru.mai.db.repositories.ChatMetadataEntityRepository;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

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
        if (blockingStub.disconnect(login).getEnumStatus().equals(EnumStatus.ENUM_STATUS_OK)) {
            log.debug("Disconnected from server");
        } else {
            log.debug("Error disconnecting from server");
        }

    }
}
