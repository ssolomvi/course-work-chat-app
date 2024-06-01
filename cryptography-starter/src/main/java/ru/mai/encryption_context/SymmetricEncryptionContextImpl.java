package ru.mai.encryption_context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mai.encryption_algorithm.EncryptionAlgorithm;
import ru.mai.encryption_mode.EncryptionMode;
import ru.mai.encryption_mode.EncryptionModeEnum;
import ru.mai.encryption_mode.abstr.EncryptionModeCounter;
import ru.mai.encryption_mode.abstr.EncryptionModePreviousNeeded;
import ru.mai.encryption_mode.abstr.EncryptionModeWithInitVector;
import ru.mai.encryption_mode.impl.*;
import ru.mai.encryption_padding_mode.PaddingMode;
import ru.mai.encryption_padding_mode.PaddingModeEnum;
import ru.mai.encryption_padding_mode.impl.ANSI_X_923;
import ru.mai.encryption_padding_mode.impl.ISO10126;
import ru.mai.encryption_padding_mode.impl.PKCS7;
import ru.mai.encryption_padding_mode.impl.Zeroes;
import ru.mai.exceptions.IllegalArgumentExceptionWithLog;
import ru.mai.exceptions.IllegalMethodCallException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * A class representing the execution context of a symmetric cryptographic
 * algorithm that provides object functionality for performing operations
 * encryption and decryption with a given key of a symmetric algorithm with
 * support for one of the encryption modes (set by transfer): ECB, CBC, PCBC, CFB,
 * OFB, CTR, Random Delta; and also with support
 * one of the packing modes (specified by enumeration): Zeros, ANSI X.923, PKCS7, ISO
 * 10126.
 * <p>
 * Class object constructor parameters: encryption key, encryption mode
 * (enumeration object), padding mode (enumeration object), initialization vector for
 * preset mode (optional), additional parameters for the specified mode
 * (collection of variable length arguments).
 * <p>
 * Parameters of overloaded methods
 * encryption/decryption: data to [de]encrypt (byte array of arbitrary
 * length) and a link to the resulting byte array, or a path to a file with input
 * data and path to the file with the result of [de]encryption).
 * <p>
 * Where possible, implement
 * parallelization of calculations. Performing encryption/decryption operations
 * must be done asynchronously.
 */
public class SymmetricEncryptionContextImpl implements EncryptionContext {
    private static final Logger log = LoggerFactory.getLogger(SymmetricEncryptionContextImpl.class);
    private final EncryptionMode encryptionMode;
    private final PaddingMode paddingMode;
    private final int minBlockLengthForThreadForEncryption; // count of blocks * size of block to be encrypted by a single thread
    private final int minBlockLengthForThreadForDecryption; // count of blocks * size of block to be decrypted by a single thread
    private final int algorithmBlockLengthForEncryption; // block length to pass in encrypt method
    private final int algorithmBlockLengthForDecryption; // block length to pass in decrypt method
    private static final int FILE_PAGE_SIZE = 65536;
    private static final int ONE_THREAD_BLOCK_SIZE = 8192; // size of byte array to pass for one thread to work on
    private final int filePageSizeModified;

    // region Constructing EncryptionContextAbstract
    public SymmetricEncryptionContextImpl(EncryptionModeEnum encryptionModeEnum, PaddingModeEnum paddingModeEnum, EncryptionAlgorithm algorithm) {
        if (paddingModeEnum == null) {
            throw new IllegalArgumentExceptionWithLog("Passed param paddingModeEnum is null", log);
        }
        if (encryptionModeEnum == null) {
            throw new IllegalArgumentExceptionWithLog("Passed param encryptionModeEnum is null", log);
        }
        if (algorithm == null) {
            throw new IllegalArgumentExceptionWithLog("Passed param algorithm is null", log);
        }
        if (encryptionModeEnum.needsInitVector()) {
            throw new IllegalArgumentExceptionWithLog(String.format("Encryption mode %s needs initial vector", encryptionModeEnum.getTitle()), log);
        }

        this.algorithmBlockLengthForEncryption = algorithm.getAlgorithmBlockForEncryption();
        this.algorithmBlockLengthForDecryption = algorithm.getAlgorithmBlockForDecryption();

        this.minBlockLengthForThreadForEncryption = ONE_THREAD_BLOCK_SIZE / algorithmBlockLengthForEncryption * algorithmBlockLengthForEncryption;
        this.minBlockLengthForThreadForDecryption = ONE_THREAD_BLOCK_SIZE / algorithmBlockLengthForDecryption * algorithmBlockLengthForDecryption;

        this.encryptionMode = getEncryptionMode(encryptionModeEnum, algorithm, null);
        this.paddingMode = getPaddingMode(paddingModeEnum);

        this.filePageSizeModified = FILE_PAGE_SIZE / algorithmBlockLengthForEncryption * algorithmBlockLengthForEncryption;
    }

    public SymmetricEncryptionContextImpl(EncryptionModeEnum encryptionModeEnum, PaddingModeEnum paddingModeEnum, EncryptionAlgorithm algorithm, byte[] initializationVector) {
        if (paddingModeEnum == null) {
            throw new IllegalArgumentExceptionWithLog("Passed param paddingModeEnum is null", log);
        }
        if (encryptionModeEnum == null) {
            throw new IllegalArgumentExceptionWithLog("Passed param encryptionModeEnum is null", log);
        }
        if (algorithm == null) {
            throw new IllegalArgumentExceptionWithLog("Passed param algorithm is null", log);
        }
        if (encryptionModeEnum.needsInitVector() && initializationVector == null) {
            throw new IllegalArgumentExceptionWithLog(String.format("Encryption mode %s needs initial vector", encryptionModeEnum.getTitle()), log);
        }

        this.algorithmBlockLengthForEncryption = algorithm.getAlgorithmBlockForEncryption();
        this.algorithmBlockLengthForDecryption = algorithm.getAlgorithmBlockForDecryption();

        this.minBlockLengthForThreadForEncryption = ONE_THREAD_BLOCK_SIZE / algorithmBlockLengthForEncryption * algorithmBlockLengthForEncryption;
        this.minBlockLengthForThreadForDecryption = ONE_THREAD_BLOCK_SIZE / algorithmBlockLengthForDecryption * algorithmBlockLengthForDecryption;

        this.encryptionMode = getEncryptionMode(encryptionModeEnum, algorithm, initializationVector);
        this.paddingMode = getPaddingMode(paddingModeEnum);

        this.filePageSizeModified = FILE_PAGE_SIZE / algorithmBlockLengthForEncryption * algorithmBlockLengthForEncryption;
    }

    private EncryptionMode getEncryptionMode(EncryptionModeEnum encryptionModeEnum,
                                             EncryptionAlgorithm algorithm, byte[] initVector) {
        switch (encryptionModeEnum) {
            case CBC -> {
                return new CBC(algorithm, initVector);
            }
            case PCBC -> {
                return new PCBC(algorithm, initVector);
            }
            case CFB -> {
                return new CFB(algorithm, initVector);
            }
            case OFB -> {
                return new OFB(algorithm, initVector);
            }
            case CTR -> {
                return new CTR(algorithm, initVector);
            }
            case RANDOM_DELTA -> {
                return new RandomDelta(algorithm, initVector);
            }
            default -> {
                return new ECB(algorithm);
            }
        }
    }

    private PaddingMode getPaddingMode(PaddingModeEnum paddingModeEnum) {
        switch (paddingModeEnum) {
            case ZEROES -> {
                return new Zeroes(algorithmBlockLengthForEncryption);
            }
            case ANSI_X_923 -> {
                return new ANSI_X_923(algorithmBlockLengthForEncryption);
            }
            case PKCS7 -> {
                return new PKCS7(algorithmBlockLengthForEncryption);
            }
            default -> {
                return new ISO10126(algorithmBlockLengthForEncryption);
            }
        }
    }

    // endregion

    /**
     * Gets byte array equal to {@code minBlockSize} or leftover of {@code input} if {@code currOffset}
     * is not enough to get full {@code minBlockSize}
     *
     * @param input      array to get byte array from
     * @param currOffset offset where byte array to return ends in {@code input}
     * @return Byte array, which was a part of {@code input}
     */
    private byte[] getByteArray(byte[] input, int currOffset, int minBlockSize) {
        int lenToReturn;
        if (currOffset + minBlockSize > input.length) {
            lenToReturn = input.length - currOffset;
        } else {
            lenToReturn = minBlockSize;
        }
        byte[] toReturn = new byte[lenToReturn];

        System.arraycopy(input, currOffset, toReturn, 0, lenToReturn);

        return toReturn;
    }

    // region Encryption

    /**
     * Encrypts {@code toEncrypt} which is guaranteed to be multiple of {@code algorithm block}.
     *
     * @param toEncrypt byte array to be encrypted. Must be multiple of {@code algorithm block}
     * @return Encrypted byte array
     */
    private byte[] encryptThreadTask(byte[] toEncrypt, int blockNumber) {
        if (toEncrypt.length % algorithmBlockLengthForEncryption != 0) {
            throw new IllegalArgumentExceptionWithLog("encryptThreadTask: param toEncrypt must be multiple of getAlgorithmBlock()", log);
        }

        byte[] encrypted;
        if (encryptionMode instanceof EncryptionModeCounter encryptionModeCounter) {
            encrypted = encryptionModeCounter.encrypt(toEncrypt, blockNumber);
        } else {
            encrypted = encryptionMode.encrypt(toEncrypt);
        }

        return encrypted;
    }

    private void encryptionSubmitThreadTaskECB(byte[] input, ExecutorService executorService, List<Future<byte[]>> threadResults) {
        int blockOffset = 0;
        while (blockOffset < input.length) {
            int finalBlockOffset = blockOffset;

            threadResults.add(
                    executorService.submit(
                            () -> encryptThreadTask(getByteArray(input, finalBlockOffset, minBlockLengthForThreadForEncryption), -1)));
            blockOffset += minBlockLengthForThreadForEncryption;
        }
    }

    private void encryptionSubmitThreadCTRAndRD(byte[] input, ExecutorService executorService, List<Future<byte[]>> threadResults) {
        int blockOffset = 0;
        while (blockOffset < input.length) {
            int finalBlockOffset = blockOffset;

            threadResults.add(
                    executorService.submit(
                            () -> encryptThreadTask(getByteArray(input, finalBlockOffset, minBlockLengthForThreadForEncryption), finalBlockOffset)));

            blockOffset += minBlockLengthForThreadForEncryption;
        }
    }

    /**
     * Encrypts {@code input} in multithreading mode.
     *
     * @param input byte array to encrypt
     * @return Encrypted {@code input}
     */
    private byte[] encryptionMultithreading(byte[] input) {
        if (input.length % algorithmBlockLengthForEncryption != 0) {
            throw new IllegalArgumentExceptionWithLog("encryptionMultithreading: param toEncrypt must be multiple of getAlgorithmBlock()", log);
        }

        int countOfBlocks = input.length / algorithmBlockLengthForEncryption;

        List<Future<byte[]>> threadResults = new ArrayList<>();

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);

        if (encryptionMode instanceof ECB) {
            encryptionSubmitThreadTaskECB(input, executorService, threadResults);
        } else if (encryptionMode instanceof EncryptionModeCounter) {
            encryptionSubmitThreadCTRAndRD(input, executorService, threadResults);
        } else {
            throw new IllegalMethodCallException("Multithreading is not possible for encryption mode " + encryptionMode.getClass().getName());
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("executorService was interrupted while was waiting for threads results");
            // todo: throw Interrupted
        }

        // algorithmBlockLengthForDecryption is length of block to pass to method decrypt
        byte[] encrypted = new byte[countOfBlocks * algorithmBlockLengthForDecryption];

        int currPos = 0;
        for (Future<byte[]> future : threadResults) {
            try {
                byte[] futureRes = future.get();
                // todo: might be weak place for RSA
                System.arraycopy(futureRes, 0, encrypted, currPos, futureRes.length);
                currPos += futureRes.length;
            } catch (InterruptedException e) {
                log.error("encryptionMultithreading: Current thread was interrupted while waiting");
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                log.error("encryptionMultithreading: The computation threw an exception");
                throw new RuntimeException(e);
            }
        }

        return encrypted;
    }

    private byte[] padInputByteArray(byte[] input) {
        int countOfBlocks = input.length / algorithmBlockLengthForEncryption;
        int lengthOfBlockToPad = input.length % algorithmBlockLengthForEncryption;

        if (lengthOfBlockToPad == 0) {
            return input;
        }

        byte[] blockToPad = new byte[lengthOfBlockToPad];
        System.arraycopy(input, input.length - lengthOfBlockToPad, blockToPad, 0, lengthOfBlockToPad);
        byte[] paddedBlock = paddingMode.pad(blockToPad);

        int lenToEncryptWithPadding = (countOfBlocks + 1) * algorithmBlockLengthForEncryption;
        int lenToEncryptWithoutPadding = countOfBlocks * algorithmBlockLengthForEncryption;

        byte[] tmp = new byte[lenToEncryptWithPadding];
        System.arraycopy(input, 0, tmp, 0, lenToEncryptWithoutPadding);
        System.arraycopy(paddedBlock, 0, tmp, lenToEncryptWithoutPadding, paddedBlock.length);

        return tmp;
    }

    /**
     * Encrypts {@code input} accordingly to algorithm, padding mode and encryption mode used in object's constructor
     *
     * @param input byte array to encrypt
     * @return Encrypted byte array
     */
    @Override
    public byte[] encrypt(byte[] input) {
        if (encryptionMode instanceof EncryptionModeWithInitVector encryptionModeWithInitVector) {
            encryptionModeWithInitVector.invokeNextAsNew();
        }
        // 1. padding, 2. encryption

        byte[] toEncrypt = padInputByteArray(input);

        if (encryptionMode instanceof ECB || encryptionMode instanceof EncryptionModeCounter) {
            return encryptionMultithreading(toEncrypt);
        }

        return encryptThreadTask(toEncrypt, -1);
    }

    public void encrypt(InputStream inputStream, OutputStream outputStream) throws IOException {
        if (encryptionMode instanceof EncryptionModeWithInitVector encryptionModeWithInitVector) {
            encryptionModeWithInitVector.invokeNextAsNew();
        }

        byte[] toEncrypt = new byte[filePageSizeModified];
        int bytesRead;

        while ((bytesRead = inputStream.read(toEncrypt)) != -1) {
            if (bytesRead != filePageSizeModified) {
                // shrink file page array
                byte[] tmp = new byte[bytesRead];
                System.arraycopy(toEncrypt, 0, tmp, 0, bytesRead);
                toEncrypt = tmp;
            }

            toEncrypt = padInputByteArray(toEncrypt);

            byte[] encrypted;
            if (encryptionMode instanceof ECB || encryptionMode instanceof EncryptionModeCounter) {
                encrypted = encryptionMultithreading(toEncrypt);
            } else {
                encrypted = encryptThreadTask(toEncrypt, -1);
            }

            outputStream.write(encrypted);
        }
    }

    @Override
    public byte[] encryptPart(byte[] toEncrypt, boolean initAsNew) throws IOException {
        if (initAsNew && encryptionMode instanceof EncryptionModeWithInitVector encryptionModeWithInitVector) {
            encryptionModeWithInitVector.invokeNextAsNew();
        }

        toEncrypt = padInputByteArray(toEncrypt);

        byte[] encrypted;
        if (encryptionMode instanceof ECB || encryptionMode instanceof EncryptionModeCounter) {
            encrypted = encryptionMultithreading(toEncrypt);
        } else {
            encrypted = encryptThreadTask(toEncrypt, -1);
        }

        return encrypted;
    }

    /**
     * Encrypts {@code input} accordingly to algorithm, padding mode and encryption mode used in object's constructor
     *
     * @param input  path to file where data to encode is
     * @param output path to file where encoded data is to be stored
     */
    @Override
    public void encrypt(Path input, Path output) {
        if (encryptionMode instanceof EncryptionModeWithInitVector encryptionModeWithInitVector) {
            encryptionModeWithInitVector.invokeNextAsNew();
        }
        // 1. read file page from file input
        // 2. pad file page
        // 3. pass padded for encryption

        // modify file page size, so we always get file part which is multiple for getAlgorithmBlock()
        // and there is no need to check for padding needed each time

        try (FileInputStream inputStream = new FileInputStream(input.toFile())) {
            try (FileOutputStream outputStream = new FileOutputStream(output.toFile())) {
                encrypt(inputStream, outputStream);
            }
        } catch (FileNotFoundException e) {
            log.error("Carrramba! File cannot be opened for reading.");
            throw new RuntimeException(e);
        } catch (IOException e) {
            log.error("Carrramba! An I/O error occurred");
            throw new RuntimeException(e);
        }
    }


    // endregion

    // region Decryption
    private byte[] decryptThreadTask(byte[] input, int blockNumber, byte[] prev) {
        if (input.length % algorithmBlockLengthForDecryption != 0) {
            throw new IllegalArgumentExceptionWithLog("decryptThreadTask: param toEncrypt must be multiple of getAlgorithmBlock()", log);
        }

        byte[] decrypted;
        if (encryptionMode instanceof EncryptionModeCounter encryptionModeCounter) {
            decrypted = encryptionModeCounter.decrypt(input, blockNumber);
        } else if (encryptionMode instanceof EncryptionModePreviousNeeded encryptionModePreviousNeeded) {
            decrypted = encryptionModePreviousNeeded.decrypt(input, prev);
        } else {
            decrypted = encryptionMode.decrypt(input);
        }

        return decrypted;
    }

    private void decryptionSubmitThreadTaskECB(byte[] input, ExecutorService executorService, List<Future<byte[]>> threadResults) {
        int blockOffset = 0;
        while (blockOffset < input.length) {
            int finalBlockOffset = blockOffset;

            threadResults.add(
                    executorService.submit(
                            () -> decryptThreadTask(getByteArray(input, finalBlockOffset, minBlockLengthForThreadForDecryption), -1, null)));
            blockOffset += minBlockLengthForThreadForDecryption;
        }
    }

    private void decryptionSubmitThreadCBCAndCFB(byte[] input, byte[] prevBlock,
                                                 ExecutorService executorService, List<Future<byte[]>> threadResults) {
        int blockOffset = 0;
        byte[] currDecrypted;
        byte[] prevC = null;
        if (prevBlock != null) {
            prevC = new byte[algorithmBlockLengthForDecryption];
            System.arraycopy(prevBlock, 0, prevC, 0, algorithmBlockLengthForDecryption);
        }

        while (blockOffset < input.length) {
            int finalBlockOffset = blockOffset;

            currDecrypted = getByteArray(input, finalBlockOffset, minBlockLengthForThreadForDecryption);

            byte[] finalCurrDecrypted = currDecrypted;
            byte[] finalPrevC = prevC;
            threadResults.add(
                    executorService.submit(
                            () -> decryptThreadTask(finalCurrDecrypted, -1, finalPrevC)));

            prevC = new byte[algorithmBlockLengthForDecryption];
            System.arraycopy(currDecrypted, currDecrypted.length - algorithmBlockLengthForDecryption,
                    prevC, 0,
                    algorithmBlockLengthForDecryption);

            blockOffset += minBlockLengthForThreadForDecryption;
        }
    }

    private void decryptionSubmitThreadCTRAndRD(byte[] input, ExecutorService executorService, List<Future<byte[]>> threadResults) {
        int blockOffset = 0;
        while (blockOffset < input.length) {
            int finalBlockOffset = blockOffset;

            threadResults.add(
                    executorService.submit(
                            () -> decryptThreadTask(getByteArray(input, finalBlockOffset, minBlockLengthForThreadForDecryption), finalBlockOffset, null)));

            blockOffset += minBlockLengthForThreadForDecryption;
        }
    }

    private byte[] decryptionMultithreading(byte[] input, byte[] prevBlock) {

        int countOfBlocks = input.length / algorithmBlockLengthForDecryption;

        List<Future<byte[]>> threadResults = new ArrayList<>();

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);

        if (encryptionMode instanceof ECB) {
            decryptionSubmitThreadTaskECB(input, executorService, threadResults);
        } else if (encryptionMode instanceof EncryptionModePreviousNeeded) {
            decryptionSubmitThreadCBCAndCFB(input, prevBlock, executorService, threadResults);
        } else if (encryptionMode instanceof EncryptionModeCounter) {
            decryptionSubmitThreadCTRAndRD(input, executorService, threadResults);
        } else {
            throw new IllegalMethodCallException("Multithreading is not possible for encryption mode " + encryptionMode.getClass().getName());
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("executorService was interrupted while was waiting for threads results");
            // todo: throw interrupted
        }

        byte[] decrypted = new byte[countOfBlocks * algorithmBlockLengthForEncryption];

        int currPos = 0;
        for (Future<byte[]> future : threadResults) {
            try {
                byte[] futureRes = future.get();
                System.arraycopy(futureRes, 0, decrypted, currPos, futureRes.length);
                currPos += futureRes.length;
            } catch (InterruptedException e) {
                log.error("decryptionMultithreading: Current thread was interrupted while waiting");
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                log.error("encryptionMultithreading: The computation threw an exception");
                throw new RuntimeException(e);
            }
        }

        return decrypted;
    }

    private byte[] erasePad(byte[] input) {
        // input is already decrypted byte array
        int lengthInputWithoutPaddedBlock = input.length - algorithmBlockLengthForEncryption;

        byte[] blockToErasePad = new byte[algorithmBlockLengthForEncryption];
        System.arraycopy(input, lengthInputWithoutPaddedBlock,
                blockToErasePad, 0, algorithmBlockLengthForEncryption);
        byte[] blockWithErasedPad = paddingMode.erasePad(blockToErasePad);

        if (blockWithErasedPad.length == blockToErasePad.length) {
            // erasing pad was not needed
            return input;
        }

        byte[] inputWithErasedPad = new byte[lengthInputWithoutPaddedBlock + blockWithErasedPad.length];
        System.arraycopy(input, 0, inputWithErasedPad, 0, lengthInputWithoutPaddedBlock);
        System.arraycopy(blockWithErasedPad, 0, inputWithErasedPad, lengthInputWithoutPaddedBlock, blockWithErasedPad.length);

        return inputWithErasedPad;
    }

    private byte[] getDecryptedBlock(byte[] input, byte[] prev) {
        if (encryptionMode instanceof ECB
                || encryptionMode instanceof EncryptionModeCounter
                || encryptionMode instanceof EncryptionModePreviousNeeded) {
            return decryptionMultithreading(input, prev);
        }
        return decryptThreadTask(input, -1, prev);
    }

    @Override
    public byte[] decrypt(byte[] input) {
        if (encryptionMode instanceof EncryptionModeWithInitVector encryptionModeWithInitVector) {
            encryptionModeWithInitVector.invokeNextAsNew();
        }

        return erasePad(getDecryptedBlock(input, null));
    }

    @Override
    public void decrypt(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] prev = null;
        byte[] toDecrypt = new byte[filePageSizeModified];

        int bytesRead;


        while ((bytesRead = inputStream.read(toDecrypt)) != -1) {

            if (bytesRead != filePageSizeModified) {
                // shrink block if needed
                byte[] tmp = new byte[bytesRead];
                System.arraycopy(toDecrypt, 0, tmp, 0, bytesRead);
                toDecrypt = tmp;
            }

            // decrypt
            byte[] decrypted = getDecryptedBlock(toDecrypt, prev);

            // check if it is the last block and de-padding needed
            if (bytesRead != filePageSizeModified || inputStream.available() == 0) {
                // de-padding needed
                decrypted = erasePad(decrypted);

                outputStream.write(decrypted);
                break;
            } else {
                // weak spot, every iteration allocating block size for prev =(
                prev = new byte[algorithmBlockLengthForDecryption];
                System.arraycopy(toDecrypt, toDecrypt.length - algorithmBlockLengthForDecryption,
                        prev, 0,
                        algorithmBlockLengthForDecryption);

                // write next byte
                outputStream.write(decrypted);
            }
        }

    }

    @Override
    public void decrypt(Path input, Path output) {
        if (encryptionMode instanceof EncryptionModeWithInitVector encryptionModeWithInitVector) {
            encryptionModeWithInitVector.invokeNextAsNew();
        }

        try (FileInputStream inputStream = new FileInputStream(input.toFile())) {
            try (FileOutputStream outputStream = new FileOutputStream(output.toFile())) {
                decrypt(inputStream, outputStream);
            }
        } catch (FileNotFoundException e) {
            log.error("CARRRAMBA! For some reason file cannot be opened for reading");
            throw new RuntimeException(e);
        } catch (IOException e) {
            log.error("CARRRAMBA! I/O exception happened");
            throw new RuntimeException(e);
        }
    }

    // endregion
}
