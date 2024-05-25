package ru.mai.kafka.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import ru.mai.encryption_context.EncryptionContext;
import ru.mai.kafka.KafkaWriter;
import ru.mai.kafka.serialization.KafkaMessageSerializer;
import ru.mai.kafka.model.KafkaMessage;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Slf4j
public class KafkaWriterImpl implements KafkaWriter {
    private final KafkaProducer<String, KafkaMessage> kafkaProducer;
    private final String topic;
    private final EncryptionContext context;
    private static final Integer FILE_PAGE_SIZE = 65536;

    public KafkaWriterImpl(Map<String, Object> kafkaProducerConfig, String topic, EncryptionContext context) {
        this.kafkaProducer = createProducer(kafkaProducerConfig);

        this.topic = topic;
        this.context = context;
    }


    private KafkaProducer<String, KafkaMessage> createProducer(Map<String, Object> kafkaProducerConfig) {
        // 124 due to KafkaMessage structure: UUID - 16 bytes, int x2 = 8 bytes, 100 bytes for filename
        int maxRequestSize = FILE_PAGE_SIZE >= 2000 ? FILE_PAGE_SIZE + FILE_PAGE_SIZE / 10 : FILE_PAGE_SIZE + 124;
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

        Future<RecordMetadata> response = kafkaProducer.send(new ProducerRecord<>(topic, message));

        Optional.ofNullable(response).ifPresent(rsp ->
                {
                    try {
                        log.info("Message send: {}: {}", rsp.get(), message.getValue());
                    } catch (InterruptedException | ExecutionException e) {
                        log.error("Error reading response: ", e);
                        Thread.currentThread().interrupt();
                    }
                }
        );
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

            while ((readBytes = inputStream.read(buffer)) != -1) {
                if (readBytes != FILE_PAGE_SIZE) {
                    byte[] tmpArr = new byte[readBytes];
                    System.arraycopy(buffer, 0, tmpArr, 0, readBytes);
                    buffer = tmpArr;
                }

                KafkaMessage message = new KafkaMessage(id, fileName, numberOfPartitions, currPartition++, buffer);

                Future<RecordMetadata> response = kafkaProducer.send(new ProducerRecord<>(topic, message));

                Optional.ofNullable(response).ifPresent(rsp ->
                        {
                            try {
                                log.info("Sent: {}; to topic: {}, message:\n{}", rsp.get(), topic, message);
                            } catch (InterruptedException | ExecutionException e) {
                                log.error("Error reading response: ", e);
                                Thread.currentThread().interrupt();
                            }
                        }
                );

            }
        }
    }

    @Override
    public void send(byte[] input) {
        byte[] encrypted = context.encrypt(input);

        if (encrypted.length <= FILE_PAGE_SIZE) {
            KafkaMessage message = new KafkaMessage(UUID.randomUUID(), "", 1, 0, encrypted);
            log.info("Sent byte array: {}", message);
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
