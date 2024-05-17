package ru.mai.utils;

public class Permutation {
    private Permutation() {

    }

    public static byte[] permute(byte[] block, int[] pBlock, Operations.IndexingRule indexRule) {
        byte[] result = new byte[pBlock.length / 8 + (pBlock.length % 8 == 0 ? 0 : 1)];

        int j = 0;
        if (indexRule == Operations.IndexingRule.FROM_MOST_TO_LEAST_START_WITH_1
        || indexRule == Operations.IndexingRule.FROM_LEAST_TO_MOST_START_WITH_1) {
            j++;
        }

        for (int i = j ; i < pBlock.length + j; i++) {
            Operations.setBit(i, Operations.getBit(pBlock[i - j], block, indexRule), result, indexRule);
        }

        return result;
    }
}
