package ru.mai.encryption_padding_mode;

public interface PaddingMode {
    byte[] pad(byte[] input);
    byte[] erasePad(byte[] input);
}
