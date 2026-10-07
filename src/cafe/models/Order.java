package cafe.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class Order {
    private int orderId;
    private int table;
    private List<OrderItem> items;
    private String status; // QUEUED, PREPARING, DONE
    private long timestamp;

    public Order(int orderId, int table, List<OrderItem> items, String status, long timestamp) {
        this.orderId = orderId;
        this.table = table;
        this.items = items;
        this.status = status;
        this.timestamp = timestamp;
    }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }
    
    public int getTable() { return table; }
    public void setTable(int table) { this.table = table; }
    
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getTotalQuantity() {
        if (items == null) return 0;
        int count = 0;
        for (OrderItem item : items) {
            count += item.getQty();
        }
        return count;
    }

    public double getTotalAmount() {
        if (items == null) return 0.0;
        double total = 0.0;
        for (OrderItem item : items) {
            total += item.getSubtotal();
        }
        return total;
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date(timestamp));
    }
}
