/**
 *
 */
package es.um.sisdist.backend.dao.user;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.function.Supplier;

import es.um.sisdist.backend.dao.models.User;
import es.um.sisdist.backend.dao.utils.Lazy;

/**
 * @author dsevilla
 *
 */
public class SQLUserDAO implements IUserDAO {
    Supplier<Connection> conn;

    public SQLUserDAO() {
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

    @Override
    public Optional<User> getUserById(String id) {
        PreparedStatement stm;
        try {
            stm = conn.get().prepareStatement("SELECT * from users WHERE id = ?");
            stm.setString(1, id);
            ResultSet result = stm.executeQuery();
            if (result.next())
                return createUser(result);
        } catch (SQLException e) {
            // Fallthrough
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> getUserByEmail(String id) {
        PreparedStatement stm;
        try {
            stm = conn.get().prepareStatement("SELECT * from users WHERE email = ?");
            stm.setString(1, id);
            ResultSet result = stm.executeQuery();
            if (result.next())
                return createUser(result);
        } catch (SQLException e) {
            // Fallthrough
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> registerUser(String name, String email, String passwordHash) {
        PreparedStatement stm;
        try {
            // Verificar si el usuario ya existe
            Optional<User> existingUser = getUserByEmail(email);
            if (existingUser.isPresent()) {
                return Optional.empty(); // El correo ya está en uso
            }

            // Generar un id único para el usuario (por ejemplo, un UUID o un hash del
            // email)
            String userId = generateUniqueId(email);

            // Insertar el nuevo usuario en la base de datos en el orden correcto
            String sql = "INSERT INTO users (id, email, password_hash, name, token, visits) VALUES (?, ?, ?, ?, ?, ?)";
            stm = conn.get().prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            stm.setString(1, userId); // id generado
            stm.setString(2, email); // correo electrónico
            stm.setString(3, passwordHash); // hash de la contraseña
            stm.setString(4, name); // nombre
            stm.setString(5, ""); // token vacío
            stm.setInt(6, 0); // visitas iniciales en 0

            int rowsAffected = stm.executeUpdate(); // Ejecuta la inserción

            if (rowsAffected > 0) {
                // Obtener el ID generado automáticamente para el nuevo usuario
                return getUserById(userId); // Devolver el nuevo usuario con el ID generado
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Manejo de errores
        }
        return Optional.empty(); // Si algo falla, devolvemos un Optional vacío
    }

    private Optional<User> createUser(ResultSet result) {
        try {
            return Optional.of(new User(result.getString(1), // id
                    result.getString(2), // email
                    result.getString(3), // pwhash
                    result.getString(4), // name
                    result.getString(5), // token
                    result.getInt(6))); // visits
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    // Método para generar un id único (por ejemplo, usando un hash del correo)
    private String generateUniqueId(String email) {
        return "user_" + email.hashCode(); // O puedes usar UUID.randomUUID().toString()
    }
}
