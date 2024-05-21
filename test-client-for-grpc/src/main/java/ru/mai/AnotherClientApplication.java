package ru.mai;

import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import ru.mai.client.ChatClient;

import java.util.concurrent.TimeUnit;

@Slf4j
public class AnotherClientApplication {
    private static final String TARGET = "localhost:50051";

    public static void main(String[] args) throws InterruptedException {
        ManagedChannel channel = Grpc.newChannelBuilder(TARGET, InsecureChannelCredentials.create())
                .build();

        try (ChatClient alexandr = new ChatClient(channel, "Alexandr")) {
            log.debug("Initiated client");

            log.info("Checking companion statuses:");
            alexandr.checkCompanionsStatuses();

            log.info("Adding room with Boba and ECB:");
            alexandr.addRoom("Boba", "ECB");
            log.info("Trying deleting non-existing room");
            Thread.sleep(20000);
            alexandr.deleteRoom("aboba");
            Thread.sleep(20000);
            log.info("Deleting room with Boba");
            alexandr.deleteRoom("Boba");

            log.info("Disconnecting");
//            client.disconnect();
        } catch (Exception e) {
            log.error("Another client application -- error happened: ", e);
        } finally {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
