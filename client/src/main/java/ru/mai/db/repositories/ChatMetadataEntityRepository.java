package ru.mai.db.repositories;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mai.db.model.ChatMetadataEntity;

import java.util.Optional;

@Repository
public interface ChatMetadataEntityRepository extends JpaRepository<ChatMetadataEntity, String> {
    public Optional<ChatMetadataEntity> findByCompanion(String companion);
}
