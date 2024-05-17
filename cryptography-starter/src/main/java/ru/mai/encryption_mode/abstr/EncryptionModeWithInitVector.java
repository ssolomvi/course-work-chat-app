package ru.mai.encryption_mode.abstr;

import ru.mai.encryption_mode.EncryptionMode;

public interface EncryptionModeWithInitVector extends EncryptionMode {
    void invokeNextAsNew();
}
