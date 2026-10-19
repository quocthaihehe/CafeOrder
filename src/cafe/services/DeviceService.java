package cafe.services;

import cafe.models.AuditLog;
import cafe.models.CafeTable;
import cafe.models.Device;
import cafe.repositories.AuditLogRepository;
import cafe.repositories.DeviceRepository;
import cafe.repositories.TableRepository;
import java.util.List;

public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final TableRepository tableRepository;
    private final AuditLogRepository auditLogRepository;

    public DeviceService(DeviceRepository deviceRepository, AuditLogRepository auditLogRepository) {
        this(deviceRepository, null, auditLogRepository);
    }

    public DeviceService(DeviceRepository deviceRepository, TableRepository tableRepository, AuditLogRepository auditLogRepository) {
        this.deviceRepository = deviceRepository;
        this.tableRepository = tableRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public List<Device> getAllDevices() {
        return deviceRepository.findAll();
    }

    public List<Device> searchDevices(String keyword, String status) {
        return deviceRepository.searchAndFilter(keyword, status);
    }

    public String assignDevice(int id, int tableNumber, String actor) {
        Device d = deviceRepository.findById(id);
        if (d == null) {
            auditLogRepository.save(new AuditLog(0, null, actor, "ASSIGN_DEVICE", "ID #" + id, "FAILED (NOT_FOUND)"));
            return "Không tìm thấy thiết bị!";
        }
        if (tableNumber <= 0) {
            auditLogRepository.save(new AuditLog(0, null, actor, "ASSIGN_DEVICE", d.getDeviceId(), "FAILED (INVALID_TABLE_NUMBER)"));
            return "Số bàn cần gán phải lớn hơn 0!";
        }

        CafeTable newTable = null;
        if (tableRepository != null) {
            newTable = tableRepository.findByTableNumber(tableNumber);
            if (newTable == null) {
                auditLogRepository.save(new AuditLog(0, null, actor, "ASSIGN_DEVICE", d.getDeviceId() + " -> Bàn " + tableNumber, "FAILED (TABLE_NOT_FOUND)"));
                return "Không tìm thấy Bàn số " + tableNumber + " trong hệ thống!";
            }
        }

        int oldTable = d.getTableNumber();

        // Gỡ mọi thiết bị khác đang trỏ vào bàn đích (đảm bảo quan hệ 1-1 tuyệt đối)
        for (Device other : deviceRepository.findAll()) {
            if (other.getId() != id && other.getTableNumber() == tableNumber) {
                deviceRepository.unassign(other.getId());
            }
        }

        deviceRepository.assignToTable(id, tableNumber);

        if (tableRepository != null) {
            // Nếu thiết bị trước đó đã gắn vào bàn khác, gỡ khỏi bàn cũ
            if (oldTable > 0 && oldTable != tableNumber) {
                CafeTable prevTable = tableRepository.findByTableNumber(oldTable);
                if (prevTable != null && d.getDeviceId().equalsIgnoreCase(prevTable.getDeviceId())) {
                    prevTable.setDeviceId("");
                    tableRepository.update(prevTable);
                }
            }
            // Gán vào bàn mới
            if (newTable != null) {
                newTable.setDeviceId(d.getDeviceId());
                tableRepository.update(newTable);
            }
        }

        auditLogRepository.save(new AuditLog(0, null, actor, "ASSIGN_DEVICE", d.getDeviceId() + " -> Bàn " + tableNumber, "SUCCESS"));
        return null;
    }

    public String unassignDevice(int id, String actor) {
        Device d = deviceRepository.findById(id);
        if (d == null) {
            auditLogRepository.save(new AuditLog(0, null, actor, "UNASSIGN_DEVICE", "ID #" + id, "FAILED (NOT_FOUND)"));
            return "Không tìm thấy thiết bị!";
        }
        int oldTable = d.getTableNumber();
        if (oldTable <= 0) {
            auditLogRepository.save(new AuditLog(0, null, actor, "UNASSIGN_DEVICE", d.getDeviceId(), "FAILED (ALREADY_UNASSIGNED)"));
            return "Thiết bị '" + d.getDeviceId() + "' hiện chưa được gán vào bàn nào!";
        }

        deviceRepository.unassign(id);

        if (tableRepository != null) {
            CafeTable tbl = tableRepository.findByTableNumber(oldTable);
            if (tbl != null && d.getDeviceId().equalsIgnoreCase(tbl.getDeviceId())) {
                tbl.setDeviceId("");
                tableRepository.update(tbl);
            }
        }

        auditLogRepository.save(new AuditLog(0, null, actor, "UNASSIGN_DEVICE", d.getDeviceId() + " (cũ: Bàn " + oldTable + ")", "SUCCESS"));
        return null;
    }

    public String resetDeviceCredential(int id, String actor) {
        Device d = deviceRepository.findById(id);
        if (d == null) {
            auditLogRepository.save(new AuditLog(0, null, actor, "RESET_DEVICE_SECRET", "ID #" + id, "FAILED (NOT_FOUND)"));
            return "Không tìm thấy thiết bị!";
        }

        deviceRepository.resetCredential(id);
        auditLogRepository.save(new AuditLog(0, null, actor, "RESET_DEVICE_SECRET", d.getDeviceId(), "SUCCESS"));
        return null;
    }
}
