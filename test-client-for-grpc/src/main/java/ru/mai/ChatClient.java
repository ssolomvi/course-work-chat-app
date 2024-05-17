package ru.mai;//package ru.mai;
//
//import io.grpc.ManagedChannel;
//import io.grpc.ManagedChannelBuilder;
//import io.grpc.stub.StreamObserver;
//
//import javax.annotation.Nullable;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//
//import static ru.mai.ChatServer.*;
//
//public class ChatClient {
//    /*  -------------------------------- LOGGER -------------------------------- */
//    private static final Logger logger = Logger.getLogger(ChatClient.class.getName());
//
//    /*  -------------------------------- CONNECTION STUFF -------------------------------- */
//    private final ManagedChannel channel;
//    private final ChatServiceGrpc.ChatServiceStub asyncStub;
//    private final ChatServiceGrpc.ChatServiceBlockingStub blockingStub;
//
//    /*  -------------------------------- LISTS -------------------------------- */
//    private final List<String> messagesPublic;
//    private final List<String> messagesPrivate;
//    private final List<String> users;
//
//    /*  -------------------------------- USER INFO -------------------------------- */
//    private User user;
//
//    /*  -------------------------------- CONSTRUCTORS -------------------------------- */
//    public ChatClient(String hostname, int portNumber) {
//        this(ManagedChannelBuilder.forAddress(hostname, portNumber).usePlaintext());
//    }
//
//    public ChatClient(ManagedChannelBuilder<?> channelBuilder) {
//        messagesPublic = new ArrayList<>();
//        messagesPrivate = new ArrayList<>();
//        users = new ArrayList<>();
//
//        /*  -------------------------------- START -------------------------------- */
//        channel = channelBuilder.build();
//        asyncStub = ChatServiceGrpc.newStub(channel);
//        blockingStub = ChatServiceGrpc.newBlockingStub(channel);
//        logger.log(Level.INFO, "Client started");
//        logger.setLevel(Level.FINE);
//    }
//
//    /*  -------------------------------- CONNECT/DISCONNECT -------------------------------- */
//    public boolean connectUser(String username) {
//        UserInfo userInfo = UserInfo.newBuilder().setName(username).build();
//        ConnectMessage response;
//        try {
//            response = blockingStub.connectUser(userInfo);
//            if (response.getIsConnected()) {
//                user = new User(username);
//                logger.log(Level.INFO, "Successfully connected to server.");
//
//                messagesPublic.add("Welcome to the chat " + username + " !");
//                syncUserList();
//                syncMessages();
//
//                sendBroadcastMsg(username + " has entered the chat");
//                return true;
//            } else {
//                logger.log(Level.WARNING, "Duplicate username (" + username + ") entered");
//                messagesPublic.add("Username already taken, choose another one.");
//            }
//        } catch (Exception e) {
//            logger.log(Level.SEVERE, "Exception" + e.getMessage());
//        }
//        return false;
//    }
//
//    public void disconnectUser() throws InterruptedException {
//        UserInfo userInfo = UserInfo.newBuilder().setName(user.getName()).build();
//        DisconnectMessage response;
//        try {
//            response = blockingStub.disconnectUser(userInfo);
//            if (response.getIsDisconnected()) {
//                logger.log(Level.INFO, "Successfully disconnected from server.");
//                sendBroadcastMsg(user.getName() + " has left the chat");
//                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
//            } else {
//                logger.log(Level.WARNING, "Failed to disconnect from server");
//                messagesPublic.add("Failed to disconnect from server, try again.");
//            }
//        } catch (Exception e) {
//            logger.log(Level.SEVERE, "Exception" + e.getMessage());
//        }
//    }
//
//    /*  -------------------------------- SENDING MESSAGES -------------------------------- */
//    // (send) add the message to the shared message's list at the serverside
//    public void sendBroadcastMsg(String text) throws Exception {
//
//        if (user != null) {
//            MessageText messageText = MessageText.newBuilder().setText(text).setSender(user.getName()).build();
//            try {
//                info("Broadcasting...");
//                blockingStub.sendBroadcastMsg(messageText);
//            } catch (Exception e) {
//                error(e.getMessage());
//                messagesPublic.add("Could not connect with server. Try again.");
//            }
//        } else {
//            throw new Exception("Could not find user");
//        }
//    }
//
//    public void sendPrivateMsg(String text, String receiverName) throws Exception {
//
//        if (user != null) {
//            // make standard message
//            MessageText messageText = MessageText.newBuilder().setText(text).setSender(user.getName()).build();
//            // make private message intended for receiver
//            PrivateMessageText privateMessageText = PrivateMessageText.newBuilder().setMessageText(messageText).setReceiver(receiverName).build();
//            try {
//                info("Send private message...");
//                blockingStub.sendPrivateMsg(privateMessageText);
//            } catch (Exception e) {
//                error(e.getMessage());
//                messagesPrivate.add("Could not connect with server. Try again.");
//            }
//        } else {
//            throw new Exception("Could not find user");
//        }
//    }
//
//    /*  -------------------------------- GETTING MESSAGES -------------------------------- */
//    // check if their are new message's in the server's message list
//    public void syncMessages() {
//        StreamObserver<MessageText> observer = new StreamObserver<MessageText>() {
//            @Override
//            public void onNext(MessageText value) {
//                info("message received from " + value.getSender() + ".");
//                placeInRightMessageList(value.getText(), value.getSender());
//            }
//
//            @Override
//            public void onError(Throwable t) {
//                error("Server-side error.");
//                messagesPublic.add("Server-side error.");
//            }
//
//            @Override
//            public void onCompleted() {
//            }
//        };
//        try {
//            asyncStub.syncMessages(UserInfo.newBuilder().setName(user.getName()).build(), observer);
//        } catch (Exception e) {
//            error(e.getMessage());
//        }
//    }
//
//    public void placeInRightMessageList(String text, String sender) {
//
//        String[] split = text.split(MESSAGE_TYPE_REGEX);
//        String MESSAGE_ID = split[0];
//        String send = split[1];
//        String content = split[2];
//
//        switch (MESSAGE_ID) {
//            case PRIVATE_MESSAGE_ID:
//                messagesPrivate.add(send + ":" + content);
//                break;
//            case PUBLIC_MESSAGE_ID:
//                messagesPublic.add(send + ":" + content);
//                break;
//        }
//    }
//
//    /*  -------------------------------- GETTING USER INFO -------------------------------- */
//    public void syncUserList() {
//        StreamObserver<UserInfo> observer = new StreamObserver<UserInfo>() {
//            @Override
//            public void onNext(UserInfo value) {
//                info("Public message received from " + value.getName() + ".");
//
//                if (!users.contains(value.getName())) {
//                    users.add(value.getName());
//                }
//
//                logger.log(Level.INFO, value.getName() + " added to list");
//            }
//
//            @Override
//            public void onError(Throwable t) {
//                error("Server error.");
//                messagesPublic.add("Server error  user list");
//            }
//
//            @Override
//            public void onCompleted() {
//            }
//        };
//        try {
//            asyncStub.syncUserList(Empty.newBuilder().build(), observer);
//        } catch (Exception e) {
//            error(e.getMessage());
//        }
//    }
//
//    public List<String> getPublicMessages() {
//        return messagesPublic;
//    }
//
//    public List<String> getPrivateMessages() {
//        return messagesPrivate;
//    }
//
//    public List<String> getUsers() {
//        return users;
//    }
//
//    public User getUser() {
//        return user;
//    }
//
//    private static void info(String msg, @Nullable Object... params) {
//        logger.log(Level.INFO, msg, params);
//    }
//
//    private static void error(String msg, @Nullable Object... params) {
//        logger.log(Level.WARNING, msg, params);
//    }
//}