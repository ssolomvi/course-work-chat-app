package ru.mai.encryption_padding_mode.impl;

import ru.mai.encryption_padding_mode.PaddingModeAbstract;

public class Zeroes extends PaddingModeAbstract {
    public Zeroes(int sizeBlock) {
        sizeNeeded = sizeBlock;
    }

    @Override
    public byte[] pad(byte[] input) {
        int inputSize = input.length;
        if (inputSize % sizeNeeded == 0) {
            return input;
        }

        byte[] toReturn = new byte[(inputSize / sizeNeeded + 1) * sizeNeeded];
        System.arraycopy(input, 0, toReturn, 0, input.length);

        return toReturn;
    }

    @Override
    public byte[] erasePad(byte[] input) {
        int counterEraseZeroes = 0;
        for (int i = input.length - 1; i > 1; i--) {
            if (input[i] == 0) {
                ++counterEraseZeroes;
            } else {
                break;
            }
        }

        int toReturnSize = input.length - counterEraseZeroes;
        byte[] toReturn = new byte[toReturnSize];
        System.arraycopy(input, 0, toReturn, 0, toReturnSize);

        return toReturn;
    }
}
