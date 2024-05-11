package ru.mai.encryption_padding_mode.impl;

import ru.mai.encryption_padding_mode.PaddingModeAbstract;
import ru.mai.utils.Operations;

public class ISO10126 extends PaddingModeAbstract {
    public ISO10126(int sizeBlock) {
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
        byte[] randomBytes = Operations.generateBytes(sizeToAdd - 1);

        System.arraycopy(input, 0, toReturn, 0, inputSize);
        // добавляем sizeToAdd - 1 случайных байтов в конец результирующего массива
        System.arraycopy(randomBytes, 0, toReturn, inputSize, sizeToAdd - 1);

        toReturn[toReturnSize - 1] = (byte) (0xFF & sizeToAdd);

        return toReturn;
    }
}
