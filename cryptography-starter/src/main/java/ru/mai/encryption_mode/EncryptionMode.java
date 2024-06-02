package ru.mai.encryption_mode;

public interface EncryptionMode {
    /**
     * Encrypts array of bytes according to chosen algorithm.
     *
     * @param input Array of bytes to encrypt, its length must be multiple of {@code getAlgorithmBlock()}
     * @return Encrypted array of bytes
     */
    byte[] encrypt(byte[] input);

    /**
     * Decrypts array of bytes according to chosen algorithm.
     *
     * @param input Array of bytes to decrypt, its length must be multiple of {@code getAlgorithmBlock()}
     * @return Decrypted array of bytes
     */
    byte[] decrypt(byte[] input);


    /**
     * @return Block length for chosen algorithm for encryption
     */
    int getAlgorithmBlockForEncryption();

    /**
     * @return Block length for chosen algorithm for decryption
     */
    int getAlgorithmBlockForDecryption();

}
