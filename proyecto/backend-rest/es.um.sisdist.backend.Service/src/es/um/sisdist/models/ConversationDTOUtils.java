package es.um.sisdist.models;

import java.util.ArrayList;
import java.util.List;

import es.um.sisdist.backend.dao.models.Conversation;

public class ConversationDTOUtils {
    public static Conversation fromDTO(ConversationDTO cdto) {
        return new Conversation(cdto.getDialogueId(), cdto.getUserId(), cdto.getName(),
                cdto.getStatus(), cdto.getCreatedAt());
    }

    public static ConversationDTO toDTO(Conversation c) {
        List<MessageDTO> dialogueDTO = new ArrayList<>();
        if (c.getDialogue() != null) {
            for (Conversation.Message m : c.getDialogue()) {
                dialogueDTO.add(new MessageDTO(m.getPrompt(), m.getAnswer(), m.getTimestamp()));
            }
        }
        ConversationDTO dto = new ConversationDTO(
                c.getDialogueId(),
                c.getUserId(),
                c.getName(),
                c.getStatus(),
                c.getCreatedAt());
        dto.setDialogue(dialogueDTO);
        dto.setEndUrl(c.getEndUrl());
        dto.setNextUrl(c.getNextUrl());
        return dto;
    }
}
