package ru.mai.round_keys.DEAL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mai.encryption_algorithm.impl.DEAL;
import ru.mai.encryption_algorithm.impl.DES;
import ru.mai.exceptions.IllegalArgumentExceptionWithLog;
import ru.mai.round_keys.RoundKeyGeneration;
import ru.mai.utils.Operations;

public class RoundKeyGenerationDEALDirect implements RoundKeyGeneration {
    private static final Logger log = LoggerFactory.getLogger(RoundKeyGenerationDEALDirect.class);

    // 0123456789abcdef -- fixed initial key for DES
    // 0000_0001 0010_0011 0100_0101 0110_0111
    // 1000_1001 1010_1011 1100_1101 1110_1111
    // ->
    // 0000_000 0010_001 0100_010 0110_011
    // 1000_100 1010_101 1100_110 1110_111
    // ->
    // 0000_0000 0100_0101 0001_0011 0011_1000
    // 1001_0101 0111_0011 0111_0111
    private static final byte[] keyDES = {0, 69, 19, 56, (byte) 149, 115, 119};

    // for all constants i-1 th bit is set and others are cleared
    // c1 = hex: 80 00 00 00 00 00 00 00
    private static final byte[] constant1 = {(byte) 128, 0, 0, 0, 0, 0, 0, 0};

    // c2 = hex: 40 00 00 00 00 00 00 00
    private static final byte[] constant2 = {64, 0, 0, 0, 0, 0, 0, 0};

    // c4 = hex: 10 00 00 00 00 00 00 00
    private static final byte[] constant4 = {16, 0, 0, 0, 0, 0, 0, 0};

    // c8 = hex: 01 00 00 00 00 00 00 00
    private static final byte[] constant8 = {1, 0, 0, 0, 0, 0, 0, 0};

    private static final DES des = new DES(keyDES);

    @Override
    public byte[][] generateRoundKeys(byte[] key) {
        if (key == null || !(key.length == DEAL.KEY_LENGTH16 || key.length == DEAL.KEY_LENGTH24 || key.length == DEAL.KEY_LENGTH32)) {
            throw new IllegalArgumentExceptionWithLog("Passed param key is not appropriate for generating round DEAL keys. Must be of length 16, 24 or 32 bytes", log);
        }

        int roundCount;
        byte[][] roundKeys;
        if (key.length == 16) {
            roundCount = 6;
            roundKeys = new byte[roundCount][8];

            byte[][] dividedKey = new byte[2][8];

            System.arraycopy(key, 0, dividedKey[0], 0, 8);
            System.arraycopy(key, 8, dividedKey[1], 0, 8);

            roundKeys[0] = des.encrypt(dividedKey[0]);
            roundKeys[1] = des.encrypt(Operations.xor(dividedKey[1], roundKeys[0]));
            roundKeys[2] = des.encrypt(Operations.xor(Operations.xor(dividedKey[0], constant1), roundKeys[1]));
            roundKeys[3] = des.encrypt(Operations.xor(Operations.xor(dividedKey[1], constant2), roundKeys[2]));
            roundKeys[4] = des.encrypt(Operations.xor(Operations.xor(dividedKey[0], constant4), roundKeys[3]));
            roundKeys[5] = des.encrypt(Operations.xor(Operations.xor(dividedKey[1], constant8), roundKeys[4]));

        } else if (key.length == 24) {
            roundCount = 6;
            roundKeys = new byte[roundCount][8];

            byte[][] dividedKey = new byte[3][8];

            System.arraycopy(key, 0, dividedKey[0], 0, 8);
            System.arraycopy(key, 8, dividedKey[1], 0, 8);
            System.arraycopy(key, 16, dividedKey[2], 0, 8);

            roundKeys[0] = des.encrypt(dividedKey[0]);
            roundKeys[1] = des.encrypt(Operations.xor(dividedKey[1], roundKeys[0]));
            roundKeys[2] = des.encrypt(Operations.xor(dividedKey[2], roundKeys[1]));
            roundKeys[3] = des.encrypt(Operations.xor(Operations.xor(dividedKey[0], constant1), roundKeys[2]));
            roundKeys[4] = des.encrypt(Operations.xor(Operations.xor(dividedKey[1], constant2), roundKeys[3]));
            roundKeys[5] = des.encrypt(Operations.xor(Operations.xor(dividedKey[2], constant4), roundKeys[4]));

        } else {
            roundCount = 8;
            roundKeys = new byte[roundCount][8];

            byte[][] dividedKey = new byte[4][8];

            System.arraycopy(key, 0, dividedKey[0], 0, 8);
            System.arraycopy(key, 8, dividedKey[1], 0, 8);
            System.arraycopy(key, 16, dividedKey[2], 0, 8);
            System.arraycopy(key, 24, dividedKey[3], 0, 8);

            roundKeys[0] = des.encrypt(dividedKey[0]);
            roundKeys[1] = des.encrypt(Operations.xor(dividedKey[1], roundKeys[0]));
            roundKeys[2] = des.encrypt(Operations.xor(dividedKey[2], roundKeys[1]));
            roundKeys[3] = des.encrypt(Operations.xor(dividedKey[3], roundKeys[2]));
            roundKeys[4] = des.encrypt(Operations.xor(Operations.xor(dividedKey[0], constant1), roundKeys[3]));
            roundKeys[5] = des.encrypt(Operations.xor(Operations.xor(dividedKey[1], constant2), roundKeys[4]));
            roundKeys[6] = des.encrypt(Operations.xor(Operations.xor(dividedKey[2], constant4), roundKeys[5]));
            roundKeys[7] = des.encrypt(Operations.xor(Operations.xor(dividedKey[3], constant8), roundKeys[6]));
        }

        return roundKeys;
    }
}
