package ru.mai.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.upload.SucceededEvent;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.FileBuffer;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;


@Route("upload")
@Slf4j
public class LargeFileUploadView extends Div {

    private static final String UPLOAD_DIRECTORY = "uploads";
    private ByteArrayOutputStream outputStream;
    private Upload upload;

    public LargeFileUploadView() {
        // Create an Upload component
        var buffer = new FileBuffer();
/*
        // create the upload component and delegate actions to the receiveUpload method
        upload = new Upload(this::receiveUpload);
        upload.getStyle().set("flex-grow", "1");

        // listen to state changes
        upload.addSucceededListener(e -> uploadSuccess(e));

        upload.addFailedListener(e -> setFailed(e.getReason().getMessage()));
        upload.addFileRejectedListener(e -> setFailed(e.getErrorMessage()));

        // only allow images to be uploaded

        // only allow single file at a time
        upload.setMaxFiles(1);


*/
        upload = new Upload(buffer);
        // set max file size to 1 GB
        upload.setMaxFileSize(1024 * 1024 * 1024);

        // Add a button to start the upload
        Button uploadButton = new Button("Upload File");
        uploadButton.addClickListener(event -> {
            InputStream inputStream = buffer.getInputStream();
            try {
                // Get the file name
                String fileName = buffer.getFileName();

                // Save the file to the upload directory
                Path targetFile = Path.of(UPLOAD_DIRECTORY, fileName);
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);

                // Show a success notification
                Notification.show("File uploaded successfully!", 3000, Notification.Position.BOTTOM_CENTER);
            } catch (IOException e) {
                // Show an error notification
                Notification.show("Error uploading file: " + e.getMessage(), 3000, Notification.Position.BOTTOM_CENTER);
            }
        });
        // Create a layout for the components
        HorizontalLayout layout = new HorizontalLayout(upload);

        // Add the layout to the view
        add(layout);
    }

    /**
     * Called when a user initializes an upload.
     * <p>
     * We prepare the bean and a destination for the binary data; Vaadin will take
     * care of the actual network operations.
     */
//    private OutputStream receiveUpload(String fileName, String mimeType) {

        // clear old errors for better user experience
//        setInvalid(false);

        // create new value bean to store the data
//        log.debug("File name: {}", fileName);
//        log.debug("Mime type: {}", mimeType);

        // set up receiving Stream
//        outputStream = new ByteArrayOutputStream();
//        return outputStream;
//    }

    /**
     * Called when an upload is successfully completed.
     */
//    private void uploadSuccess(SucceededEvent e) {

        // store the binary data into our bean
//        value.setImage(outputStream.toByteArray());

        // fire value changes so that Binder can do its thing
//        setModelValue(value, true);

        // show the new image
//        updateImage();

        // clear the upload component 'finished files' list for a cleaner appearance.
        // there is yet no API for it on the server side, see
        // https://github.com/vaadin/vaadin-upload-flow/issues/96
//        upload.getElement().executeJs("this.files=[]");
//    }

    /**
     * Shows an error message to the user.
     */
//    private void setFailed(String message) {
//        setInvalid(true);
//        log.debug("Failed: {}", message);
//        setErrorMessage(message);
//    }
}