import org.junit.jupiter.api.Test;
import ru.mai.encryption_algorithm.impl.RC6;
import ru.mai.utils.Operations;

import static ru.mai.utils.Operations.cyclicShiftLeft;
import static ru.mai.utils.Operations.printByteArray;

public class RC6Test {
    @Test
    public void someTest() {
        byte[] testArr = {-1, 2, 43, 124};
        int testInt = 567878345;

        System.out.println("testArr:");
        Operations.printByteArray(testArr);

        System.out.println();
        System.out.println("testInt: " + testInt);

        int arrInt = Operations.bytesArrToInt(testArr);
        System.out.println("arrInt: " + arrInt);
        System.out.println("Sum:");
        Operations.printByteArray(Operations.additionByteArraysLength4(testArr, Operations.intToBytes(testInt)));

        System.out.println((int) (Math.log(RC6.w) / Math.log(2)));
    }

    private static int rotr(int val, int pas) {
        return (val >>> pas) | (val << (32 - pas));
    }

    private static int rotl(int val, int pas) {
        return (val << pas) | (val >>> (32 - pas));
    }

    @Test
    public void tests() {
//        byte[] arr = Operations.intToBytes(-1089828067);
//        int num = Operations.bytesArrToInt(arr);
//        System.out.println(num);
//        printByteArray(intToBytes(num));
//        System.out.println(rotl(num, -1));
//        printByteArray(intToBytes(rotl(num, -1)));
//
//        printByteArray(arr);
//
//        Operations.printByteArray(cyclicShiftLeft(arr, -1));
//
//        Operations.printByteArray(arr);
//        Operations.printByteArray(cyclicShiftRight(arr, 1));

        byte[] arr = Operations.intToBytes(1024 * 1024 * 1024 * 2);
        printByteArray(arr);
        printByteArray(cyclicShiftLeft(arr, 1));
    }

    @Test
    public void encDecTest() {
//        byte[] arr = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
//        byte[] key = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
//        byte[] arr = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
//        byte[] key = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        byte[] arr = {0x02, 0x13, 0x24, 0x35, 0x46, 0x57, 0x68, 0x79,
                (byte) 0x8a, (byte) 0x9b, (byte) 0xac, (byte) 0xbd, (byte) 0xce, (byte) 0xdf, (byte) 0xe0, (byte) 0xf1};
        byte[] key = {(byte) 0x01, (byte) 0x23, (byte) 0x45, (byte) 0x67, (byte) 0x89, (byte) 0xab, (byte) 0xcd, (byte) 0xef,
                (byte) 0x01, (byte) 0x12, (byte) 0x23, (byte) 0x34, (byte) 0x45, (byte) 0x56, (byte) 0x67, (byte) 0x78};

        System.out.println("Arr:");
        Operations.printByteArray(arr);

        RC6 rc6 = new RC6(key);

        byte[] encrypted = rc6.encrypt(arr);
        byte[] decrypted = rc6.decrypt(encrypted);

        System.out.println("Encrypted:");
        Operations.printByteArray(encrypted);
        System.out.println("Decrypted:");
        Operations.printByteArray(decrypted);

//        Assertions.assertTrue(byteArrEqual(arr, decrypted).isEmpty());
    }
}
