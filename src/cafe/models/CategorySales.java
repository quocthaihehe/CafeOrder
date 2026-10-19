package cafe.models;

public class CategorySales {
    private String categoryName;
    private int orderCount;
    private double revenue;
    private double percentage;

    public CategorySales(String categoryName, int orderCount, double revenue, double percentage) {
        this.categoryName = categoryName;
        this.orderCount = orderCount;
        this.revenue = revenue;
        this.percentage = percentage;
    }

    public String getCategoryName() { return categoryName; }
    public int getOrderCount() { return orderCount; }
    public double getRevenue() { return revenue; }
    public double getPercentage() { return percentage; }

    public String getFormattedRevenue() {
        return String.format("%,.0f đ", revenue);
    }

    public String getFormattedPercentage() {
        return String.format("%.1f%%", percentage);
    }
}
