package ru.mai.services;

import org.springframework.stereotype.Repository;
import ru.mai.encryption_context.EncryptionContext;
import ru.mai.services.chatroom.ContextBuilder;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ContextsRepository {
    /**
     * Encryption context for communication with key=companion
     */
    private final Map<String, EncryptionContext> contexts = new ConcurrentHashMap<>();

    public void put(Map<String, EncryptionContext> toPut) {
        contexts.putAll(toPut);
    }

    public void put(String companion, EncryptionContext context) {
        contexts.put(companion, context);
    }

    public void put(String companion, String encryptionMode, String paddingMode, String algorithm, byte[] initVector, byte[] key) {
        EncryptionContext context = ContextBuilder.createEncryptionContext(encryptionMode, paddingMode, algorithm, initVector, key);
        put(companion, context);
    }

    public boolean contains(String companion) {
        return contexts.containsKey(companion);
    }

    public Optional<EncryptionContext> get(String companion) {
        return Optional.ofNullable(contexts.get(companion));
    }

    public void remove(String companion) {
        contexts.remove(companion);
    }

}
