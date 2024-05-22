package ru.mai.client.room;

import ru.mai.InitRoomResponse;
import ru.mai.encryption_algorithm.EncryptionAlgorithm;
import ru.mai.encryption_algorithm.impl.DEAL;
import ru.mai.encryption_algorithm.impl.DES;
import ru.mai.encryption_algorithm.impl.Rijndael;
import ru.mai.encryption_context.EncryptionContext;
import ru.mai.encryption_context.SymmetricEncryptionContextImpl;
import ru.mai.encryption_mode.EncryptionModeEnum;
import ru.mai.encryption_padding_mode.PaddingModeEnum;

import java.nio.charset.StandardCharsets;

public class EncryptionContextBuilderOfInitRoomResponse {
    private static final int LEN_BLOCK_FOR_RIJNDAEL = 24; // options: 16, 24, 32

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
                byte[] normalizedKey = normalizeKey(key, DEAL.KEY_LENGTH24);
                return new DEAL(normalizedKey);
            }
            case "Rijndael": {
                byte[] normalizedKey = normalizeKey(key, Rijndael.KEY_LENGTH24);
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
        // todo: insert in db with spring data jpa
    }

    public EncryptionContext buildEncryptionContext(InitRoomResponse response, byte[] key) {
        EncryptionModeEnum encryptionMode = getEncryptionModeEnum(response.getEncryptionMode());
        PaddingModeEnum paddingMode = getPaddingModeEnum(response.getPaddingMode());

        if (encryptionMode.needsInitVector()) {
            saveChatRoomMetadataToDb(response.getCompanionLogin(), encryptionMode, paddingMode,
                    response.getAlgorithm(), response.getInitVector().getBytes(StandardCharsets.UTF_8), key);
            return new SymmetricEncryptionContextImpl(
                    encryptionMode,
                    paddingMode,
                    getEncryptionAlgorithm(response.getAlgorithm(), key),
                    response.getInitVector().getBytes(StandardCharsets.UTF_8));
        }
        saveChatRoomMetadataToDb(response.getCompanionLogin(), encryptionMode, paddingMode, response.getAlgorithm(),
                null, key);
        return new SymmetricEncryptionContextImpl(
                encryptionMode,
                paddingMode,
                getEncryptionAlgorithm(response.getAlgorithm(), key));
    }

}
