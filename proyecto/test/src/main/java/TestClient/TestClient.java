package main.java.TestClient;

import jakarta.ws.rs.client.*;
import jakarta.ws.rs.core.*;
import org.json.JSONObject;
import org.json.JSONArray;

public class TestClient {

    // Método para obtener el texto oficial del código HTTP (ej: "OK" para 200)
    public static String getStatusText(int statusCode) {
        try {
            return Response.Status.fromStatusCode(statusCode).getReasonPhrase();
        } catch (IllegalArgumentException e) {
            return "Unknown Status";
        }
    }

    // Método para colorear códigos HTTP según rango y añadir el texto oficial
    public static String colorStatus(int status) {
        final String ANSI_RESET = "\u001B[0m";
        final String ANSI_RED = "\u001B[31m";
        final String ANSI_GREEN = "\u001B[32m";
        final String ANSI_YELLOW = "\u001B[33m";

        String text = getStatusText(status);
        String coloredStatus;

        if (status >= 200 && status < 300) 
            coloredStatus = ANSI_GREEN + status + " " + text + ANSI_RESET;
        else if (status >= 300 && status < 400) 
            coloredStatus = ANSI_YELLOW + status + " " + text + ANSI_RESET;
        else if (status >= 400) 
            coloredStatus = ANSI_RED + status + " " + text + ANSI_RESET;
        else 
            coloredStatus = status + " " + text;

        return coloredStatus;
    }

    // Función para dar formato (pretty print) a un string JSON (JSONObject o JSONArray)
    public static String prettyPrintJSON(String jsonStr) {
        try {
            if (jsonStr.trim().startsWith("{")) {
                JSONObject json = new JSONObject(jsonStr);
                return json.toString(2);
            } else if (jsonStr.trim().startsWith("[")) {
                JSONArray json = new JSONArray(jsonStr);
                return json.toString(2);
            }
        } catch (Exception e) {
            // Si no es JSON válido, devolver como está
        }
        return jsonStr;
    }

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
        System.out.println("Registro: " + colorStatus(reg.getStatus()) + "\n" + prettyPrintJSON(regResponseStr));

        String email = "juan@um.es";
        String convName = "miConversacion";

        // 2. Crear conversación
        JSONObject conversation = new JSONObject();
        conversation.put("name", convName);

        Response createConv = client.target(baseUrl + "/u/" + email + "/dialogue")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(conversation.toString()));
        String createConvStr = createConv.readEntity(String.class);
        System.out.println("Crear conversación: " + colorStatus(createConv.getStatus()) + "\n" + prettyPrintJSON(createConvStr));

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
            prompt.put("timestamp", System.currentTimeMillis());

            Response sendPrompt = client.target(baseUrl + nextUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(prompt.toString()));
            String sendPromptStr = sendPrompt.readEntity(String.class);
            System.out.println("Enviar prompt: " + colorStatus(sendPrompt.getStatus()) + "\n" + prettyPrintJSON(sendPromptStr));

            // Esperar 7 segundos
            System.out.println("Esperando 7 segundos...");
            Thread.sleep(7000);

            // Obtener conversación actualizada
            Response updatedConv = client.target(baseUrl + "/u/" + email + "/dialogue/" + convName)
                    .request(MediaType.APPLICATION_JSON)
                    .get();
            String updatedConvStr = updatedConv.readEntity(String.class);
            System.out.println("Respuesta actualizada (raw): " + colorStatus(updatedConv.getStatus()) + "\n" + prettyPrintJSON(updatedConvStr));

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
            String endConvStr = endConv.readEntity(String.class);
            System.out.println("Finalizar conversación: " + colorStatus(endConv.getStatus()) + "\n" + prettyPrintJSON(endConvStr));
        }

        // 4. Listar conversaciones
        Response list = client.target(baseUrl + "/u/" + email + "/dialogue")
                .request(MediaType.APPLICATION_JSON)
                .get();
        String convListStr = list.readEntity(String.class);
        System.out.println("Conversaciones:\n" + prettyPrintJSON(convListStr));

        // 5. Obtener logs
        Response logsResp = client.target(baseUrl + "/u/" + email + "/dialogue/logs")
                .request(MediaType.APPLICATION_JSON)
                .get();
        String logsStr = logsResp.readEntity(String.class);
        System.out.println("Logs:\n" + prettyPrintJSON(logsStr));

        // 6. Eliminar conversación si hay logs
        try {
            JSONArray logsArr = new JSONArray(logsStr);
            if (logsArr.length() > 0) {
                String name = logsArr.getJSONObject(0).getString("name");
                Response del = client.target(baseUrl + "/u/" + email + "/dialogue/logs/" + name)
                        .request().delete();
                String delStr = del.readEntity(String.class);
                System.out.println("Eliminar conversación: " + colorStatus(del.getStatus()) + "\n" + delStr);
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
        String deleteUserStr = deleteUser.readEntity(String.class);
        System.out.println("Eliminar usuario: " + colorStatus(deleteUser.getStatus()) + "\n" + deleteUserStr);

        client.close();
    }
}
