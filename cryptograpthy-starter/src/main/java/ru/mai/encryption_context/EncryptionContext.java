package ru.mai.encryption_context;

import java.nio.file.Path;

/**
 * An interface that provides a description of the functionality for performing encryption and
 * decryption using a symmetric algorithm (method parameter: [de]cipherable block
 * (byte array)) with pre-configured round keys using a separate method (parameter
 * method: [de]encryption key (byte array));
 * */
public interface EncryptionContext {
    byte[] encrypt(byte[] input);

    // todo: return future instead, so this method will be accessible in multithreading
    void encrypt(Path input, Path output);

    byte[] decrypt(byte[] input);

    void decrypt(Path input, Path output);
}