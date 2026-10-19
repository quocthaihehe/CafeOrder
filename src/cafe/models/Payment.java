package cafe.models;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Payment {
    private int id;
    private int orderId;
    private double amount;
    private String method; // "CASH", "CARD", "BANK_TRANSFER"
    private String status; // "SUCCESS", "PENDING", "FAILED"
    private String transactionCode;
    private String processedBy;
    private String createdAt;
    private String paidAt;

    public Payment(int id, int orderId, double amount, String method, String status, String transactionCode, String processedBy, String createdAt, String paidAt) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.method = method != null ? method : "CASH";
        this.status = status != null ? status : "SUCCESS";
        this.transactionCode = transactionCode != null ? transactionCode : "TXN" + System.currentTimeMillis();
        this.processedBy = processedBy != null ? processedBy : "Thu ngân";
        this.createdAt = createdAt != null ? createdAt : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        this.paidAt = paidAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTransactionCode() { return transactionCode; }
    public void setTransactionCode(String transactionCode) { this.transactionCode = transactionCode; }

    public String getProcessedBy() { return processedBy; }
    public void setProcessedBy(String processedBy) { this.processedBy = processedBy; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getPaidAt() { return paidAt; }
    public void setPaidAt(String paidAt) { this.paidAt = paidAt; }

    public String getFormattedAmount() {
        return String.format("%,.0f đ", amount);
    }
}
