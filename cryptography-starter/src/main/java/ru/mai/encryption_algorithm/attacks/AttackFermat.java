package ru.mai.encryption_algorithm.attacks;

import ru.mai.utils.MathOperationsBigInteger;
import ru.mai.utils.Pair;

import java.math.BigInteger;

public class AttackFermat implements RSAAttacks {
    /**
     * Calculates privateExponent and phi from publicExponent and n (modulo RSA)
     * @param publicKey a {@code Pair} where Key = publicExponent, Value = n (modulo RSA)
     * @return a {@code Pair} where Key = privateExponent, Value = phi
     */
    @Override
    public Pair<BigInteger, BigInteger> attack(Pair<BigInteger, BigInteger> publicKey) {
        BigInteger publicExponent = publicKey.getKey();
        BigInteger n = publicKey.getValue();

        BigInteger[] PAndQ = findPAndQ(n);

        BigInteger phi = PAndQ[0].subtract(BigInteger.ONE).multiply(PAndQ[1].subtract(BigInteger.ONE));
        BigInteger privateExponent = findD(publicExponent, phi);

        return new Pair<>(privateExponent, phi);
    }

    private static BigInteger[] findPAndQ(BigInteger n) {
        BigInteger[] nSqrt = n.sqrtAndRemainder();
        if (nSqrt[1].equals(BigInteger.ZERO)) {
            return new BigInteger[] {nSqrt[0], nSqrt[0]};
        }

        BigInteger probablyA = nSqrt[0], probablyB;
        BigInteger probablyASquared = probablyA.pow(2), probablyBSquared;

        do {
            probablyASquared = probablyASquared.
                    add(probablyA.multiply(BigInteger.TWO)).
                    add(BigInteger.ONE);
            probablyA = probablyA.add(BigInteger.ONE);
            probablyBSquared = probablyASquared.subtract(n);
        } while(!MathOperationsBigInteger.isSquare(probablyBSquared));

        probablyB = probablyBSquared.sqrt();
        return new BigInteger[] {probablyA.add(probablyB), probablyA.subtract(probablyB)};
    }

    private BigInteger findD(BigInteger publicExponent, BigInteger phi) {
        // privateExponent is the part of solution Diophantine equation d * e ≡ 1 (mod phi)
        return MathOperationsBigInteger.gcdExtended(publicExponent, phi)[1];
    }
}