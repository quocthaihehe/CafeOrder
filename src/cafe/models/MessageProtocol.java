package cafe.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;

public class MessageProtocol {
    private static final Gson gson = new GsonBuilder().create();

    public static class HelloMessage {
        public String type = MessageType.HELLO.name();
        public int table;
        public HelloMessage(int table) { this.table = table; }
    }

    public static class NewOrderMessage {
        public String type = MessageType.NEW_ORDER.name();
        public int table;
        public String orderType;
        public double discountAmount;
        public String voucherCode;
        public String note;
        public List<OrderItem> items;

        public NewOrderMessage(int table, List<OrderItem> items) {
            this(table, "DINE_IN", 0.0, null, null, items);
        }

        public NewOrderMessage(int table, String orderType, double discountAmount, String voucherCode, String note, List<OrderItem> items) {
            this.table = table;
            this.orderType = orderType != null ? orderType : "DINE_IN";
            this.discountAmount = discountAmount;
            this.voucherCode = voucherCode;
            this.note = note;
            this.items = items;
        }
    }

    public static class OrderAckMessage {
        public String type = MessageType.ORDER_ACK.name();
        public int orderId;
        public String status;
        public int position;
        public OrderAckMessage(int orderId, String status, int position) {
            this.orderId = orderId;
            this.status = status;
            this.position = position;
        }
    }

    // Gói tin cập nhật trạng thái (Phase 3)
    public static class OrderStatusUpdateMessage {
        public String type = MessageType.ORDER_STATUS_UPDATE.name();
        public int orderId;
        public String status; // QUEUED, PREPARING, DONE
        
        public OrderStatusUpdateMessage(int orderId, String status) {
            this.orderId = orderId;
            this.status = status;
        }
    }

    public static String toJson(Object message) {
        return gson.toJson(message) + "\n";
    }

    public static MessageType getMessageType(String jsonLine) {
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonLine).getAsJsonObject();
            if (jsonObject.has("type")) {
                return MessageType.valueOf(jsonObject.get("type").getAsString());
            }
        } catch (Exception e) {}
        return null;
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }
    
    // Tiện ích dịch ngôn ngữ hiển thị (Việt hóa)
    public static String translateStatus(String rawStatus) {
        if (rawStatus == null) return "KHÔNG RÕ";
        switch (rawStatus.toUpperCase()) {
            case "QUEUED": return "ĐANG CHỜ";
            case "PREPARING": return "ĐANG PHA CHẾ";
            case "DONE": return "ĐÃ HOÀN THÀNH";
            default: return rawStatus;
        }
    }
}
