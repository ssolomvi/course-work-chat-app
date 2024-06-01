package ru.mai.util;

import ru.mai.Algorithm;
import ru.mai.EncryptionMode;
import ru.mai.InitRoomResponse;
import ru.mai.PaddingMode;
import ru.mai.primality_test.impl.ProbabilisticPrimalityTestSolovayStrassen;
import ru.mai.utils.MathOperationsBigInteger;
import ru.mai.utils.Operations;
import ru.mai.utils.Pair;

public class InitRoomResponseBuilder {

    public static Pair<Integer, Integer> getKeyAndBlockLength(Algorithm algorithm) {
        return switch (algorithm) {
            case ALGORITHM_DEAL -> new Pair<>(AlgorithmsConfigs.DEAL_KEY_LENGTH, AlgorithmsConfigs.DEAL_BLOCK_LENGTH);
            case ALGORITHM_RIJNDAEL -> new Pair<>(AlgorithmsConfigs.RIJNDAEL_KEY_LENGTH, AlgorithmsConfigs.RIJNDAEL_BLOCK_LENGTH);
            case ALGORITHM_RC6 -> new Pair<>(AlgorithmsConfigs.RC6_KEY_LENGTH, AlgorithmsConfigs.RC6_BLOCK_LENGTH);
            case ALGORITHM_LOKI97 -> new Pair<>(AlgorithmsConfigs.LOKI97_KEY_LENGTH, AlgorithmsConfigs.LOKI97_BLOCK_LENGTH);
            case ALGORITHM_MARS -> new Pair<>(AlgorithmsConfigs.MARS_KEY_LENGTH, AlgorithmsConfigs.MARS_BLOCK_LENGTH);
            default -> new Pair<>(AlgorithmsConfigs.DES_KEY_LENGTH, AlgorithmsConfigs.DES_BLOCK_LENGTH);
        };
    }


    public static InitRoomResponse build(String companionLogin, Algorithm algorithm, EncryptionMode encryptionMode, PaddingMode paddingMode) {
        Pair<Integer, Integer> keyAndBlockLength = getKeyAndBlockLength(algorithm);

        String initVector = new String(Operations.generateBytes(keyAndBlockLength.getValue()));

        String diffieHellmanP = MathOperationsBigInteger.generateProbablePrime(8 * keyAndBlockLength.getKey(), new ProbabilisticPrimalityTestSolovayStrassen(), 0.999).toString();

        return InitRoomResponse.newBuilder()
                .setCompanionLogin(companionLogin)
                .setEncryptionMode(encryptionMode)
                .setPaddingMode(paddingMode)
                .setAlgorithm(algorithm)
                .setInitVector(initVector)
                .setDiffieHellmanP(diffieHellmanP)
                .build();
    }

    public static InitRoomResponse buildForCompanion(String ownLogin, InitRoomResponse ownResponse) {
        return InitRoomResponse.newBuilder()
                .setCompanionLogin(ownLogin)
                .setEncryptionMode(ownResponse.getEncryptionMode())
                .setPaddingMode(ownResponse.getPaddingMode())
                .setAlgorithm(ownResponse.getAlgorithm())
                .setInitVector(ownResponse.getInitVector())
                .setDiffieHellmanP(ownResponse.getDiffieHellmanP())
                .build();
    }

    private InitRoomResponseBuilder() {
        // to hide implicit constructor
    }
}
