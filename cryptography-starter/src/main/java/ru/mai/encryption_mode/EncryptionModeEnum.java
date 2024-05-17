package ru.mai.encryption_mode;

public enum EncryptionModeEnum {
    ECB("Electronic codebook"),
    CBC("Cipher block chaining"),
    PCBC("Propagating cipher block chaining"),
    CFB("Cipher feedback"),
    OFB("Output feedback"),
    CTR("Counter mode"),
    RANDOM_DELTA("Random Delta");

    private final String title;

    EncryptionModeEnum(String s) {
        this.title = s;
    }

    public String getTitle() {
        return title;
    }

    public boolean needsInitVector() {
        return this != ECB;
    }
}

