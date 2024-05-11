package ru.mai.encryption_mode.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mai.encryption_algorithm.EncryptionAlgorithm;
import ru.mai.encryption_mode.abstr.EncryptionModeAbstract;
import ru.mai.encryption_mode.abstr.EncryptionModeWithInitVector;
import ru.mai.exceptions.IllegalArgumentExceptionWithLog;
import ru.mai.utils.Operations;

/**
 * Propagating Cipher Block Chaining is a CBC mode option.
 */
public class PCBC extends EncryptionModeAbstract implements EncryptionModeWithInitVector {
    private static final Logger log = LoggerFactory.getLogger(PCBC.class);
    private byte[] previousEncryptionOpen; // previous block of open text for encrypt method
    private byte[] previousEncryptionCiphered; // previous block of ciphered text for encrypt method
    private byte[] previousDecryptionOpen; // previous block of open text for decrypt method
    private byte[] previousDecryptionCiphered; // previous block of ciphered text for decrypt method

    private boolean encryptionAccessed;
    private boolean decryptionAccessed;

    public PCBC(EncryptionAlgorithm algorithm, byte[] initVector) {
        super(algorithm, initVector);
        invokeNextAsNew();
    }

    /**
     * Encrypts array of bytes according to formula:
     * <p>
     * P<sub>{@code 0}</sub> xor C<sub>{@code 0}</sub> = IV
     * <p>
     * C<sub>{@code i}</sub> = E<sub>{@code k}</sub> (P<sub>{@code i}</sub> xor P<sub>{@code i - 1}</sub> xor C<sub>{@code i - 1}</sub>)
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

        // P_0 xor C_0 = IV
        // C_i = E(P_i xor P_i-1 xor C_i-1)
        for (int i = 0, idxInput = 0, idxOutput = 0;
             i < countOfBlocks;
             i++, idxInput += getAlgorithmBlockForEncryption(), idxOutput += getAlgorithmBlockForDecryption()) {

            System.arraycopy(input, idxInput,
                    toEncrypt, 0,
                    getAlgorithmBlockForEncryption());

            if (!encryptionAccessed) {
                previousEncryptionCiphered = algorithm.encrypt(Operations.xor(toEncrypt, initVector));
                encryptionAccessed = true;
            } else {
                previousEncryptionCiphered = algorithm.encrypt(Operations.xor(toEncrypt,
                        Operations.xor(previousEncryptionOpen, previousEncryptionCiphered)));
            }

            previousEncryptionOpen = toEncrypt.clone();

            System.arraycopy(previousEncryptionCiphered, 0,
                    encryptResult, idxOutput,
                    getAlgorithmBlockForDecryption());
        }

        return encryptResult;
    }

    /**
     * Decrypts array of bytes according to formula:
     * <p>
     * P<sub>{@code 0}</sub> xor C<sub>{@code 0}</sub> = IV
     * <p>
     * P<sub>{@code i}</sub> = D<sub>{@code k}</sub> (C<sub>{@code i}</sub>) xor C<sub>{@code i - 1}</sub> xor P<sub>{@code i - 1}</sub>
     */
    @Override
    public byte[] decrypt(byte[] input) {
        if (input.length % getAlgorithmBlockForDecryption() != 0) {
            throw new IllegalArgumentExceptionWithLog("Input block size is not appropriate for this algorithm", log);
        }
        if (input.length == 0) {
            return new byte[0];
        }

        int countOfBlocks = input.length / getAlgorithmBlockForDecryption();
        byte[] decryptResult = new byte[countOfBlocks * getAlgorithmBlockForEncryption()];

        byte[] toDecrypt = new byte[getAlgorithmBlockForDecryption()];

        // P_0 xor C_0 = IV
        // P_i = D(C_i) xor P_i-1 xor C_i-1
        for (int i = 0, idxInput = 0, idxOutput = 0;
             i < countOfBlocks;
             i++, idxInput += getAlgorithmBlockForDecryption(), idxOutput += getAlgorithmBlockForEncryption()) {

            System.arraycopy(input, idxInput,
                    toDecrypt, 0,
                    getAlgorithmBlockForDecryption());

            if (!decryptionAccessed) {
                previousDecryptionOpen = Operations.xor(algorithm.decrypt(toDecrypt), initVector);
                decryptionAccessed = true;
            } else {
                previousDecryptionOpen = Operations.xor(algorithm.decrypt(toDecrypt),
                        Operations.xor(previousDecryptionOpen, previousDecryptionCiphered));
            }

            previousDecryptionCiphered = toDecrypt.clone();

            System.arraycopy(previousDecryptionOpen, 0,
                    decryptResult, idxOutput,
                    getAlgorithmBlockForEncryption());
        }

        return decryptResult;
    }

    @Override
    public void invokeNextAsNew() {
        encryptionAccessed = false;
        decryptionAccessed = false;
    }
}
