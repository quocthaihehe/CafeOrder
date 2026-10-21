package cafe.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;

public class MessageProtocol {
    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public static class HelloMessage {
        public String type = MessageType.HELLO.name();
        public int table;
        public String sessionId;
        public String deviceType; // "KIOSK", "STAFF_PAD", "KDS"

        public HelloMessage(int table) {
            this(table, null, "KIOSK");
        }

        public HelloMessage(int table, String sessionId, String deviceType) {
            this.table = table;
            this.sessionId = sessionId;
            this.deviceType = deviceType != null ? deviceType : "KIOSK";
        }
    }

    public static class NewOrderMessage {
        public String type = MessageType.NEW_ORDER.name();
        public int table;
        public String orderType;
        public double discountAmount;
        public String voucherCode;
        public String note;
        public String idempotencyKey;
        public String customerPhone;
        public String customerName;
        public List<OrderItem> items;

        public NewOrderMessage(int table, List<OrderItem> items) {
            this(table, "DINE_IN", 0.0, null, null, items);
        }

        public NewOrderMessage(int table, String orderType, double discountAmount, String voucherCode, String note, List<OrderItem> items) {
            this(table, orderType, discountAmount, voucherCode, note, null, null, null, items);
        }

        public NewOrderMessage(int table, String orderType, double discountAmount, String voucherCode, String note, 
                               String idempotencyKey, String customerPhone, String customerName, List<OrderItem> items) {
            this.table = table;
            this.orderType = orderType != null ? orderType : "DINE_IN";
            this.discountAmount = discountAmount;
            this.voucherCode = voucherCode;
            this.note = note;
            this.idempotencyKey = idempotencyKey;
            this.customerPhone = customerPhone;
            this.customerName = customerName;
            this.items = items;
        }
    }

    public static class OrderAckMessage {
        public String type = MessageType.ORDER_ACK.name();
        public int orderId;
        public String status;
        public int position;
        public boolean persisted = false;
        public String storageMode = "IN_MEMORY"; // "DATABASE" hoac "IN_MEMORY"

        public OrderAckMessage(int orderId, String status, int position) {
            this(orderId, status, position, false, "IN_MEMORY");
        }

        public OrderAckMessage(int orderId, String status, int position, boolean persisted, String storageMode) {
            this.orderId = orderId;
            this.status = status;
            this.position = position;
            this.persisted = persisted;
            this.storageMode = storageMode != null ? storageMode : "IN_MEMORY";
        }
    }

    public static class OrderStatusUpdateMessage {
        public String type = MessageType.ORDER_STATUS_UPDATE.name();
        public int orderId;
        public int table;
        public String status; // CREATED, QUEUED, CONFIRMED, PREPARING, READY, DONE, SERVED, WAITING_PAYMENT, PAID, COMPLETED, CANCELLED

        public OrderStatusUpdateMessage(int orderId, String status) {
            this(orderId, 0, status);
        }

        public OrderStatusUpdateMessage(int orderId, int table, String status) {
            this.orderId = orderId;
            this.table = table;
            this.status = status;
        }
    }

    public static class TableFocusChangeMessage {
        public String type = MessageType.TABLE_FOCUS_CHANGE.name();
        public int newTable;
        public String sessionId;

        public TableFocusChangeMessage(int newTable, String sessionId) {
            this.newTable = newTable;
            this.sessionId = sessionId;
        }
    }

    public static class TableStatusBroadcastMessage {
        public String type = MessageType.TABLE_STATUS_BROADCAST.name();
        public int table;
        public String status; // "AVAILABLE", "OCCUPIED", "RESERVED", "MAINTENANCE"

        public TableStatusBroadcastMessage(int table, String status) {
            this.table = table;
            this.status = status;
        }
    }

    public static class StockStatusBroadcastMessage {
        public String type = MessageType.STOCK_STATUS_BROADCAST.name();
        public int itemId;
        public boolean inStock;
        public String itemName;

        public StockStatusBroadcastMessage(int itemId, boolean inStock, String itemName) {
            this.itemId = itemId;
            this.inStock = inStock;
            this.itemName = itemName;
        }
    }

    public static class SessionKickedMessage {
        public String type = MessageType.SESSION_KICKED.name();
        public String sessionId;
        public String reason;
        public long banMs;
        public long timestamp;

        public SessionKickedMessage() {
            this(null, "Phiên làm việc đã bị ngắt bởi Quản trị viên!", 60000L);
        }

        public SessionKickedMessage(String sessionId, String reason, long banMs) {
            this.sessionId = sessionId;
            this.reason = reason;
            this.banMs = banMs;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public static class PingMessage {
        public String type = MessageType.PING.name();
        public long timestamp = System.currentTimeMillis();
    }

    public static class PongMessage {
        public String type = MessageType.PONG.name();
        public long timestamp = System.currentTimeMillis();
    }

    public static class SyncCatalogMessage {
        public String type = MessageType.SYNC_CATALOG.name();
        public long timestamp = System.currentTimeMillis();
    }

    public static String toJson(Object message) {
        return gson.toJson(message) + "\n";
    }

    public static MessageType getMessageType(String jsonLine) {
        try {
            if (jsonLine == null || jsonLine.trim().isEmpty()) return null;
            JsonObject jsonObject = JsonParser.parseString(jsonLine.trim()).getAsJsonObject();
            if (jsonObject.has("type")) {
                return MessageType.valueOf(jsonObject.get("type").getAsString());
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }

    // Tiện ích dịch ngôn ngữ hiển thị (Việt hóa chuẩn hệ thống CafeOrder)
    public static String translateStatus(String rawStatus) {
        if (rawStatus == null) return "KHÔNG RÕ";
        switch (rawStatus.toUpperCase()) {
            case "CREATED": return "MỚI TẠO";
            case "QUEUED": return "ĐANG CHỜ";
            case "CONFIRMED": return "ĐÃ TIẾP NHẬN";
            case "PREPARING": return "ĐANG PHA CHẾ";
            case "READY":
            case "DONE": return "ĐÃ HOÀN THÀNH";
            case "SERVED": return "ĐÃ PHỤC VỤ";
            case "WAITING_PAYMENT": return "CHỜ THANH TOÁN";
            case "PAID": return "ĐÃ THANH TOÁN";
            case "COMPLETED": return "HOÀN TẤT";
            case "CANCELLED": return "ĐÃ HỦY";
            default: return rawStatus;
        }
    }
}
