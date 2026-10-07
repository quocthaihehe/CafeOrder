package cafe.models;

import java.util.ArrayList;
import java.util.List;

public class OrderItem {
    private int productId;
    private String name;
    private int qty;
    private double basePrice;
    private double unitPrice;
    private String sizeCode; // S, M, L
    private double sizeDelta;
    private int sugarPercent; // 0, 30, 50, 70, 100
    private int icePercent;   // 0, 30, 50, 70, 100
    private String temperature; // COLD, HOT
    private List<ToppingItem> toppings = new ArrayList<>();
    private String note;

    public OrderItem(String name, int qty, String note) {
        this(0, name, qty, 0.0, "M", 0.0, 100, 100, "COLD", new ArrayList<>(), note);
    }

    public OrderItem(String name, int qty, String note, double unitPrice) {
        this(0, name, qty, unitPrice, "M", 0.0, 100, 100, "COLD", new ArrayList<>(), note);
    }

    public OrderItem(int productId, String name, int qty, double basePrice, String sizeCode, double sizeDelta,
                     int sugarPercent, int icePercent, String temperature, List<ToppingItem> toppings, String note) {
        this.productId = productId;
        this.name = name;
        this.qty = qty;
        this.basePrice = basePrice;
        this.sizeCode = sizeCode != null ? sizeCode : "M";
        this.sizeDelta = sizeDelta;
        this.sugarPercent = sugarPercent;
        this.icePercent = icePercent;
        this.temperature = temperature != null ? temperature : "COLD";
        this.toppings = toppings != null ? toppings : new ArrayList<>();
        this.note = note != null ? note : "";

        recalculateUnitPrice();
    }

    public void recalculateUnitPrice() {
        double total = basePrice + sizeDelta;
        if (toppings != null) {
            for (ToppingItem t : toppings) {
                total += t.getPrice();
            }
        }
        this.unitPrice = Math.max(0, total);
    }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }

    public double getBasePrice() { return basePrice; }
    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
        recalculateUnitPrice();
    }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public String getSizeCode() { return sizeCode; }
    public void setSizeCode(String sizeCode) { this.sizeCode = sizeCode; }

    public double getSizeDelta() { return sizeDelta; }
    public void setSizeDelta(double sizeDelta) {
        this.sizeDelta = sizeDelta;
        recalculateUnitPrice();
    }

    public int getSugarPercent() { return sugarPercent; }
    public void setSugarPercent(int sugarPercent) { this.sugarPercent = sugarPercent; }

    public int getIcePercent() { return icePercent; }
    public void setIcePercent(int icePercent) { this.icePercent = icePercent; }

    public String getTemperature() { return temperature; }
    public void setTemperature(String temperature) { this.temperature = temperature; }

    public List<ToppingItem> getToppings() { return toppings; }
    public void setToppings(List<ToppingItem> toppings) {
        this.toppings = toppings != null ? toppings : new ArrayList<>();
        recalculateUnitPrice();
    }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public double getSubtotal() {
        return unitPrice * qty;
    }

    public String getFormattedSubtotal() {
        return String.format("%,.0f đ", getSubtotal());
    }

    public String getCustomizationSummary() {
        List<String> parts = new ArrayList<>();
        if (sizeCode != null && !sizeCode.isEmpty()) {
            parts.add("Size " + sizeCode);
        }
        if ("HOT".equalsIgnoreCase(temperature)) {
            parts.add("Nóng");
        } else {
            parts.add("Đá " + icePercent + "%");
        }
        if (sugarPercent != 100) {
            parts.add(sugarPercent + "% đường");
        }
        if (toppings != null && !toppings.isEmpty()) {
            List<String> topNames = new ArrayList<>();
            for (ToppingItem t : toppings) {
                topNames.add(t.getName());
            }
            parts.add("+" + String.join(", ", topNames));
        }
        if (note != null && !note.trim().isEmpty()) {
            parts.add("«" + note.trim() + "»");
        }
        return String.join(" • ", parts);
    }

    @Override
    public String toString() {
        String custom = getCustomizationSummary();
        return qty + "x " + name + (custom.isEmpty() ? "" : " (" + custom + ")");
    }
}
