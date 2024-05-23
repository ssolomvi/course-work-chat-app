package ru.mai.round_keys.LOKI97;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mai.encryption_conversion.EncryptionConversionFeistelFunctionLOKI97;
import ru.mai.exceptions.IllegalArgumentExceptionWithLog;
import ru.mai.round_keys.RoundKeyGeneration;
import ru.mai.utils.Operations;

public class RoundKeyGenerationLOKI97 implements RoundKeyGeneration {
    static final Logger log = LoggerFactory.getLogger(RoundKeyGenerationLOKI97.class);
    private final EncryptionConversionFeistelFunctionLOKI97 encryptionConversion;
    private static final int ROUND_COUNT_LOKI97 = 16;
    private final long DELTA = 0x9E3779B97F4A7C15L; // |_ (sqrt(5) - 1) * 2^63 _| , where |_ x _| - function "floor"

    public RoundKeyGenerationLOKI97(EncryptionConversionFeistelFunctionLOKI97 encryptionConversion) {
        this.encryptionConversion = encryptionConversion;
    }

    /**
     * Function {@code g(K1, K3, K2) = f(K1 + K3 + (DELTA * i), K2)},
     * where "+" - the addition of two long values (module {@code 2^64)}
     * @param i number of round
     * @return Result of using Feistel function
     */
    private byte[] g(byte[] K1, byte[] K3, byte[] K2, int i) {
        long deltaMultiplyI = DELTA * i;
        return encryptionConversion.encrypt(
                Operations.additionByteArrays(
                        K1,
                        Operations.additionByteArrayAndLong(
                                K3,
                                deltaMultiplyI)),
                K2);
    }

    @Override
    public byte[][] generateRoundKeys(byte[] key) {
        int lenKey = key.length;

        byte[] K1, K2, K3, K4;

        if (lenKey == 16) {
            int lenLeftRight = lenKey / 2;
            byte[] keyA = new byte[lenLeftRight];
            byte[] keyB = new byte[lenLeftRight];

            System.arraycopy(key, 0, keyA, 0, lenLeftRight);
            System.arraycopy(key, lenLeftRight, keyB, 0, lenLeftRight);

            K4 = keyA;
            K3 = keyB;
            K2 = encryptionConversion.encrypt(keyB, keyA);
            K1 = encryptionConversion.encrypt(keyA, keyB);
        } else if (lenKey == 24) {
            int lenOnePart = lenKey / 3;

            byte[] keyA = new byte[lenOnePart];
            byte[] keyB = new byte[lenOnePart];
            byte[] keyC = new byte[lenOnePart];

            System.arraycopy(key, 0, keyA, 0, lenOnePart);
            System.arraycopy(key, lenOnePart, keyB, 0, lenOnePart);
            System.arraycopy(key, 2 * lenOnePart, keyC, 0, lenOnePart);

            K4 = keyA;
            K3 = keyB;
            K2 = keyC;
            K1 = encryptionConversion.encrypt(keyA, keyB);
        } else if (lenKey == 32) {
            int lenOnePart = lenKey / 4;

            byte[] keyA = new byte[lenOnePart];
            byte[] keyB = new byte[lenOnePart];
            byte[] keyC = new byte[lenOnePart];
            byte[] keyD = new byte[lenOnePart];

            System.arraycopy(key, 0, keyA, 0, lenOnePart);
            System.arraycopy(key, lenOnePart, keyB, 0, lenOnePart);
            System.arraycopy(key, 2 * lenOnePart, keyC, 0, lenOnePart);
            System.arraycopy(key, 3 * lenOnePart, keyD, 0, lenOnePart);

            K4 = keyA;
            K3 = keyB;
            K2 = keyC;
            K1 = keyD;
        } else {
            throw new IllegalArgumentExceptionWithLog("generateRoundKeys: Param key must be size of 16, 24 or 32 bytes", log);
        }

        byte[][] roundKeys = new byte[48][8];
        for (int i = 1; i <= 3 * ROUND_COUNT_LOKI97; i++) {
            roundKeys[i - 1] = Operations.xor(K4, g(K1, K3, K2, i));
            K4 = K3;
            K3 = K2;
            K2 = K1;
            K1 = roundKeys[i - 1].clone();
        }

        return roundKeys;
    }
}