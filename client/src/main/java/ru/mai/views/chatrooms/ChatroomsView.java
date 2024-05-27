package ru.mai.views.chatrooms;

import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import ru.mai.services.ChatClientService;
import ru.mai.views.MainLayout;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@PageTitle("Chat-rooms")
@Route(value = "chat", layout = MainLayout.class)
public class ChatroomsView extends HorizontalLayout implements HasUrlParameter<String> {
    private String login = "dummy";

    public static String createParameter(String username) {
        return username;
    }

    @Override
    public void setParameter(BeforeEvent event, @WildcardParameter String parameter) {
        Notification.show(String.format("Hello, %s!", parameter));
        this.login = parameter;

    }

    @Getter
    public static class ChatTab extends Tab {
        private final ChatInfo chatInfo;

        public ChatTab(ChatInfo chatInfo) {
            this.chatInfo = chatInfo;
        }

    }

    public static class ChatInfo {
        private final String name;

        private ChatInfo(String name) {
            this.name = name;
        }

        public String getCollaborationTopic() {
            return "chat/" + name;
        }
    }

//    private final ChatClientService clientService;

    private final List<ChatInfo> chats = new CopyOnWriteArrayList<>();
    private ChatInfo currentChat;
    private Tabs tabs;

    public ChatroomsView(RouteParameters routeParameters,   @Autowired ChatClientService clientService) {
        this.login = routeParameters.get("chat").orElse("");
        Notification.show(String.format("Hello, %s!", login));

        // todo: путь (route) должен меняться при переключении на табу чата
        // т.е.: когда пользователь только заходит, путь страницы /login
        // ниже не обязательно
        // когда появляется первый активный чат, путь страницы /login/chat1, где chat1 может быть логином собеседника
        // при нескольких чатах появляется возможность выбирать чат.
        // В зависимости от открытого таба путь страницы превращается в /login/chatN

//        this.clientService = clientService;
//        this.clientService.setLogin(login); // todo:
        // this.clientService.connect();

        addClassNames("chat-view", LumoUtility.Width.FULL, LumoUtility.Display.FLEX, LumoUtility.Flex.AUTO);
        setSpacing(false);

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserInfo userInfo = new UserInfo(userDetails.getUsername(), userDetails.getUsername());

        // Layouting



        // side panel
        Aside side = createAside();
        VerticalLayout chatContainer = createChatContainer();
        add(chatContainer, side);


//        Header header = createHeader();

//        side.add(header, tabs, buttonAddChatRoom);

        // messages container
        // upload files button


        // Change the topic id of the chat when a new tab is selected

    }

    private VerticalLayout createChatContainer() {
        VerticalLayout chatContainer = new VerticalLayout();
//        chatContainer.addClassNames(LumoUtility.Flex.AUTO);
        chatContainer.addClassNames(LumoUtility.Flex.AUTO, LumoUtility.Overflow.HIDDEN);

        MessageList list = new MessageList();
        list.setSizeFull();

        MessageInput input = new MessageInput();
        input.setWidthFull();

        Upload dropDisabledSingleFileUpload = createUpload();

        HorizontalLayout msgInputBtnAddFileContainer = new HorizontalLayout();
        msgInputBtnAddFileContainer.setMaxWidth(chatContainer.getWidth());
        msgInputBtnAddFileContainer.addClassNames(LumoUtility.Flex.AUTO, LumoUtility.FlexDirection.ROW, LumoUtility.Width.AUTO, LumoUtility.AlignItems.CENTER, Padding.SMALL);

        msgInputBtnAddFileContainer.add(dropDisabledSingleFileUpload, input);

        // todo: add interrupt encryption
        ProgressBar progressBar = new ProgressBar(0, 1, 0);

        chatContainer.add(list, msgInputBtnAddFileContainer, progressBar);

        setSizeFull();
        expand(list);

        return chatContainer;
    }

    private Aside createAside() {
        Aside side = new Aside();
        side.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Flex.GROW_NONE, LumoUtility.Flex.SHRINK_NONE, LumoUtility.Background.CONTRAST_5);
        side.setWidth("18rem");

        tabs = createTabs(chats);
        tabs.addSelectedChangeListener(event -> {
            if (tabs.getComponentCount() != 0) {
                currentChat = ((ChatTab) event.getSelectedTab()).getChatInfo();
            } else {
                currentChat = null;
//                msgInputBtnAddFileContainer.setVisible(false);
//                progressBar.setVisible(false);
            }
        });

        Dialog dialogAddChatRoom = createDialog();
        Button buttonAddChatRoom = createButtonAddChatRoom(dialogAddChatRoom);
        side.add(tabs, buttonAddChatRoom);

        return side;
    }

    private Tabs createTabs(List<ChatInfo> chats) {
        tabs = new Tabs();
        for (ChatInfo chat : chats) {
            ChatTab tab = new ChatTab(chat);
            tab.addClassNames(LumoUtility.JustifyContent.BETWEEN);

            tab.add(new Span("# " + chat.name));

            Button buttonCloseTab = new Button(new Icon("lumo", "cross"),
                    e -> {
                        tabs.remove(tab);
                        int countOfChildrenTabs = tabs.getComponentCount();
                        if (countOfChildrenTabs != 0) {
                            tabs.setSelectedIndex(0);
                        }
                    });
            buttonCloseTab.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            tab.add(buttonCloseTab);
            tabs.add(tab);
        }
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        tabs.addClassNames(LumoUtility.Flex.GROW, LumoUtility.Flex.SHRINK, LumoUtility.Overflow.HIDDEN);
        return tabs;
    }

    private void setMsgInputContainerAndProgressBarVisible(HorizontalLayout msgInputBtnAddFileContainer, ProgressBar progressBar, boolean isVisible) {
        msgInputBtnAddFileContainer.setVisible(isVisible);
        progressBar.setVisible(isVisible);

    }

    private Upload createUpload() {
        MemoryBuffer bufferAddFile = new MemoryBuffer();
        Upload upload = new Upload(bufferAddFile);

        upload.setDropAllowed(false);

        upload.addSucceededListener(event -> {
            // Get information about the uploaded file
            InputStream fileData = bufferAddFile.getInputStream();
            String fileName = event.getFileName();
            long contentLength = event.getContentLength();
            String mimeType = event.getMIMEType();

            // Do something with the file data
            // processFile(fileData, fileName, contentLength, mimeType);
        });

        return upload;
    }

    private Dialog createDialog() {
        Dialog dialog = new Dialog();
        dialog.getElement().setAttribute("aria-label", "Add note");

        VerticalLayout dialogLayout = createDialogLayout();
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
        btn.addClickListener(clickEvent -> {

        });

        return btn;
    }

    private VerticalLayout createDialogLayout() {
        TextField loginField = new TextField("Login", "",
                "User's login");
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

        Button submit = new Button("Submit");
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
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
//        clientService.disconnect();
    }

    private void setMobile(boolean mobile) {
        tabs.setOrientation(mobile ? Tabs.Orientation.HORIZONTAL : Tabs.Orientation.VERTICAL);
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

        public void showTextMessage(String textMessage, Destination destination) {
            Optional<UI> uiOptional = getUI();

            if (uiOptional.isPresent()) {
                UI ui = uiOptional.get();

                ui.access(() -> {
                    Div messageDiv = new Div();
                    messageDiv.setText(textMessage);

                    if (destination.equals(Destination.OWN)) {
                        messageDiv.getStyle()
                                .set("margin-left", "auto")
                                .set("background-color", "#cceeff");

//                        setPossibilityToDelete(messagesLayout, messageDiv);
                    } else {
                        messageDiv.getStyle()
                                .set("margin-right", "auto")
                                .set("background-color", "#f2f2f2");
                    }

                    messageDiv.getStyle()
                            .set("border-radius", "5px")
                            .set("padding", "10px")
                            .set("border", "1px solid #ddd");

                    messagesLayout.add(messageDiv);
                    messagesLayout.getElement().executeJs("this.scrollTo(0, this.scrollHeight);");
                });
            }
        }

        public void showImageMessage(String nameFile, byte[] data, Destination destination) {
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
                                .set("margin-left", "auto")
                                .set("background-color", "#cceeff");
//                        setPossibilityToDelete(messagesLayout, imageDiv);
                    } else {
                        imageDiv.getStyle()
                                .set("margin-right", "auto")
                                .set("background-color", "#f2f2f2");
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
                });
            }
        }

        public void showFileMessage(String nameFile, byte[] data, Destination destination) {
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
                                .set("margin-left", "auto")
                                .set("background-color", "#cceeff");

//                        setPossibilityToDelete(messagesLayout, fileDiv);
                    } else {
                        fileDiv.getStyle()
                                .set("margin-right", "auto")
                                .set("background-color", "#f2f2f2");
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
                });
            }
        }

//        private void setPossibilityToDelete(VerticalLayout messagesLayout, Div fileDiv) {
//            messagesLayout.getElement().executeJs("this.scrollTo(0, this.scrollHeight);");
//
//            fileDiv.addClickListener(event -> {
//                int indexMessage = messagesLayout.indexOf(fileDiv);
//                messagesLayout.remove(fileDiv);
//                kafkaWriter.processing(new Message("delete_message", "text", null, indexMessage, null).toBytes(), outputTopic);
//            });
//        }

        private void clearMessages() {
            Optional<UI> uiOptional = getUI();

            if (uiOptional.isPresent()) {
                UI ui = uiOptional.get();
                ui.access(messagesLayout::removeAll);
            }
        }
//
//        private void deleteMessage(int index) {
//            Optional<UI> uiOptional = getUI();
//
//            if (uiOptional.isPresent()) {
//                UI ui = uiOptional.get();
//                ui.access(() -> {
//                    Component componentToRemove = messagesLayout.getComponentAt(index);
//                    messagesLayout.remove(componentToRemove);
//                });
//            }
//        }
    }

}
