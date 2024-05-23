package ru.mai.db.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mai.db.ChatMetadataDb;

import java.util.Optional;

@Repository
public interface ChatMetadataDbRepository extends JpaRepository<ChatMetadataDb, Long> {
    void deleteByCompanion(String companion);
    Optional<ChatMetadataDb> findByCompanion(String companion);
}
