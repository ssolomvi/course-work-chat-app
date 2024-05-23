package ru.mai.repositories;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import ru.mai.InitRoomResponse;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Repository
public class AddRoomRequestsRepository {
    /**
     * key is who should check, value is who initiated:
     * <p>
     * Map<own login, Map<companion, chat room meta data>>
     */
    private Map<String, Map<String, InitRoomResponse>> initRoomRequests;

    public Optional<Map<String, InitRoomResponse>> getIfAny(String login) {
        if (initRoomRequests == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(initRoomRequests.remove(login));
    }

    public boolean checkIfCompanionRequested(String login, String companionLogin) {
        if (initRoomRequests == null) {
            log.debug("Repository is not initialized");
            return false;
        }

        if (initRoomRequests.containsKey(login)) {
            return initRoomRequests.get(login).containsKey(companionLogin);
        }

        log.debug("{} has no requests", login);
        return false;
    }

    public void putRequest(String login, String companionLogin, InitRoomResponse response) {
        if (initRoomRequests == null) {
            initRoomRequests = new ConcurrentHashMap<>();
        }

        if (!initRoomRequests.containsKey(companionLogin)) {
            initRoomRequests.put(companionLogin, new ConcurrentHashMap<>());
        }

        log.debug("Putting add room request from {} to {}", login, companionLogin);
        initRoomRequests.get(companionLogin).put(login, response);
    }

}
