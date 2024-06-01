package ru.mai.services.connections;

import com.vaadin.flow.spring.annotation.SpringComponent;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import ru.mai.ChatServiceGrpc;
import ru.mai.EnumStatus;
import ru.mai.Login;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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

    public List<String> checkForDisconnected(Login login) {
        List<String> disconnected = new LinkedList<>();
        try {
            Iterator<Login> disconnectedClients = blockingStub.checkDisconnect(login);

            if (!disconnectedClients.hasNext()) {
                return Collections.emptyList();
            }

            final Login dummy = Login.getDefaultInstance();
            while (disconnectedClients.hasNext()) {
                Login client = disconnectedClients.next();
                if (client.equals(dummy)) {
                    continue;
                }
                disconnected.add(client.getLogin());
            }
        } catch (StatusRuntimeException e) {
            log.error("{} checkForDisconnected: Error happened, cause: ", login, e);
            return Collections.emptyList();
        }
        return disconnected;
    }
}
