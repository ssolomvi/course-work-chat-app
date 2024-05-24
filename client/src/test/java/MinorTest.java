import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

class MinorTest {
//    String filename = "src/test/resources/catSmall.txt";
    String filename = "src/test/resources/catBig.txt";
    int filePageSize = 4096;

    @Test
    void inputStreamPassedInMethodTest() throws IOException {
        byte[] arr = new byte[filePageSize];
        try (InputStream in = new FileInputStream(filename)) {
            while ((arr = read(in, arr)).length == filePageSize) {
                System.out.println(new String(arr));
            }
            System.out.println(new String(arr));
        }
    }

    byte[] read(InputStream inputStream, byte[] arr) {
        int readBytes;

        try {
            readBytes = inputStream.read(arr);

            if (readBytes != filePageSize) {
                byte[] tmpArr = new byte[readBytes];
                System.arraycopy(arr, 0, tmpArr, 0, readBytes);
                arr = tmpArr;
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return arr;
    }
}
