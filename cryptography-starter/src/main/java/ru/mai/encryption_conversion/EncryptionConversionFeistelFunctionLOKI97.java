package ru.mai.encryption_conversion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mai.utils.Operations;
import ru.mai.utils.Permutation;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import static ru.mai.utils.Operations.IndexingRule.FROM_LEAST_TO_MOST_START_WITH_0;
import static ru.mai.utils.Operations.IndexingRule.FROM_MOST_TO_LEAST_START_WITH_0;

public class EncryptionConversionFeistelFunctionLOKI97 implements EncryptionConversion {
    static final Logger log = LoggerFactory.getLogger(EncryptionConversionFeistelFunctionLOKI97.class);
    private static final Operations.IndexingRule indexRule = FROM_MOST_TO_LEAST_START_WITH_0;
    private static final String fileNameS1 = "src/test/resources/LOKI97/S1.txt";
    private static final String fileNameS2 = "src/test/resources/LOKI97/S2.txt";
    boolean isInitiated = false;
    private static final byte[] S1 = new byte[0x1FFF + 1];
    private static final byte[] S2 = new byte[0x7FF + 1];
    private static final int[] expansionTableE = {
            4, 3, 2, 1, 0,
            63, 62, 61, 60, 59, 58, 57, 56,
            58, 57, 56, 55, 54, 53, 52, 51, 50, 49, 48,
            52, 51, 50, 49, 48, 47, 46, 45, 44, 43, 42, 41, 40,
            42, 41, 40, 39, 38, 37, 36, 35, 34, 33, 32,
            34, 33, 32, 31, 30, 29, 28, 27, 26, 25, 24,
            28, 27, 26, 25, 24, 23, 22, 21, 20, 19, 18, 17, 16,
            18, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8,
            12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0
    };

    private static final int[] permutationTableP = {
            56,	48,	40,	32,	24,	16,	8,	0,	57,	49,	41,	33,	25,	17,	9,	1,
            58,	50,	42,	34,	26,	18,	10,	2,	59,	51,	43,	35,	27,	19,	11, 3,
            60,	52,	44,	36,	28,	20,	12,	4,	61,	53,	45,	37,	29,	21,	13,	5,
            62,	54,	46,	38,	30,	22,	14,	6,	63,	55,	47,	39,	31,	23,	15,	7
    };

    private void initSBoxes() throws IOException {
        Path pathS1 = Paths.get(fileNameS1);
        Path pathS2 = Paths.get(fileNameS2);

        if (!isInitiated) {
            try (Scanner ScannerS1 = new Scanner(pathS1)) {
                try (Scanner ScannerS2 = new Scanner(pathS2)) {
                    for (int i = 0; i < S1.length; i++) {
                        S1[i] = ScannerS1.nextByte();
                    }

                    for (int i = 0; i < S2.length; i++) {
                        S2[i] = ScannerS2.nextByte();
                    }
                }
            }

            isInitiated = true;
        }
    }

    private byte[] KP(byte[] A, byte[] roundKey) {
        int lenLeftRight = A.length / 2;
        byte[] leftA = new byte[lenLeftRight];
        byte[] rightA = new byte[lenLeftRight];
        byte[] rightRoundKey = new byte[lenLeftRight];

        System.arraycopy(A, 0, leftA, 0, lenLeftRight);
        System.arraycopy(A, lenLeftRight, rightA, 0, lenLeftRight);
        System.arraycopy(roundKey, lenLeftRight, rightRoundKey, 0, lenLeftRight);

        return Operations.mergeByteArrays(
                Operations.or(
                        Operations.and(leftA, Operations.negate(rightRoundKey)),
                        Operations.and(rightA, rightRoundKey)),
                Operations.or(
                        Operations.and(rightA, Operations.negate(rightRoundKey)),
                        Operations.and(leftA, rightRoundKey)));

//        return Operations.or(
//                Operations.mergeByteArrays(
//                        Operations.and(leftA, Operations.negate(rightRoundKey)),
//                        Operations.and(rightA, rightRoundKey)),
//                Operations.mergeByteArrays(
//                        Operations.and(rightA, Operations.negate(rightRoundKey)),
//                        Operations.and(leftA, rightRoundKey)));
    }

    private byte[] E(byte[] input) {
        return Permutation.permute(input, expansionTableE, indexRule);
    }

    private byte[] Sa(byte[] input) { // input = 96 bits = 12 bytes
        if (!isInitiated) {
            try {
                initSBoxes();
            } catch (IOException e) {
                log.error("CARRRAMBA! I/O exception happened!", e);
            }
        }

        int idx0 = ((((input[0] & 0xff) << 8) | (input[1] & 0xf8)) >>> 3);
        int idx1 = ((input[1] & 0x07) << 8) | (input[2] & 0xff);
        int idx2 = ((((input[3] & 0xff) << 8) | (input[4] & 0xf8)) >>> 3);
        int idx3 = ((input[4] & 0x07) << 8) | (input[5] & 0xff);
        int idx4 = ((((input[6] & 0xff) << 8) | (input[7] & 0xe0)) >>> 5);
        int idx5 = ((input[7] & 0x1f) << 8) | (input[8] & 0xff);
        int idx6 = ((((input[9] & 0xff) << 8) | (input[10] & 0xe0)) >>> 5);
        int idx7 = ((input[10] & 0x1f) << 8) | (input[11] & 0xff);

        byte[] result = new byte[8];
        result[0] = S1[idx0];
        result[1] = S2[idx1];
        result[2] = S1[idx2];
        result[3] = S2[idx3];
        result[4] = S2[idx4];
        result[5] = S1[idx5];
        result[6] = S2[idx6];
        result[7] = S1[idx7];

        return result;
    }

    private byte[] P(byte[] input) {
        return Permutation.permute(input, permutationTableP, indexRule);
    }

    private byte[] Sb(byte[] input, byte[] roundKey) {
        int lenLeftRight = input.length / 2;
        byte[] leftRoundKey = new byte[lenLeftRight];
        System.arraycopy(roundKey, 0, leftRoundKey, 0, lenLeftRight);

        byte[] expanded = Operations.mergeByteArrays(leftRoundKey, input);

        int idx0 = ((((expanded[0] & 0xff) << 8) | (expanded[1] & 0xe0)) >>> 5);
        int idx1 = ((((expanded[1] & 0x1f) << 8) | (expanded[2] & 0xfc)) >>> 2);
        int idx2 = ((((expanded[2] & 0x03) << 16) | ((expanded[3] & 0xff) << 8) | (expanded[4] & 0xe0)) >>> 5);
        int idx3 = ((expanded[4] & 0x1f) << 8) | (expanded[5] & 0xff);
        int idx4 = ((((expanded[6] & 0xff) << 8) | (expanded[7] & 0xe0)) >>> 5);
        int idx5 = ((((expanded[7] & 0x1f) << 8) | (expanded[8] & 0xfc)) >>> 2);
        int idx6 = ((((expanded[8] & 0x03) << 16) | ((expanded[9] & 0xff) << 8) | (expanded[10] & 0xe0)) >>> 5);
        int idx7 = ((expanded[10] & 0x1f) << 8) | (expanded[11] & 0xff);

        byte[] result = new byte[8];
        result[0] = S2[idx0];
        result[1] = S2[idx1];
        result[2] = S1[idx2];
        result[3] = S1[idx3];
        result[4] = S2[idx4];
        result[5] = S2[idx5];
        result[6] = S1[idx6];
        result[7] = S1[idx7];

        return result;
    }

    // input = 8 bytes
    @Override
    public byte[] encrypt(byte[] input, byte[] roundKey) {
        // Sb(P(Sa(E(KP(input, roundKey)))), roundKey)
        byte[] arrayAfterKP = KP(input, roundKey);
        byte[] arrayAfterE = E(arrayAfterKP);
        byte[] arrayAfterSa = Sa(arrayAfterE);
        byte[] arrayAfterP = P(arrayAfterSa);

        return Sb(arrayAfterP, roundKey);
    }
}
