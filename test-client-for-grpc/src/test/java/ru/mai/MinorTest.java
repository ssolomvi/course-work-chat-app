package ru.mai;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MinorTest {
    @Test
    void djalyab() {
        int aboba = 0;
        System.out.println(++aboba);
        aboba = 0;
        System.out.println(aboba++);
    }

    @Test
    void checkModelIsInitialized() {
        DiffieHellmanNumber number = DiffieHellmanNumber.getDefaultInstance();

//        Assertions.assertTrue(number.equals(DiffieHellmanNumber.getDefaultInstance()));
        Assertions.assertTrue(number.isInitialized());
    }
}
