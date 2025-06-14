package es.um.sisdist.backend.dao;

import es.um.sisdist.backend.dao.conversations.IConversationsDAO;
import es.um.sisdist.backend.dao.user.IUserDAO;

/**
 * @author dsevilla
 *
 */
public interface IDAOFactory
{
    public IUserDAO createSQLUserDAO();

    public IUserDAO createMongoUserDAO();

    public IConversationsDAO createConversationsDAO();
}
