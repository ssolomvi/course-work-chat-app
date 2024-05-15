package ru.mai.kafka;

public interface KafkaReader extends AutoCloseable {
    // Запускает KafkaConsumer в бесконечном цикле и читает сообщения.
    // Внутри метода происходит обработка сообщений по правилам и отправка сообщений в Kafka выходной топик.
    // Конфигурация для консьюмера из файла *.conf
    void processing();

    @Override
    void close();
}
