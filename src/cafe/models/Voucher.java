package cafe.models;

public class Voucher {
    private String code;
    private String discountType; // "PERCENT" or "AMOUNT"
    private double discountValue;
    private double minOrderAmount;
    private boolean active = true;
    private String expiryDate = "31/12/2026";
    private int usageCount = 18;

    public Voucher(String code, String discountType, double discountValue, double minOrderAmount) {
        this(code, discountType, discountValue, minOrderAmount, true, "31/12/2026", 18);
    }

    public Voucher(String code, String discountType, double discountValue, double minOrderAmount, boolean active, String expiryDate, int usageCount) {
        this.code = code != null ? code.trim().toUpperCase() : "";
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrderAmount = minOrderAmount;
        this.active = active;
        this.expiryDate = expiryDate != null ? expiryDate : "31/12/2026";
        this.usageCount = usageCount;
    }

    public String getCode() { return code; }
    public String getDiscountType() { return discountType; }
    public double getDiscountValue() { return discountValue; }
    public double getMinOrderAmount() { return minOrderAmount; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
    public int getUsageCount() { return usageCount; }
    public void setUsageCount(int usageCount) { this.usageCount = usageCount; }

    public double calculateDiscount(double subtotal) {
        if (!active || subtotal < minOrderAmount) {
            return 0.0;
        }
        if ("PERCENT".equalsIgnoreCase(discountType)) {
            return subtotal * (discountValue / 100.0);
        } else {
            return Math.min(discountValue, subtotal);
        }
    }

    public String getDescription() {
        if ("PERCENT".equalsIgnoreCase(discountType)) {
            return String.format("Giảm %.0f%% cho đơn từ %,.0f đ", discountValue, minOrderAmount);
        } else {
            return String.format("Giảm %,.0f đ cho đơn từ %,.0f đ", discountValue, minOrderAmount);
        }
    }
}
