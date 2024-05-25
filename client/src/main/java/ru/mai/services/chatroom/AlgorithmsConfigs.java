package ru.mai.services.chatroom;

import ru.mai.encryption_algorithm.impl.*;

public class AlgorithmsConfigs {
    public static final int DES_KEY_LENGTH = DES.KEY_SIZE;
    public static final int DES_BLOCK_LENGTH = DES.BLOCK_LENGTH;
    public static final int DEAL_KEY_LENGTH = DEAL.KEY_LENGTH16;
    public static final int DEAL_BLOCK_LENGTH = DEAL.BLOCK_LENGTH;
    public static final int RIJNDAEL_KEY_LENGTH = Rijndael.KEY_LENGTH16;
    public static final int RIJNDAEL_BLOCK_LENGTH = 16;
    public static final int LOKI97_KEY_LENGTH = LOKI97.KEY_LENGTH_LOKI97_16;
    public static final int LOKI97_BLOCK_LENGTH = LOKI97.BLOCK_LENGTH_LOKI97;
    public static final int RC6_KEY_LENGTH = RC6.KEY_LENGTH_RC6_16;
    public static final int RC6_BLOCK_LENGTH = RC6.BLOCK_LENGTH_RC6_16;
    // todo
//    public static final int MARS_KEY_LENGTH = ;
//    public static final int MARS_BLOCK_LENGTH = ;
}
