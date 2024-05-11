package ru.mai.encryption_algorithm;

public interface EncryptionAlgorithm {
    byte[] encrypt(byte[] input);

    byte[] decrypt(byte[] input);

//    byte[] kek(boolean beb_isEncryption);

    /**
     * @return algorithm block (count of bytes), used to pass in method {@code byte[] encrypt(byte[] input)}
     */
    int getAlgorithmBlockForEncryption();

    /**
     * @return algorithm block (count of bytes), used to pass in method {@code byte[] decrypt(byte[] input)}
     */
    int getAlgorithmBlockForDecryption();
}
