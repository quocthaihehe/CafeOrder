package cafe.services;

import cafe.models.AuditLog;
import cafe.models.CafeTable;
import cafe.models.Device;
import cafe.repositories.AuditLogRepository;
import cafe.repositories.DeviceRepository;
import cafe.repositories.OrderRepository;
import cafe.repositories.TableRepository;
import java.util.Arrays;
import java.util.List;

public class TableService {
    private final TableRepository tableRepository;
    private final DeviceRepository deviceRepository;
    private final OrderRepository orderRepository;
    private final AuditLogRepository auditLogRepository;

    public TableService(TableRepository tableRepository, AuditLogRepository auditLogRepository) {
        this(tableRepository, null, null, auditLogRepository);
    }

    public TableService(TableRepository tableRepository, DeviceRepository deviceRepository, AuditLogRepository auditLogRepository) {
        this(tableRepository, deviceRepository, null, auditLogRepository);
    }

    public TableService(TableRepository tableRepository, DeviceRepository deviceRepository, OrderRepository orderRepository, AuditLogRepository auditLogRepository) {
        this.tableRepository = tableRepository;
        this.deviceRepository = deviceRepository;
        this.orderRepository = orderRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public List<CafeTable> getAllTables() {
        return tableRepository.findAll();
    }

    public List<CafeTable> searchTables(String keyword, String status) {
        return tableRepository.searchAndFilter(keyword, status);
    }

    private boolean isValidTableStatus(String status) {
        if (status == null) return false;
        String s = status.trim().toUpperCase();
        return s.equals("AVAILABLE") || s.equals("OCCUPIED") || s.equals("OFFLINE") || s.equals("INACTIVE");
    }

    private CafeTable findTableUsingDevice(String deviceId, int excludeTableId) {
        if (deviceId == null || deviceId.trim().isEmpty()) return null;
        String target = deviceId.trim();
        for (CafeTable t : tableRepository.findAll()) {
            if (t.getId() != excludeTableId && t.getDeviceId() != null && t.getDeviceId().equalsIgnoreCase(target)) {
                return t;
            }
        }
        return null;
    }

    public String createTable(int tableNumber, String deviceId, String status, String actor) {
        if (tableNumber <= 0 || tableNumber > 999) {
            auditLogRepository.save(new AuditLog(0, null, actor, "CREATE_TABLE", "Bàn " + tableNumber, "FAILED (INVALID_TABLE_NUMBER)"));
            return "Số bàn phải từ 1 đến 999!";
        }
        if (!isValidTableStatus(status)) {
            auditLogRepository.save(new AuditLog(0, null, actor, "CREATE_TABLE", "Bàn " + tableNumber, "FAILED (INVALID_STATUS)"));
            return "Trạng thái bàn không hợp lệ (AVAILABLE, OCCUPIED, OFFLINE, INACTIVE)!";
        }
        if (tableRepository.existsByTableNumber(tableNumber)) {
            auditLogRepository.save(new AuditLog(0, null, actor, "CREATE_TABLE", "Bàn " + tableNumber, "FAILED (DUPLICATE_TABLE)"));
            return "Bàn số " + tableNumber + " đã tồn tại trong hệ thống!";
        }

        String devId = deviceId != null ? deviceId.trim() : "";
        if (!devId.isEmpty()) {
            CafeTable conflictingTable = findTableUsingDevice(devId, -1);
            if (conflictingTable != null) {
                auditLogRepository.save(new AuditLog(0, null, actor, "CREATE_TABLE", "Bàn " + tableNumber + " [" + devId + "]", "FAILED (DUPLICATE_DEVICE_ID)"));
                return "Mã thiết bị '" + devId + "' đang được gán cho Bàn " + conflictingTable.getTableNumber() + "! Không thể gán trùng 1 thiết bị cho 2 bàn.";
            }
            if (deviceRepository != null) {
                Device existingDev = deviceRepository.findByDeviceId(devId);
                if (existingDev != null && existingDev.getTableNumber() > 0 && existingDev.getTableNumber() != tableNumber) {
                    auditLogRepository.save(new AuditLog(0, null, actor, "CREATE_TABLE", "Bàn " + tableNumber + " [" + devId + "]", "FAILED (DEVICE_BUSY)"));
                    return "Thiết bị '" + devId + "' đang được gán cho Bàn " + existingDev.getTableNumber() + "!";
                }
            }
        }

        String normalizedStatus = status.trim().toUpperCase();
        CafeTable table = new CafeTable(0, tableNumber, devId, normalizedStatus, null);
        tableRepository.save(table);

        if (deviceRepository != null && !devId.isEmpty()) {
            Device d = deviceRepository.findByDeviceId(devId);
            if (d != null) {
                deviceRepository.assignToTable(d.getId(), tableNumber);
            } else {
                deviceRepository.save(new Device(0, devId, tableNumber, "ONLINE", null, "ACTIVE"));
            }
        }

        auditLogRepository.save(new AuditLog(0, null, actor, "CREATE_TABLE", "Bàn " + tableNumber, "SUCCESS"));
        return null;
    }

    public String updateTable(int id, int tableNumber, String deviceId, String status, String actor) {
        CafeTable existing = tableRepository.findById(id);
        if (existing == null) {
            auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_TABLE", "ID #" + id, "FAILED (NOT_FOUND)"));
            return "Không tìm thấy bàn!";
        }
        if (tableNumber <= 0 || tableNumber > 999) {
            auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_TABLE", "Bàn " + tableNumber, "FAILED (INVALID_TABLE_NUMBER)"));
            return "Số bàn phải từ 1 đến 999!";
        }
        if (!isValidTableStatus(status)) {
            auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_TABLE", "Bàn " + tableNumber, "FAILED (INVALID_STATUS)"));
            return "Trạng thái bàn không hợp lệ (AVAILABLE, OCCUPIED, OFFLINE, INACTIVE)!";
        }

        CafeTable duplicate = tableRepository.findByTableNumber(tableNumber);
        if (duplicate != null && duplicate.getId() != id) {
            auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_TABLE", "Bàn " + tableNumber, "FAILED (DUPLICATE_TABLE)"));
            return "Số bàn " + tableNumber + " đã được sử dụng!";
        }

        int oldTableNumber = existing.getTableNumber();
        String oldDeviceId = existing.getDeviceId();
        String newDevId = deviceId != null ? deviceId.trim() : "";
        String normalizedStatus = status.trim().toUpperCase();

        // Kiểm tra không cho phép giải phóng/vô hiệu hóa hoặc đổi số bàn khi bàn đang có đơn hoạt động
        if (orderRepository != null) {
            boolean hasActiveOrders = orderRepository.findAll().stream().anyMatch(o ->
                    o.getTable() == oldTableNumber &&
                    !Arrays.asList("COMPLETED", "CANCELLED").contains(o.getStatus().toUpperCase())
            );
            if (hasActiveOrders) {
                if (tableNumber != oldTableNumber) {
                    auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_TABLE", "Bàn " + oldTableNumber, "FAILED (HAS_ACTIVE_ORDERS)"));
                    return "Bàn " + oldTableNumber + " đang có đơn hàng hoạt động, không thể đổi số bàn!";
                }
                if (!"OCCUPIED".equalsIgnoreCase(normalizedStatus)) {
                    auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_TABLE", "Bàn " + oldTableNumber + " -> " + normalizedStatus, "FAILED (HAS_ACTIVE_ORDERS)"));
                    return "Bàn " + oldTableNumber + " đang có đơn hàng chưa hoàn tất, không thể chuyển sang trạng thái '" + normalizedStatus + "'!";
                }
            }
        }

        // Kiểm tra chống gán trùng deviceId với bàn khác
        if (!newDevId.isEmpty()) {
            CafeTable conflictingTable = findTableUsingDevice(newDevId, id);
            if (conflictingTable != null) {
                auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_TABLE", "Bàn " + tableNumber + " [" + newDevId + "]", "FAILED (DUPLICATE_DEVICE_ID)"));
                return "Mã thiết bị '" + newDevId + "' đang được gán cho Bàn " + conflictingTable.getTableNumber() + "! Không thể gán trùng 1 thiết bị cho 2 bàn.";
            }
            if (deviceRepository != null) {
                Device dev = deviceRepository.findByDeviceId(newDevId);
                if (dev != null && dev.getTableNumber() > 0 && dev.getTableNumber() != oldTableNumber && dev.getTableNumber() != tableNumber) {
                    auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_TABLE", "Bàn " + tableNumber + " [" + newDevId + "]", "FAILED (DEVICE_BUSY)"));
                    return "Thiết bị '" + newDevId + "' đang được gán cho Bàn " + dev.getTableNumber() + "!";
                }
            }
        }

        existing.setTableNumber(tableNumber);
        existing.setDeviceId(newDevId);
        existing.setStatus(normalizedStatus);
        tableRepository.update(existing);

        if (deviceRepository != null) {
            if (oldDeviceId != null && !oldDeviceId.isEmpty() && !oldDeviceId.equalsIgnoreCase(newDevId)) {
                Device oldDev = deviceRepository.findByDeviceId(oldDeviceId);
                if (oldDev != null && oldDev.getTableNumber() == oldTableNumber) {
                    deviceRepository.unassign(oldDev.getId());
                }
            }
            if (!newDevId.isEmpty()) {
                Device newDev = deviceRepository.findByDeviceId(newDevId);
                if (newDev != null) {
                    deviceRepository.assignToTable(newDev.getId(), tableNumber);
                } else {
                    deviceRepository.save(new Device(0, newDevId, tableNumber, "ONLINE", null, "ACTIVE"));
                }
            }
        }

        auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_TABLE", "Bàn " + tableNumber + " (" + normalizedStatus + ")", "SUCCESS"));
        return null;
    }
}
