package ru.mai.compression.deflate;

import java.util.Objects;


public final class DataFormatException extends RuntimeException {

    /*---- Field ----*/

    private final Reason reason;


    /*---- Constructor ----*/

    public DataFormatException(Reason rsn, String msg) {
        super(msg);
        reason = Objects.requireNonNull(rsn);
    }


    /*---- Function ----*/

    /*
     * Always throws, never returns. Use this shorter form whenever possible:
     *     DataFormatException.throwUnexpectedEnd();
     * Otherwise if definite control flow manipulation is needed, then use:
     *     int foo;
     *     try {
     *         foo = bar();
     *     } catch (EOFException e) {
     *         throw DataFormatException.throwUnexpectedEnd();
     *     }
     *     print(foo);
     */
    public static DataFormatException throwUnexpectedEnd() {
        throw new DataFormatException(
                Reason.UNEXPECTED_END_OF_STREAM,
                "Unexpected end of stream");
    }


    /*---- Method ----*/

    public Reason getReason() {
        return reason;
    }



    /*---- Enumeration ----*/

    public enum Reason {
        UNEXPECTED_END_OF_STREAM,
        RESERVED_BLOCK_TYPE,
        UNCOMPRESSED_BLOCK_LENGTH_MISMATCH,
        HUFFMAN_CODE_UNDER_FULL,
        HUFFMAN_CODE_OVER_FULL,
        NO_PREVIOUS_CODE_LENGTH_TO_COPY,
        CODE_LENGTH_CODE_OVER_FULL,
        END_OF_BLOCK_CODE_ZERO_LENGTH,
        RESERVED_LENGTH_SYMBOL,
        RESERVED_DISTANCE_SYMBOL,
        LENGTH_ENCOUNTERED_WITH_EMPTY_DISTANCE_CODE,
        COPY_FROM_BEFORE_DICTIONARY_START,

    }

}
