package ru.mai;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class Client {
    public static void main(String[] args) {
        try {
            ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:8080")
                    .usePlaintext().build();

            GreetingServiceGrpc.GreetingServiceBlockingStub stub = GreetingServiceGrpc.newBlockingStub(channel);

            HelloRequest request = HelloRequest.newBuilder()
                    .setName("Aboba")
                    .addHobbies("chilling")
                    .build();

            HelloResponse response = stub.greeting(request);

            System.out.println(response);

            channel.shutdownNow();
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }
}
