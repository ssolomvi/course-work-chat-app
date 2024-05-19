package ru.mai.observers;

import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.mai.CompanionStatus;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class CompanionStatusObserver implements StreamObserver<CompanionStatus> {
    @Getter
    private final Map<String, Boolean> companionsAndStatus = new HashMap<>();
    private final CompanionStatus dummy = CompanionStatus.getDefaultInstance();

    @Override
    public void onNext(CompanionStatus value) {
        if (value.equals(dummy)) {
            return;
        }

        companionsAndStatus.put(value.getCompanionLogin(), value.getStatus());
        log.debug("Companion {} is {}", value.getCompanionLogin(), value.getStatus() ? "online" : "offline");
    }

    @Override
    public void onError(Throwable t) {
        log.error("Error occurred, cause: ", t);
    }

    @Override
    public void onCompleted() {
        log.debug("Connection stream ended");
    }

}
