package cafe.models;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Device {
    private int id;
    private String deviceId;
    private int tableNumber; // 0 if unassigned
    private String connectionStatus; // "ONLINE", "OFFLINE"
    private String lastSeen;
    private String credentialStatus; // "ACTIVE", "RESET_REQUIRED"
    private String secretHash; // SHA-256 hash of device secret/passcode, never plaintext

    public Device(int id, String deviceId, int tableNumber, String connectionStatus, String lastSeen, String credentialStatus) {
        this(id, deviceId, tableNumber, connectionStatus, lastSeen, credentialStatus, hashSecret("SECRET-" + deviceId));
    }

    public Device(int id, String deviceId, int tableNumber, String connectionStatus, String lastSeen, String credentialStatus, String secretHash) {
        this.id = id;
        this.deviceId = deviceId;
        this.tableNumber = tableNumber;
        this.connectionStatus = connectionStatus != null ? connectionStatus : "OFFLINE";
        this.lastSeen = lastSeen != null ? lastSeen : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        this.credentialStatus = credentialStatus != null ? credentialStatus : "ACTIVE";
        this.secretHash = (secretHash != null && !secretHash.trim().isEmpty()) ? secretHash : hashSecret("SECRET-" + deviceId);
    }

    public static String hashSecret(String rawSecret) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((rawSecret != null ? rawSecret : "").getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "sha256_fallback_hash";
        }
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public int getTableNumber() { return tableNumber; }
    public void setTableNumber(int tableNumber) { this.tableNumber = tableNumber; }

    public String getConnectionStatus() { return connectionStatus; }
    public void setConnectionStatus(String connectionStatus) { this.connectionStatus = connectionStatus; }

    public String getLastSeen() { return lastSeen; }
    public void setLastSeen(String lastSeen) { this.lastSeen = lastSeen; }

    public String getCredentialStatus() { return credentialStatus; }
    public void setCredentialStatus(String credentialStatus) { this.credentialStatus = credentialStatus; }

    public String getSecretHash() { return secretHash; }
    public void setSecretHash(String secretHash) { this.secretHash = secretHash; }

    public String getAssignedTableDisplay() {
        return tableNumber > 0 ? "Bàn " + (tableNumber < 10 ? "0" + tableNumber : tableNumber) : "Chưa gán";
    }
}
