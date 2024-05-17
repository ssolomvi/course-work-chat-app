package ru.mai.chat_service;//package ru.mai.chat_service;
//
//import java.util.Objects;
//
//public class ChatRoomTopic {
//    private final String loginCompanion;
//    private final String topicIn; // from where loginCompanion listens
//
//    @Override
//    public boolean equals(Object obj) {
//        if (obj == this) {
//            return true;
//        } else if (!(obj instanceof ChatRoomTopic other)) {
//            return false;
//        } else {
//            return Objects.equals(this.loginCompanion, other.loginCompanion);
//        }
//    }
//
//    @Override
//    public int hashCode() {
//        final int PRIME = 59;
//        return PRIME + (this.loginCompanion == null ? 43 : this.loginCompanion.hashCode());
//    }
//
//    public ChatRoomTopic(String loginCompanion, String topicIn) {
//        this.loginCompanion = loginCompanion;
//        this.topicIn = topicIn;
//    }
//
//
//    public String getLoginCompanion() {
//        return loginCompanion;
//    }
//
//    public String getTopicIn() {
//        return topicIn;
//    }
//}