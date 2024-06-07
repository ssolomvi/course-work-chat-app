package ru.mai.cipher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mai.encryption_algorithm.impl.MARS;
import ru.mai.exceptions.IllegalArgumentExceptionWithLog;
import ru.mai.round_keys.RoundKeyGeneration;
import ru.mai.utils.Operations;

public class FeistelCipherMARS {
    private static final Logger log = LoggerFactory.getLogger(FeistelCipherMARS.class);
    private static byte[][] roundKeys;

    public FeistelCipherMARS(RoundKeyGeneration keyGen, byte[] key) {
        if (keyGen == null) {
            throw new IllegalArgumentExceptionWithLog("FeistelCipherMARS: Passed param keyGen is null", log);
        }
        if (key == null) {
            throw new IllegalArgumentExceptionWithLog("FeistelCipherMARS: Passed param key is null", log);
        }

        roundKeys = keyGen.generateRoundKeys(key);
    }

    private int[] encryptionFunction(int A, int k1, int k2, int[] S) {
        int[] result = new int[3];

        int M, L, R;
        M = A + k1;
        R = Operations.cyclicShiftLeftInt(A, 13) * k2;
        L = S[M & 0x000001ff];
        R = Operations.cyclicShiftLeftInt(R, 5);
        M = Operations.cyclicShiftLeftInt(M, R & 0x0000001f);
        L = L ^ R;
        R = Operations.cyclicShiftLeftInt(R, 5);
        L = L ^ R;
        L = Operations.cyclicShiftLeftInt(L, R & 0x0000001f);

        result[0] = L;
        result[1] = M;
        result[2] = R;

        return result;
    }

    public byte[] methodForConstructingBlockCiphersEncryption(byte[] input, int roundCount) {
        int lenInput = input.length;
        if (lenInput != 16) {
            throw new IllegalArgumentExceptionWithLog("methodForConstructingBlockCiphersEncryption:" +
                    "Param input must be length 16 bytes", log);
        }

        int[] S = MARS.sBox;
        int wBytes = 32 / 8; // count of bytes in typo int

        byte[] byteA = new byte[wBytes];
        byte[] byteB = new byte[wBytes];
        byte[] byteC = new byte[wBytes];
        byte[] byteD = new byte[wBytes];

        System.arraycopy(input, 0, byteA, 0, wBytes);
        System.arraycopy(input, wBytes, byteB, 0, wBytes);
        System.arraycopy(input, 2 * wBytes, byteC, 0, wBytes);
        System.arraycopy(input, 3 * wBytes, byteD, 0, wBytes);

        int A = Operations.bytesArrToInt(Operations.reverseByteArray(byteA));
        int B = Operations.bytesArrToInt(Operations.reverseByteArray(byteB));
        int C = Operations.bytesArrToInt(Operations.reverseByteArray(byteC));
        int D = Operations.bytesArrToInt(Operations.reverseByteArray(byteD));

        int[] K = new int[roundKeys.length]; // roundKeys like 32-bits words
        for (int i = 0; i < K.length; i++) {
            K[i] = Operations.bytesArrToInt(new byte[]{roundKeys[i][0], roundKeys[i][1], roundKeys[i][2], roundKeys[i][3]});
        }

        A += K[0];
        B += K[1];
        C += K[2];
        D += K[3];

        // Forward Mixing
        for (int i = 0; i < 8; i++) {
            B = (B ^ S[Operations.lowestNBit(A, 8)]) +
                    S[256 + Operations.lowestNBit(Operations.cyclicShiftRightInt(A, 8), 8)];
            C = C + S[Operations.lowestNBit(Operations.cyclicShiftRightInt(A, 16), 8)];
            D = D ^ S[256 + Operations.lowestNBit(Operations.cyclicShiftRightInt(A, 24), 8)];

            A = Operations.cyclicShiftRightInt(A, 24);

            if (i == 1 || i == 5) {
                A = A + B;
            } else if (i == 0 || i == 4) {
                A = A + D;
            }

            int tmp = A;
            A = B;
            B = C;
            C = D;
            D = tmp;
        }

        // Cryptographic Core
        for (int i = 0; i < roundCount; i++) {
            int[] functionE = encryptionFunction(A, K[2 * i + 4], K[2 * i + 5], S);

            int R = functionE[2];
            int M = functionE[1];
            int L = functionE[0];

            A = Operations.cyclicShiftLeftInt(A, 13);
            C = C + M;

            if (i < 8) {
                B = B + L;
                D = D ^ R;
            } else {
                B = B ^ R;
                D = D + L;
            }

            int tmp = A;
            A = B;
            B = C;
            C = D;
            D = tmp;
        }

        // Backwards Mixing
        for (int i = 0; i < 8; i++) {
            if (i == 3 || i == 7) {
                A = A - B;
            } else if (i == 2 || i == 6) {
                A = A - D;
            }

            B = B ^ S[256 + Operations.lowestNBit(A, 8)];
            C = C - S[Operations.lowestNBit(Operations.cyclicShiftLeftInt(A, 8), 8)];
            D = (D - S[256 + Operations.lowestNBit(Operations.cyclicShiftLeftInt(A, 16), 8)]) ^
                    S[Operations.lowestNBit(Operations.cyclicShiftLeftInt(A, 24), 8)];

            int tmp = Operations.cyclicShiftLeftInt(A, 24);
            A = B;
            B = C;
            C = D;
            D = tmp;
        }

        A -= K[36];
        B -= K[37];
        C -= K[38];
        D -= K[39];

        byteA = Operations.intToBytes(A);
        byteB = Operations.intToBytes(B);
        byteC = Operations.intToBytes(C);
        byteD = Operations.intToBytes(D);

        return Operations.mergeByteArrays(
                Operations.mergeByteArrays(Operations.reverseByteArray(byteA), Operations.reverseByteArray(byteB)),
                Operations.mergeByteArrays(Operations.reverseByteArray(byteC), Operations.reverseByteArray(byteD))
        );
    }

    public byte[] methodForConstructingBlockCiphersDecryption(byte[] input, int roundCount) {
        int lenInput = input.length;
        if (lenInput != 16) {
            throw new IllegalArgumentExceptionWithLog("methodForConstructingBlockCiphersDecryption:" +
                    "Param input must be length 16 bytes", log);
        }

        int[] S = MARS.sBox;
        int wBytes = 32 / 8; // count of bytes in typo int

        byte[] byteA = new byte[wBytes];
        byte[] byteB = new byte[wBytes];
        byte[] byteC = new byte[wBytes];
        byte[] byteD = new byte[wBytes];

        System.arraycopy(input, 0, byteA, 0, wBytes);
        System.arraycopy(input, wBytes, byteB, 0, wBytes);
        System.arraycopy(input, 2 * wBytes, byteC, 0, wBytes);
        System.arraycopy(input, 3 * wBytes, byteD, 0, wBytes);

        int A = Operations.bytesArrToInt(Operations.reverseByteArray(byteA));
        int B = Operations.bytesArrToInt(Operations.reverseByteArray(byteB));
        int C = Operations.bytesArrToInt(Operations.reverseByteArray(byteC));
        int D = Operations.bytesArrToInt(Operations.reverseByteArray(byteD));

        int[] K = new int[roundKeys.length]; // roundKeys like 32-bits words
        for (int i = 0; i < K.length; i++) {
            K[i] = Operations.bytesArrToInt(new byte[]{roundKeys[i][0], roundKeys[i][1], roundKeys[i][2], roundKeys[i][3]});
        }

        A += K[36];
        B += K[37];
        C += K[38];
        D += K[39];

        // Forward Mixing
        for (int i = 7; i >= 0; i--) {
            int tmp = D;
            D = C;
            C = B;
            B = A;
            A = tmp;

            A = Operations.cyclicShiftRightInt(A, 24);

            D = D ^ S[Operations.lowestNBit(Operations.cyclicShiftRightInt(A, 8), 8)];
            D = D + S[256 + Operations.lowestNBit(Operations.cyclicShiftRightInt(A, 16), 8)];
            C = C + S[Operations.lowestNBit(Operations.cyclicShiftRightInt(A, 24), 8)];
            B = B ^ S[256 + Operations.lowestNBit(A, 8)];


            if (i == 2 || i == 6) {
                A = A + D;
            } else if (i == 3 || i == 7) {
                A = A + B;
            }
        }

        // Cryptographic Core
        for (int i = roundCount - 1; i >= 0; i--) {
            int tmp = D;
            D = C;
            C = B;
            B = A;
            A = tmp;

            A = Operations.cyclicShiftRightInt(A, 13);
            int[] functionE = encryptionFunction(A, K[2 * i + 4], K[2 * i + 5], S);
            int L = functionE[0];
            int M = functionE[1];
            int R = functionE[2];
            C = C - M;

            if (i < 8) {
                B = B - L;
                D = D ^ R;
            } else {
                D = D - L;
                B = B ^ R;
            }
        }

        // Backwards Mixing
        for (int i = 7; i >= 0; i--) {
            int tmp = D;
            D = C;
            C = B;
            B = A;
            A = tmp;

            if (i == 0 || i == 4) {
                A = A - D;
            } else if (i == 1 || i == 5) {
                A = A - B;
            }

            A = Operations.cyclicShiftLeftInt(A, 24);

            D = D ^ S[256 + Operations.lowestNBit(Operations.cyclicShiftRightInt(A, 24), 8)];
            C = C - S[Operations.lowestNBit(Operations.cyclicShiftRightInt(A, 16), 8)];
            B = B - S[256 + Operations.lowestNBit(Operations.cyclicShiftRightInt(A, 8), 8)];
            B = B ^ S[Operations.lowestNBit(A, 8)];
        }

        A -= K[0];
        B -= K[1];
        C -= K[2];
        D -= K[3];

        byteA = Operations.intToBytes(A);
        byteB = Operations.intToBytes(B);
        byteC = Operations.intToBytes(C);
        byteD = Operations.intToBytes(D);

        return Operations.mergeByteArrays(
                Operations.mergeByteArrays(Operations.reverseByteArray(byteA), Operations.reverseByteArray(byteB)),
                Operations.mergeByteArrays(Operations.reverseByteArray(byteC), Operations.reverseByteArray(byteD))
        );
    }
}