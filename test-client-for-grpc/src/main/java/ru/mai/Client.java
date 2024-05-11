package ru.mai;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class Client {
    public static void main(String[] args) {
        try {
            var channel = ManagedChannelBuilder.forTarget("localhost:8080")
                    .usePlaintext().build();

            channel.shutdownNow();
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }
}
