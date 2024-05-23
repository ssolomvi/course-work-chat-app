package ru.mai.db.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;
import ru.mai.db.ChatMetadataDb;
import ru.mai.db.repositories.ChatMetadataDbRepository;

import java.util.Optional;

@Service
public class ChatMetadataDbService {
    private final ChatMetadataDbRepository repository;

    public ChatMetadataDbService(@Autowired ChatMetadataDbRepository repository) {
        this.repository = repository;
    }

    public void save(ChatMetadataDb entity) {
        repository.saveAndFlush(entity);
    }

    public Optional<ChatMetadataDb> getChatMetadataByCompanion(String companion) {
        return repository.findByCompanion(companion);
    }

    public void deleteChatMetadataByCompanion(String companion) {
        repository.deleteByCompanion(companion);
    }
}
