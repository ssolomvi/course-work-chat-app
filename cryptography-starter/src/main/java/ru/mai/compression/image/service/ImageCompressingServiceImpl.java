package ru.mai.compression.image.service;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import ru.mai.compression.image.compressor.ImageCompressorImpl;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

@Service(ImageCompressingService.NAME)
public class ImageCompressingServiceImpl implements ImageCompressingService {

    @Override
    public InputStream compress(String filename, InputStream is, int coefficient) throws IOException {
        var originalImage = acquireBufferedImage(is);
        var compressedImage = compress(originalImage, coefficient);

        return toInputStream(extension(filename), compressedImage);
    }

    @Nullable
    private BufferedImage acquireBufferedImage(InputStream is) throws IOException {
        return ImageIO.read(is);
    }

    private BufferedImage compress(BufferedImage imageToCompress, int coefficient) {
        var imageCompressor = new ImageCompressorImpl();

        return imageCompressor.compress(imageToCompress, coefficient);
    }

    private InputStream toInputStream(String ext, BufferedImage image) throws IOException {
        var os = new ByteArrayOutputStream();
        ImageIO.write(image, ext, os);

        return new ByteArrayInputStream(os.toByteArray());
    }

    private String extension(String filepath) {
        return filepath.substring(filepath.lastIndexOf(".") + 1);
    }

}
