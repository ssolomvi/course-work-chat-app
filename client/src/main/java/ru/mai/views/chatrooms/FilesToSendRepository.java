package ru.mai.views.chatrooms;

import lombok.Getter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Scope("prototype")
public class FilesToSendRepository {
    private final Map<String, List<FilesToSendMetadata>> filesToSend = new ConcurrentHashMap<>();

    public List<FilesToSendMetadata> getFilesToSend(String companion) {
        if (filesToSend.containsKey(companion)) {
            return filesToSend.get(companion);
        }
        return new LinkedList<>();
    }

    public void put(String companion, String filename, InputStream streamForSending, InputStream streamForDepicting, long size) {
        List<FilesToSendMetadata> filesToSendMetadata = filesToSend.getOrDefault(companion, new LinkedList<>());

        filesToSendMetadata.add(new FilesToSendMetadata(filename, streamForSending, streamForDepicting, size));

        filesToSend.put(companion, filesToSendMetadata);
    }

    public void removeByCompanion(String companion) {
        filesToSend.remove(companion);
    }

    @Getter
    public static class FilesToSendMetadata {
        private final String filename;
        private final InputStream streamForSending;
        private final InputStream streamForDepicting;
        private final long size;

        public FilesToSendMetadata(String filename, InputStream streamForSending, InputStream streamForDepicting, long size) {
            this.filename = filename;
            this.streamForSending = streamForSending;
            this.streamForDepicting = streamForDepicting;
            this.size = size;
        }
    }
}
