package ru.mai.compression.deflate.comp;

import java.io.IOException;


public interface BitOutputStream {

    void writeBits(int value, int numBits) throws IOException;


    int getBitPosition();

}
