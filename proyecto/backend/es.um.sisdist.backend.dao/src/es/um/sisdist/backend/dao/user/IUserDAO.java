package es.um.sisdist.backend.dao.user;

import java.util.Optional;

import es.um.sisdist.backend.dao.models.User;

public interface IUserDAO
{
    public Optional<User> getUserById(String id);

    public Optional<User> getUserByEmail(String id);

    public Optional<User> registerUser(String name, String email, String passwordHash);

    public void updateUser(String id, User user);

    public boolean deleteUserByEmail(String email);
}
