package ru.mai.encryption_algorithm.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mai.encryption_algorithm.EncryptionAlgorithm;
import ru.mai.exceptions.IllegalArgumentExceptionWithLog;
import ru.mai.round_keys.Rijndael.RoundKeyGenerationRijndael;
import ru.mai.utils.GaloisFieldPolynomialOperations;
import ru.mai.utils.Operations;

public class Rijndael implements EncryptionAlgorithm {
    private static final Logger log = LoggerFactory.getLogger(Rijndael.class);
    private boolean isInitiated = false;
    private int lenBlock; // in bytes
    private final byte[] key;
    private final int Nk; // number of columns in key
    private int Nc; // number of columns in state
    private int Nn; // count of rounds
    private final byte polynomialIrreducibleWithDegreeEIGHT;
    public byte[] sBox;
    public byte[] sBoxInverse;
    private byte[][] roundKeys; // array of words (columns)
    public static final int KEY_LENGTH16 = 16;
    public static final int KEY_LENGTH24 = 24;
    public static final int KEY_LENGTH32 = 32;

    /**
     * Rijndael constructor.
     *
     * @param key                                  byte[] with size 16, 24 or 32.
     * @param polynomialIrreducibleWithDegreeEIGHT byte with a binary polynomial that must be irreducible.
     * @param lenBlock                             length of encryption/decryption input block; can be changed later.
     */
    public Rijndael(byte[] key, byte polynomialIrreducibleWithDegreeEIGHT, int lenBlock) {
        int lenKey = key.length;
        if (!(lenKey == 16 || lenKey == 24 || lenKey == 32)) {
            throw new IllegalArgumentExceptionWithLog("Rijndael: Param key must be 128, 192 or 256 bits", log);
        }

        if (!GaloisFieldPolynomialOperations.isPolynomialWithDegreeEIGHTIrreducible(
                polynomialIrreducibleWithDegreeEIGHT)) {
            throw new IllegalArgumentExceptionWithLog("Rijndael: Param polynomialIrreducibleWithDegreeEIGHT must be irreducible", log);
        }

        this.polynomialIrreducibleWithDegreeEIGHT = polynomialIrreducibleWithDegreeEIGHT;

        initSBoxes();

        this.Nk = lenKey / 4;
        this.key = key;
        this.lenBlock = lenBlock;
    }

    public void changeLenBlock(int newLenBlock) {
        isInitiated = false;
        lenBlock = newLenBlock;
        checkInitiated(lenBlock);
    }

    private static boolean getBit(byte b, int index) {
        // FROM_LEAST_TO_MOST_START_WITH_0
        return (b & 0xff & (1 << index)) != 0;
    }

    private void initSBoxes() {
        // b -> GF(2^8) b^-1
        // A * b^-1 + f, f = 0x63
        // p(x) = from constructor

        byte c = 0x63;
        sBox = new byte[256];

        // init S box
        for (int i = 0; i < 256; i++) {
            byte b = (byte) (0xff & i);
            byte bInv = GaloisFieldPolynomialOperations.invert(b, polynomialIrreducibleWithDegreeEIGHT);
            byte bModified = 0;
            for (int j = 0; j < 8; j++) {
                int bitToSet = (getBit(bInv, j)
                        ^ getBit(bInv, (j + 4) % 8)
                        ^ getBit(bInv, (j + 5) % 8)
                        ^ getBit(bInv, (j + 6) % 8)
                        ^ getBit(bInv, (j + 7) % 8)
                        ^ getBit(c, j)) ? 1 : 0;

                bModified |= (byte) (bitToSet << j);
            }
            sBox[i] = bModified;
        }
        // init S box inverse
        sBoxInverse = new byte[256];

        for (int i = 0; i < 256; i++) {
            sBoxInverse[sBox[i] & 0xff] = (byte) (i & 0xff);
        }
    }

    private void generateRoundKeys() {
        RoundKeyGenerationRijndael keyGen = new RoundKeyGenerationRijndael(key, Nc, Nk, Nn, sBox,
                polynomialIrreducibleWithDegreeEIGHT);

        roundKeys = keyGen.generateRoundKeys(key);
    }

    private void checkInitiated(int lenInput) {
        if (!(lenInput == 16 || lenInput == 24 || lenInput == 32)) {
            throw new IllegalArgumentExceptionWithLog("checkInitiated: Param input must be 128, 192 or 256 bits", log);
        }

        if (!isInitiated) {
            Nc = lenInput / 4;
            if (Nc == 8 || Nk == 8) {
                Nn = 14;
            } else if (Nc == 6 || Nk == 6) {
                Nn = 12;
            } else {
                Nn = 10;
            }

            generateRoundKeys();
            isInitiated = true;
            return;
        }

        if (Nc == 8 && lenInput != 32) {
            throw new IllegalArgumentExceptionWithLog("encrypt: Param input must be 256 bits", log);
        } else if (Nc == 6 && lenInput != 24) {
            throw new IllegalArgumentExceptionWithLog("encrypt: Param input must be 192 bits", log);
        } else if (Nc == 4 && lenInput != 16) {
            throw new IllegalArgumentExceptionWithLog("encrypt: Param input must be 128 bits", log);
        }
    }

    private int getIndexByRowColumnOfState(int row, int column) {
        return row * Nc + column;
    }

    /**
     * The SubBytes transformation is a non-linear byte substitution that acts on every byte of
     * the state in isolation to produce a new byte value using an S-box substitution table
     *
     * @param state byte[4 * Nc].
     */
    private void byteSub(byte[] state) {
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < Nc; c++) {
                state[getIndexByRowColumnOfState(r, c)] = sBox[state[getIndexByRowColumnOfState(r, c)] & 0xff];
            }
        }
    }

    private void byteSubInv(byte[] state) {
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < Nc; c++) {
                state[getIndexByRowColumnOfState(r, c)] = sBoxInverse[state[getIndexByRowColumnOfState(r, c)] & 0xff];
            }
        }
    }

    private void shiftNthRowForK(byte[] state, int n, int k) {
        byte[] row = new byte[Nc];

        // copy n-th row
        int nthRowIdx = n * Nc;
        System.arraycopy(state, nthRowIdx, row, 0, Nc);

        row = Operations.cyclicShiftLeftBytes(row, k);

        System.arraycopy(row, 0, state, nthRowIdx, Nc);
    }

    private void invShiftNthRowForK(byte[] state, int n, int k) {
        byte[] row = new byte[Nc];

        // copy n-th row
        int nthRowIdx = n * Nc;
        System.arraycopy(state, nthRowIdx, row, 0, Nc);

        row = Operations.cyclicShiftRightBytes(row, k);

        System.arraycopy(row, 0, state, nthRowIdx, Nc);
    }

    /**
     * The ShiftRows transformation operates individually on each of the last three rows of the
     * state by cyclically shifting the bytes in the row with specially rule.
     *
     * @param state byte[4 * Nc].
     */
    private void shiftRow(byte[] state) {
        // the rows of the State are cyclically shifted over different offsets. Row 0 is not
        // shifted, Row 1 is shifted over C1 bytes, row 2 over C2 bytes and row 3 over C3 bytes.

        // The shift offsets C1, C2 and C3 depend on the block length Nb.
        // for C1 we always shift byte 1,
        // for C2 we shift by 2 if Nb == 4 || Nb == 6 and by 3 if Nb == 8
        // for C3 we shift by 3 if Nb == 4 || Nb == 6 and by 4 if Nb == 8

        if (state.length == 16 || state.length == 24) {
            shiftNthRowForK(state, 1, 1);
            shiftNthRowForK(state, 2, 2);
            shiftNthRowForK(state, 3, 3);
        } else {
            shiftNthRowForK(state, 1, 1);
            shiftNthRowForK(state, 2, 3);
            shiftNthRowForK(state, 3, 4);
        }
    }

    private void shiftRowInv(byte[] state) {
        // the rows of the State are cyclically shifted over different offsets. Row 0 is not
        // shifted, Row 1 is shifted over C1 bytes, row 2 over C2 bytes and row 3 over C3 bytes.

        // The shift offsets C1, C2 and C3 depend on the block length Nb.
        // for C1 we always shift byte 1,
        // for C2 we shift by 2 if Nb == 4 || Nb == 6 and by 3 if Nb == 8
        // for C3 we shift by 3 if Nb == 4 || Nb == 6 and by 4 if Nb == 8

        if (state.length == 16 || state.length == 24) {
            invShiftNthRowForK(state, 1, 1);
            invShiftNthRowForK(state, 2, 2);
            invShiftNthRowForK(state, 3, 3);
        } else {
            invShiftNthRowForK(state, 1, 1);
            invShiftNthRowForK(state, 2, 3);
            invShiftNthRowForK(state, 3, 4);
        }
    }

    /**
     * In mixColumn, the columns of the state are considered as polynomials over GF(2^8) and multiplied modulo
     * x^4 + 1 with a fixed polynomial c(x): = ‘03’ x^3 + ‘01’ x^2 + ‘01’ x + ‘02’ .
     * The inverse to c(x): d(x ) = ‘0B’ x^3 + ‘0D’ x^2 + ‘09’ x + ‘0E’ .
     *
     * @param state byte[4 * Nc].
     */
    private void mixColumns(byte[] state) {
        byte[] t = new byte[4]; // curr column

        for (int c = 0; c < Nc; c++) {
            for (int r = 0; r < 4; r++) {
                t[r] = state[getIndexByRowColumnOfState(r, c)];
            }

            for (int r = 0; r < 4; r++) {
                state[getIndexByRowColumnOfState(r, c)] = (byte) (
                        (GaloisFieldPolynomialOperations.multiplyByModulo((byte) 0x02, t[r], polynomialIrreducibleWithDegreeEIGHT) & 0xff)
                                ^ (GaloisFieldPolynomialOperations.multiplyByModulo((byte) 0x03, t[(r + 1) % 4], polynomialIrreducibleWithDegreeEIGHT) & 0xff)
                                ^ ((t[(r + 2) % 4]) & 0xff) ^ (t[(r + 3) % 4] & 0xff));
            }
        }
    }

    private void mixColumnsInv(byte[] state) {
        byte[] t = new byte[4]; // curr column
        for (int c = 0; c < Nc; c++) {
            for (int r = 0; r < 4; r++) {
                t[r] = state[getIndexByRowColumnOfState(r, c)];
            }
            for (int r = 0; r < 4; r++) {
                state[getIndexByRowColumnOfState(r, c)] = (byte) (
                        (GaloisFieldPolynomialOperations.multiplyByModulo((byte) 0x0e, t[r], polynomialIrreducibleWithDegreeEIGHT) & 0xff)
                                ^ (GaloisFieldPolynomialOperations.multiplyByModulo((byte) 0x0b, t[(r + 1) % 4], polynomialIrreducibleWithDegreeEIGHT) & 0xff)
                                ^ (GaloisFieldPolynomialOperations.multiplyByModulo((byte) 0x0d, t[(r + 2) % 4], polynomialIrreducibleWithDegreeEIGHT) & 0xff)
                                ^ (GaloisFieldPolynomialOperations.multiplyByModulo((byte) 0x09, t[(r + 3) % 4], polynomialIrreducibleWithDegreeEIGHT) & 0xff));
            }
        }
    }

    /**
     * In the xorRoundKey transformation Nc words from the key schedule (the round key
     * described later) are each added (XOR’d) into the columns of the state.
     *
     * @param state byte[4 * Nc].
     * @param round current round number (from 0).
     * @return state ^= roundKeys[round].
     */
    private byte[] xorRoundKey(byte[] state, int round) {
        for (int c = 0; c < Nc; c++) { // columns
            for (int r = 0; r < 4; r++) { // rows
                state[getIndexByRowColumnOfState(r, c)] = (byte) ((state[getIndexByRowColumnOfState(r, c)]) ^ (roundKeys[round * Nc + c][3 - r]));
            }
        }

        return state;
    }

    private void roundEncryption(byte[] state, int roundNumber) {
        byteSub(state);
        shiftRow(state);
        mixColumns(state);
        System.arraycopy(xorRoundKey(state, roundNumber), 0, state, 0, state.length);
    }

    private void finalRoundEncryption(byte[] state) {
        byteSub(state);
        shiftRow(state);
        xorRoundKey(state, Nn);
    }

    @Override
    public byte[] encrypt(byte[] input) {
        byte[] state = input.clone();
        checkInitiated(state.length);

        xorRoundKey(state, 0);
        for (int i = 1; i <= Nn - 1; i++) {
            roundEncryption(state, i);
        }

        finalRoundEncryption(state);

        return state;
    }

    private void roundDecryption(byte[] state, int roundNumber) {
        shiftRowInv(state);
        byteSubInv(state);
        System.arraycopy(xorRoundKey(state, roundNumber), 0, state, 0, state.length);
        mixColumnsInv(state);
    }

    private void finalRoundDecryption(byte[] state) {
        shiftRowInv(state);
        byteSubInv(state);
        xorRoundKey(state, 0);
    }

    @Override
    public byte[] decrypt(byte[] input) {
        byte[] state = input.clone();
        checkInitiated(state.length);

        xorRoundKey(state, Nn);
        for (int i = Nn - 1; i >= 1; i--) {
            roundDecryption(state, i);
        }

        finalRoundDecryption(state);

        return state;
    }

    @Override
    public int getAlgorithmBlockForEncryption() {
        return lenBlock;
    }

    @Override
    public int getAlgorithmBlockForDecryption() {
        return lenBlock;
    }
}
