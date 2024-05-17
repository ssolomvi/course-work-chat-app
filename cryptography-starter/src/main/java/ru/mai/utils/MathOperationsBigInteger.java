package ru.mai.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mai.primality_test.ProbabilisticPrimalityTest;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Random;

public class MathOperationsBigInteger {
    private static final Logger log = LoggerFactory.getLogger(MathOperationsBigInteger.class);
    private final static Random random = new Random();

    private MathOperationsBigInteger() {

    }

    /**
     * Decrements a BigInteger number
     * @param number any BigInteger number
     * @return this--
     */
    public static BigInteger decrement(BigInteger number) {
        return number.subtract(BigInteger.ONE);
    }

    /**
     * Increments a BigInteger number
     * @param number any BigInteger number
     * @return this++
     */
    public static BigInteger increment(BigInteger number) {
        return number.add(BigInteger.ONE);
    }

    /**
     * Determines whether a BigInteger number is even
     * @param number any BigInteger number
     * @return {@code true} if and only if n is even
     */
    public static boolean isEven(BigInteger number) {
//        return !number.testBit(0);
        return number.getLowestSetBit() != 0;
    }

    /**
     * Determines whether a BigInteger number is odd
     * @param number any BigInteger number
     * @return {@code true} if and only if n is odd
     */
    public static boolean isOdd(BigInteger number) {
        return !isEven(number);
    }

    /**
     * Finds greatest common divisor of two numbers
     * @param a any BigInteger number
     * @param b any BigInteger number
     * @return gcd(a, b)
     */
    public static BigInteger gcd(BigInteger a, BigInteger b) {
        if (a.equals(BigInteger.ZERO) && b.equals(BigInteger.ZERO)) {
            return BigInteger.ZERO;
        }

        if (a.compareTo(BigInteger.ZERO) < 0) {
            a = a.negate();
        }
        if (b.compareTo(BigInteger.ZERO) < 0) {
            b = b.negate();
        }

        BigInteger reminder;
        while (!b.equals(BigInteger.ZERO)) {
            reminder = a.remainder(b);
            a = b;
            b = reminder;
        }

        return a;
    }

    /**
     * Finds Bézout coefficients x, y and gcd(a, b) such that a * x + b * y = gcd(a, b)
     * @param a any BigInteger number
     * @param b any BigInteger number
     * @return BigInteger[] {gcd, x, y}: a * x + b * y = gcd
     */
    public static BigInteger[] gcdExtended(BigInteger a, BigInteger b) {
        BigInteger rPrev = a, r = b;
        BigInteger sPrev = BigInteger.ONE, s = BigInteger.ZERO;
        BigInteger tPrev = BigInteger.ZERO, t = BigInteger.ONE;

        while (!r.equals(BigInteger.ZERO)) {
            BigInteger quotient = rPrev.divide(r);
            BigInteger tmp;

            // (rPrev, r) := (r, rPrev − quotient * r)
            tmp = r;
            r = rPrev.subtract(quotient.multiply(r));
            rPrev = tmp;

            // (sPrev, s) := (s, sPrev − quotient * s)
            tmp = s;
            s = sPrev.subtract(quotient.multiply(s));
            sPrev = tmp;

            // (tPrev, t) := (t, tPrev − quotient * t)
            tmp = t;
            t = tPrev.subtract(quotient.multiply(t));
            tPrev = tmp;
        }

        if ((a.equals(BigInteger.ZERO) && b.compareTo(BigInteger.ZERO) < 0)
                || (b.equals(BigInteger.ZERO) && a.compareTo(BigInteger.ZERO) < 0)
                || (rPrev.compareTo(BigInteger.ZERO) < 0)) {
            rPrev = rPrev.negate();
            sPrev = sPrev.negate();
            tPrev = tPrev.negate();
        }

        // rPrev = gcd, sPrev and tPrev - Bezu coefficients: a * sPrev + b * tPrev = gcd
        return new BigInteger[] {rPrev, sPrev, tPrev};
    }

    /**
     * Determines whether a and b are coprime (relative prime)
     * @param s any BigInteger number
     * @param f any BigInteger number
     * @return {@code true} if and only if gcd(f, s) == 1
     */
    public static boolean isRelativePrime(BigInteger f, BigInteger s) {
        return gcd(f, s).equals(BigInteger.ONE);
    }

    /**
     * Raises number to degree by modulo
     * @param number any positive BigInteger number
     * @param degree any BigInteger number
     * @param modulo any positive BigInteger number
     * @return number^degree (mod modulo)
     */
    public static BigInteger fastPowMod(BigInteger number, BigInteger degree, BigInteger modulo) {
        if (degree.equals(BigInteger.ZERO)) {
            return BigInteger.ONE;
        }

        number = number.remainder(modulo);
        degree = degree.remainder(modulo);

        BigInteger toReturn = BigInteger.ONE;
        boolean flag = true;

        if (degree.compareTo(BigInteger.ZERO) < 0) {
            degree = degree.negate();
            flag = false;
        }

        while (degree.compareTo(BigInteger.ZERO) > 0) {
            if (isOdd(degree)) {
                toReturn = toReturn.multiply(number);
                toReturn = toReturn.remainder(modulo);
            }

            number = number.multiply(number);
            number = number.remainder(modulo);
            toReturn = toReturn.remainder(modulo);
            degree = degree.shiftRight(1);
        }

        if (!flag) {
            // todo: возвращать обратное число (ведь мы работаем в конечном поле чисел)
            return BigInteger.ONE;
        }

        return toReturn;
    }

    /**
     * Counts the Jacobin symbol for a number
     * @param a is any whole number
     * @param P is any positive odd P
     * @return symbol Jacobi (a/P)
     */
    public static int symbolJacobi(BigInteger a, BigInteger P) {
        if (isEven(P)) {
            throw new IllegalArgumentException("Param P must be even");
        }
        if (P.compareTo(BigInteger.ZERO) <= 0) {
            throw new IllegalArgumentException("Param P must be positive");
        }

        // Jacobi's symbol properties
        if (P.equals(BigInteger.ONE) || a.equals(BigInteger.ONE)) {
            return 1;
        }

        // calculation Jacobi's symbol
        // step 1
        if (!gcd(a, P).equals(BigInteger.ONE) || a.equals(BigInteger.ZERO)) {
            return 0;
        }

        // step 2
        int r = 1;

        BigInteger bigIntegerTHREE = BigInteger.valueOf(3L);
        BigInteger bigIntegerFOUR = BigInteger.valueOf(4L);
        BigInteger bigIntegerFIVE = BigInteger.valueOf(5L);
        BigInteger bigIntegerEIGHT = BigInteger.valueOf(8L);

        // step 3
        if (a.compareTo(BigInteger.ZERO) < 0) {
            a = a.negate();
            if (P.remainder(bigIntegerFOUR).equals(bigIntegerTHREE)) {
                r = -r;
            }
        }

        // step 4
        while (!a.equals(BigInteger.ZERO)) {
            int t = 0;
            while (isEven(a)) {
                ++t;
                a = a.divide(BigInteger.TWO);
            }

            if ((t & 1) == 1) {
                if (P.remainder(bigIntegerEIGHT).equals(bigIntegerTHREE) || P.remainder(bigIntegerEIGHT).equals(bigIntegerFIVE)) {
                    r = -r;
                }
            }

            // step 5
            if ((a.remainder(bigIntegerFOUR).equals(bigIntegerTHREE) && P.remainder(bigIntegerFOUR).equals(bigIntegerTHREE))) {
                r = -r;
            }

            BigInteger tmp = a;
            a = P.remainder(tmp);
            P = tmp;

            if (a.equals(BigInteger.ONE)) {
                break;
            }
        }

        // step 6
        return r;
    }

    /**
     * Generates a random number from upperLimit to 0 or 0 to upperLimit
     * @param upperLimit any BigInteger number
     * @return random BigInteger number between upperLimit and 0 or 0 and upperLimit
     */
    public static BigInteger generateRandomBigInteger(BigInteger upperLimit) {
        boolean flag = false;
        if (upperLimit.compareTo(BigInteger.ZERO) < 0) {
            upperLimit = upperLimit.negate();
            flag = true;
        }

        BigInteger randomNumber;
        do {
            randomNumber = new BigInteger(upperLimit.bitLength(), random);
        } while ((randomNumber.compareTo(upperLimit) >= 0));

        return flag ? randomNumber.negate() : randomNumber;
    }

    /**
     * Generates a random number from lowerLimit to upperLimit
     * @param lowerLimit any BigInteger number
     * @param upperLimit any BigInteger number
     * @return random BigInteger number between lowerLimit and upperLimit
     */
    public static BigInteger generateRandomBigInteger(BigInteger lowerLimit, BigInteger upperLimit) {
        // todo: maybe throws exception to this case?
        if (upperLimit.compareTo(lowerLimit) < 0) {
            BigInteger tmp = upperLimit;
            upperLimit = lowerLimit;
            lowerLimit = tmp;
        }

        return lowerLimit.add(generateRandomBigInteger(upperLimit.subtract(lowerLimit)));
    }

    /**
     * Generates a random probable prime number given bitLength and given minProbability
     * @param bitLength any positive integer
     * @param test any object of class ProbabilisticPrimalityTest
     * @param minProbability any double in [0.5; 1)
     * @return random BigInteger number between lowerLimit and upperLimit
     */
    public static BigInteger generateProbablePrime(int bitLength, ProbabilisticPrimalityTest test, double minProbability) {
        if (bitLength <= 0) {
            log.error("generateProbablePrime: Param bitLength must be positive");
            throw new IllegalArgumentException("generateProbablePrime: Param bitLength must be positive");
        }
        if (test == null) {
            log.error("generateProbablePrime: Param test mustn't be null");
            throw new IllegalArgumentException("generateProbablePrime: Param test mustn't be null");
        }
        if (minProbability < 0.5 || minProbability >= 1) {
            log.error("generateProbablePrime: Param minProbability must be in [0.5; 1)");
            throw new IllegalArgumentException("generateProbablePrime: Param minProbability must be in [0.5; 1)");
        }

        BigInteger lowerLimit = BigInteger.TWO.pow(bitLength - 1);
        BigInteger upperLimit = BigInteger.TWO.pow(bitLength).subtract(BigInteger.ONE);

        BigInteger probablyPrime;
        do {
            probablyPrime = generateRandomBigInteger(lowerLimit, upperLimit);
        } while (!test.probabilisticPrimalityTest(probablyPrime, minProbability));

        return probablyPrime;
    }

    /**
     * Determines (exactly) whether n is a prime number
     * @param number any BigInteger number
     * @return {@code true} if and only if n is a prime
     */
    public static boolean isPrime(BigInteger number) {
        if (number.compareTo(BigInteger.ZERO) <= 0) {
            return false;
        }

        if (number.equals(BigInteger.TWO)) {
            return true;
        }

        if (MathOperationsBigInteger.isEven(number) || number.equals(BigInteger.ONE)) {
            return false;
        }


        for (BigInteger i = new BigInteger("3"); i.compareTo(number.sqrt()) <= 0; i = i.add(BigInteger.TWO)) {
            BigInteger remainder = number.remainder(i);
            if (remainder.equals(BigInteger.ZERO)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Determines whether n is a perfect square
     * @param n any BigInteger number
     * @return {@code true} if and only if n is a perfect square
     */
    public static boolean isSquare(BigInteger n) {
        // constants needed
        BigInteger bigIntegerTHREE = BigInteger.valueOf(3L);
        BigInteger bigIntegerFIVE = BigInteger.valueOf(5L);
        BigInteger bigIntegerSIX = BigInteger.valueOf(6L);
        BigInteger bigIntegerSEVEN = BigInteger.valueOf(7L);
        BigInteger bigIntegerEIGHT = BigInteger.valueOf(8L);
        BigInteger bigIntegerNINE = BigInteger.valueOf(9L);
        BigInteger bigIntegerELEVEN = BigInteger.valueOf(11L);
        BigInteger bigIntegerTHIRTEEN = BigInteger.valueOf(13L);

        // trivial checks
        if (n.compareTo(BigInteger.ZERO) < 0) {
            return false;
        }
        if (n.equals(BigInteger.ZERO)) {
            return true;
        }

        // deleting last pairs of zeros: ...x0000 -> ...x
        // that equals reduction by powers of 4
        while (n.and(bigIntegerTHREE).equals(BigInteger.ZERO)) {
            n = n.shiftRight(2);
        }

        // all perfect squares, in binary,
        // end in 001, when powers of 4 are factored out
        if (!n.and(bigIntegerSEVEN).equals(BigInteger.ONE)) {
            return false;
        }

        // case when n is power of 4 or even power of 2
        if (n.equals(BigInteger.ONE)) {
            return true;
        }

        // modulo equivalency tests
        // n ≡ {3, 7} (mod 10) => n is non square etc.
        BigInteger lastDigit = n.remainder(BigInteger.TEN);
        if (lastDigit.equals(bigIntegerTHREE) || lastDigit.equals(bigIntegerSEVEN)) {
            return false;
        }

        BigInteger nRemainderSEVEN = n.remainder(bigIntegerSEVEN);
        if (nRemainderSEVEN.equals(bigIntegerTHREE)
                || nRemainderSEVEN.equals(bigIntegerFIVE)
                || nRemainderSEVEN.equals(bigIntegerSIX)) {
            return false;
        }

        BigInteger nRemainderNINE = n.remainder(bigIntegerNINE);
        if (nRemainderNINE.equals(BigInteger.TWO)
                || nRemainderNINE.equals(bigIntegerTHREE)
                || nRemainderNINE.equals(bigIntegerFIVE)
                || nRemainderNINE.equals(bigIntegerSIX)
                || nRemainderNINE.equals(bigIntegerEIGHT)) {
            return false;
        }

        BigInteger nRemainderTHIRTEEN = n.remainder(bigIntegerTHIRTEEN);
        if (nRemainderTHIRTEEN.equals(BigInteger.TWO)
                || nRemainderTHIRTEEN.equals(bigIntegerFIVE)
                || nRemainderTHIRTEEN.equals(bigIntegerSIX)
                || nRemainderTHIRTEEN.equals(bigIntegerSEVEN)
                || nRemainderTHIRTEEN.equals(bigIntegerEIGHT)
                || nRemainderTHIRTEEN.equals(bigIntegerELEVEN)) {
            return false;
        }

        // other patterns
        // checks if number ends in a 5
        if (lastDigit.equals(bigIntegerFIVE)) {
            BigInteger nDivideTEN = n.divide(BigInteger.TEN);
            BigInteger nDivideTENRemainderTEN = nDivideTEN.remainder(BigInteger.TEN);
            if (!nDivideTENRemainderTEN.equals(BigInteger.TWO)) {
                return false; // then it must end in 25
            }

            BigInteger nDivideHUNDRED = nDivideTEN.divide(BigInteger.TEN);
            BigInteger nDivideHUNDREDRemainderTEN = nDivideHUNDRED.divide(BigInteger.TEN);
            if (!(nDivideHUNDREDRemainderTEN.equals(BigInteger.ZERO)
                    || nDivideHUNDREDRemainderTEN.equals(BigInteger.TWO)
                    || nDivideHUNDREDRemainderTEN.equals(bigIntegerSIX))) {
                return false; // then must end 025, 225 or 625
            }

            BigInteger nDivideTHOUSENDRemainderTEN = nDivideHUNDRED.divide(BigInteger.TEN).remainder(BigInteger.TEN);
            if (nDivideHUNDREDRemainderTEN.equals(bigIntegerSIX)) {
                if (!(nDivideTHOUSENDRemainderTEN.equals(BigInteger.ZERO)
                        || nDivideTHOUSENDRemainderTEN.equals(bigIntegerFIVE))) {
                    return false; // then must end 0625 or 5625
                }
            }
        }
        else  {
            if (!n.divide(BigInteger.TEN).remainder(BigInteger.valueOf(4L)).equals(BigInteger.ZERO)) {
                return false; // (4k)*10 + (1,9)
            }
        }

        return n.sqrt().pow(2).equals(n);
    }

    /**
     * Converts fraction {@code numerator}/{@code denominator} to continued fraction
     * @param numerator any non-negative BigInteger
     * @param denominator any positive BigInteger
     * @return ArrayList of BigIntegers with continued fractions coefficients
     * @throws IllegalArgumentException {@code numerator} or {@code denominator} < 0, or {@code denominator} == 0
     */
    public static ArrayList<BigInteger> getContinuedFraction(BigInteger numerator, BigInteger denominator) {
        if ((numerator.compareTo(BigInteger.ZERO) < 0) || (denominator.compareTo(BigInteger.ZERO) < 0)) {
            log.error("getContinuedFraction: both of the numbers must be non negative");
            throw new IllegalArgumentException("getContinuedFraction: both of the numbers must be non negative");
        }

        if (denominator.equals(BigInteger.ZERO)) {
            log.error("getContinuedFraction: denominator mustn't be ZERO");
            throw new IllegalArgumentException("getContinuedFraction: denominator mustn't be ZERO");
        }

        ArrayList<BigInteger> toReturn = new ArrayList<>();

        while (!denominator.equals(BigInteger.ZERO)) {
            BigInteger remainder = numerator.remainder(denominator);
            toReturn.add(numerator.subtract(remainder).divide(denominator));
            numerator = denominator;
            denominator = remainder;
        }

        return toReturn;
    }
}
