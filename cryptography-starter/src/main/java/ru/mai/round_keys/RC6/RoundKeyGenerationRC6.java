package ru.mai.round_keys.RC6;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mai.encryption_algorithm.impl.RC6;
import ru.mai.round_keys.LOKI97.RoundKeyGenerationLOKI97;
import ru.mai.round_keys.RoundKeyGeneration;
import ru.mai.utils.Operations;

import static java.lang.Math.max;

public class RoundKeyGenerationRC6 implements RoundKeyGeneration {
    static final Logger log = LoggerFactory.getLogger(RoundKeyGenerationLOKI97.class);
    private static final int ROUND_COUNT_RC6 = 20;
    private static final int Q32 = 0x9e3779b9;
    private static final int P32 = 0xb7e15163;

    @Override
    public byte[][] generateRoundKeys(byte[] key) {
        int w = RC6.w;
        int wBytes = w / 8;
        int c = key.length / wBytes;
        byte[][] L = new byte[c][wBytes];

        // preload b byte key into the c-word array L
        for (int i = 0; i < c; i++) {
            System.arraycopy(key, i * wBytes, L[i], 0, wBytes);
        }

        int t = 2 * (ROUND_COUNT_RC6 + 2);
        byte[][] S = new byte[t][w / 8];

        S[0] = Operations.intToBytes(P32);
        for (int i = 1; i < t; i++) {
            S[i] = Operations.additionByteArrayLength4AndInt(S[i - 1], Q32);
        }

        int A = 0, B = 0, i = 0, j = 0;
        int log2w = (int) (Math.log(wBytes * 8) / Math.log(2));

        int v = 3 * max(c, t);
        for (int s = 1; s <= v; s++) {
            S[i] = Operations.intToBytes(
                    Operations.cyclicShiftLeftInt(
                            Operations.bytesArrToInt(S[i]) + A + B,
                            3
                    )
            );
//            S[i] = Operations.cyclicShiftLeft(
//                    Operations.additionByteArrayLength4AndInt(
//                            Operations.additionByteArrayLength4AndInt(
//                                    S[i], A
//                            ),
//                            B),
//                    3);
            A = Operations.bytesArrToInt(S[i]);

            L[j] = Operations.intToBytes(
                    Operations.cyclicShiftLeftInt(Operations.bytesArrToInt(L[j]) + A + B,
                    A + B)
            );
            B = Operations.bytesArrToInt(L[j]);
//            L[j] = Operations.cyclicShiftLeft(
//                    Operations.additionByteArrayLength4AndInt(
//                            Operations.additionByteArrayLength4AndInt(
//                                    L[j], A
//                            ),
//                            B),
//                    (A + B) & ~(~0 << log2w));

            i = (i + 1) % t;
            j = (j + 1) % c;
        }

        return S;
    }
}
