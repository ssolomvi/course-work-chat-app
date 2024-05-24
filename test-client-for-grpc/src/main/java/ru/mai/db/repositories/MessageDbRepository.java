package ru.mai.db.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mai.db.MessageDb;

@Repository
public interface MessageDbRepository extends JpaRepository<MessageDb, Long> {
}
