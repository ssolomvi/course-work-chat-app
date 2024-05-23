package ru.mai.services;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ChatServiceServer {
    @Autowired
    private ChatService chatService;

    private int port = 50051;
    private final Server server;

    public ChatServiceServer() {
        this.server = Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create())
                .addService(chatService)
                .build();
    }

    /**
     * Create a server on certain port using Grpc.newServerBuilderForPort as base
     **/
    public ChatServiceServer(int port) {
        this.port = port;
        this.server = Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create())
                .addService(chatService)
                .build();
    }

    /**
     * Start serving requests.
     */
    public void start() throws IOException {
        server.start();
        log.info("Server started, listening on " + port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            try {
                ChatServiceServer.this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
            System.err.println("*** server shut down");
        }));
    }

    /**
     * Stop serving requests and shutdown resources.
     */
    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
}
