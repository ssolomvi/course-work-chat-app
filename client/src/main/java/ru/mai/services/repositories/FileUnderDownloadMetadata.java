package ru.mai.services.repositories;

import lombok.Getter;

public class FileUnderDownloadMetadata {
    @Getter
    private final String filename;
    @Getter
    private final String companion;
    private int partitionsLeft;

    public FileUnderDownloadMetadata(String filename, String companion, int partitionsCount) {
        this.filename = filename;
        this.companion = companion;
        this.partitionsLeft = partitionsCount;
    }

    public void decrementPartitionsLeft() {
        --partitionsLeft;
    }

    public boolean isFinished() {
        return partitionsLeft == 0;
    }
}
