package cafe.repositories;

import cafe.models.AuditLog;
import java.util.List;

public interface AuditLogRepository {
    List<AuditLog> findAll();
    AuditLog save(AuditLog log);
    List<AuditLog> searchAndFilter(String keyword, String user, String action, String result);
}
