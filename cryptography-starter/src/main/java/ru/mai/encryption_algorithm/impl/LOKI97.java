package ru.mai.encryption_algorithm.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mai.cipher.FeistelCipherLOKI97;
import ru.mai.encryption_algorithm.EncryptionAlgorithm;
import ru.mai.encryption_conversion.EncryptionConversionFeistelFunctionLOKI97;
import ru.mai.exceptions.IllegalArgumentExceptionWithLog;
import ru.mai.round_keys.LOKI97.RoundKeyGenerationLOKI97Direct;

// doing LOKI97 for 128 bits key and 128 bit block
public class LOKI97 implements EncryptionAlgorithm {
    private static final Logger log = LoggerFactory.getLogger(LOKI97.class);
    private int lenBlock = 16; // in bytes
    private int lenKey; // in bytes
    private FeistelCipherLOKI97 cipherEncryption;
    private FeistelCipherLOKI97 cipherDecryption;


    public LOKI97(byte[] key) {
        int lenKey = key.length;
        if (!(lenKey == 16 || lenKey == 32)) {
            throw new IllegalArgumentExceptionWithLog("Key in LOKI07 must be of size 16 or 32", log);
        }

        this.lenKey = lenKey;
        EncryptionConversionFeistelFunctionLOKI97 encryptionConversionFeistelFunctionLOKI97 = new EncryptionConversionFeistelFunctionLOKI97();
        RoundKeyGenerationLOKI97Direct roundKeyGenerationLOKI97Direct = new RoundKeyGenerationLOKI97Direct(encryptionConversionFeistelFunctionLOKI97);
        this.cipherEncryption = new FeistelCipherLOKI97(roundKeyGenerationLOKI97Direct, encryptionConversionFeistelFunctionLOKI97, key);
        this.cipherDecryption = new FeistelCipherLOKI97(roundKeyGenerationLOKI97Direct, encryptionConversionFeistelFunctionLOKI97, key);
    }

    @Override
    public byte[] encrypt(byte[] input) {
        return cipherEncryption.methodForConstructingBlockCiphersEncryption(input, 16);
    }

    @Override
    public byte[] decrypt(byte[] input) {
        return cipherDecryption.methodForConstructingBlockCiphersDecryption(input, 16);
    }

    @Override
    public int getAlgorithmBlockForEncryption() {
        return lenBlock;
    }

    @Override
    public int getAlgorithmBlockForDecryption() {
        return lenBlock;
    }
}
