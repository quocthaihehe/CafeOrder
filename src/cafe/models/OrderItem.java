package cafe.models;

public class OrderItem {
    private String name;
    private int qty;
    private String note;
    private double unitPrice;
    private String status = "CONFIRMED"; // CONFIRMED, OUT_OF_STOCK, CANCELLED

    public OrderItem(String name, int qty, String note) {
        this(name, qty, note, 0.0, "CONFIRMED");
    }

    public OrderItem(String name, int qty, String note, double unitPrice) {
        this(name, qty, note, unitPrice, "CONFIRMED");
    }

    public OrderItem(String name, int qty, String note, double unitPrice, String status) {
        this.name = name;
        this.qty = qty;
        this.note = note;
        this.unitPrice = unitPrice;
        this.status = status != null ? status : "CONFIRMED";
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isBillable() {
        return qty > 0
                && Double.isFinite(unitPrice)
                && unitPrice >= 0
                && (status == null || "CONFIRMED".equalsIgnoreCase(status.trim()));
    }

    public double getSubtotal() {
        if (qty <= 0 || !Double.isFinite(unitPrice) || unitPrice < 0) {
            return 0.0;
        }
        return unitPrice * qty;
    }

    public String getFormattedSubtotal() {
        return String.format("%,.0f đ", getSubtotal());
    }

    @Override
    public String toString() {
        return qty + "x " + name + (note != null && !note.isEmpty() ? " (" + note + ")" : "");
    }
}
