package ru.mai.cipher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mai.encryption_algorithm.impl.RC6;
import ru.mai.exceptions.IllegalArgumentExceptionWithLog;
import ru.mai.round_keys.RoundKeyGeneration;
import ru.mai.utils.Operations;

import java.util.Collection;

public class FeistelCipherRC6 {
    private static final Logger log = LoggerFactory.getLogger(FeistelCipherRC6.class);
    private static byte[][] roundKeys;

    public FeistelCipherRC6(RoundKeyGeneration keyGen, byte[] key) {
        if (keyGen == null) {
            throw new IllegalArgumentExceptionWithLog("FeistelCipherRC6: Passed param keyGen is null", log);
        }
        if (key == null) {
            throw new IllegalArgumentExceptionWithLog("FeistelCipherRC6: Passed param key is null", log);
        }

        roundKeys = keyGen.generateRoundKeys(key);
    }

    public byte[] methodForConstructingBlockCiphersEncryption(byte[] input, int roundCount) {
        if (roundKeys.length != 2 * (roundCount + 2)) {
            throw new IllegalArgumentExceptionWithLog("methodForConstructingBlockCiphersEncryption: " +
                    "Param roundCount must be 2 * (roundCount + 2) == roundKeys.length", log);
        }

        int lenInput = input.length;
        int wBytes = RC6.w / 8;

        if (lenInput != wBytes * 4) {
            throw new IllegalArgumentExceptionWithLog("methodForConstructingBlockCiphersEncryption:" +
                    "Param input must be length 16 bytes", log);
        }

        int log2w = (int) (Math.log(wBytes * 8) / Math.log(2));

        // encryption RC6
        byte[] A = new byte[wBytes];
        byte[] B = new byte[wBytes];
        byte[] C = new byte[wBytes];
        byte[] D = new byte[wBytes];

        System.arraycopy(input, 0, A, 0, wBytes);
        System.arraycopy(input, wBytes, B, 0, wBytes);
        System.arraycopy(input, 2 * wBytes, C, 0, wBytes);
        System.arraycopy(input, 3 * wBytes, D, 0, wBytes);

        B = Operations.additionByteArraysLength4(B, roundKeys[0]);
        D = Operations.additionByteArraysLength4(D, roundKeys[1]);

        for (int i = 1; i <= roundCount; i++) {
            byte[] t = Operations.intToBytes(
                    Operations.cyclicShiftLeftInt(
                            Operations.bytesArrToInt(
                                    Operations.multiplyingByteArrayLength4(
                                            B,
                                            Operations.additionByteArrayLength4AndInt(
                                                    Operations.multiplyingByteArrayLength4AndInt(
                                                            B,
                                                            2
                                                    ),
                                                    1
                                            )
                                    )),
                            log2w
                    ));

            byte[] u = Operations.intToBytes(Operations.cyclicShiftLeftInt(
                    Operations.bytesArrToInt(
                            Operations.multiplyingByteArrayLength4(
                                    D,
                                    Operations.additionByteArrayLength4AndInt(
                                            Operations.multiplyingByteArrayLength4AndInt(
                                                    D,
                                                    2
                                            ),
                                            1
                                    )
                            )),
                    log2w
            ));

            A = Operations.additionByteArraysLength4(
                    Operations.intToBytes(
                            Operations.cyclicShiftLeftInt(
                                    Operations.bytesArrToInt(
                                            Operations.xor(
                                                    A,
                                                    t
                                            )),
//                            u[3] & 0xff & ~(~0 << log2w)
                                    Operations.bytesArrToInt(u)
                            )),
                    roundKeys[2 * i]
            );

            C = Operations.additionByteArraysLength4(
                    Operations.intToBytes(
                            Operations.cyclicShiftLeftInt(
                                    Operations.bytesArrToInt(
                                            Operations.xor(
                                                    C,
                                                    u
                                            )),
//                            t[3] & 0xff & ~(~0 << log2w)
                                    Operations.bytesArrToInt(t)
                            )),
                    roundKeys[2 * i + 1]
            );

            byte[] tmp = A;
            A = B;
            B = C;
            C = D;
            D = tmp;
        }

        A = Operations.additionByteArraysLength4(A, roundKeys[2 * roundCount + 2]);
        C = Operations.additionByteArraysLength4(C, roundKeys[2 * roundCount + 3]);

        A = Operations.reverseByteArray(A);
        B = Operations.reverseByteArray(B);
        C = Operations.reverseByteArray(C);
        D = Operations.reverseByteArray(D);

        return Operations.mergeByteArrays(
                A,
                Operations.mergeByteArrays(
                        B,
                        Operations.mergeByteArrays(C, D)
                )
        );
    }

    public byte[] methodForConstructingBlockCiphersDecryption(byte[] input, int roundCount) {
        if (roundKeys.length != 2 * (roundCount + 2)) {
            throw new IllegalArgumentExceptionWithLog("methodForConstructingBlockCiphersEncryption: " +
                    "Param roundCount must be 2 * (roundCount + 2) == roundKeys.length", log);
        }

        int lenInput = input.length;
        int wBytes = RC6.w / 8;

        if (lenInput != wBytes * 4) {
            throw new IllegalArgumentExceptionWithLog("methodForConstructingBlockCiphersEncryption:" +
                    "Param input must be length 16 bytes", log);
        }

        int log2w = (int) (Math.log(wBytes * 8) / Math.log(2));

        // decryption RC6
        byte[] A = new byte[wBytes];
        byte[] B = new byte[wBytes];
        byte[] C = new byte[wBytes];
        byte[] D = new byte[wBytes];

        System.arraycopy(input, 0, A, 0, wBytes);
        System.arraycopy(input, wBytes, B, 0, wBytes);
        System.arraycopy(input, 2 * wBytes, C, 0, wBytes);
        System.arraycopy(input, 3 * wBytes, D, 0, wBytes);

        A = Operations.reverseByteArray(A);
        B = Operations.reverseByteArray(B);
        C = Operations.reverseByteArray(C);
        D = Operations.reverseByteArray(D);

        C = Operations.subtractionByteArraysLength4(C, roundKeys[2 * roundCount + 3]);
        A = Operations.subtractionByteArraysLength4(A, roundKeys[2 * roundCount + 2]);

        for (int i = roundCount; i >= 1; i--) {
            byte[] tmp = D;
            D = C;
            C = B;
            B = A;
            A = tmp;

            byte[] u = Operations.intToBytes(
                    Operations.cyclicShiftLeftInt(
                            Operations.bytesArrToInt(
                                    Operations.multiplyingByteArrayLength4(
                                            D,
                                            Operations.additionByteArrayLength4AndInt(
                                                    Operations.multiplyingByteArrayLength4AndInt(
                                                            D,
                                                            2
                                                    ),
                                                    1
                                            )
                                    )),
                            log2w
                    ));

            byte[] t = Operations.intToBytes(
                    Operations.cyclicShiftLeftInt(
                            Operations.bytesArrToInt(
                                    Operations.multiplyingByteArrayLength4(
                                            B,
                                            Operations.additionByteArrayLength4AndInt(
                                                    Operations.multiplyingByteArrayLength4AndInt(
                                                            B,
                                                            2
                                                    ),
                                                    1
                                            )
                                    )),
                            log2w
                    ));

            C = Operations.xor(
                    Operations.intToBytes(
                            Operations.cyclicShiftRightInt(
                                    Operations.bytesArrToInt(
                                            Operations.subtractionByteArraysLength4(
                                                    C,
                                                    roundKeys[2 * i + 1]
                                            )),
//                            t[3] & 0xff & ~(~0 << log2w)
                                    Operations.bytesArrToInt(t)
                            )),
                    u
            );

            A = Operations.xor(
                    Operations.intToBytes(
                            Operations.cyclicShiftRightInt(
                                    Operations.bytesArrToInt(
                                            Operations.subtractionByteArraysLength4(
                                                    A,
                                                    roundKeys[2 * i]
                                            )),
//                            u[3] & 0xff & ~(~0 << log2w)
                                    Operations.bytesArrToInt(u)
                            )),
                    t
            );
        }

        D = Operations.subtractionByteArraysLength4(D, roundKeys[1]);
        B = Operations.subtractionByteArraysLength4(B, roundKeys[0]);

        return Operations.mergeByteArrays(
                A,
                Operations.mergeByteArrays(
                        B,
                        Operations.mergeByteArrays(C, D)
                )
        );
    }
}
