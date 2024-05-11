package ru.mai.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Random;
import java.util.StringJoiner;

public class Operations {
    private static final Random random = new Random();

    public static byte[][] reverseArrayOfArray(byte[][] bytes) {
        byte[][] reversed = bytes.clone();
        for (int i = 0; i < reversed.length / 2; i++) {
            byte[] temp = reversed[i];
            reversed[i] = reversed[reversed.length - i - 1];
            reversed[reversed.length - i - 1] = temp;
        }
        return reversed;
    }

    public enum IndexingRule {
        FROM_LEAST_TO_MOST_START_WITH_0,
        FROM_LEAST_TO_MOST_START_WITH_1,
        FROM_MOST_TO_LEAST_START_WITH_0,
        FROM_MOST_TO_LEAST_START_WITH_1
    }

    private static void checkOutOfBounds(int index, int blockLen, IndexingRule indexRule) {
        int bounder;
        if (indexRule == IndexingRule.FROM_LEAST_TO_MOST_START_WITH_0
                || indexRule == IndexingRule.FROM_MOST_TO_LEAST_START_WITH_0) {
            bounder = blockLen * 8 - 1;
        } else {
            bounder = blockLen * 8;
        }
        if (index > bounder) {
            throw new IndexOutOfBoundsException("Param index cannot be more than " + bounder);
        }
    }

    /**
     * <p>
     * Getting a bit is possible with: byte & (1 << shift) != 0
     * <p>
     * Where byte is byte accessible through [] from param block, shift is count of bits to shift,
     * shift == position, which is relative index of bit (that is, position in certain block)
     *
     * @param index     index by which to retrieve a bit
     * @param block     array of byte from which to retrieve a bit
     * @param indexRule rule explaining indexing in block
     * @return The bit value found at {@code index} in {@code block} according to {@code indexRule}
     */
    public static boolean getBit(int index, byte[] block, IndexingRule indexRule) {
        boolean returnBit;
        checkOutOfBounds(index, block.length, indexRule);
        switch (indexRule) {
            case FROM_LEAST_TO_MOST_START_WITH_0 ->
                    returnBit = (block[block.length - index / 8 - 1] & 0xff & (1 << (index % 8))) != 0;
            case FROM_LEAST_TO_MOST_START_WITH_1 ->
                    returnBit = (block[block.length - (index - 1) / 8 - 1] & 0xff & (1 << ((index - 1) % 8 + 1 - 1))) != 0;
            case FROM_MOST_TO_LEAST_START_WITH_0 -> returnBit = (block[index / 8] & 0xff & (1 << (7 - index % 8))) != 0;
            default -> returnBit = (block[(index - 1) / 8] & 0xff & (1 << (7 - (index - 1) % 8))) != 0;
        }
        return returnBit;
    }

    /**
     * Sets a {@code bit} value to {@code index} position in {@code block}
     *
     * @param index     index by which to set a bit
     * @param bit       value to set
     * @param block     array of byte to set a bit
     * @param indexRule rule explaining indexing in block
     */
    public static void setBit(int index, boolean bit, byte[] block, IndexingRule indexRule) {
        // Setting a bit is possible with: byte | (1 << shift)
        switch (indexRule) {
            case FROM_LEAST_TO_MOST_START_WITH_0 -> {
                if (bit) {
                    block[block.length - index / 8 - 1] |= (byte) (1 << (7 - index % 8));
                } else {
                    block[block.length - index / 8 - 1] &= (byte) (~(1 << (7 - index % 8)));
                }
            }
            case FROM_LEAST_TO_MOST_START_WITH_1 -> {
                if (bit) {
                    block[block.length - ((index - 1) / 8) - 1] |= (byte) (1 << (7 - (index - 1) % 8));
                } else {
                    block[block.length - ((index - 1) / 8) - 1] &= (byte) (~(1 << (7 - (index - 1) % 8)));
                }
            }
            case FROM_MOST_TO_LEAST_START_WITH_0 -> {
                if (bit) {
                    block[index / 8] |= (byte) (1 << (7 - index % 8));
                } else {
                    block[index / 8] &= (byte) (~(1 << (7 - index % 8)));
                }
            }
            case FROM_MOST_TO_LEAST_START_WITH_1 -> {
                if (bit) {
                    block[(index - 1) / 8] |= (byte) (1 << (7 - (index - 1) % 8));
                } else {
                    block[(index - 1) / 8] &= (byte) (~(1 << (7 - (index - 1) % 8)));
                }
            }
        }
    }

    public static byte[] xor(byte[] first, byte[] second) {
        // todo: ask Ilusha, f.len == s.len?
        int minLen = Math.min(first.length, second.length);
        byte[] result = new byte[minLen];

        for (int i = 0; i < minLen; i++) {
            result[i] = (byte) (first[i] ^ second[i]);
        }

        return result;
    }

    private static byte setBit(int index, boolean bit, byte b) {
        if (bit) {
            b |= (byte) (1 << (7 - index % 8));
        } else {
            b &= (byte) (~(1 << (7 - index % 8)));
        }
        return b;
    }

    public static byte shiftLeft(byte b, int n) {
        byte res = (byte) (b >> n);
        for (int i = 0; i < n; i++) {
            res = setBit(i, false, res);
        }
        return res;
    }

    public static byte[] cyclicShiftLeft(byte[] input, int n) {
        //Performs bitwise circular shift of 'arr' by 'nShift' bits to the left
        //RETURN:
        //      = Result
        byte[] res = new byte[input.length];

        if (input.length > 0) {
            int nByteShift = n / 8;
            int nBitShift = n % 8;

            if (nByteShift >= input.length)
                nByteShift %= input.length;

            int s = input.length - 1;
            int d = s - nByteShift;

            for (int nCnt = 0; nCnt < input.length; nCnt++, d--, s--) {
                while (d < 0)
                    d += input.length;
                while (s < 0)
                    s += input.length;

                byte byteS = input[s];

                res[d] |= (byte) (byteS << nBitShift);
                res[d > 0 ? d - 1 : res.length - 1] |= shiftLeft(byteS, 8 - nBitShift);
            }
        }

        return res;
    }

    public static byte[] cyclicShiftRight(byte[] input, int n) {
        //Performs bitwise circular shift of 'arr' by 'nShift' bits to the left
        //RETURN:
        //      = Result
        byte[] res = new byte[input.length];

        if (input.length > 0) {
            int nByteShift = n / 8;   //Adjusted after @dasblinkenlight's correction
            int nBitShift = n % 8;

            if (nByteShift >= input.length)
                nByteShift %= input.length;

            int s = input.length - 1;
            int d = s - nByteShift;

            for (int nCnt = 0; nCnt < input.length; nCnt++, d--, s--) {
                while (d < 0)
                    d += input.length;
                while (s < 0)
                    s += input.length;

                byte byteS = input[s];

                res[d] |= shiftLeft(byteS, nBitShift);
                res[d > 0 ? d - 1 : res.length - 1] |= (byte) (byteS << (8 - nBitShift));
            }
        }
        return res;
    }

    public static byte[] cyclicShiftLeftBytes(byte[] input, int n) {
        byte[] res = input.clone();
        int lenInput = input.length;

        for (int i = 0; i < n; i++) {
            byte tmp = res[0];
            for (int j = 0; j < lenInput - 1; j++) {
                res[j] = res[j + 1];
            }
            res[lenInput - 1] = tmp;
        }

        return res;
    }

    public static byte[] cyclicShiftRightBytes(byte[] input, int n) {
        byte[] res = input.clone();
        int lenInput = input.length;

        for (int i = 0; i < n; i++) {
            byte tmp = res[lenInput - 1];
            for (int j = lenInput - 1; j > 0; j--) {
                res[j] = res[j - 1];
            }
            res[0] = tmp;
        }

        return res;
    }

    public static byte[] generateBytes(int size) {
        byte[] generatedArray = new byte[size];
        random.nextBytes(generatedArray);
        return generatedArray;
    }

    public static byte[] mergeByteArrays (byte[] leftArray, byte[] rightArray) {
        int leftSize = leftArray.length;
        int rightSize = rightArray.length;

        byte[] mergedArray = new byte[leftSize + rightSize];

        System.arraycopy(leftArray, 0, mergedArray, 0, leftSize);
        System.arraycopy(rightArray, 0, mergedArray, leftSize, rightSize);

        return mergedArray;
    }

    public static int getOldestSetBit(byte number) {
        for (int i = 7; i >= 0; i--) {
            if (((number >> i) & 1) == 1) {
                return i;
            }
        }

        return -1;
    }
    public static int getOldestSetBit(short number) {
        for (int i = 15; i >= 0; i--) {
            if (((number >> i) & 1) == 1) {
                return i;
            }
        }

        return -1;
    }

    public static Optional<Integer> byteArrEqual(byte[] f, byte[] s) {
        if (f.length != s.length) {
            return Optional.of(-1);
        }

        for (int i = 0; i < f.length; i++) {
            if (f[i] != s[i]) {
                return Optional.of(i);
            }
        }

        return Optional.empty();
    }

    public static void printByteArray(byte[] array) {
        StringJoiner builderStr = new StringJoiner("     ");
        StringBuilder builder = new StringBuilder();

        for (byte b : array) {
            builderStr.add("1 2 3 4 5 6 7 8");
            for (int j = 0; j < 8; j++) {
                builder.append(((b & 0xff & (1 << (7 - j))) != 0) ? 1 + " " : 0 + " ");
            }
            builder.append("\t");
        }
        System.out.println(builderStr);
        System.out.println(builder);
    }



    public static boolean filesAreEqual(Path f, Path s) throws IOException {
        final long size = Files.size(f);
        if (size != Files.size(s))
            return false;

//        if (size < 4096)
//            return Arrays.equals(Files.readAllBytes(f), Files.readAllBytes(s));

        try (FileInputStream fInput = new FileInputStream(f.toFile())) {
            try (FileInputStream sInput = new FileInputStream(s.toFile())) {
                int fData, i = 0;
                while ((fData = fInput.read()) != -1) {
                    if (fData != sInput.read()) {
                        System.out.println("Fucked up at idx: " + i);
                        return false;
                    }
                    ++i;
                }
            }
        }
        return true;
    }

    private Operations() {
    }
}
