package ru.mai;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import org.testcontainers.utility.DockerImageName;
import ru.mai.encryption_algorithm.impl.DES;
import ru.mai.encryption_context.EncryptionContext;
import ru.mai.encryption_context.SymmetricEncryptionContextImpl;
import ru.mai.encryption_mode.EncryptionModeEnum;
import ru.mai.encryption_padding_mode.PaddingModeEnum;
import ru.mai.kafka.KafkaContext;
import ru.mai.kafka.KafkaReader;
import ru.mai.kafka.KafkaWriter;
import ru.mai.kafka.impl.KafkaContextImpl;
import ru.mai.kafka.impl.KafkaReaderImpl;
import ru.mai.kafka.impl.KafkaWriterImpl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Stream;

@Slf4j
@Testcontainers
class TestKafkaEncryptionDecryption {
    @Container
    private final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka"));
    ExecutorService executorForTest = Executors.newFixedThreadPool(2);
    private Admin adminClient;
    private final static String TEST_TOPIC_IN = "test_topic_in";
    private final static String TEST_TOPIC_OUT = "test_topic_out";
    private final short replicaFactor = 1;
    private final int partitions = 3;
//    private final Config config = ConfigFactory.load();

    private KafkaContext kafkaContext;
    EncryptionContext context = new SymmetricEncryptionContextImpl(EncryptionModeEnum.ECB, PaddingModeEnum.ZEROES,
            new DES("abcdefg".getBytes(StandardCharsets.UTF_8)));

    public static final byte[] testByteArr = ("""
            The cat (Felis catus), commonly referred to as the domestic cat or house cat, is a small domesticated carnivorous
            mammal. It is the only domesticed species in the family Felidae. Recent advances in archaeology and genetics have shown
            that the domestication of the cat occurred in the Near East around 7500 BC. It is commonly kept as a house pet and farm
            cat, but also ranges freely as a feral cat avoiding human contact. It is valued by humans for companionship and its
            ability to kill vermin. Its retractable claws are adapted to killing small prey like mice and rats. It has a strong,
            flexible body, quick reflexes, sharp teeth, and its night vision and sense of smell are well developed. It is a social
            species, but a solitary hunter and a crepuscular predator. Cat communication includes vocalizations like meowing,
            purring, trilling, hissing, growling, and grunting as well as cat body language. It can hear sounds too faint or too
            high in frequency for human ears, such as those made by small mammals. It also secretes and perceives pheromones.""").getBytes(StandardCharsets.UTF_8);


    KafkaReader createKafkaReader() {
        return new KafkaReaderImpl(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test_group_consumer",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"),
                "test_topic_in",
                context);
    }

    KafkaWriter createKafkaWriter() {
        return new KafkaWriterImpl(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ProducerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString()
//                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092"
        ),
                "test_topic_in",
                context);
    }

    @BeforeEach
    void initTestEnvironment() {
        adminClient = createAdminClient();
        List<NewTopic> topics = Stream.of(TEST_TOPIC_IN)
                .map(topicName -> new NewTopic(topicName, partitions, replicaFactor))
                .toList();

        log.info("Topics: {}, replica factor {}, partitions {}", topics, replicaFactor, partitions);

        checkAndCreateRequiredTopics(adminClient, topics);

        kafkaContext = new KafkaContextImpl(
                Map.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                        ConsumerConfig.GROUP_ID_CONFIG, "test_group_consumer_01_17_05",
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"),
//                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest"),
                Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                        ProducerConfig.MAX_REQUEST_SIZE_CONFIG, "")
//                        ProducerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString())
                ,
                TEST_TOPIC_IN,
                TEST_TOPIC_IN,
                context);
    }

    @AfterEach
    void cleanEnvironment() throws Exception {
        kafkaContext.close();
    }

    private AdminClient createAdminClient() {
        log.info("Create admin client");
        return AdminClient.create(ImmutableMap.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()));
    }

    private void checkAndCreateRequiredTopics(Admin adminClient, List<NewTopic> topics) {
        log.info("Check required topics");
        try {
            Set<String> existingTopics = adminClient.listTopics().names().get();
            if (existingTopics.isEmpty()) {
                log.info("Topic not exist. Create topics {}", topics);
                adminClient.createTopics(topics).all().get(30, TimeUnit.SECONDS);
            } else {
                topics.stream().map(NewTopic::name).filter(t -> !existingTopics.contains(t)).forEach(t -> {
                    try {
                        log.info("Topic not exist {}. Create topic {}", t, t);
                        adminClient.createTopics(List.of(new NewTopic(t, partitions, replicaFactor))).all().get(30, TimeUnit.SECONDS);
//                        adminClient.createTopics(List.of(new NewTopic(t, partitions, replicaFactor).configs(Map.of(TopicConfig.RETENTION_MS_CONFIG, "200000")))).all().get(30, TimeUnit.SECONDS);
//                        adminClient.createTopics(List.of(new NewTopic(t, partitions, replicaFactor).configs(Map.of("max.message.bytes", "9000")))).all().get(30, TimeUnit.SECONDS);
                    } catch (InterruptedException | TimeoutException | ExecutionException e) {
                        log.error("Error creating topic Kafka", e);
                    }
                });
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("Error checking topics", e);
        }
    }


    /**
     * Проверяет готовность Kafka
     */
    @Test
    void testStartKafka() {
        Assertions.assertTrue(kafka.isRunning());
    }

    /**
     * Проверяет возможность читать и писать из Kafka
     */
    @Test
    void testKafkaWriteReadByteArray() throws InterruptedException {
        log.info("Bootstrap.servers: {}", kafka.getBootstrapServers());

        kafkaContext.send(testByteArr);
        log.info("Sent message");

        log.info("Consumer start reading");
        executorForTest.submit(() -> {
            try {
                kafkaContext.startListening();
            } catch (IOException e) {
                log.error("I/O exception happened, ", e);
                throw new RuntimeException(e);
            }
        });
        Thread.sleep(10000);
        //            getConsumerRecordsOutputTopic(kafkaReader, 10, 1);
    }

    @Test
    void testKafkaWriteReadFile() throws InterruptedException {
        log.info("Bootstrap.servers: {}", kafka.getBootstrapServers());

        log.info("Consumer start reading");

        try {
            executorForTest.submit(() -> {
                try {
                    kafkaContext.startListening();
                } catch (IOException e) {
                    log.error("I/O exception happened, ", e);
                    throw new RuntimeException(e);
                }
            });

            Path inputFile = Paths.get("src/test/resources/veryBigCat.txt");
            kafkaContext.send(inputFile);
        }
        catch (Exception e) {
            log.error("EXCEPTION: ", e);
        }
//        Thread.sleep(60000);
        Thread.sleep(10000);
    }

}