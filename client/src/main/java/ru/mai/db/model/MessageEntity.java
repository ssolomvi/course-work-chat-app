package ru.mai.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.util.UUID;

@Entity
@Table(name = "messages")
@Getter
public class MessageEntity {
    @Id
    @Column(name = "message_id")
    private UUID messageId;

    @Column(name = "companion")
    private String companion;

    @Column(name = "filename")
    private String filename;

    @Column(name = "value")
    private String value;

    @Column(name = "is_file")
    private Boolean isFile;

//    @Column(name = "timestamp")
//    private Date timestamp;

    protected MessageEntity() {
        // конструктор без аргументов, требуемый спецификацией JPA
        // protected, поскольку не предназначен для использования напрямую
    }

    public MessageEntity(UUID messageId, String companion, String filename, String value, Boolean isFile/*Date timestamp*/) {
        this.messageId = messageId;
        this.companion = companion;
        this.filename = filename;
        this.value = value;
        this.isFile = isFile;
//        this.timestamp = timestamp;
    }

}
