package ru.mai.services;

import org.springframework.stereotype.Repository;
import ru.mai.encryption_context.EncryptionContext;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ContextsRepository {
    private final Map<String, EncryptionContext> contexts = new ConcurrentHashMap<>();

    public void put(Map<String, EncryptionContext> toPut) {
        contexts.putAll(toPut);
    }

    public void put(String companion, EncryptionContext context) {
        contexts.put(companion, context);
    }

    public Optional<EncryptionContext> get(String companion) {
        return Optional.ofNullable(contexts.get(companion));
    }

    public void remove(String companion) {
        contexts.remove(companion);
    }

}
