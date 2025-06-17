package es.um.sisdist.models;

public class MessageDTO {
    private String prompt;
    private String response;
    private long timestamp;

    public MessageDTO() {
    }
    public MessageDTO(String prompt, String response, long timestamp) {
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
