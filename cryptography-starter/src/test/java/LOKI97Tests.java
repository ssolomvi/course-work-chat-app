import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.mai.encryption_algorithm.impl.LOKI97;
import ru.mai.utils.Operations;

import java.nio.charset.StandardCharsets;

import static ru.mai.utils.Operations.byteArrEqual;
import static ru.mai.utils.Operations.printByteArray;

public class LOKI97Tests {

    @Test
    public void additionBytesArraysTest() {

        byte[] arr1 = {-1, -1};
        Operations.printByteArray(arr1);

        byte[] arr2 = {0, 2};
        Operations.printByteArray(arr2);

        byte[] arr3 = Operations.additionByteArrays(arr1, arr2);

        Operations.printByteArray(arr3);
    }

    @Test
    public void arrayToIntTest() {
        byte[] input = {56, 30, 2, 8, 23, -2, 45, 4, 98, 25, 0, -1};
        int idx0 = ((((input[0] & 0xff) << 8) | (input[1] & 0xf8)) >>> 3);
        int idx1 = ((input[1] & 0x07) << 8) | (input[2] & 0xff);
        int idx2 = ((((input[3] & 0xff) << 8) | (input[4] & 0xf8)) >>> 3);
        int idx3 = ((input[4] & 0x07) << 8) | (input[5] & 0xff);
        int idx4 = ((((input[6] & 0xff) << 8) | (input[7] & 0xe0)) >>> 5);
        int idx5 = ((input[7] & 0x1f) << 8) | (input[8] & 0xff);
        int idx6 = ((((input[9] & 0xff) << 8) | (input[10] & 0xe0)) >>> 5);
        int idx7 = ((input[10] & 0x1f) << 8) | (input[11] & 0xff);

        int[] output = {idx0, idx1, idx2, idx3, idx4, idx5, idx6, idx7};
        Operations.printByteArray(input);

        for (int i = 0; i < output.length; i++) {
            System.out.println(output[i]);
            System.out.println(Integer.toBinaryString(output[i]));
        }
    }

    @Test
    public void LOKI97Test() {
//        byte[] testArray1 = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
//        byte[] testArray = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f};
//        byte[] testArray = Operations.generateBytes(16);
        // The cat (Felis catus), commonly
        byte[] testArray1 = "The cat (Felis c".getBytes(StandardCharsets.UTF_8);
        byte[] testArray2 = "atus), commonly ".getBytes(StandardCharsets.UTF_8);

        byte[] key = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f,
                0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f};

        printByteArray(testArray1);
        LOKI97 loki97 = new LOKI97(key);


        byte[] encrypted = loki97.encrypt(testArray1);
        byte[] decrypted = loki97.decrypt(encrypted);

        printByteArray(encrypted);
        printByteArray(decrypted);

        Assertions.assertTrue(byteArrEqual(testArray1, decrypted).isEmpty());

        encrypted = loki97.encrypt(testArray2);
        decrypted = loki97.decrypt(encrypted);

        printByteArray(encrypted);
        printByteArray(decrypted);

        Assertions.assertTrue(byteArrEqual(testArray2, decrypted).isEmpty());
    }
}
