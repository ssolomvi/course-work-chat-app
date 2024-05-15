package ru.mai.kafka.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;
import ru.mai.model.KafkaMessage;

import java.io.IOException;
import java.util.Map;

public class KafkaMessageDeserializer implements Deserializer<KafkaMessage> {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        Deserializer.super.configure(configs, isKey);
    }

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

    @Override
    public KafkaMessage deserialize(String topic, Headers headers, byte[] data) {
        return Deserializer.super.deserialize(topic, headers, data);
    }

    @Override
    public void close() {
        Deserializer.super.close();
    }
}
