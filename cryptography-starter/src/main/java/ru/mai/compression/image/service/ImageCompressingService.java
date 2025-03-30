package ru.mai.compression.image.service;

import java.io.IOException;
import java.io.InputStream;

public interface ImageCompressingService {

    String NAME = "mai_ImageCompressingService";

    int STRONG_COMPRESSION = 14;
    int MILD_COMPRESSION = 24;
    int WEAK_COMPRESSION = 34;

    /**
     * Compresses image from input stream
     * @param is input stream of bytes of image to compress
     * @return input stream of bytes of compressed image
     */
    InputStream compress(String filename, InputStream is, int coefficient) throws IOException;

}
