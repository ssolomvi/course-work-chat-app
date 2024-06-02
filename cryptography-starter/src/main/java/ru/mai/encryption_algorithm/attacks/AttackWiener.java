package ru.mai.encryption_algorithm.attacks;

import ru.mai.utils.MathOperationsBigInteger;
import ru.mai.utils.Pair;

import java.math.BigInteger;
import java.util.ArrayList;

public class AttackWiener implements RSAAttacks {

    // an array that contains all convergent fractions taken from last attack
    // contains pairs <p, q> where p/q - convergent fraction
    private final ArrayList<Pair<BigInteger, BigInteger>> convergentFractions = new ArrayList<>();

    public ArrayList<Pair<BigInteger, BigInteger>> getConvergentFractions() {
        return convergentFractions;
    }

    public void printConvergentFractions() {
        for (Pair<BigInteger, BigInteger> convergentFraction : convergentFractions) {
            System.out.println(convergentFraction.getKey() + " / " + convergentFraction.getValue());
        }
    }

    /**
     * Calculates next convergent fraction for coefficient {@code a}.
     * If convergentFractions contains zero elements, p0 = {@code a}, q0 = 1.
     * If convergentFractions contains one element, p1 = a * p0 + 1, q1 = a * q0 + 0.
     * Else p_n = a_n * (p_(n-1)) + (p_(n-2)), q_n = q_n * (q_(n-1)) + (q_(n-2)).
     *
     * @param a coefficient from continued fraction.
     * @return pair that represents convergent fraction for this {@code a}.
     */
    private Pair<BigInteger, BigInteger> getNextConvergentFraction(BigInteger a) {
        if (convergentFractions.isEmpty()) {
            // p_0 = a_0, q_0 = 1
            convergentFractions.add(new Pair<>(a, BigInteger.ONE));
        } else if (convergentFractions.size() == 1) {
            // p_-1 = 1, q_-1 = 0
            convergentFractions.add(new Pair<>(a.multiply(convergentFractions.get(0).getKey()).add(BigInteger.ONE),
                    a.multiply(convergentFractions.get(0).getValue()).add(BigInteger.ZERO)));
        } else {
            // p_n = a_n * (p_(n-1)) + (p_(n-2))
            // q_n = q_n * (q_(n-1)) + (q_(n-2))

            int lastIndex = convergentFractions.size() - 1;
            BigInteger p_n1 = convergentFractions.get(lastIndex).getKey();
            BigInteger p_n2 = convergentFractions.get(lastIndex - 1).getKey();
            BigInteger q_n1 = convergentFractions.get(lastIndex).getValue();
            BigInteger q_n2 = convergentFractions.get(lastIndex - 1).getValue();
            convergentFractions.add(new Pair<>(a.multiply(p_n1).add(p_n2),
                    a.multiply(q_n1).add(q_n2)));
        }

        return convergentFractions.get(convergentFractions.size() - 1);
    }

    /**
     * Calculates privateExponent and phi from publicExponent and n (modulo RSA)
     *
     * @param publicKey a {@code Pair} where Key = publicExponent, Value = n (modulo RSA)
     * @return a {@code Pair} where Key = privateExponent, Value = phi
     */
    @Override
    public Pair<BigInteger, BigInteger> attack(Pair<BigInteger, BigInteger> publicKey) {
        convergentFractions.clear();
        BigInteger bigIntegerFOUR = BigInteger.valueOf(4L);
        BigInteger e = publicKey.getKey(), n = publicKey.getValue();
        BigInteger phiToReturn = BigInteger.ZERO, dToReturn = BigInteger.ZERO;

        var continuedFractionForPublicKey = MathOperationsBigInteger.getContinuedFraction(e, n);

        for (BigInteger currCoefficientOfContinuedFraction : continuedFractionForPublicKey) {
            var currConvergentFraction = getNextConvergentFraction(currCoefficientOfContinuedFraction);
            BigInteger k = currConvergentFraction.getKey();
            BigInteger d = currConvergentFraction.getValue();

            if (k.equals(BigInteger.ZERO)) {
                continue;
            }

            BigInteger phi = e.multiply(d).subtract(BigInteger.ONE).divide(k);

            // for quadratic equation
            BigInteger b = n.subtract(phi).add(BigInteger.ONE);
            BigInteger discriminant = b.pow(2).subtract(bigIntegerFOUR.multiply(n));
            if (discriminant.compareTo(BigInteger.ZERO) < 0) {
                continue;
            }

            BigInteger p = b.negate().subtract(discriminant.sqrt()).divide(BigInteger.TWO);
            BigInteger q = b.negate().add(discriminant.sqrt()).divide(BigInteger.TWO);

            if (p.multiply(q).equals(n)) {
                phiToReturn = phi;
                dToReturn = d;
            }
        }

        return new Pair<>(dToReturn, phiToReturn);
    }
}
