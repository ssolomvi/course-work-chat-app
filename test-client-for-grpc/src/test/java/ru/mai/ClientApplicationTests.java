package ru.mai;

import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.Test;
import ru.mai.client.ChatClient;

import java.util.concurrent.TimeUnit;

class ClientApplicationTests {
    String simpleUserLogin = "aboba";

    @Test
    void checkClientConnectDisconnect() throws InterruptedException {
        // Access a service running on the local machine on port 50051
        String target = "localhost:8080";
        ManagedChannel channel = Grpc.newChannelBuilder(target, InsecureChannelCredentials.create()).build();

        try {
            ChatClient client = new ChatClient(channel, simpleUserLogin);

            client.disconnect();
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
