package ru.mai.services.chatroom;

import ru.mai.encryption_algorithm.EncryptionAlgorithm;
import ru.mai.encryption_algorithm.impl.DEAL;
import ru.mai.encryption_algorithm.impl.DES;
import ru.mai.encryption_algorithm.impl.Rijndael;
import ru.mai.encryption_context.EncryptionContext;
import ru.mai.encryption_context.SymmetricEncryptionContextImpl;
import ru.mai.encryption_mode.EncryptionModeEnum;
import ru.mai.encryption_padding_mode.PaddingModeEnum;

public class ContextBuilder {
    private static final int KEY_LENGTH_FOR_DEAL = DEAL.KEY_LENGTH24;
    private static final int LEN_BLOCK_FOR_RIJNDAEL = Rijndael.KEY_LENGTH24;
    private static final int KEY_LENGTH_FOR_RIJNDAEL = Rijndael.KEY_LENGTH24;

    public static EncryptionModeEnum getEncryptionModeEnum(String encModeStr) {
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

    public static PaddingModeEnum getPaddingModeEnum(String padModeStr) {
        return switch (padModeStr) {
            case "ANSI_X_923" -> PaddingModeEnum.ANSI_X_923;
            case "PKCS7" -> PaddingModeEnum.PKCS7;
            case "ISO10126" -> PaddingModeEnum.ISO10126;
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

    private static EncryptionAlgorithm getEncryptionAlgorithm(String algorithm, byte[] key) {
        //            case "LOKI97" : {
        //                return new LOKI97();
        //            }
        //            case "MARS" : {
        //                return new MARS();
        //            }
        //            case "RC6" : {
        //                return new RC6();
        //            }
        return switch (algorithm) {
            case "DEAL" -> {
                byte[] normalizedKey = normalizeKey(key, KEY_LENGTH_FOR_DEAL);
                yield new DEAL(normalizedKey);
            }
            case "Rijndael" -> {
                byte[] normalizedKey = normalizeKey(key, KEY_LENGTH_FOR_RIJNDAEL);
                yield new Rijndael(normalizedKey, (byte) 27, LEN_BLOCK_FOR_RIJNDAEL);
            }
            default -> {
                byte[] normalizedKey = normalizeKey(key, DES.KEY_SIZE);
                yield new DES(normalizedKey);
            }
        };
    }


    public static EncryptionContext createEncryptionContext(EncryptionModeEnum encryptionModeEnum,
                                                            PaddingModeEnum paddingModeEnum,
                                                            String algorithm,
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
