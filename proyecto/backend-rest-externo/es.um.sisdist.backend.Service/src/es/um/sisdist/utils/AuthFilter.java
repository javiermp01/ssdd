package es.um.sisdist.utils;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import jakarta.xml.bind.DatatypeConverter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Optional;
import com.google.rpc.context.AttributeContext.Response;

import es.um.sisdist.backend.Service.impl.AppLogicImpl;

@Provider
public class AuthFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = Logger.getLogger(AuthFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath(); // ej: "Service/u/dsevilla@um.es"
        LOGGER.info("Filtering request for path: " + path);

        // Solo proteger ciertos endpoints:
        if (!path.contains("dialogue")) {
            LOGGER.info("Ruta no protegida, se permite acceso sin autenticación.");
            return;
        }

        String user = requestContext.getHeaderString("User");
        String date = requestContext.getHeaderString("Date");
        String token = requestContext.getHeaderString("Auth-Token");

        LOGGER.info(String.format("Headers recibidos - User: %s, Date: %s, Auth-Token: %s", user, date, token));

        if (user == null || date == null || token == null) {
            LOGGER.warning("Faltan cabeceras de autenticación. Abortando con 401 Unauthorized.");
            requestContext.abortWith(jakarta.ws.rs.core.Response.status(401).build());
            return;
        }

        java.util.Optional<String> privateToken = getUserPrivateToken(user);
        if (privateToken.isEmpty()) {
            requestContext.abortWith(jakarta.ws.rs.core.Response.status(401).entity("Usuario no encontrado").build());
            return;
        }
        String privateTokenValue = privateToken.get();
        String uri = requestContext.getUriInfo().getRequestUri().toString();

        // Calcular el Auth-Token esperado
        String expectedToken = md5(uri + date + privateTokenValue);
        LOGGER.info(String.format("Cálculo Auth-Token -> URI: %s + Date: %s + TokenPrivado: %s = %s", uri, date, privateTokenValue, expectedToken));

        if (!expectedToken.equals(token)) {
            LOGGER.warning("Token de autenticación no válido. Abortando con 403 Forbidden.");
            requestContext.abortWith(jakarta.ws.rs.core.Response.status(403).build());
            return;
        }

        LOGGER.info("Autenticación correcta. Token validado.");

        // (Opcional) Añadir el token validado al contexto, si se quiere usar más adelante
        requestContext.setProperty("Auth-Token-Validated", expectedToken);
    }

    private java.util.Optional<String> getUserPrivateToken(String userId) {
        java.util.Optional<String> tokenOpt = AppLogicImpl.getInstance().getPrivateTokenByEmail(userId);
        return tokenOpt; // Puedes abortar si prefieres no usar un valor por defecto
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return DatatypeConverter.printHexBinary(hash).toLowerCase();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error calculando MD5", e);
            throw new RuntimeException(e);
        }
    }
}
