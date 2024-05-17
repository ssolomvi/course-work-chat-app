package ru.mai.kafka.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import ru.mai.encryption_context.EncryptionContext;
import ru.mai.kafka.KafkaReader;
import ru.mai.kafka.serialization.KafkaMessageDeserializer;
import ru.mai.model.KafkaMessage;
import ru.mai.utils.Pair;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class KafkaReaderImpl implements KafkaReader {
    private final KafkaConsumer<String, KafkaMessage> kafkaConsumer;
    private final String topic; // consumer topic to subscribe
    private final EncryptionContext context;

    private final ExecutorService executorForDecrypting = Executors.newFixedThreadPool(1);

    // (Integer) -- max index?
    private final HashMap<UUID, Pair<String, Integer>> files = new HashMap<>();
    private static final Integer FILE_PAGE_SIZE = 4092;


    public KafkaReaderImpl(Map<String, Object> kafkaConsumerConfigs,
                           String topic, EncryptionContext context) {
        this.topic = topic;
        this.kafkaConsumer = createConsumer(kafkaConsumerConfigs);
        this.kafkaConsumer.subscribe(Collections.singletonList(topic));

        this.context = context;
    }

    private KafkaConsumer<String, KafkaMessage> createConsumer(Map<String, Object>  kafkaConsumerConfig) {
        return new KafkaConsumer<>(
                kafkaConsumerConfig,
                new StringDeserializer(),
                new KafkaMessageDeserializer()
        );
    }

    @Override
    public void processing() throws IOException {
        log.info("Start reading Kafka topic: {}", topic);
        boolean isLast = false; // todo: not in prod

        while (true) {
            ConsumerRecords<String, KafkaMessage> consumerRecords = kafkaConsumer.poll(Duration.ofMillis(100));

            for (ConsumerRecord<String, KafkaMessage> consumerRecord : consumerRecords) {
                log.info("Partition number: {}, Record value:\n{}", consumerRecord.partition(), consumerRecord.value());
                KafkaMessage msg = consumerRecord.value();

                if (msg.getFileName().isEmpty()) {
                    isLast = processAsByteArray(msg);
                } else {
                    isLast = processAsPath(msg);
                }

            }
            if (!consumerRecords.isEmpty() && isLast) {
                log.info("Breaking");
                break;
            }
        }
    }

    @Override
    public void close() {
//        this.kafkaConsumer.close();
        this.executorForDecrypting.shutdown();
    }

    // todo: later change return to void
    private boolean processAsByteArray(KafkaMessage msg) {
        executorForDecrypting.submit(() -> decryptAndLogic(msg.getValue()));
        return true;
    }

    private boolean processAsPath(KafkaMessage msg) throws IOException {
        try (RandomAccessFile rnd = new RandomAccessFile(msg.getFileName(), "rw")) {
            log.info("File {} opened as RandomAccessFile", msg.getFileName());
            byte[] toWrite = msg.getValue();
            rnd.seek((long) msg.getCurrIndex() * FILE_PAGE_SIZE);
            rnd.write(toWrite);

            if (files.containsKey(msg.getMessageId())) {
                var metaInformation = files.get(msg.getMessageId());
                var updated = new Pair<>(msg.getFileName(), metaInformation.getValue() - 1);

                files.put(msg.getMessageId(), updated);
            } else {
                files.put(msg.getMessageId(), new Pair<>(msg.getFileName(), msg.getNumberOfPartitions() - 1));
            }

            if (files.get(msg.getMessageId()).getValue() == 0) {
                // todo: for debug, testing
                context.decrypt(Path.of(msg.getFileName()), Path.of("TMP.txt"));
                log.info("File TMP.txt READY!");
                return true;
            }

        }

        return false;
    }

    private void decryptAndLogic(byte[] encrypted) {
        byte[] decrypted = context.decrypt(encrypted);
        log.info("DECRYPTED: {}", new String(decrypted));
        // todo: invoke client's request to db, ui in this loop
    }

}
