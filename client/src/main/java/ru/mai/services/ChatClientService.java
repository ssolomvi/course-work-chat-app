package ru.mai.services;

import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import ru.mai.Login;
import ru.mai.kafka.KafkaManager;
import ru.mai.kafka.model.MessageDto;
import ru.mai.services.chatroom.ChatRoomHandler;
import ru.mai.services.connections.ConnectionHandler;
import ru.mai.services.messages.MessageHandler;
import ru.mai.utils.Pair;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Slf4j
@SpringComponent
@Scope("prototype")
@VaadinSessionScope
public class ChatClientService {
    private final ConnectionHandler connectionHandler;
    private final ChatRoomHandler chatRoomHandler;
    private final MessageHandler messageHandler;
    private String login;
    private Login loginStructure;
    private BigInteger dhG;
    @Getter
    private int checkForDiffieHellmanNumbers;

    public ChatClientService(@Autowired ConnectionHandler connectionHandler,
                             @Autowired ChatRoomHandler chatRoomHandler,
                             @Autowired MessageHandler messageHandler) {
        this.connectionHandler = connectionHandler;
        this.chatRoomHandler = chatRoomHandler;
        this.messageHandler = messageHandler;
    }

    public void setLogin(String login) {
        this.login = login;
        this.loginStructure = Login.newBuilder().setLogin(login).build();
        initMessageHandler();
    }

    public void initMessageHandler() {
        this.messageHandler.init(login);
    }

    public void connect() {
        this.dhG = connectionHandler.connect(loginStructure);
        this.checkForDiffieHellmanNumbers = 0;
    }

    public void disconnect() {
        log.debug("Disconnect invoked");
        connectionHandler.disconnect(loginStructure);
        chatRoomHandler.close();
        messageHandler.close();
    }

    public List<String> checkForDisconnected() {
        return connectionHandler.checkForDisconnected(loginStructure);
    }

    public boolean addRoom(String companion, String algorithm, String encryptionMode, String paddingMode) {
        if (chatRoomHandler.initRoom(login, companion, algorithm, encryptionMode, paddingMode)) {
            passDiffieHellmanNumber(companion);
            return true;
        }
        return false;
    }

    public void checkForInitRoomRequests() {
        var companions = chatRoomHandler.checkForInitRoomRequests(loginStructure);

        if (companions.isEmpty()) {
            return;
        }

        for (String companion : companions) {
            passDiffieHellmanNumber(companion);
        }
    }

    private void passDiffieHellmanNumber(String companion) {
        chatRoomHandler.passDiffieHellmanNumber(login, companion, dhG);
        ++checkForDiffieHellmanNumbers;
    }

    public List<String> checkForDiffieHellmanNumbers() {
        var response = chatRoomHandler.anyDiffieHellmanNumbers(loginStructure);

        List<String> newCompanions = new LinkedList<>();
        if (!response.isEmpty()) {
            checkForDiffieHellmanNumbers -= response.size();

            newCompanions.addAll(response.keySet());
            messageHandler.addAllContexts(response);
        }
        return newCompanions;
    }


    /**
     * Invokes room deletion
     *
     * @param companion companion login
     * @return {@code true} if room was deleted, {@code false} otherwise (companion is not online or companion already requested deletion)
     */
    public boolean deleteRoom(String companion) {
        var result = chatRoomHandler.deleteRoom(login, companion);
        if (result) {
            messageHandler.remove(companion);
        }
        return result;
    }


    public List<String> checkForDeleteRoomRequest() {
        return chatRoomHandler.checkForDeleteRoomRequests(loginStructure);
    }

    public void sendMessage(String companion, String message) {
        messageHandler.sendByteArray(login, companion, message.getBytes(StandardCharsets.UTF_8));
    }

    public void sendFile(String companion, String filename, InputStream stream, long fileSize) throws IllegalArgumentException, IOException, InterruptedException {
        messageHandler.sendFile(login, companion, filename, stream, fileSize);
    }

    public List<MessageDto> checkForMessages() {
        return messageHandler.anyMessages(loginStructure);
    }


    public Optional<String> processByteArrayMessage(MessageDto msg) {
        return messageHandler.processByteArrayMessage(msg);
    }

    public Optional<Pair<String, InputStream>> processFileMessage(MessageDto msg) throws IOException {
        return messageHandler.processFileMessage(msg);
    }

//    public Optional<Pair<String, byte[]>> processFileMessage(MessageDto msg) {
//        return messageHandler.processFileMessage(msg);
//    }

}
