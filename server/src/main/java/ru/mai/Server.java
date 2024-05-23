package ru.mai;

import io.grpc.ServerBuilder;
//import net.devh.boot.grpc.server.autoconfigure.GrpcHealthServiceAutoConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.mai.services.ChatService;

import java.io.IOException;

//@SpringBootApplication(exclude = GrpcHealthServiceAutoConfiguration.class)
@Slf4j
@SpringBootApplication
public class Server {

    public static void main(String[] args) throws IOException, InterruptedException {
        SpringApplication.run(Server.class, args);

        AnnotationConfigApplicationContext context
                = new AnnotationConfigApplicationContext();

//        context.scan("");
        context.scan("ru.mai");
        context.refresh();

        ChatService chatService = context.getBean(ChatService.class);

        var server = ServerBuilder.forPort(8090).addService(chatService).build().start();

        log.info("Server started at port 8090");

        if (server != null) {
            server.awaitTermination();
        }
    }
}