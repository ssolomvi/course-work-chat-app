package ru.mai;//package ru.mai;
//
//import io.grpc.ManagedChannel;
//import io.grpc.Status;
//import io.grpc.StatusRuntimeException;
//import io.grpc.stub.StreamObserver;
//import ru.mai.chat_service.ChatRoomTopic;
//
//import java.math.BigInteger;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//
//public class ServerChatService extends ChatServiceGrpc.ChatServiceImplBase {
//    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet(); // Store online users
//    private final Map<String, String> userChannels = new ConcurrentHashMap<>(); // Store user -> channel mapping
//
//    // modify map tp string -> list<String>
//    private final Map<String, List<ChatRoomTopic>> chatRooms = new ConcurrentHashMap<>(); // Store chat room information (client1_login -> ChatRoomTopic)
//
//    private static final String G = "88005553535";
//
//    @Override
//    public void login(ChatServiceOuterClass.LoginRequest request, StreamObserver<ChatServiceOuterClass.LoginResponse> responseObserver) {
//        onlineUsers.add(request.getUsername());
//        responseObserver.onNext(ChatServiceOuterClass.LoginResponse.newBuilder().setG(G).build());
//        responseObserver.onCompleted();
//    }
//
//    @Override
//    public void logout(ChatServiceOuterClass.LoginRequest request, StreamObserver<ChatServiceOuterClass.Empty> responseObserver) {
//        onlineUsers.remove(request.getUsername());
//
//        userChannels.remove(request.getUsername());
//
//        responseObserver.onNext(ChatServiceOuterClass.Empty.newBuilder().build());
//        responseObserver.onCompleted();
//    }
//
//    private void addNewCompanion(String login, String companionLogin, String topicIn) {
//        var newChatRoomTopic = new ChatRoomTopic(companionLogin, topicIn);
//
//        List<ChatRoomTopic> companions;
//        if (chatRooms.containsKey(login)) {
//            companions = chatRooms.get(login);
//        } else {
//            // todo: ask Ilusha chat rooms is thread safe collection, what to do with inner collection?
//            companions = new ArrayList<>();
//        }
//        companions.add(newChatRoomTopic);
//        chatRooms.put(login, companions);
//    }
//
//    private String getTopicIn(String f, String s) {
//        return String.format("%s_%s", f, s);
//    }
//
//    @Override
//    public void createChatRoom(ChatServiceOuterClass.ChatRoomRequest request, StreamObserver<ChatServiceOuterClass.ChatRoomResponse> responseObserver) {
//        String client1 = request.getClient1Login(); // initiator
//        String client2 = request.getClient2Login();
//        String algorithm = request.getAlgorithm();
//
//        if (onlineUsers.contains(client2)) {
//            String topicInForClient1 = getTopicIn(client1, client2);
//            String topicInForClient2 = getTopicIn(client2, client1);
//
//            addNewCompanion(client1, client2, topicInForClient1);
//            addNewCompanion(client2, client1, topicInForClient2);
//
//            // todo: get info for encryption/decryption
//
//            ChatServiceOuterClass.ChatRoomResponse chatRoomResponseClient2 = ChatServiceOuterClass.ChatRoomResponse.newBuilder()
//                    .setCompanionLogin(client1)
//                    .setAlgorithm(algorithm)
//                    .setEncryptionMode("ECB")
//                    .setPaddingMode("Zeroes")
//                    .setInitVector("")
//                    .setTopicIn(topicInForClient2)
//                    .setTopicOut(topicInForClient1)
//                    .setDiffieHellmanP(BigInteger.TWO.pow(64).toString()) // 2 ^64 for DES?
//                    .build();
//
////            sendToClient(client2, chatRoomResponseClient2); // todo
//
//            ChatServiceOuterClass.ChatRoomResponse chatRoomResponseClient1 = ChatServiceOuterClass.ChatRoomResponse.newBuilder()
//                    .setCompanionLogin(client2)
//                    .setAlgorithm(algorithm)
//                    .setEncryptionMode("ECB")
//                    .setPaddingMode("Zeroes")
//                    .setInitVector("")
//                    .setTopicIn(topicInForClient2)
//                    .setTopicOut(topicInForClient1)
//                    .setDiffieHellmanP(BigInteger.TWO.pow(64).toString()) // 2 ^64 for DES?
//                    .build();
//
//            responseObserver.onNext(chatRoomResponseClient1);
//            responseObserver.onCompleted();
//        } else {
//            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription(String.format("Client %s not found", client2))));
//        }
//    }
//
//    private boolean deleteIfContains(String login, String companion) {
//        var companions = chatRooms.get(login);
//        if (companions == null) {
//            return false;
//        }
//
//        var dummy = new ChatRoomTopic(companion, "");
//        if (!companions.contains(dummy)) {
//            return false;
//        }
//
//        companions.remove(dummy);
//
//        return true;
//    }
//
//    @Override
//    public void deleteChatRoom(ChatServiceOuterClass.ChatRoomRequest request, StreamObserver<ChatServiceOuterClass.Empty> responseObserver) {
//        String client1 = request.getClient1Login();
//        String client2 = request.getClient2Login();
//
//        // cannot be done if client2 is not active (cause client2 will still have messages)
//        if (!onlineUsers.contains(client2)) {
//            responseObserver.onError(new StatusRuntimeException(Status.CANCELLED.withDescription(String.format("Chat room with client %s cannot be shut, because client %s is not online", client2, client2))));
//        }
//
//        deleteIfContains(client2, client1);
//
//        if (deleteIfContains(client1, client2)) {
////            sendToClient(client2, ChatServiceOuterClass.ChatRoomDeletedResponse.newBuilder); // todo
//
//            responseObserver.onNext(ChatServiceOuterClass.Empty.newBuilder().build());
//            responseObserver.onCompleted();
//        } else {
//            // Handle error: chat room not found
//            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND.withDescription(String.format("Chat room with client %s not found", client2))));
//        }
//    }
//
//    private boolean chatRoomExists(String login, String companion) {
//        if (!chatRooms.containsKey(login) || !chatRooms.containsKey(companion)) {
//            return false;
//        }
//
//        var dummyForClient = new ChatRoomTopic(companion, "");
//        var dummyForCompanion = new ChatRoomTopic(login, "");
//
//        return chatRooms.get(login).contains(dummyForClient) && chatRooms.get(companion).contains(dummyForCompanion);
//    }
//
//    private void createChatRoomIfNotExist(String login, String companion) {
//        if (!chatRooms.containsKey(login)) {
//            List<ChatRoomTopic> companions = new ArrayList<>();
//            companions.add(new ChatRoomTopic(companion, getTopicIn(login, companion)));
//            chatRooms.put(login, companions);
//        } else {
//            var companions = chatRooms.get(login);
//            ChatRoomTopic newChatRoomTopic = new ChatRoomTopic(companion, getTopicIn(login, companion));
//
//            if (!companions.contains(newChatRoomTopic)) {
//                companions.add(newChatRoomTopic);
//                chatRooms.put(login, companions);
//            }
//        }
//    }
//
//    @Override
//    public void connectToRoom(ChatServiceOuterClass.ChatRoomRequest request, StreamObserver<ChatServiceOuterClass.RoomInfo> responseObserver) {
//        String client1 = request.getClient1Login();
//        String client2 = request.getClient2Login();
//
//        // if chat room does not exist, we should create it.
//        createChatRoomIfNotExist(client1, client2);
//        createChatRoomIfNotExist(client2, client1);
//
//        responseObserver.onNext(ChatServiceOuterClass.RoomInfo.newBuilder()
//                .setTopicIn(getTopicIn(client1, client2))
//                .setTopicOut(getTopicIn(client2, client1))
//                .build());
//    }
//
//    @Override
//    public void disconnectFromRoom(ChatServiceOuterClass.ChatRoomRequest request, StreamObserver<ChatServiceOuterClass.Empty> responseObserver) {
//        String client1 = request.getClient1Login();
//        String client2 = request.getClient2Login();
//
//        // todo: send
//        onlineUsers.remove(client1);
//        userChannels.remove(client1);
////        sendToClient(client2, CompanionDisconnected);
//
//        responseObserver.onNext(ChatServiceOuterClass.Empty.newBuilder().build());
//        responseObserver.onCompleted();
//    }
//
//    @Override
//    public void sendDirectMessage(ChatServiceOuterClass.Message request, StreamObserver<ChatServiceOuterClass.Empty> responseObserver) {
//        super.sendDirectMessage(request, responseObserver);
//        // я хуй знает как это писать. Нужно получать channel всех юзеров каким-то образом, но я хуй знает, сработает ли это вообще
//    }
//}
