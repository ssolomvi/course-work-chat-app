package ru.mai.repositories;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Repository
public class DeleteRoomRequestsRepository {

    /**
     * key is to notify, value is who deleted
     * <p>
     * Map<own login, Set<companion login>>
     */
    private Map<String, Set<String>> deleteRoomRequests;

    public Optional<Set<String>> getIfAny(String login) {
        if (deleteRoomRequests == null) {
            return Optional.empty();
        }

        var toReturn = Optional.ofNullable(deleteRoomRequests.get(login));

        deleteRoomRequests.remove(login);

        return toReturn;
    }

    public boolean checkIfCompanionRequested(String login, String companionLogin) {
        if (deleteRoomRequests == null) {
            return false;
        }

        if (deleteRoomRequests.containsKey(login)) {
            return deleteRoomRequests.get(login).contains(companionLogin);
        }
        return false;
    }

    public void putRequest(String login, String companion) {
        if (deleteRoomRequests == null) {
            deleteRoomRequests = new ConcurrentHashMap<>();
        }

        if (!deleteRoomRequests.containsKey(companion)) {
            deleteRoomRequests.put(companion, ConcurrentHashMap.newKeySet());
        }

        deleteRoomRequests.get(companion).add(login);
        log.debug("Put request to delete room: {} -> {}", login, companion);
    }

}
