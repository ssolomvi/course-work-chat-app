package ru.mai.kafka.impl;

import lombok.extern.slf4j.Slf4j;
import ru.mai.encryption_context.EncryptionContext;
import ru.mai.kafka.KafkaContext;
import ru.mai.kafka.KafkaReader;
import ru.mai.kafka.KafkaWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
public class KafkaContextImpl implements KafkaContext {
    private final KafkaReader reader;
    private final KafkaWriter writer;

    private static final String PATTERN_FORMAT = "yy-MM-dd_HH-mm-ss-SS";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PATTERN_FORMAT).withZone(ZoneId.systemDefault());

    public KafkaContextImpl(Map<String, Object> consumerConfig, Map<String, Object> producerConfig,
                            String consumerTopic, String producerTopic,
                            EncryptionContext context) {
        reader = constructKafkaReader(consumerConfig, consumerTopic, context);
        writer = constructKafkaWriter(producerConfig, producerTopic, context);
    }

    public KafkaWriter constructKafkaWriter(Map<String, Object> producerConfig, String producerTopic, EncryptionContext context) {
        return new KafkaWriterImpl(producerConfig, producerTopic, context);
    }

    public KafkaReader constructKafkaReader(Map<String, Object> consumerConfig, String consumerTopic, EncryptionContext context) {
        return new KafkaReaderImpl(consumerConfig, consumerTopic, context);
    }

    @Override
    public void send(byte[] msg) {
        writer.send(msg);
    }


    /**
     * Sends a file using Kafka producer
     *
     * @param msg path to file to send
     * @throws IOException if error deleting temporary file
     */
    @Override
    public void send(Path msg) throws IOException {
        String fileName = msg.getName(msg.getNameCount() - 1).toString();
        String[] tmp = fileName.split("\\.");

        StringBuilder builder = new StringBuilder();
        builder.append(tmp[0]).append("_tmp").append(formatter.format(Instant.now())).append(".");
        for (int i = 1; i < tmp.length; i++) {
            builder.append(tmp[i]);
        }

        Path tmpFile = Paths.get(builder.toString());
        log.debug("Passing sending message to kafkaWriter");
        writer.send(msg, tmpFile);
        Files.delete(tmpFile);
    }

    @Override
    public void startListening() throws IOException {
        reader.processing();
    }

    @Override
    public void close() {
        this.writer.close();
        this.reader.close();
    }
}
