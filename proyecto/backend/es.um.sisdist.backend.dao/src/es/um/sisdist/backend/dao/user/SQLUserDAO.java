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
import java.util.UUID;
import java.util.function.Supplier;
// import java.util.logging.Logger;

import es.um.sisdist.backend.dao.models.User;
import es.um.sisdist.backend.dao.utils.Lazy;

/**
 * @author dsevilla
 *
 */
public class SQLUserDAO implements IUserDAO {
    Supplier<Connection> conn;

    // private static final Logger logger = Logger.getLogger(SQLUserDAO.class.getName());

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
            e.printStackTrace();
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
            e.printStackTrace();
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

            // Generar un token único utilizando UUID
            String token = UUID.randomUUID().toString(); // Genera un token único

            // Insertar el nuevo usuario en la base de datos en el orden correcto
            String sql = "INSERT INTO users (email, password_hash, name, token, visits) VALUES (?, ?, ?, ?, ?)";
            stm = conn.get().prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            stm.setString(1, email); // correo electrónico
            stm.setString(2, passwordHash); // hash de la contraseña
            stm.setString(3, name); // nombre
            stm.setString(4, token); // token vacío
            stm.setInt(5, 0); // visitas iniciales en 0

            int rowsAffected = stm.executeUpdate(); // Ejecuta la inserción

            if (rowsAffected > 0) {
                // Obtener el ID generado automáticamente para el nuevo usuario
                return getUserByEmail(email); // Devolver el nuevo usuario con el ID generado
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

    // Método para actualizar un usuario en la base de datos
    @Override
    public void updateUser(String email, User user) {
        String query = "UPDATE users SET email = ?, password_hash = ?, name = ? WHERE id = ?";

        try (PreparedStatement stmt = conn.get().prepareStatement(query)){

            stmt.setString(1, user.getEmail());
            stmt.setString(2, user.getPassword_hash());
            stmt.setString(3, user.getName());
            stmt.setString(4, user.getId());

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated == 0) {
                throw new SQLException("No user found with the given email");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Aquí podrías lanzar una excepción personalizada si lo deseas
        }
    }

    @Override
    public boolean deleteUserByEmail(String email) {
        String query = "DELETE FROM users WHERE email = ?";
        try (PreparedStatement stmt = conn.get().prepareStatement(query)) {
            stmt.setString(1, email);
            int rowsDeleted = stmt.executeUpdate();
            return rowsDeleted > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
