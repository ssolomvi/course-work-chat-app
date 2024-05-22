package ru.mai.encryption_padding_mode;

public enum PaddingModeEnum {
    ZEROES("Zeroes"),
    ANSI_X_923("ANSI_X_923"),
    PKCS7("PKCS7"),
    ISO10126("ISO10126");

    private final String title;

    PaddingModeEnum(String s) {
        this.title = s;
    }

    public String getTitle() {
        return title;
    }
}