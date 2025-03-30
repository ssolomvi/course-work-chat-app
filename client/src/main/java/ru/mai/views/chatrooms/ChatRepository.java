package ru.mai.views.chatrooms;

import com.vaadin.flow.component.html.Div;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Scope("prototype")
public class ChatRepository {

    private final Map<String, List<Div>> chats = new ConcurrentHashMap<>();

    public void putMessage(String companion, Div item) {
        List<Div> messages;
        if (chats.containsKey(companion)) {
            messages = chats.get(companion);
        } else {
            messages = new LinkedList<>();
        }

        messages.add(item);
        chats.put(companion, messages);
    }

    public void removeChat(String companion) {
        chats.remove(companion);
    }

    public List<Div> getAllMessages(String companion) {
        return chats.getOrDefault(companion, new LinkedList<>());
    }

}
