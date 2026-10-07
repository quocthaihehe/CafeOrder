package cafe.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class Order {
    private int orderId;
    private int table;
    private String orderType; // DINE_IN, TAKEAWAY
    private List<OrderItem> items;
    private String status; // QUEUED, PREPARING, DONE, PAID, CANCELLED
    private double discountAmount;
    private double vatAmount;
    private String voucherCode;
    private String note;
    private long timestamp;

    public Order(int orderId, int table, List<OrderItem> items, String status, long timestamp) {
        this(orderId, table, "DINE_IN", items, status, 0.0, 0.0, null, null, timestamp);
    }

    public Order(int orderId, int table, String orderType, List<OrderItem> items, String status,
                 double discountAmount, double vatAmount, String voucherCode, String note, long timestamp) {
        this.orderId = orderId;
        this.table = table;
        this.orderType = orderType != null ? orderType : "DINE_IN";
        this.items = items;
        this.status = status != null ? status : "QUEUED";
        this.discountAmount = discountAmount;
        this.vatAmount = vatAmount;
        this.voucherCode = voucherCode;
        this.note = note;
        this.timestamp = timestamp;
    }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }
    
    public int getTable() { return table; }
    public void setTable(int table) { this.table = table; }

    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }

    public double getVatAmount() { return vatAmount; }
    public void setVatAmount(double vatAmount) { this.vatAmount = vatAmount; }

    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    
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

    public double getSubtotalAmount() {
        if (items == null) return 0.0;
        double total = 0.0;
        for (OrderItem item : items) {
            total += item.getSubtotal();
        }
        return total;
    }

    public double getTotalAmount() {
        return Math.max(0, getSubtotalAmount() - discountAmount + vatAmount);
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date(timestamp));
    }

    public String getOrderTypeLabel() {
        if ("TAKEAWAY".equalsIgnoreCase(orderType)) {
            return "MANG VỀ";
        } else if ("DELIVERY".equalsIgnoreCase(orderType)) {
            return "GIAO HÀNG";
        }
        return "BÀN " + (table < 10 ? "0" + table : table);
    }
}
