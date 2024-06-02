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

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@SpringComponent
@Scope("prototype")
public class MessageHandler {
    public static final Integer FILE_PAGE_SIZE = 2 << 18; // 1/4 MB
    private static final Integer FILE_PAGE_SIZE_FOR_ENCRYPTED = (int) (FILE_PAGE_SIZE * 0.1 + FILE_PAGE_SIZE);
    private static final String FILE_PREFIX_DOWNLOAD = "downloads" + File.separator;
    private static final String FILE_PREFIX_UPLOAD = "uploads" + File.separator;
    private final ExecutorService fileWriterExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
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

    public void sendFile(String own, String companion, String filename, InputStream in, long fileSize) throws IOException {
        var op = contextsRepository.get(companion);
        if (op.isEmpty()) {
            log.debug("Context for {} not found", companion);
            throw new IllegalArgumentException(String.format("Context for companion %s not found", companion));
        }

        var context = op.get();

        int numberOfPartitions = (int) (fileSize / FILE_PAGE_SIZE + (fileSize % FILE_PAGE_SIZE == 0 ? 0 : 1));
        UUID id = UUID.randomUUID();

        File outFile = new File(String.format("%s%s%s", FILE_PREFIX_UPLOAD, LocalDateTime.now().format(formatter), filename)); // encrypted file

        try (in) {
            try (FileOutputStream out = new FileOutputStream(outFile)) {
                context.encrypt(in, out);
            }
        }

        try (RandomAccessFile encIn = new RandomAccessFile(outFile, "r")) {
            byte[] buffer;

            if (encIn.length() < FILE_PAGE_SIZE) {
                buffer = new byte[(int) encIn.length()];
            } else {
                buffer = new byte[FILE_PAGE_SIZE];
            }

            if (buffer.length != 0) {
                int readBytes;
                int currIndex = 0;

                while ((readBytes = encIn.read(buffer)) != -1) {
                    if (readBytes != FILE_PAGE_SIZE) {
                        byte[] tmp = new byte[readBytes];
                        System.arraycopy(buffer, 0, tmp, 0, readBytes);
                        buffer = tmp;
                    }

                    kafkaMessageHandler.sendMessage(companion, new MessageDto(id, own, filename, numberOfPartitions, currIndex++, buffer));
                    log.debug("Sent file part #{} to {}", currIndex - 1, companion);
                }
            } else {
                kafkaMessageHandler.sendMessage(companion, new MessageDto(id, own, filename, 1, 0, buffer));
            }
        }

        if (outFile.delete()) {
            log.debug("Deleted tmp file successfully");
        } else {
            log.debug("Error deleting tmp file");
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
            if (!message.value().getFilename().isEmpty()) {
                log.debug("Got message: sender {} : filename {} : partitions {} : current {}", message.value().getSender(), message.value().getFilename(),
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

    private String getFilenameWithDownloadDirectory(String fileName) {
        return String.format("%s%s", FILE_PREFIX_DOWNLOAD, fileName);
    }

    public Optional<Pair<String, InputStream>> processFileMessage(MessageDto msg) throws IOException {
        final UUID messageId = msg.getMessageId();
        final String filename = msg.getFilename();
        final String sender = msg.getSender();

        final String downloadFilename;
        Optional<String> downloadFilenameOp = fileUnderDownloadRepository.getTmpFilename(messageId);
        if (downloadFilenameOp.isEmpty()) {
            String tmpFilename = String.format("%s%s", filename, LocalDateTime.now().format(formatter));
            downloadFilename = getFilenameWithDownloadDirectory(tmpFilename);
            fileUnderDownloadRepository.put(messageId, filename, downloadFilename, sender, msg.getNumberOfPartitions());
        } else {
            downloadFilename = downloadFilenameOp.get();
        }

        // write part to download file (the part is encrypted)
        try (RandomAccessFile rnd = new RandomAccessFile(downloadFilename, "rw")) {
            rnd.seek((long) msg.getCurrIndex() * FILE_PAGE_SIZE);
            rnd.write(msg.getValue());
        }

        fileUnderDownloadRepository.decrementPartitionsLeft(messageId);

        if (!fileUnderDownloadRepository.isFinished(messageId)) {
            return Optional.empty();
        }

        var contextOp = contextsRepository.get(sender);
        if (contextOp.isEmpty()) {
            // todo: better throw custom exception
            log.warn("Error trying to decrypt file {} for {} : no encryption context", msg.getFilename(), msg.getSender());
            return Optional.empty();
        }

        var context = contextOp.get();

        File toDecryptFile = new File(downloadFilename);
        File decryptedFile = new File(getFilenameWithDownloadDirectory(filename));

        try (FileInputStream in = new FileInputStream(toDecryptFile)) {
            try (FileOutputStream out = new FileOutputStream(decryptedFile)) {
                context.decrypt(in, out);
            }
        }

        if (toDecryptFile.delete()) {
            log.debug("Deleted tmp file successfully");
        } else {
            log.warn("Failed to delete tmp file");
        }

        return Optional.of(new Pair<>(filename, new FileInputStream(decryptedFile)));
    }

    public void close() {
        contextsRepository.clear();
        kafkaMessageHandler.close();
    }
}
