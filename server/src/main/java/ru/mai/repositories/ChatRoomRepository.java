package ru.mai.repositories;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import ru.mai.util.CustomPair;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Repository
public class ChatRoomRepository {
    private final Set<CustomPair<String>> chatRooms = ConcurrentHashMap.newKeySet();

    public boolean put(String c1, String c2) {
        if (c1.equals(c2)) {
            log.debug("Can't create a chat room with yourself: {}", c1);
            return false;
        }
        if (chatRooms.contains(new CustomPair<>(c1, c2))) {
            log.debug("Chat room {} <-> {} has already been created", c1, c2);
            return false;
        }
        chatRooms.add(new CustomPair<>(c1, c2));
        return true;
    }

    public void remove(String c1, String c2) {
        chatRooms.remove(new CustomPair<>(c1, c2));
    }

    // invoked if user disconnects from server, 'cause we treat connected after disconnection user as newly created
    public void removeIfDisconnected(String login) {
        chatRooms.removeIf(pair ->
        {
            log.debug("Deleting chat room {} <-> {}, 'cause {} disconnected", pair.getKey(), pair.getValue(), login);
            return pair.getKey().equals(login) || pair.getValue().equals(login);
        });
    }

    public boolean contains(String c1, String c2) {
        return chatRooms.contains(new CustomPair<>(c1, c2));
    }

    public List<String> getAllCompanions(String login) {
        List<String> result = new LinkedList<>();
        for (var pair : chatRooms) {
            if (pair.getValue().equals(login)) {
                result.add(pair.getKey());
            } else if (pair.getKey().equals(login)) {
                result.add(pair.getValue());
            }
        }
        return result;
    }
}
