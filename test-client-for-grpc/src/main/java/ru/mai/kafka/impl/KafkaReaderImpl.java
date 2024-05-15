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

import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class KafkaReaderImpl implements KafkaReader {
    private final Map<String, Object> kafkaConsumerConfigs;
    private final KafkaConsumer<String, KafkaMessage> kafkaConsumer;
    //    private final KafkaConsumer<String, String> kafkaConsumer;
    private final String topic; // consumer topic to subscribe
    private final EncryptionContext context;

    private final ExecutorService executorForDecrypting = Executors.newFixedThreadPool(1);

    // (Integer) -- max index?
    private final HashMap<UUID, Pair<Path, Integer>> files = new HashMap<>();


    public KafkaReaderImpl(Map<String, Object> kafkaConsumerConfigs,
                           String topic, EncryptionContext context) {
        this.kafkaConsumerConfigs = kafkaConsumerConfigs;
        this.topic = topic;
        this.kafkaConsumer = createConsumer();
        this.kafkaConsumer.subscribe(Collections.singletonList(topic));

        this.context = context;
    }

    private KafkaConsumer<String, KafkaMessage> createConsumer() {
        return new KafkaConsumer<>(
                kafkaConsumerConfigs,
                new StringDeserializer(),
                new KafkaMessageDeserializer()
        );
    }

    @Override
    public void processing() {
        log.info("Start reading Kafka topic: {}", topic);

        // todo: not in prod
        boolean isLast = false;

        while (true) {
            ConsumerRecords<String, KafkaMessage> consumerRecords = kafkaConsumer.poll(Duration.ofMillis(100));

            for (ConsumerRecord<String, KafkaMessage> consumerRecord : consumerRecords) {
                // TODO: files
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
//        get message from kafka reader, decrypt it, return it
    }

    @Override
    public void close() {
        this.kafkaConsumer.close();
    }

    // todo: later change return to void
    private boolean processAsByteArray(KafkaMessage msg) {
        // todo: consumer record in not necessary to be in right order
        executorForDecrypting.submit(() -> decryptAndLogic(msg.getValue()));
        return true;
    }

    private boolean processAsPath(KafkaMessage msg) {
        // todo:
        return true;
    }
    
    private void decryptAndLogic(byte[] encrypted) {
            byte[] decrypted = context.decrypt(encrypted);
            log.info("DECRYPTED: {}", new String(decrypted));
            // todo: invoke client's request to db, ui in this loop
    }

    /**
     * Запускает KafkaConsumer в бесконечном цикле и читает сообщения. Внутри метода происходит обработка
     * сообщений по правилам и отправка сообщений в Kafka выходной топик. Конфигурация для консьюмера из файла *.conf
     */
    /*
    @Override
    public void processing() {
        // start scheduled db reading, updating rules
        // in endless iteration:
        // get message, process message with rules, write message
        try {
            log.info("Start reading Kafka topic: {}", topic);

            scheduleGettingRulesFromDb();

            while (true) { // ugly, cannot complete w/o exception
                ConsumerRecords<String, String> consumerRecords = kafkaConsumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                    log.debug("Read message: {} : {}, from topic: {}", Integer.toHexString(System.identityHashCode(consumerRecord.value())), consumerRecord.value(), topic);

                    // build initial message
                    Message toProcess = Message.builder()
                            .value(consumerRecord.value())
                            .deduplicationState(false)
                            .build();

                    executorForDeduplication.submit(() -> filterMessage(toProcess));

                }
            }
        } catch (Exception e) {
            log.error("Exception happened: ", e);
        } finally {
            executorForDeduplication.shutdown();
            kafkaConsumer.close();
            kafkaWriter.close();
            ruleProcessor.close();
        }
    }

    private KafkaConsumer<String, String> createConsumer() {
        return new KafkaConsumer<>(
                kafkaConsumerConfigs,
                new StringDeserializer(),
                new StringDeserializer()
        );
    }

    private void scheduleGettingRulesFromDb() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() ->
        {
            // it might be better to sort rules here
            Rule[] rulesFromDB = dbReader.readRulesFromDB();
            for (Rule rule : rulesFromDB) {
                if (rules.contains(rule)) {
                    // change rule for a newer one
                    rules.remove(rule);
                    rules.add(rule);
                    log.debug("Updated rule {}", rule);
                } else {
                    // simply add rule
                    rules.add(rule);
                    log.debug("Added rule {}", rule);
                }
            }
            List<Rule> tmpRules = rules
                    .stream()
                    .sorted((o1, o2) -> (int) (o1.getRuleId() - o2.getRuleId()))
                    .toList();
            // it will be good if rules.clear and rules addAll be a transaction
            rules.clear();
            rules.addAll(tmpRules);
        }, 0, updateIntervalSec, TimeUnit.SECONDS);
    }

    private void filterMessage(Message message) {
        Rule[] filterRules = rules.toArray(new Rule[0]);

        // process string
        Message resultProcessedRules = ruleProcessor.processing(message, filterRules);

        // write string
        if (message.isDeduplicationState()) {
            kafkaWriter.processing(resultProcessedRules);
        }
    }*/
}
