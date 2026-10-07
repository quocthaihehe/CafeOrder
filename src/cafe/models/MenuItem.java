package cafe.models;

public class MenuItem {
    private int id;
    private String name;
    private double price;
    private String category;
    private String description;
    private String iconEmoji;

    public MenuItem(String name, double price) {
        this(0, name, price, "Món khác", "", "☕");
    }

    public MenuItem(int id, String name, double price, String category, String description, String iconEmoji) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.description = description;
        this.iconEmoji = iconEmoji != null && !iconEmoji.isEmpty() ? iconEmoji : "☕";
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

    public String getFormattedPrice() {
        return String.format("%,.0f đ", price);
    }
}
