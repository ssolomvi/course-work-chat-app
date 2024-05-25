package ru.mai.kafka.serialization;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;
import ru.mai.kafka.model.MessageDto;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

@Slf4j
public class MessageDtoSerializer implements Serializer<MessageDto> {
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        Serializer.super.configure(configs, isKey);
    }

    @Override
    public byte[] serialize(String topic, MessageDto data) {
        if (data == null) {
            return new byte[0];
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (DataOutputStream dos = new DataOutputStream(outputStream)) {
            dos.writeUTF(data.getMessageId().toString());
            dos.writeUTF(data.getSender());
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

    @Override
    public byte[] serialize(String topic, Headers headers, MessageDto data) {
        return Serializer.super.serialize(topic, headers, data);
    }

    @Override
    public void close() {
        Serializer.super.close();
    }
}

