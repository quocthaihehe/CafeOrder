package cafe.repositories;

import cafe.models.MenuItem;
import java.util.List;

public interface MenuItemRepository {
    List<MenuItem> findAll();
    MenuItem findById(int id);
    MenuItem findByName(String name);
    boolean existsByName(String name);
    MenuItem save(MenuItem item);
    boolean update(MenuItem item);
    boolean setStatus(int id, String status);
    List<MenuItem> searchAndFilter(String keyword, String category, String status);
    List<String> getCategoryNames();
}
