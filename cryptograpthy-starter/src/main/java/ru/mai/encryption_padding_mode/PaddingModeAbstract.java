package ru.mai.encryption_padding_mode;

public abstract class PaddingModeAbstract implements PaddingMode {
    protected int sizeNeeded;

    @Override
    public byte[] erasePad(byte[] input) {
        int addedBytes = input[input.length - 1] & 0xff;

        if (sizeNeeded <= addedBytes) {
            return input;
        }

        byte[] paddingErased = new byte[input.length - addedBytes];
        System.arraycopy(input, 0, paddingErased, 0, input.length - addedBytes);
        return paddingErased;
    }
}
