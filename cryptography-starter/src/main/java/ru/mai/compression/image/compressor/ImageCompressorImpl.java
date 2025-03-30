package ru.mai.compression.image.compressor;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ImageCompressorImpl implements ImageCompressor {

    private static final Logger LOGGER = Logger.getLogger(ImageCompressorImpl.class.getName());
    private static final int BLOCK_SIZE = 8;
    private boolean isColor = false;
    private final double[][] scalingFactors = calculateScalingFactors();
    private final double[][] cosCache = new double[BLOCK_SIZE][BLOCK_SIZE];

    public ImageCompressorImpl() {
        // Precalculate cosine values for DCT and IDCT
        for (int u = 0; u < BLOCK_SIZE; u++) {
            for (int i = 0; i < BLOCK_SIZE; i++) {
                cosCache[u][i] = Math.cos((2 * i + 1) * u * Math.PI / (2.0 * BLOCK_SIZE));
            }
        }
    }

    @Override
    public BufferedImage compress(BufferedImage img, int coefficients) {
        isColor = img.getType() > 0 && img.getType() < 10;
        LOGGER.log(Level.INFO, "Image type: {0}", img.getType());

        if (isColor) {
            LOGGER.log(Level.INFO, "Image loaded as color.");
        } else {
            LOGGER.log(Level.INFO, "Image loaded as grayscale.");
        }

        return compressParallel(img, zScanMask(coefficients));
    }

    private static double[][] zScanMask(int C) {
        var mask = new double[BLOCK_SIZE][BLOCK_SIZE];
        var maskM = 0;
        var maskN = 0;
        for (var i = 0; i < C; i++) {
            if (i != 0) {
                if ((maskM + maskN) % 2 == 0) {
                    maskM--;
                    maskN++;
                    if (maskM < 0) {
                        maskM++;
                    }
                    if (maskN >= ImageCompressorImpl.BLOCK_SIZE) {
                        maskN--;
                    }

                } else {
                    maskM++;
                    maskN--;
                    if (maskM >= ImageCompressorImpl.BLOCK_SIZE) {
                        maskM--;
                    }
                    if (maskN < 0) {
                        maskN++;
                    }
                }

            }
            mask[maskM][maskN] = 1;
        }

        return mask;
    }


    private BufferedImage compressParallel(BufferedImage img, double[][] mask) {
        int originalWidth = img.getWidth();
        int originalHeight = img.getHeight();

        int compressedWidth = (originalWidth / BLOCK_SIZE) * BLOCK_SIZE;
        int compressedHeight = (originalHeight / BLOCK_SIZE) * BLOCK_SIZE;
        int type = isColor ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_BYTE_GRAY;
        BufferedImage compressedImg = new BufferedImage(compressedWidth, compressedHeight, type);

        var nThreads = Runtime.getRuntime().availableProcessors() - 1;
        var executorService = Executors.newFixedThreadPool(nThreads);

        for (var height = 0; height < compressedHeight; height += BLOCK_SIZE) {
            for (var width = 0; width < compressedWidth; width += BLOCK_SIZE) {
                var finalHeight = height;
                var finalWidth = width;
                executorService.submit(() -> {
                    if (isColor) {
                        compressColorBlock(img, compressedImg, mask, finalHeight, finalWidth);
                    } else {
                        compressGrayBlock(img, compressedImg, mask, finalHeight, finalWidth);
                    }
                });
            }
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(3L * nThreads, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Interrupted while waiting for thread pool to terminate", e);
            Thread.currentThread().interrupt();
        }

        return compressedImg;
    }


    private void compressColorBlock(BufferedImage img, BufferedImage compressedImg, double[][] mask,
                                    int m, int n) {
        double[][] blockR = new double[BLOCK_SIZE][BLOCK_SIZE];
        double[][] blockG = new double[BLOCK_SIZE][BLOCK_SIZE];
        double[][] blockB = new double[BLOCK_SIZE][BLOCK_SIZE];

        for (int y = 0; y < BLOCK_SIZE; y++) {
            for (int x = 0; x < BLOCK_SIZE; x++) {
                Color color = new Color(img.getRGB(n + x, m + y));
                blockR[y][x] = color.getRed();
                blockG[y][x] = color.getGreen();
                blockB[y][x] = color.getBlue();
            }
        }

        double[][] coeffR = dctWithScalingFactorsPrecalculated(blockR);
        double[][] coeffG = dctWithScalingFactorsPrecalculated(blockG);
        double[][] coeffB = dctWithScalingFactorsPrecalculated(blockB);

        // Apply the mask and do the inverse DCT
        double[][] iDCT_R = iDCTWithScalingFactorsPrecalculated(applyMask(coeffR, mask));
        double[][] iDCT_G = iDCTWithScalingFactorsPrecalculated(applyMask(coeffG, mask));
        double[][] iDCT_B = iDCTWithScalingFactorsPrecalculated(applyMask(coeffB, mask));

        int red, green, blue;
        for (int y = 0; y < BLOCK_SIZE; y++) {
            for (int x = 0; x < BLOCK_SIZE; x++) {
                red = clamp(iDCT_R[y][x]);
                green = clamp(iDCT_G[y][x]);
                blue = clamp(iDCT_B[y][x]);

                compressedImg.setRGB(n + x, m + y, new Color(red, green, blue).getRGB());
            }
        }
    }


    private void compressGrayBlock(BufferedImage img, BufferedImage compressedImg, double[][] mask, int m, int n) {
        double[][] block = new double[BLOCK_SIZE][BLOCK_SIZE];
        for (int y = 0; y < BLOCK_SIZE; y++) {
            for (int x = 0; x < BLOCK_SIZE; x++) {
                block[y][x] = new Color(img.getRGB(n + x, m + y)).getRed();
            }
        }
        double[][] coefficients = dctWithScalingFactorsPrecalculated(block);

        // Apply the mask and do the inverse DCT
        double[][] iDCT = iDCTWithScalingFactorsPrecalculated(applyMask(coefficients, mask));
        int grayValue;
        for (int y = 0; y < BLOCK_SIZE; y++) {
            for (int x = 0; x < BLOCK_SIZE; x++) {
                grayValue = clamp(iDCT[y][x]);
                compressedImg.setRGB(n + x, m + y, new Color(grayValue, grayValue, grayValue).getRGB());
            }
        }
    }


    private double[][] applyMask(double[][] coeff, double[][] mask) {
        int rows = coeff.length;
        int cols = coeff[0].length;
        double[][] maskedCoeff = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                maskedCoeff[i][j] = coeff[i][j] * mask[i][j];
            }
        }

        return maskedCoeff;
    }

    private static double[][] calculateScalingFactors() {
        double[][] scalingFactors = new double[BLOCK_SIZE][BLOCK_SIZE];
        double firstFactor = Math.sqrt(1.0 / BLOCK_SIZE);
        double secondFactor = Math.sqrt(1.4 / BLOCK_SIZE);
        for (int i = 0; i < BLOCK_SIZE; i++) {
            for (int j = 0; j < BLOCK_SIZE; j++) {
                scalingFactors[i][j] = (i == 0) ? firstFactor : secondFactor;
            }
        }
        return scalingFactors;
    }


    private double[][] dctWithScalingFactorsPrecalculated(double[][] block) {
        int N = block.length;
        double[][] result = new double[N][N];
        double sum;

        for (int u = 0; u < N; u++) {
            for (int v = 0; v < N; v++) {
                sum = 0.0;
                for (int i = 0; i < N; i++) {
                    for (int j = 0; j < N; j++) {
                        sum += block[i][j]
                                * cosCache[u][i]
                                * cosCache[v][j];
                    }
                }
                result[u][v] = scalingFactors[u][v] * scalingFactors[u][v] * sum;
            }
        }
        return result;
    }


    private double[][] iDCTWithScalingFactorsPrecalculated(double[][] coeff) {
        int N = coeff.length;
        double[][] result = new double[N][N];
        double sum;

        for (int x = 0; x < N; x++) {
            for (int y = 0; y < N; y++) {
                sum = 0.0;
                for (int u = 0; u < N; u++) {
                    for (int v = 0; v < N; v++) {
                        sum += scalingFactors[u][v]
                                * scalingFactors[u][v]
                                * coeff[u][v]
                                * cosCache[u][x]
                                * cosCache[v][y];
                    }
                }
                result[x][y] = sum;
            }
        }
        return result;
    }

    private static int clamp(double value) {
        return (int) Math.max(0, Math.min(255, value));
    }

}
