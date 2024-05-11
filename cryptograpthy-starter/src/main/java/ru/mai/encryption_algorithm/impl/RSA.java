package ru.mai.encryption_algorithm.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mai.encryption_algorithm.EncryptionAlgorithm;
import ru.mai.exceptions.IllegalArgumentExceptionWithLog;
import ru.mai.primality_test.ProbabilisticPrimalityTest;
import ru.mai.primality_test.impl.ProbabilisticPrimalityTestFermat;
import ru.mai.primality_test.impl.ProbabilisticPrimalityTestMillerRabin;
import ru.mai.primality_test.impl.ProbabilisticPrimalityTestSolovayStrassen;
import ru.mai.utils.MathOperationsBigInteger;
import ru.mai.utils.Pair;

import java.math.BigInteger;
import java.security.ProviderException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// todo: context must work with keySize byte for encrypt / decrypt
// todo: init vector size
public class RSA implements EncryptionAlgorithm {
    private static final Logger log = LoggerFactory.getLogger(RSA.class);
    private final KeyGenerationRSA generator;

    public RSA(ProbabilisticPrimalityTestEnum testEnum,
               double minProbability,
               int keyBitLength) {
        super();
        this.generator = new KeyGenerationRSA(testEnum, minProbability, keyBitLength);
    }

    public enum ProbabilisticPrimalityTestEnum {
        FERMAT,
        MILLER_RABIN,
        SOLOVAY_STRASSEN
    }

    private static class KeyGenerationRSA {
        private final ProbabilisticPrimalityTest test;
        private final double minProbability;
        private BigInteger p = null;
        private final int lenP; // in bytes
        private BigInteger q = null;
        private final int lenQ; // in bytes
        private final BigInteger minValuePQ;
        private final int diffSizePQ; // in bytes
        private BigInteger n = null;
        private final int keySize; // in bytes
        private BigInteger publicExponent = BigInteger.valueOf(65537);
        private BigInteger privateExponent = null;
        private boolean initiated = false;
        private final Lock lock = new ReentrantLock();

        public KeyGenerationRSA(ProbabilisticPrimalityTestEnum probabilisticPrimalityTest,
                                double minProbability,
                                int bitLength) {
            if (probabilisticPrimalityTest == null) {
                throw new IllegalArgumentExceptionWithLog("KeyGenerationRSA: Param probabilisticPrimalityTest mustn't be null", log);
            }
            if (minProbability < 0.5 || minProbability >= 1) {
                throw new IllegalArgumentExceptionWithLog("KeyGenerationRSA: Param minProbability must be in range [0.5, 1)", log);
            }
            if (bitLength < 16) {
                throw new IllegalArgumentExceptionWithLog("KeyGenerationRSA: Param bitLength must >= 16", log);
            }
            if (bitLength % 8 != 0) {
                throw new IllegalArgumentExceptionWithLog("KeyGenerationRSA: Param bitLength must be % 8", log);
            }

            this.test = getProbabilisticPrimalityTest(probabilisticPrimalityTest);
            this.minProbability = minProbability;
            this.keySize = bitLength / 4; // bit length / 8 * 2, / 8 => from bit length to byte, * 2 => len N or keySize

            this.lenP = (keySize + 1) >> 1;
            this.lenQ = keySize - lenP;

            // against Fermat's attack
            this.diffSizePQ = (int) (lenP - 0.1 * lenP);
            this.minValuePQ = BigInteger.TWO.pow(bitLength - 1);
        }

        private static ProbabilisticPrimalityTest getProbabilisticPrimalityTest(ProbabilisticPrimalityTestEnum enumGot) {
            switch (enumGot) {
                case FERMAT -> {
                    return new ProbabilisticPrimalityTestFermat();
                }
                case MILLER_RABIN -> {
                    return new ProbabilisticPrimalityTestMillerRabin();
                }
                default -> {
                    return new ProbabilisticPrimalityTestSolovayStrassen();
                }
            }
        }

        private void initPQAndN() {
            int i = 0;
            while (++i < 10 * lenP) {
                // generate probable prime
                BigInteger tmpP = MathOperationsBigInteger.generateProbablePrime(lenP * 8, test, minProbability);

                // p must be more than MIN_SIZE AND
                // gcd(PUBLIC_EXPONENT, phi(n)) == 1 = | phi(n) = (p - 1)(q - 1) | =>
                // => gcd(PUBLIC_EXPONENT, p - 1) == 1
                if (tmpP.compareTo(minValuePQ) > 0
                        && MathOperationsBigInteger.isRelativePrime(publicExponent, tmpP.subtract(BigInteger.ONE))) {
                    p = tmpP;
                    break;
                }
            }
            if (p == null) {
                throw new ProviderException("Cannot generate prime P");
            }

            i = 0;
            while (++i < 20 * lenQ) {
                // generate probable prime
                BigInteger tmpQ = MathOperationsBigInteger.generateProbablePrime(lenQ * 8, test, minProbability);

                // p must be more than MIN_SIZE AND
                // gcd(PUBLIC_EXPONENT, phi(n)) == 1 => gcd(PUBLIC_EXPONENT, q - 1) == 1 AND
                // to prevent Fermat attack P and Q must not be close to each other
                if (tmpQ.compareTo(minValuePQ) > 0
                        && MathOperationsBigInteger.isRelativePrime(publicExponent, tmpQ.subtract(BigInteger.ONE))
                        && p.subtract(tmpQ).abs().compareTo(BigInteger.TWO.pow(diffSizePQ)) > 0) {
                    q = tmpQ;
                    break;
                }
            }
            if (q == null) {
                throw new ProviderException("Cannot generate prime Q");
            }

            n = p.multiply(q);
        }

        /**
         * Generates d such that e * d === 1 (mod phi)
         * <p>
         * and 1/3 N <sup>{@code 1/4}</sup> < d < 2<sup>{@code p.bitLength}</sup>
         */
        private void generateD() {
            // phi(n) = (p - 1) (q - 1)
            BigInteger pMinus1 = p.subtract(BigInteger.ONE);
            BigInteger qMinus1 = q.subtract(BigInteger.ONE);
            BigInteger phi = pMinus1.multiply(qMinus1);

            // d * e === 1 mod (phi(n))
            // find d as a modular multiplicative inverse to e with gcdExtended (мультипликативное обратное по модулю)
            // gcdExtended(a, b) returns gcd(a, b) and x, y: a * x + b * y = gcd(a, b)
            // to find d, put b = phi(n)
            // note: gcd(e, phi(n)) == 1, so gcd(a, b) = 1
            // e * x + phi(n) * y = 1
            // take modulo phi(n):
            // e * x + phi(n) * y = 1 mod (phi(n))
            // note: (phi(n) * y) mod (phi(n)) == 0
            // e * x = 1 mod (phi(n))
            // so, x = d can be found with gcdExtended(e, phi(n))

            privateExponent = MathOperationsBigInteger.gcdExtended(publicExponent, phi)[1];
            if (privateExponent.compareTo(BigInteger.ZERO) < 0) {
                privateExponent = privateExponent.add(phi);
            }

            BigInteger minSize = n.sqrt().sqrt().divide(BigInteger.valueOf(3)); // Wiener's theorem

            while (privateExponent.compareTo(minSize) < 0) {
                regenerateE(phi);
                privateExponent = MathOperationsBigInteger.gcdExtended(publicExponent, phi)[1];
                if (privateExponent.compareTo(BigInteger.ZERO) < 0) {
                    privateExponent = privateExponent.add(phi);
                }
            }
        }

        /**
         * Calculates publicExponent as ((e - 1) << 1) - 1 such that gcd(e, phi) = 1
         *
         * @param phi Euler function of n, phi = (p - 1) * (q - 1)
         */
        private void regenerateE(BigInteger phi) {
            do {
                publicExponent = publicExponent.subtract(BigInteger.ONE).shiftLeft(1).add(BigInteger.ONE);
            } while (!publicExponent.gcd(phi).equals(BigInteger.ONE)); // check: n and e must be coprime
        }

        private void checkInitiated() {
            if (!initiated) {
                // weak place for multithreading
                lock.lock();
                if (initiated) {
                    // first thread locked and initiated p, q, n, d.
                    // second thread saw initiated = false (generating p, q, n, d might take quiet a long time) and
                    // waited till first thread unlocked. Now second thread wants to initiate p, q, n, d again.
                    // But we catch it here.
                    lock.unlock();
                    return;
                }
                try {
                    initPQAndN();
                    generateD();
                    initiated = true;
                } finally {
                    lock.unlock();
                }
            }
        }


        /**
         * @return pair of BigInteger, public exponent and N
         */
        public Pair<BigInteger, BigInteger> getPublicKey() {
            checkInitiated();
            return new Pair<>(publicExponent, n);
        }

        /**
         * @return pair of BigInteger, private exponent and N
         */
        public Pair<BigInteger, BigInteger> getPrivateKey() {
            checkInitiated();
            return new Pair<>(privateExponent, n);
        }

        public void generateNewKeyPair() {
            // todo: ask Ilusha how we can regenerateE for Hastad attack
            initPQAndN();
            generateD();
            initiated = true;
        }
    }

    /**
     * @return encrypted block with leading zero-byte
     */
    @Override
    public byte[] encrypt(byte[] input) {
        if (input.length > getAlgorithmBlockForEncryption()) {
            throw new IllegalArgumentExceptionWithLog("encrypt: Param input must have length less or equal to algorithm block", log);
        }
        if (input.length == 0) {
            return new byte[0];
        }

        Pair<BigInteger, BigInteger> publicKey = generator.getPublicKey();

        // if encrypted block have size > keySize then first bit is sign bit 0 and it must be deleted
        byte[] encrypted = MathOperationsBigInteger.fastPowMod(new BigInteger(input), publicKey.getKey(), publicKey.getValue()).toByteArray();

        if (encrypted.length <= generator.keySize) {
            // add leading '0'
            byte[] tmp = new byte[getAlgorithmBlockForDecryption()];
            System.arraycopy(encrypted, 0, tmp, getAlgorithmBlockForDecryption() - encrypted.length, encrypted.length);
            encrypted = tmp;
        }

        return encrypted;
    }

    @Override
    public byte[] decrypt(byte[] input) {
        if (input.length > getAlgorithmBlockForDecryption()) {
            throw new IllegalArgumentExceptionWithLog("decrypt: Param input must have length less or equal to algorithm block", log);
        }
        if (input.length == 0) {
            return new byte[0];
        }

        Pair<BigInteger, BigInteger> privateKey = generator.getPrivateKey();
        return MathOperationsBigInteger.fastPowMod(new BigInteger(input), privateKey.getKey(), privateKey.getValue()).toByteArray();
    }

    public Pair<BigInteger, BigInteger> getPublicKey() {
        return this.generator.getPublicKey();
    }

    public Pair<BigInteger, BigInteger> generateNewKeyPair() {
        this.generator.generateNewKeyPair();
        return getPublicKey();
    }

    /**
     * Returns max count of bytes able to encrypt or decrypt
     */
    @Override
    public int getAlgorithmBlockForEncryption() {
        return generator.keySize - 1;
    }

    @Override
    public int getAlgorithmBlockForDecryption() {
        return generator.keySize + 1;
    }
}
