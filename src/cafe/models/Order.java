package cafe.models;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class Order {
    private int orderId;
    private int table;
    private List<OrderItem> items;
    private String status; // CREATED, CONFIRMED, PREPARING, READY, SERVED, WAITING_PAYMENT, PAID, COMPLETED, CANCELLED
    private long timestamp;
    private String createdBy;
    private String requestId;
    private List<String> timeline = new ArrayList<>();

    public Order(int orderId, int table, List<OrderItem> items, String status, long timestamp) {
        this(orderId, table, items, status, timestamp, "Bàn " + (table < 10 ? "0" + table : table), UUID.randomUUID().toString());
    }

    public Order(int orderId, int table, List<OrderItem> items, String status, long timestamp, String createdBy) {
        this(orderId, table, items, status, timestamp, createdBy, UUID.randomUUID().toString());
    }

    public Order(int orderId, int table, List<OrderItem> items, String status, long timestamp, String createdBy, String requestId) {
        this.orderId = orderId;
        this.table = table;
        this.items = items != null ? items : new ArrayList<>();
        this.status = status;
        this.timestamp = timestamp;
        this.createdBy = createdBy != null ? createdBy : "Bàn " + table;
        this.requestId = (requestId != null && !requestId.trim().isEmpty()) ? requestId : UUID.randomUUID().toString();
        initDefaultTimeline();
    }

    private void initDefaultTimeline() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        String timeStr = sdf.format(new Date(timestamp));
        timeline.add(timeStr + " - CREATED (Đã tạo đơn)");
        if (!"CREATED".equalsIgnoreCase(status) && !"QUEUED".equalsIgnoreCase(status)) {
            timeline.add(timeStr + " - CONFIRMED (Đã xác nhận)");
        }
        if ("PREPARING".equalsIgnoreCase(status) || "READY".equalsIgnoreCase(status) || "SERVED".equalsIgnoreCase(status) || "WAITING_PAYMENT".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status) || "DONE".equalsIgnoreCase(status)) {
            timeline.add(timeStr + " - PREPARING (Đang pha chế)");
        }
        if ("READY".equalsIgnoreCase(status) || "SERVED".equalsIgnoreCase(status) || "WAITING_PAYMENT".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status) || "DONE".equalsIgnoreCase(status)) {
            timeline.add(timeStr + " - READY (Đã hoàn thành món)");
        }
        if ("SERVED".equalsIgnoreCase(status) || "WAITING_PAYMENT".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status)) {
            timeline.add(timeStr + " - SERVED (Đã phục vụ tại bàn)");
        }
        if ("WAITING_PAYMENT".equalsIgnoreCase(status)) {
            timeline.add(timeStr + " - WAITING_PAYMENT (Yêu cầu thanh toán)");
        }
        if ("PAID".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status)) {
            timeline.add(timeStr + " - PAID (Đã thanh toán)");
            timeline.add(timeStr + " - COMPLETED (Hoàn tất đơn hàng)");
        }
        if ("CANCELLED".equalsIgnoreCase(status)) {
            timeline.add(timeStr + " - CANCELLED (Đã hủy)");
        }
    }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }
    
    public int getTable() { return table; }
    public void setTable(int table) { this.table = table; }
    
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { 
        this.status = status; 
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        timeline.add(sdf.format(new Date()) + " - " + status);
    }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public List<String> getTimeline() { return timeline; }
    public void setTimeline(List<String> timeline) { this.timeline = timeline; }

    public int getTotalQuantity() {
        if (items == null) return 0;
        int count = 0;
        for (OrderItem item : items) {
            if (item != null && item.isBillable() && item.getQty() > 0) {
                count += item.getQty();
            }
        }
        return count;
    }

    public double getTotalAmount() {
        if (items == null) return 0.0;
        double total = 0.0;
        for (OrderItem item : items) {
            if (item != null && item.isBillable()) {
                total += item.getSubtotal();
            }
        }
        return total;
    }

    public String getFormattedTotal() {
        return String.format("%,.0f đ", getTotalAmount());
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date(timestamp));
    }

    public String getFormattedDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return sdf.format(new Date(timestamp));
    }
}
