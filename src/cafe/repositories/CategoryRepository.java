package cafe.repositories;

import cafe.models.Category;
import java.util.List;

public interface CategoryRepository {
    List<Category> findAll();
    Category findById(int id);
    Category findByName(String name);
    boolean existsByName(String name);
    Category save(Category category);
    boolean update(Category category);
    boolean setStatus(int id, String status);
    List<Category> search(String keyword);
}
