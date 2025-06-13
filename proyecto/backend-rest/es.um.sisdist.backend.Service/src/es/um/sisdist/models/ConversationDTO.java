package es.um.sisdist.models;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ConversationDTO {
    private String dialogueId;
    private String userId;
    private String name;
    private String status;
    private long createdAt;
    private List<MessageDTO> dialogue;
    private String endUrl;
    private String nextUrl;

    public ConversationDTO() {
    }

    public ConversationDTO(String dialogueId, String userId, String name, String status, String nextToken, long createdAt) {
        super();
        this.dialogueId = dialogueId;
        this.userId = userId;
        this.name = name;
        this.status = status;
        this.createdAt = createdAt;
        this.dialogue = new ArrayList<>();
        this.endUrl = "/u/" + userId + "/dialogue/" + name + "/end";
        this.nextUrl = "/u/" + userId + "/dialogue/" + name + "/next/" + nextToken;
    }

    public List<MessageDTO> getDialogue() {
        return dialogue;
    }

    public void setDialogue(List<MessageDTO> dialogue) {
        this.dialogue = dialogue;
    }

    public String getEndUrl() {
        return endUrl;
    }

    public void setEndUrl(String endUrl) {
        this.endUrl = endUrl;
    }

    public String getNextUrl() {
        return nextUrl;
    }

    public void setNextUrl(String nextUrl) {
        this.nextUrl = nextUrl;
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

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
