package ru.mai.services.repositories;

import lombok.Getter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class FileUnderDownloadMetadata {
    @Getter
    private final String sender;
    private final AtomicReference<String> tmpFilename = new AtomicReference<>();
    private final AtomicBoolean firstCall = new AtomicBoolean(true);
    private final AtomicInteger partsSent = new AtomicInteger(0);

    public FileUnderDownloadMetadata(String sender) {
        this.sender = sender;
    }

    public String getTmpFilename(String filename) {
        if (firstCall.compareAndSet(true, false)) {
            tmpFilename.set(String.format("%s%s", filename, System.currentTimeMillis()));
        }
        return tmpFilename.get();
    }

    public int incrementAndGetPartsGot() {
        return partsSent.incrementAndGet();
    }

}
