package ru.mai.primality_test.impl;

import ru.mai.primality_test.ProbabilisticPrimalityTestAbstract;
import ru.mai.utils.MathOperationsBigInteger;

import java.math.BigInteger;

public class ProbabilisticPrimalityTestMillerRabin extends ProbabilisticPrimalityTestAbstract {
    private int s = 0;
    private BigInteger t = BigInteger.ZERO;
    private BigInteger numberDecremented = BigInteger.ZERO;

    public ProbabilisticPrimalityTestMillerRabin() {
        this.constantProbability = 0.25;
    }

    @Override
    protected void doBeforeCycle(BigInteger number) {
        numberDecremented = MathOperationsBigInteger.decrement(number);
        s = numberDecremented.getLowestSetBit();
        t = numberDecremented.divide(BigInteger.TWO.pow(s));
    }

    @Override
    protected boolean oneRoundProbabilisticPrimalityTest(BigInteger number) {
        BigInteger primalityWitness = getNewPrimalityWitness(number);

        if (!MathOperationsBigInteger.gcd(number, primalityWitness).equals(BigInteger.ONE)) {
            return false;
        }

        BigInteger x = MathOperationsBigInteger.fastPowMod(primalityWitness, t, number);
        if (x.equals(BigInteger.ONE) || x.equals(numberDecremented)) {
            return true;
        }

        for (int i = 0; i < s - 1; i++) {
            x = MathOperationsBigInteger.fastPowMod(x, BigInteger.TWO, number);
            if (x.equals(BigInteger.ONE)) {
                return false;
            }

            if (x.equals(numberDecremented)) {
                return true;
            }
        }

        return false;
    }
}
