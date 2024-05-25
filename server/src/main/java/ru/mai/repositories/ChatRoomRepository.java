package ru.mai.repositories;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import ru.mai.util.CustomPair;

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
            log.debug("Can't create a chat room twice: {} <-> {}", c1, c2);
            return false;
        }
        chatRooms.add(new CustomPair<>(c1, c2));
        return true;
    }

    public void remove(String c1, String c2) {
        chatRooms.remove(new CustomPair<>(c1, c2));
    }

    public boolean contains(String c1, String c2) {
        return chatRooms.contains(new CustomPair<>(c1, c2));
    }
}
