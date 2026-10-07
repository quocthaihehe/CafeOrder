package cafe.models;

public class Voucher {
    private String code;
    private String discountType; // "PERCENT" or "AMOUNT"
    private double discountValue;
    private double minOrderAmount;

    public Voucher(String code, String discountType, double discountValue, double minOrderAmount) {
        this.code = code;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrderAmount = minOrderAmount;
    }

    public String getCode() { return code; }
    public String getDiscountType() { return discountType; }
    public double getDiscountValue() { return discountValue; }
    public double getMinOrderAmount() { return minOrderAmount; }

    public double calculateDiscount(double subtotal) {
        if (subtotal < minOrderAmount) {
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
