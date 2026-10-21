package cafe.models;

public class ProductSales {
    private String productName;
    private String category;
    private int quantitySold;
    private double revenue;

    public ProductSales(String productName, String category, int quantitySold, double revenue) {
        this.productName = productName;
        this.category = category;
        this.quantitySold = quantitySold;
        this.revenue = revenue;
    }

    public String getProductName() { return productName; }
    public String getCategory() { return category; }
    public int getQuantitySold() { return quantitySold; }
    public double getRevenue() { return revenue; }

    public String getFormattedRevenue() {
        return String.format("%,.0f đ", revenue);
    }
}
