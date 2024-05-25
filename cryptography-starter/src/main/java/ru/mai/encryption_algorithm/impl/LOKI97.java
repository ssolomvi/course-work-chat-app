package ru.mai.encryption_algorithm.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mai.cipher.FeistelCipherLOKI97;
import ru.mai.encryption_algorithm.EncryptionAlgorithm;
import ru.mai.encryption_conversion.EncryptionConversionFeistelFunctionLOKI97;
import ru.mai.exceptions.IllegalArgumentExceptionWithLog;
import ru.mai.round_keys.LOKI97.RoundKeyGenerationLOKI97;

public class LOKI97 implements EncryptionAlgorithm {
    private static final Logger log = LoggerFactory.getLogger(LOKI97.class);
    private final int roundCount = 16;
    private final FeistelCipherLOKI97 cipherEncryption;
    private final FeistelCipherLOKI97 cipherDecryption;
    public static final int KEY_LENGTH_LOKI97_16 = 16;
    public static final int KEY_LENGTH_LOKI97_24 = 24;
    public static final int KEY_LENGTH_LOKI97_32 = 32;
    public static final int BLOCK_LENGTH_LOKI97 = 16;

    public LOKI97(byte[] key) {
        int lenKey = key.length;
        if (!(lenKey == 16 || lenKey == 24|| lenKey == 32)) {
            throw new IllegalArgumentExceptionWithLog("LOKI97: Param key must be of size 16, 24 or 32", log);
        }

        EncryptionConversionFeistelFunctionLOKI97 encryptionConversionFeistelFunctionLOKI97 = new EncryptionConversionFeistelFunctionLOKI97();
        RoundKeyGenerationLOKI97 roundKeyGenerationLOKI97 = new RoundKeyGenerationLOKI97(encryptionConversionFeistelFunctionLOKI97);
        this.cipherEncryption = new FeistelCipherLOKI97(roundKeyGenerationLOKI97, encryptionConversionFeistelFunctionLOKI97, key);
        this.cipherDecryption = new FeistelCipherLOKI97(roundKeyGenerationLOKI97, encryptionConversionFeistelFunctionLOKI97, key);
    }

    @Override
    public byte[] encrypt(byte[] input) {
        return cipherEncryption.methodForConstructingBlockCiphersEncryption(input, roundCount);
    }

    @Override
    public byte[] decrypt(byte[] input) {
        return cipherDecryption.methodForConstructingBlockCiphersDecryption(input, roundCount);
    }

    @Override
    public int getAlgorithmBlockForEncryption() {
        return BLOCK_LENGTH_LOKI97;
    }

    @Override
    public int getAlgorithmBlockForDecryption() {
        return BLOCK_LENGTH_LOKI97;
    }
}
