package ru.mai.services.messages;

import com.vaadin.flow.spring.annotation.SpringComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import ru.mai.Login;
import ru.mai.encryption_context.EncryptionContext;
import ru.mai.kafka.KafkaMessageHandler;
import ru.mai.kafka.model.MessageDto;
import ru.mai.services.repositories.ContextsRepository;
import ru.mai.services.repositories.FilesUnderDownloadRepository;
import ru.mai.utils.Pair;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ru.mai.config.ClientConstants.FILE_PAGE_SIZE;

@Slf4j
@SpringComponent
@Scope("prototype")
public class MessageHandler {
    private static final Integer FILE_PAGE_SIZE_FOR_ENCRYPTED = (int) (FILE_PAGE_SIZE * 0.1 + FILE_PAGE_SIZE);
    private static final String FILE_PREFIX_DOWNLOAD = "downloads" + File.separator;
    private static final String FILE_PREFIX_UPLOAD = "uploads" + File.separator;
    private final ExecutorService fileExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2);
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
        fileUnderDownloadRepository.removeBySender(companion);
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

    public void sendFile(String own, String companion, String filename, InputStream in, long fileSize) throws IOException, InterruptedException {
        if (in.available() == 0) {
            kafkaMessageHandler.sendMessage(companion, new MessageDto(UUID.randomUUID(), own, filename, 1, 0, new byte[0]));
            return;
        }

        var op = contextsRepository.get(companion);
        if (op.isEmpty()) {
            log.debug("Context for {} not found", companion);
            throw new IllegalArgumentException(String.format("Context for companion %s not found", companion));
        }

        var context = op.get();

        int numberOfPartitions = (int) (fileSize / FILE_PAGE_SIZE + (fileSize % FILE_PAGE_SIZE == 0 ? 0 : 1));
        UUID id = UUID.randomUUID();

        File outFile = new File(String.format("%s%s%s", FILE_PREFIX_UPLOAD, System.currentTimeMillis(), filename)); // encrypted file

        try (in) {
            try (FileOutputStream out = new FileOutputStream(outFile)) {
                context.encrypt(in, out);
            }
        }

        long blockOffset = 0;
        int currIndex = 0;
        CountDownLatch countDownLatch = new CountDownLatch(numberOfPartitions);

        final long encryptedFileSize = Files.size(outFile.toPath());

        log.debug("Sending file {} of size {}", filename, Files.size(outFile.toPath()));

        try {
            while (blockOffset < encryptedFileSize) {
                long finalBlockOffset = blockOffset;
                int finalCurrIndex = currIndex;

                fileExecutor.submit(() -> {
                    byte[] buffer;
                    try (RandomAccessFile encIn = new RandomAccessFile(outFile, "r")) {
                        encIn.seek(finalBlockOffset);
                        if (encryptedFileSize - finalBlockOffset < FILE_PAGE_SIZE) {
                            buffer = new byte[(int) (encryptedFileSize - finalBlockOffset)];
                        } else {
                            buffer = new byte[FILE_PAGE_SIZE];
                        }

                        encIn.read(buffer);
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                        throw new RuntimeException(e);
                    }

                    kafkaMessageHandler.sendMessage(companion, new MessageDto(id, own, filename, numberOfPartitions, finalCurrIndex, buffer));
                    countDownLatch.countDown();
                    log.debug("Sent file part {} #{} of size {} to {}", filename, finalCurrIndex, buffer.length, companion);
                });

                ++currIndex;
                blockOffset += FILE_PAGE_SIZE;
            }
        } catch (RuntimeException e) {
            log.debug("Error encrypting and sending file, ", e);
            deleteFileWithLog(outFile);
        }

        countDownLatch.await();
        log.info("Sent file {} to {}", filename, companion);

        deleteFileWithLog(outFile);
    }

    private void deleteFileWithLog(File file) {
        if (file.delete()) {
            log.debug("Deleted file {} successfully", file.getAbsoluteFile());
        } else {
            log.debug("Error deleting file {}", file.getAbsoluteFile());
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
        return Optional.of(decryptedString);
    }

    private String getFilenameWithDownloadDirectory(String fileName) {
        return String.format("%s%s", FILE_PREFIX_DOWNLOAD, fileName);
    }


    /**
     * Writes file part to corresponding tmp file and decrypts it if all parts are received
     *
     * @param msg file part
     * @return empty optional, if not all parts are received; a pair of sender and a pair of filename and input stream to
     * decrytpted file
     * @throws IOException if an I/O error occurs
     */
    public Optional<Pair<String, Pair<String, InputStream>>> processFileMessage(MessageDto msg) throws IOException {
        final UUID messageId = msg.getMessageId();
        final String filename = msg.getFilename();
        final String sender = msg.getSender();

        String downloadFilename = fileUnderDownloadRepository.getTmpFilename(sender, messageId, filename);

        // write part to download file (the part is encrypted)
        try (RandomAccessFile rnd = new RandomAccessFile(downloadFilename, "rw")) {
            rnd.seek((long) msg.getCurrIndex() * FILE_PAGE_SIZE);
            rnd.write(msg.getValue());
        }

        if (fileUnderDownloadRepository.incrementAndGetPartsSent(messageId) != msg.getNumberOfPartitions()) {
            return Optional.empty();
        }

        fileUnderDownloadRepository.remove(messageId);

        File toDecryptFile = new File(downloadFilename);

        var contextOp = contextsRepository.get(sender);
        if (contextOp.isEmpty()) {
            // todo: better throw custom exception
            log.warn("Error trying to decrypt file {} for {} : no encryption context", msg.getFilename(), msg.getSender());
            deleteFileWithLog(toDecryptFile);
            return Optional.empty();
        }

        var context = contextOp.get();

        File decryptedFile = new File(getFilenameWithDownloadDirectory(filename));

        if (Files.size(toDecryptFile.toPath()) == 0) {
            deleteFileWithLog(toDecryptFile);
            return Optional.of(new Pair<>(sender, new Pair<>(filename, new ByteArrayInputStream(new byte[0]))));
        }

        try (FileInputStream in = new FileInputStream(toDecryptFile)) {
            try (FileOutputStream out = new FileOutputStream(decryptedFile)) {
                context.decrypt(in, out);
            }
        }

        deleteFileWithLog(toDecryptFile);

        return Optional.of(new Pair<>(sender, new Pair<>(filename, new FileInputStream(decryptedFile))));
    }

    public void close() {
        contextsRepository.clear();
        kafkaMessageHandler.close();
    }
}
