package ru.mai;

import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.mai.client.ChatClient;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootApplication
public class ClientApplication implements CommandLineRunner {
    private static final String TARGET = "localhost:50051";

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        ManagedChannel channel = Grpc.newChannelBuilder(TARGET, InsecureChannelCredentials.create())
                .build();

        try (ChatClient boba = new ChatClient(channel, "Boba")) {
            log.debug("Initiated client Boba");
            Thread.sleep(3000);
            try (ChatClient alex = new ChatClient(channel, "Alexandr")) {
                log.debug("Initiated client Alexandr");

                alex.addRoom("Boba", "ECB");
                alex.checkForDiffieHellmanNumbers();
                Thread.sleep(5000);
//                boba.deleteRoom("Alexandr");
//                alex.checkForDeleteRoomRequest();
                alex.checkForDiffieHellmanNumbers();
                boba.checkForDiffieHellmanNumbers();
                List<Integer> list = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
                Thread.sleep(5000);
//                alex.disconnect();
            }
//            boba.disconnect();
        } catch (Exception e) {
            log.error("Client application -- error happened: ", e);
        } finally {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
