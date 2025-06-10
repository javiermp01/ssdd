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
    public Optional<Conversation> getConversation(String email, String dialogueId) {
        // Aquí se implementaría la lógica para obtener una conversación específica
        // utilizando consultas SQL.
        return Optional.empty(); // Retorna un Optional vacío como placeholder
    }

    @Override
public Conversation createConversation(String email, String name) {
    try (
        PreparedStatement getUserStmt = conn.get().prepareStatement(
            "SELECT id FROM users WHERE email = ?")
    ) {
        getUserStmt.setString(1, email);
        try (ResultSet userRs = getUserStmt.executeQuery()) {
            if (!userRs.next()) {
                return null; // Usuario no encontrado
            }

            String userId = String.valueOf(userRs.getInt("id"));

            try (
                PreparedStatement insertStmt = conn.get().prepareStatement(
                    "INSERT INTO conversations (user_id, name) VALUES (?, ?)",
                    PreparedStatement.RETURN_GENERATED_KEYS)
            ) {
                insertStmt.setInt(1, Integer.parseInt(userId));
                insertStmt.setString(2, name);
                int affectedRows = insertStmt.executeUpdate();

                if (affectedRows == 0) {
                    throw new SQLException("No se pudo crear la conversación, ninguna fila afectada.");
                }

                try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        String dialogueId = String.valueOf(generatedKeys.getInt(1));
                        long now = System.currentTimeMillis();

                        return new Conversation(
                            dialogueId,
                            userId,
                            name,
                            "READY",
                            new ArrayList<>(), // diálogo vacío inicialmente
                            null, // nextToken
                            now
                        );
                    } else {
                        throw new SQLException("No se pudo obtener el ID de la conversación creada.");
                    }
                }
            }
        }
    } catch (SQLException e) {
        e.printStackTrace();
        return null;
    }
}


    // boolean addPrompt(String email, String dialogueId, Prompt prompt);

    @Override
    public boolean endConversation(String email, String dialogueId) {
        // Aquí se implementaría la lógica para finalizar una conversación
        // utilizando consultas SQL.
        // Retornaría true si la operación fue exitosa, false en caso contrario.
        return true; // Placeholder para indicar que la conversación se ha finalizado correctamente
    }

}
