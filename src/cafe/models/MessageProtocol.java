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
        public String staffUsername;

        public HelloMessage(int table) {
            this(table, null, "KIOSK");
        }

        public HelloMessage(int table, String sessionId, String deviceType) {
            this(table, sessionId, deviceType, null);
        }

        public HelloMessage(int table, String sessionId, String deviceType, String staffUsername) {
            this.table = table;
            this.sessionId = sessionId;
            this.deviceType = deviceType != null ? deviceType : "KIOSK";
            this.staffUsername = staffUsername;
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
        public Order updatedOrder;

        public OrderStatusUpdateMessage() {}

        public OrderStatusUpdateMessage(int orderId, String status) {
            this(orderId, 0, status, null);
        }

        public OrderStatusUpdateMessage(int orderId, int table, String status) {
            this(orderId, table, status, null);
        }

        public OrderStatusUpdateMessage(int orderId, int table, String status, Order updatedOrder) {
            this.orderId = orderId;
            this.table = table;
            this.status = status;
            this.updatedOrder = updatedOrder;
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
        public String status; // "EMPTY", "AVAILABLE", "OCCUPIED", "SERVING", "RESERVED", "CLEANING"
        public List<Order> tableOrders;
        public cafe.services.CafeServerService.ReservationInfo reservation;

        public TableStatusBroadcastMessage() {}

        public TableStatusBroadcastMessage(int table, String status) {
            this(table, status, null, null);
        }

        public TableStatusBroadcastMessage(int table, String status, List<Order> tableOrders, cafe.services.CafeServerService.ReservationInfo reservation) {
            this.table = table;
            this.status = status;
            this.tableOrders = tableOrders;
            this.reservation = reservation;
        }
    }

    public static class TableActionSyncMessage {
        public String type = MessageType.TABLE_ACTION_SYNC.name();
        public String action; // "TRANSFER", "MERGE", "RESERVE", "CANCEL_RESERVE", "SET_STATUS", "CHECKOUT", "CANCEL_ORDER", "RESET_SEED"
        public int fromTable;
        public int toTable;
        public String status;
        public int orderId;
        public String customerName;
        public String phone;
        public String reservationTime;
        public int guestCount;
        public String note;

        public TableActionSyncMessage() {}

        public static TableActionSyncMessage forTransfer(int fromTable, int toTable) {
            TableActionSyncMessage m = new TableActionSyncMessage();
            m.action = "TRANSFER";
            m.fromTable = fromTable;
            m.toTable = toTable;
            return m;
        }

        public static TableActionSyncMessage forMerge(int sourceTable, int targetTable) {
            TableActionSyncMessage m = new TableActionSyncMessage();
            m.action = "MERGE";
            m.fromTable = sourceTable;
            m.toTable = targetTable;
            return m;
        }

        public static TableActionSyncMessage forReserve(int table, String customerName, String phone, String time, int guests, String note) {
            TableActionSyncMessage m = new TableActionSyncMessage();
            m.action = "RESERVE";
            m.fromTable = table;
            m.customerName = customerName;
            m.phone = phone;
            m.reservationTime = time;
            m.guestCount = guests;
            m.note = note;
            return m;
        }

        public static TableActionSyncMessage forCancelReserve(int table) {
            TableActionSyncMessage m = new TableActionSyncMessage();
            m.action = "CANCEL_RESERVE";
            m.fromTable = table;
            return m;
        }

        public static TableActionSyncMessage forSetStatus(int table, String status) {
            TableActionSyncMessage m = new TableActionSyncMessage();
            m.action = "SET_STATUS";
            m.fromTable = table;
            m.status = status;
            return m;
        }

        public static TableActionSyncMessage forCheckout(int table) {
            TableActionSyncMessage m = new TableActionSyncMessage();
            m.action = "CHECKOUT";
            m.fromTable = table;
            return m;
        }

        public static TableActionSyncMessage forCancelOrder(int orderId) {
            TableActionSyncMessage m = new TableActionSyncMessage();
            m.action = "CANCEL_ORDER";
            m.orderId = orderId;
            return m;
        }

        public static TableActionSyncMessage forResetSeed() {
            TableActionSyncMessage m = new TableActionSyncMessage();
            m.action = "RESET_SEED";
            return m;
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

    public static class StaffStatusBroadcastMessage {
        public String type = MessageType.STAFF_STATUS_BROADCAST.name();
        public int staffId;
        public String username;
        public String status; // "ACTIVE" or "LOCKED"
        public String role;

        public StaffStatusBroadcastMessage() {}

        public StaffStatusBroadcastMessage(int staffId, String username, String status, String role) {
            this.staffId = staffId;
            this.username = username;
            this.status = status;
            this.role = role;
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

    public static class ReconnectRequestMessage {
        public String type = MessageType.RECONNECT_REQUEST.name();
        public String deviceId;
        public int tableNumber;
        public String deviceType;
        public long requestTime;
        public String note;

        public ReconnectRequestMessage() {}

        public ReconnectRequestMessage(String deviceId, int tableNumber, String deviceType, String note) {
            this.deviceId = deviceId;
            this.tableNumber = tableNumber;
            this.deviceType = deviceType;
            this.requestTime = System.currentTimeMillis();
            this.note = note;
        }
    }

    public static class ReconnectApprovedMessage {
        public String type = MessageType.RECONNECT_APPROVED.name();
        public String sessionId;
        public long approvedAt;

        public ReconnectApprovedMessage() {}

        public ReconnectApprovedMessage(String sessionId) {
            this.sessionId = sessionId;
            this.approvedAt = System.currentTimeMillis();
        }
    }

    public static class ReconnectRejectedMessage {
        public String type = MessageType.RECONNECT_REJECTED.name();
        public String reason;

        public ReconnectRejectedMessage() {}

        public ReconnectRejectedMessage(String reason) {
            this.reason = reason;
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

    public static class SessionSnapshotDto {
        public String sessionId;
        public int currentTable;
        public String deviceType;
        public String remoteAddress;
        public long rttLatencyMs;
        public long packetsReceived;
        public long packetsSent;
        public long bytesReceived;
        public long bytesSent;
        public boolean isThrottled;
        public long throttledCount;
        public String authenticatedUsername;
        public String linkState = "ONLINE"; // "ONLINE", "STALE"
        public long silentSeconds = 0;

        public SessionSnapshotDto() {}

        public SessionSnapshotDto(String sessionId, int currentTable, String deviceType, String remoteAddress,
                                  long rttLatencyMs, long packetsReceived, long packetsSent,
                                  long bytesReceived, long bytesSent, boolean isThrottled,
                                  long throttledCount, String authenticatedUsername) {
            this(sessionId, currentTable, deviceType, remoteAddress, rttLatencyMs, packetsReceived, packetsSent,
                 bytesReceived, bytesSent, isThrottled, throttledCount, authenticatedUsername, "ONLINE", 0);
        }

        public SessionSnapshotDto(String sessionId, int currentTable, String deviceType, String remoteAddress,
                                  long rttLatencyMs, long packetsReceived, long packetsSent,
                                  long bytesReceived, long bytesSent, boolean isThrottled,
                                  long throttledCount, String authenticatedUsername,
                                  String linkState, long silentSeconds) {
            this.sessionId = sessionId;
            this.currentTable = currentTable;
            this.deviceType = deviceType;
            this.remoteAddress = remoteAddress;
            this.rttLatencyMs = rttLatencyMs;
            this.packetsReceived = packetsReceived;
            this.packetsSent = packetsSent;
            this.bytesReceived = bytesReceived;
            this.bytesSent = bytesSent;
            this.isThrottled = isThrottled;
            this.throttledCount = throttledCount;
            this.authenticatedUsername = authenticatedUsername;
            this.linkState = (linkState != null && !linkState.trim().isEmpty()) ? linkState : "ONLINE";
            this.silentSeconds = Math.max(0, silentSeconds);
        }
    }

    public static class ReconnectRequestDto {
        public String deviceId;
        public int tableNumber;
        public String deviceType;
        public String ip;
        public long requestTime;
        public String note;

        public ReconnectRequestDto() {}

        public ReconnectRequestDto(String deviceId, int tableNumber, String deviceType, String ip, long requestTime, String note) {
            this.deviceId = deviceId;
            this.tableNumber = tableNumber;
            this.deviceType = deviceType;
            this.ip = ip;
            this.requestTime = requestTime;
            this.note = note;
        }
    }

    public static class AdminGetTelemetryMessage {
        public String type = MessageType.ADMIN_GET_TELEMETRY.name();
        public long timestamp = System.currentTimeMillis();
    }

    public static class AdminTelemetrySnapshotMessage {
        public String type = MessageType.ADMIN_TELEMETRY_SNAPSHOT.name();
        public int activePort;
        public String lanIp;
        public long avgRttMs;
        public long totalBytesReceived;
        public long totalBytesSent;
        public List<SessionSnapshotDto> sessions;
        public List<ReconnectRequestDto> pendingRequests;

        public AdminTelemetrySnapshotMessage() {}

        public AdminTelemetrySnapshotMessage(int activePort, String lanIp, long avgRttMs,
                                             long totalBytesReceived, long totalBytesSent,
                                             List<SessionSnapshotDto> sessions,
                                             List<ReconnectRequestDto> pendingRequests) {
            this.activePort = activePort;
            this.lanIp = lanIp;
            this.avgRttMs = avgRttMs;
            this.totalBytesReceived = totalBytesReceived;
            this.totalBytesSent = totalBytesSent;
            this.sessions = sessions;
            this.pendingRequests = pendingRequests;
        }
    }

    public static class AdminCommandMessage {
        public String type = MessageType.ADMIN_COMMAND.name();
        public String action; // PING_SINGLE, PING_ALL, KICK_SESSION, APPROVE_RECONNECT, REJECT_RECONNECT, TOGGLE_STOCK_86, REBIND_PORT, UPDATE_ORDER_STATUS, STAFF_STATUS
        public String targetId;
        public String param1;
        public boolean flag;

        public AdminCommandMessage() {}

        public AdminCommandMessage(String action, String targetId, String param1, boolean flag) {
            this.action = action;
            this.targetId = targetId;
            this.param1 = param1;
            this.flag = flag;
        }
    }

    public static class AdminPingResultMessage {
        public String type = MessageType.ADMIN_PING_RESULT.name();
        public String sessionId;
        public long rttMs;
        public long avgRttMs;
        public boolean success;

        public AdminPingResultMessage() {}

        public AdminPingResultMessage(String sessionId, long rttMs, long avgRttMs, boolean success) {
            this.sessionId = sessionId;
            this.rttMs = rttMs;
            this.avgRttMs = avgRttMs;
            this.success = success;
        }
    }

    public static class OrdersSnapshotSyncMessage {
        public String type = MessageType.ORDERS_SNAPSHOT_SYNC.name();
        public List<Order> orders;

        public OrdersSnapshotSyncMessage() {}

        public OrdersSnapshotSyncMessage(List<Order> orders) {
            this.orders = orders;
        }
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
