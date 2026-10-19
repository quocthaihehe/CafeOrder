package cafe.services;

import cafe.models.AuditLog;
import cafe.repositories.AuditLogRepository;
import java.util.List;

public class AuditLogService {
    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAll();
    }

    public List<AuditLog> searchLogs(String keyword, String user, String action, String result) {
        return auditLogRepository.searchAndFilter(keyword, user, action, result);
    }

    public void log(String actor, String action, String target, String result) {
        auditLogRepository.save(new AuditLog(0, null, actor, action, target, result));
    }
}
