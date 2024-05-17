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
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
public class KafkaWriterImpl implements KafkaWriter {
    private final KafkaProducer<String, KafkaMessage> kafkaProducer;
    private final String topic;
    private final EncryptionContext context;
    private static final Integer FILE_PAGE_SIZE = 4092;

    public KafkaWriterImpl(Map<String, Object> kafkaProducerConfig, String topic, EncryptionContext context) {
        this.kafkaProducer = createProducer(kafkaProducerConfig);

        this.topic = topic;
        this.context = context;
    }


    private KafkaProducer<String, KafkaMessage> createProducer(Map<String, Object> kafkaProducerConfig) {
        int maxRequestSize = FILE_PAGE_SIZE >= 1000 ? FILE_PAGE_SIZE + FILE_PAGE_SIZE / 100 : FILE_PAGE_SIZE + 10;
        String maxRequestSizeConfig = Integer.toString(maxRequestSize);
        if (kafkaProducerConfig.containsKey(ProducerConfig.MAX_REQUEST_SIZE_CONFIG)) {
            if (!Objects.equals(kafkaProducerConfig.get(ProducerConfig.MAX_REQUEST_SIZE_CONFIG), maxRequestSizeConfig)) {
                Map<String, Object> configs = new HashMap<>(kafkaProducerConfig);
                configs.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, maxRequestSizeConfig);
                return new KafkaProducer<>(configs, new StringSerializer(), new KafkaMessageSerializer());
            }
        } else {
            Map<String, Object> configs = new HashMap<>(kafkaProducerConfig);
            configs.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, maxRequestSize);
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
    public void send(Path input, Path tmp) throws IOException {
        log.info("Start sending message with use of {} as tmp file", tmp.toString());
        context.encrypt(input, tmp);

        UUID id = UUID.randomUUID();
        // todo: return input file name after debug
//        String fileName = input.getFileName().toString();
        String fileName = tmp.getFileName().toString();

        long fileSize = Files.size(tmp);
        int numberOfPartitions = (int) (fileSize / FILE_PAGE_SIZE + (fileSize % FILE_PAGE_SIZE == 0 ? 0 : 1));

        try (FileInputStream inputStream = new FileInputStream(tmp.toFile())) {
            byte[] buffer = new byte[FILE_PAGE_SIZE];
            int currPartition = 0;
            int readBytes;
            int currFilePos = 0;

            while ((readBytes = inputStream.read(buffer)) != -1) {
                if (readBytes + currFilePos >= fileSize) {
                    byte[] tmpArr = new byte[readBytes];
                    System.arraycopy(buffer, 0, tmpArr, 0, readBytes);
                    buffer = tmpArr;
                }

                KafkaMessage message = new KafkaMessage(id, buffer, fileName, numberOfPartitions, currPartition++);
                kafkaProducer.send(new ProducerRecord<>(topic, message));
                log.info("Sent a part to topic: {}, current index: {}, message:\n{}", topic, currPartition, message);
            }
        }
    }

    @Override
    public void send(byte[] input) {
        byte[] encrypted = context.encrypt(input);

        if (encrypted.length <= FILE_PAGE_SIZE) {
            KafkaMessage message = new KafkaMessage(UUID.randomUUID(), encrypted, "", 1, 0);
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
