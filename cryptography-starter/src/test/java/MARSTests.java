import org.junit.jupiter.api.Test;
import ru.mai.encryption_algorithm.impl.MARS;
import ru.mai.utils.Operations;

import java.nio.charset.StandardCharsets;

public class MARSTests {
    @Test
    public void testMod() {
        System.out.println(-7 % 15);
        System.out.println(Math.floorMod(-7, 15));
        int r = -7 % 15;
        if (r < 0) {
            r += 15;
        }

        System.out.println(r);

    }

    @Test
    public void testMARS() {
//        byte[] testArray = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
        byte[] testArray = "The cat (Felis c".getBytes(StandardCharsets.UTF_8);
        byte[] key16 = "verySecureKeyYea".getBytes(StandardCharsets.UTF_8);
        byte[] key = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};

        MARS mars = new MARS(key16);

        Operations.printByteArray(testArray);

        byte[] encrypted = mars.encrypt(testArray);
        Operations.printByteArray(encrypted);

        byte[] decrypted = mars.decrypt(encrypted);
        Operations.printByteArray(decrypted);
    }
}
