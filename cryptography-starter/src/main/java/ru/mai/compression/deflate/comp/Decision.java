package ru.mai.compression.deflate.comp;

import java.io.IOException;


public interface Decision {


    void compressTo(BitOutputStream out, boolean isFinal) throws IOException;

}
