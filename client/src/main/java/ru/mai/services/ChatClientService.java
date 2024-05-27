package ru.mai.services;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mai.Login;
import ru.mai.db.model.MessageEntity;
import ru.mai.db.repositories.ChatMetadataEntityRepository;
import ru.mai.db.repositories.MessageEntityRepository;
import ru.mai.encryption_context.EncryptionContext;
import ru.mai.kafka.model.MessageDto;
import ru.mai.services.chatroom.ChatRoomHandler;
import ru.mai.services.connections.ConnectionHandler;
import ru.mai.services.messages.MessageHandler;
import ru.mai.services.repositories.CompanionStatusesRepository;
import ru.mai.services.repositories.FilesUnderDownloadRepository;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static ru.mai.services.messages.MessageHandler.FILE_PAGE_SIZE;

@Slf4j
@Component
public class ChatClientService {
    private final ScheduledExecutorService scheduled = Executors.newSingleThreadScheduledExecutor();
    private final ConnectionHandler connectionHandler;
    private final ChatRoomHandler chatRoomHandler;
    private final MessageHandler messageHandler;
    private final CompanionStatusesRepository companionStatusesRepository;
    private final ContextsRepository contextsRepository;
    private final FilesUnderDownloadRepository fileUnderDownloadRepository;
    private final ChatMetadataEntityRepository metadataEntityRepository;
    private final MessageEntityRepository messageRepository;
    @Getter
    private final Set<String> companionsLogins = ConcurrentHashMap.newKeySet();
    private String login;
    private Login loginStructure;
    private BigInteger dhG;
    private int checkForDiffieHellmanNumbers = 0;

    public ChatClientService(@Autowired ConnectionHandler connectionHandler,
                             @Autowired ChatRoomHandler chatRoomHandler,
                             @Autowired MessageHandler messageHandler,
                             @Autowired CompanionStatusesRepository companionStatusesRepository,
                             @Autowired ContextsRepository contextsRepository,
                             @Autowired FilesUnderDownloadRepository fileUnderDownloadRepository,
                             @Autowired ChatMetadataEntityRepository metadataEntityRepository,
                             @Autowired MessageEntityRepository messageRepository) {
        this.connectionHandler = connectionHandler;
        this.chatRoomHandler = chatRoomHandler;
        this.messageHandler = messageHandler;
        this.companionStatusesRepository = companionStatusesRepository;
        this.contextsRepository = contextsRepository;
        this.fileUnderDownloadRepository = fileUnderDownloadRepository;
        this.metadataEntityRepository = metadataEntityRepository;
        this.messageRepository = messageRepository;
        pingServer();
    }

    public void setLogin(String login) {
        this.login = login;
        this.loginStructure = Login.newBuilder().setLogin(login).build();
    }

    private void pingServer() {
        log.debug("{}: pinging server", login);
        scheduled.scheduleAtFixedRate(
                () -> {
                    checkForInitRoomRequests();
                    checkForDeleteRoomRequest();
                    if (checkForDiffieHellmanNumbers != 0) {
                        checkForDiffieHellmanNumbers();
                    }
                },
                0, 5, TimeUnit.SECONDS
        );
    }

    public void connect() {
        this.dhG = connectionHandler.connect(loginStructure);
    }

    public void disconnect() {
        connectionHandler.disconnect(loginStructure);
    }

    public boolean checkCompanionStatus(String companion) {
        return companionStatusesRepository.get(companion);
    }

    public boolean addRoom(String companion, String algorithm, String encryptionMode, String paddingMode) {
        if (chatRoomHandler.initRoom(login, companion, algorithm, encryptionMode, paddingMode)) {
            passDiffieHellmanNumber(companion);
            return true;
        }
        return false;
    }

    private void checkForInitRoomRequests() {
        var companions = chatRoomHandler.checkForInitRoomRequests(loginStructure);

        if (companions.isEmpty()) {
            return;
        }

        for (String companion : companions) {
            passDiffieHellmanNumber(companion);
        }
    }

    private void passDiffieHellmanNumber(String companion) {
        chatRoomHandler.passDiffieHellmanNumber(login, companion, dhG);
        ++checkForDiffieHellmanNumbers;
    }

    public void checkForDiffieHellmanNumbers() {
        var response = chatRoomHandler.anyDiffieHellmanNumbers(loginStructure);
        if (!response.isEmpty()) {
            checkForDiffieHellmanNumbers -= response.size();

            contextsRepository.put(response);
            companionsLogins.addAll(response.keySet());
            // todo: make a new chat rooms (ui)
        }
    }

    private void deleteRoomUtil(String companion) {
        contextsRepository.remove(companion);
        // todo: send to ui request for room deletion

        metadataEntityRepository.deleteById(companion);
        messageRepository.deleteAllByCompanion(companion);
        companionsLogins.remove(companion);
    }


    /**
     * Invokes room deletion
     *
     * @param companion companion login
     * @return {@code true} if room was deleted, {@code false} otherwise (companion is not online or companion already requested deletion)
     */
    public boolean deleteRoom(String companion) {
        var response = chatRoomHandler.deleteRoom(login, companion);
        deleteRoomUtil(companion);
        return response;
    }


    public void checkForDeleteRoomRequest() {
        var response = chatRoomHandler.checkForDeleteRoomRequests(loginStructure);

        if (response.isEmpty()) {
            return;
        }

        for (String companion : response) {
            deleteRoomUtil(companion);
        }
    }

    public void sendMessage(String companion, String message) {
        // todo: byte array in message must not be > 2^13
        messageHandler.sendByteArray(login, companion, message.getBytes(StandardCharsets.UTF_8));
    }

    public void sendFile(String companion, String filename, InputStream inputStream) {
        messageHandler.sendFile(login, companion, inputStream, filename);
    }

    public void checkForMessages() {
        var response = messageHandler.anyMessages(loginStructure);

        if (response.isEmpty()) {
            return;
        }

        for (var msg : response) {
            if (msg.getFileName().isEmpty()) {
                // it is a byte arr (not file)
                // if it is a text, print message to chat
                processByteArrayMessage(msg);
            } else {
                // if it is a file, print message to chat that file (filename) may be found at location (location)
                processFileMessage(msg);
            }
        }
    }


    private void processByteArrayMessage(MessageDto msg) {
        UUID id = msg.getMessageId();
        String sender = msg.getSender();

        Optional<EncryptionContext> op = contextsRepository.get(sender);

        if (op.isEmpty()) {
            log.warn("No encryption context for {}", sender);
            return;
        }

        EncryptionContext context = op.get();

        byte[] decrypted = context.decrypt(msg.getValue());
        // todo: depict message after decryption

        messageRepository.save(new MessageEntity(id, sender, msg.getFileName(), new String(decrypted), false));
    }


    private void processFileMessage(MessageDto msg) {
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
                    return;
                }
                fileUnderDownloadRepository.remove(msg.getMessageId());
                EncryptionContext context = op.get();
                Path tmpPath = Path.of("TMP.txt");
                context.decrypt(Path.of(msg.getFileName()), tmpPath);

                if (tmpPath.toFile().renameTo(Path.of(msg.getFileName()).toFile())) {
                    log.info("Successfully decrypted file {} for {}", msg.getFileName(), msg.getSender());
                    // todo: depict with ui
                    messageRepository.save(new MessageEntity(msg.getMessageId(),
                            msg.getSender(),
                            msg.getFileName(),
                            "",
                            true));
                } else {
                    log.warn("Decrypting file {} for {} unsuccessful", msg.getFileName(), msg.getSender());
                }
            }

        } catch (IOException e) {
            log.error("I/O error while writing to file {}: ", msg.getFileName(), e);
        }
    }

}
