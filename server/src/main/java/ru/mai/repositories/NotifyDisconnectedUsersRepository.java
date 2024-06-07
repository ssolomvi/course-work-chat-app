package ru.mai.repositories;

import org.springframework.stereotype.Repository;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class NotifyDisconnectedUsersRepository {
    // key disconnected, value to notify about
    private final Map<String, List<String>> notifyAbout = new ConcurrentHashMap<>();

    public void put(String user, List<String> users) {
        notifyAbout.put(user, users);
    }

    public List<String> get(String userToNotify) {
        List<String> toNotifyAbout = new LinkedList<>();

        for (var tmp : notifyAbout.entrySet()) {
            if (tmp.getValue().contains(userToNotify)) {
                toNotifyAbout.add(tmp.getKey());
                notifyAbout.remove(userToNotify);
            }
        }
        return toNotifyAbout;
    }

    public void remove(String companion, String userToNotify) {
        List<String> toNotify;
        if ((toNotify = notifyAbout.get(companion)) != null) {
            toNotify.remove(userToNotify);
            if (toNotify.isEmpty()) {
                notifyAbout.remove(companion);
            } else {
                notifyAbout.put(companion, toNotify);
            }
        }
    }
}
