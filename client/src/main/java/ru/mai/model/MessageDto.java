package ru.mai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageDto {
    private UUID messageId; // generated
    private String sender;
    private String fileName; // if is a file
    private Integer numberOfPartitions;
    private Integer currIndex;
    private byte[] value; // encrypted data
}
