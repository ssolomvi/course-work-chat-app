package ru.mai.encryption_mode.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mai.encryption_algorithm.EncryptionAlgorithm;
import ru.mai.encryption_mode.abstr.EncryptionModeAbstract;
import ru.mai.encryption_mode.abstr.EncryptionModePreviousNeeded;
import ru.mai.exceptions.IllegalArgumentExceptionWithLog;
import ru.mai.utils.Operations;

// todo: ecnrypt() and decrypt() uses algorithm.E, in RSA: block A = algorithm.E(B), A.length != B.length
public class CFB extends EncryptionModeAbstract implements EncryptionModePreviousNeeded {
    private static final Logger log = LoggerFactory.getLogger(CFB.class);

    private byte[] previousEncrypted;

    public CFB(EncryptionAlgorithm algorithm, byte[] initVector) {
        super(algorithm, initVector);
        invokeNextAsNew();
    }

    /**
     * Encrypts array of bytes according to formula:
     * <p>
     * C_0 = IV
     * <p>
     * C_i = E(C_i-1) xor P_i
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

            previousEncrypted = Operations.xor(algorithm.encrypt(previousEncrypted), toEncrypt);

            System.arraycopy(previousEncrypted, 0,
                    encryptResult, idxOutput,
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
     * P_i = E(C_i-1) xor C_i
     */
    @Override
    public byte[] decrypt(byte[] curr, byte[] prev) {
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

        for (int i = 0, idxInput = 0, idxOutput = 0;
             i < countOfBlocks;
             i++, idxInput += getAlgorithmBlockForDecryption(), idxOutput += getAlgorithmBlockForEncryption()) {

            System.arraycopy(curr, idxInput,
                    toDecrypt, 0,
                    getAlgorithmBlockForDecryption());

            System.arraycopy(Operations.xor(algorithm.encrypt(prev), toDecrypt), 0,
                    decryptResult, idxOutput,
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
