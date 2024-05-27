package ru.mai.repositories;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Repository
public class ActiveUsersAndConsumersRepository {
    private final Set<String> activeUsers = ConcurrentHashMap.newKeySet();
    /**
     * One consumer per one user
     */
    public boolean isActive(String user) {
        return activeUsers.contains(user);
    }

    public void putUser(String user) {
        if (activeUsers.contains(user)) {
            log.debug("{} is already online", user);
            return;
        }

        activeUsers.add(user);
    }

    public void deleteUser(String user) {
        activeUsers.remove(user);
    }

    public boolean contains(String user) {
        return activeUsers.contains(user);
    }

}
