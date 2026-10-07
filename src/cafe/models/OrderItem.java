package cafe.models;

public class OrderItem {
    private String name;
    private int qty;
    private String note;
    private double unitPrice;

    public OrderItem(String name, int qty, String note) {
        this(name, qty, note, 0.0);
    }

    public OrderItem(String name, int qty, String note, double unitPrice) {
        this.name = name;
        this.qty = qty;
        this.note = note;
        this.unitPrice = unitPrice;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public double getSubtotal() {
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
