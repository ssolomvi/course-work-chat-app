package ru.mai.views.chatrooms;

import com.vaadin.flow.component.html.Input;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.io.InputStream;
import java.nio.file.Path;
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

    public void put(String companion, String filename, Path data) {
        List<FilesToSendMetadata> filesToSendMetadata;
        if (filesToSend.containsKey(companion)) {
            filesToSendMetadata = filesToSend.get(companion);
        } else {
            filesToSendMetadata = new LinkedList<>();
        }

        filesToSendMetadata.add(new FilesToSendMetadata(filename, data));
        filesToSend.put(companion, filesToSendMetadata);
    }

    public void removeForCompanion(String companion) {
        filesToSend.remove(companion);
    }

    @Getter
    public static class FilesToSendMetadata {
        private final String filename;
        private final Path data;

        public FilesToSendMetadata(String filename, Path data) {
            this.filename = filename;
            this.data = data;
        }
    }
}
