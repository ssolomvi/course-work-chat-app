package ru.mai.views.chatrooms;

import lombok.Getter;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import ru.mai.Utils;
import ru.mai.compression.image.service.ImageCompressingService;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Scope("prototype")
@Log
public class FilesToSendRepository {

    private final ImageCompressingService imageCompressingService;

    private final Map<String, List<FilesToSendMetadata>> filesToSend = new ConcurrentHashMap<>();

    @Autowired
    public FilesToSendRepository(ImageCompressingService imageCompressingService) {
        this.imageCompressingService = imageCompressingService;
    }

    public List<FilesToSendMetadata> getFilesToSend(String companion) {
        if (filesToSend.containsKey(companion)) {
            return filesToSend.get(companion);
        }
        return new LinkedList<>();
    }

    public void put(String companion,
                    String filename,
                    InputStream streamForSending,
                    InputStream streamForDepicting,
                    long size) {
        List<FilesToSendMetadata> filesToSendMetadata = filesToSend.getOrDefault(companion, new LinkedList<>());

        if (Utils.SUPPORTED_IMAGE_FORMATS.contains(extension(filename))) {
            try {
                streamForSending = imageCompressingService.compress(
                        filename,
                        streamForSending,
                        ImageCompressingService.WEAK_COMPRESSION
                );
                streamForDepicting = imageCompressingService.compress(
                        filename,
                        streamForDepicting,
                        ImageCompressingService.WEAK_COMPRESSION
                );
            } catch (IOException e) {
                log.severe(String.format("Ex happened while trying to compress an image: %s", e.getMessage()));
            }
        }
        // todo: else compress with DEFLATE?
        //  or compress all files to send in a single archive before encrypting
        //  and decompress after decrypting
        filesToSendMetadata.add(new FilesToSendMetadata(filename, streamForSending, streamForDepicting, size));

        filesToSend.put(companion, filesToSendMetadata);
    }

    public void removeByCompanion(String companion) {
        filesToSend.remove(companion);
    }

    private String extension(String fileName) {
        String extension;
        var extensionIndex = fileName.lastIndexOf(".") + 1;
        if (fileName.length() > extensionIndex) {
            extension = fileName.substring(extensionIndex);
        } else {
            extension = "";
        }

        return extension;
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
