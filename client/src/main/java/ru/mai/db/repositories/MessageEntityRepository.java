package ru.mai.db.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mai.db.model.MessageEntity;

import java.util.UUID;

@Repository
public interface MessageEntityRepository extends JpaRepository<MessageEntity, UUID> {
    void deleteAllByCompanion(String companion);
}
