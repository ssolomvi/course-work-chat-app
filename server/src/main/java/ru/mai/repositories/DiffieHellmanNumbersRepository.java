package ru.mai.repositories;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Repository
public class DiffieHellmanNumbersRepository {
    /**
     * key is destination, value is sender
     */
    private Map<String, Map<String, String>> diffieHellmanNumbers;

    public Optional<Map<String, String>> getIfAny(String login) {
        if (diffieHellmanNumbers == null) {
            return Optional.empty();
        }

        var toReturn = Optional.ofNullable(diffieHellmanNumbers.get(login));

        diffieHellmanNumbers.remove(login);

        return toReturn;
    }

    public void put(String login, String companion, String number) {
        if (diffieHellmanNumbers == null) {
            diffieHellmanNumbers = new ConcurrentHashMap<>();
        }

        if (!diffieHellmanNumbers.containsKey(companion)) {
            diffieHellmanNumbers.put(companion, new ConcurrentHashMap<>());
        }

        diffieHellmanNumbers.get(companion).put(login, number);
    }
}
