package cafe.services;

import cafe.models.MessageProtocol;
import cafe.models.MessageType;
import cafe.models.Order;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CafeServerService {
    private static final int DEFAULT_PORT = 5000;

    private ServerSocket serverSocket;
    private final AtomicInteger orderIdCounter = new AtomicInteger(1000);
    
    // Dynamic Session & Table Subscriptions (Decoupled Routing)
    private final Map<String, ClientSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<Integer, Set<String>> tableSubscribers = new ConcurrentHashMap<>();
    
    private final Map<Integer, Order> activeOrders = new ConcurrentHashMap<>();
    private volatile boolean isRunning = false;

    private final AtomicInteger clientWorkerCounter = new AtomicInteger(1);

    // Optimized Thread Pools
    private final java.util.concurrent.ExecutorService clientPool = java.util.concurrent.Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setName("KDS-ClientWorker-" + clientWorkerCounter.getAndIncrement());
        t.setDaemon(true);
        return t;
    });

    private final java.util.concurrent.ExecutorService dbExecutor = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setName("KDS-DatabaseWorker");
        t.setDaemon(true);
        return t;
    });

    // Multicast Event Listeners
    private final java.util.List<Consumer<Order>> orderReceivedListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final java.util.List<BiConsumer<Integer, String>> orderStatusChangedListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final java.util.List<BiConsumer<Integer, Boolean>> tableConnectionListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final java.util.List<BiConsumer<Integer, String>> tableStatusBroadcastListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final java.util.List<Consumer<String>> serverLogListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static class ClientSession {
        public final String sessionId;
        public final Socket socket;
        public final PrintWriter writer;
        public volatile int currentTable;
        public volatile String deviceType;
        public volatile long lastHeartbeat;

        public ClientSession(String sessionId, Socket socket, PrintWriter writer, int currentTable, String deviceType) {
            this.sessionId = sessionId;
            this.socket = socket;
            this.writer = writer;
            this.currentTable = currentTable;
            this.deviceType = deviceType;
            this.lastHeartbeat = System.currentTimeMillis();
        }

        public synchronized void send(String jsonLine) {
            if (writer != null) {
                writer.print(jsonLine);
                writer.flush();
            }
        }
    }

    public void setOnOrderReceived(Consumer<Order> callback) {
        if (callback != null) this.orderReceivedListeners.add(callback);
    }

    public void setOnOrderStatusChanged(BiConsumer<Integer, String> callback) {
        if (callback != null) this.orderStatusChangedListeners.add(callback);
    }

    public void setOnTableConnectionChanged(BiConsumer<Integer, Boolean> callback) {
        if (callback != null) this.tableConnectionListeners.add(callback);
    }

    public void setOnTableStatusBroadcasted(BiConsumer<Integer, String> callback) {
        if (callback != null) this.tableStatusBroadcastListeners.add(callback);
    }

    public void setOnServerLog(Consumer<String> callback) {
        if (callback != null) this.serverLogListeners.add(callback);
    }

    public void start(int port) {
        if (isRunning) return;
        isRunning = true;

        Thread serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                log("Máy chủ KDS pha chế & Quản trị CoffeePOS đã mở cổng " + port + ". Đang sẵn sàng đón nhận đơn.");

                while (isRunning) {
                    Socket clientSocket = serverSocket.accept();
                    clientSocket.setTcpNoDelay(true);
                    clientSocket.setKeepAlive(true);
                    clientPool.submit(() -> handleClientConnection(clientSocket));
                }
            } catch (IOException e) {
                if (isRunning) {
                    log("Lỗi Server Socket: " + e.getMessage());
                }
            } finally {
                stop();
            }
        });
        serverThread.setName("KDS-AcceptorThread");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    public void start() {
        start(DEFAULT_PORT);
    }

    private void handleClientConnection(Socket socket) {
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        ClientSession session = null;
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)
        ) {
            session = new ClientSession(sessionId, socket, writer, -1, "KIOSK");
            activeSessions.put(sessionId, session);

            String line;
            while ((line = in.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                MessageType type = MessageProtocol.getMessageType(line);
                if (type == null) continue;

                session.lastHeartbeat = System.currentTimeMillis();

                switch (type) {
                    case HELLO:
                        MessageProtocol.HelloMessage hello = MessageProtocol.fromJson(line, MessageProtocol.HelloMessage.class);
                        int registeredTable = hello.table;
                        session.currentTable = registeredTable;
                        if (hello.deviceType != null) session.deviceType = hello.deviceType;

                        // Đăng ký bảng định tuyến bàn
                        bindTableSubscriber(registeredTable, sessionId);

                        log("Thiết bị [" + session.deviceType + " #" + sessionId + "] gắn với Bàn " + registeredTable + " đã kết nối.");
                        final int tConnect = registeredTable;
                        dispatch(() -> {
                            for (BiConsumer<Integer, Boolean> l : tableConnectionListeners) l.accept(tConnect, true);
                        });
                        break;

                    case TABLE_FOCUS_CHANGE:
                        MessageProtocol.TableFocusChangeMessage focus = MessageProtocol.fromJson(line, MessageProtocol.TableFocusChangeMessage.class);
                        int oldTable = session.currentTable;
                        int newTable = focus.newTable;
                        session.currentTable = newTable;
                        unbindTableSubscriber(oldTable, sessionId);
                        bindTableSubscriber(newTable, sessionId);
                        log("Thiết bị #" + sessionId + " chuyển trọng tâm phục vụ: Bàn " + oldTable + " -> Bàn " + newTable);
                        break;

                    case PING:
                        session.send(MessageProtocol.toJson(new MessageProtocol.PongMessage()));
                        break;

                    case NEW_ORDER:
                        MessageProtocol.NewOrderMessage orderMsg = MessageProtocol.fromJson(line, MessageProtocol.NewOrderMessage.class);
                        int newOrderId = orderIdCounter.incrementAndGet();

                        Order newOrder = new Order(
                            newOrderId,
                            orderMsg.table,
                            orderMsg.orderType,
                            orderMsg.items,
                            "QUEUED",
                            orderMsg.discountAmount,
                            0.0,
                            orderMsg.voucherCode,
                            orderMsg.note,
                            System.currentTimeMillis()
                        );
                        activeOrders.put(newOrderId, newOrder);
                        log("Nhận Đơn hàng #" + newOrderId + " (" + newOrder.getOrderTypeLabel() + ") - Bàn " + orderMsg.table + " (" + newOrder.getTotalQuantity() + " món).");

                        // 1. Phản hồi ORDER_ACK siêu tốc (<1ms) ngay lập tức về Kiosk
                        MessageProtocol.OrderAckMessage ack = new MessageProtocol.OrderAckMessage(newOrderId, "QUEUED", activeOrders.size());
                        session.send(MessageProtocol.toJson(ack));

                        // 2. Bắn sự kiện tức thì lên giao diện KDS Bar và Dashboard Admin
                        dispatch(() -> {
                            for (Consumer<Order> l : orderReceivedListeners) l.accept(newOrder);
                        });

                        // 3. Broadcast trạng thái bàn sang OCCUPIED cho tất cả các máy trạm sơ đồ bàn
                        broadcastTableStatus(orderMsg.table, "OCCUPIED");

                        // 4. Bất đồng bộ lưu bền vững vào SQL Server qua worker riêng, không chặn socket
                        final Order orderToSave = newOrder;
                        dbExecutor.submit(() -> {
                            try {
                                DatabaseManager.saveOrder(orderToSave);
                            } catch (Exception ex) {
                                log("Lỗi lưu đơn vào DB: " + ex.getMessage());
                            }
                        });
                        break;

                    default:
                        break;
                }
            }
        } catch (SocketException ignored) {
            // Client ngắt kết nối
        } catch (IOException e) {
            log("Lỗi I/O client [" + sessionId + "]: " + e.getMessage());
        } finally {
            if (session != null) {
                int disconnectedTable = session.currentTable;
                activeSessions.remove(sessionId);
                unbindTableSubscriber(disconnectedTable, sessionId);
                log("Thiết bị #" + sessionId + " (Bàn " + disconnectedTable + ") đã rời mạng.");
                if (disconnectedTable != -1) {
                    dispatch(() -> {
                        for (BiConsumer<Integer, Boolean> l : tableConnectionListeners) l.accept(disconnectedTable, false);
                    });
                }
            }
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException ignored) {}
        }
    }

    private void bindTableSubscriber(int table, String sessionId) {
        if (table <= 0 || sessionId == null) return;
        tableSubscribers.computeIfAbsent(table, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
    }

    private void unbindTableSubscriber(int table, String sessionId) {
        if (table <= 0 || sessionId == null) return;
        Set<String> set = tableSubscribers.get(table);
        if (set != null) {
            set.remove(sessionId);
            if (set.isEmpty()) tableSubscribers.remove(table);
        }
    }

    public boolean updateOrderStatus(int orderId, String newStatus) {
        Order order = activeOrders.get(orderId);
        if (order != null) {
            order.setStatus(newStatus);
            log("Cập nhật Đơn #" + orderId + " (Bàn " + order.getTable() + ") -> " + MessageProtocol.translateStatus(newStatus));

            // Bắn tín hiệu về tất cả Client đang theo dõi bàn đó
            MessageProtocol.OrderStatusUpdateMessage updateMsg = new MessageProtocol.OrderStatusUpdateMessage(orderId, order.getTable(), newStatus);
            String updateJson = MessageProtocol.toJson(updateMsg);

            Set<String> subscribers = tableSubscribers.get(order.getTable());
            if (subscribers != null && !subscribers.isEmpty()) {
                for (String sid : subscribers) {
                    ClientSession s = activeSessions.get(sid);
                    if (s != null) s.send(updateJson);
                }
                log("Đã thông báo trạng thái mới về Bàn " + order.getTable());
            }

            // Kích hoạt callback UI
            dispatch(() -> {
                for (BiConsumer<Integer, String> l : orderStatusChangedListeners) l.accept(orderId, newStatus);
            });

            // Nếu Barista bấm Hoàn tất (DONE) -> Tự động kích hoạt trừ tồn kho nguyên vật liệu (BOM)
            if ("DONE".equalsIgnoreCase(newStatus)) {
                dbExecutor.submit(() -> {
                    try {
                        DatabaseManager.updateOrderStatusInDb(orderId, "DONE");
                        DatabaseManager.deductStockForOrder(order);
                    } catch (Exception ex) {
                        System.err.println("[SERVER] Lỗi trừ kho định lượng: " + ex.getMessage());
                    }
                });
            } else {
                dbExecutor.submit(() -> DatabaseManager.updateOrderStatusInDb(orderId, newStatus));
            }

            return true;
        }
        return false;
    }

    public void broadcastTableStatus(int tableNumber, String status) {
        MessageProtocol.TableStatusBroadcastMessage msg = new MessageProtocol.TableStatusBroadcastMessage(tableNumber, status);
        String json = MessageProtocol.toJson(msg);
        for (ClientSession session : activeSessions.values()) {
            session.send(json);
        }
        dispatch(() -> {
            for (BiConsumer<Integer, String> l : tableStatusBroadcastListeners) l.accept(tableNumber, status);
        });
        dbExecutor.submit(() -> DatabaseManager.updateTableStatusInDb(tableNumber, status));
    }

    public Map<Integer, Order> getActiveOrders() {
        return activeOrders;
    }

    public Map<String, ClientSession> getActiveSessions() {
        return activeSessions;
    }

    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}
        clientPool.shutdownNow();
        dbExecutor.shutdown();
        log("Máy chủ đã dừng.");
    }

    private void log(String message) {
        dispatch(() -> {
            for (Consumer<String> l : serverLogListeners) l.accept(message);
        });
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
