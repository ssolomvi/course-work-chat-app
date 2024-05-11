package ru.mai.encryption_algorithm.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mai.encryption_algorithm.EncryptionAlgorithmForDESAndDEAL;
import ru.mai.exceptions.IllegalArgumentExceptionWithLog;
import ru.mai.round_keys.DEAL.RoundKeyGenerationDEALDirect;
import ru.mai.utils.Operations;

public class DEAL extends EncryptionAlgorithmForDESAndDEAL {
    private static final Logger log = LoggerFactory.getLogger(DEAL.class);
    private final DES[] desRound;
    private final int roundCount;
    public static final int BLOCK_LENGTH = 16;
    public static final int KEY_LENGTH16 = 16;
    public static final int KEY_LENGTH24 = 24;
    public static final int KEY_LENGTH32 = 32;

    public DEAL(byte[] key) {
        super();
        if (!(key.length == KEY_LENGTH16 || key.length == KEY_LENGTH24 || key.length == KEY_LENGTH32)) {
            throw new IllegalArgumentExceptionWithLog("Param key for DEAL must be either 16 or 24 or 32 byte length", log);
        }

        if (key.length == 16 || key.length == 24) {
            roundCount = 6;
        } else {
            roundCount = 8;
        }

        RoundKeyGenerationDEALDirect directKeyGen = new RoundKeyGenerationDEALDirect();

        byte[][] roundKeysDirect = directKeyGen.generateRoundKeys(key);

        this.desRound = new DES[roundCount];
        for (int i = 0; i < roundCount; i++) {
            desRound[i] = new DES(roundKeysDirect[i]);
        }
    }

    /// input is 128 bit == 16 byte
    @Override
    public byte[] encrypt(byte[] input) {
        if (input == null || input.length != BLOCK_LENGTH) {
            throw new IllegalArgumentExceptionWithLog("encrypt(byte[] input): Param input must be 16 bytes length", log);
        }

        byte[] left = new byte[BLOCK_LENGTH / 2];
        byte[] right = new byte[BLOCK_LENGTH / 2];
        System.arraycopy(input, 0, left, 0, BLOCK_LENGTH / 2);
        System.arraycopy(input, BLOCK_LENGTH / 2, right, 0, BLOCK_LENGTH / 2);

        for (int i = 0; i < roundCount; i++) {
            byte[] tmp = Operations.xor(desRound[i].encrypt(left), right);
            right = left;
            left = tmp;
        }

        byte[] tmp = right;
        right = left;
        left = tmp;

        return Operations.mergeByteArrays(left, right);
    }

    @Override
    public byte[] decrypt(byte[] input) {
        if (input == null || input.length != BLOCK_LENGTH) {
            throw new IllegalArgumentExceptionWithLog("decrypt(byte[] input): Param input must be 16 bytes length", log);
        }

        byte[] left = new byte[BLOCK_LENGTH / 2];
        byte[] right = new byte[BLOCK_LENGTH / 2];
        System.arraycopy(input, 0, left, 0, BLOCK_LENGTH / 2);
        System.arraycopy(input, BLOCK_LENGTH / 2, right, 0, BLOCK_LENGTH / 2);

        for (int i = roundCount - 1; i > -1; i--) {
            byte[] tmp = Operations.xor(desRound[i].encrypt(left), right);
            right = left;
            left = tmp;
        }

        byte[] tmp = right;
        right = left;
        left = tmp;

        return Operations.mergeByteArrays(left, right);
    }

    @Override
    public int getAlgorithmBlockForEncryption() {
        return BLOCK_LENGTH;
    }

    @Override
    public int getAlgorithmBlockForDecryption() {
        return BLOCK_LENGTH;
    }
}
