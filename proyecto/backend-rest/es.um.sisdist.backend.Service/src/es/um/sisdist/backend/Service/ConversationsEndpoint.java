package es.um.sisdist.backend.Service;

import es.um.sisdist.backend.Service.impl.AppLogicImpl;
import es.um.sisdist.models.UserDTO;
import es.um.sisdist.models.UserDTOUtils;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/u/{email}/dialogue")
public class ConversationsEndpoint {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listConversations(@PathParam("email") String email) {
        var conversations = AppLogicImpl.getInstance().getConversationsByUserId(email);
        if (conversations == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("No conversations found for user").build();
        }
        return Response.ok(conversations).build();
    }

    /**
     * @POST
     *       public Response createConversation(@PathParam("userId") String userId,
     *       ConversationDTO data) { ... }
     * 
     * @GET
     *      @Path("/{dialogueId}")
     *      public Response getConversation(@PathParam("userId") String
     *      userId, @PathParam("dialogueId") String dialogueId) { ... }
     * 
     * @POST
     *       @Path("/{dialogueId}/next/{nextToken}")
     *       public Response sendPrompt(@PathParam("userId") String
     *       userId, @PathParam("dialogueId") String
     *       dialogueId, @PathParam("nextToken") String nextToken, PromptDTO prompt)
     *       { ... }
     * 
     * @POST
     *       @Path("/{dialogueId}/end")
     *       public Response endConversation(@PathParam("userId") String
     *       userId, @PathParam("dialogueId") String dialogueId) {
     *       boolean ended = true;//impl.endConversation(userId, dialogueId);
     *       if (ended) {
     *       return Response.ok().build();
     *       } else {
     *       return Response.status(Response.Status.NOT_FOUND).entity("Conversation
     *       not found").build();
     *       }
     *       }
     */
}
