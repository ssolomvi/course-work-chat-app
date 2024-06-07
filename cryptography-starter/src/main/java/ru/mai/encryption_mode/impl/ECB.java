package ru.mai.encryption_mode.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mai.encryption_algorithm.EncryptionAlgorithm;
import ru.mai.encryption_mode.abstr.EncryptionModeAbstract;
import ru.mai.exceptions.IllegalArgumentExceptionWithLog;

// C_i = E_k (P_i, k)
// P_i = D_k (C_i, k)
public class ECB extends EncryptionModeAbstract {
    private static final Logger log = LoggerFactory.getLogger(ECB.class);

    public ECB(EncryptionAlgorithm algorithm) {
        super(algorithm);
    }

    /**
     * Encrypts array of bytes according to formula:
     * <p>
     * C_i = E(P_i)
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

            byte[] encrypted = algorithm.encrypt(toEncrypt);

            System.arraycopy(encrypted, 0,
//            System.arraycopy(algorithm.encrypt(toEncrypt), 0,
                    encryptResult, idxOutput,
                    getAlgorithmBlockForDecryption());
        }

        return encryptResult;
    }

    /**
     * Decrypts array of bytes according to formula:
     * <p>
     * P_i = D(C_i)
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

        for (int i = 0, idxInput = 0, idxOutput = 0;
             i < countOfBlocks;
             i++, idxInput += getAlgorithmBlockForDecryption(), idxOutput += getAlgorithmBlockForEncryption()) {

            System.arraycopy(input, idxInput,
                    toDecrypt, 0,
                    getAlgorithmBlockForDecryption());

            System.arraycopy(algorithm.decrypt(toDecrypt), 0,
                    decryptResult, idxOutput,
                    getAlgorithmBlockForEncryption());
        }

        return decryptResult;
    }

}
