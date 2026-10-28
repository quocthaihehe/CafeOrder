package cafe.models;

import java.util.ArrayList;
import java.util.List;

/**
 * CustomerFeedback - Mô hình Đánh giá & Phản hồi dịch vụ từ khách hàng:
 * - Lưu điểm số sao (1-5 sao)
 * - Các nhãn đánh giá nhanh (Pha chế ngon, Phục vụ nhanh, Thân thiện...)
 * - Lời nhắn góp ý và thời gian đánh giá
 * - Liên kết với nhân viên phục vụ / quầy pha chế
 */
public class CustomerFeedback {
    private String feedbackId;
    private int orderId;
    private int tableNumber;
    private String staffUsername;
    private String staffFullName;
    private int rating; // 1 đến 5 sao
    private List<String> tags = new ArrayList<>();
    private String comment;
    private long timestamp;

    public CustomerFeedback() {
        this.timestamp = System.currentTimeMillis();
    }

    public CustomerFeedback(String feedbackId, int orderId, int tableNumber, String staffUsername, String staffFullName, int rating, List<String> tags, String comment, long timestamp) {
        this.feedbackId = feedbackId;
        this.orderId = orderId;
        this.tableNumber = tableNumber;
        this.staffUsername = staffUsername != null ? staffUsername : "all";
        this.staffFullName = staffFullName != null ? staffFullName : "Đội ngũ quán";
        this.rating = Math.max(1, Math.min(5, rating));
        this.tags = tags != null ? tags : new ArrayList<>();
        this.comment = comment != null ? comment.trim() : "";
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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
