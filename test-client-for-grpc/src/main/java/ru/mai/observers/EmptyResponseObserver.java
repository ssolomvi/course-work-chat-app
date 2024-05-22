package ru.mai.observers;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import ru.mai.Empty;

@Slf4j
public class EmptyResponseObserver implements StreamObserver<Empty> {
    @Override
    public void onNext(Empty value) {
        //
    }

    @Override
    public void onError(Throwable t) {
        log.error("Error happened, cause: ", t);
    }

    @Override
    public void onCompleted() {
        //
    }
}
