package cafe.repositories.mock;

import cafe.models.Device;
import cafe.repositories.DeviceRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MockDeviceRepository implements DeviceRepository {
    private final List<Device> devices = new ArrayList<>();
    private final AtomicInteger idCounter = new AtomicInteger(15);

    public MockDeviceRepository() {
        initMockData();
    }

    private void initMockData() {
        devices.add(new Device(1, "DEV-TB01-A9F", 1, "ONLINE", "2026-10-08 20:30:15", "ACTIVE"));
        devices.add(new Device(2, "DEV-TB02-B8E", 2, "ONLINE", "2026-10-08 20:45:00", "ACTIVE"));
        devices.add(new Device(3, "DEV-TB03-C7D", 3, "ONLINE", "2026-10-08 20:48:32", "ACTIVE"));
        devices.add(new Device(4, "DEV-TB04-D6C", 4, "ONLINE", "2026-10-08 20:20:10", "ACTIVE"));
        devices.add(new Device(5, "DEV-TB05-E5B", 5, "ONLINE", "2026-10-08 20:50:00", "ACTIVE"));
        devices.add(new Device(6, "DEV-TB06-F4A", 6, "ONLINE", "2026-10-08 19:15:22", "ACTIVE"));
        devices.add(new Device(7, "DEV-TB07-G39", 7, "OFFLINE", "2026-10-08 14:00:00", "ACTIVE"));
        devices.add(new Device(8, "DEV-TB08-H28", 8, "ONLINE", "2026-10-08 20:35:10", "ACTIVE"));
        devices.add(new Device(9, "DEV-TB09-J17", 9, "ONLINE", "2026-10-08 20:51:14", "ACTIVE"));
        devices.add(new Device(10, "DEV-TB10-K06", 10, "ONLINE", "2026-10-08 20:12:05", "ACTIVE"));
        devices.add(new Device(11, "DEV-TB11-L95", 11, "ONLINE", "2026-10-08 20:05:44", "ACTIVE"));
        devices.add(new Device(12, "DEV-TB12-M84", 12, "OFFLINE", "2026-10-07 18:20:00", "RESET_REQUIRED"));
        devices.add(new Device(13, "DEV-SPARE-01", 0, "OFFLINE", "2026-10-05 10:00:00", "ACTIVE"));
        devices.add(new Device(14, "DEV-SPARE-02", 0, "OFFLINE", "2026-10-06 11:30:00", "RESET_REQUIRED"));
    }

    @Override
    public List<Device> findAll() {
        return new ArrayList<>(devices);
    }

    @Override
    public Device findById(int id) {
        return devices.stream().filter(d -> d.getId() == id).findFirst().orElse(null);
    }

    @Override
    public Device findByDeviceId(String deviceId) {
        if (deviceId == null) return null;
        return devices.stream().filter(d -> d.getDeviceId().equalsIgnoreCase(deviceId.trim())).findFirst().orElse(null);
    }

    @Override
    public Device findByTableNumber(int tableNumber) {
        return devices.stream().filter(d -> d.getTableNumber() == tableNumber).findFirst().orElse(null);
    }

    @Override
    public synchronized Device save(Device device) {
        device.setId(idCounter.incrementAndGet());
        devices.add(device);
        return device;
    }

    @Override
    public synchronized boolean assignToTable(int id, int tableNumber) {
        Device d = findById(id);
        if (d != null) {
            if (tableNumber > 0) {
                for (Device other : devices) {
                    if (other.getId() != id && other.getTableNumber() == tableNumber) {
                        other.setTableNumber(0);
                    }
                }
            }
            d.setTableNumber(tableNumber);
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean unassign(int id) {
        Device d = findById(id);
        if (d != null) {
            d.setTableNumber(0);
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean resetCredential(int id) {
        Device d = findById(id);
        if (d != null) {
            d.setCredentialStatus("RESET_REQUIRED");
            d.setSecretHash(Device.hashSecret("RESET-" + d.getDeviceId() + "-" + System.nanoTime()));
            return true;
        }
        return false;
    }

    @Override
    public List<Device> searchAndFilter(String keyword, String status) {
        return devices.stream().filter(d -> {
            boolean matchKw = keyword == null || keyword.trim().isEmpty() ||
                    d.getDeviceId().toLowerCase().contains(keyword.toLowerCase().trim()) ||
                    String.valueOf(d.getTableNumber()).contains(keyword.trim());
            boolean matchStatus = status == null || status.trim().isEmpty() || "ALL".equalsIgnoreCase(status.trim()) || "Tất cả".equalsIgnoreCase(status.trim()) || d.getConnectionStatus().equalsIgnoreCase(status.trim());
            return matchKw && matchStatus;
        }).collect(Collectors.toList());
    }
}
