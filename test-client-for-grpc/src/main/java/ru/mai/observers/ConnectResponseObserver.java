package ru.mai.observers;

import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.mai.ConnectResponse;

@Getter
@Slf4j
public class ConnectResponseObserver implements StreamObserver<ConnectResponse> {
    private String diffieHellmanG;

    @Override
    public void onNext(ConnectResponse value) {
        this.diffieHellmanG = value.getDiffieHellmanG();
    }

    @Override
    public void onError(Throwable t) {
        log.error("Error occurred, cause: ", t);
    }

    @Override
    public void onCompleted() {
        log.debug("Got G: {}", diffieHellmanG);
    }

}
