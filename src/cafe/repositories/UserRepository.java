package cafe.repositories;

import cafe.models.Role;
import cafe.models.User;
import java.util.List;

public interface UserRepository {
    List<User> findAll();
    User findById(int id);
    User findByUsername(String username);
    boolean existsByUsername(String username);
    User save(User user);
    boolean update(User user);
    boolean setStatus(int id, String status);
    boolean resetPassword(int id, String newPassword);
    List<User> searchAndFilter(String keyword, Role role, String status);
}
