package ru.mai.db;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import ru.mai.encryption_mode.EncryptionModeEnum;
import ru.mai.encryption_padding_mode.PaddingModeEnum;


@Entity
@Table(name = "chat_metadata")
@Setter
@Getter
@Builder
public class ChatMetadataDb {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_metadata_id")
    private Long chatId;

    @Column(name = "companion")
    private String companion;

    @Column(name = "encryption_mode")
    private EncryptionModeEnum encryptionMode;

    @Column(name = "padding_mode")
    private PaddingModeEnum paddingMode;

    @Column(name = "algorithm")
    private String algorithm;

    @Column(name = "init_vector", columnDefinition = "bytea")
    private byte[] initVector;

    @Column(name = "key", columnDefinition = "bytea")
    private byte[] key;
}
