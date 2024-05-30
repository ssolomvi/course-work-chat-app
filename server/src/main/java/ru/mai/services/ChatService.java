package ru.mai.services;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mai.*;
import ru.mai.repositories.*;
import ru.mai.util.InitRoomResponseBuilder;

import java.util.List;

@Slf4j
@Component
public class ChatService extends ChatServiceGrpc.ChatServiceImplBase {
    private static final String DIFFIE_HELLMAN_G = "88005553535";
    private final ConnectResponse connectResponse = ConnectResponse.newBuilder().setDiffieHellmanG(DIFFIE_HELLMAN_G).build();
    private final ActiveUsersAndConsumersRepository usersRep;
    private final NotifyDisconnectedUsersRepository notifyDisconnectedRep;
    private final DeleteRoomRequestsRepository deleteRequestsRep;
    private final AddRoomRequestsRepository addRequestsRep;
    private final DiffieHellmanNumbersRepository numbersRep;
    private final ChatRoomRepository chatRep;


    public ChatService(@Autowired ActiveUsersAndConsumersRepository usersRep,
                       @Autowired NotifyDisconnectedUsersRepository notifyDisconnectedRep,
                       @Autowired DeleteRoomRequestsRepository deleteRequestsRep,
                       @Autowired AddRoomRequestsRepository addRequestsRep,
                       @Autowired DiffieHellmanNumbersRepository numbersRep,
                       @Autowired ChatRoomRepository chatRep) {
        this.usersRep = usersRep;
        this.notifyDisconnectedRep = notifyDisconnectedRep;
        this.deleteRequestsRep = deleteRequestsRep;
        this.addRequestsRep = addRequestsRep;
        this.numbersRep = numbersRep;
        this.chatRep = chatRep;
    }

    @Override
    public void connect(Login request, StreamObserver<ConnectResponse> responseObserver) {
        usersRep.putUser(request.getLogin());

        responseObserver.onNext(connectResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void disconnect(Login request, StreamObserver<Status> responseObserver) {
        if (!usersRep.contains(request.getLogin())) {
            log.debug("User {} is already offline / never been created", request.getLogin());
            responseObserver.onNext(Status.newBuilder().setEnumStatus(EnumStatus.ENUM_STATUS_ERROR).build());
            responseObserver.onCompleted();
            return;
        }
        log.debug("{}: disconnected", request.getLogin());
        notifyDisconnectedRep.put(request.getLogin(), chatRep.getAllCompanions(request.getLogin()));
        usersRep.deleteUser(request.getLogin());

        chatRep.removeIfDisconnected(request.getLogin());
        responseObserver.onNext(Status.newBuilder().setEnumStatus(EnumStatus.ENUM_STATUS_OK).build());
        responseObserver.onCompleted();
    }

    @Override
    public void checkDisconnect(Login request, StreamObserver<Login> responseObserver) {
        List<String> disconnectedCompanions = notifyDisconnectedRep.get(request.getLogin());
        if (!disconnectedCompanions.isEmpty()) {
            for (var companion : disconnectedCompanions) {
                notifyDisconnectedRep.remove(companion, request.getLogin());
                responseObserver.onNext(Login.newBuilder().setLogin(companion).build());
            }
        } else {
            responseObserver.onNext(Login.getDefaultInstance());
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
}
