package ru.mai.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import ru.mai.client.room.EncryptionContextBuilderOfInitRoomResponse;
import ru.mai.db.repositories.ChatMetadataDbRepository;
import ru.mai.db.service.ChatMetadataDbService;

@SpringBootConfiguration
public class ClientConfig {
//    @Bean
//    public EncryptionContextBuilderOfInitRoomResponse encryptionContextBuilderOfInitRoomResponse(
//            @Autowired ChatMetadataDbService service
//            ) {
//        return new EncryptionContextBuilderOfInitRoomResponse(service);
//    }
}
