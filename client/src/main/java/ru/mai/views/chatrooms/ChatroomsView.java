package ru.mai.views.chatrooms;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.FileRejectedEvent;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileBuffer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import ru.mai.kafka.model.MessageDto;
import ru.mai.services.ChatClientService;
import ru.mai.utils.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

import static ru.mai.config.ClientConstants.FILE_PAGE_SIZE;

@Slf4j
@PageTitle("Chat-rooms")
@Route(value = "chat")
public class ChatroomsView extends HorizontalLayout implements HasUrlParameter<String> {
    private final HashMap<String, ChatTab> companionsChatTab = new HashMap<>();
    private final ScheduledExecutorService scheduledForPingingServer = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService scheduledForPingingConsumer = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService executorForFileDownloading = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2);
    private final Aside aside;
    private final ChatClientService chatClientService; // for server communication
    private final ChatRepository chatRepository; // for depicting objects for chat
    private final FilesToSendRepository filesToSendRepository;
    private MessagesLayoutScrollerWrapper wrapper;
    private String login;
    private ChatInfo currentChat;
    private Tabs tabs;

    @Override
    public void setParameter(BeforeEvent event, @WildcardParameter String parameter) {
        Notification.show(String.format("Hello, %s!", parameter), 3000, Notification.Position.BOTTOM_END);
        this.login = parameter;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        Page page = attachEvent.getUI().getPage();
        page.retrieveExtendedClientDetails(details -> setMobile(details.getWindowInnerWidth() < 740));
        page.addBrowserWindowResizeListener(e -> setMobile(e.getWidth() < 740));

        getUI().ifPresent(ui -> {
            ((HorizontalLayout) aside.getComponentAt(0)).add(new Icon("vaadin", "user-heart"));
            ((HorizontalLayout) aside.getComponentAt(0)).add(login);

            chatClientService.setLogin(login);
            chatClientService.connect();
            pingServer(login);
            pingConsumer();
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        log.debug("onDetach");
        chatClientService.disconnect();
        scheduledForPingingServer.shutdownNow();
        scheduledForPingingConsumer.shutdownNow();
    }

    @Getter
    public static class ChatTab extends Tab {
        private final ChatInfo chatInfo;

        public ChatTab(ChatInfo chatInfo) {
            this.chatInfo = chatInfo;
        }

    }

    @Getter
    public static class ChatInfo {
        private final String companion;

        private ChatInfo(String name) {
            this.companion = name;
        }
    }

    public class MessagesLayoutScrollerWrapper {
        private final Scroller messagesLayout;

        public enum Destination {
            OWN,
            ANOTHER
        }

        public MessagesLayoutScrollerWrapper(Scroller messagesLayout) {
            this.messagesLayout = messagesLayout;
            this.messagesLayout.setContent(new VerticalLayout());
        }

        public void showTextMessage(String textMessage, String companion, Destination destination) {
            Optional<UI> uiOptional = getUI();

            if (uiOptional.isPresent()) {
                UI ui = uiOptional.get();

                ui.access(() -> {
                    Div messageDiv = new Div();
                    messageDiv.setText(textMessage);

                    if (destination.equals(Destination.OWN)) {
                        messageDiv.getStyle()
                                .set("margin-left", "auto");
                    } else {
                        messageDiv.getStyle()
                                .set("margin-right", "auto");
                    }

                    messageDiv.getStyle()
                            .set("border-radius", "5px")
                            .set("padding", "5px")
                            .set("border", "1px solid #2D3D52");

                    if (currentChat.getCompanion().equals(companion)) {
                        VerticalLayout updated = ((VerticalLayout) messagesLayout.getContent());
                        updated.add(messageDiv);

                        messagesLayout.setContent(updated);
                    }

                    chatRepository.putMessage(companion, messageDiv);
                });
            }
        }

        public void showImageMessage(String fileName, InputStream stream, String companion, Destination destination) {
            getUI().ifPresent(ui -> ui.access(() -> {
                Div imageDiv = new Div();

                Image image;

                try {
                    StreamResource resource = new StreamResource(fileName, () -> stream);
                    image = new Image(resource, "Uploaded image");
                } catch (Exception e) {
                    log.debug("EXCEPTION: ", e);
                    return;
                }


                imageDiv.add(image);

                if (destination.equals(Destination.OWN)) {
                    imageDiv.getStyle()
                            .set("margin-left", "auto");
                } else {
                    imageDiv.getStyle()
                            .set("margin-right", "auto");
                }

                imageDiv.getStyle()
                        .set("overflow", "hidden")
                        .set("padding", "5px")
                        .set("border-radius", "5px")
                        .set("border", "1px solid #2D3D52")
                        .set("width", "40%")
                        .set("flex-shrink", "0");

                image.getStyle()
                        .set("width", "100%")
                        .set("height", "100%");

                if (currentChat.getCompanion().equals(companion)) {
                    VerticalLayout updated = ((VerticalLayout) messagesLayout.getContent());
                    updated.add(imageDiv);

                    messagesLayout.setContent(updated);
                }

                chatRepository.putMessage(companion, imageDiv);
            }));
        }

        public void showFileMessage(String fileName, InputStream stream, String companion, Destination destination) {
            Optional<UI> uiOptional = getUI();

            if (uiOptional.isPresent()) {
                UI ui = uiOptional.get();

                ui.access(() -> {
                    Div fileDiv = new Div();
                    StreamResource resource = new StreamResource(fileName, () -> stream);

                    Anchor downloadLink = new Anchor(resource, "");
                    downloadLink.getElement().setAttribute("download", true);

                    Button downloadButton = new Button(fileName, event -> downloadLink.getElement().callJsFunction("click"));

                    fileDiv.add(downloadButton, downloadLink);

                    if (destination.equals(Destination.OWN)) {
                        fileDiv.getStyle()
                                .set("margin-left", "auto");
                    } else {
                        fileDiv.getStyle()
                                .set("margin-right", "auto");
                    }

                    fileDiv.getStyle()
                            .set("display", "inline-block")
                            .set("max-width", "80%")
                            .set("overflow", "hidden")
                            .set("padding", "5px")
                            .set("border-radius", "5px")
                            .set("border", "1px solid #2D3D52")
                            .set("flex-shrink", "0");

                    if (currentChat.getCompanion().equals(companion)) {
                        VerticalLayout updated = ((VerticalLayout) messagesLayout.getContent());
                        updated.add(fileDiv);

                        messagesLayout.setContent(updated);
                    }

                    chatRepository.putMessage(companion, fileDiv);
                });
            }
        }

        private void clearMessages() {
            getUI().ifPresent(ui -> ui.access(
                    () -> messagesLayout.setContent(new VerticalLayout())
            ));
        }

        private void showMessages(String companion) {
            var messages = chatRepository.getAllMessages(companion);

            wrapper.clearMessages();
            if (messages != null && !messages.isEmpty()) {
                getUI().ifPresent(ui -> ui.access(() -> {
                    VerticalLayout layout = new VerticalLayout();
                    for (var message : messages) {
                        layout.add(message);
                    }
                    messagesLayout.setContent(layout);
                }));
            }
        }
    }

    public ChatroomsView(@Autowired ChatClientService chatClientService,
                         @Autowired ChatRepository chatRepository,
                         @Autowired FilesToSendRepository filesToSendRepository) {
        this.chatClientService = chatClientService;
        this.chatRepository = chatRepository;
        this.filesToSendRepository = filesToSendRepository;

        addClassNames("chat-view", LumoUtility.Width.FULL, LumoUtility.Display.FLEX, LumoUtility.Flex.AUTO);
        setSpacing(false);

        // Lay-outing
        aside = createAside();
        add(aside, createChatContainer());

        setSizeFull();
    }


    private VerticalLayout createChatContainer() {
        VerticalLayout chatLayout = new VerticalLayout();
        chatLayout.addClassNames(LumoUtility.Flex.AUTO, LumoUtility.Width.AUTO, LumoUtility.Height.FULL, LumoUtility.Overflow.HIDDEN);

        VerticalLayout messageListLayout = new VerticalLayout();
        Scroller scroller = new Scroller(messageListLayout);
        scroller.addClassNames(LumoUtility.Width.AUTO);
        scroller.setSizeFull();

        this.wrapper = new MessagesLayoutScrollerWrapper(scroller);

        var msgInputBtnAddFileContainer = createMessageContainer();

        chatLayout.add(scroller, msgInputBtnAddFileContainer);

        return chatLayout;
    }

    private HorizontalLayout createMessageContainer() {
        HorizontalLayout messageLayout = new HorizontalLayout();
        messageLayout.addClassNames(LumoUtility.Flex.AUTO, LumoUtility.FlexDirection.ROW, LumoUtility.AlignItems.START, LumoUtility.JustifyContent.BETWEEN, Padding.SMALL);

        Upload dropDisabledSingleFileUpload = createUpload();

        TextField messageInput = new TextField();
        messageInput.setPlaceholder("Enter message");
        messageInput.setWidth("900px");

        Button sendMessageBtn = new Button("Send", event -> {
            if (currentChat == null || currentChat.getCompanion() == null) {
                Notification.show("No chat selected", 5000, Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }

            // нажали на кнопку отправить
            sendMessage(dropDisabledSingleFileUpload);

            String message = messageInput.getValue();
            if (!message.isEmpty()) {
                if (message.length() > FILE_PAGE_SIZE) {
                    Notification.show(String.format("Message is too long, it must not be more than %d bytes.", FILE_PAGE_SIZE)).addThemeVariants(NotificationVariant.LUMO_WARNING);
                    return;
                }
                chatClientService.sendMessage(currentChat.getCompanion(), message);
                wrapper.showTextMessage(message, currentChat.getCompanion(), MessagesLayoutScrollerWrapper.Destination.OWN);
                messageInput.clear();
            }
        });

        messageLayout.add(dropDisabledSingleFileUpload, messageInput, sendMessageBtn);
        return messageLayout;
    }

    private Aside createAside() {
        Aside side = new Aside();
        side.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Flex.GROW_NONE,
                LumoUtility.Flex.SHRINK_NONE, LumoUtility.Background.CONTRAST_5, LumoUtility.Margin.SMALL);
        side.setWidth("18rem");

        var loginHeaderContainer = new HorizontalLayout();
        loginHeaderContainer.addClassNames(LumoUtility.JustifyContent.CENTER, LumoUtility.AlignItems.START, LumoUtility.Margin.SMALL);

        tabs = createTabs();
        tabs.addSelectedChangeListener(event -> {
            if (tabs.getComponentCount() != 0) {
                currentChat = ((ChatTab) event.getSelectedTab()).getChatInfo();
                wrapper.showMessages(currentChat.companion);
            } else {
                currentChat = null;
            }
        });

        Dialog dialogAddChatRoom = createDialog();
        Button buttonAddChatRoom = createButtonAddChatRoom(dialogAddChatRoom);
        side.add(loginHeaderContainer, tabs, buttonAddChatRoom);

        return side;
    }

    private Tabs createTabs() {
        tabs = new Tabs();
        tabs.addClassNames(LumoUtility.AlignItems.END);
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        tabs.setWidthFull();
        tabs.addClassNames(LumoUtility.Flex.GROW, LumoUtility.Flex.SHRINK, LumoUtility.Overflow.HIDDEN);
        return tabs;
    }

    private Upload createUpload() {
        var memoryBuffer = new MultiFileBuffer();
        Upload upload = new Upload(memoryBuffer);
        upload.setMaxFileSize(1024 * 1024 * 1024); // gigabyte

        upload.setDropAllowed(false);

        upload.addFileRejectedListener(event -> log.debug("File rejected: " + event.getErrorMessage()));

        upload.addSucceededListener(event -> {
            if (currentChat == null) {
                String detailError = "No chat selected";
                Notification.show(detailError, 5000, Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.LUMO_WARNING);
                fireEvent(new FileRejectedEvent(upload, detailError));
                return;
            }
            // Get information about the uploaded file
            String fileName = event.getFileName();

            filesToSendRepository.put(currentChat.getCompanion(), fileName, memoryBuffer.getInputStream(fileName), memoryBuffer.getInputStream(fileName), event.getContentLength());
        });

        return upload;
    }

    private Dialog createDialog() {
        Dialog dialog = new Dialog();
        dialog.getElement().setAttribute("aria-label", "Add chat room");

        VerticalLayout dialogLayout = createDialogLayout(dialog);
        dialog.add(dialogLayout);
        dialog.setHeaderTitle("Add chat room");

        Button closeButton = new Button(new Icon("lumo", "cross"), e -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        dialog.getHeader().add(closeButton);

        return dialog;
    }

    private VerticalLayout createDialogLayout(Dialog dialog) {
        TextField loginField = new TextField("Companion's login", "",
                "Companion's login");
        loginField.getStyle().set("padding-top", "0");

        Select<String> selectEncryptionMode = new Select<>();
        selectEncryptionMode.setLabel("Encryption mode:");
        selectEncryptionMode.setItems("ECB", "CBC", "PCBC", "OFB", "CFB", "CTR", "RANDOM_DELTA");
        selectEncryptionMode.setValue("ECB");

        Select<String> selectPaddingMode = new Select<>();
        selectPaddingMode.setLabel("Padding mode:");
        selectPaddingMode.setItems("Zeroes", "ANSI_X_923", "ISO10126", "PKCS7");
        selectPaddingMode.setValue("Zeroes");

        Select<String> selectAlgorithm = new Select<>();
        selectAlgorithm.setLabel("Algorithm:");
        selectAlgorithm.setItems("LOKI97", "MARS", "RC6", "DES", "DEAL", "Rijndael");
        selectAlgorithm.setValue("LOKI97");

        Button submit = new Button("Submit", e -> {
            String companion = loginField.getValue();
            if (companion.equals(login)) {
                Notification.show("You cannot create chat room with yourself, sorry", 5000, Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.LUMO_WARNING);
                loginField.clear();
                return;
            }

            if (companion.isEmpty()) {
                Notification.show("Companion's login field is empty!", 3000, Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            if (chatClientService.addRoom(companion, selectAlgorithm.getValue(), selectEncryptionMode.getValue(), selectPaddingMode.getValue())) {
                Notification.show(String.format("Chat room with %s was initiated!", companion), 5000, Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                loginField.clear();
                dialog.close();
            } else {
                Notification.show(String.format("Chat room has not been created, 'cause %s is offline or already sent u a request", companion), 10000, Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.LUMO_WARNING);
                loginField.clear();
            }
        });
        submit.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        VerticalLayout fieldLayout = new VerticalLayout(loginField, selectEncryptionMode, selectPaddingMode, selectAlgorithm, submit);
        fieldLayout.setSpacing(false);
        fieldLayout.setPadding(false);
        fieldLayout.setAlignItems(Alignment.STRETCH);
        fieldLayout.getStyle().set("width", "300px").set("max-width", "100%");

        return fieldLayout;
    }

    private Button createButtonAddChatRoom(Dialog dialogAddChatRoom) {
        Button btn = new Button("Add chat room", e -> dialogAddChatRoom.open());
        btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btn.setWidthFull();

        return btn;
    }

    private void pingServer(String user) {
        log.debug("{}: start pinging server", user);
        scheduledForPingingServer.scheduleAtFixedRate(
                () -> {
                    checkForDisconnectedCompanions();

                    chatClientService.checkForInitRoomRequests();

                    checkForDeleteRoomRequest();

                    checkForDiffieHellmanNumbers();
                },
                0, 3, TimeUnit.SECONDS
        );
    }

    private void pingConsumer() {
        scheduledForPingingConsumer.scheduleAtFixedRate(() -> {
                    if (tabs.getComponentCount() != 0) {
                        checkForMessages();
                    }
                },
                0, 1, TimeUnit.SECONDS);
    }

    private void checkForDisconnectedCompanions() {
        var disconnectedCompanions = chatClientService.checkForDisconnected();

        if (disconnectedCompanions.isEmpty()) {
            return;
        }

        for (var disconnectedCompanion : disconnectedCompanions) {
            removeTab(disconnectedCompanion);
            log.debug("Deleting chat tab due to disconnected companion {}", disconnectedCompanion);
        }
    }

    private void checkForDeleteRoomRequest() {
        var response = chatClientService.checkForDeleteRoomRequest();

        if (response.isEmpty()) {
            return;
        }

        // remove all corresponding tabs
        for (String deletedCompanion : response) {
            removeTab(deletedCompanion);
            log.debug("Deleting chat tab due to deleted companion {}", deletedCompanion);
        }
    }

    private void checkForDiffieHellmanNumbers() {
        if (chatClientService.getCheckForDiffieHellmanNumbers() != 0) {
            var newCompanions = chatClientService.checkForDiffieHellmanNumbers();
            var newChatTabs = newCompanions.stream().map(this::createNewChatTab).toList();

            for (var newChatTab : newChatTabs) {
                getUI().ifPresent(ui -> ui.access(() -> {
                    tabs.add(newChatTab);
                    if (tabs.getComponentCount() == 1) {
                        currentChat = newChatTab.getChatInfo();
                        tabs.setSelectedTab(newChatTab);
                    }
                }));
                companionsChatTab.put(newChatTab.getChatInfo().getCompanion(), newChatTab);
                log.debug("Added chat tab with companion: " + newChatTab.getChatInfo().getCompanion());
            }
        }
    }

    private ChatTab createNewChatTab(String companion) {
        var newChatTab = new ChatTab(new ChatInfo(companion));
        newChatTab.addClassNames(LumoUtility.JustifyContent.BETWEEN);
        newChatTab.add(new Span("# " + newChatTab.getChatInfo().getCompanion()));

        Button closeTabButton = new Button(new Icon("lumo", "cross"), e -> {
            removeTab(companion);
            log.debug("Deleting chat tab due to pressing on btn close");
        });

        closeTabButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        newChatTab.add(closeTabButton);

        return newChatTab;
    }

    private void removeTab(String companion) {
        getUI().ifPresent(ui -> ui.access(() ->
        {
            ChatTab chatTab = companionsChatTab.remove(companion);
            if (chatTab == null) {
                return;
            }
            tabs.remove(chatTab);
        }));

        deleteRoom(companion);
        wrapper.clearMessages();
        chatRepository.removeChat(companion);
        filesToSendRepository.removeByCompanion(companion);

        if (tabs.getComponentCount() != 0) {

            getUI().ifPresent(ui -> ui.access(() -> {
                tabs.setSelectedIndex(0);
                ChatTab selected = (ChatTab) (tabs.getSelectedTab());
                currentChat = selected.getChatInfo();
                wrapper.showMessages(currentChat.getCompanion());
            }));
        }
    }

    private void deleteRoom(String companion) {
        if (chatClientService.deleteRoom(companion)) {
            Notification.show(String.format("Chat room with %s deleted successfully", companion), 5000, Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } else {
            Notification.show(String.format("Companion %s already requested to delete room", companion), 5000, Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.LUMO_WARNING);
        }
    }

    private void checkForMessages() {
        var response = chatClientService.checkForMessages();

        if (response.isEmpty()) {
            return;
        }
        log.debug("Got {} records", response.size());

        List<MessageDto> textMessages = response.stream().filter(messageDto -> messageDto.getFilename().isEmpty()).toList();
        List<MessageDto> fileMessages = response.stream().filter(messageDto -> !messageDto.getFilename().isEmpty()).toList();

        List<Future<Optional<Pair<String, Pair<String, InputStream>>>>> fileMessagesOpFutures = new LinkedList<>();
        for (MessageDto msg : fileMessages) {
            fileMessagesOpFutures.add(
                    executorForFileDownloading.submit(() -> chatClientService.processFileMessage(msg))
            );
        }

        try {
            for (var fileMessageOpFuture : fileMessagesOpFutures) {
                var fileMessageOp = fileMessageOpFuture.get();

                if (fileMessageOp.isPresent()) {
                    String sender = fileMessageOp.get().getKey();
                    String fileName = fileMessageOp.get().getValue().getKey();
                    InputStream inputStream = fileMessageOp.get().getValue().getValue();

                    if (fileName.endsWith(".jpg") || fileName.endsWith(".png") || fileName.endsWith(".jpeg")) {
                        wrapper.showImageMessage(fileName, inputStream, sender, MessagesLayoutScrollerWrapper.Destination.ANOTHER);
                    } else {
                        wrapper.showFileMessage(fileName, inputStream, sender, MessagesLayoutScrollerWrapper.Destination.ANOTHER);
                    }
                }
            }
        } catch (ExecutionException e) {
            Notification.show("the computation threw an exception trying to get the result", 5000, Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (InterruptedException e) {
            Notification.show("the computation was interrupted trying to get the result", 5000, Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }

        for (var textMessage : textMessages) {
            var textMsgOp = chatClientService.processByteArrayMessage(textMessage);

            textMsgOp.ifPresent(s -> wrapper.showTextMessage(s, textMessage.getSender(), MessagesLayoutScrollerWrapper.Destination.ANOTHER));
        }
    }

    private void sendMessage(Upload upload) {
        try {
            upload.clearFileList();
            String companion = currentChat.getCompanion();
            var metadatas = filesToSendRepository.getFilesToSend(companion);
            if (metadatas == null || metadatas.isEmpty()) {
                return;
            }
            filesToSendRepository.removeByCompanion(companion);

            for (var metadata : metadatas) {
                String fileName = metadata.getFilename();
                InputStream streamForSending = metadata.getStreamForSending();
                InputStream streamForDepicting = metadata.getStreamForDepicting();
                long size = metadata.getSize();

                chatClientService.sendFile(companion, fileName, streamForSending, size);

                if (fileName.endsWith(".jpg") || fileName.endsWith(".png") || fileName.endsWith(".jpeg")) {
                    wrapper.showImageMessage(fileName, streamForDepicting, companion, MessagesLayoutScrollerWrapper.Destination.OWN);
                } else {
                    wrapper.showFileMessage(fileName, streamForDepicting, companion, MessagesLayoutScrollerWrapper.Destination.OWN);
                }
            }
        } catch (IOException | RuntimeException e) {
            // runtime exception is thrown if encryption context not found
            log.error("I/O exception happened trying to send file, ", e);
        } catch (InterruptedException e) {
            log.error("Interrupted exception happened trying to send file", e);
        }
    }

    private void setMobile(boolean mobile) {
        tabs.setOrientation(mobile ? Tabs.Orientation.HORIZONTAL : Tabs.Orientation.VERTICAL);
    }
}
