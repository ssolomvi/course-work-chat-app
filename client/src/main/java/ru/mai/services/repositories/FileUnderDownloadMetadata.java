package ru.mai.services.repositories;

import lombok.Getter;

@Getter
public class FileUnderDownloadMetadata {
    private final String filename;
    private final String tmpFilename;
    private final String companion;
    private int partitionsLeft;

    public FileUnderDownloadMetadata(String filename, String tmpFilename, String companion, int partitionsCount) {
        this.filename = filename;
        this.tmpFilename = tmpFilename;
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
