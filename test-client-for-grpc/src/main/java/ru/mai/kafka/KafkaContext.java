package ru.mai.kafka;

import java.io.IOException;
import java.nio.file.Path;

public interface KafkaContext extends AutoCloseable {
    void send(byte[] msg);
    void send(Path msg) throws IOException;

    void startListening() throws IOException;
}
