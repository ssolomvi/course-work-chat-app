package ru.mai.round_keys;


/**
 * An interface that provides a description of the functionality for the key expansion procedure
 * (generating round keys) (method parameter: input key - byte array, result
 * - array of round keys (each round key is an array of bytes));
 */
public interface RoundKeyGeneration {
    byte[][] generateRoundKeys(byte[] key);
}
