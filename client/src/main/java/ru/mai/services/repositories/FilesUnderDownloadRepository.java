package ru.mai.services.repositories;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class FilesUnderDownloadRepository {
    private final Map<UUID, FileUnderDownloadMetadata> filesUnderDownload = new ConcurrentHashMap<>();

    public String getTmpFilename(String sender, UUID messageId, String filename) {
        FileUnderDownloadMetadata metadata = filesUnderDownload.getOrDefault(messageId, new FileUnderDownloadMetadata(sender));
        filesUnderDownload.put(messageId, metadata);
        return metadata.getTmpFilename(filename);
    }

    public int incrementAndGetPartsSent(UUID messageId) {
        if (filesUnderDownload.containsKey(messageId)) {
            return filesUnderDownload.get(messageId).incrementAndGetPartsGot();
        }
        return 0;
    }

    public void remove(UUID id) {
        filesUnderDownload.remove(id);
    }

    public void removeBySender(String sender) {
        filesUnderDownload.values().removeIf(metadata -> metadata.getSender().equals(sender));
    }
}
