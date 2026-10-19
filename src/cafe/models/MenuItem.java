package cafe.models;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MenuItem {
    private int id;
    private String name;
    private double price;
    private String category;
    private String description;
    private String iconEmoji;
    private String status = "ACTIVE"; // "ACTIVE", "INACTIVE"
    private String updatedAt;

    public MenuItem(String name, double price) {
        this(0, name, price, "Món khác", "", "☕");
    }

    public MenuItem(int id, String name, double price, String category, String description, String iconEmoji) {
        this(id, name, price, category, description, iconEmoji, "ACTIVE", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));
    }

    public MenuItem(int id, String name, double price, String category, String description, String iconEmoji, String status, String updatedAt) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.description = description;
        this.iconEmoji = iconEmoji != null && !iconEmoji.isEmpty() ? iconEmoji : "☕";
        this.status = status != null ? status : "ACTIVE";
        this.updatedAt = updatedAt != null ? updatedAt : new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIconEmoji() { return iconEmoji; }
    public void setIconEmoji(String iconEmoji) { this.iconEmoji = iconEmoji; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getFormattedPrice() {
        return String.format("%,.0f đ", price);
    }
}
