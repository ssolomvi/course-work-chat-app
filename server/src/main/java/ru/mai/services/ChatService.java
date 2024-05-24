package ru.mai.services;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mai.*;
import ru.mai.kafka.KafkaMessageHandler;
import ru.mai.repositories.ActiveUsersAndConsumersRepository;
import ru.mai.repositories.AddRoomRequestsRepository;
import ru.mai.repositories.DeleteRoomRequestsRepository;
import ru.mai.repositories.DiffieHellmanNumbersRepository;
import ru.mai.util.InitRoomResponseBuilder;

@Slf4j
@Component
public class ChatService extends ChatServiceGrpc.ChatServiceImplBase {
    private static final String DIFFIE_HELLMAN_G = "88005553535";

    private final ConnectResponse connectResponse = ConnectResponse.newBuilder().setDiffieHellmanG(DIFFIE_HELLMAN_G).build();

    private final ActiveUsersAndConsumersRepository usersRep;
    private final DeleteRoomRequestsRepository deleteRequestsRep;
    private final AddRoomRequestsRepository addRequestsRep;
    private final DiffieHellmanNumbersRepository numbersRep;
    private final KafkaMessageHandler messageHandler;


    public ChatService(@Autowired ActiveUsersAndConsumersRepository usersRep,
                       @Autowired DeleteRoomRequestsRepository deleteRequestsRep,
                       @Autowired AddRoomRequestsRepository addRequestsRep,
                       @Autowired DiffieHellmanNumbersRepository numbersRep,
                       @Autowired KafkaMessageHandler messageHandler) {
        this.usersRep = usersRep;
        this.deleteRequestsRep = deleteRequestsRep;
        this.addRequestsRep = addRequestsRep;
        this.numbersRep = numbersRep;
        this.messageHandler = messageHandler;
    }

    @Override
    public void connect(Login request, StreamObserver<ConnectResponse> responseObserver) {
        log.debug("{}: connected", request.getLogin());
        usersRep.putUser(request.getLogin());
        responseObserver.onNext(connectResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void disconnect(Login request, StreamObserver<Empty> responseObserver) {
        log.debug("{}: disconnected", request.getLogin());
        usersRep.deleteUser(request.getLogin());
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void checkCompanionStatus(ChatRoomLogins request, StreamObserver<CompanionStatus> responseObserver) {
        if (usersRep.isActive(request.getCompanionLogin())) {
            log.debug("{} -> {}: companion is online", request.getOwnLogin(), request.getCompanionLogin());
            responseObserver.onNext(CompanionStatus.newBuilder()
                    .setCompanionLogin(request.getCompanionLogin())
                    .setStatus(true)
                    .build());
        } else {
            log.debug("{} -> {}: companion is offline", request.getOwnLogin(), request.getCompanionLogin());
            responseObserver.onNext(CompanionStatus.newBuilder()
                    .setCompanionLogin(request.getCompanionLogin())
                    .setStatus(false)
                    .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void initRoom(InitRoomRequest request, StreamObserver<InitRoomResponse> responseObserver) {
        String ownLogin = request.getOwnLogin();
        String companionLogin = request.getCompanionLogin();

        if (!usersRep.isActive(companionLogin)) {
            responseObserver.onNext(InitRoomResponse.getDefaultInstance());
            responseObserver.onCompleted();
            return;
        }

        if (addRequestsRep.checkIfCompanionRequested(ownLogin, companionLogin)) {
            // request already has been initiated. Now ownLogin must check for it
            responseObserver.onNext(InitRoomResponse.getDefaultInstance());
            responseObserver.onCompleted();
            return;
        }

        InitRoomResponse response = InitRoomResponseBuilder.build(companionLogin, request.getAlgorithm());

        addRequestsRep.putRequest(ownLogin, companionLogin, InitRoomResponseBuilder.buildForCompanion(ownLogin, response));

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
    public void passDiffieHellmanNumber(DiffieHellmanNumber request, StreamObserver<Empty> responseObserver) {
        numbersRep.put(request.getOwnLogin(), request.getCompanionLogin(), request.getNumber());

        log.debug("{} -> {}: sent diffie-hellman number {}", request.getOwnLogin(), request.getCompanionLogin(), request.getNumber());

        responseObserver.onNext(Empty.getDefaultInstance());
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
        }

        responseObserver.onCompleted();
    }

    @Override
    public void deleteRoom(ChatRoomLogins request, StreamObserver<CompanionStatus> responseObserver) {
        if (!usersRep.isActive(request.getCompanionLogin()) || deleteRequestsRep.checkIfCompanionRequested(request.getOwnLogin(), request.getCompanionLogin())) {
            responseObserver.onNext(CompanionStatus.newBuilder()
                    .setCompanionLogin(request.getCompanionLogin())
                    .setStatus(false)
                    .build());
            responseObserver.onCompleted();
            return;
        }

        deleteRequestsRep.putRequest(request.getOwnLogin(), request.getCompanionLogin());

        log.debug("{} -> {}: initiated deletion of chat", request.getOwnLogin(), request.getCompanionLogin());

        responseObserver.onNext(CompanionStatus.newBuilder()
                .setCompanionLogin(request.getCompanionLogin())
                .setStatus(true)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void checkForDeletedRoom(Login request, StreamObserver<CompanionStatus> responseObserver) {
        var op = deleteRequestsRep.getIfAny(request.getLogin());

        if (op.isEmpty()) {
            responseObserver.onNext(CompanionStatus.getDefaultInstance());
            responseObserver.onCompleted();
            return;
        }

        var requesters = op.get();

        for (var deletionInitiator : requesters) {
            log.debug("{} -> {}: companion deleted chat", request.getLogin(), deletionInitiator);
            responseObserver.onNext(CompanionStatus.newBuilder().setCompanionLogin(deletionInitiator).setStatus(true).build());
        }

        responseObserver.onCompleted();
    }

    @Override
    public void sendMessage(MessageToCompanion request, StreamObserver<Empty> responseObserver) {
        if (usersRep.isActive(request.getCompanionLogin())) {
            messageHandler.sendMessage(request.getCompanionLogin(), request.getKafkaMessage());
            log.debug("-> {}: sent message", request.getCompanionLogin());
        }
        responseObserver.onNext(Empty.getDefaultInstance());
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
            log.debug("{} <- {}: got message", request.getLogin(), consumerRecord.topic());
            responseObserver.onNext(MessageToCompanion.newBuilder()
                    .setCompanionLogin("")
                    .setKafkaMessage(consumerRecord.value())
                    .build()
            );
        }

        responseObserver.onCompleted();
    }
}
