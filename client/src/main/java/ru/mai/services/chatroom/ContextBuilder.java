package ru.mai.services.chatroom;

import ru.mai.Algorithm;
import ru.mai.EncryptionMode;
import ru.mai.PaddingMode;
import ru.mai.encryption_algorithm.EncryptionAlgorithm;
import ru.mai.encryption_algorithm.impl.*;
import ru.mai.encryption_context.EncryptionContext;
import ru.mai.encryption_context.SymmetricEncryptionContextImpl;
import ru.mai.encryption_mode.EncryptionModeEnum;
import ru.mai.encryption_padding_mode.PaddingModeEnum;

public class ContextBuilder {

    public static EncryptionModeEnum getEncryptionModeEnum(EncryptionMode encModeStr) {
        return switch (encModeStr) {
            case ENCRYPTION_MODE_CBC -> EncryptionModeEnum.CBC;
            case ENCRYPTION_MODE_PCBC -> EncryptionModeEnum.PCBC;
            case ENCRYPTION_MODE_CFB -> EncryptionModeEnum.CFB;
            case ENCRYPTION_MODE_OFB -> EncryptionModeEnum.OFB;
            case ENCRYPTION_MODE_CTR -> EncryptionModeEnum.CTR;
            case ENCRYPTION_MODE_RANDOM_DELTA -> EncryptionModeEnum.RANDOM_DELTA;
            default -> EncryptionModeEnum.ECB;
        };
    }

    public static PaddingModeEnum getPaddingModeEnum(PaddingMode padModeStr) {
        return switch (padModeStr) {
            case PADDING_MODE_ANSI_X_923 -> PaddingModeEnum.ANSI_X_923;
            case PADDING_MODE_PKCS7 -> PaddingModeEnum.PKCS7;
            case PADDING_MODE_ISO10126 -> PaddingModeEnum.ISO10126;
            default -> PaddingModeEnum.ZEROES;
        };
    }

    public static byte[] normalizeKey(byte[] oldKey, int sizeNeeded) {
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

    private static EncryptionAlgorithm getEncryptionAlgorithm(Algorithm algorithm, byte[] key) {
        //            case "MARS" : {
        //                return new MARS();
        return switch (algorithm) {
            case ALGORITHM_DEAL -> {
                byte[] normalizedKey = normalizeKey(key, AlgorithmsConfigs.DEAL_KEY_LENGTH);
                yield new DEAL(normalizedKey);
            }
            case ALGORITHM_RIJNDAEL -> {
                byte[] normalizedKey = normalizeKey(key, AlgorithmsConfigs.RIJNDAEL_KEY_LENGTH);
                yield new Rijndael(normalizedKey, (byte) 27, AlgorithmsConfigs.RIJNDAEL_BLOCK_LENGTH);
            }
            case ALGORITHM_RC6 -> {
                byte[] normalizedKey = normalizeKey(key, AlgorithmsConfigs.RIJNDAEL_KEY_LENGTH);
                yield new RC6(normalizedKey);
            }
            case ALGORITHM_LOKI97 -> {
                byte[] normalizedKey = normalizeKey(key, AlgorithmsConfigs.RIJNDAEL_KEY_LENGTH);
                yield new LOKI97(normalizedKey);
            }
            default -> {
                byte[] normalizedKey = normalizeKey(key, AlgorithmsConfigs.DES_KEY_LENGTH);
                yield new DES(normalizedKey);
            }
        }

                ;
    }


    public static EncryptionContext createEncryptionContext(EncryptionModeEnum encryptionModeEnum,
                                                            PaddingModeEnum paddingModeEnum,
                                                            Algorithm algorithm,
                                                            byte[] initVector,
                                                            byte[] normalizedKey) {
        if (encryptionModeEnum.needsInitVector()) {
            return new SymmetricEncryptionContextImpl(encryptionModeEnum, paddingModeEnum, getEncryptionAlgorithm(algorithm, normalizedKey), initVector);
        }
        return new SymmetricEncryptionContextImpl(encryptionModeEnum, paddingModeEnum, getEncryptionAlgorithm(algorithm, normalizedKey));
    }

    private ContextBuilder() {
        // to hide the implicit public constructor
    }
}
