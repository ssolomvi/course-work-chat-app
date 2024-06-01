package ru.mai.services.chatroom;

import org.springframework.stereotype.Repository;
import ru.mai.InitRoomResponse;
import ru.mai.utils.Operations;
import ru.mai.utils.Pair;

import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InitMetadataRepository {
    /**
     * key is companion's login, value is a pair of init room response and minor number for diffie-hellman
     */
    private final Map<String, Pair<InitRoomResponse, BigInteger>> metadata = new ConcurrentHashMap<>();

    public void put(String companion, InitRoomResponse response) {
        metadata.put(companion, new Pair<>(response, DiffieHellmanNumbersHandler.generateMinorNumber()));
    }

    public Optional<Pair<InitRoomResponse, BigInteger>> get(String companion) {
        return Optional.ofNullable(metadata.get(companion));
    }

    public void remove(String companion) {
        metadata.remove(companion);
    }

    public void clear() {
        metadata.clear();
    }
}
