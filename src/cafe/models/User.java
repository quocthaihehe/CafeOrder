package cafe.models;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;

public class User {
    private int id;
    private String username;
    private String fullName;
    private String password; // Stored as SHA-256 hex hash
    private Role role;
    private String status; // "ACTIVE", "DISABLED"
    private String createdAt;

    public User(int id, String username, String fullName, String password, Role role, String status, String createdAt) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.password = ensureHashed(password);
        this.role = role;
        this.status = status != null ? status : "ACTIVE";
        this.createdAt = createdAt != null ? createdAt : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
    }

    public static String hashPassword(String rawPassword) {
        if (rawPassword == null) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return rawPassword;
        }
    }

    private static String ensureHashed(String pwd) {
        if (pwd == null) return "";
        if (pwd.matches("^[a-fA-F0-9]{64}$")) {
            return pwd.toLowerCase();
        }
        return hashPassword(pwd);
    }

    public boolean verifyPassword(String rawPassword) {
        if (rawPassword == null || this.password == null) return false;
        String candidateHash = ensureHashed(rawPassword);
        return this.password.equalsIgnoreCase(candidateHash);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = ensureHashed(password); }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
