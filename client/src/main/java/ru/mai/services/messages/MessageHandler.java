package ru.mai.services.messages;

import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mai.*;
import ru.mai.db.model.MessageEntity;
import ru.mai.db.repositories.MessageEntityRepository;
import ru.mai.model.MessageDto;
import ru.mai.services.ContextsRepository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Slf4j
@Component
public class MessageHandler {
    public static final Integer FILE_PAGE_SIZE = 65536;
    private static final Integer FILE_PAGE_SIZE_FOR_ENCRYPTED = (int) (FILE_PAGE_SIZE * 0.1 + FILE_PAGE_SIZE);
    private final ChatServiceGrpc.ChatServiceBlockingStub blockingStub;
    private final ContextsRepository contextsRepository;
    private final MessageEntityRepository messageRepository;

    public MessageHandler(@Autowired ChatServiceGrpc.ChatServiceBlockingStub blockingStub,
                          @Autowired ContextsRepository contextsRepository,
                          @Autowired MessageEntityRepository messageRepository) {
        this.blockingStub = blockingStub;
        this.contextsRepository = contextsRepository;
        this.messageRepository = messageRepository;
    }

    public void sendMessage(MessageToCompanion message) {
        try {
            if (blockingStub.sendMessage(message).getEnumStatus().equals(EnumStatus.ENUM_STATUS_OK)) {
                log.debug("Sent message to {}", message.getCompanionLogin());
            } else {
                log.debug("Error sending message to {}", message.getCompanionLogin());
            }

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
            MessageToCompanion data = MessageToCompanion.newBuilder()
                    .setCompanionLogin(companion)
                    .setUuid(UUID.randomUUID().toString())
                    .setSender(own)
                    .setFilename("")
                    .setPartitions(1)
                    .setCurrIndex(0)
                    .setValue(ByteString.copyFrom(encrypted))
                    .build();

            try {
                Status sendStatus = blockingStub.sendMessage(data);
                if (sendStatus.getEnumStatus().equals(EnumStatus.ENUM_STATUS_OK)) {
                    // save to db
                    messageRepository.save(new MessageEntity(
                            UUID.fromString(data.getUuid()),
                            data.getSender(),
                            data.getFilename(), // must be == ""
                            new String(arr),
                            false));

                    log.debug("Sent byte array to {}", companion);
                } else {
                    log.warn("Error sending simple text message to {}", companion);
                }
            } catch (StatusRuntimeException e) {
                log.debug("sendMessage to {}: Error occurred, cause:, ", companion, e);
            }
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
            String idStr = id.toString();

            while ((readBytes = inputStream.read(arr)) != -1) {
                if (readBytes < FILE_PAGE_SIZE) {
                    byte[] tmpArr = new byte[readBytes];
                    System.arraycopy(arr, 0, tmpArr, 0, readBytes);
                    arr = tmpArr;
                }

                byte[] encryptedPart = context.encryptPart(arr, currIndex == 0);

                Status response = blockingStub.sendMessage(MessageToCompanion.newBuilder()
                        .setCompanionLogin(companion)
                        .setUuid(idStr)
                        .setSender(own)
                        .setFilename(fileName)
                        .setPartitions(numberOfPartitions)
                        .setCurrIndex(currIndex++)
                        .setValue(ByteString.copyFrom(encryptedPart))
                        .build());

                if (response.getEnumStatus().equals(EnumStatus.ENUM_STATUS_OK)) {
                    log.debug("Successfully sent file part to {}", companion);
                } else {
                    log.warn("Error sending file part to {}", companion);
                    return;
                }

                // save to db
                messageRepository.save(new MessageEntity(
                        id,
                        own,
                        fileName,
                        "",
                        true));
            }
        } catch (IOException e) {
            log.error("I/O exception happened to {} sent by {}", fileName, companion);
        }
    }


    public List<MessageDto> anyMessages(Login login) {
        Iterator<MessageToCompanion> response;
        try {
            response = blockingStub.anyMessages(login);
        } catch (StatusRuntimeException e) {
            log.error("{}: passDiffieHellmanNumber: Error happened, cause: ", login.getLogin(), e);
            return Collections.emptyList();
        }

        if (!response.hasNext()) {
            return Collections.emptyList();
        }

        List<MessageDto> messages = new LinkedList<>();
        MessageToCompanion dummy = MessageToCompanion.getDefaultInstance();

        while (response.hasNext()) {
            var message = response.next();
            if (message.equals(dummy)) {
                continue;
            }

            messages.add(new MessageDto(UUID.fromString(message.getUuid()), message.getSender(), message.getFilename(), message.getPartitions(), message.getCurrIndex(), message.getValue().toByteArray()));
        }
        return messages;
    }
}
