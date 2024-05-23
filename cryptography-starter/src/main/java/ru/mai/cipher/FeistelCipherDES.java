package ru.mai.cipher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mai.encryption_conversion.EncryptionConversion;
import ru.mai.exceptions.IllegalArgumentExceptionWithLog;
import ru.mai.round_keys.RoundKeyGeneration;
import ru.mai.utils.Operations;

public class FeistelCipherDES {
    private static final Logger log = LoggerFactory.getLogger(FeistelCipherDES.class);
    private final RoundKeyGeneration keyGeneration;
    private final EncryptionConversion encryptionConversion;
    private byte[][] roundKeys;

    public FeistelCipherDES(RoundKeyGeneration keyGen, EncryptionConversion conversion, byte[] key) {
        if (keyGen == null) {
            throw new IllegalArgumentExceptionWithLog("Passed param keyGen is null", log);
        }
        if (conversion == null) {
            throw new IllegalArgumentExceptionWithLog("Passed param conversion is null", log);
        }
        if (key == null) {
            throw new IllegalArgumentExceptionWithLog("Passed param key is null", log);
        }

        this.keyGeneration = keyGen;
        this.encryptionConversion = conversion;
        this.roundKeys = keyGeneration.generateRoundKeys(key);
    }

    /**
     * for roundCount
     * <p>
     * divide input in 2 parts
     * <p>
     * found L_i, R_i:
     * <p>
     * L_i: R_i-1 xor f(L_i, roundKey)
     * <p>
     * R_i: L_i-1
     * <p>
     * last iteration don't swap
     */
    public byte[] methodForConstructingBlockCiphers(byte[] input, int roundCount) {
        int leftRightSize = input.length / 2 + (input.length % 2 == 0 ? 0 : 1);

        byte[] inputFixed = new byte[leftRightSize * 2];
        if (input.length % 2 == 0) {
            inputFixed = input;
        } else {
            System.arraycopy(input, 0, inputFixed, 1, input.length);
        }

        byte[] right = new byte[leftRightSize];
        byte[] left = new byte[leftRightSize];

        System.arraycopy(inputFixed, 0, left, 0, leftRightSize);
        System.arraycopy(inputFixed, leftRightSize, right, 0, leftRightSize);

        for (int i = 0; i < roundCount; ++i) {
            byte[] tmp = Operations.xor(right, encryptionConversion.encrypt(left, roundKeys[i]));
            right = left;
            left = tmp;
        }

        byte[] tmp = right;
        right = left;
        left = tmp;

        return Operations.mergeByteArrays(left, right);
    }

    public void regenerateRoundKeysWithNewKey(byte[] key) {
        if (key == null) {
            throw new IllegalArgumentExceptionWithLog("Passed param key is null", log);
        }

        roundKeys = keyGeneration.generateRoundKeys(key);
    }
}
