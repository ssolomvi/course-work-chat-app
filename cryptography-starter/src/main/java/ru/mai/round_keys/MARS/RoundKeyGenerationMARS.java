package ru.mai.round_keys.MARS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mai.encryption_algorithm.impl.MARS;
import ru.mai.exceptions.IllegalArgumentExceptionWithLog;
import ru.mai.round_keys.RoundKeyGeneration;
import ru.mai.utils.Operations;

public class RoundKeyGenerationMARS implements RoundKeyGeneration {
    static final Logger log = LoggerFactory.getLogger(RoundKeyGenerationMARS.class);

    private static int generateMask(int x){
        int m;

        m = (~x ^ (x>>>1)) & 0x7fffffff;
        m &= (m >> 1) & (m >> 2);
        m &= (m >> 3) & (m >> 6);

        if (m == 0)
            return 0;

        m <<= 1; m |= (m << 1); m |= (m << 2); m |= (m << 4);

        m |= (m << 1) & ~x & 0x80000000;

        return m & 0xfffffffc;
    }

    @Override
    public byte[][] generateRoundKeys(byte[] key) {
        int n = key.length / 4; // count of 32-bit words in key
        if (n < 4 || n > 14) {
            throw new IllegalArgumentExceptionWithLog("generateRoundKeys: Passed param key must be 4 <= n <= 14", log);
        }
        int[] S = MARS.sBox;
        int[] K = new int[40];

        int[] T = new int[n + 15]; // array of words
        for (int i = 0; i < n; i++) {
            T[i] = Operations.bytesArrToInt(new byte[]{key[4 * i + 3], key[4 * i + 2], key[4 * i + 1], key[4 * i]});
        }
        T[n] = n;

        for (int j = 0; j < 4; j++) {
            for (int i = 0; i < 15; i++) {
                T[i] = T[i] ^ (Operations.cyclicShiftLeftInt(
                        T[Math.abs((i - 7) % 15)] ^ T[Math.abs((i - 2) % 15)],
                        3))
                        ^ (4 * i + j);

            }

            for (int k = 0; k < 4; k++) {
                for (int i = 0; i < T.length; i++) {
                    T[i] = Operations.cyclicShiftLeftInt(
                            T[i] + S[Operations.lowestNBit(T[Math.abs((i - 1) % 15)], 9)],
                            9);
                }
            }

            for (int i = 0; i < 10; i++) {
                K[10 * j + i] = T[Math.abs((4 * i) % 15)];
            }
        }

        int[] B = {0xa4a8d57b, 0x5b5d193b, 0xc8a8309b, 0x73f9a978};

        for (int i = 5; i < 36; i += 2) {
            int j = Operations.lowestNBit(K[i], 2);
            int w = K[i] | 3;

            int M = generateMask(w);

            int r = Operations.lowestNBit(K[i - 1], 5);

            int p = Operations.cyclicShiftLeftInt(B[j], r);
            K[i] = w ^ (p & M);
        }

        byte[][] roundKeys = new byte[40][4];
        for (int i = 0; i < 40; i++) {
            roundKeys[i] = Operations.intToBytes(K[i]);
        }

        return roundKeys;
    }
}
