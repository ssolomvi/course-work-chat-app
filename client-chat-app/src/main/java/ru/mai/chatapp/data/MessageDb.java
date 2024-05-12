package ru.mai.chatapp.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "messages")
@Getter
@Setter
public class MessageDb {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "companion")
    private String companion;

    @Column(name = "type")
    private String type;

    @Column(name = "msg")
    private String msg;
}
