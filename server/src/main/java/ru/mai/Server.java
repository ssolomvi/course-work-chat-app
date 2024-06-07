package ru.mai;

import io.grpc.ServerBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.mai.services.ChatService;

@Slf4j
@SpringBootApplication
public class Server implements CommandLineRunner {

    @Autowired
    ChatService chatService;

    public static void main(String[] args) {
        SpringApplication.run(Server.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        var server = ServerBuilder.forPort(8090).addService(chatService).build().start();

        log.info("Server started at port 8090");

        if (server != null) {
            server.awaitTermination();
        }
    }
}