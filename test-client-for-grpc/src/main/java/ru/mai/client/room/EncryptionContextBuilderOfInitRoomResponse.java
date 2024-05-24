package ru.mai.client.room;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.mai.InitRoomResponse;
import ru.mai.db.ChatMetadataDb;
import ru.mai.db.repositories.ChatMetadataDbRepository;
import ru.mai.db.service.ChatMetadataDbService;
import ru.mai.encryption_algorithm.EncryptionAlgorithm;
import ru.mai.encryption_algorithm.impl.DEAL;
import ru.mai.encryption_algorithm.impl.DES;
import ru.mai.encryption_algorithm.impl.Rijndael;
import ru.mai.encryption_context.EncryptionContext;
import ru.mai.encryption_context.SymmetricEncryptionContextImpl;
import ru.mai.encryption_mode.EncryptionModeEnum;
import ru.mai.encryption_padding_mode.PaddingModeEnum;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class EncryptionContextBuilderOfInitRoomResponse {
    private static final int LEN_BLOCK_FOR_RIJNDAEL = Rijndael.KEY_LENGTH24;
    private static final int KEY_LENGTH_FOR_DEAL = DEAL.KEY_LENGTH24;
    private static final int KEY_LENGTH_FOR_RIJNDAEL = Rijndael.KEY_LENGTH24;
//    private static final int KEY_LENGTH_FOR_LOKI97 = LOKI97.KEY_LENGTH24;

    @Autowired
    private ChatMetadataDbService service;

    public EncryptionContextBuilderOfInitRoomResponse() {
//        this.service = service;
    }

    private EncryptionModeEnum getEncryptionModeEnum(String encModeStr) {
        return switch (encModeStr) {
            case "Cipher block chaining" -> EncryptionModeEnum.CBC;
            case "Propagating cipher block chaining" -> EncryptionModeEnum.PCBC;
            case "Cipher feedback" -> EncryptionModeEnum.CFB;
            case "Output feedback" -> EncryptionModeEnum.OFB;
            case "Counter mode" -> EncryptionModeEnum.CTR;
            case "Random Delta" -> EncryptionModeEnum.RANDOM_DELTA;
            default -> EncryptionModeEnum.ECB;
        };
    }

    private PaddingModeEnum getPaddingModeEnum(String padModeStr) {
        return switch (padModeStr) {
            case "ANSI_X_923" -> PaddingModeEnum.ANSI_X_923;
            case "PKCS7" -> PaddingModeEnum.PKCS7;
            case "ISO10126" -> PaddingModeEnum.ISO10126;
            default -> PaddingModeEnum.ZEROES;
        };
    }

    private byte[] normalizeKey(byte[] oldKey, int sizeNeeded) {
        if (oldKey.length == sizeNeeded) {
            return oldKey;
        }

        byte[] newKey = new byte[sizeNeeded];

        if (oldKey.length < sizeNeeded) {
            System.arraycopy(oldKey, 0, newKey, sizeNeeded - oldKey.length, oldKey.length); // expanding old key
        } else {
            System.arraycopy(oldKey, 0, newKey, 0, sizeNeeded); // shrinking old key
        }
        return newKey;
    }

    private EncryptionAlgorithm getEncryptionAlgorithm(String algorithm, byte[] key) {
        switch (algorithm) {
            case "DEAL": {
                byte[] normalizedKey = normalizeKey(key, KEY_LENGTH_FOR_DEAL);
                return new DEAL(normalizedKey);
            }
            case "Rijndael": {
                byte[] normalizedKey = normalizeKey(key, KEY_LENGTH_FOR_RIJNDAEL);
                return new Rijndael(normalizedKey, (byte) 27, LEN_BLOCK_FOR_RIJNDAEL);
            }
//            case "LOKI97" : {
//                return new LOKI97();
//            }
//            case "MARS" : {
//                return new MARS();
//            }
//            case "RC6" : {
//                return new RC6();
//            }
            default: {
                byte[] normalizedKey = normalizeKey(key, DES.KEY_SIZE);
                return new DES(normalizedKey);
            }
        }
    }

    private void saveChatRoomMetadataToDb(String companion,
                                          EncryptionModeEnum encMode,
                                          PaddingModeEnum padMode,
                                          String algorithm,
                                          byte[] initVector,
                                          byte[] key) {
        ChatMetadataDb metadata = ChatMetadataDb.builder()
                .companion(companion)
                .encryptionMode(encMode)
                .paddingMode(padMode)
                .algorithm(algorithm)
                .initVector(initVector)
                .key(key)
                .build();

        service.save(metadata);
        log.debug("companion {} chat info saved to db?", companion);
    }

    public EncryptionContext buildEncryptionContext(InitRoomResponse response, byte[] key) {
        EncryptionModeEnum encryptionMode = getEncryptionModeEnum(response.getEncryptionMode());
        PaddingModeEnum paddingMode = getPaddingModeEnum(response.getPaddingMode());

        if (encryptionMode.needsInitVector()) {
            // todo: ask Vlad
//            saveChatRoomMetadataToDb(response.getCompanionLogin(), encryptionMode, paddingMode,
//                    response.getAlgorithm(), response.getInitVector().getBytes(StandardCharsets.UTF_8), key);

            return new SymmetricEncryptionContextImpl(
                    encryptionMode,
                    paddingMode,
                    getEncryptionAlgorithm(response.getAlgorithm(), key),
                    response.getInitVector().getBytes(StandardCharsets.UTF_8));
        }

//        saveChatRoomMetadataToDb(response.getCompanionLogin(), encryptionMode, paddingMode, response.getAlgorithm(),
//                null, key);

        return new SymmetricEncryptionContextImpl(
                encryptionMode,
                paddingMode,
                getEncryptionAlgorithm(response.getAlgorithm(), key));
    }

}
