package ru.mai.primality_test.impl;

import ru.mai.primality_test.ProbabilisticPrimalityTestAbstract;
import ru.mai.utils.MathOperationsBigInteger;

import java.math.BigInteger;

public class ProbabilisticPrimalityTestFermat extends ProbabilisticPrimalityTestAbstract {

    public ProbabilisticPrimalityTestFermat() {
        this.constantProbability = 0.5;
    }

    /**
     * Do not need this method for ProbabilisticPrimalityTestFermat
     * */
    @Override
    protected void doBeforeCycle(BigInteger number) {

    }

    @Override
    protected boolean oneRoundProbabilisticPrimalityTest(BigInteger number) {
        BigInteger primalityWitness = getNewPrimalityWitness(number);

        if (!MathOperationsBigInteger.gcd(number, primalityWitness).equals(BigInteger.ONE)) {
            return false;
        }

        return MathOperationsBigInteger.fastPowMod(primalityWitness, number.subtract(BigInteger.ONE), number).equals(BigInteger.ONE);
    }
}
