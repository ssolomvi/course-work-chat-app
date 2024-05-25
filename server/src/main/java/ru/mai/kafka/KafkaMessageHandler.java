package ru.mai.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mai.kafka.model.MessageDto;
import ru.mai.repositories.ActiveUsersAndConsumersRepository;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
public class KafkaMessageHandler {
    private static final Duration POLL_DATA_TIME = Duration.ofMillis(300);
    private final ActiveUsersAndConsumersRepository repository;

    public KafkaMessageHandler(@Autowired ActiveUsersAndConsumersRepository repository) {
        this.repository = repository;
    }

    KafkaProducer<String, MessageDto> producer = KafkaManager.createKafkaProducer();

    public void sendMessage(String companion, MessageDto message) {
        producer.send(new ProducerRecord<>(KafkaManager.getTopicName(companion), message));
    }

    public Optional<ConsumerRecords<String, MessageDto>> readMessages(String user) {
        var op = repository.getConsumer(user);
        if (op.isEmpty()) {
            log.debug("No consumer for user {}", user);
            return Optional.empty();
        }

        var consumer = op.get();
        return Optional.of(consumer.poll(POLL_DATA_TIME));
    }
}
