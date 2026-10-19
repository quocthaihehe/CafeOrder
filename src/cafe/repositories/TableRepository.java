package cafe.repositories;

import cafe.models.CafeTable;
import java.util.List;

public interface TableRepository {
    List<CafeTable> findAll();
    CafeTable findById(int id);
    CafeTable findByTableNumber(int tableNumber);
    boolean existsByTableNumber(int tableNumber);
    CafeTable save(CafeTable table);
    boolean update(CafeTable table);
    boolean setStatus(int id, String status);
    List<CafeTable> searchAndFilter(String keyword, String status);
}
