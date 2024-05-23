package ru.mai.config;

import io.grpc.protobuf.services.HealthStatusManager;
//import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import ru.mai.kafka.KafkaMessageHandler;
import ru.mai.repositories.ActiveUsersAndConsumersRepository;
import ru.mai.repositories.AddRoomRequestsRepository;
import ru.mai.repositories.DeleteRoomRequestsRepository;
import ru.mai.repositories.DiffieHellmanNumbersRepository;
import ru.mai.services.ChatService;

@Configuration
//@ConditionalOnClass(HealthStatusManager.class)
//@ConditionalOnProperty(prefix = "grpc.server", name = "health-service-enabled", matchIfMissing = true)
public class ServerConfig {
//    @Bean
//    public KafkaMessageHandler kafkaMessageHandler(@Autowired ActiveUsersAndConsumersRepository repository) {
//        return new KafkaMessageHandler(repository);
//    }

//    @Bean
//    public ChatService chatService(@Autowired ActiveUsersAndConsumersRepository usersRep,
//                                   @Autowired DeleteRoomRequestsRepository deleteRequestsRep,
//                                   @Autowired AddRoomRequestsRepository addRequestsRep,
//                                   @Autowired DiffieHellmanNumbersRepository numbersRep,
//                                   @Autowired KafkaMessageHandler messageHandler) {
//        return new ChatService(usersRep, deleteRequestsRep, addRequestsRep, numbersRep, messageHandler);
//    }
}
