package ru.mai.services.connections;

import com.vaadin.flow.spring.annotation.SpringComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import ru.mai.ChatServiceGrpc;
import ru.mai.EnumStatus;
import ru.mai.Login;

import java.math.BigInteger;

/**
 * Used for connecting / disconnecting from server with or without chat rooms
 */
@Slf4j
@SpringComponent
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
