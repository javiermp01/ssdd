package es.um.sisdist.backend.Service;

import es.um.sisdist.backend.Service.impl.AppLogicImpl;
import es.um.sisdist.models.ConversationDTO;
import es.um.sisdist.models.MessageDTO;
import es.um.sisdist.models.ConversationDTOUtils;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/u/{email}/dialogue")
public class ConversationsEndpoint {

    private AppLogicImpl impl = AppLogicImpl.getInstance();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listConversations(@PathParam("email") String email) {
        var conversations = impl.getConversationsByUserId(email);
        if (conversations == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("No conversations found for user").build();
        }
        return Response.ok(conversations).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createConversation(@PathParam("email") String email, ConversationDTO conv) {
        var conversation = impl.createConversation(email, conv.getName());
        if (conversation != null) {
            return Response.status(Response.Status.CREATED).entity(ConversationDTOUtils.toDTO(conversation.get()))
                    .build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity("No se pudo crear la conversación").build();
        }
    }

    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConversation(
            @PathParam("email") String email,
            @PathParam("name") String name) {
        var conversationOpt = impl.getConversation(email, name);
        if (conversationOpt.isPresent()) {
            return Response.ok(ConversationDTOUtils.toDTO(conversationOpt.get())).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Conversation not found")
                    .build();
        }
    }

    @POST
    @Path("/{name}/end")
    @Produces(MediaType.APPLICATION_JSON)
    public Response endConversation(
            @PathParam("email") String email,
            @PathParam("name") String name) {
        // 1. Obtener la conversación y comprobar estado y token
        var conversationOpt = impl.getConversation(email, name);
        if (conversationOpt.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).entity("Conversación no encontrada").build();
        }
        var conversation = conversationOpt.get();
        if (!"READY".equals(conversation.getStatus())) {
            return Response.status(Response.Status.NO_CONTENT)
                    .entity("La conversación no está en estado READY").build();
        }
        var ended = impl.endConversation(email, name);
        if (ended) {
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("No se puede finalizar la conversación (no está READY o no existe)").build();
        }
    }

    @POST
    @Path("/{name}/next/{nextToken}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response sendPrompt(
            @PathParam("email") String email,
            @PathParam("name") String name,
            @PathParam("nextToken") String nextToken,
            MessageDTO prompt) {
        // 1. Obtener la conversación y comprobar estado y token
        var conversationOpt = impl.getConversation(email, name);
        if (conversationOpt.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).entity("Conversación no encontrada").build();
        }
        var conversation = conversationOpt.get();
        if (!"READY".equals(conversation.getStatus())) {
            return Response.status(Response.Status.NO_CONTENT)
                    .entity("La conversación no está en estado READY").build();
        }
        if (!nextToken.equals(conversation.getNextToken())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Token incorrecto").build();
        }

        // 2. Cambiar estado a BUSY, guardar el prompt, generar nuevo nextToken, llamar a gRPC, etc.
        boolean ok = impl.sendPrompt(email, name, prompt.getPrompt(), prompt.getTimestamp());
        if (!ok) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error enviando prompt").build();
        }

        // 3. Devolver Location con la URL de la conversación
        String location = "/u/" + email + "/dialogue/" + name;
        return Response.status(Response.Status.ACCEPTED)
                .header("Location", location)
                .build();
    }
    /**
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
