package es.um.sisdist.backend.dao.models;

import java.util.ArrayList;
import java.util.List;

public class Conversation {
    private String dialogueId;
    private String userId;
    private String name;
    private String status; // READY, BUSY, FINISHED
    private List<Message> dialogue; // Lista de mensajes (prompt/answer/timestamp)
    private String nextToken;
    private String endUrl;
    private String nextUrl;
    private long createdAt;

    public Conversation() {
        this.dialogue = new ArrayList<>();
        this.status = "READY";
        this.createdAt = System.currentTimeMillis();
    }

    public Conversation(String dialogueId, String userId, String name, String status, long createdAt) {
        this.dialogueId = dialogueId;
        this.userId = userId;
        this.name = name;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Conversation(String dialogueId, String userId, String name, String status, List<Message> dialogue,
                       String nextToken, long createdAt) {
        this.dialogueId = dialogueId;
        this.userId = userId;
        this.name = name;
        this.status = status;
        this.dialogue = dialogue != null ? dialogue : new ArrayList<>();
        this.nextToken = nextToken;
        this.createdAt = createdAt;
    }

    public String getDialogueId() {
        return dialogueId;
    }

    public void setDialogueId(String dialogueId) {
        this.dialogueId = dialogueId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Message> getDialogue() {
        return dialogue;
    }

    public void setDialogue(List<Message> dialogue) {
        this.dialogue = dialogue;
    }

    public String getNextToken() {
        return nextToken;
    }

    public void setNextToken(String nextToken) {
        this.nextToken = nextToken;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getNextUrl() {
        return nextUrl;
    }

    public void setNextUrl(String nextUrl) {
        this.nextUrl = nextUrl;
    }

    public String getEndUrl() {
        return endUrl;
    }

    public void setEndUrl(String endUrl) {
        this.endUrl = endUrl;
    }

    @Override
    public String toString() {
        return "Conversation [dialogueId=" + dialogueId + ", userId=" + userId + ", name=" + name + ", status=" + status
                + ", dialogue=" + dialogue + ", nextToken=" + nextToken + ", createdAt=" + createdAt + "]";
    }

    // Clase interna para los mensajes de la conversación
    public static class Message {
        private String prompt;
        private String answer;
        private long timestamp;

        public Message() {}

        public Message(String prompt, String answer, long timestamp) {
            this.prompt = prompt;
            this.answer = answer;
            this.timestamp = timestamp;
        }

        public String getPrompt() {
            return prompt;
        }

        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return "Message [prompt=" + prompt + ", answer=" + answer + ", timestamp=" + timestamp + "]";
        }
    }
}