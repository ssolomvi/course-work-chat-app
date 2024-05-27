package ru.mai.services.repositories;

import org.springframework.stereotype.Repository;
import ru.mai.services.connections.ConnectionHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class CompanionStatusesRepository {
    private final Map<String, Boolean> companionsStatuses = new ConcurrentHashMap<>();

    public void put(String companion, Boolean status) {
        companionsStatuses.put(companion, status);
    }

    public void put(Map<String, Boolean> statuses) {
        companionsStatuses.putAll(statuses);
    }

    public boolean get(String companion) {
        Boolean status = companionsStatuses.get(companion);
        if (status == null) {
            return false;
        }
        return status;
    }
}
