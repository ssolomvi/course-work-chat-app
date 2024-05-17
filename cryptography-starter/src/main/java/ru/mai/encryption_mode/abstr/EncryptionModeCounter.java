package ru.mai.encryption_mode.abstr;

import ru.mai.encryption_mode.EncryptionMode;

// CTR, RD
public interface EncryptionModeCounter extends EncryptionMode {
    /**
     * Encrypts {@code input} in multithreading mode with use of pre-counting some data.
     * <p>
     * Is compatible with next modes:
     * <p>
     * {@link ru.mai.encryption_mode.impl.CTR},
     * {@link ru.mai.encryption_mode.impl.RandomDelta}
     *
     * @param input       block which is byte array to encrypt
     * @param blockNumber number of block to encrypt
     * @return Encrypted block
     */
    byte[] encrypt(byte[] input, int blockNumber);

    /**
     * Decrypts {@code input} in multithreading mode with use of pre-counting some data.
     * <p>
     * Is compatible with next modes:
     * <p>
     * {@link ru.mai.encryption_mode.impl.CTR},
     * {@link ru.mai.encryption_mode.impl.RandomDelta}
     *
     * @param input       block which is byte array to decrypt
     * @param blockNumber number of block to decrypt
     * @return Decrypted block
     */
    byte[] decrypt(byte[] input, int blockNumber);
}
