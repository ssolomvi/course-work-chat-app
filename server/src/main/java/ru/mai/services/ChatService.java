package ru.mai.services;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mai.*;
import ru.mai.kafka.KafkaMessageHandler;
import ru.mai.kafka.model.MessageDto;
import ru.mai.repositories.*;
import ru.mai.util.InitRoomResponseBuilder;

import java.util.UUID;

@Slf4j
@Component
public class ChatService extends ChatServiceGrpc.ChatServiceImplBase {
    private static final String DIFFIE_HELLMAN_G = "88005553535";

    private final ConnectResponse connectResponse = ConnectResponse.newBuilder().setDiffieHellmanG(DIFFIE_HELLMAN_G).build();

    private final ActiveUsersAndConsumersRepository usersRep;
    private final DeleteRoomRequestsRepository deleteRequestsRep;
    private final AddRoomRequestsRepository addRequestsRep;
    private final DiffieHellmanNumbersRepository numbersRep;
    private final ChatRoomRepository chatRep;
    private final KafkaMessageHandler messageHandler;


    public ChatService(@Autowired ActiveUsersAndConsumersRepository usersRep,
                       @Autowired DeleteRoomRequestsRepository deleteRequestsRep,
                       @Autowired AddRoomRequestsRepository addRequestsRep,
                       @Autowired DiffieHellmanNumbersRepository numbersRep,
                       @Autowired ChatRoomRepository chatRep,
                       @Autowired KafkaMessageHandler messageHandler) {
        this.usersRep = usersRep;
        this.deleteRequestsRep = deleteRequestsRep;
        this.addRequestsRep = addRequestsRep;
        this.numbersRep = numbersRep;
        this.chatRep = chatRep;
        this.messageHandler = messageHandler;
    }

    @Override
    public void connect(Login request, StreamObserver<ConnectResponse> responseObserver) {
        log.debug("{}: connected", request.getLogin());
        usersRep.putUser(request.getLogin());

        // todo: check authorized
        responseObserver.onNext(connectResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void registerChatRooms(ChatRoomLogins request, StreamObserver<Status> responseObserver) {
        super.registerChatRooms(request, responseObserver);

        if (chatRep.put(request.getOwnLogin(), request.getCompanionLogin())) {
            log.debug("{} <-> {}: room registered successfully", request.getOwnLogin(), request.getCompanionLogin());
        } else {
            log.debug("{} -> {}: room already exists or logins are equal", request.getOwnLogin(), request.getCompanionLogin());
            responseObserver.onNext(Status.newBuilder().setEnumStatus(EnumStatus.ENUM_STATUS_ERROR).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void disconnect(Login request, StreamObserver<Status> responseObserver) {
        if (!usersRep.contains(request.getLogin())) {
            log.debug("User {} is already offline / never been created", request.getLogin());
            responseObserver.onNext(Status.newBuilder().setEnumStatus(EnumStatus.ENUM_STATUS_ERROR).build());
            responseObserver.onCompleted();
        }
        log.debug("{}: disconnected", request.getLogin());
        usersRep.deleteUser(request.getLogin());
        responseObserver.onNext(Status.newBuilder().setEnumStatus(EnumStatus.ENUM_STATUS_OK).build());
        responseObserver.onCompleted();
    }

    @Override
    public void checkCompanionStatus(ChatRoomLogins request, StreamObserver<CompanionAndStatus> responseObserver) {
        if (usersRep.isActive(request.getCompanionLogin())) {
            log.debug("{} -> {}: companion is online", request.getOwnLogin(), request.getCompanionLogin());
            responseObserver.onNext(CompanionAndStatus.newBuilder()
                    .setCompanionLogin(request.getCompanionLogin())
                    .setStatus(true)
                    .build());
        } else {
            log.debug("{} -> {}: companion is offline", request.getOwnLogin(), request.getCompanionLogin());
            responseObserver.onNext(CompanionAndStatus.newBuilder()
                    .setCompanionLogin(request.getCompanionLogin())
                    .setStatus(false)
                    .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void initRoom(InitRoomRequest request, StreamObserver<InitRoomResponse> responseObserver) {
        if (chatRep.contains(request.getOwnLogin(), request.getCompanionLogin())) {
            log.debug("{} -> {}: room already exists", request.getOwnLogin(), request.getCompanionLogin());
            responseObserver.onNext(InitRoomResponse.getDefaultInstance());
            responseObserver.onCompleted();
            return;
        }

        String ownLogin = request.getOwnLogin();
        String companionLogin = request.getCompanionLogin();

        if (!usersRep.isActive(companionLogin)) {
            log.debug("Companion {} is offline", companionLogin);
            responseObserver.onNext(InitRoomResponse.getDefaultInstance());
            responseObserver.onCompleted();
            return;
        }

        if (addRequestsRep.checkIfCompanionRequested(ownLogin, companionLogin)) {
            // request already has been initiated. Now ownLogin must check for it
            log.debug("Request for room {} <-> {} initiation has already been initiated", ownLogin, companionLogin);
            responseObserver.onNext(InitRoomResponse.getDefaultInstance());
            responseObserver.onCompleted();
            return;
        }

        InitRoomResponse response = InitRoomResponseBuilder.build(companionLogin, request.getAlgorithm(), request.getEncryptionMode(), request.getPaddingMode());

        if (!addRequestsRep.putRequest(ownLogin, companionLogin, InitRoomResponseBuilder.buildForCompanion(ownLogin, response))) {
            responseObserver.onNext(InitRoomResponse.getDefaultInstance());
            responseObserver.onCompleted();
            return;
        }

        log.debug("{} -> {}: initiated room creation", ownLogin, companionLogin);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void checkForInitRoomRequest(Login request, StreamObserver<InitRoomResponse> responseObserver) {
        var op = addRequestsRep.getIfAny(request.getLogin());

        if (op.isEmpty()) {
            responseObserver.onNext(InitRoomResponse.getDefaultInstance());
            responseObserver.onCompleted();
            return;
        }

        var requesters = op.get();

        for (var requester : requesters.entrySet()) {
            log.debug("{} -> {}: got invited to chat", request.getLogin(), requester.getKey());
            responseObserver.onNext(requester.getValue());
        }

        responseObserver.onCompleted();
    }

    @Override
    public void passDiffieHellmanNumber(DiffieHellmanNumber request, StreamObserver<Status> responseObserver) {
        numbersRep.put(request.getOwnLogin(), request.getCompanionLogin(), request.getNumber());

        log.debug("{} -> {}: sent diffie-hellman number {}", request.getOwnLogin(), request.getCompanionLogin(), request.getNumber());

        responseObserver.onNext(Status.newBuilder().setEnumStatus(EnumStatus.ENUM_STATUS_OK).build());
        responseObserver.onCompleted();
    }

    @Override
    public void anyDiffieHellmanNumber(Login request, StreamObserver<DiffieHellmanNumber> responseObserver) {
        var op = numbersRep.getIfAny(request.getLogin());

        if (op.isEmpty()) {
            responseObserver.onNext(DiffieHellmanNumber.getDefaultInstance());
            responseObserver.onCompleted();
            return;
        }

        var senders = op.get();

        for (var sender : senders.entrySet()) {
            log.debug("{} <- {}: got diffie-hellman number {}", request.getLogin(), sender.getKey(), sender.getValue());
            responseObserver.onNext(DiffieHellmanNumber.newBuilder()
                    .setOwnLogin(request.getLogin())
                    .setCompanionLogin(sender.getKey())
                    .setNumber(sender.getValue())
                    .build());
            chatRep.put(request.getLogin(), sender.getKey());
        }

        responseObserver.onCompleted();
    }

    @Override
    public void deleteRoom(ChatRoomLogins request, StreamObserver<CompanionAndStatus> responseObserver) {
        if (!usersRep.isActive(request.getCompanionLogin())
                || deleteRequestsRep.checkIfCompanionRequested(request.getOwnLogin(), request.getCompanionLogin())
                || !chatRep.contains(request.getOwnLogin(), request.getCompanionLogin())) {
            responseObserver.onNext(CompanionAndStatus.newBuilder()
                    .setCompanionLogin(request.getCompanionLogin())
                    .setStatus(false)
                    .build());
            responseObserver.onCompleted();
            return;
        }

        deleteRequestsRep.putRequest(request.getOwnLogin(), request.getCompanionLogin());
        chatRep.remove(request.getOwnLogin(), request.getCompanionLogin());
        log.debug("{} -> {}: initiated deletion of chat", request.getOwnLogin(), request.getCompanionLogin());

        responseObserver.onNext(CompanionAndStatus.newBuilder()
                .setCompanionLogin(request.getCompanionLogin())
                .setStatus(true)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void checkForDeletedRoom(Login request, StreamObserver<CompanionAndStatus> responseObserver) {
        var op = deleteRequestsRep.getIfAny(request.getLogin());

        if (op.isEmpty()) {
            responseObserver.onNext(CompanionAndStatus.getDefaultInstance());
            responseObserver.onCompleted();
            return;
        }

        var requesters = op.get();

        for (var deletionInitiator : requesters) {
            log.debug("{} -> {}: companion deleted chat", request.getLogin(), deletionInitiator);
            responseObserver.onNext(CompanionAndStatus.newBuilder().setCompanionLogin(deletionInitiator).setStatus(true).build());
        }

        responseObserver.onCompleted();
    }

    @Override
    public void sendMessage(MessageToCompanion request, StreamObserver<Status> responseObserver) {
        if (usersRep.isActive(request.getCompanionLogin())) {
            log.debug("Sending message {} to {}", request.getValue(), request.getCompanionLogin());
            messageHandler.sendMessage(request.getCompanionLogin(),
                    new MessageDto(UUID.fromString(request.getUuid()),
                            request.getSender(), request.getFilename(),
                            request.getPartitions(), request.getCurrIndex(),
                            request.getValue().toByteArray()));
        }
        responseObserver.onNext(Status.newBuilder().setEnumStatus(EnumStatus.ENUM_STATUS_OK).build());
        responseObserver.onCompleted();
    }

    @Override
    public void anyMessages(Login request, StreamObserver<MessageToCompanion> responseObserver) {
        var op = messageHandler.readMessages(request.getLogin());
        if (op.isEmpty()) {
            log.debug("No messages for {}", request.getLogin());
            responseObserver.onNext(MessageToCompanion.getDefaultInstance());
            responseObserver.onCompleted();
            return;
        }

        var consumerRecords = op.get();

        for (var consumerRecord : consumerRecords) {
            String sender = consumerRecord.value().getSender();
            log.debug("{} <- {}: got message: {}", request.getLogin(), sender, consumerRecord.value().getValue());

            MessageDto dto = consumerRecord.value();

            responseObserver.onNext(MessageToCompanion.newBuilder()
                    .setCompanionLogin(sender)
                    .setUuid(consumerRecord.value().getMessageId().toString())
                    .setSender(sender)
                    .setFilename(dto.getFileName())
                    .setPartitions(dto.getNumberOfPartitions())
                    .setCurrIndex(dto.getCurrIndex())
                    .setValue(ByteString.copyFrom(dto.getValue()))
                    .build()
            );
        }

        responseObserver.onCompleted();
    }
}