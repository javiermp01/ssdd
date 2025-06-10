package es.um.sisdist.backend.dao.conversations;

import java.util.List;
import java.util.Optional;
import es.um.sisdist.backend.dao.models.Conversation;

public interface IConversationsDAO {

    public List<String> getConversationsByUserId(String email);
    public Optional<Conversation> getConversation(String email, String dialogueId);
    public Conversation createConversation(String email, String name);
    //boolean addPrompt(String email, String dialogueId, Prompt prompt);
    public boolean endConversation(String email, String dialogueId);
}
