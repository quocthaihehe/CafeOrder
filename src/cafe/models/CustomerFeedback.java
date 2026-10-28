package cafe.models;

import java.util.ArrayList;
import java.util.List;

/**
 * CustomerFeedback - Mô hình Đánh giá & Phản hồi dịch vụ từ khách hàng:
 * - Lưu điểm số sao (1-5 sao)
 * - Tên khách hàng / Tên định danh bàn
 * - Các nhãn đánh giá nhanh (Pha chế ngon, Phục vụ nhanh, Thân thiện...)
 * - Lời nhắn góp ý và thời gian đánh giá
 * - Liên kết với nhân viên phục vụ / quầy pha chế
 * - Hỗ trợ phản hồi từ Quản lý / Chủ quán (Admin Reply)
 */
public class CustomerFeedback {
    private String feedbackId;
    private int orderId;
    private int tableNumber;
    private String customerName; // VD: "Khách Bàn 02", "Anh Hoàng", "Khách Mang Về"
    private String staffUsername;
    private String staffFullName;
    private int rating; // 1 đến 5 sao
    private List<String> tags = new ArrayList<>();
    private String comment;
    private String adminReply; // Lời phản hồi từ Quản lý quán
    private long replyTimestamp;
    private long timestamp;

    public CustomerFeedback() {
        this.timestamp = System.currentTimeMillis();
    }

    public CustomerFeedback(String feedbackId, int orderId, int tableNumber, String staffUsername, String staffFullName, int rating, List<String> tags, String comment, long timestamp) {
        this(feedbackId, orderId, tableNumber, "Khách Bàn " + (tableNumber < 10 ? "0" + tableNumber : tableNumber), staffUsername, staffFullName, rating, tags, comment, null, 0, timestamp);
    }

    public CustomerFeedback(String feedbackId, int orderId, int tableNumber, String customerName,
                            String staffUsername, String staffFullName, int rating,
                            List<String> tags, String comment, String adminReply, long replyTimestamp, long timestamp) {
        this.feedbackId = feedbackId;
        this.orderId = orderId;
        this.tableNumber = tableNumber;
        this.customerName = customerName != null ? customerName : "Khách Bàn " + (tableNumber < 10 ? "0" + tableNumber : tableNumber);
        this.staffUsername = staffUsername != null ? staffUsername : "all";
        this.staffFullName = staffFullName != null ? staffFullName : "Đội ngũ quán";
        this.rating = Math.max(1, Math.min(5, rating));
        this.tags = tags != null ? tags : new ArrayList<>();
        this.comment = comment != null ? comment.trim() : "";
        this.adminReply = adminReply != null ? adminReply.trim() : null;
        this.replyTimestamp = replyTimestamp;
        this.timestamp = timestamp > 0 ? timestamp : System.currentTimeMillis();
    }

    public String getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(String feedbackId) {
        this.feedbackId = feedbackId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(int tableNumber) {
        this.tableNumber = tableNumber;
    }

    public String getCustomerName() {
        if (customerName == null || customerName.trim().isEmpty()) {
            return tableNumber <= 0 ? "Khách Mang Về" : "Khách Bàn " + (tableNumber < 10 ? "0" + tableNumber : tableNumber);
        }
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getStaffUsername() {
        return staffUsername;
    }

    public void setStaffUsername(String staffUsername) {
        this.staffUsername = staffUsername;
    }

    public String getStaffFullName() {
        return staffFullName;
    }

    public void setStaffFullName(String staffFullName) {
        this.staffFullName = staffFullName;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = Math.max(1, Math.min(5, rating));
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getAdminReply() {
        return adminReply;
    }

    public void setAdminReply(String adminReply) {
        this.adminReply = adminReply;
    }

    public long getReplyTimestamp() {
        return replyTimestamp;
    }

    public void setReplyTimestamp(long replyTimestamp) {
        this.replyTimestamp = replyTimestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
