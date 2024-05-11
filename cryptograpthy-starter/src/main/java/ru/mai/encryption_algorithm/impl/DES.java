package ru.mai.encryption_algorithm.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mai.cipher.FeistelCipher;
import ru.mai.encryption_algorithm.EncryptionAlgorithmForDESAndDEAL;
import ru.mai.encryption_conversion.EncryptionConversionFeistelFunction;
import ru.mai.exceptions.IllegalArgumentExceptionWithLog;
import ru.mai.round_keys.DES.RoundKeyGenerationDESDirect;
import ru.mai.round_keys.DES.RoundKeyGenerationDESInverse;
import ru.mai.utils.Operations;
import ru.mai.utils.Permutation;

public class DES extends EncryptionAlgorithmForDESAndDEAL {
    private static final Logger log = LoggerFactory.getLogger(DES.class);
    private int roundCount = 16;
    private final FeistelCipher cipherEncryption;
    private final FeistelCipher cipherDecryption;
    public static final int BLOCK_LENGTH = 8;
    public static final int KEY_SIZE = 7;

    /**
     * Creates a new instance of class DES with modified count of rounds
     * @param key array of bytes used for creating round keys. Must be of length 7.
     * @param roundCount count of round for Feistel Cipher
     */
    public DES(byte[] key, int roundCount) {
        super();
        if (!(key.length == KEY_SIZE || key.length == 8)) {
            throw new IllegalArgumentExceptionWithLog("Key in DES must be of size 7", log);
        }
        if (roundCount < 1) {
            throw new IllegalArgumentExceptionWithLog("Passed param roundCount < 1", log);
        }
        this.roundCount = roundCount;

        this.cipherEncryption = new FeistelCipher(new RoundKeyGenerationDESDirect(), new EncryptionConversionFeistelFunction(), key);
        this.cipherDecryption = new FeistelCipher(new RoundKeyGenerationDESInverse(), new EncryptionConversionFeistelFunction(), key);
    }

    /**
     * Creates a new instance of class DES with modified count of rounds
     * @param key array of bytes used for creating round keys. Must be of length 7.
     */
    public DES(byte[] key) {
        super();
        if (!(key.length == KEY_SIZE || key.length == 8)) {
            throw new IllegalArgumentExceptionWithLog("Key in DES must be of size 7", log);
        }

        this.cipherEncryption = new FeistelCipher(new RoundKeyGenerationDESDirect(), new EncryptionConversionFeistelFunction(), key);
        this.cipherDecryption = new FeistelCipher(new RoundKeyGenerationDESInverse(), new EncryptionConversionFeistelFunction(), key);
    }

    /**
     * 1. Input: block 64 bit, key 64 bit
     * <p>
     * 2. Make initial permutation {IP}: table1
     * <p>
     * 3. Use FeistelCipherImpl with EncryptionConversionFeistelFunction && RoundKeyGenerationDES
     * <p>
     * 4. Make initial^(-1) permutation {IP^(-1)}
     * @param input array of bytes of size {@code BLOCK_SIZE = 8}
     * @return Encoded input of size {@code BLOCK_SIZE = 8}
     */
    @Override
    public byte[] encrypt(byte[] input) {
        if (input.length != BLOCK_LENGTH) {
            throw new IllegalArgumentExceptionWithLog("Passed param input len is not appropriate for this algorithm", log);
        }

        byte[] initialPermutationArr = Permutation.permute(input, initialPermutation,
                Operations.IndexingRule.FROM_MOST_TO_LEAST_START_WITH_1);

        byte[] ciphered = cipherEncryption.methodForConstructingBlockCiphers(initialPermutationArr, roundCount);

        return Permutation.permute(ciphered, reverseInitialPermutation,
                Operations.IndexingRule.FROM_MOST_TO_LEAST_START_WITH_1);
    }


    @Override
    public byte[] decrypt(byte[] input) {
        if (input.length != BLOCK_LENGTH) {
            throw new IllegalArgumentExceptionWithLog("Passed param input len is not appropriate for this algorithm", log);
        }

        byte[] initialPermutationArr = Permutation.permute(input, initialPermutation,
                Operations.IndexingRule.FROM_MOST_TO_LEAST_START_WITH_1);

        byte[] ciphered = cipherDecryption.methodForConstructingBlockCiphers(initialPermutationArr, roundCount);

        return Permutation.permute(ciphered, reverseInitialPermutation,
                Operations.IndexingRule.FROM_MOST_TO_LEAST_START_WITH_1);
    }

    @Override
    public int getAlgorithmBlockForEncryption() {
        return BLOCK_LENGTH;
    }

    @Override
    public int getAlgorithmBlockForDecryption() {
        return BLOCK_LENGTH;
    }
}
