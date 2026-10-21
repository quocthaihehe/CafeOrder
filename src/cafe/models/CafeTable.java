package cafe.models;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CafeTable {
    private int id;
    private int tableNumber;
    private String deviceId;
    private String status; // "AVAILABLE", "OCCUPIED", "OFFLINE"
    private String createdAt;
    private String deviceSecretHash; // Mã băm mật mã xác thực Kiosk (Không lưu plaintext)

    public CafeTable(int id, int tableNumber, String deviceId, String status, String createdAt) {
        this(id, tableNumber, deviceId, status, createdAt, null);
    }

    public CafeTable(int id, int tableNumber, String deviceId, String status, String createdAt, String deviceSecretHash) {
        this.id = id;
        this.tableNumber = tableNumber;
        this.deviceId = deviceId != null ? deviceId : "";
        this.status = status != null ? status : "AVAILABLE";
        this.createdAt = createdAt != null ? createdAt : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
        this.deviceSecretHash = deviceSecretHash;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTableNumber() { return tableNumber; }
    public void setTableNumber(int tableNumber) { this.tableNumber = tableNumber; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId != null ? deviceId : ""; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getDeviceSecretHash() { return deviceSecretHash; }
    public void setDeviceSecretHash(String deviceSecretHash) { this.deviceSecretHash = deviceSecretHash; }

    public String getDisplayName() {
        return "Bàn " + (tableNumber < 10 ? "0" + tableNumber : tableNumber);
    }
}
