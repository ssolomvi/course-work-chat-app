package ru.mai.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mai.exceptions.IllegalArgumentExceptionWithLog;

import java.util.ArrayList;

public class GaloisFieldPolynomialOperations {

    private static final Logger log = LoggerFactory.getLogger(GaloisFieldPolynomialOperations.class);
//    private static final ArrayList<Byte> polynomialsIrreducibleWithDegreeLessEIGHT = new ArrayList<>();
    private static final ArrayList<Byte> polynomialsIrreducibleWithDegreeEIGHT = new ArrayList<>();

    public static void printAllPolynomialsIrreducibleWithDegreeEIGHT() {
        if (polynomialsIrreducibleWithDegreeEIGHT.isEmpty()) {
            constructingPolynomialsIrreducibleWithDegreeEIGHT();
        }

        int numerous = 0;
        for (byte polynomialIrreducible : polynomialsIrreducibleWithDegreeEIGHT) {
            numerous++;
            System.out.println(numerous + ") " + "x^8 + " + polynomyalToString(polynomialIrreducible));
        }
    }

//    private static void constructingPolynomialsIrreducibleWithDegreeLessEIGHT() {
//        if (!polynomialsIrreducibleWithDegreeLessEIGHT.isEmpty()) {
//            return;
//        }
//
//        polynomialsIrreducibleWithDegreeLessEIGHT.add((byte) 2);
//        boolean flag = true;
//
//        for (int i = 3; i < 256; i++) {
//            for (byte polynomialIrreducible : polynomialsIrreducibleWithDegreeLessEIGHT) {
//                if (remainder((byte) i, polynomialIrreducible) == 0) {
//                    flag = false;
//                    break;
//                }
//            }
//            if (flag) {
//                polynomialsIrreducibleWithDegreeLessEIGHT.add((byte) i);
//            }
//            flag = true;
//        }
//    }
    private static void constructingPolynomialsIrreducibleWithDegreeEIGHT() {
        if (!polynomialsIrreducibleWithDegreeEIGHT.isEmpty()) {
            return;
        }

        boolean flag = true;

        for (short i = 256; i < 512; i++) {
            for (short j = 2; j < i; j++) {
                if (remainder(i, j) == 0) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                polynomialsIrreducibleWithDegreeEIGHT.add((byte) i);
            }
            flag = true;
        }
    }

    public static boolean isPolynomialWithDegreeEIGHTIrreducible(byte poly) {
        if (polynomialsIrreducibleWithDegreeEIGHT.isEmpty()) {
            constructingPolynomialsIrreducibleWithDegreeEIGHT();
        }

        return polynomialsIrreducibleWithDegreeEIGHT.contains(poly);
    }

    public static byte add(byte poly1, byte poly2)
    {
        return (byte) (poly1 ^ poly2);
    }

    public static byte remainder(byte dividend, byte divider) {
        if (divider == 0) {
            log.error("remainder: divider mustn't be zero");
            throw new IllegalArgumentException("remainder: divider mustn't be zero");
        }

        int dividerOlderDegree = Operations.getOldestSetBit(divider);
        int dividendOlderDegree = Operations.getOldestSetBit(dividend);
        int differentOfDegrees = dividendOlderDegree - dividerOlderDegree;

        if (differentOfDegrees < 0) {
            log.error("remainder: divider must have degree non more than dividend's degree");
            throw new IllegalArgumentException("remainder: divider must have degree non more than dividend's degree");
        }

        while (differentOfDegrees >= 0) {
            dividend ^= (byte) (divider << differentOfDegrees);
            dividendOlderDegree = Operations.getOldestSetBit(dividend);
            differentOfDegrees = dividendOlderDegree - dividerOlderDegree;
        }

        return dividend;
    }

    public static short remainder(short dividend, short divider) {
        if (divider == 0) {
            log.error("remainder: divider mustn't be zero");
            throw new IllegalArgumentException("remainder: divider mustn't be zero");
        }

        int dividerOlderDegree = Operations.getOldestSetBit(divider);
        int dividendOlderDegree = Operations.getOldestSetBit(dividend);
        int differentOfDegrees = dividendOlderDegree - dividerOlderDegree;

        if (differentOfDegrees < 0) {
            log.error("remainder: divider must have degree non more than dividend's degree");
            throw new IllegalArgumentException("remainder: divider must have degree non more than dividend's degree");
        }

        while (differentOfDegrees >= 0) {
            dividend ^= (short) (divider << differentOfDegrees);
            dividendOlderDegree = Operations.getOldestSetBit(dividend);
            differentOfDegrees = dividendOlderDegree - dividerOlderDegree;
        }

        return dividend;
    }

    public static byte divide(byte dividend, byte divider) {
        if (divider == 0) {
            log.error("divide: divider mustn't be zero");
            throw new IllegalArgumentException("divide: divider mustn't be zero");
        }

        int dividerOlderDegree = Operations.getOldestSetBit(divider);
        int dividendOlderDegree = Operations.getOldestSetBit(dividend);
        int differentOfDegrees = dividendOlderDegree - dividerOlderDegree;

        if (differentOfDegrees < 0) {
            log.error("divide: divider must have degree non more than dividend's degree");
            throw new IllegalArgumentException("divide: divider must have degree non more than dividend's degree");
        }

        byte result = 0;
        while (differentOfDegrees >= 0) {
            dividend ^= (byte) (divider << differentOfDegrees);
            result |= (byte) (1 << differentOfDegrees);
            dividendOlderDegree = Operations.getOldestSetBit(dividend);
            differentOfDegrees = dividendOlderDegree - dividerOlderDegree;
        }

        return result;
    }

    public static short divide(short dividend, short divider) {
        if (divider == 0) {
            log.error("divide: divider mustn't be zero");
            throw new IllegalArgumentException("divide: divider mustn't be zero");
        }

        int dividerOlderDegree = Operations.getOldestSetBit(divider);
        int dividendOlderDegree = Operations.getOldestSetBit(dividend);
        int differentOfDegrees = dividendOlderDegree - dividerOlderDegree;

        if (differentOfDegrees < 0) {
            log.error("divide: divider must have degree non more than dividend's degree");
            throw new IllegalArgumentException("divide: divider must have degree non more than dividend's degree");
        }

        short result = 0;
        while (differentOfDegrees >= 0) {
            dividend ^= (short) (divider << differentOfDegrees);
            result |= (short) (1 << differentOfDegrees);
            dividendOlderDegree = Operations.getOldestSetBit(dividend);
            differentOfDegrees = dividendOlderDegree - dividerOlderDegree;
        }

        return result;
    }

    public static byte multiplyByModulo(byte poly1, byte poly2, byte modulo) {
        if (!isPolynomialWithDegreeEIGHTIrreducible(modulo)) {
            throw new IllegalArgumentExceptionWithLog("multiplyByModulo: polynomial modulo must be irreducible", log);
        }

        short result = 0;

        // 8 is a count of bits in byte
        for (int i = 0; i < 8; i++) {
            if (((poly2 >> i) & 1) == 0)
            {
                continue;
            }
            result ^= (short) (((poly1) & 0xFF) << i); // it's very important!!!!!!!!!!
        }

        return getDivisionRemainderIrreduciblePolynomial(result, modulo);
    }

    private static byte getDivisionRemainderIrreduciblePolynomial(short multiplicationResult, byte modulo) {
        int leftShiftValue = 6;
        while (leftShiftValue >= 0)
        {
            if (((multiplicationResult >> (8 + leftShiftValue)) & 1) == 1)
            {
                // degrees from 0 to 7
                multiplicationResult ^= (short) ((short) ((modulo & 0xFF) << leftShiftValue)  );
                // degree == 8
                multiplicationResult &= (short) ((~(1 << (8 + leftShiftValue))));
            }

            --leftShiftValue;
        }

        return (byte) (multiplicationResult);
    }

    public static byte fastPowModForPolynomials(byte poly, int degree, byte modulo) {
        if (degree == 0) {
            return 1;
        }

        byte toReturn = 1;
        boolean flag = true;

        if (degree < 0) {
            degree = (byte) -degree;
            flag = false;
        }

        while (degree > 0) {
            if ((degree & 1) == 1) {
                toReturn = GaloisFieldPolynomialOperations.multiplyByModulo(toReturn, poly, modulo);
            }

            poly = GaloisFieldPolynomialOperations.multiplyByModulo(poly, poly, modulo);
            toReturn = GaloisFieldPolynomialOperations.multiplyByModulo(toReturn, (byte) 1, modulo);
            degree >>= 1;
        }

        if (!flag) {
            return invert(toReturn, modulo);
        }

        return toReturn;
    }

    public static byte invert(byte polyToInvert, byte modulo) {
        if (!isPolynomialWithDegreeEIGHTIrreducible(modulo)) {
            log.error("invert: param modulo must be irreducible polynomial");
            throw new IllegalArgumentException("invert: param modulo must be irreducible polynomial");
        }

        if (polyToInvert == 0) {
            return 0;
        }

        return fastPowModForPolynomials(polyToInvert, 254, modulo);
    }

    public static String polynomyalToString(byte poly) {
        if (poly == 0) {
            return "0";
        }

        StringBuilder builder = new StringBuilder();
        boolean flag = false; // tracks the first bit equal to 1
        for (int i = 7; i >= 0; i--) {
            // if i-th bit is 1
            if (((poly >> i) & 1) == 1) {
                // if i == 0, we don't print symbol "x"
                if (i == 0) {
                    if (flag) {
                        builder.append(" + 1");
                    } else {
                        builder.append("1");
                    }
                } else {
                    if (flag) {
                        builder.append(" + x^").append(i);
                    } else {
                        builder.append("x^").append(i);
                        flag = true;
                    }
                }
            }
        }

        return builder.toString();
    }
    public static String polynomyalToString(short poly) {
        if (poly == 0) {
            return "0";
        }

        StringBuilder builder = new StringBuilder();
        boolean flag = false; // tracks the first bit equal to 1
        for (int i = 15; i >= 0; i--) {
            // if i-th bit is 1
            if (((poly >> i) & 1) == 1) {
                // if i == 0, we don't print symbol "x"
                if (i == 0) {
                    if (flag) {
                        builder.append(" + 1");
                    } else {
                        builder.append("1");
                    }
                } else {
                    if (flag) {
                        builder.append(" + x^").append(i);
                    } else {
                        builder.append("x^").append(i);
                        flag = true;
                    }
                }
            }
        }

        return builder.toString();
    }
}
