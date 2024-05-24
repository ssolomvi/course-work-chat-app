package ru.mai.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class KafkaManager {
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String GROUP_ID_CONFIG = "server_group_id";
    private static final String AUTO_OFFSET_RESET = "earliest";
    private static final int PARTITIONS = 1;
    private static final int REPLICA_FACTOR = 1;
    private static final Map<String, String> TOPIC_CONFIG = Map.of(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(60 * 60 * 1000)); // for an hour
    private static final int WAIT_AT_MOST_SEC = 30;
    private static final String TOPIC_PREFIX = "chat_app_topic";
    private static final Integer FILE_PAGE_SIZE = 4092;

    private static final AdminClient admin = AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS));
    private static KafkaProducer<String, String> producer;

    public static String getTopicName(String userLogin) {
        return String.format("%s_%s", TOPIC_PREFIX, userLogin);
    }


    public static KafkaConsumer<String, String> createKafkaConsumer(String topic) {
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(
                Map.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS,
                        ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID_CONFIG,
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, AUTO_OFFSET_RESET),
                new StringDeserializer(),
                new StringDeserializer()
        );

        consumer.subscribe(Collections.singleton(topic));
        return consumer;
    }

    public static KafkaProducer<String, String> createKafkaProducer() {
        if (producer == null) {
            int maxRequestSize = FILE_PAGE_SIZE >= 2000 ? FILE_PAGE_SIZE + FILE_PAGE_SIZE / 10 : FILE_PAGE_SIZE + 150;
            String maxRequestSizeConfig = Integer.toString(maxRequestSize);

            producer = new KafkaProducer<>(
                    Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS,
                            ProducerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString(),
                            ProducerConfig.MAX_REQUEST_SIZE_CONFIG, maxRequestSizeConfig),
                    new StringSerializer(),
                    new StringSerializer());
        }

        return producer;
    }

    public static void createTopic(String topicName) {
        try {
            NewTopic topic = new NewTopic(topicName, PARTITIONS, (short) REPLICA_FACTOR).configs(TOPIC_CONFIG);

            ListTopicsResult topics = admin.listTopics();
            if (topics.names().get() != null && (topics.names().get().contains(topicName))) {
                    log.info("Topic {} already exist", topicName);
                    return;

            }

            admin.createTopics(Collections.singleton(topic)).all().get(WAIT_AT_MOST_SEC, TimeUnit.SECONDS);

        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            log.error("Error creating topic Kafka", e);
        }
    }

    public static void deleteTopic(String topicName) {
        try {
            admin.deleteTopics(Collections.singletonList(topicName)).all().get(WAIT_AT_MOST_SEC, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("Error deleting topic Kafka", e);
        }
    }

    private KafkaManager() {
        // to hide default constructor
    }
}
