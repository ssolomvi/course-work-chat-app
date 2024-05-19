package ru.mai.observers;

import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.mai.DiffieHellmanNumber;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class DiffieHellmanNumberObserver implements StreamObserver<DiffieHellmanNumber> {
    @Getter
    private final Map<String, String> numbers = new HashMap<>();
    private final DiffieHellmanNumber dummy = DiffieHellmanNumber.getDefaultInstance();

    @Override
    public void onNext(DiffieHellmanNumber value) {
        if (!value.equals(dummy)) {
            numbers.put(value.getCompanionLogin(), value.getNumber());
        }
    }

    @Override
    public void onError(Throwable t) {
        log.error("Error occurred, cause: ", t);
    }

    @Override
    public void onCompleted() {
        //
    }
}

