package cafe.models;

public class MenuItem {
    private int id;
    private String name;
    private double price;
    private String category;
    private String description;
    private String iconEmoji;
    private String imagePath;
    private boolean allowSize;
    private boolean isHotAvailable;
    private boolean isBestseller;
    private boolean isNew;
    private int prepMinutes;

    public MenuItem(String name, double price) {
        this(0, name, price, "Món khác", "", "☕", true, false, false, false, 5);
    }

    public MenuItem(int id, String name, double price, String category, String description, String iconEmoji) {
        this(id, name, price, category, description, iconEmoji, true, false, false, false, 5);
    }

    public MenuItem(int id, String name, double price, String category, String description, String iconEmoji,
                    boolean allowSize, boolean isHotAvailable, boolean isBestseller, boolean isNew, int prepMinutes) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
        this.description = description;
        this.iconEmoji = iconEmoji != null && !iconEmoji.isEmpty() ? iconEmoji : "☕";
        this.imagePath = "images/products/" + id + ".jpg";
        this.allowSize = allowSize;
        this.isHotAvailable = isHotAvailable;
        this.isBestseller = isBestseller;
        this.isNew = isNew;
        this.prepMinutes = prepMinutes > 0 ? prepMinutes : 5;
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

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public boolean isAllowSize() { return allowSize; }
    public void setAllowSize(boolean allowSize) { this.allowSize = allowSize; }

    public boolean isHotAvailable() { return isHotAvailable; }
    public void setHotAvailable(boolean hotAvailable) { isHotAvailable = hotAvailable; }

    public boolean isBestseller() { return isBestseller; }
    public void setBestseller(boolean bestseller) { isBestseller = bestseller; }

    public boolean isNew() { return isNew; }
    public void setNew(boolean aNew) { isNew = aNew; }

    public int getPrepMinutes() { return prepMinutes; }
    public void setPrepMinutes(int prepMinutes) { this.prepMinutes = prepMinutes; }

    public String getFormattedPrice() {
        return String.format("%,.0f đ", price);
    }

    public String getImageFileName() {
        return cafe.utils.ImageManager.getProductImageFileName(this);
    }
}

