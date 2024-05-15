package ru.mai.kafka.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import ru.mai.encryption_context.EncryptionContext;
import ru.mai.kafka.KafkaWriter;
import ru.mai.kafka.serialization.KafkaMessageSerializer;
import ru.mai.model.KafkaMessage;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
public class KafkaWriterImpl implements KafkaWriter {
    private final Map<String, Object> kafkaProducerConfig;
        private final KafkaProducer<String, KafkaMessage> kafkaProducer;
//    private final KafkaProducer<String, String> kafkaProducer;
    private final String topic;
    private final EncryptionContext context;
    private static final Integer FILE_PAGE_SIZE = 1024;

    public KafkaWriterImpl(Map<String, Object> kafkaProducerConfig, String topic, EncryptionContext context) {
        this.kafkaProducerConfig = kafkaProducerConfig;
        this.kafkaProducer = createProducer();

        this.topic = topic;
        this.context = context;
    }


    private KafkaProducer<String, KafkaMessage> createProducer() {
        if (kafkaProducerConfig.containsKey(ProducerConfig.MAX_REQUEST_SIZE_CONFIG)) {
            if (!Objects.equals(kafkaProducerConfig.get(ProducerConfig.MAX_REQUEST_SIZE_CONFIG), FILE_PAGE_SIZE.toString())) {
                Map<String, Object> configs = new HashMap<>(kafkaProducerConfig);
                configs.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, FILE_PAGE_SIZE.toString());
                return new KafkaProducer<>(configs, new StringSerializer(), new KafkaMessageSerializer());
            }
        } else {
            Map<String, Object> configs = new HashMap<>(kafkaProducerConfig);
            configs.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, FILE_PAGE_SIZE.toString());
            return new KafkaProducer<>(configs, new StringSerializer(), new KafkaMessageSerializer());
        }
        return new KafkaProducer<>(
                kafkaProducerConfig,
                new StringSerializer(),
                new KafkaMessageSerializer()
        );
    }

    @Override
    public void send(KafkaMessage message) {
        message.setValue(context.encrypt(message.getValue()));
        kafkaProducer.send(new ProducerRecord<>(topic, message));
    }

    @Override
    public void send(Path input, Path output) {
        // todo
    }

    @Override
    public void send(byte[] input) {
        byte[] encrypted = context.encrypt(input);

        if (encrypted.length <= FILE_PAGE_SIZE) {
            KafkaMessage message = new KafkaMessage(UUID.randomUUID(), encrypted, true, "", 0);
            kafkaProducer.send(new ProducerRecord<>(topic, message));
        } else {
            log.error("Big length!");
            throw new IllegalArgumentException("input byte array length must be less than " + FILE_PAGE_SIZE);
        }
    }

    @Override
    public void close() {
        this.kafkaProducer.flush();
        this.kafkaProducer.close();
    }
}
