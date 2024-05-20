package ru.mai;

import ru.mai.server.ChatServiceServer;

import java.io.IOException;

public class ServerApplication {
    private static final int PORT = 50051;

    public static void main(String[] args) throws InterruptedException, IOException {
        ChatServiceServer chatServiceServer = new ChatServiceServer(PORT);

        chatServiceServer.start();
        chatServiceServer.blockUntilShutdown();
    }
}