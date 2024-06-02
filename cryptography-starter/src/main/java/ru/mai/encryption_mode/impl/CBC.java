package ru.mai.encryption_mode.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mai.encryption_algorithm.EncryptionAlgorithm;
import ru.mai.encryption_mode.abstr.EncryptionModeAbstract;
import ru.mai.encryption_mode.abstr.EncryptionModePreviousNeeded;
import ru.mai.exceptions.IllegalArgumentExceptionWithLog;
import ru.mai.utils.Operations;

/**
 * Cipher Block Chaining is one of the encryption modes for a symmetric block cipher using a feedback mechanism.
 */
public class CBC extends EncryptionModeAbstract implements EncryptionModePreviousNeeded {
    private static final Logger log = LoggerFactory.getLogger(CBC.class);

    private byte[] previousEncrypted;

    public CBC(EncryptionAlgorithm algorithm, byte[] initVector) {
        super(algorithm, initVector);
        invokeNextAsNew();
    }

    /**
     * Encrypts array of bytes according to formula:
     * <p>
     * C_0 = IV
     * <p>
     * C_i = E(P_i xor C_i-1)
     */
    @Override
    public byte[] encrypt(byte[] input) {
        if (input.length % getAlgorithmBlockForEncryption() != 0) {
            throw new IllegalArgumentExceptionWithLog("Input block size is not appropriate for this algorithm", log);
        }
        if (input.length == 0) {
            return new byte[0];
        }

        int countOfBlocks = input.length / getAlgorithmBlockForEncryption();
        byte[] encryptResult = new byte[countOfBlocks * getAlgorithmBlockForDecryption()];

        byte[] toEncrypt = new byte[getAlgorithmBlockForEncryption()];

        for (int i = 0, idxInput = 0, idxEncrypted = 0;
             i < countOfBlocks;
             i++, idxInput += getAlgorithmBlockForEncryption(), idxEncrypted += getAlgorithmBlockForDecryption()) {

            System.arraycopy(input, idxInput,
                    toEncrypt, 0,
                    getAlgorithmBlockForEncryption());

            previousEncrypted = algorithm.encrypt(Operations.xor(toEncrypt, previousEncrypted));

            System.arraycopy(previousEncrypted, 0,
                    encryptResult, idxEncrypted,
                    getAlgorithmBlockForDecryption());

        }

        return encryptResult;
    }

    @Override
    public byte[] decrypt(byte[] input) {
        String errorString = "Method byte[] decrypt(byte[] input) cannot be invoked in class " + getClass().getName();
        log.error(errorString);
        throw new UnsupportedOperationException(errorString);
    }

    /**
     * Decrypts array of bytes according to formula:
     * <p>
     * C_0 = IV
     * <p>
     * P_i = D(C_i) xor C_i-1
     */
    @Override
    public byte[] decrypt(byte[] curr, byte[] prev) {
        // if invoked as new and no iteration has been done, use previousDecrypted instead of prev
        if (curr.length % getAlgorithmBlockForDecryption() != 0) {
            throw new IllegalArgumentExceptionWithLog("Input block size is not appropriate for this algorithm", log);
        }
        if (curr.length == 0) {
            return new byte[0];
        }

        if (prev == null) {
            prev = initVector;
        }

        int countOfBlocks = curr.length / getAlgorithmBlockForDecryption();
        byte[] decryptResult = new byte[countOfBlocks * getAlgorithmBlockForEncryption()];

        byte[] toDecrypt = new byte[getAlgorithmBlockForDecryption()];

        for (int i = 0, idxInput = 0, idxDecrypt = 0;
             i < countOfBlocks;
             i++, idxInput += getAlgorithmBlockForDecryption(), idxDecrypt += getAlgorithmBlockForEncryption()) {

            System.arraycopy(curr, idxInput,
                    toDecrypt, 0,
                    getAlgorithmBlockForDecryption());

            System.arraycopy(Operations.xor(algorithm.decrypt(toDecrypt), prev), 0,
                    decryptResult, idxDecrypt,
                    getAlgorithmBlockForEncryption());

            prev = toDecrypt.clone();
        }

        return decryptResult;
    }

    @Override
    public void invokeNextAsNew() {
        this.previousEncrypted = initVector;
    }
}
