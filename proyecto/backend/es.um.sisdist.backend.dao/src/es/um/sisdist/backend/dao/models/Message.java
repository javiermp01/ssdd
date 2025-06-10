package es.um.sisdist.backend.dao.models;

public class Message {
    private String prompt;
    private String response;
    private long timestamp;

    public Message() {
    }

    public Message(String prompt, String response, long timestamp) {
        this.prompt = prompt;
        this.response = response;
        this.timestamp = timestamp;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
