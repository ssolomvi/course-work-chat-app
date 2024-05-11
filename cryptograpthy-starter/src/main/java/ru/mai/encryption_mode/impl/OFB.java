package ru.mai.encryption_mode.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mai.encryption_algorithm.EncryptionAlgorithm;
import ru.mai.encryption_mode.abstr.EncryptionModeAbstract;
import ru.mai.encryption_mode.abstr.EncryptionModeWithInitVector;
import ru.mai.exceptions.IllegalArgumentExceptionWithLog;
import ru.mai.utils.Operations;

// todo: encrypt() and decrypt() uses algorithm.E, in RSA: block A = algorithm.E(B), A.length != B.length
public class OFB extends EncryptionModeAbstract implements EncryptionModeWithInitVector {
    private static final Logger log = LoggerFactory.getLogger(OFB.class);

    byte[] previousEncryptionO;
    byte[] previousDecryptionO;

    public OFB(EncryptionAlgorithm algorithm, byte[] initVector) {
        super(algorithm, initVector);
        invokeNextAsNew();
    }

    /**
     * Encrypts array of bytes according to formula:
     * <p>
     * O_0 = IV
     * <p>
     * O_i = E(O_i-1)
     * <p>
     * C_i = P_i xor O_i
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

        for (int i = 0, idxInput = 0, idxOutput = 0;
             i < countOfBlocks;
             i++, idxInput += getAlgorithmBlockForEncryption(), idxOutput += getAlgorithmBlockForDecryption()) {

            System.arraycopy(input, idxInput,
                    toEncrypt, 0,
                    getAlgorithmBlockForEncryption());

            previousEncryptionO = algorithm.encrypt(previousEncryptionO);

            System.arraycopy(Operations.xor(toEncrypt, previousEncryptionO), 0,
                    encryptResult, idxOutput,
                    getAlgorithmBlockForDecryption());
        }

        return encryptResult;
    }

    /**
     * Decrypts array of bytes according to formula:
     * <p>
     * O_0 = IV
     * <p>
     * O_i = E(O_i-1)
     * <p>
     * P_i = C_i xor O_i
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

        // O_0 = IV
        // O_i = E(O_i-1)
        // P_i = C_i xor O_i
        for (int i = 0, idxInput = 0, idxOutput = 0;
             i < countOfBlocks;
             i++, idxInput += getAlgorithmBlockForDecryption(), idxOutput += getAlgorithmBlockForEncryption()) {

            System.arraycopy(input, idxInput,
                    toDecrypt, 0,
                    getAlgorithmBlockForDecryption());

            previousDecryptionO = algorithm.encrypt(previousDecryptionO);

            System.arraycopy(Operations.xor(toDecrypt, previousDecryptionO), 0,
                    decryptResult, idxOutput,
                    getAlgorithmBlockForEncryption());
        }

        return decryptResult;
    }

    @Override
    public void invokeNextAsNew() {
        this.previousEncryptionO = initVector;
        this.previousDecryptionO = initVector;
    }
}
