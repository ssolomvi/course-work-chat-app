package ru.mai;

import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import ru.mai.client.ChatClient;

import java.util.concurrent.TimeUnit;

@Slf4j
public class ClientApplication {
    private static final String TARGET = "localhost:50051";

    public static void main(String[] args) throws InterruptedException {
        ManagedChannel channel = Grpc.newChannelBuilder(TARGET, InsecureChannelCredentials.create())
                .build();

        try (ChatClient boba = new ChatClient(channel, "Boba")) {
            log.debug("Initiated client Boba");
            Thread.sleep(3000);
            try (ChatClient alex = new ChatClient(channel, "Alexandr")) {
                log.debug("Initiated client Alexandr");

                alex.addRoom("Boba", "ECB");
                boba.checkForInitRoomRequests();
                boba.checkCompanionsStatuses();
                Thread.sleep(5000);
                alex.checkForDiffieHellmanNumbers();
            }
            Thread.sleep(20000);
//            log.info("Disconnecting");
//            client.disconnect();
        } catch (Exception e) {
            log.error("Client application -- error happened: ", e);
        } finally {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
/*
2024-05-22 01:42:24,222 DEBUG [grpc-default-executor-0] r.m.s.ChatService.connect: Boba: connected
2024-05-22 01:42:27,267 DEBUG [grpc-default-executor-0] r.m.s.ChatService.connect: Alexandr: connected
2024-05-22 01:42:27,282 DEBUG [grpc-default-executor-2] r.m.s.ChatService.initRoom: Alexandr -> Boba: initiated room creation
2024-05-22 01:42:27,296 DEBUG [grpc-default-executor-2] r.m.s.ChatService$3.onNext: Alexandr -> Boba: passed diffie-hellman number 1
2024-05-22 01:42:27,297 DEBUG [grpc-default-executor-1] r.m.s.ChatService.checkForInitRoomRequest: Boba -> Alexandr: got invited to chat
2024-05-22 01:42:27,302 DEBUG [grpc-default-executor-1] r.m.s.ChatService$3.onNext: Boba -> Alexandr: passed diffie-hellman number 1
2024-05-22 01:42:29,286 DEBUG [grpc-default-executor-0] r.m.s.ChatService$3.onNext: Boba -> Alexandr: passed diffie-hellman number 1
2024-05-22 01:42:29,288 DEBUG [grpc-default-executor-1] r.m.s.ChatService.anyDiffieHellmanNumber: Boba <- Alexandr: got diffie-hellman number 1
2024-05-22 01:42:32,283 DEBUG [grpc-default-executor-1] r.m.s.ChatService$3.onNext: Alexandr -> Boba: passed diffie-hellman number 1
2024-05-22 01:42:32,283 DEBUG [grpc-default-executor-1] r.m.s.ChatService$3.onCompleted: Alexandr: completed passing dh
2024-05-22 01:42:32,284 DEBUG [grpc-default-executor-1] r.m.s.ChatService.anyDiffieHellmanNumber: Alexandr <- Boba: got diffie-hellman number 1
2024-05-22 01:42:34,276 DEBUG [grpc-default-executor-1] r.m.s.ChatService.anyDiffieHellmanNumber: Boba <- Alexandr: got diffie-hellman number 1
* */
}
