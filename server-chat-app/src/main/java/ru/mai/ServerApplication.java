package ru.mai;

import io.grpc.*;
import ru.mai.service.ChatService;

import java.util.concurrent.TimeUnit;

public class ServerApplication {
    public static void main(String[] args) {
        try {
            Server server= Grpc.newServerBuilderForPort(8080, InsecureServerCredentials.create())
                    .addService(new ChatService())
                    .build()
                    .start();
            System.out.printf("Server started on port %d!%n", server.getPort());

//            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
//                System.err.println("*** shutting down gRPC server since JVM is shutting down");
//                try {
//                    if (server != null) {
//                        server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
//                    }
//                } catch (InterruptedException e) {
//                    e.printStackTrace(System.err);
//                }
//                System.err.println("*** server shut down");
//            }));

            server.awaitTermination();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}