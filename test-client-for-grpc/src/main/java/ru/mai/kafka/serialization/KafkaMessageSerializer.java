package ru.mai.kafka.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;
import ru.mai.model.KafkaMessage;

import java.io.IOException;
import java.util.Map;

public class KafkaMessageSerializer implements Serializer<KafkaMessage> {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        Serializer.super.configure(configs, isKey);
    }

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

    @Override
    public byte[] serialize(String topic, Headers headers, KafkaMessage data) {
        return Serializer.super.serialize(topic, headers, data);
    }

    @Override
    public void close() {
        Serializer.super.close();
    }
}

