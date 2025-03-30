import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.mai.encryption_algorithm.impl.LOKI97;
import ru.mai.utils.Operations;

import java.nio.charset.StandardCharsets;

import static ru.mai.utils.Operations.byteArrEqual;
import static ru.mai.utils.Operations.printByteArray;

public class LOKI97Tests {

    @Test
    public void creativeTest() {
//        byte[] arr = {-82, 92, -119, -53, -73, 113, -68, 87};
//        byte[] arr = {123, 100};
        byte[] arr = {19, -113, -62, -20, 50, -62, -101, -105};
//        int[] indexingTable = {0, 1, 2, 3, 4, 5, 6, 15};
        Operations.printByteArray(arr);

//        byte[] permArr = Permutation.permute(arr, , Operations.IndexingRule.FROM_MOST_TO_LEAST_START_WITH_0);
//        byte[] permArr = Permutation.permute(arr, indexingTable, Operations.IndexingRule.FROM_LEAST_TO_MOST_START_WITH_0);
//        printByteArray(permArr);
    }

//    @Test
    public void additionBytesArraysTest() {

        byte[] arr1 = {-1, -1};
        Operations.printByteArray(arr1);

        byte[] arr2 = {0, 2};
        Operations.printByteArray(arr2);

        byte[] arr3 = Operations.additionByteArraysLength8(arr1, arr2);

        Operations.printByteArray(arr3);
    }

    @Test
    public void LOKI97Test() {
//        byte[] testArray1 = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
//        byte[] testArray1 = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f};
//        byte[] testArray = Operations.generateBytes(16);

        byte[] testArray1 = "wo to five kitte".getBytes(StandardCharsets.UTF_8);
        byte[] testArray2 = "atus), commonly ".getBytes(StandardCharsets.UTF_8);

        byte[] key = {(byte) 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f,
                0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f};

//        byte[] key = "veryVeryVeryVeryVerSecureKeyYeah".getBytes(StandardCharsets.UTF_8);
//        byte[] key = "verySecureKeyYea".getBytes(StandardCharsets.UTF_8);


        System.out.println("Arr1:");
        printByteArray(testArray1);
        LOKI97 loki97 = new LOKI97(key);

        byte[] encrypted = loki97.encrypt(testArray1);
        byte[] decrypted = loki97.decrypt(encrypted);

        System.out.println("Encrypted:");
        printByteArray(encrypted);
        System.out.println("Decrypted:");
        printByteArray(decrypted);

        Assertions.assertTrue(byteArrEqual(testArray1, decrypted).isEmpty());

        System.out.println("Arr2:");
        printByteArray(testArray2);
        encrypted = loki97.encrypt(testArray2);
        decrypted = loki97.decrypt(encrypted);

        System.out.println("Encrypted:");
        printByteArray(encrypted);
        System.out.println("Decrypted:");
        printByteArray(decrypted);

        Assertions.assertTrue(byteArrEqual(testArray2, decrypted).isEmpty());
    }
}
