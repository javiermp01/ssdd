package main.java.TestClient;

import jakarta.ws.rs.client.*;
import jakarta.ws.rs.core.*;
import org.json.JSONObject;
import org.json.JSONArray;

public class TestClient {
    public static void main(String[] args) throws InterruptedException {
        Client client = ClientBuilder.newClient();
        String baseUrl = "http://localhost:8080/Service";

        // 1. Registro de usuario
        JSONObject user = new JSONObject();
        user.put("email", "juan@um.es");
        user.put("password", "juanjuan");
        user.put("name", "juan");

        Response reg = client.target(baseUrl + "/signup")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(user.toString()));
        String regResponseStr = reg.readEntity(String.class);
        System.out.println("Registro: " + reg.getStatus() + " " + regResponseStr);

        String email = "juan@um.es";
        String convName = "miConversacion";

        // 2. Crear conversación
        JSONObject conversation = new JSONObject();
        conversation.put("name", convName);

        Response createConv = client.target(baseUrl + "/u/" + email + "/dialogue")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(conversation.toString()));
        String createConvStr = createConv.readEntity(String.class);
        System.out.println("Crear conversación: " + createConv.getStatus() + " " + createConvStr);

        String nextUrl = null;
        String endUrl = null;

        try {
            JSONObject convDetails = new JSONObject(createConvStr);
            nextUrl = convDetails.getString("nextUrl");
            endUrl = convDetails.getString("endUrl");
        } catch (Exception e) {
            System.out.println("No se pudo obtener nextUrl o endUrl: " + e.getMessage());
        }

        // 3. Enviar prompt, esperar, consultar, finalizar
        if (nextUrl != null && endUrl != null) {
            JSONObject prompt = new JSONObject();
            prompt.put("prompt", "¿Cuál es la capital de Francia?");
            prompt.put("timestamp", System.currentTimeMillis());  // <-- timestamp como número

            Response sendPrompt = client.target(baseUrl + nextUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(prompt.toString()));
            System.out.println("Enviar prompt: " + sendPrompt.getStatus() + " " + sendPrompt.readEntity(String.class));

            // Esperar 7 segundos
            System.out.println("Esperando 7 segundos...");
            Thread.sleep(7000);

            // Obtener conversación actualizada
            Response updatedConv = client.target(baseUrl + "/u/" + email + "/dialogue/" + convName)
                    .request(MediaType.APPLICATION_JSON)
                    .get();
            String updatedConvStr = updatedConv.readEntity(String.class);
            System.out.println("Respuesta actualizada (raw): " + updatedConv.getStatus() + " " + updatedConvStr);

            try {
                JSONObject updatedConvJson = new JSONObject(updatedConvStr);
                JSONArray dialogueArr = updatedConvJson.getJSONArray("dialogue");
                if (dialogueArr.length() > 0) {
                    JSONObject lastEntry = dialogueArr.getJSONObject(dialogueArr.length() - 1);
                    String responseText = lastEntry.optString("response", "Sin respuesta");
                    System.out.println("Respuesta del prompt: " + responseText);
                } else {
                    System.out.println("No hay respuestas en el diálogo.");
                }
            } catch (Exception e) {
                System.out.println("Error al extraer la respuesta: " + e.getMessage());
            }

            // Finalizar conversación
            Response endConv = client.target(baseUrl + endUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(""));
            System.out.println("Finalizar conversación: " + endConv.getStatus() + " " + endConv.readEntity(String.class));
        }

        // 4. Listar conversaciones
        Response list = client.target(baseUrl + "/u/" + email + "/dialogue")
                .request(MediaType.APPLICATION_JSON)
                .get();
        String convListStr = list.readEntity(String.class);
        System.out.println("Conversaciones: " + convListStr);

        // 5. Obtener logs
        Response logsResp = client.target(baseUrl + "/u/" + email + "/dialogue/logs")
                .request(MediaType.APPLICATION_JSON)
                .get();
        String logsStr = logsResp.readEntity(String.class);
        System.out.println("Logs: " + logsStr);

        // 6. Eliminar conversación si hay logs
        try {
            JSONArray logsArr = new JSONArray(logsStr);
            if (logsArr.length() > 0) {
                String name = logsArr.getJSONObject(0).getString("name");
                Response del = client.target(baseUrl + "/u/" + email + "/dialogue/logs/" + name)
                        .request().delete();
                System.out.println("Eliminar conversación: " + del.getStatus() + " " + del.readEntity(String.class));
            } else {
                System.out.println("No hay logs para borrar.");
            }
        } catch (Exception e) {
            System.out.println("Error procesando logs: " + e.getMessage());
        }

        // 7. Eliminar usuario de prueba
        Response deleteUser = client.target(baseUrl + "/u/" + email)
                .request()
                .delete();
        System.out.println("Eliminar usuario: " + deleteUser.getStatus() + " " + deleteUser.readEntity(String.class));

        client.close();
    }
}
