package ru.mai.model;

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
    private byte[] value; // encrypted data
    private Boolean isLast; // if send partially
    private String fileName; // if is a file
    private Integer index;
}
