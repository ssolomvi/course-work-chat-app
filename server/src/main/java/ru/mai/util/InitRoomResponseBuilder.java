package ru.mai.util;

import org.springframework.stereotype.Component;
import ru.mai.InitRoomResponse;
import ru.mai.encryption_algorithm.impl.DES;
import ru.mai.encryption_algorithm.impl.DEAL;
import ru.mai.encryption_algorithm.impl.Rijndael;
import ru.mai.encryption_mode.EncryptionModeEnum;
import ru.mai.encryption_padding_mode.PaddingModeEnum;
import ru.mai.utils.Operations;
import ru.mai.utils.Pair;

import java.math.BigInteger;
import java.util.Random;

public class InitRoomResponseBuilder {
    private static final Random RANDOM = new Random();

    public static Pair<Integer, Integer> getKeyAndBlockLength(String algorithm) {
        return switch (algorithm) {
            case "DEAL" -> new Pair<>(DEAL.KEY_LENGTH16, DES.BLOCK_LENGTH);
            case "Rijndael" -> new Pair<>(Rijndael.KEY_LENGTH16, 16);
//            case "RC6" -> new Pair<>(RC6.KEY_LENGTH16, RC6.BLOCK_LENGTH);
//            case "LOKI97" -> new Pair<>(LOKI97.KEY_LENGTH16, LOKI97.BLOCK_LENGTH);
//            case "MARS" -> new Pair<>(MARS.KEY_LENGTH16, MARS.BLOCK_LENGTH);
            default -> new Pair<>(DES.KEY_SIZE, DES.BLOCK_LENGTH);
        };
    }


    public static InitRoomResponse build(String companionLogin, String algorithm) {
        String encryptionMode = EncryptionModeEnum.values()[RANDOM.nextInt(EncryptionModeEnum.values().length)].toString();
        String paddingMode = PaddingModeEnum.values()[RANDOM.nextInt(PaddingModeEnum.values().length)].toString();

        Pair<Integer, Integer> keyAndBlockLength = getKeyAndBlockLength(algorithm);

        String initVector = new String(Operations.generateBytes(keyAndBlockLength.getValue()));

        String diffieHellmanP = BigInteger.TWO.pow(8 * keyAndBlockLength.getKey()).toString();

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
}
