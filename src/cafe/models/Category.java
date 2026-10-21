package cafe.models;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Category {
    private int id;
    private String name;
    private String status; // "ACTIVE", "INACTIVE"
    private String createdAt;

    public Category(int id, String name, String status, String createdAt) {
        this.id = id;
        this.name = name;
        this.status = status != null ? status : "ACTIVE";
        this.createdAt = createdAt != null ? createdAt : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return name;
    }
}
