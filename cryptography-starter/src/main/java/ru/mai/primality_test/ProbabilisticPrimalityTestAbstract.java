package ru.mai.primality_test;

import ru.mai.utils.MathOperationsBigInteger;

import java.math.BigInteger;
import java.util.Set;
import java.util.TreeSet;

public abstract class ProbabilisticPrimalityTestAbstract implements ProbabilisticPrimalityTest {
    // for different tests the constant takes different values, e.g.
    // for Ferma and Solovay-Strassen tests constantProbability = 1/2;
    // for Miller–Rabin test constantProbability = 1/4.

    private final Set<BigInteger> primalityWitnesses = new TreeSet<>();
    protected double constantProbability;

    private int getRoundCount(double minProbability) {
        return (int) Math.ceil(Math.log10(1 - minProbability) / Math.log10(constantProbability));
    }

    protected ProbabilisticPrimalityTestAbstract() {
    }

    protected BigInteger getNewPrimalityWitness(BigInteger upperLimit) {
        BigInteger randomNumber;
        do {
            randomNumber = MathOperationsBigInteger.generateRandomBigInteger(upperLimit.subtract(BigInteger.TWO));

            if (randomNumber.compareTo(BigInteger.TWO) < 0) {
                randomNumber = randomNumber.add(BigInteger.TWO);
            }

            if (!primalityWitnesses.contains(randomNumber)) {
                primalityWitnesses.add(randomNumber);
                break;
            }
        } while (true);

        return randomNumber;
    }

    /**
     * @param number         a candidate of primality
     * @param minProbability a minimal value of probability for primality test in interval [0.5, 1)
     */
    @Override
    public boolean probabilisticPrimalityTest(BigInteger number, double minProbability) {
        if (MathOperationsBigInteger.isEven(number) || number.equals(BigInteger.ZERO) ||
                (number.equals(BigInteger.ONE)) || number.equals(BigInteger.TWO)) {
            return false;
        }

        int roundCount = getRoundCount(minProbability);
        doBeforeCycle(number);
        for (int i = 0; i < roundCount; i++) {
            if (!oneRoundProbabilisticPrimalityTest(number)) {
                primalityWitnesses.clear();
                return false;
            }
        }

        primalityWitnesses.clear();
        return true;
    }

    protected abstract void doBeforeCycle(BigInteger number);

    protected abstract boolean oneRoundProbabilisticPrimalityTest(BigInteger number);
}
