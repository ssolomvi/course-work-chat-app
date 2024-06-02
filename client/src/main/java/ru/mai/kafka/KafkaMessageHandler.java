package ru.mai.kafka;

import com.vaadin.flow.spring.annotation.SpringComponent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.errors.InterruptException;
import org.springframework.context.annotation.Scope;
import ru.mai.kafka.model.MessageDto;

import java.time.Duration;
import java.util.ConcurrentModificationException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Slf4j
@SpringComponent
@Scope("prototype")
public class KafkaMessageHandler {
    private static final Duration POLL_DATA_TIME = Duration.ofMillis(300);
    private KafkaProducer<String, MessageDto> producer = null;
    private KafkaConsumer<String, MessageDto> consumer = null;

    public void init(String login) {
        this.producer = KafkaManager.createKafkaProducer();
        this.consumer = KafkaManager.createKafkaConsumer(login);
    }

    public void sendMessage(String companion, MessageDto message) {
        Future<RecordMetadata> response =
                producer.send(new ProducerRecord<>(KafkaManager.getTopicName(companion), message));
        Optional.ofNullable(response).ifPresent(rsp ->
                {
                    try {
                        log.info("Message send: {} from {} filename {}", rsp.get(), message.getSender(), message.getFilename());
                    } catch (InterruptedException | ExecutionException e) {
                        log.error("Error reading response: ", e);
                        Thread.currentThread().interrupt();
                    }
                }
        );
    }

    public Optional<ConsumerRecords<String, MessageDto>> readMessages() {
        return Optional.ofNullable(consumer.poll(POLL_DATA_TIME));
    }

    public void close() {
        try {
//            KafkaManager.deleteTopic(KafkaManager.getTopicName(login));
            producer.close();
            consumer.close();
        } catch (InterruptException | ConcurrentModificationException e) {
            log.warn("Error closing kafka producer/consumer: ", e);
        }
    }
}
