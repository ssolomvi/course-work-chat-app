package ru.mai.observers;

import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.mai.InitRoomResponse;

import java.util.LinkedList;
import java.util.List;

@Slf4j
public class InitRoomObserver implements StreamObserver<InitRoomResponse> {
    @Getter
    private final List<InitRoomResponse> responses = new LinkedList<>();
    private final InitRoomResponse dummy = InitRoomResponse.getDefaultInstance();

    @Override
    public void onNext(InitRoomResponse value) {
        if (!value.equals(dummy)) {
            responses.add(value);
        }
    }

    @Override
    public void onError(Throwable t) {
        log.error("Error happened, cause: ", t);
    }

    @Override
    public void onCompleted() {
        // no need to do anything
    }

}
