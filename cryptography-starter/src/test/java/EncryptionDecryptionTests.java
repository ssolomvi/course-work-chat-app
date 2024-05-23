import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mai.encryption_algorithm.EncryptionAlgorithm;
import ru.mai.encryption_algorithm.impl.DEAL;
import ru.mai.encryption_algorithm.impl.DES;
import ru.mai.encryption_algorithm.impl.LOKI97;
import ru.mai.encryption_algorithm.impl.Rijndael;
import ru.mai.encryption_context.EncryptionContext;
import ru.mai.encryption_context.SymmetricEncryptionContextImpl;
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
import ru.mai.utils.Operations;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import static ru.mai.utils.Operations.byteArrEqual;
import static ru.mai.utils.Operations.filesAreEqual;

public class EncryptionDecryptionTests {
    static final Logger log = LoggerFactory.getLogger(EncryptionDecryptionTests.class);
    public static final byte[] testByteArr = ("""
            The cat (Felis catus), commonly referred to as the domestic cat or house cat, is a small domesticated carnivorous
            mammal. It is the only domesticed species in the family Felidae. Recent advances in archaeology and genetics have shown
            that the domestication of the cat occurred in the Near East around 7500 BC. It is commonly kept as a house pet and farm
            cat, but also ranges freely as a feral cat avoiding human contact. It is valued by humans for companionship and its
            ability to kill vermin. Its retractable claws are adapted to killing small prey like mice and rats. It has a strong,
            flexible body, quick reflexes, sharp teeth, and its night vision and sense of smell are well developed. It is a social
            species, but a solitary hunter and a crepuscular predator. Cat communication includes vocalizations like meowing,
            purring, trilling, hissing, growling, and grunting as well as cat body language. It can hear sounds too faint or too
            high in frequency for human ears, such as those made by small mammals. It also secretes and perceives pheromones.""").getBytes(StandardCharsets.UTF_8);

    public static final byte[] littleTestStrNotMultiple = "IrbitskiyIlushaSergeevich".getBytes(StandardCharsets.UTF_8);
    public static final byte[] littleTestStrMultiple = "IrbitskiyIlushaS".getBytes(StandardCharsets.UTF_8);

    void encryptionModeTest(EncryptionMode mode, byte[] testArray) {
        if (mode instanceof EncryptionModeWithInitVector encryptionModeWithInitVector) {
            encryptionModeWithInitVector.invokeNextAsNew();
        }

        byte[] encrypted;
        if (mode instanceof EncryptionModeCounter encryptionModeCounter) {
            encrypted = encryptionModeCounter.encrypt(testArray, 0);
        } else {
            encrypted = mode.encrypt(testArray);
        }

        byte[] decrypted;
        if (mode instanceof EncryptionModeCounter encryptionModeCounter) {
            decrypted = encryptionModeCounter.decrypt(encrypted, 0);
        } else if (mode instanceof EncryptionModePreviousNeeded encryptionModePreviousNeeded) {
            decrypted = encryptionModePreviousNeeded.decrypt(encrypted, null);
        } else {
            decrypted = mode.decrypt(encrypted);
        }

        Assertions.assertTrue(byteArrEqual(testArray, decrypted).isEmpty());
    }

    @Test
    @DisplayName("Encryption modes with DEAL")
    void testEncryptionModes() {
        byte[] key = Operations.generateBytes(16);

        byte[] initVector = Operations.generateBytes(DEAL.BLOCK_LENGTH);

        EncryptionAlgorithm deal = new DEAL(key);

        byte[] testArr = Operations.generateBytes(DEAL.BLOCK_LENGTH * 16);

        EncryptionMode[] modes = new EncryptionMode[EncryptionModeEnum.values().length];
        modes[0] = new ECB(deal);
        modes[1] = new CBC(deal, initVector);
        modes[2] = new PCBC(deal, initVector);
        modes[3] = new CFB(deal, initVector);
        modes[4] = new OFB(deal, initVector);
        modes[5] = new CTR(deal, initVector);
        modes[6] = new RandomDelta(deal, initVector);

        for (EncryptionMode encryption : modes) {
            log.info("Invoked {} mode: ", encryption.getClass().getName());
            encryptionModeTest(encryption, testArr);
        }
    }

    @Test
    @DisplayName("Context byte array test")
    void fullContextTestByteArray() {
        // init keys
        byte[] keyDES = "secureK".getBytes(StandardCharsets.UTF_8);
        byte[] key16 = "verySecureKeyYea".getBytes(StandardCharsets.UTF_8);
        byte[] key24 = "veryVeryVerySecureKeyYea".getBytes(StandardCharsets.UTF_8);
        byte[] key32 = "veryVeryVeryVeryVerSecureKeyYeah".getBytes(StandardCharsets.UTF_8);

        // init algorithm
        int countAlgorithmsVariations = 7;
        EncryptionAlgorithm[] algorithms = new EncryptionAlgorithm[countAlgorithmsVariations];
        algorithms[0] = new DES(keyDES);
        algorithms[1] = new DEAL(key16);
        algorithms[2] = new DEAL(key24);
        algorithms[3] = new DEAL(key32);
        algorithms[4] = new LOKI97(key16);
        algorithms[5] = new LOKI97(key24);
        algorithms[6] = new LOKI97(key32);

        // init initVector
        byte[] initVector8 = "initVect".getBytes(StandardCharsets.UTF_8);
        byte[] initVector16 = "initializaVector".getBytes(StandardCharsets.UTF_8);

        for (EncryptionAlgorithm algorithm : algorithms) {
            for (EncryptionModeEnum encryption : EncryptionModeEnum.values()) {
                for (PaddingModeEnum padding : PaddingModeEnum.values()) {
                    log.info("Algorithm: {}\nEncryption: {}\nPadding: {}\n", algorithm.getClass().getName(), encryption, padding);
                    if (!encryption.needsInitVector()) {
                        contextWithConfigurationsTestByteArr(encryption, padding, algorithm, null);
                    } else if (algorithm.getAlgorithmBlockForEncryption() == 8) {
                        contextWithConfigurationsTestByteArr(encryption, padding, algorithm, initVector8);
                    } else if (algorithm.getAlgorithmBlockForEncryption() == 16) {
                        contextWithConfigurationsTestByteArr(encryption, padding, algorithm, initVector16);
                    } else {
                        log.error("Expected algorithm block to be of len 16 or 8");
                        throw new RuntimeException("Expected algorithm block to be of len 16 or 8");
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("Context paths test")
    void fullContextTestPaths() {
        Path inputFile = Paths.get("src/test/resources/catBig.txt");
        Path encryptionFile = Paths.get("src/test/resources/catEncrypted.txt");
        Path decryptionFile = Paths.get("src/test/resources/catDecrypted.txt");

        // init keys
        byte[] keyDES = "secureK".getBytes(StandardCharsets.UTF_8);
        byte[] key16 = "verySecureKeyYea".getBytes(StandardCharsets.UTF_8);
        byte[] key24 = "veryVeryVerySecureKeyYea".getBytes(StandardCharsets.UTF_8);
        byte[] key32 = "veryVeryVeryVeryVerSecureKeyYeah".getBytes(StandardCharsets.UTF_8);

        // init algorithm
        int countAlgorithmsVariations = 7;
        EncryptionAlgorithm[] algorithms = new EncryptionAlgorithm[countAlgorithmsVariations];
        algorithms[0] = new DES(keyDES);
        algorithms[1] = new DEAL(key16);
        algorithms[2] = new DEAL(key24);
        algorithms[3] = new DEAL(key32);
        algorithms[4] = new LOKI97(key16);
        algorithms[5] = new LOKI97(key24);
        algorithms[6] = new LOKI97(key32);

        // init initVector
        byte[] initVector8 = "initVect".getBytes(StandardCharsets.UTF_8);
        byte[] initVector16 = "initializaVector".getBytes(StandardCharsets.UTF_8);

        try {
            for (EncryptionAlgorithm algorithm : algorithms) {
                for (EncryptionModeEnum encryption : EncryptionModeEnum.values()) {
                    for (PaddingModeEnum padding : PaddingModeEnum.values()) {
                        log.info("Algorithm: {}\nEncryption: {}\nPadding: {}\n", algorithm.getClass().getName(), encryption, padding);

                        if (!encryption.needsInitVector()) {
                            contextWithConfigurationsTestByteArr(inputFile, encryptionFile, decryptionFile, encryption, padding, algorithm, null);
                        } else if (algorithm.getAlgorithmBlockForEncryption() == 8) {
                            log.debug("Init vector of size 8");
                            contextWithConfigurationsTestByteArr(inputFile, encryptionFile, decryptionFile, encryption, padding, algorithm, initVector8);
                        } else if (algorithm.getAlgorithmBlockForEncryption() == 16) {
                            log.debug("Init vector of size 16");
                            contextWithConfigurationsTestByteArr(inputFile, encryptionFile, decryptionFile, encryption, padding, algorithm, initVector16);
                        } else {
                            log.error("Expected algorithm block to be of len 16 or 8");
                            throw new RuntimeException("Expected algorithm block to be of len 16 or 8");
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("CARRRAMBA! I/O exception happened! ", e);
        }
    }

    @Test
    void simpleContextTestPaths() {
        Path inputFile = Paths.get("src/test/resources/catBig.txt");
        Path encryptionFile = Paths.get("src/test/resources/catEncrypted.txt");
        Path decryptionFile = Paths.get("src/test/resources/catDecrypted.txt");

        byte[] key = "secureK".getBytes(StandardCharsets.UTF_8);
        var des = new DES(key);

        EncryptionContext context = new SymmetricEncryptionContextImpl(EncryptionModeEnum.ECB, PaddingModeEnum.ZEROES, des);

        context.encrypt(inputFile, encryptionFile);
        context.decrypt(encryptionFile, decryptionFile);

        try {
            Assertions.assertTrue(filesAreEqual(inputFile, decryptionFile));
        } catch (IOException e) {
            log.info(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    void contextWithConfigurationsTestByteArr(EncryptionModeEnum modeEnum, PaddingModeEnum paddingModeEnum,
                                              EncryptionAlgorithm algorithm, byte[] initVector) {
        EncryptionContext context = new SymmetricEncryptionContextImpl(modeEnum, paddingModeEnum, algorithm, initVector);

        byte[] decrypted = context.decrypt(context.encrypt(testByteArr));

        Assertions.assertTrue(byteArrEqual(testByteArr, decrypted).isEmpty());
    }

    void contextWithConfigurationsTestByteArr(Path input, Path encryption, Path decryption,
                                              EncryptionModeEnum modeEnum, PaddingModeEnum paddingModeEnum,
                                              EncryptionAlgorithm algorithm, byte[] initVector) throws IOException {
        EncryptionContext context = new SymmetricEncryptionContextImpl(modeEnum, paddingModeEnum, algorithm, initVector);

        context.encrypt(input, encryption);
        context.decrypt(encryption, decryption);

        Assertions.assertTrue(filesAreEqual(input, decryption));
    }

    @Test
    @DisplayName("Padding string is multiple")
    void paddingMultipleTest() {
        PaddingMode[] modes = new PaddingMode[PaddingModeEnum.values().length];
        int blockSize = 16;
        modes[0] = new Zeroes(blockSize);
        modes[1] = new ANSI_X_923(blockSize);
        modes[2] = new PKCS7(blockSize);
        modes[3] = new ISO10126(blockSize);

        for (PaddingMode padding : modes) {
            log.info("Invoked padding mode {}", padding.getClass().getName());
            byte[] dePadded = padding.erasePad(padding.pad(littleTestStrMultiple));
            Assertions.assertTrue(byteArrEqual(littleTestStrMultiple, dePadded).isEmpty());
        }
    }

    @Test
    @DisplayName("Padding string is not multiple")
    void paddingNonMultipleTest() {
        PaddingMode[] modes = new PaddingMode[PaddingModeEnum.values().length];
        int blockSize = 16;
        modes[0] = new Zeroes(blockSize);
        modes[1] = new ANSI_X_923(blockSize);
        modes[2] = new PKCS7(blockSize);
        modes[3] = new ISO10126(blockSize);

        for (PaddingMode padding : modes) {
            log.info("Invoked padding mode {}", padding.getClass().getName());
            byte[] dePadded = padding.erasePad(padding.pad(littleTestStrNotMultiple));
            Assertions.assertTrue(byteArrEqual(littleTestStrNotMultiple, dePadded).isEmpty());
        }
    }

    @Test
    @DisplayName("Rijndael tests")
    void rijndaelTest() {
        byte[] keyRijndael16 = "keyForRijndael16".getBytes(StandardCharsets.UTF_8);
        byte[] keyRijndael24 = "abMSE2Uv)|fCEW6xj8[}XZZg".getBytes(StandardCharsets.UTF_8);
        byte[] keyRijndael32 = "RUjQPjeXGDFgcOwMymRoEmJptmKZdfNF".getBytes(StandardCharsets.UTF_8);

        Rijndael rijndael16 = new Rijndael(keyRijndael16, (byte) 27, 16);
        Rijndael rijndael24 = new Rijndael(keyRijndael24, (byte) 27, 16);
        Rijndael rijndael32 = new Rijndael(keyRijndael32, (byte) 27, 16);

        byte[] input16 = "nGvJLcfbDwbVPmDo".getBytes(StandardCharsets.UTF_8);
        byte[] input24 = "amkQfJXkFJONbbBnIrQVyFdQ".getBytes(StandardCharsets.UTF_8);
        byte[] input32 = "hjOhsaUTdxsermRXZSZkXOfVWnPxxBTY".getBytes(StandardCharsets.UTF_8);

        simpleRijndaelTest(rijndael16, input16);
        simpleRijndaelTest(rijndael24, input16);
        simpleRijndaelTest(rijndael32, input16);

        rijndael16.changeLenBlock(24);
        rijndael24.changeLenBlock(24);
        rijndael32.changeLenBlock(24);
        simpleRijndaelTest(rijndael16, input24);
        simpleRijndaelTest(rijndael24, input24);
        simpleRijndaelTest(rijndael32, input24);

        rijndael16.changeLenBlock(32);
        rijndael24.changeLenBlock(32);
        rijndael32.changeLenBlock(32);
        simpleRijndaelTest(rijndael16, input32);
        simpleRijndaelTest(rijndael24, input32);
        simpleRijndaelTest(rijndael32, input32);
    }

    void simpleRijndaelTest(Rijndael rijndael, byte[] input) {
        byte[] decrypted = rijndael.decrypt(rijndael.encrypt(input));
        Assertions.assertTrue(byteArrEqual(input, decrypted).isEmpty());
    }

}
