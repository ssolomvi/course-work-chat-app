package ru.mai.services.chatroom;

import ru.mai.utils.MathOperationsBigInteger;
import ru.mai.utils.Operations;

import java.math.BigInteger;

public class DiffieHellmanNumbersHandler {
    private static final int DIFFIE_HELLMAN_NUMBER_SIZE = 50;

    public static BigInteger generateMinorNumber() {
        return new BigInteger(Operations.generateBytes(DiffieHellmanNumbersHandler.DIFFIE_HELLMAN_NUMBER_SIZE - 1));
    }

    public static BigInteger generateMajorNumber(BigInteger g, BigInteger p, BigInteger minorNumber) {
        return MathOperationsBigInteger.fastPowMod(g, minorNumber, p);
    }

    public static byte[] getKey(BigInteger companionNumber, BigInteger minorNumber, BigInteger p) {
        // key = B ^ a mod P
        return MathOperationsBigInteger.fastPowMod(companionNumber, minorNumber, p).toByteArray();
    }

    private DiffieHellmanNumbersHandler() {
        // private constructor to hide the implicit public one
    }
}
