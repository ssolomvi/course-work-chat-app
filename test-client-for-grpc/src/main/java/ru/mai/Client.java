package ru.mai;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import ru.mai.client.ChatClient;

import java.nio.channels.Channel;

public class Client {
    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder.forTarget("")
                .usePlaintext()
                .build();

        ChatClient client = new ChatClient(channel, "Aboba");

        client.disconnect();
        channel.shutdownNow();
    }

}
