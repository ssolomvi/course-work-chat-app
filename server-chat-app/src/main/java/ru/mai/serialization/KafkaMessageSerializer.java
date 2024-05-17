package ru.mai.serialization;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;
import ru.mai.model.KafkaMessage;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

@Slf4j
public class KafkaMessageSerializer implements Serializer<KafkaMessage> {
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        Serializer.super.configure(configs, isKey);
    }

    @Override
    public byte[] serialize(String topic, KafkaMessage data) {
        if (data == null) {
            return new byte[0];
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (DataOutputStream dos = new DataOutputStream(outputStream)) {
            dos.writeUTF(data.getMessageId().toString());
            dos.writeInt(data.getNumberOfPartitions() != null ? data.getNumberOfPartitions() : -1);
            dos.writeUTF(data.getFileName() != null ? data.getFileName() : "");
            dos.writeInt(data.getCurrIndex() != null ? data.getCurrIndex() : -1);
            dos.write(data.getValue());
        } catch (IOException e) {
            throw new SerializationException("Error serializing KafkaMessage", e);
        }
        // Ignore close exception
        return outputStream.toByteArray();
    }

    /*
    @Override
    public byte[] serialize(String topic, KafkaMessage data) {
        byte[] returnValue;

        try {
            returnValue = mapper.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Error serializing KafkaMessage object: " + e.getMessage());
        }
        return returnValue;
    }
    */

    @Override
    public byte[] serialize(String topic, Headers headers, KafkaMessage data) {
        return Serializer.super.serialize(topic, headers, data);
    }

    @Override
    public void close() {
        Serializer.super.close();
    }
}

