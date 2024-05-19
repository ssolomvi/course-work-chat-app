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

    private EncryptionAlgorithm getEncryptionAlgorithm(String algorithm, byte[] key) {
        switch (algorithm) {
            case "DEAL": {
                return new DEAL(key);
            }
            case "Rijndael": {
                return new Rijndael(key, (byte) 27, LEN_BLOCK_FOR_RIJNDAEL);
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
                if (key.length < 7) {
                    throw new IllegalArgumentException("CRITICAL ERROR! key is less than 7 bytes!");
                }
                if (key.length == 8 || key.length == 7) {
                    return new DES(key);
                }
                byte[] shrunkKey = new byte[8];
                System.arraycopy(key, 0, shrunkKey, 0, 8);
                return new DES(shrunkKey);
            }
        }
    }

    public EncryptionContext buildEncryptionContext(InitRoomResponse response, byte[] key) {
        EncryptionModeEnum encryptionMode = getEncryptionModeEnum(response.getEncryptionMode());
        PaddingModeEnum paddingMode = getPaddingModeEnum(response.getPaddingMode());

        if (encryptionMode.needsInitVector()) {
            return new SymmetricEncryptionContextImpl(
                    encryptionMode,
                    paddingMode,
                    getEncryptionAlgorithm(response.getAlgorithm(), key),
                    response.getInitVector().getBytes(StandardCharsets.UTF_8));
        }
        return new SymmetricEncryptionContextImpl(
                encryptionMode,
                paddingMode,
                getEncryptionAlgorithm(response.getAlgorithm(), key));
    }

}
