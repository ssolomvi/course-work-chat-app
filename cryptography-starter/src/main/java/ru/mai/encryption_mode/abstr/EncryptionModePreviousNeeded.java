package ru.mai.encryption_mode.abstr;

// CBC, CFB
public interface EncryptionModePreviousNeeded extends EncryptionModeWithInitVector {

    /**
     * @param curr current encrypted block to decrypt.
     * @param prev previous encrypted block. If one passes the first block, it must be equal to null.
     */
    byte[] decrypt(byte[] curr, byte[] prev);
}
