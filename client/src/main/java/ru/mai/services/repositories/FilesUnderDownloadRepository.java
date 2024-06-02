package ru.mai.services.repositories;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class FilesUnderDownloadRepository {
    private final Map<UUID, FileUnderDownloadMetadata> filesUnderDownload = new ConcurrentHashMap<>();

    public void put(UUID id, String filename, String tmpFilename, String companion, int partitionsCount) {
        filesUnderDownload.put(id, new FileUnderDownloadMetadata(filename, tmpFilename, companion, partitionsCount));
    }

    public boolean contains(UUID id) {
        return filesUnderDownload.containsKey(id);
    }

    public void decrementPartitionsLeft(UUID id) {
        if (contains(id)) {
            filesUnderDownload.get(id).decrementPartitionsLeft();
        }
    }

    public boolean isFinished(UUID id) {
        if (!filesUnderDownload.containsKey(id)) {
            return false;
        }

        return filesUnderDownload.get(id).isFinished();
    }

    public void remove(UUID id) {
        filesUnderDownload.remove(id);
    }

    public void remove(String companion) {
        filesUnderDownload.values().removeIf(metadata -> metadata.getCompanion().equals(companion));
    }

    public Optional<String> getFilename(UUID id) {
        if (!filesUnderDownload.containsKey(id)) {
            return Optional.empty();
        }
        return Optional.of(filesUnderDownload.get(id).getFilename());
    }
    public Optional<String> getTmpFilename(UUID id) {
        if (!filesUnderDownload.containsKey(id)) {
            return Optional.empty();
        }
        return Optional.of(filesUnderDownload.get(id).getTmpFilename());
    }

    public Optional<String> getCompanion(UUID id) {
        if (!filesUnderDownload.containsKey(id)) {
            return Optional.empty();
        }
        return Optional.of(filesUnderDownload.get(id).getCompanion());
    }
}
