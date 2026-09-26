package cafe.models;

public class OrderItem {
    private String name;
    private int qty;
    private String note;

    public OrderItem(String name, int qty, String note) {
        this.name = name;
        this.qty = qty;
        this.note = note;
    }

    public String getName() { return name; }
    public int getQty() { return qty; }
    public String getNote() { return note; }
    
    @Override
    public String toString() {
        return qty + "x " + name + (note != null && !note.isEmpty() ? " (" + note + ")" : "");
    }
}
