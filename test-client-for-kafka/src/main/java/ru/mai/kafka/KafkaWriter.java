package ru.mai.kafka;


import ru.mai.model.KafkaMessage;

import java.io.IOException;
import java.nio.file.Path;

public interface KafkaWriter extends AutoCloseable {
    void send(KafkaMessage message); // отправляет сообщения с deduplicationState = true в выходной топик. Конфигурация берется из файла *.conf
//    void processing(String message); // отправляет сообщения с deduplicationState = true в выходной топик. Конфигурация берется из файла *.conf
    void send(Path input, Path output) throws IOException;
    void send(byte[] input);
    @Override
    void close();
}
