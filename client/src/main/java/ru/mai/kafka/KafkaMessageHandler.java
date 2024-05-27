package ru.mai.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import ru.mai.kafka.model.MessageDto;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
public class KafkaMessageHandler {
    private static final Duration POLL_DATA_TIME = Duration.ofMillis(300);
    KafkaProducer<String, MessageDto> producer;
    KafkaConsumer<String, MessageDto> consumer;

    public void init(String login) {
        this.producer = KafkaManager.createKafkaProducer();
        this.consumer = KafkaManager.createKafkaConsumer(KafkaManager.getTopicName(login));
    }

    public void sendMessage(String companion, MessageDto message) {
        producer.send(new ProducerRecord<>(KafkaManager.getTopicName(companion), message));
        log.debug("-> {}: sent message {}", companion, message);
    }

    public Optional<ConsumerRecords<String, MessageDto>> readMessages() {
        return Optional.ofNullable(consumer.poll(POLL_DATA_TIME));
    }
}
