package ru.mai.cipher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mai.encryption_conversion.EncryptionConversion;
import ru.mai.exceptions.IllegalArgumentExceptionWithLog;
import ru.mai.round_keys.RoundKeyGeneration;
import ru.mai.utils.Operations;

public class FeistelCipherLOKI97 {
    private static final Logger log = LoggerFactory.getLogger(FeistelCipherDES.class);
    private final RoundKeyGeneration keyGeneration;
    private final EncryptionConversion encryptionConversion;
    private byte[][] roundKeys;

    public FeistelCipherLOKI97(RoundKeyGeneration keyGen, EncryptionConversion conversion, byte[] key) {
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

    public byte[] methodForConstructingBlockCiphersEncryption(byte[] input, int roundCount) {
        if (roundCount * 3 != roundKeys.length) {
            throw new IllegalArgumentExceptionWithLog("methodForConstructingBlockCiphersEncryption:" +
                    "For LOKI97 there are must be 16 * 3 round keys", log);
        }

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

        for (int i = 1; i <= roundCount; ++i) {
            int currRoundSubKeysIndex = 3 * roundCount - 1; // for SKi
            byte[] tmp = Operations.xor(left, encryptionConversion.encrypt(
                    Operations.additionByteArrays(right, roundKeys[currRoundSubKeysIndex - 2]),
                    roundKeys[currRoundSubKeysIndex - 1]));
            left = Operations.additionByteArrays(
                    Operations.additionByteArrays(
                            right, roundKeys[currRoundSubKeysIndex - 2]), roundKeys[currRoundSubKeysIndex]);
            right = tmp;
        }

        byte[] tmp = right;
        right = left;
        left = tmp;

        return Operations.mergeByteArrays(left, right);
    }
    public byte[] methodForConstructingBlockCiphersDecryption(byte[] input, int roundCount) {
        if (roundCount * 3 != roundKeys.length) {
            throw new IllegalArgumentExceptionWithLog("methodForConstructingBlockCiphersDecryption: " +
                    "For LOKI97 there are must be 16 * 3 round keys", log);
        }

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

        for (int i = roundCount; i >= 1; --i) {
            int currRoundSubKeysIndex = 3 * roundCount - 1; // for SKi
            byte[] tmp = Operations.xor(left, encryptionConversion.encrypt(
                    Operations.subtractionByteArrays(right, roundKeys[currRoundSubKeysIndex]),
                    roundKeys[currRoundSubKeysIndex - 1]));
            left = Operations.subtractionByteArrays(
                    Operations.subtractionByteArrays(
                            right, roundKeys[currRoundSubKeysIndex]), roundKeys[currRoundSubKeysIndex - 2]);
            right = tmp;
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
