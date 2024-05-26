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
    public static EncryptionModeEnum getEncryptionModeEnum(String mode) {
        return switch (mode) {
            case "CBC" -> EncryptionModeEnum.CBC;
            case "PCBC" -> EncryptionModeEnum.PCBC;
            case "CFB" -> EncryptionModeEnum.CFB;
            case "OFB" -> EncryptionModeEnum.OFB;
            case "CTR" -> EncryptionModeEnum.CTR;
            case "RANDOM_DELTA" -> EncryptionModeEnum.RANDOM_DELTA;
            default -> EncryptionModeEnum.ECB;
        };
    }

    public static EncryptionMode getEncryptionModeForGrpc(String mode) {
        return switch (mode) {
            case "CBC" -> EncryptionMode.ENCRYPTION_MODE_CBC;
            case "PCBC" -> EncryptionMode.ENCRYPTION_MODE_PCBC;
            case "CFB" -> EncryptionMode.ENCRYPTION_MODE_CFB;
            case "OFB" -> EncryptionMode.ENCRYPTION_MODE_OFB;
            case "CTR" -> EncryptionMode.ENCRYPTION_MODE_CTR;
            case "RANDOM_DELTA" -> EncryptionMode.ENCRYPTION_MODE_RANDOM_DELTA;
            default -> EncryptionMode.ENCRYPTION_MODE_ECB;
        };
    }

    public static PaddingModeEnum getPaddingModeEnum(String mode) {
        return switch (mode) {
            case "ANSI_X_923" -> PaddingModeEnum.ANSI_X_923;
            case "PKCS7" -> PaddingModeEnum.PKCS7;
            case "ISO10126" -> PaddingModeEnum.ISO10126;
            default -> PaddingModeEnum.ZEROES;
        };
    }

    public static PaddingMode getPaddingModeForGrpc(String mode) {
        return switch (mode) {
            case "ANSI_X_923" -> PaddingMode.PADDING_MODE_ANSI_X_923;
            case "PKCS7" -> PaddingMode.PADDING_MODE_PKCS7;
            case "ISO10126" -> PaddingMode.PADDING_MODE_ISO10126;
            default -> PaddingMode.PADDING_MODE_ZEROES;
        };
    }

    public static Algorithm getAlgorithmForGrpc(String algorithm) {
        return switch (algorithm) {
            case "DEAL" -> Algorithm.ALGORITHM_DEAL;
            case "Rijndael" -> Algorithm.ALGORITHM_RIJNDAEL;
            case "LOKI97" -> Algorithm.ALGORITHM_LOKI97;
            case "RC6" -> Algorithm.ALGORITHM_RC6;
            case "MARS" -> Algorithm.ALGORITHM_MARS;
            default -> Algorithm.ALGORITHM_DES;
        };
    }


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
        };
    }

    private static EncryptionAlgorithm getEncryptionAlgorithm(String algorithm, byte[] key) {
        //            case "MARS" : {
        //                return new MARS();
        return switch (algorithm) {
            case "DEAL" -> {
                byte[] normalizedKey = normalizeKey(key, AlgorithmsConfigs.DEAL_KEY_LENGTH);
                yield new DEAL(normalizedKey);
            }
            case "RIJNDAEL" -> {
                byte[] normalizedKey = normalizeKey(key, AlgorithmsConfigs.RIJNDAEL_KEY_LENGTH);
                yield new Rijndael(normalizedKey, (byte) 27, AlgorithmsConfigs.RIJNDAEL_BLOCK_LENGTH);
            }
            case "RC6" -> {
                byte[] normalizedKey = normalizeKey(key, AlgorithmsConfigs.RIJNDAEL_KEY_LENGTH);
                yield new RC6(normalizedKey);
            }
            case "LOKI97" -> {
                byte[] normalizedKey = normalizeKey(key, AlgorithmsConfigs.RIJNDAEL_KEY_LENGTH);
                yield new LOKI97(normalizedKey);
            }
            default -> {
                byte[] normalizedKey = normalizeKey(key, AlgorithmsConfigs.DES_KEY_LENGTH);
                yield new DES(normalizedKey);
            }
        };
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

    public static EncryptionContext createEncryptionContext(String encryptionModeStr,
                                                            String paddingModeStr,
                                                            String algorithmStr,
                                                            byte[] initVector,
                                                            byte[] key) {
        EncryptionModeEnum encryptionModeEnum = getEncryptionModeEnum(encryptionModeStr);
        PaddingModeEnum paddingModeEnum = getPaddingModeEnum(paddingModeStr);
        EncryptionAlgorithm algorithm = getEncryptionAlgorithm(algorithmStr, key);

        if (encryptionModeEnum.needsInitVector()) {
            return new SymmetricEncryptionContextImpl(encryptionModeEnum, paddingModeEnum, algorithm, initVector);
        }
        return new SymmetricEncryptionContextImpl(encryptionModeEnum, paddingModeEnum, algorithm);
    }

    private ContextBuilder() {
        // to hide the implicit public constructor
    }
}
