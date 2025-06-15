package es.um.sisdist.backend.dao.conversations;

import java.util.List;
import java.util.Optional;
import es.um.sisdist.backend.dao.models.Conversation;

public interface IConversationsDAO {

    public List<String> getConversationsByUserId(String email);
    public Optional<Conversation> getConversation(String email, String name);
    public Optional<Conversation> createConversation(String email, String name);
    //boolean addPrompt(String email, String name, Prompt prompt);
    public boolean endConversation(String email, String name);
    public boolean addPrompt(String email, String name, String prompt, long timestamp);
    public boolean addResponse(String email, String name, String prompt, String respuesta, long timestamp);
    public List<Conversation> getAllConversationsByEmail(String email);
    public boolean deleteConversationById(String dialogueId);
}
