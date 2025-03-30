package ru.mai.compression;

import org.springframework.stereotype.Component;
import ru.mai.compression.deflate.DeflaterOutputStream;
import ru.mai.compression.deflate.InflaterInputStream;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Component(CompressingService.NAME)
public class CompressingServiceImpl implements CompressingService {

    private static final int BUF_SIZE = 1 << 20;

    @Override
    public void compress(InputStream is, FileOutputStream compressedFos) throws IOException {
        var byteBuf = new byte[BUF_SIZE];
        int bytesRead;

        var deflaterOs = new DeflaterOutputStream(compressedFos);
        while ((bytesRead = is.read(byteBuf)) != -1) {
            deflaterOs.write(byteBuf, 0, bytesRead);
        }
    }

    @Override
    public void decompress(FileInputStream decryptedFis, FileOutputStream decompressedFos) throws IOException {
        var byteBuf = new byte[BUF_SIZE];

        var inflaterIs = new InflaterInputStream(decryptedFis);
        while (inflaterIs.read(byteBuf) != -1) {
            decompressedFos.write(byteBuf);
        }
    }

}
