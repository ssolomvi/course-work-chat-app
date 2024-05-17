package ru.mai.serialization;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;
import ru.mai.model.KafkaMessage;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class KafkaMessageDeserializer implements Deserializer<KafkaMessage> {
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        Deserializer.super.configure(configs, isKey);
    }

    @Override
    public KafkaMessage deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {
            String messageId = dis.readUTF();
            Integer numberOfPartitions = dis.readInt();
            if (numberOfPartitions == -1) {
                numberOfPartitions = 0;
            }
            String fileName = dis.readUTF();
            if (fileName.isEmpty()) {
                fileName = "";
            }
            Integer currIndex = dis.readInt();
            if (currIndex == -1) {
                currIndex = 0;
            }
            byte[] value = new byte[dis.available()];
            dis.readFully(value);
            return new KafkaMessage(UUID.fromString(messageId), fileName, numberOfPartitions, currIndex, value);
        } catch (IOException e) {
            throw new SerializationException("Error deserializing KafkaMessage", e);
        }
        // Ignore close exception
    }

    /*
    @Override
    public KafkaMessage deserialize(String topic, byte[] data) {
        KafkaMessage message;

        try {
             message = mapper.readValue(new String(data), KafkaMessage.class);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Error deserializing KafkaMessage object: " + e.getMessage());
        }
        return message;
    }
     */

    @Override
    public KafkaMessage deserialize(String topic, Headers headers, byte[] data) {
        return Deserializer.super.deserialize(topic, headers, data);
    }

    @Override
    public void close() {
        Deserializer.super.close();
    }
}
