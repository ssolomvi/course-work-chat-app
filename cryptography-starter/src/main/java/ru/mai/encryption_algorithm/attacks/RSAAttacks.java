package ru.mai.encryption_algorithm.attacks;

import ru.mai.utils.Pair;

import java.math.BigInteger;

public interface RSAAttacks {
    // d, phi
    Pair<BigInteger, BigInteger> attack(Pair<BigInteger, BigInteger> publicKey);
}
