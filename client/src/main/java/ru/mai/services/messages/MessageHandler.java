package ru.mai.services.messages;

import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mai.ChatServiceGrpc;
import ru.mai.MessageToCompanion;
import ru.mai.model.KafkaMessage;
import ru.mai.services.ContextsRepository;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Component
public class MessageHandler {
    public static final Integer FILE_PAGE_SIZE = 4092;
    private static final Integer FILE_PAGE_SIZE_FOR_ENCRYPTED = (int) (FILE_PAGE_SIZE * 0.1 + FILE_PAGE_SIZE);
    private final ChatServiceGrpc.ChatServiceBlockingStub blockingStub;
    private final ContextsRepository contextsRepository;

    public MessageHandler(@Autowired ChatServiceGrpc.ChatServiceBlockingStub blockingStub,
                          @Autowired ContextsRepository contextsRepository) {
        this.blockingStub = blockingStub;
        this.contextsRepository = contextsRepository;
    }

    public void sendMessage(MessageToCompanion message) {
        try {
            blockingStub.sendMessage(message);
            log.debug("Sent message to {}", message.getCompanionLogin());
        } catch (StatusRuntimeException e) {
            log.debug("sendMessage to {}: Error occurred, cause:, ", message.getCompanionLogin(), e);
        }
    }

    public void sendByteArray(String own, String companion, byte[] arr) {
        var op = contextsRepository.get(companion);
        if (op.isEmpty()) {
            log.debug("Context for {} not found", companion);
            return;
        }

        var context = op.get();
        byte[] encrypted = context.encrypt(arr);

        if (encrypted.length <= FILE_PAGE_SIZE_FOR_ENCRYPTED) {
            KafkaMessage message = new KafkaMessage(UUID.randomUUID(), own, "", 1, 0, encrypted);
            MessageToCompanion data = MessageToCompanion.newBuilder()
                    .setCompanionLogin(companion)
                            .setKafkaMessage(message.toString())
                                    .build();

            try {
                blockingStub.sendMessage(data);
                log.debug("Sent byte array to {}", companion);
            } catch (StatusRuntimeException e) {
                log.debug("sendMessage to {}: Error occurred, cause:, ", companion, e);
            }
        } else {
            log.error("Big length!");
            throw new IllegalArgumentException("input byte array length must be less than " + FILE_PAGE_SIZE);
        }
    }

    public void sendFile(String companion, InputStream inputStream, String fileName) {
        byte[] arr = new byte[FILE_PAGE_SIZE];
        try {
            inputStream.read(arr);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
