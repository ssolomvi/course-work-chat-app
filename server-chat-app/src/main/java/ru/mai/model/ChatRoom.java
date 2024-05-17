package ru.mai.model;

import ru.mai.util.Pair;

@lombok.Getter
public class ChatRoom {
    private final Pair<String, String> loginAndTopicIn1;
    private final Pair<String, String> loginAndTopicIn2;

    public ChatRoom(String login1, String login2, String topicIn1, String topicIn2) {
        this.loginAndTopicIn1 = new Pair<>(login1, topicIn1);
        this.loginAndTopicIn2 = new Pair<>(login2, topicIn2);
    }


    @Override
    public int hashCode() {
        return getLoginAndTopicIn1().hashCode() + getLoginAndTopicIn2().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ChatRoom another)) {
            return false;
        }

        if (!(this.loginAndTopicIn1.equals(another.loginAndTopicIn1))
                && (!(this.loginAndTopicIn1.equals(another.loginAndTopicIn2)))) {
            return false;
        }

        return this.loginAndTopicIn2.equals(another.loginAndTopicIn1)
                || (this.loginAndTopicIn2.equals(another.loginAndTopicIn2));
    }
}
