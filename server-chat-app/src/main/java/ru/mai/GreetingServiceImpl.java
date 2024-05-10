package ru.mai;

import io.grpc.stub.StreamObserver;

public class GreetingServiceImpl extends GreetingServiceGrpc.GreetingServiceImplBase {
    @Override
    public void greeting(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
        System.out.println(request);
        var helloResponse = HelloResponse.newBuilder()
                .setGreeting("Hello from server, " + request.getName())
                .build();

        responseObserver.onNext(helloResponse);

        responseObserver.onCompleted();
    }
}
