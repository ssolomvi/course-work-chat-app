//package ru.mai;
//
//import com.google.protobuf.Empty;
//import io.grpc.ManagedChannel;
//import io.grpc.ManagedChannelBuilder;
//import io.grpc.netty.NettyChannelBuilder;
//import io.grpc.stub.StreamObserver;
//
//public class SubscriberClient {
//
//    public static void main(String[] args) throws Exception {
//        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8080).usePlaintext().build();
//        PublisherGrpc.PublisherStub stub = PublisherGrpc.newStub(channel);
//
//        SubsribeService.Subscription subscription = SubsribeService.Subscription.newBuilder().setTopic("1").build(); // Empty subscription for all messages
//
//        StreamObserver<SubsribeService.Message> responseObserver = new StreamObserver<>() {
//            @Override
//            public void onNext(SubsribeService.Message message) {
//                System.out.println("Received message: " + message.getContent());
//            }
//
////            Error subscribing: UNAVAILABLE: io exception
////            Channel Pipeline: [SslHandler#0, ProtocolNegotiators$ClientTlsHandler#0, WriteBufferingAndExceptionHandler#0, DefaultChannelPipeline$TailContext#0]
//
//            @Override
//            public void onError(Throwable throwable) {
//                System.err.println("Error subscribing: " + throwable.getMessage());
//            }
//
//            @Override
//            public void onCompleted() {
//                System.out.println("Subscription established.");
//            }
//        };
//
//        stub.subscribe(subscription, responseObserver);
//        stub.publishMessage(SubsribeService.Message.newBuilder().setContent("ABOBA").build(), new StreamObserver<Empty>() {
//            @Override
//            public void onNext(Empty value) {
//                //
//            }
//
//            @Override
//            public void onError(Throwable t) {
//                System.err.println("WHAT: " + t.getMessage());
////
//            }
//
//            @Override
//            public void onCompleted() {
////
//            }
//        });
////        server.publish(SubsribeService.Message.newBuilder().setContent("ABOBA").build());
//
//        // Keep the application running to receive messages
//        Thread.sleep(Long.MAX_VALUE);
//    }
//}
