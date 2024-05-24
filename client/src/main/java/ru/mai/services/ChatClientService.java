package ru.mai.services;

import org.springframework.beans.factory.annotation.Autowired;
import ru.mai.services.chatroom.ChatRoomHandler;
import ru.mai.services.connections.ConnectionHandler;
import ru.mai.services.messages.MessageHandler;

public class ChatClientService {
    private final ConnectionHandler connectionHandler;
    private final ChatRoomHandler chatRoomHandler;
    private final MessageHandler messageHandler;


    public ChatClientService(@Autowired ConnectionHandler connectionHandler,
                             @Autowired ChatRoomHandler chatRoomHandler,
                             @Autowired MessageHandler messageHandler) {
        this.connectionHandler = connectionHandler;
        this.chatRoomHandler = chatRoomHandler;
        this.messageHandler = messageHandler;
    }
}
