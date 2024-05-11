package ru.mai.encryption_padding_mode.impl;

import ru.mai.encryption_padding_mode.PaddingModeAbstract;

public class PKCS7 extends PaddingModeAbstract {
    public PKCS7(int sizeBlock) {
        sizeNeeded = sizeBlock;
    }

    @Override
    public byte[] pad(byte[] input) {
        int inputSize = input.length;
        if (inputSize % sizeNeeded == 0) {
            return input;
        }

        byte sizeToAdd = (byte) (sizeNeeded - inputSize % sizeNeeded);
        int toReturnSize = (inputSize / sizeNeeded + 1) * sizeNeeded;
        byte[] toReturn = new byte[toReturnSize];

        for (int i = toReturnSize - 1; i > inputSize - 1; i--) {
            toReturn[i] = (byte) (0xFF & sizeToAdd);
        }

        System.arraycopy(input, 0, toReturn, 0, inputSize);

        return toReturn;
    }
}
