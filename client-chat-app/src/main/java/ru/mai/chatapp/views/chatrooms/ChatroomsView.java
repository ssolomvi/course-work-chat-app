package ru.mai.chatapp.views.chatrooms;

import com.vaadin.collaborationengine.CollaborationAvatarGroup;
import com.vaadin.collaborationengine.CollaborationMessageInput;
import com.vaadin.collaborationengine.CollaborationMessageList;
import com.vaadin.collaborationengine.UserInfo;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Aside;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import ru.mai.chatapp.views.MainLayout;

import java.io.InputStream;

@PageTitle("Chat-rooms")
@Route(value = "chat", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@RolesAllowed("USER")
public class ChatroomsView extends HorizontalLayout {
    public static class ChatTab extends Tab {
        private final ChatInfo chatInfo;

        public ChatTab(ChatInfo chatInfo) {
            this.chatInfo = chatInfo;
        }

        public ChatInfo getChatInfo() {
            return chatInfo;
        }
    }

    public static class ChatInfo {
        private String name;

        private ChatInfo(String name) {
            this.name = name;
        }

        public String getCollaborationTopic() {
            return "chat/" + name;
        }
    }

    // TODO: load from db
    private ChatInfo[] chats = new ChatInfo[]{new ChatInfo("Peter Parker"), new ChatInfo("Ilya"),
            new ChatInfo("Alex")};
    private ChatInfo currentChat = chats[0];
    private Tabs tabs;

    public ChatroomsView() {
        // connect to server - heartbeat, listen for new rooms
        // init kafka consumer (communication with another person)

        addClassNames("chat-view", LumoUtility.Width.FULL, LumoUtility.Display.FLEX, LumoUtility.Flex.AUTO);
        setSpacing(false);

        // initHeartBeat();


        // UserInfo is used by Collaboration Engine and is used to share details
        // of users to each other to able collaboration. Replace this with
        // information about the actual user that is logged, providing a user
        // identifier, and the user's real name. You can also provide the users
        // avatar by passing an url to the image as a third parameter, or by
        // configuring an `ImageProvider` to `avatarGroup`.

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserInfo userInfo = new UserInfo(userDetails.getUsername(), userDetails.getUsername());

        tabs = createTabs(chats);

        // CollaborationMessageList displays messages that are in a
        // Collaboration Engine topic. You should give in the user details of
        // the current user using the component, and a topic Id. Topic id can be
        // any freeform string. In this template, we have used the format
        // "chat/#general".
        CollaborationMessageList list = new CollaborationMessageList(userInfo, currentChat.getCollaborationTopic());
        list.setSizeFull();

        // `CollaborationMessageInput is a textfield and button, to be able to
        // submit new messages. To avoid having to set the same info into both
        // the message list and message input, the input takes in the list as an
        // constructor argument to get the information from there.
        CollaborationMessageInput input = new CollaborationMessageInput(list);
        input.setWidthFull();
//        input.addAttachListener()

        // upload files button
        MemoryBuffer bufferAddFile = new MemoryBuffer();
        Upload dropDisabledSingleFileUpload = new Upload(bufferAddFile);
        dropDisabledSingleFileUpload.setDropAllowed(false);

        dropDisabledSingleFileUpload.addSucceededListener(event -> {
            // Get information about the uploaded file
            InputStream fileData = bufferAddFile.getInputStream();
            String fileName = event.getFileName();
            long contentLength = event.getContentLength();
            String mimeType = event.getMIMEType();

            // Do something with the file data
            // processFile(fileData, fileName, contentLength, mimeType);
        });

        // Layouting

        VerticalLayout chatContainer = new VerticalLayout();
        chatContainer.addClassNames(LumoUtility.Flex.AUTO /*LumoUtility.Overflow.HIDDEN*/);

        Aside side = new Aside();
        side.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Flex.GROW_NONE, LumoUtility.Flex.SHRINK_NONE, LumoUtility.Background.CONTRAST_5);
        side.setWidth("18rem");
        Header header = new Header();
        header.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.ROW, LumoUtility.Width.FULL, LumoUtility.AlignItems.CENTER, Padding.MEDIUM,
                LumoUtility.BoxSizing.BORDER);
        H3 channels = new H3("Chat rooms");
        channels.addClassNames(LumoUtility.Flex.GROW, LumoUtility.Margin.NONE);

        CollaborationAvatarGroup avatarGroup = new CollaborationAvatarGroup(userInfo, "chat");
        avatarGroup.setMaxItemsVisible(4);
        avatarGroup.addClassNames(LumoUtility.Width.AUTO);

        header.add(channels, avatarGroup);

        // button add new chat room
        Dialog dialogAddChatRoom = new Dialog();
        dialogAddChatRoom.getElement().setAttribute("aria-label", "Add note");

        VerticalLayout dialogLayout = createDialogLayout();
        dialogAddChatRoom.add(dialogLayout);
        dialogAddChatRoom.setHeaderTitle("Add chat room");

        Button closeButton = new Button(new Icon("lumo", "cross"),
                (e) -> dialogAddChatRoom.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        dialogAddChatRoom.getHeader().add(closeButton);

        Button buttonAddChatRoom = new Button("Add chat room", e -> dialogAddChatRoom.open());
        buttonAddChatRoom.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
//        buttonAddChatRoom.addClickListener(clickEvent -> {
        // todo: rest api: create room
        // todo: heartbeat conection server for listening server for new rooms
//        });

        // add to side
        side.add(header, tabs, buttonAddChatRoom);

        // messages container
        HorizontalLayout msgInputBtnAddFileContainer = new HorizontalLayout();
        msgInputBtnAddFileContainer.addClassNames(LumoUtility.Flex.AUTO, LumoUtility.FlexDirection.ROW, LumoUtility.AlignItems.CENTER, Padding.SMALL);
        msgInputBtnAddFileContainer.setMaxWidth(chatContainer.getWidth());

        msgInputBtnAddFileContainer.add(dropDisabledSingleFileUpload, input);

        // todo: add interrupt encryption
        ProgressBar progressBar = new ProgressBar(0, 1, 0);

        chatContainer.add(list, msgInputBtnAddFileContainer, progressBar);

        add(chatContainer, side);
        setSizeFull();
        expand(list);

        // Change the topic id of the chat when a new tab is selected
        tabs.addSelectedChangeListener(event -> {
            if (tabs.getComponentCount() != 0) {
                currentChat = ((ChatTab) event.getSelectedTab()).getChatInfo();
                list.setTopic(currentChat.getCollaborationTopic());
            }
            else {
                currentChat = null;
                list.setTopic("");
                setMsgInputContainerAndProgressBarVisible(msgInputBtnAddFileContainer, progressBar, false);
//                msgInputBtnAddFileContainer.setVisible(false);
//                progressBar.setVisible(false);
            }
        });
    }

    private Tabs createTabs(ChatroomsView.ChatInfo[] chats) {
        tabs = new Tabs();
        for (ChatInfo chat : chats) {
            ChatTab tab = new ChatTab(chat);
            tab.addClassNames(LumoUtility.JustifyContent.BETWEEN);

            tab.add(new Span("# " + chat.name));

            // todo: problem with all tabs closed
            Button buttonCloseTab = new Button(new Icon("lumo", "cross"),
                    (e) -> {
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

    private VerticalLayout createDialogLayout() {
        TextField loginField = new TextField("Login", "",
                "User's login");
        loginField.getStyle().set("padding-top", "0");

        Select<String> selectAlgorithm = new Select<>();
        selectAlgorithm.setLabel("Encrypt by");
        // todo: algorithms
        selectAlgorithm.setItems("LOKI", "MARS", "RC6", "DES", "DEAL", "Rijndael", "RSA");
        selectAlgorithm.setValue("LOKI");

        VerticalLayout fieldLayout = new VerticalLayout(loginField, selectAlgorithm);
        fieldLayout.setSpacing(false);
        fieldLayout.setPadding(false);
        fieldLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        fieldLayout.getStyle().set("width", "300px").set("max-width", "100%");

        return fieldLayout;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        Page page = attachEvent.getUI().getPage();
        page.retrieveExtendedClientDetails(details -> setMobile(details.getWindowInnerWidth() < 740));
        page.addBrowserWindowResizeListener(e -> setMobile(e.getWidth() < 740));
    }

    private void setMobile(boolean mobile) {
        tabs.setOrientation(mobile ? Tabs.Orientation.HORIZONTAL : Tabs.Orientation.VERTICAL);
    }
}
