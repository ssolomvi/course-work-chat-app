package ru.mai.kafka.serialization;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;
import ru.mai.kafka.model.MessageDto;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class MessageDtoDeserializer implements Deserializer<MessageDto> {
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        Deserializer.super.configure(configs, isKey);
    }

    @Override
    public MessageDto deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {
            // UUID messageId
            String messageId = dis.readUTF();
            // String sender
            String sender = dis.readUTF();
            // Integer numberOfPartitions
            Integer numberOfPartitions = dis.readInt();
            if (numberOfPartitions == -1) {
                numberOfPartitions = 0;
            }
            // String fileName
            String fileName = dis.readUTF();
            if (fileName.isEmpty()) {
                fileName = "";
            }
            // Integer currIndex
            Integer currIndex = dis.readInt();
            if (currIndex == -1) {
                currIndex = 0;
            }
            // byte[] value
            byte[] value = new byte[dis.available()];
            dis.readFully(value);
            return new MessageDto(UUID.fromString(messageId), sender, fileName, numberOfPartitions, currIndex, value);
        } catch (IOException e) {
            throw new SerializationException("Error deserializing message", e);
        }
        // Ignore close exception
    }

    @Override
    public MessageDto deserialize(String topic, Headers headers, byte[] data) {
        return Deserializer.super.deserialize(topic, headers, data);
    }

    @Override
    public void close() {
        Deserializer.super.close();
    }
}
