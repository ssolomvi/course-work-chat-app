package ru.mai.primality_test.impl;

import ru.mai.primality_test.ProbabilisticPrimalityTestAbstract;
import ru.mai.utils.MathOperationsBigInteger;

import java.math.BigInteger;

public class ProbabilisticPrimalityTestSolovayStrassen extends ProbabilisticPrimalityTestAbstract {

    private BigInteger numberDecremented = BigInteger.ZERO;
    private BigInteger halfNumberDecremented = BigInteger.ZERO;

    public ProbabilisticPrimalityTestSolovayStrassen() {
        this.constantProbability = 0.5;
    }

    @Override
    protected void doBeforeCycle(BigInteger number) {
        numberDecremented = MathOperationsBigInteger.decrement(number);
        halfNumberDecremented = numberDecremented.divide(BigInteger.TWO);
    }

    @Override
    protected boolean oneRoundProbabilisticPrimalityTest(BigInteger number) {
        BigInteger primalityWitness = getNewPrimalityWitness(number);

        if (!MathOperationsBigInteger.gcd(number, primalityWitness).equals(BigInteger.ONE)) {
            return false;
        }

        long symbolJacobi = MathOperationsBigInteger.symbolJacobi(primalityWitness, number);
        BigInteger eulerCriterion = MathOperationsBigInteger.fastPowMod(primalityWitness, halfNumberDecremented, number);

        if (eulerCriterion.equals(numberDecremented)) {
            eulerCriterion = BigInteger.valueOf(-1L);
        }

        return eulerCriterion.equals(BigInteger.valueOf(symbolJacobi));
    }
}
