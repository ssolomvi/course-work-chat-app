package ru.mai.db;

import org.hibernate.annotations.Type;
import ru.mai.encryption_mode.EncryptionModeEnum;
import ru.mai.encryption_padding_mode.PaddingModeEnum;

import javax.persistence.*;

@Entity
@Table(name = "chat_metadata")
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
    private PaddingModeEnum algorithm;

    @Column(name = "init_vector")
    @Type(type = "org.hibernate.type.RowVersionType")
    private byte[] initVector;

    @Column(name = "key")
    @Type(type = "org.hibernate.type.RowVersionType")
    private byte[] key;
}
