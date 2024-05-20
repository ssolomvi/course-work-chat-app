package ru.mai;

import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import ru.mai.client.ChatClient;

import java.util.concurrent.TimeUnit;

public class ClientApplication {
    private static final String TARGET = "localhost:50051";

    public static void main(String[] args) throws InterruptedException {
        ManagedChannel channel = Grpc.newChannelBuilder(TARGET, InsecureChannelCredentials.create())
                .build();

        try {
            ChatClient client = new ChatClient(channel, "aboba");

            client.disconnect();
        } finally {
            channel.shutdown().awaitTermination(100, TimeUnit.SECONDS);
        }
    }

}
