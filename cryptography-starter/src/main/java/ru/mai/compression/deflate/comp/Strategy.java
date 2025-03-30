package ru.mai.compression.deflate.comp;


public interface Strategy {

    Decision decide(byte[] b, int off, int historyLen, int dataLen);

}
