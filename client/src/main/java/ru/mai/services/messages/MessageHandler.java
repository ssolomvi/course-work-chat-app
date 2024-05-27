package ru.mai.services.messages;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mai.Login;
import ru.mai.kafka.KafkaMessageHandler;
import ru.mai.kafka.model.MessageDto;
import ru.mai.services.ContextsRepository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class MessageHandler {
    public static final Integer FILE_PAGE_SIZE = 65536;
    private static final Integer FILE_PAGE_SIZE_FOR_ENCRYPTED = (int) (FILE_PAGE_SIZE * 0.1 + FILE_PAGE_SIZE);
    private final ContextsRepository contextsRepository;
    private final KafkaMessageHandler kafkaMessageHandler;

    public MessageHandler(@Autowired ContextsRepository contextsRepository,
                          @Autowired KafkaMessageHandler kafkaMessageHandler) {
        this.contextsRepository = contextsRepository;
        this.kafkaMessageHandler = kafkaMessageHandler;
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
            MessageDto dto = new MessageDto(UUID.randomUUID(), own, "", 1, 0, encrypted);

            kafkaMessageHandler.sendMessage(companion, dto);
            log.debug("Sent byte array to {}", companion);
        } else {
            log.error("Big length!");
            throw new IllegalArgumentException("input byte array length must be less than " + FILE_PAGE_SIZE);
        }
    }

    public void sendFile(String own, String companion, InputStream inputStream, String fileName) {
        int readBytes;

        try {
            long fileSize = Files.size(Path.of(fileName));
            int numberOfPartitions = (int) (fileSize / FILE_PAGE_SIZE + (fileSize % FILE_PAGE_SIZE == 0 ? 0 : 1));
            int currIndex = 0;

            byte[] arr;

            if (fileSize < FILE_PAGE_SIZE) {
                arr = new byte[(int) fileSize];
            } else {
                arr = new byte[FILE_PAGE_SIZE];
            }

            var op = contextsRepository.get(companion);
            if (op.isEmpty()) {
                log.debug("Context for {} not found", companion);
                return;
            }

            var context = op.get();

            UUID id = UUID.randomUUID();

            while ((readBytes = inputStream.read(arr)) != -1) {
                if (readBytes < FILE_PAGE_SIZE) {
                    byte[] tmpArr = new byte[readBytes];
                    System.arraycopy(arr, 0, tmpArr, 0, readBytes);
                    arr = tmpArr;
                }

                byte[] encryptedPart = context.encryptPart(arr, currIndex == 0);

                kafkaMessageHandler.sendMessage(companion, new MessageDto(id, own, fileName, numberOfPartitions, currIndex++, encryptedPart));
                log.debug("Sent file part to {}", companion);
            }
        } catch (IOException e) {
            log.error("I/O exception happened to {} sent by {}", fileName, companion);
        }
    }


    public List<MessageDto> anyMessages(Login login) {
        var op = kafkaMessageHandler.readMessages();
        if (op.isEmpty()) {
            log.trace("No messages for {}", login.getLogin());
            return Collections.emptyList();
        }

        var read = op.get(); // прочитанные
        List<MessageDto> messages = new LinkedList<>();

        for (var message : read) {
            messages.add(message.value());
        }

        return messages;
    }
}
