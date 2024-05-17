package ru.mai;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class Main {
    public static void main(String[] args) {
        try {
            Server server = ServerBuilder.forPort(8080)
                    .build();

            server.start();

            System.out.println("Server started!");

            server.awaitTermination();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}