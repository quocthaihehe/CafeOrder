package cafe.repositories;

import cafe.models.Device;
import java.util.List;

public interface DeviceRepository {
    List<Device> findAll();
    Device findById(int id);
    Device findByDeviceId(String deviceId);
    Device findByTableNumber(int tableNumber);
    Device save(Device device);
    boolean assignToTable(int id, int tableNumber);
    boolean unassign(int id);
    boolean resetCredential(int id);
    List<Device> searchAndFilter(String keyword, String status);
}
