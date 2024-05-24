package ru.mai.client.kafka.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KafkaMessage {
    private UUID messageId; // generated
    private String fileName; // if is a file
    private Integer numberOfPartitions;
    private Integer currIndex;
    private byte[] value; // encrypted data
}
