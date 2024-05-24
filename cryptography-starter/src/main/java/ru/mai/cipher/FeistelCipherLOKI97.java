package ru.mai.cipher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mai.encryption_conversion.EncryptionConversion;
import ru.mai.exceptions.IllegalArgumentExceptionWithLog;
import ru.mai.round_keys.RoundKeyGeneration;
import ru.mai.utils.Operations;

public class FeistelCipherLOKI97 {
    private static final Logger log = LoggerFactory.getLogger(FeistelCipherDES.class);
    private final EncryptionConversion encryptionConversion;
    private static byte[][] roundKeys; // must be 48 for roundCount = 16

    public FeistelCipherLOKI97(RoundKeyGeneration keyGen, EncryptionConversion conversion, byte[] key) {
        if (keyGen == null) {
            throw new IllegalArgumentExceptionWithLog("FeistelCipherLOKI97: Passed param keyGen is null", log);
        }
        if (conversion == null) {
            throw new IllegalArgumentExceptionWithLog("FeistelCipherLOKI97: Passed param conversion is null", log);
        }
        if (key == null) {
            throw new IllegalArgumentExceptionWithLog("FeistelCipherLOKI97: Passed param key is null", log);
        }

        this.encryptionConversion = conversion;
        roundKeys = keyGen.generateRoundKeys(key);
    }

    public byte[] methodForConstructingBlockCiphersEncryption(byte[] input, int roundCount) {
        if (roundCount * 3 != roundKeys.length) {
            throw new IllegalArgumentExceptionWithLog("methodForConstructingBlockCiphersEncryption:" +
                    "Param roundCount must be equal roundKeysCount / 3", log);
        }
        if (input.length != 16) {
            throw new IllegalArgumentExceptionWithLog("methodForConstructingBlockCiphersEncryption:" +
                    "Param input must be length 16 bytes", log);
        }

        int leftRightSize = input.length / 2;
        byte[] right = new byte[leftRightSize];
        byte[] left = new byte[leftRightSize];

        System.arraycopy(input, 0, left, 0, leftRightSize);
        System.arraycopy(input, leftRightSize, right, 0, leftRightSize);

        for (int i = 1; i <= roundCount; i++) {
            int currRoundSubKeysIndex = 3 * i - 1; // for SKi
            byte[] tmp = Operations.xor(
                    left, encryptionConversion.encrypt(
                            Operations.additionByteArraysLength8(
                                    right,
                                    roundKeys[currRoundSubKeysIndex - 2]),
                            roundKeys[currRoundSubKeysIndex - 1]));
            left = Operations.additionByteArraysLength8(
                    Operations.additionByteArraysLength8(
                            right,
                            roundKeys[currRoundSubKeysIndex - 2]),
                    roundKeys[currRoundSubKeysIndex]);
            right = tmp;
        }

        return Operations.mergeByteArrays(right, left);
    }

    public byte[] methodForConstructingBlockCiphersDecryption(byte[] input, int roundCount) {
        if (roundCount * 3 != roundKeys.length) {
            throw new IllegalArgumentExceptionWithLog("methodForConstructingBlockCiphersDecryption:" +
                    "Param roundCount must be equal roundKeysCount / 3", log);
        }
        if (input.length != 16) {
            throw new IllegalArgumentExceptionWithLog("methodForConstructingBlockCiphersDecryption:" +
                    "Param input must be length 16 bytes", log);
        }

        int leftRightSize = input.length / 2;
        byte[] right = new byte[leftRightSize];
        byte[] left = new byte[leftRightSize];

        System.arraycopy(input, 0, right, 0, leftRightSize);
        System.arraycopy(input, leftRightSize, left, 0, leftRightSize);

        for (int i = roundCount; i >= 1; i--) {
            int currRoundSubKeysIndex = 3 * i - 1; // for SKi
            byte[] tmp = Operations.xor(right, encryptionConversion.encrypt(
                    Operations.subtractionByteArraysLength8(left, roundKeys[currRoundSubKeysIndex]),
                    roundKeys[currRoundSubKeysIndex - 1]));
            right = Operations.subtractionByteArraysLength8(
                    Operations.subtractionByteArraysLength8(
                            left, roundKeys[currRoundSubKeysIndex]),
                    roundKeys[currRoundSubKeysIndex - 2]);
            left = tmp;
        }

        return Operations.mergeByteArrays(left, right);
    }
}
