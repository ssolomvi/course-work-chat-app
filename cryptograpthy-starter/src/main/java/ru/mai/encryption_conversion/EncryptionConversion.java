package ru.mai.encryption_conversion;

/**
 * An interface that provides a description of the functionality for performing encryption
 * transformations (method parameters: input block - byte array, round key -
 * byte array, result: output block - byte array);
 * */
public interface EncryptionConversion {
    public byte[] encrypt(byte[] input, byte[] roundKey);
}
