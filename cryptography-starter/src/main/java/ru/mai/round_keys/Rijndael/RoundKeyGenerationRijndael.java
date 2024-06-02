package ru.mai.round_keys.Rijndael;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mai.exceptions.IllegalArgumentExceptionWithLog;
import ru.mai.round_keys.RoundKeyGeneration;
import ru.mai.utils.GaloisFieldPolynomialOperations;
import ru.mai.utils.Operations;

public class RoundKeyGenerationRijndael implements RoundKeyGeneration {
    private static final Logger log = LoggerFactory.getLogger(RoundKeyGenerationRijndael.class);
    private final int Nc; // number of columns of block
    private final int Nk; // number of columns of key
    private final int Nn; // number of rounds
    private final byte[] SBox;
    private byte[][] RCon;
    private final byte polynomialIrreducibleWithDegreeEIGHT;

    /**
     * Initiated the {@code RCon} - array of words (word == {@code byte[4]}). The word array {@code RCon[i]}
     * contains the values {@code [0, 0, 0, x^(i-1)]} with {@code x^(i-1)} being the powers of {@code x}
     * in the field GF(256) (note that the index {@code i} starts at 1).
     */
    private void initRCon() {
        int RConLen = Nc * (Nn + 1) / Nk + 1;
        RCon = new byte[RConLen][4];
        byte x = 1;
        for (int i = 1; i < RConLen; i++) {
            RCon[i][3] = x;
            x = GaloisFieldPolynomialOperations.multiplyByModulo(x, (byte) 2, polynomialIrreducibleWithDegreeEIGHT);
        }
    }

    /**
     * @param key                                  Rijndael key
     * @param Nc                                   number of columns in input block
     * @param Nk                                   number of columns
     * @param Nn                                   number of rounds
     * @param SBox                                 SBox for this rijndael's irreducible polynomial
     * @param polynomialIrreducibleWithDegreeEIGHT polynomial that must be irreducible in GF(256)
     */
    public RoundKeyGenerationRijndael(byte[] key, int Nc, int Nk, int Nn, byte[] SBox,
                                      byte polynomialIrreducibleWithDegreeEIGHT) {
        int lenKey = key.length;
        if (!(lenKey == 16 || lenKey == 24 || lenKey == 32)) {
            throw new IllegalArgumentExceptionWithLog(
                    "RoundKeyGenerationRijndael: Param key must be 128, 192 or 256 bits", log);
        }

        this.Nc = Nc;
        this.Nk = Nk;
        this.Nn = Nn;
        this.SBox = SBox;
        this.polynomialIrreducibleWithDegreeEIGHT = polynomialIrreducibleWithDegreeEIGHT;

        initRCon();
    }

    /**
     * Gives an output word which the S-box substitution has been individually
     * applied to each of the four bytes of its input {@code x}
     *
     * @param word byte[4] array
     * @return byte[4] modified with S-box permutation array
     */
    private byte[] subWord(byte[] word) {
        word[0] = SBox[word[0] & 0xff];
        word[1] = SBox[word[1] & 0xff];
        word[2] = SBox[word[2] & 0xff];
        word[3] = SBox[word[3] & 0xff];

        return word;
    }

    /**
     * Converts an input word [b3, b2, b1, b0] to an output [b0, b3, b2, b1]
     *
     * @param word byte[4] array
     * @return byte[4] array with rotation shift
     */
    private byte[] rotWord(byte[] word) {
        byte tmp = word[3];
        word[3] = word[2];
        word[2] = word[1];
        word[1] = word[0];
        word[0] = tmp;

        return word;
    }

    /**
     * Returns {@code byte[(Nn + 1) * Nc][4]} with {@code Nn + 1} round keys
     *
     * @param key byte[]
     * @return {@code Nn + 1} round keys
     */
    @Override
    public byte[][] generateRoundKeys(byte[] key) {
        int i = 0;
        byte[][] k = new byte[(Nn + 1) * Nc][4];
        while (i < Nk) {
            k[i][0] = key[4 * i + 3];
            k[i][1] = key[4 * i + 2];
            k[i][2] = key[4 * i + 1];
            k[i][3] = key[4 * i];
            i += 1;
        }

        i = Nk;
        while (i < Nc * (Nn + 1)) {
            byte[] tmp;
            tmp = k[i - 1].clone();

            if (i % Nk == 0) {
                tmp = Operations.xor(subWord(rotWord(tmp)), RCon[i / Nk]);
            } else if ((Nk > 6) && (i % Nk == 4)) {
                subWord(tmp);
            }

            k[i] = Operations.xor(k[i - Nk], tmp);
            i += 1;
        }

        return k;
    }
}
