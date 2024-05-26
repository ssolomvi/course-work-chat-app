package ru.mai.db.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import ru.mai.Algorithm;
import ru.mai.db.repositories.ChatMetadataEntityRepository;
import ru.mai.encryption_mode.EncryptionModeEnum;
import ru.mai.encryption_padding_mode.PaddingModeEnum;


@Entity
@Table(name = "chat_metadata")
@Getter
public class ChatMetadataEntity {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "chat_metadata_id")
//    private Long chatId;


    /*
    * identity field should be numeric and not string based, both for space saving and for performance reasons
    * (matching keys on strings is slower than matching on integers)
    * */
    @Id
    @Column(name = "companion")
    private String companion;

    @Column(name = "encryption_mode")
    private String encryptionMode;

    @Column(name = "padding_mode")
    private String paddingMode;

    @Column(name = "algorithm")
    private String algorithm;

    @Column(name = "init_vector", columnDefinition = "bytea")
    private byte[] initVector;

    @Column(name = "key", columnDefinition = "bytea")
    private byte[] key;

    ChatMetadataEntityRepository rep;

    protected ChatMetadataEntity() {
        // конструктор без аргументов, требуемый спецификацией JPA
        // protected, поскольку не предназначен для использования напрямую
    }

    public ChatMetadataEntity(String companion,
                              String encryptionMode,
                              String paddingMode,
                              String algorithm,
                              byte[] initVector,
                              byte[] key) {
        this.companion = companion;
        this.encryptionMode = encryptionMode;
        this.paddingMode = paddingMode;
        this.algorithm = algorithm;
        this.initVector = initVector;
        this.key = key;
    }
}
