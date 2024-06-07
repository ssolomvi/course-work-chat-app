package ru.mai.encryption_mode.abstr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mai.encryption_algorithm.EncryptionAlgorithm;
import ru.mai.encryption_mode.EncryptionMode;
import ru.mai.exceptions.IllegalArgumentExceptionWithLog;

public abstract class EncryptionModeAbstract implements EncryptionMode {
    private static final Logger log = LoggerFactory.getLogger(EncryptionModeAbstract.class);
    protected EncryptionAlgorithm algorithm;
    protected byte[] initVector;

    protected EncryptionModeAbstract(EncryptionAlgorithm algorithm) {
        if (algorithm == null) {
            throw new IllegalArgumentExceptionWithLog("Passed param algorithm is null", log);
        }
        this.algorithm = algorithm;
    }

    protected EncryptionModeAbstract(EncryptionAlgorithm algorithm, byte[] initVector) {
        if (algorithm == null) {
            throw new IllegalArgumentExceptionWithLog("Passed param algorithm is null", log);
        }
        if (initVector == null) {
            throw new IllegalArgumentExceptionWithLog("Passed param initVector in null", log);
        }
        this.algorithm = algorithm;
        if (initVector.length < (Math.max(algorithm.getAlgorithmBlockForEncryption(), algorithm.getAlgorithmBlockForDecryption()))) {
            throw new IllegalArgumentExceptionWithLog("Passed param initVector must be not less than algorithm block for encryption or decryption", log);
        }

        this.initVector = initVector;
    }

    @Override
    public int getAlgorithmBlockForEncryption() {
        return algorithm.getAlgorithmBlockForEncryption();
    }

    @Override
    public int getAlgorithmBlockForDecryption() {
        return algorithm.getAlgorithmBlockForDecryption();
    }
}
