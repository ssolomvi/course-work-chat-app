package ru.mai.services.messages;

import com.vaadin.flow.spring.annotation.SpringComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import ru.mai.Login;
import ru.mai.encryption_context.EncryptionContext;
import ru.mai.kafka.KafkaMessageHandler;
import ru.mai.kafka.model.MessageDto;
import ru.mai.services.ContextsRepository;
import ru.mai.services.repositories.FilesUnderDownloadRepository;
import ru.mai.utils.Pair;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Slf4j
@SpringComponent
@Scope("prototype")
public class MessageHandler {
    public static final Integer FILE_PAGE_SIZE = 65536;
    private static final Integer FILE_PAGE_SIZE_FOR_ENCRYPTED = (int) (FILE_PAGE_SIZE * 0.1 + FILE_PAGE_SIZE);
    private final ContextsRepository contextsRepository;
    private final FilesUnderDownloadRepository fileUnderDownloadRepository;
    private final KafkaMessageHandler kafkaMessageHandler;

    public MessageHandler(@Autowired ContextsRepository contextsRepository,
                          @Autowired FilesUnderDownloadRepository fileUnderDownloadRepository,
                          @Autowired KafkaMessageHandler kafkaMessageHandler) {
        this.contextsRepository = contextsRepository;
        this.fileUnderDownloadRepository = fileUnderDownloadRepository;
        this.kafkaMessageHandler = kafkaMessageHandler;
    }

    public void init(String login) {
        kafkaMessageHandler.init(login);
    }

    public void addAllContexts(Map<String, EncryptionContext> companionsAndContexts) {
        for (var companionAndContext : companionsAndContexts.entrySet()) {
            contextsRepository.put(companionAndContext.getKey(), companionAndContext.getValue());
            log.debug("Got context {} for {}", companionAndContext.getValue(), companionAndContext.getKey());
        }
    }

    public void remove(String companion) {
        contextsRepository.remove(companion);
        fileUnderDownloadRepository.remove(companion);
    }

    public void sendByteArray(String own, String companion, byte[] arr) {
        var op = contextsRepository.get(companion);
        if (op.isEmpty()) {
            log.debug("Context for {} not found", companion);
            throw new RuntimeException(String.format("Context for companion %s not found", companion));
        }

        var context = op.get();
        byte[] encrypted = context.encrypt(arr);

        if (encrypted.length <= FILE_PAGE_SIZE_FOR_ENCRYPTED) {
            MessageDto dto = new MessageDto(UUID.randomUUID(), own, "", 1, 0, encrypted);

            log.debug("Sending text msg {} -> {}: {}", own, companion, dto);
            kafkaMessageHandler.sendMessage(companion, dto);
        } else {
            log.error("Big length!");
            throw new IllegalArgumentException("input byte array length must be less than " + FILE_PAGE_SIZE);
        }
    }

    public void sendFile(String own, String companion, InputStream inputStream, String fileName) {
        var op = contextsRepository.get(companion);
        if (op.isEmpty()) {
            log.debug("Context for {} not found", companion);
            throw new RuntimeException(String.format("Context for companion %s not found", companion));
        }

        var context = op.get();

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
            log.debug("No messages for {}", login.getLogin());
            return Collections.emptyList();
        }

        var read = op.get(); // прочитанные
        List<MessageDto> messages = new LinkedList<>();

        for (var message : read) {
            messages.add(message.value());
        }

        return messages;
    }

    public Optional<String> processByteArrayMessage(MessageDto msg) {
        String sender = msg.getSender();

        Optional<EncryptionContext> op = contextsRepository.get(sender);

        if (op.isEmpty()) {
            log.warn("No encryption context for {}", sender);
            return Optional.empty();
        }

        EncryptionContext context = op.get();

        byte[] decrypted = context.decrypt(msg.getValue());
        String decryptedString = new String(decrypted);
        log.debug("GOT MESSAGE: {} from {}", decryptedString, sender);
        return Optional.of(decryptedString);
    }

    public Optional<Pair<String, InputStream>> processFileMessage(MessageDto msg) {
        // decrypt part to
        try (RandomAccessFile rnd = new RandomAccessFile(msg.getFileName(), "rw")) {
            log.info("File {} opened as RandomAccessFile", msg.getFileName());
            byte[] toWrite = msg.getValue();
            rnd.seek((long) msg.getCurrIndex() * FILE_PAGE_SIZE);
            rnd.write(toWrite);

            if (fileUnderDownloadRepository.contains(msg.getMessageId())) {
                fileUnderDownloadRepository.decrementPartitionsLeft(msg.getMessageId());
            } else {
                fileUnderDownloadRepository.put(msg.getMessageId(), msg.getFileName(), msg.getSender(), msg.getNumberOfPartitions() - 1);
            }

            if (fileUnderDownloadRepository.isFinished(msg.getMessageId())) {
                var op = contextsRepository.get(msg.getSender());
                if (op.isEmpty()) {
                    log.warn("Error trying to decrypt file {} for {} : no encryption context", msg.getFileName(), msg.getSender());
                    return Optional.empty();
                }
                fileUnderDownloadRepository.remove(msg.getMessageId());

                String fileName = msg.getFileName();

                EncryptionContext context = op.get();
                Path tmpPath = Path.of("TMP.txt");
                Path fileNamePath = Path.of(fileName);
                context.decrypt(fileNamePath, tmpPath);

                if (tmpPath.toFile().renameTo(fileNamePath.toFile())) {
                    log.info("Successfully decrypted file {} for {}", fileName, msg.getSender());
                    return Optional.of(new Pair<>(fileName, new FileInputStream(fileNamePath.toFile())));
                } else {
                    log.warn("Decrypting file {} for {} unsuccessful", msg.getFileName(), msg.getSender());
                    Files.delete(tmpPath);
                }
            }

        } catch (IOException e) {
            log.error("I/O error while writing to file {}: ", msg.getFileName(), e);
        }
        return Optional.empty();
    }

    public void close() {
        contextsRepository.clear();
        kafkaMessageHandler.close();
    }
}
