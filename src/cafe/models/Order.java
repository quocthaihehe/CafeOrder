package cafe.models;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class Order {
    private int orderId;
    private int table;
    private String orderType = "DINE_IN"; // DINE_IN, TAKEAWAY, DELIVERY
    private List<OrderItem> items;
    private String status; // CREATED, CONFIRMED, PREPARING, READY, SERVED, WAITING_PAYMENT, PAID, COMPLETED, CANCELLED
    private double discountAmount;
    private double vatAmount;
    private String voucherCode;
    private String note;
    private long timestamp;
    private String createdBy;
    private String requestId;
    private List<String> timeline = new ArrayList<>();

    public Order(int orderId, int table, List<OrderItem> items, String status, long timestamp) {
        this(orderId, table, "DINE_IN", items, status, 0.0, 0.0, null, null, timestamp, "Bàn " + (table < 10 ? "0" + table : table), UUID.randomUUID().toString());
    }

    public Order(int orderId, int table, List<OrderItem> items, String status, long timestamp, String createdBy) {
        this(orderId, table, "DINE_IN", items, status, 0.0, 0.0, null, null, timestamp, createdBy, UUID.randomUUID().toString());
    }

    public Order(int orderId, int table, List<OrderItem> items, String status, long timestamp, String createdBy, String requestId) {
        this(orderId, table, "DINE_IN", items, status, 0.0, 0.0, null, null, timestamp, createdBy, requestId);
    }

    public Order(int orderId, int table, String orderType, List<OrderItem> items, String status,
                 double discountAmount, double vatAmount, String voucherCode, String note, long timestamp) {
        this(orderId, table, orderType, items, status, discountAmount, vatAmount, voucherCode, note, timestamp, "Bàn " + (table < 10 ? "0" + table : table), UUID.randomUUID().toString());
    }

    public Order(int orderId, int table, String orderType, List<OrderItem> items, String status,
                 double discountAmount, double vatAmount, String voucherCode, String note, long timestamp,
                 String createdBy, String requestId) {
        this.orderId = orderId;
        this.table = table;
        this.orderType = orderType != null ? orderType : "DINE_IN";
        this.items = items != null ? items : new ArrayList<>();
        this.status = status != null ? status : "CREATED";
        this.discountAmount = discountAmount;
        this.vatAmount = vatAmount;
        this.voucherCode = voucherCode;
        this.note = note;
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

    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { 
        this.status = status; 
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        timeline.add(sdf.format(new Date()) + " - " + status);
    }

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

    public double getSubtotalAmount() {
        if (items == null) return 0.0;
        double total = 0.0;
        for (OrderItem item : items) {
            if (item != null && item.isBillable()) {
                total += item.getSubtotal();
            }
        }
        return total;
    }

    public double getTotalAmount() {
        return Math.max(0, getSubtotalAmount() - discountAmount + vatAmount);
    }

    public double getFinalAmount() {
        return getTotalAmount();
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

    public String getOrderTypeLabel() {
        if ("TAKEAWAY".equalsIgnoreCase(orderType)) {
            return "MANG VỀ";
        } else if ("DELIVERY".equalsIgnoreCase(orderType)) {
            return "GIAO HÀNG";
        }
        return "BÀN " + (table < 10 ? "0" + table : table);
    }

    public long getCreatedAtEpochMs() {
        return timestamp;
    }

    public String getItemsSummaryText() {
        if (items == null || items.isEmpty()) return "Chưa có món";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            OrderItem it = items.get(i);
            if (i > 0) sb.append(", ");
            sb.append(it.getQty()).append("x ").append(it.getName());
        }
        return sb.toString();
    }
}
