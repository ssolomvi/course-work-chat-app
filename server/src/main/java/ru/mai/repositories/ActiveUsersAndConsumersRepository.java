package ru.mai.repositories;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.stereotype.Repository;
import ru.mai.kafka.KafkaManager;
import ru.mai.kafka.model.MessageDto;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Repository
public class ActiveUsersAndConsumersRepository {
    /**
     * One consumer per one user
     */
    private Map<String, KafkaConsumer<String, MessageDto>> usersAndConsumers;
    public boolean isActive(String user) {
        if (usersAndConsumers == null) {
            return false;
        }

        return usersAndConsumers.containsKey(user);
    }

    public Optional<KafkaConsumer<String, MessageDto>> getConsumer(String user) {
        if (usersAndConsumers == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(usersAndConsumers.get(user));
    }

    public void putUser(String user) {
        if (usersAndConsumers == null) {
            usersAndConsumers = new ConcurrentHashMap<>();
        }

        if (usersAndConsumers.containsKey(user)) {
            log.debug("{} is already online", user);
            return;
        }

        String topicName = KafkaManager.getTopicName(user);

        KafkaManager.createTopic(topicName);

        usersAndConsumers.put(user, KafkaManager.createKafkaConsumer(topicName));
        log.debug("{} is online", user);
    }

    public void deleteUser(String user) {
        if (!usersAndConsumers.containsKey(user)) {
            return;
        }

        usersAndConsumers.get(user).close();

        String topicName = KafkaManager.getTopicName(user);
        KafkaManager.deleteTopic(topicName);

        usersAndConsumers.remove(user);
    }

}
