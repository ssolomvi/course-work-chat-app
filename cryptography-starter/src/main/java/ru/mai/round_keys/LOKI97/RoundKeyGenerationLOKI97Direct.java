package ru.mai.round_keys.LOKI97;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mai.encryption_conversion.EncryptionConversionFeistelFunctionLOKI97;
import ru.mai.exceptions.IllegalArgumentExceptionWithLog;
import ru.mai.round_keys.RoundKeyGeneration;
import ru.mai.utils.Operations;

import java.math.BigInteger;

public class RoundKeyGenerationLOKI97Direct implements RoundKeyGeneration {
    static final Logger log = LoggerFactory.getLogger(RoundKeyGenerationLOKI97Direct.class);
    private final EncryptionConversionFeistelFunctionLOKI97 encryptionConversion;
    private final BigInteger DELTA = new BigInteger("11400714819323198485");

    public RoundKeyGenerationLOKI97Direct(EncryptionConversionFeistelFunctionLOKI97 encryptionConversion) {
        this.encryptionConversion = encryptionConversion;
    }

    private byte[] g(byte[] K1, byte[] K3, byte[] K2, int i) {
        long I = i;
        return encryptionConversion.encrypt(
                Operations.additionByteArrays(
                        K1, Operations.additionByteArrayAndBigInteger(K3, DELTA.multiply(BigInteger.valueOf(I)))),
                K2);
    }

    @Override
    public byte[][] generateRoundKeys(byte[] key) {
        int lenKey = key.length;

        byte[] K1 = new byte[8];
        byte[] K2 = new byte[8];
        byte[] K3 = new byte[8];
        byte[] K4 = new byte[8];

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
            // todo:
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
            throw new IllegalArgumentExceptionWithLog("Param key must be size of 16, 24 or 32 bytes", log);
        }

        byte[][] roundKeys = new byte[48][8];
        for (int i = 1; i <= 48; i++) {
            K1 = Operations.xor(K4, g(K1, K3, K2, i));
            K4 = K3;
            K3 = K2;
            K2 = K1;
            roundKeys[i - 1] = K1;
        }

        return roundKeys;
    }
}
