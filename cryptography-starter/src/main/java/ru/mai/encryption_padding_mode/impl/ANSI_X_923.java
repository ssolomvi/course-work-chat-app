package ru.mai.encryption_padding_mode.impl;

import ru.mai.encryption_padding_mode.PaddingModeAbstract;

public class ANSI_X_923 extends PaddingModeAbstract {
    public ANSI_X_923(int sizeBlock) {
        sizeNeeded = sizeBlock;
    }

    /**
     * Adds bytes up to length of input will have size needed as a delimeter
     * */
    @Override
    public byte[] pad(byte[] input) {
        int inputSize = input.length;
        if (inputSize % sizeNeeded == 0) {
            return input;
        }

        byte sizeToAdd = (byte) (sizeNeeded - inputSize % sizeNeeded);
        int toReturnSize = (inputSize / sizeNeeded + 1) * sizeNeeded;
        byte[] toReturn = new byte[toReturnSize];
        System.arraycopy(input, 0, toReturn, 0, inputSize);

        toReturn[toReturnSize - 1] = (byte) (0xFF & sizeToAdd);

        return toReturn;
    }
}
