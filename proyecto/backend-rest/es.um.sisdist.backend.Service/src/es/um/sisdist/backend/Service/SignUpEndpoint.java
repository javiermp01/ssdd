package es.um.sisdist.backend.Service;

import es.um.sisdist.backend.Service.impl.AppLogicImpl;
import es.um.sisdist.models.UserDTO;
import es.um.sisdist.models.UserDTOUtils;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Path("/signup")
public class SignUpEndpoint {

    private AppLogicImpl impl = AppLogicImpl.getInstance();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerUser(UserDTO userDTO) {
        // Llamamos al método para registrar el usuario
        var newUser = impl.registerUser(userDTO.getName(), userDTO.getEmail(), userDTO.getPassword());

        // Si el usuario es registrado con éxito
        if (newUser.isPresent()) {
            return Response.status(Status.CREATED).entity(UserDTOUtils.toDTO(newUser.get())).build();
        } else {
            // Si el usuario ya existe
            return Response.status(Status.BAD_REQUEST).entity("Email already registered.").build();
        }
    }
}
