package ru.mai.round_keys.DES;

import ru.mai.round_keys.RoundKeyGeneration;
import ru.mai.utils.Operations;
import ru.mai.utils.Permutation;

public class RoundKeyGenerationDESDirect implements RoundKeyGeneration {
    private static final int[] permutationTableC = {57, 49, 41, 33, 25, 17, 9, 1, 58, 50, 42, 34, 26, 18, 10, 2, 59,
            51, 43, 35, 27, 19, 11, 3, 60, 52, 44, 36};
    private static final int[] permutationTableD = {63, 55, 47, 39, 31, 23, 15, 7, 62, 54, 46, 38, 30, 22, 14, 6, 61,
            53, 45, 37, 29, 21, 13, 5, 28, 20, 12, 4};

    private static final int[] permutationTableCD = {14, 17, 11, 24, 1, 5, 3, 28, 15, 6, 21, 10, 23, 19, 12, 4, 26,
            8, 16, 7, 27, 20, 13, 2, 41, 52, 31, 37, 47, 55, 30, 40, 51, 45, 33, 48, 44, 49, 39, 56, 34, 53, 46, 42,
            50, 36, 29, 32};

    private static final int[] cycleShiftTable = {1, 1, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 1};

    private static final int ROUND_COUNT_DES = 16;

    /**
     * Expand key by operation:
     * write 7 bits from {@code key}, 8-th bit is sum(mod2) of written 7 bits
     *
     * @param key initial key with length equal to 7 byte
     * @return Expanded key with length equal to 8 byte
     */
    private byte[] expandKey(byte[] key) {
        if (!(key.length == 7 || key.length == 8)) {
            throw new IllegalArgumentException("Param key must be of length 7 or 8");
        }

        byte[] result = new byte[8];

        int counterForKey = 0, sum = 0, counterForExp = 0;

        if (key.length == 7) {
            // 56 = 7 bytes, each has 8 bits in it, overall 7*8 = 56 bits
            while (counterForKey != 56) {
                if (counterForKey % 7 == 0 && counterForKey != 0) {
                    if (sum % 2 == 0) {
                        result[counterForExp / 8] |= 1;
                    }

                    sum = 0;
                    ++counterForExp;
                }

                boolean bit = (key[counterForKey / 8] & 0xff & (1 << (7 - counterForKey % 8))) != 0;
                result[counterForExp / 8] |= (byte) ((bit ? 1 : 0) << (7 - counterForExp % 8));
                sum += bit ? 1 : 0;
                counterForExp++;
                counterForKey++;
            }

            if (sum % 2 == 0) {
                result[counterForExp / 8] |= 1;
            }
        } else {
            while (counterForKey != 64) {
                boolean bit = (key[counterForKey / 8] & 0xff & (1 << (7 - counterForKey % 8))) != 0;
                result[counterForExp / 8] |= (byte) ((bit ? 1 : 0) << (7 - counterForExp % 8));
                sum += bit ? 1 : 0;
                counterForExp++;
                counterForKey++;

                if (counterForKey % 8 == 7) {
                    if (sum % 2 == 0) {
                        result[counterForExp / 8] |= 1;
                    }

                    sum = 0;
                    ++counterForExp;
                    ++counterForKey;
                }
            }
        }

        return result;
    }

    private byte[] mergeCD(byte[] C, byte[] D) {
        byte[] res = new byte[7];
        System.arraycopy(C, 0, res, 0, C.length);
        byte[] DShifted = Operations.cyclicShiftRight(D, 4);
        res[C.length - 1] |= DShifted[0];
        System.arraycopy(DShifted, 1, res, C.length, D.length - 1);
        return res;
    }

    @Override
    public byte[][] generateRoundKeys(byte[] key) {
        if (!(key.length == 7 || key.length == 8)) {
            throw new IllegalArgumentException("Param key must be of length 7 or 8");
        }

        byte[] expandedKey = expandKey(key); // 8 bytes

        byte[][] C = new byte[ROUND_COUNT_DES][4];
        byte[][] D = new byte[ROUND_COUNT_DES][4];

        C[0] = Permutation.permute(expandedKey, permutationTableC, Operations.IndexingRule.FROM_MOST_TO_LEAST_START_WITH_1);
        D[0] = Permutation.permute(expandedKey, permutationTableD, Operations.IndexingRule.FROM_MOST_TO_LEAST_START_WITH_1);

        for (int i = 1; i < ROUND_COUNT_DES; i++) {
            C[i] = Operations.cyclicShiftLeft(C[i - 1], cycleShiftTable[i]);
            D[i] = Operations.cyclicShiftLeft(D[i - 1], cycleShiftTable[i]);
        }

        byte[][] roundKeys = new byte[ROUND_COUNT_DES][];

        for (int i = 0; i < ROUND_COUNT_DES; i++) {
            roundKeys[i] = Permutation.permute(mergeCD(C[i], D[i]), permutationTableCD,
                    Operations.IndexingRule.FROM_MOST_TO_LEAST_START_WITH_1);
        }

        return roundKeys;
    }
}
