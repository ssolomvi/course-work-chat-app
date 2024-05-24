package ru.mai.config;

import io.grpc.Channel;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.mai.ChatServiceGrpc;

@Configuration
public class ClientConfig {
    private static final String TARGET = "localhost:50051";

    @Bean
    public Channel channel() {
        return Grpc.newChannelBuilder(TARGET, InsecureChannelCredentials.create()).build();
    }

    @Bean
    public ChatServiceGrpc.ChatServiceBlockingStub blockingStub(@Autowired Channel channel) {
        return ChatServiceGrpc.newBlockingStub(channel);
    }

}
