package es.um.sisdist.backend.dao.conversations;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Logger;
import es.um.sisdist.backend.dao.models.Conversation;
import es.um.sisdist.backend.dao.utils.Lazy;

public class SQLConversationsDAO implements IConversationsDAO {
    Supplier<Connection> conn;

    private static final Logger logger = Logger.getLogger(SQLConversationsDAO.class.getName());

    public SQLConversationsDAO() {
        conn = Lazy.lazily(() -> {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver").getConstructor().newInstance();

                // Si el nombre del host se pasa por environment, se usa aquí.
                // Si no, se usa localhost. Esto permite configurarlo de forma
                // sencilla para cuando se ejecute en el contenedor, y a la vez
                // se pueden hacer pruebas locales
                String sqlServerName = Optional.ofNullable(System.getenv("SQL_SERVER")).orElse("localhost");
                String dbName = Optional.ofNullable(System.getenv("DB_NAME")).orElse("ssdd");
                return DriverManager.getConnection(
                        "jdbc:mysql://" + sqlServerName + "/" + dbName + "?user=root&password=root");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();

                return null;
            }
        });
    }

    // Implementación de los métodos de IConversationesDAO para SQL
    // Aquí irían las consultas SQL y la lógica para manejar las conversaciones

    @Override
    public List<String> getConversationsByUserId(String email) {
        List<String> conversations = new ArrayList<>();

        try (
                PreparedStatement getUserStm = conn.get().prepareStatement(
                        "SELECT id FROM users WHERE email = ?")) {
            getUserStm.setString(1, email);

            try (ResultSet userRs = getUserStm.executeQuery()) {
                if (userRs.next()) {
                    int userId = userRs.getInt("id");

                    try (
                            PreparedStatement getConvsStm = conn.get().prepareStatement(
                                    "SELECT name FROM conversations WHERE user_id = ?")) {
                        getConvsStm.setInt(1, userId);

                        try (ResultSet convsRs = getConvsStm.executeQuery()) {
                            while (convsRs.next()) {
                                conversations.add(convsRs.getString("name"));
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return conversations;
    }

    @Override
    public Optional<Conversation> getConversation(String email, String name) {
        try (
            PreparedStatement getUserStmt = conn.get().prepareStatement(
                "SELECT id FROM users WHERE email = ?")
        ) {
            getUserStmt.setString(1, email);
            try (ResultSet userRs = getUserStmt.executeQuery()) {
                if (!userRs.next()) {
                    return Optional.empty(); // Usuario no encontrado
                }
                int userId = userRs.getInt("id");

                try (PreparedStatement getConvStmt = conn.get().prepareStatement(
                    "SELECT dialogue_id, status, created_at, next_token FROM conversations WHERE user_id = ? AND name = ?")) {
                    getConvStmt.setInt(1, userId);
                    getConvStmt.setString(2, name);

                    try (ResultSet convRs = getConvStmt.executeQuery()) {
                        if (convRs.next()) {
                            int dialogueIdInt = convRs.getInt("dialogue_id");
                            String dialogueId = String.valueOf(dialogueIdInt);
                            String status = convRs.getString("status");
                            long createdAt = convRs.getTimestamp("created_at").getTime();
                            String nextToken = convRs.getString("next_token");

                            // Cargar mensajes asociados a la conversación
                            List<Conversation.Message> dialogue = new ArrayList<>();
                            try (
                                PreparedStatement getMsgsStmt = conn.get().prepareStatement(
                                    "SELECT prompt, response, created_at FROM messages WHERE dialogue_id = ? ORDER BY created_at ASC")
                            ) {
                                getMsgsStmt.setInt(1, dialogueIdInt);
                                try (ResultSet msgsRs = getMsgsStmt.executeQuery()) {
                                    while (msgsRs.next()) {
                                        String prompt = msgsRs.getString("prompt");
                                        String response = msgsRs.getString("response");
                                        long ts = msgsRs.getTimestamp("created_at").getTime();
                                        dialogue.add(new Conversation.Message(prompt, response, ts));
                                    }
                                }
                            }

                            return Optional.of(new Conversation(
                                dialogueId,
                                email,
                                name,
                                status,
                                dialogue,
                                nextToken,
                                createdAt));
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public Optional<Conversation> createConversation(String email, String name) {
        try (
            PreparedStatement getUserStmt = conn.get().prepareStatement(
                "SELECT id FROM users WHERE email = ?")
        ) {
            getUserStmt.setString(1, email);
            try (ResultSet userRs = getUserStmt.executeQuery()) {
                if (!userRs.next()) {
                    return Optional.empty(); // Usuario no encontrado
                }

                int userId = userRs.getInt("id");
                long now = System.currentTimeMillis();
                String nextToken = String.valueOf(now);

                try (
                    PreparedStatement insertStmt = conn.get().prepareStatement(
                        "INSERT INTO conversations (user_id, name, next_token, created_at) VALUES (?, ?, ?, ?)",
                        PreparedStatement.RETURN_GENERATED_KEYS)
                ) {
                    insertStmt.setInt(1, userId);
                    insertStmt.setString(2, name);
                    insertStmt.setString(3, nextToken);
                    insertStmt.setTimestamp(4, new java.sql.Timestamp(now));
                    int affectedRows = insertStmt.executeUpdate();

                    if (affectedRows == 0) {
                        throw new SQLException("No se pudo crear la conversación, ninguna fila afectada.");
                    }

                    try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            String dialogueId = String.valueOf(generatedKeys.getInt(1));

                            return Optional.of(new Conversation(
                                    dialogueId,
                                    email,
                                    name,
                                    "READY",
                                    new ArrayList<>(), // diálogo vacío inicialmente
                                    nextToken, // <-- aquí el token
                                    now));
                        } else {
                            throw new SQLException("No se pudo obtener el ID de la conversación creada.");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public boolean endConversation(String email, String name) {
        // Cambia el estado a FINISHED solo si está READY
        try (PreparedStatement stmt = conn.get().prepareStatement(
                "UPDATE conversations SET status = 'FINISHED' WHERE name = ? AND user_id = (SELECT id FROM users WHERE email = ?) AND status = 'READY'")) {
            stmt.setString(1, name);
            stmt.setString(2, email);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean addPrompt(String email, String name, String prompt, long timestamp) {
        String nextToken = String.valueOf(timestamp);
        try (PreparedStatement stmt = conn.get().prepareStatement(
                "UPDATE conversations SET status = 'BUSY', next_token = ? WHERE name = ? AND user_id = (SELECT id FROM users WHERE email = ?) AND status = 'READY'")) {
            stmt.setString(1, nextToken);
            stmt.setString(2, name);
            stmt.setString(3, email);
            int updated = stmt.executeUpdate();
            if (updated == 0) return false;

            // Añade el mensaje (prompt, respuesta vacía, timestamp)
            try (PreparedStatement insertMsg = conn.get().prepareStatement(
                    "INSERT INTO messages (dialogue_id, prompt, response, created_at) VALUES ((SELECT dialogue_id FROM conversations WHERE name = ? AND user_id = (SELECT id FROM users WHERE email = ?)), ?, '', FROM_UNIXTIME(?))")) {
                insertMsg.setString(1, name);
                insertMsg.setString(2, email);
                insertMsg.setString(3, prompt);
                insertMsg.setLong(4, timestamp / 1000);
                insertMsg.executeUpdate();
            }
            
            // Simula espera de 5 segundos (gRPC)
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Cambia el estado a READY
            try (PreparedStatement readyStmt = conn.get().prepareStatement(
                    "UPDATE conversations SET status = 'READY' WHERE name = ? AND user_id = (SELECT id FROM users WHERE email = ?)")) {
                readyStmt.setString(1, name);
                readyStmt.setString(2, email);
                readyStmt.executeUpdate();
            }
            
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

}
