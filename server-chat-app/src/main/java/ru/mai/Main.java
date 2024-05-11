package ru.mai;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        try {
            Server server = ServerBuilder.forPort(8080)
//                    .addService(new GreetingServiceImpl())
                    .build();

            server.start();

            System.out.println("Server started!");

            server.awaitTermination();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}