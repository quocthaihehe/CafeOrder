package cafe.services;

import cafe.models.MessageProtocol;
import cafe.models.MessageType;
import cafe.models.OrderItem;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ClientSocketService {
    private static String defaultHost() { return cafe.utils.NetworkConfig.getServerHost(); }
    private static int defaultPort() { return cafe.utils.NetworkConfig.getServerPort(); }
    private static volatile ClientSocketService activeInstance;

    public ClientSocketService() {
        activeInstance = this;
    }

    public static ClientSocketService getActiveInstance() {
        return activeInstance;
    }

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread listenerThread;
    private Thread heartbeatThread;
    private volatile boolean running = false;

    private final String localSessionId = UUID.randomUUID().toString().substring(0, 8);
    private int activeTableNumber = 5;
    private String deviceType = "KIOSK"; // "KIOSK" or "STAFF_PAD"
    private String authenticatedStaffUsername = null;

    // Callbacks
    private Consumer<Boolean> onConnectionStateChanged;
    private Consumer<MessageProtocol.OrderAckMessage> onOrderAckReceived;
    private Consumer<MessageProtocol.OrderStatusUpdateMessage> onStatusUpdateReceived;
    private BiConsumer<Integer, String> onTableStatusReceived;
    private final java.util.List<BiConsumer<Integer, String>> tableStatusListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final java.util.List<Consumer<MessageProtocol.OrderStatusUpdateMessage>> orderStatusListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final java.util.List<Consumer<MessageProtocol.OrderAckMessage>> orderAckListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final java.util.List<Consumer<MessageProtocol.AdminTelemetrySnapshotMessage>> telemetryListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final java.util.List<Consumer<MessageProtocol.AdminPingResultMessage>> pingResultListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    public void addTelemetryListener(Consumer<MessageProtocol.AdminTelemetrySnapshotMessage> listener) {
        if (listener != null && !telemetryListeners.contains(listener)) {
            telemetryListeners.add(listener);
        }
    }

    public void addPingResultListener(Consumer<MessageProtocol.AdminPingResultMessage> listener) {
        if (listener != null && !pingResultListeners.contains(listener)) {
            pingResultListeners.add(listener);
        }
    }

    public boolean requestAdminTelemetry() {
        if (!isConnected()) {
            openSocketInternal(defaultHost(), defaultPort());
        }
        if (isConnected() && out != null) {
            try {
                out.print(MessageProtocol.toJson(new MessageProtocol.AdminGetTelemetryMessage()));
                out.flush();
                return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    public boolean sendAdminCommand(String action, String targetId, String param1, boolean flag) {
        if (!isConnected()) {
            openSocketInternal(defaultHost(), defaultPort());
        }
        if (isConnected() && out != null) {
            try {
                MessageProtocol.AdminCommandMessage cmd = new MessageProtocol.AdminCommandMessage(action, targetId, param1, flag);
                out.print(MessageProtocol.toJson(cmd));
                out.flush();
                return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    public void addTableStatusListener(BiConsumer<Integer, String> listener) {
        if (listener != null && !tableStatusListeners.contains(listener)) {
            tableStatusListeners.add(listener);
        }
    }

    public void addOrderStatusListener(Consumer<MessageProtocol.OrderStatusUpdateMessage> listener) {
        if (listener != null && !orderStatusListeners.contains(listener)) {
            orderStatusListeners.add(listener);
        }
    }

    public void addOrderAckListener(Consumer<MessageProtocol.OrderAckMessage> listener) {
        if (listener != null && !orderAckListeners.contains(listener)) {
            orderAckListeners.add(listener);
        }
    }

    public boolean sendTableActionSync(MessageProtocol.TableActionSyncMessage msg) {
        if (msg == null) return false;
        if (!isConnected()) {
            openSocketInternal(cafe.utils.NetworkConfig.getServerHost(), cafe.utils.NetworkConfig.getServerPort());
        }
        if (isConnected() && out != null) {
            try {
                out.print(MessageProtocol.toJson(msg));
                out.flush();
                return true;
            } catch (Exception ignored) {}
        }
        return false;
    }
    private Consumer<MessageProtocol.StockStatusBroadcastMessage> onStockStatusReceived;
    private Consumer<MessageProtocol.SessionKickedMessage> onSessionKickedReceived;
    private Consumer<String> onErrorOccurred;
    private volatile boolean isKickedByAdmin = false;

    public void setOnSessionKickedReceived(Consumer<MessageProtocol.SessionKickedMessage> callback) {
        this.onSessionKickedReceived = callback;
    }

    public boolean isKickedByAdmin() {
        return isKickedByAdmin;
    }

    public void setOnStockStatusReceived(Consumer<MessageProtocol.StockStatusBroadcastMessage> callback) {
        this.onStockStatusReceived = callback;
    }

    public void setOnConnectionStateChanged(Consumer<Boolean> callback) {
        this.onConnectionStateChanged = callback;
    }

    public void setOnOrderAckReceived(Consumer<MessageProtocol.OrderAckMessage> callback) {
        this.onOrderAckReceived = callback;
    }

    public void setOnStatusUpdateReceived(Consumer<MessageProtocol.OrderStatusUpdateMessage> callback) {
        this.onStatusUpdateReceived = callback;
    }

    public void setOnTableStatusReceived(BiConsumer<Integer, String> callback) {
        this.onTableStatusReceived = callback;
    }

    public void setOnErrorOccurred(Consumer<String> callback) {
        this.onErrorOccurred = callback;
    }

    private Consumer<MessageProtocol.ReconnectApprovedMessage> onReconnectApproved;
    private Consumer<MessageProtocol.ReconnectRejectedMessage> onReconnectRejected;

    public void setOnReconnectApproved(Consumer<MessageProtocol.ReconnectApprovedMessage> callback) {
        this.onReconnectApproved = callback;
    }

    public void setOnReconnectRejected(Consumer<MessageProtocol.ReconnectRejectedMessage> callback) {
        this.onReconnectRejected = callback;
    }

    public void resetKickedState() {
        this.isKickedByAdmin = false;
    }

    public boolean sendReconnectRequest(String deviceId, int tableNumber, String deviceType, String note) {
        this.isKickedByAdmin = false;
        if (!isConnected()) {
            openSocketInternal(defaultHost(), defaultPort());
        }
        if (isConnected() && out != null) {
            MessageProtocol.ReconnectRequestMessage req = new MessageProtocol.ReconnectRequestMessage(
                deviceId, tableNumber, deviceType, note
            );
            out.print(MessageProtocol.toJson(req));
            out.flush();
            return true;
        }
        return false;
    }

    /**
     * Kết nối ngầm trước tới Server ngay khi màn hình Login vừa hiển thị.
     * Khi người dùng nhấn Đăng nhập, Socket đã mở sẵn nên thời gian chờ bằng 0ms.
     */
    public void warmupConnectAsync() {
        Thread t = new Thread(() -> {
            synchronized (this) {
                if (!isConnected()) {
                    openSocketInternal(defaultHost(), defaultPort());
                }
            }
        }, "Socket-Warmup");
        t.setDaemon(true);
        t.start();
    }

    private synchronized boolean openSocketInternal(String host, int port) {
        if (isKickedByAdmin) {
            return false;
        }
        if (isConnected() && out != null) {
            return true;
        }
        String resolvedHost = "localhost".equalsIgnoreCase(host) ? "127.0.0.1" : host;
        try {
            socket = new Socket();
            socket.connect(new java.net.InetSocketAddress(resolvedHost, port), 400);
            socket.setTcpNoDelay(true);
            socket.setKeepAlive(true);
            out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            running = true;
            startListening();
            startHeartbeat();
            dispatch(() -> {
                if (onConnectionStateChanged != null) onConnectionStateChanged.accept(true);
            });
            return true;
        } catch (IOException ex) {
            disconnect();
            dispatch(() -> {
                if (onConnectionStateChanged != null) onConnectionStateChanged.accept(false);
            });
            return false;
        }
    }

    public synchronized boolean tryConnectToServer() {
        return openSocketInternal(defaultHost(), defaultPort());
    }

    public synchronized boolean connect(String host, int port, int tableNumber, String devType) {
        this.activeTableNumber = tableNumber;
        this.deviceType = devType != null ? devType : "KIOSK";

        if (!isConnected() || out == null) {
            boolean opened = openSocketInternal(host, port);
            if (!opened) {
                dispatch(() -> {
                    if (onConnectionStateChanged != null) onConnectionStateChanged.accept(false);
                    if (onErrorOccurred != null) onErrorOccurred.accept("Không thể kết nối đến máy chủ!");
                });
                return false;
            }
        }

        // Gửi gói tin HELLO tức thì (0ms) trên kết nối đã mở sẵn
        MessageProtocol.HelloMessage hello = new MessageProtocol.HelloMessage(tableNumber, localSessionId, this.deviceType, this.authenticatedStaffUsername);
        out.print(MessageProtocol.toJson(hello));
        out.flush();

        dispatch(() -> {
            if (onConnectionStateChanged != null) onConnectionStateChanged.accept(true);
        });
        return true;
    }

    public boolean connect(String host, int port, int tableNumber) {
        return connect(host, port, tableNumber, "KIOSK");
    }

    public boolean connect(int tableNumber) {
        return connect(defaultHost(), defaultPort(), tableNumber, "KIOSK");
    }

    public boolean connectStaff(int tableNumber) {
        return connectStaff(tableNumber, null);
    }

    public boolean connectStaff(int tableNumber, String staffUsername) {
        this.authenticatedStaffUsername = staffUsername;
        return connect(defaultHost(), defaultPort(), tableNumber, "STAFF_PAD");
    }

    public void switchTable(int newTable) {
        this.activeTableNumber = newTable;
        if (isConnected() && out != null) {
            MessageProtocol.TableFocusChangeMessage msg = new MessageProtocol.TableFocusChangeMessage(newTable, localSessionId);
            out.print(MessageProtocol.toJson(msg));
            out.flush();
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public boolean sendOrder(int tableNumber, List<OrderItem> items) {
        return sendOrder(tableNumber, "DINE_IN", 0.0, null, null, items);
    }

    public boolean sendOrder(int tableNumber, String orderType, double discount, String voucherCode, String note, List<OrderItem> items) {
        return sendOrder(tableNumber, orderType, discount, voucherCode, note, null, null, items);
    }

    public boolean sendOrder(int tableNumber, String orderType, double discount, String voucherCode, String note, 
                             String customerPhone, String customerName, List<OrderItem> items) {
        if (!isConnected() || out == null || items == null || items.isEmpty()) {
            return false;
        }
        return sendOrderWithKey(tableNumber, orderType, discount, voucherCode, note, UUID.randomUUID().toString(), customerPhone, customerName, items);
    }

    public boolean sendOrderWithKey(int tableNumber, String orderType, double discount, String voucherCode, String note, String idempotencyKey, String customerPhone, String customerName, List<OrderItem> items) {
        if (!isConnected() || out == null || items == null || items.isEmpty()) {
            return false;
        }
        try {
            String key = idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString();
            MessageProtocol.NewOrderMessage msg = new MessageProtocol.NewOrderMessage(
                tableNumber, orderType, discount, voucherCode, note, key, customerPhone, customerName, items
            );
            out.print(MessageProtocol.toJson(msg));
            out.flush();
            return true;
        } catch (Exception e) {
            dispatch(() -> {
                if (onErrorOccurred != null) onErrorOccurred.accept("Lỗi khi gửi đơn hàng: " + e.getMessage());
            });
            return false;
        }
    }

    private void startListening() {
        listenerThread = new Thread(() -> {
            try {
                String line;
                while (running && in != null && (line = in.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    MessageType type = MessageProtocol.getMessageType(line);
                    if (type == null) continue;

                    final String rawJson = line;
                    switch (type) {
                        case ORDER_ACK:
                            MessageProtocol.OrderAckMessage ack = MessageProtocol.fromJson(rawJson, MessageProtocol.OrderAckMessage.class);
                            dispatch(() -> {
                                if (onOrderAckReceived != null) onOrderAckReceived.accept(ack);
                                for (Consumer<MessageProtocol.OrderAckMessage> l : orderAckListeners) l.accept(ack);
                            });
                            break;

                        case ORDER_STATUS_UPDATE:
                            MessageProtocol.OrderStatusUpdateMessage update = MessageProtocol.fromJson(rawJson, MessageProtocol.OrderStatusUpdateMessage.class);
                            if (update != null) {
                                CafeServerService.getInstance().applyRemoteOrderStatusSync(
                                    update.orderId, update.table, update.status, update.updatedOrder
                                );
                            }
                            dispatch(() -> {
                                if (onStatusUpdateReceived != null && update != null) onStatusUpdateReceived.accept(update);
                                if (update != null) {
                                    for (Consumer<MessageProtocol.OrderStatusUpdateMessage> l : orderStatusListeners) l.accept(update);
                                }
                            });
                            break;

                        case TABLE_STATUS_BROADCAST:
                            MessageProtocol.TableStatusBroadcastMessage tBroadcast = MessageProtocol.fromJson(rawJson, MessageProtocol.TableStatusBroadcastMessage.class);
                            if (tBroadcast != null) {
                                CafeServerService.getInstance().applyRemoteTableSync(
                                    tBroadcast.table, tBroadcast.status, tBroadcast.tableOrders, tBroadcast.reservation
                                );
                            }
                            dispatch(() -> {
                                if (onTableStatusReceived != null && tBroadcast != null) onTableStatusReceived.accept(tBroadcast.table, tBroadcast.status);
                                if (tBroadcast != null) {
                                    for (BiConsumer<Integer, String> l : tableStatusListeners) l.accept(tBroadcast.table, tBroadcast.status);
                                }
                            });
                            break;

                        case STOCK_STATUS_BROADCAST:
                            MessageProtocol.StockStatusBroadcastMessage stockMsg = MessageProtocol.fromJson(rawJson, MessageProtocol.StockStatusBroadcastMessage.class);
                            if (stockMsg != null) {
                                for (cafe.models.MenuItem mi : cafe.models.MenuRepository.getAllItems()) {
                                    if (mi.getId() == stockMsg.itemId || (stockMsg.itemName != null && mi.getName().equalsIgnoreCase(stockMsg.itemName))) {
                                        mi.setInStock(stockMsg.inStock);
                                    }
                                }
                            }
                            dispatch(() -> {
                                if (onStockStatusReceived != null) onStockStatusReceived.accept(stockMsg);
                            });
                            break;

                        case ORDERS_SNAPSHOT_SYNC:
                            MessageProtocol.OrdersSnapshotSyncMessage ordSnap = MessageProtocol.fromJson(rawJson, MessageProtocol.OrdersSnapshotSyncMessage.class);
                            if (ordSnap != null && ordSnap.orders != null) {
                                CafeServerService.getInstance().applyRemoteOrdersSnapshot(ordSnap.orders);
                            }
                            break;

                        case ADMIN_TELEMETRY_SNAPSHOT:
                            MessageProtocol.AdminTelemetrySnapshotMessage teleSnap = MessageProtocol.fromJson(rawJson, MessageProtocol.AdminTelemetrySnapshotMessage.class);
                            if (teleSnap != null) {
                                CafeServerService.getInstance().applyRemoteTelemetrySnapshot(teleSnap);
                                dispatch(() -> {
                                    for (Consumer<MessageProtocol.AdminTelemetrySnapshotMessage> l : telemetryListeners) l.accept(teleSnap);
                                });
                            }
                            break;

                        case ADMIN_PING_RESULT:
                            MessageProtocol.AdminPingResultMessage pingRes = MessageProtocol.fromJson(rawJson, MessageProtocol.AdminPingResultMessage.class);
                            if (pingRes != null) {
                                dispatch(() -> {
                                    for (Consumer<MessageProtocol.AdminPingResultMessage> l : pingResultListeners) l.accept(pingRes);
                                });
                            }
                            break;

                        case STAFF_STATUS_BROADCAST:
                            MessageProtocol.StaffStatusBroadcastMessage staffMsg = MessageProtocol.fromJson(rawJson, MessageProtocol.StaffStatusBroadcastMessage.class);
                            cafe.services.DatabaseManager.setStaffStatus(staffMsg.staffId, staffMsg.status);
                            if ("LOCKED".equalsIgnoreCase(staffMsg.status) && authenticatedStaffUsername != null && authenticatedStaffUsername.equalsIgnoreCase(staffMsg.username)) {
                                isKickedByAdmin = true;
                                running = false;
                                try {
                                    if (socket != null && !socket.isClosed()) socket.close();
                                } catch (IOException ignored) {}
                                MessageProtocol.SessionKickedMessage kickMsg = new MessageProtocol.SessionKickedMessage(
                                    localSessionId,
                                    "Tài khoản '" + staffMsg.username + "' đã bị KHÓA bởi Quản trị viên hệ thống!",
                                    3600000L
                                );
                                dispatch(() -> {
                                    if (onSessionKickedReceived != null) onSessionKickedReceived.accept(kickMsg);
                                });
                            }
                            break;

                        case SESSION_KICKED:
                            isKickedByAdmin = true;
                            running = false;
                            MessageProtocol.SessionKickedMessage kickMsg = MessageProtocol.fromJson(rawJson, MessageProtocol.SessionKickedMessage.class);
                            try {
                                if (socket != null && !socket.isClosed()) socket.close();
                            } catch (IOException ignored) {}
                            dispatch(() -> {
                                if (onSessionKickedReceived != null) onSessionKickedReceived.accept(kickMsg);
                            });
                            break;

                        case RECONNECT_APPROVED:
                            isKickedByAdmin = false;
                            MessageProtocol.ReconnectApprovedMessage approvedMsg = MessageProtocol.fromJson(rawJson, MessageProtocol.ReconnectApprovedMessage.class);
                            dispatch(() -> {
                                if (onReconnectApproved != null) onReconnectApproved.accept(approvedMsg);
                            });
                            break;

                        case RECONNECT_REJECTED:
                            isKickedByAdmin = true;
                            MessageProtocol.ReconnectRejectedMessage rejectedMsg = MessageProtocol.fromJson(rawJson, MessageProtocol.ReconnectRejectedMessage.class);
                            dispatch(() -> {
                                if (onReconnectRejected != null) onReconnectRejected.accept(rejectedMsg);
                            });
                            break;

                        case PING:
                            if (out != null) {
                                out.print(MessageProtocol.toJson(new MessageProtocol.PongMessage()));
                                out.flush();
                            }
                            break;

                        case PONG:
                            break;

                        default:
                            break;
                    }
                }
            } catch (IOException e) {
                if (running) {
                    dispatch(() -> {
                        if (onConnectionStateChanged != null) onConnectionStateChanged.accept(false);
                        if (onErrorOccurred != null) onErrorOccurred.accept("Mất kết nối với máy chủ!");
                    });
                }
            }
        });
        listenerThread.setName("Client-SocketReader");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void startHeartbeat() {
        heartbeatThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(15000); // 15 giây gửi Ping 1 lần
                    if (running && isConnected() && out != null) {
                        out.print(MessageProtocol.toJson(new MessageProtocol.PingMessage()));
                        out.flush();
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Exception ignored) {}
            }
        });
        heartbeatThread.setName("Client-Heartbeat");
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }

    public void disconnect() {
        running = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
        if (heartbeatThread != null) heartbeatThread.interrupt();
    }

    public int getActiveTableNumber() {
        return activeTableNumber;
    }

    public String getLocalSessionId() {
        return localSessionId;
    }

    private void dispatch(Runnable action) {
        try {
            if (Platform.isFxApplicationThread()) {
                action.run();
            } else {
                Platform.runLater(action);
            }
        } catch (IllegalStateException e) {
            action.run();
        }
    }
}
