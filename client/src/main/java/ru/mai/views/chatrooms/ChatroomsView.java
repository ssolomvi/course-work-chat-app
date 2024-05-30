package ru.mai.views.chatrooms;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import ru.mai.services.ChatClientService;
import ru.mai.utils.Pair;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@PageTitle("Chat-rooms")
@Route(value = "chat")
public class ChatroomsView extends HorizontalLayout implements HasUrlParameter<String> {
    private final List<Pair<String, InputStream>> files = new LinkedList<>();
    private final HashMap<String, ChatTab> companionsChatTab = new HashMap<>();
    private final ScheduledExecutorService scheduled = Executors.newSingleThreadScheduledExecutor();
    private final ChatClientService chatClientService;
    private final ChatRepository chatRepository;
    private MessagesLayoutScrollerWrapper wrapper;
    private String login;
    private ChatInfo currentChat;
    private Tabs tabs;
    private static final int FILE_PAGE_SIZE = 65536;

    @Override
    public void setParameter(BeforeEvent event, @WildcardParameter String parameter) {
        Notification.show(String.format("Hello, %s!", parameter));
        this.login = parameter;
        chatClientService.setLogin(parameter);
        pingServer(parameter);
        chatClientService.connect();
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

    public ChatroomsView(@Autowired ChatClientService chatClientService,
                         @Autowired ChatRepository chatRepository) {
        this.chatClientService = chatClientService;
        this.chatRepository = chatRepository;

        addClassNames("chat-view", LumoUtility.Width.FULL, LumoUtility.Display.FLEX, LumoUtility.Flex.AUTO);
        setSpacing(false);

        // Lay-outing
        add(createAside(), createChatContainer());
        // TODO: MESSAGING!!!


        setSizeFull();
    }

    private void pingServer(String user) {
        log.debug("{}: start pinging server", user);
        scheduled.scheduleAtFixedRate(
                () -> {
                    checkForDisconnectedCompanions();

                    chatClientService.checkForInitRoomRequests();

                    checkForDeleteRoomRequest();

                    checkForDiffieHellmanNumbers();

                    if (tabs.getComponentCount() != 0) {
                        checkForMessages();
                    }
                },
                0, 5, TimeUnit.SECONDS
        );
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
                getUI().ifPresent(ui -> ui.access(() -> tabs.add(newChatTab)));
                currentChat = newChatTab.chatInfo;
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
                    int countOfChildrenTabs = tabs.getComponentCount();
                    if (countOfChildrenTabs != 0) {
                        tabs.setSelectedIndex(0);
                    }
                    deleteRoom(companion);
                    wrapper.clearMessages();
                    chatRepository.removeChat(companion);
                }
        ));
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

        for (var msg : response) {
            if (msg.getFileName().isEmpty()) {
                // it is a byte arr (not file)
                // if it is a text, print message to chat
                var textMsgOp = chatClientService.processByteArrayMessage(msg);

                textMsgOp.ifPresent(s -> wrapper.showTextMessage(s, msg.getSender(), MessagesLayoutScrollerWrapper.Destination.ANOTHER));
            } else {
                // if it is a file, print message to chat that file (filename) may be found at location (location)
                var fileMsgOp = chatClientService.processFileMessage(msg);

                if (fileMsgOp.isPresent()) {
                    try {
                        String fileName = fileMsgOp.get().getKey();
                        InputStream fileInputStream = fileMsgOp.get().getValue();
                        if (fileName.endsWith(".jpg") || fileName.endsWith(".png") || fileName.endsWith(".jpeg")) {
                            wrapper.showImageMessage(fileName, fileInputStream.readAllBytes(), msg.getSender(), MessagesLayoutScrollerWrapper.Destination.ANOTHER);
                        } else {
                            wrapper.showFileMessage(fileName, fileInputStream.readAllBytes(), msg.getSender(), MessagesLayoutScrollerWrapper.Destination.ANOTHER);
                        }
                    } catch (IOException e) {
                        log.debug("Error while reading file: " + fileMsgOp.get().getKey());
                    }
                }
            }
        }
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

        // todo: add interrupt encryption
        ProgressBar progressBar = new ProgressBar(0, 1, 0);

        chatLayout.add(scroller, msgInputBtnAddFileContainer, progressBar);

        return chatLayout;
    }

    private HorizontalLayout createMessageContainer() {
        HorizontalLayout messageLayout = new HorizontalLayout();
        messageLayout.addClassNames(LumoUtility.Flex.AUTO, LumoUtility.FlexDirection.ROW, LumoUtility.AlignItems.CENTER, Padding.SMALL);

        Upload dropDisabledSingleFileUpload = createUpload();

        MessageInput input = new MessageInput();
        input.addSubmitListener(event -> {
            if (currentChat == null || currentChat.getCompanion() == null) {
                Notification.show("No chat selected", 5000, Notification.Position.BOTTOM_END).addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }

            // нажали на кнопку отправить
            sendMessage(dropDisabledSingleFileUpload);

            String message = event.getValue();
            if (!message.isEmpty()) {
                if (message.length() > FILE_PAGE_SIZE) {
                    Notification.show(String.format("Message is too long, it must not be more than %d bytes.", FILE_PAGE_SIZE)).addThemeVariants(NotificationVariant.LUMO_WARNING);
                    return;
                }
                chatClientService.sendMessage(currentChat.getCompanion(), event.getValue());
                wrapper.showTextMessage(event.getValue(), currentChat.getCompanion(), MessagesLayoutScrollerWrapper.Destination.OWN);
            }
        });

        input.setWidthFull();

        messageLayout.add(dropDisabledSingleFileUpload, input);
        return messageLayout;
    }

    private void sendMessage(Upload upload) {
        try {
            String companion = currentChat.getCompanion();
            for (var file : files) {
                String fileName = file.getKey();
                chatClientService.sendFile(companion, file.getKey(), file.getValue());

                if (fileName.endsWith(".jpg") || fileName.endsWith(".png") || fileName.endsWith(".jpeg")) {
                    wrapper.showImageMessage(fileName, file.getValue().readAllBytes(), companion, MessagesLayoutScrollerWrapper.Destination.OWN);
                } else {
                    wrapper.showFileMessage(fileName, file.getValue().readAllBytes(), companion, MessagesLayoutScrollerWrapper.Destination.OWN);
                }
            }
            upload.clearFileList();
            files.clear();
        } catch (IOException | RuntimeException e) {
            // runtime exception is thrown if encryption context not found
            log.error("I/O exception happened trying to send file, ", e);
        }
    }

    private Aside createAside() {
        Aside side = new Aside();
        side.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Flex.GROW_NONE,
                LumoUtility.Flex.SHRINK_NONE, LumoUtility.Background.CONTRAST_5, LumoUtility.AlignItems.END, LumoUtility.Margin.SMALL);
        side.setWidth("18rem");

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
        side.add(tabs, buttonAddChatRoom);

        return side;
    }

    private Tabs createTabs() {
        tabs = new Tabs();
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        tabs.setWidthFull();
        tabs.addClassNames(LumoUtility.Flex.GROW, LumoUtility.Flex.SHRINK, LumoUtility.Overflow.HIDDEN);
        return tabs;
    }

    private Upload createUpload() {
        MultiFileMemoryBuffer multiFileMemoryBuffer = new MultiFileMemoryBuffer();
        Upload upload = new Upload(multiFileMemoryBuffer);

        upload.setDropAllowed(false);

        upload.addSucceededListener(event -> {
            // Get information about the uploaded file
            String fileName = event.getFileName();

            files.add(new Pair<>(fileName, multiFileMemoryBuffer.getInputStream(fileName)));
            // Do something with the file data
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

    private Button createButtonAddChatRoom(Dialog dialogAddChatRoom) {
        Button btn = new Button("Add chat room", e -> dialogAddChatRoom.open());
        btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btn.setWidthFull();

        return btn;
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
        selectAlgorithm.setItems("LOKI97", "MARS", "RC6", "DES", "DEAL", "Rijndael", "RSA");
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

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        Page page = attachEvent.getUI().getPage();
        page.retrieveExtendedClientDetails(details -> setMobile(details.getWindowInnerWidth() < 740));
        page.addBrowserWindowResizeListener(e -> setMobile(e.getWidth() < 740));

        chatClientService.initMessageHandler();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        log.debug("onDetach");
        chatClientService.disconnect();
        scheduled.shutdownNow();
    }

    private void setMobile(boolean mobile) {
        tabs.setOrientation(mobile ? Tabs.Orientation.HORIZONTAL : Tabs.Orientation.VERTICAL);
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
                            .set("padding", "10px")
                            .set("border", "1px solid #ddd");

                    if (currentChat.getCompanion().equals(companion)) {
                        VerticalLayout updated = ((VerticalLayout) messagesLayout.getContent());
                        updated.add(messageDiv);

                        messagesLayout.setContent(updated);
                    }

                    chatRepository.putMessage(companion, messageDiv);

//                    messagesLayout.add(messageDiv);
//                    messagesLayout.getElement().executeJs("this.scrollTo(0, this.scrollHeight);");
//                    chatRepository.putMessage(companion, messageDiv);
                });
            }
        }

        public void showImageMessage(String nameFile, byte[] data, String companion, Destination destination) {
            Optional<UI> uiOptional = getUI();

            if (uiOptional.isPresent()) {
                UI ui = uiOptional.get();

                ui.access(() -> {
                    Div imageDiv = new Div();

                    StreamResource resource = new StreamResource(nameFile, () -> new ByteArrayInputStream(data));
                    Image image = new Image(resource, "Uploaded image");

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
                            .set("padding", "10px")
                            .set("border-radius", "5px")
                            .set("border", "1px solid #ddd")
                            .set("width", "60%")
                            .set("flex-shrink", "0");

                    image.getStyle()
                            .set("width", "100%")
                            .set("height", "100%");

                    VerticalLayout updated = ((VerticalLayout) messagesLayout.getContent());
                    updated.add(imageDiv);

                    messagesLayout.setContent(updated);
                    chatRepository.putMessage(companion, imageDiv);

//                    messagesLayout.add(imageDiv);
//                    messagesLayout.getElement().executeJs("this.scrollTo(0, this.scrollHeight);");
                });
            }
        }

        public void showFileMessage(String nameFile, byte[] data, String companion, Destination destination) {
            Optional<UI> uiOptional = getUI();

            if (uiOptional.isPresent()) {
                UI ui = uiOptional.get();

                ui.access(() -> {
                    Div fileDiv = new Div();
                    StreamResource resource = new StreamResource(nameFile, () -> new ByteArrayInputStream(data));

                    Anchor downloadLink = new Anchor(resource, "");
                    downloadLink.getElement().setAttribute("download", true);

                    Button downloadButton = new Button(nameFile, event -> downloadLink.getElement().callJsFunction("click"));

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
                            .set("padding", "10px")
                            .set("border-radius", "5px")
                            .set("border", "1px solid #ddd")
                            .set("flex-shrink", "0");

                    VerticalLayout updated = ((VerticalLayout) messagesLayout.getContent());
                    updated.add(fileDiv);

                    messagesLayout.setContent(updated);
                    chatRepository.putMessage(companion, fileDiv);

//                    messagesLayout.add(fileDiv);
//                    messagesLayout.getElement().executeJs("this.scrollTo(0, this.scrollHeight);");
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
//            else {
//                getUI().ifPresent(ui -> ui.access(() -> {
//                    messagesLayout.setContent(new VerticalLayout());
//                }));
//            }
        }
    }


    public class MessagesLayoutWrapper {
        private final VerticalLayout messagesLayout;

        public enum Destination {
            OWN,
            ANOTHER
        }

        public MessagesLayoutWrapper(VerticalLayout messagesLayout) {
            this.messagesLayout = messagesLayout;
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
                            .set("padding", "10px")
                            .set("border", "1px solid #ddd");

                    messagesLayout.add(messageDiv);
                    messagesLayout.getElement().executeJs("this.scrollTo(0, this.scrollHeight);");
                    chatRepository.putMessage(companion, messageDiv);
                });
            }
        }

        public void showImageMessage(String nameFile, byte[] data, String companion, Destination destination) {
            Optional<UI> uiOptional = getUI();

            if (uiOptional.isPresent()) {
                UI ui = uiOptional.get();

                ui.access(() -> {
                    Div imageDiv = new Div();

                    StreamResource resource = new StreamResource(nameFile, () -> new ByteArrayInputStream(data));
                    Image image = new Image(resource, "Uploaded image");

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
                            .set("padding", "10px")
                            .set("border-radius", "5px")
                            .set("border", "1px solid #ddd")
                            .set("width", "60%")
                            .set("flex-shrink", "0");

                    image.getStyle()
                            .set("width", "100%")
                            .set("height", "100%");

                    messagesLayout.add(imageDiv);
                    messagesLayout.getElement().executeJs("this.scrollTo(0, this.scrollHeight);");
                    chatRepository.putMessage(companion, imageDiv);
                });
            }
        }

        public void showFileMessage(String nameFile, byte[] data, String companion, Destination destination) {
            Optional<UI> uiOptional = getUI();

            if (uiOptional.isPresent()) {
                UI ui = uiOptional.get();

                ui.access(() -> {
                    Div fileDiv = new Div();
                    StreamResource resource = new StreamResource(nameFile, () -> new ByteArrayInputStream(data));

                    Anchor downloadLink = new Anchor(resource, "");
                    downloadLink.getElement().setAttribute("download", true);

                    Button downloadButton = new Button(nameFile, event -> downloadLink.getElement().callJsFunction("click"));

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
                            .set("padding", "10px")
                            .set("border-radius", "5px")
                            .set("border", "1px solid #ddd")
                            .set("flex-shrink", "0");

                    messagesLayout.add(fileDiv);
                    messagesLayout.getElement().executeJs("this.scrollTo(0, this.scrollHeight);");
                    chatRepository.putMessage(companion, fileDiv);
                });
            }
        }

        private void clearMessages() {
            Optional<UI> uiOptional = getUI();

            if (uiOptional.isPresent()) {
                UI ui = uiOptional.get();
                ui.access(messagesLayout::removeAll);
            }
        }

        private void showMessages(String companion) {
            var messages = chatRepository.getAllMessages(companion);

            if (messages != null && !messages.isEmpty()) {
                wrapper.clearMessages();

                for (var message : messages) {
                    messagesLayout.add(message);
                }
                messagesLayout.getElement().executeJs("this.scrollTo(0, this.scrollHeight);");
            }
        }
    }

}
