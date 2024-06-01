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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

@Slf4j
@SpringComponent
@Scope("prototype")
public class MessageHandler {
    public static final Integer FILE_PAGE_SIZE = 65536;
    private static final Integer FILE_PAGE_SIZE_FOR_ENCRYPTED = (int) (FILE_PAGE_SIZE * 0.1 + FILE_PAGE_SIZE);
    private static final String FILE_PREFIX = "upload" + File.separator;
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

            kafkaMessageHandler.sendMessage(companion, dto);
        } else {
            log.error("Big length!");
            throw new IllegalArgumentException("input byte array length must be less than " + FILE_PAGE_SIZE);
        }
    }

    public void sendFile(String own, String companion, String filename, byte[] bytes) {
        var op = contextsRepository.get(companion);
        if (op.isEmpty()) {
            log.debug("Context for {} not found", companion);
            throw new IllegalArgumentException(String.format("Context for companion %s not found", companion));
        }

        var context = op.get();

        int fileSize = bytes.length;

        int numberOfPartitions = (fileSize / FILE_PAGE_SIZE + (fileSize % FILE_PAGE_SIZE == 0 ? 0 : 1));

        byte[] encrypted = context.encrypt(bytes);
        log.debug("Sending file of length {} bytes", encrypted.length);
        byte[] arr;

        UUID id = UUID.randomUUID();

        for (int currIndex = 0; currIndex < numberOfPartitions; currIndex++) {
            int leftoverSize = encrypted.length - currIndex * FILE_PAGE_SIZE;
            if (leftoverSize >= FILE_PAGE_SIZE) {
                arr = new byte[FILE_PAGE_SIZE];
                System.arraycopy(encrypted, currIndex * FILE_PAGE_SIZE, arr, 0, FILE_PAGE_SIZE);
            } else if (leftoverSize != 0){
                arr = new byte[leftoverSize];
                System.arraycopy(encrypted, currIndex * FILE_PAGE_SIZE, arr, 0, leftoverSize);
            } else {
                break;
            }
            log.debug("Sent file part to {}", companion);
            kafkaMessageHandler.sendMessage(companion ,new MessageDto(id, own, filename, numberOfPartitions, currIndex, arr));
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
            if (!message.value().getFileName().isEmpty()) {
                log.debug("GOT MESSAGE: {} : {} : {} : {}", message.value().getSender(), message.value().getFileName(),
                        message.value().getNumberOfPartitions(), message.value().getCurrIndex());
            }
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
        return Optional.of(decryptedString);
    }

    private String getFileName(String fileName) {
        return String.format("%s%s", FILE_PREFIX, fileName);
    }

    public Optional<Pair<String, byte[]>> processFileMessage(MessageDto msg) {
        final String fileName = getFileName(msg.getFileName());

        // decrypt part to
        try (RandomAccessFile rnd = new RandomAccessFile(fileName, "rw")) {
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

                EncryptionContext context = op.get();

                byte[] encrypted = readFromRnd(rnd, msg.getNumberOfPartitions());
                log.debug("Got encrypted file of length {} bytes", encrypted.length);

                byte[] decrypted = context.decrypt(encrypted);

                log.debug("Decrypted file {} for {}", msg.getFileName(), msg.getSender());
                return Optional.of(new Pair<>(msg.getFileName(), decrypted));
            }

        } catch (IOException e) {
            log.error("I/O error while writing to file {}: ", msg.getFileName(), e);
        } catch (Exception e) {
            log.error("Exception happened {}: ", msg.getFileName(), e);
        }
        return Optional.empty();
    }

    private byte[] readFromRnd(RandomAccessFile rnd, int numberOfPartitions) throws IOException {
        rnd.seek(0);

        int readBytes;

        byte[] result = new byte[0];
        byte[] part = new byte[FILE_PAGE_SIZE];

        for (int currPosition = 0; currPosition < numberOfPartitions; currPosition++) {
            readBytes = rnd.read(part, 0, FILE_PAGE_SIZE);
            if (readBytes != FILE_PAGE_SIZE) {
                byte[] tmp = new byte[readBytes];
                System.arraycopy(part, 0, tmp, 0, readBytes);
                part = tmp;
            }

            int totalLength = result.length + readBytes;
            byte[] tmp = new byte[totalLength];
            System.arraycopy(result, 0, tmp, 0, result.length);
            System.arraycopy(part, 0, tmp, currPosition * FILE_PAGE_SIZE, part.length);

            result = tmp;
        }
        return result;
    }

    public void close() {
        contextsRepository.clear();
        kafkaMessageHandler.close();
    }
}
