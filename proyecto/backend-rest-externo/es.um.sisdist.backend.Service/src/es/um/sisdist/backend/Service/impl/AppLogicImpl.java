/**
 *
 */
package es.um.sisdist.backend.Service.impl;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import es.um.sisdist.backend.grpc.GrpcServiceGrpc;
import es.um.sisdist.backend.grpc.PingRequest;
import es.um.sisdist.backend.grpc.PromptRequest;
import es.um.sisdist.backend.grpc.PromptResultRequest;
import es.um.sisdist.backend.dao.DAOFactoryImpl;
import es.um.sisdist.backend.dao.IDAOFactory;
import es.um.sisdist.backend.dao.conversations.IConversationsDAO;
import es.um.sisdist.backend.dao.models.Conversation;
import es.um.sisdist.backend.dao.models.Statistics;
import es.um.sisdist.backend.dao.models.User;
import es.um.sisdist.backend.dao.models.utils.UserUtils;
import es.um.sisdist.backend.dao.user.IUserDAO;
import es.um.sisdist.models.UserDTO;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * @author dsevilla
 *
 */
public class AppLogicImpl {
    IDAOFactory daoFactory;
    IUserDAO dao;
    IConversationsDAO conversationsDAO;

    private static final Logger logger = Logger.getLogger(AppLogicImpl.class.getName());

    private final ManagedChannel channel;
    private final GrpcServiceGrpc.GrpcServiceBlockingStub blockingStub;
    // private final GrpcServiceGrpc.GrpcServiceStub asyncStub;

    static AppLogicImpl instance = new AppLogicImpl();

    private AppLogicImpl() {
        daoFactory = new DAOFactoryImpl();
        Optional<String> backend = Optional.ofNullable(System.getenv("DB_BACKEND"));

        if (backend.isPresent() && backend.get().equals("mongo"))
            dao = daoFactory.createMongoUserDAO();
        else {
            dao = daoFactory.createSQLUserDAO();
            conversationsDAO = daoFactory.createConversationsDAO();
        }

        var grpcServerName = Optional.ofNullable(System.getenv("GRPC_SERVER"));
        var grpcServerPort = Optional.ofNullable(System.getenv("GRPC_SERVER_PORT"));

        channel = ManagedChannelBuilder
                .forAddress(grpcServerName.orElse("localhost"), Integer.parseInt(grpcServerPort.orElse("50051")))
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS
                // to avoid needing certificates.
                .usePlaintext().build();
        blockingStub = GrpcServiceGrpc.newBlockingStub(channel);
        // asyncStub = GrpcServiceGrpc.newStub(channel);
    }

    public static AppLogicImpl getInstance() {
        return instance;
    }

    public Optional<User> getUserByEmail(String userId) {
        logger.info("Requested user info for username: " + userId);
        Optional<User> u = dao.getUserByEmail(userId);
        return u;
    }

    public Optional<String> getPrivateTokenByEmail(String email) {
        Optional<User> userOpt = getUserByEmail(email);
        return userOpt.map(User::getToken); // Devuelve el token si el usuario existe
    }

    public Optional<User> getUserById(String userId) {
        return dao.getUserById(userId);
    }

    public boolean ping(int v) {
        logger.info("Issuing ping, value: " + v);

        // Test de grpc, puede hacerse con la BD
        var msg = PingRequest.newBuilder().setV(v).build();
        var response = blockingStub.ping(msg);

        return response.getV() == v;
    }

    // El frontend, a través del formulario de login,
    // envía el usuario y pass, que se convierte a un DTO. De ahí
    // obtenemos la consulta a la base de datos, que nos retornará,
    // si procede,
    public Optional<User> checkLogin(String email, String pass) {
        Optional<User> u = dao.getUserByEmail(email);

        if (u.isPresent()) {
            String hashed_pass = UserUtils.md5pass(pass);
            if (0 == hashed_pass.compareTo(u.get().getPassword_hash()))
            {
                // Si el usuario y pass son correctos, incrementamos el contador de visitas
                dao.incrementLogin(email);
                return u;
            }
        }

        return Optional.empty();
    }

    public Optional<User> registerUser(String name, String email, String password) {
        String hashedPassword = UserUtils.md5pass(password);
        Optional<User> newUser = dao.registerUser(name, email, hashedPassword);

        return newUser;
    }

    public boolean updateUser(String email, UserDTO updatedData) {
        Optional<User> originalUser = dao.getUserByEmail(email);
        logger.info("Updating user: " + email + " with data: " + updatedData.getEmail() + ", "
                + updatedData.getName() + ", " + updatedData.getPassword());
        if (originalUser.isPresent()) {
            logger.info("Original user: " + originalUser);
            User user = new User(
                    originalUser.get().getId(),
                    updatedData.getEmail(),
                    UserUtils.md5pass(updatedData.getPassword()),
                    updatedData.getName(),
                    originalUser.get().getToken(),
                    originalUser.get().getVisits());
            dao.updateUser(email, user);
            return true;
        }
        // Si no encontramos al usuario, devolver false
        return false;
    }

    public boolean deleteUserByEmail(String email) {
        return dao.deleteUserByEmail(email);
    }

    public Object getConversationsByUserId(String email) {
        return conversationsDAO.getConversationsByUserId(email);
    }

    public Optional<Conversation> createConversation(String email, String name) {
        return conversationsDAO.createConversation(email, name);
    }

    public Optional<Conversation> getConversation(String email, String dialogueId) {
        return conversationsDAO.getConversation(email, dialogueId);
    }

    public boolean endConversation(String email, String name) {
        var convOpt = conversationsDAO.getConversation(email, name);
        if (convOpt.isEmpty()) return false;
        var conv = convOpt.get();
        if (!"READY".equals(conv.getStatus())) return false;
        return conversationsDAO.endConversation(email, name);
    }

    public boolean sendPrompt(String email, String name, String prompt, long timestamp) {
        boolean ok = conversationsDAO.addPrompt(email, name, prompt, timestamp);
        if (!ok) return false;

        dao.incrementPrompt(email);

        var grpcRequest = PromptRequest.newBuilder()
            .setPrompt(prompt)
            .setEmail(email)
            .setName(name)
            .build();
        var grpcResponse = blockingStub.sendPrompt(grpcRequest);
        //String taskId = grpcResponse.getTaskId();
        // Opcional: guardar el taskId en la BBDD si quieres rastrear el estado

        return true;
    }

    public Optional<Statistics> getStatistics(String email) {
        return dao.getStatistics(email);
    }

    // Eliminar un log por nombre
    public boolean deleteConversationLog(String name) {
        return conversationsDAO.deleteConversationById(name);
    }

    public List<Conversation> getAllConversationLogsByEmail(String email) {
        return conversationsDAO.getAllConversationsByEmail(email);
    }
}
