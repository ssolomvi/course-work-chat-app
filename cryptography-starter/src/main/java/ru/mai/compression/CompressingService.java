package ru.mai.compression;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public interface CompressingService {

    String NAME = "mai_CompressingService";

    void compress(InputStream in, FileOutputStream compressedFos) throws IOException;

    void decompress(FileInputStream decryptedFis, FileOutputStream decompressedFos) throws IOException;

}
