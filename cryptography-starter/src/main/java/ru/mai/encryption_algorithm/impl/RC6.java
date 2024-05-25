package ru.mai.encryption_algorithm.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mai.cipher.FeistelCipherRC6;
import ru.mai.encryption_algorithm.EncryptionAlgorithm;
import ru.mai.exceptions.IllegalArgumentExceptionWithLog;
import ru.mai.round_keys.RC6.RoundKeyGenerationRC6;

public class RC6 implements EncryptionAlgorithm {
    private static final Logger log = LoggerFactory.getLogger(RC6.class);
    private final int roundCount = 20;
    private final FeistelCipherRC6 cipherEncryption;
    private final FeistelCipherRC6 cipherDecryption;
    public static final int KEY_LENGTH_RC6_16 = 16;
    public static final int KEY_LENGTH_RC6_24 = 24;
    public static final int KEY_LENGTH_RC6_32 = 32;
    public static final int BLOCK_LENGTH_RC6_16 = 16;
    public static final int w = 32; // длина машинного слова в битах
    private final int r = 20; // число раундов
    private final int b; // длина ключа в битах

    // RC6-32/20/128
    public RC6(byte[] key) {
        int lenKey = key.length;
        if (lenKey != 16) {
            throw new IllegalArgumentExceptionWithLog("RC6: Param key must be of size 16", log);
        }

        b = lenKey * 8;

        RoundKeyGenerationRC6 roundKeyGenerationRC6 = new RoundKeyGenerationRC6();
        this.cipherEncryption = new FeistelCipherRC6(roundKeyGenerationRC6, key)    ;
        this.cipherDecryption = new FeistelCipherRC6(roundKeyGenerationRC6, key);
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
        return w / 2;
    }

    @Override
    public int getAlgorithmBlockForDecryption() {
        return w / 2;
    }
}
