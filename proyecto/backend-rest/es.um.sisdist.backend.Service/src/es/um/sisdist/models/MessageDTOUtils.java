package es.um.sisdist.models;

import es.um.sisdist.backend.dao.models.Conversation.Message;

public class MessageDTOUtils {

    public static Message fromDTO(MessageDTO mdto) {
        return new Message(mdto.getPrompt(), mdto.getResponse(), mdto.getTimestamp());
    }

    public static MessageDTO toDTO(Message m) {
        return new MessageDTO(m.getPrompt(), m.getResponse(), m.getTimestamp());
    }
}
