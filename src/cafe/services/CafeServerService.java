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
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CafeServerService {
    private static final int DEFAULT_PORT = 5000;
    private static volatile CafeServerService activeInstance;

    public static class ReservationInfo {
        public final int tableNumber;
        public final String customerName;
        public final String phone;
        public final String reservationTime;
        public final int guestCount;
        public final String note;

        public ReservationInfo(int tableNumber, String customerName, String phone, String reservationTime, int guestCount, String note) {
            this.tableNumber = tableNumber;
            this.customerName = customerName;
            this.phone = phone;
            this.reservationTime = reservationTime;
            this.guestCount = guestCount;
            this.note = note;
        }
    }

    private ServerSocket serverSocket;
    private final AtomicInteger orderIdCounter = new AtomicInteger(1020);
    
    // Dynamic Session & Table Subscriptions (Decoupled Routing)
    private final Map<String, ClientSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<Integer, Set<String>> tableSubscribers = new ConcurrentHashMap<>();
    
    private final Map<Integer, Order> activeOrders = new ConcurrentHashMap<>();
    public final Map<String, Integer> processedIdempotencyKeys = new ConcurrentHashMap<>();
    private final Map<Integer, String> tableStatuses = new ConcurrentHashMap<>();
    private final Map<Integer, ReservationInfo> tableReservations = new ConcurrentHashMap<>();
    private volatile boolean isRunning = false;
    private final java.util.Queue<Order> dbRetryQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();
    public java.util.Queue<Order> getDbRetryQueue() { return dbRetryQueue; }

    public CafeServerService() {
        activeInstance = this;
        seedInitialActiveOrders();
    }

    public static synchronized CafeServerService getInstance() {
        if (activeInstance == null) {
            activeInstance = new CafeServerService();
        }
        return activeInstance;
    }

    private void seedInitialActiveOrders() {
        List<Order> dbOrders = DatabaseManager.loadActiveOrdersFromDb();
        if (dbOrders != null && !dbOrders.isEmpty()) {
            for (Order o : dbOrders) {
                activeOrders.put(o.getOrderId(), o);
                if (o.getTable() > 0) {
                    tableStatuses.put(o.getTable(), "OCCUPIED");
                }
                if (o.getOrderId() >= orderIdCounter.get()) {
                    orderIdCounter.set(o.getOrderId());
                }
                if (o.getRequestId() != null && !o.getRequestId().trim().isEmpty()) {
                    processedIdempotencyKeys.put(o.getRequestId().trim(), o.getOrderId());
                }
                if ("DONE".equalsIgnoreCase(o.getStatus()) || "PAID".equalsIgnoreCase(o.getStatus())) {
                    o.setStockDeducted(true);
                }
            }
            log("Đã khôi phục " + dbOrders.size() + " đơn hàng đang xử lý từ SQL Server.");
            return;
        }
        if (!DatabaseManager.isAvailable()) {
            log("[SYSTEM NOTICE] SQL Server khong kha dung -> He thong dang hoat dong o che do IN-MEMORY FALLBACK (Du lieu chi luu tren RAM).");
        }
        long now = System.currentTimeMillis();
        // Bàn 01 (Đang phục vụ - 14 phút trước)
        addSeedOrder(1001, 1, "DINE_IN", "PREPARING", 0, null, "Ít đá 1 ly", now - 14 * 60_000L,
            new cafe.models.OrderItem("Cà phê Sữa Đá", 2, "Size M • Đá 70%", 35000),
            new cafe.models.OrderItem("Bánh Sừng Bò Hạnh Nhân", 1, "Hâm nóng", 42000)
        );
        // Bàn 03 (Đang phục vụ - 25 phút trước)
        addSeedOrder(1002, 3, "DINE_IN", "DONE", 15000, "GIAM15K", "Khách VIP", now - 25 * 60_000L,
            new cafe.models.OrderItem("Cold Brew Cam Sả", 2, "Size L • 70% đường", 55000),
            new cafe.models.OrderItem("Matcha Latte Kem Phô Mai", 1, "Size M • Đá 50%", 59000),
            new cafe.models.OrderItem("Tiramisu Cổ Điển", 1, "", 55000)
        );
        // Bàn 06 (Đang phục vụ - 8 phút trước)
        addSeedOrder(1003, 6, "DINE_IN", "QUEUED", 0, null, "", now - 8 * 60_000L,
            new cafe.models.OrderItem("Trà Đào Cam Sả", 2, "Size L • +Đào miếng", 49000),
            new cafe.models.OrderItem("Bạc Xỉu Đá Sài Gòn", 1, "Size M", 35000)
        );
        // Bàn 09 (Đang phục vụ - 19 phút trước)
        addSeedOrder(1004, 9, "DINE_IN", "PREPARING", 0, null, "Mang nước lọc kèm", now - 19 * 60_000L,
            new cafe.models.OrderItem("Caramel Macchiato", 2, "Size M • Nóng", 55000),
            new cafe.models.OrderItem("Trà Ô Long Kem Cheese", 2, "Size L • 50% đường", 52000),
            new cafe.models.OrderItem("Croissant Bơ Pháp", 2, "Nướng giòn", 38000)
        );
        // Bàn 12 (Đang phục vụ - 32 phút trước)
        addSeedOrder(1005, 12, "DINE_IN", "DONE", 0, null, "", now - 32 * 60_000L,
            new cafe.models.OrderItem("Espresso Đá", 1, "Size M", 32000),
            new cafe.models.OrderItem("Trà Sen Vàng Kem Macchiato", 2, "Size L • +Hạt sen", 55000)
        );
        // Tầng 2 & Tầng 3
        addSeedOrder(1006, 201, "DINE_IN", "DONE", 0, null, "", now - 28 * 60_000L,
            new cafe.models.OrderItem("Americano Đá", 2, "Size M • Không đường", 39000),
            new cafe.models.OrderItem("Bánh Mousse Chanh Dây", 1, "", 45000)
        );
        addSeedOrder(1007, 205, "DINE_IN", "PREPARING", 0, null, "Họp nhóm", now - 12 * 60_000L,
            new cafe.models.OrderItem("Cà phê Muối Huế", 3, "Size M", 42000),
            new cafe.models.OrderItem("Trà Vải Hoa Hồng", 2, "Size L", 49000)
        );
        addSeedOrder(1008, 209, "DINE_IN", "DONE", 0, null, "", now - 40 * 60_000L,
            new cafe.models.OrderItem("Latte Hạnh Nhân", 2, "Size M • Nóng", 55000)
        );
        addSeedOrder(1009, 214, "DINE_IN", "QUEUED", 0, null, "", now - 5 * 60_000L,
            new cafe.models.OrderItem("Sinh Tố Bơ Sầu Riêng", 2, "Size L", 62000)
        );
        addSeedOrder(1010, 302, "DINE_IN", "PREPARING", 0, null, "Ban công", now - 16 * 60_000L,
            new cafe.models.OrderItem("Cold Brew Quất Mật Ong", 2, "Size L", 55000),
            new cafe.models.OrderItem("Bánh Phô Mai Nướng Basque", 2, "", 58000)
        );
        addSeedOrder(1011, 305, "DINE_IN", "DONE", 0, null, "", now - 35 * 60_000L,
            new cafe.models.OrderItem("Trà Hoa Đậu Biếc Macchiato", 2, "Size M", 49000)
        );
        addSeedOrder(1012, 309, "DINE_IN", "PREPARING", 0, null, "", now - 11 * 60_000L,
            new cafe.models.OrderItem("Cappuccino Đá", 2, "Size M • Nóng", 49000)
        );
        addSeedOrder(1013, 311, "DINE_IN", "DONE", 0, null, "", now - 21 * 60_000L,
            new cafe.models.OrderItem("Matcha Đá Xay", 3, "Size L • +Kem tươi", 59000)
        );

        // Đơn mang về mẫu
        addSeedOrder(1014, 0, "TAKEAWAY", "DONE", 0, null, "Lấy ngay tại quầy", now - 18 * 60_000L,
            new cafe.models.OrderItem("Cà phê Sữa Đá", 2, "Size L", 39000)
        );

        // Đặt trước mặc định cho Bàn 05 & Bàn 10
        tableStatuses.put(5, "RESERVED");
        tableReservations.put(5, new ReservationInfo(5, "Nguyễn Hoàng Nam", "0908.345.678", "19:30", 2, "Ngồi gần cửa sổ"));
        tableStatuses.put(10, "RESERVED");
        tableReservations.put(10, new ReservationInfo(10, "LÃª Báº£o Ngá»c", "0912.888.999", "20:00", 6, "Sinh nháº­t gia Ä‘Ã¬nh"));
    }

    private void addSeedOrder(int orderId, int table, String type, String status, double discount, String voucher, String note, long ts, cafe.models.OrderItem... items) {
        java.util.List<cafe.models.OrderItem> list = new java.util.ArrayList<>(java.util.Arrays.asList(items));
        Order o = new Order(orderId, table, type, list, status, discount, 0.0, voucher, note, ts);
        activeOrders.put(orderId, o);
        if (table > 0 && !"PAID".equalsIgnoreCase(status) && !"CANCELLED".equalsIgnoreCase(status)) {
            tableStatuses.put(table, "OCCUPIED");
        }
    }

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
        activeInstance = this;

        Thread serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                log("MÃ¡y chá»§ KDS pha cháº¿ & Quáº£n trá»‹ CoffeePOS Ä‘Ã£ má»Ÿ cá»•ng " + port + ". Äang sáºµn sÃ ng Ä‘Ã³n nháº­n Ä‘Æ¡n.");

                while (isRunning) {
                    Socket clientSocket = serverSocket.accept();
                    clientSocket.setTcpNoDelay(true);
                    clientSocket.setKeepAlive(true);
                    clientPool.submit(() -> handleClientConnection(clientSocket));
                }
            } catch (IOException e) {
                if (isRunning) {
                    log("Lá»—i Server Socket: " + e.getMessage());
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

                        // ÄÄƒng kÃ½ báº£ng Ä‘á»‹nh tuyáº¿n bÃ n
                        bindTableSubscriber(registeredTable, sessionId);

                        log("Thiáº¿t bá»‹ [" + session.deviceType + " #" + sessionId + "] gáº¯n vá»›i BÃ n " + registeredTable + " Ä‘Ã£ káº¿t ná»‘i.");
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
                        log("Thiáº¿t bá»‹ #" + sessionId + " chuyá»ƒn trá»ng tÃ¢m phá»¥c vá»¥: BÃ n " + oldTable + " -> BÃ n " + newTable);
                        break;

                    case PING:
                        session.send(MessageProtocol.toJson(new MessageProtocol.PongMessage()));
                        break;

                    case NEW_ORDER:
                        MessageProtocol.NewOrderMessage orderMsg = MessageProtocol.fromJson(line, MessageProtocol.NewOrderMessage.class);
                        if (orderMsg == null || orderMsg.items == null || orderMsg.items.isEmpty()) {
                            break;
                        }

                        // P0-1: Chống tạo đơn trùng lặp nguyên tử (Atomic Idempotency Reservation)
                        int allocatedOrderId;
                        String idempKey = (orderMsg.idempotencyKey != null && !orderMsg.idempotencyKey.trim().isEmpty())
                                ? orderMsg.idempotencyKey.trim() : null;

                        if (idempKey != null) {
                            synchronized (processedIdempotencyKeys) {
                                Integer existingId = processedIdempotencyKeys.get(idempKey);
                                if (existingId != null) {
                                    Order existingOrder = activeOrders.get(existingId);
                                    String curStatus = existingOrder != null ? existingOrder.getStatus() : "QUEUED";
                                    boolean isDb = DatabaseManager.isAvailable();
                                    MessageProtocol.OrderAckMessage dupAck = new MessageProtocol.OrderAckMessage(existingId, curStatus, activeOrders.size(), isDb, isDb ? "DATABASE" : "IN_MEMORY");
                                    session.send(MessageProtocol.toJson(dupAck));
                                    log("Bỏ qua gói tin gửi lặp đồng thời (IdempotencyKey: " + idempKey + ") cho Đơn #" + existingId);
                                    break;
                                }
                                allocatedOrderId = orderIdCounter.incrementAndGet();
                                processedIdempotencyKeys.put(idempKey, allocatedOrderId);
                            }
                        } else {
                            allocatedOrderId = orderIdCounter.incrementAndGet();
                        }

                        // P0-2: Xác thực tính toàn vẹn giá, voucher và tình trạng còn hàng (Out-of-stock)
                        validateAndSanitizeOrder(orderMsg);
                        if (orderMsg.items.isEmpty()) {
                            log("Từ chối đơn hàng Bàn " + orderMsg.table + ": Toàn bộ món trong đơn đã hết hàng hoặc không hợp lệ.");
                            if (idempKey != null) processedIdempotencyKeys.remove(idempKey);
                            break;
                        }

                        int newOrderId = allocatedOrderId;
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
                            System.currentTimeMillis(),
                            "Bàn " + orderMsg.table,
                            idempKey != null ? idempKey : UUID.randomUUID().toString()
                        );
                        activeOrders.put(newOrderId, newOrder);
                        log("Nháº­n ÄÆ¡n hÃ ng #" + newOrderId + " (" + newOrder.getOrderTypeLabel() + ") - BÃ n " + orderMsg.table + " (" + newOrder.getTotalQuantity() + " mÃ³n).");

                        // 1. Pháº£n há»“i ORDER_ACK siÃªu tá»‘c (<1ms) ngay láº­p tá»©c vá» Kiosk
                        boolean isDbReady = DatabaseManager.isAvailable();
                        String storageMode = isDbReady ? "DATABASE" : "IN_MEMORY";
                        MessageProtocol.OrderAckMessage ack = new MessageProtocol.OrderAckMessage(newOrderId, "QUEUED", activeOrders.size(), isDbReady, storageMode);
                        session.send(MessageProtocol.toJson(ack));

                        // 2. Báº¯n sá»± kiá»‡n tá»©c thÃ¬ lÃªn giao diá»‡n KDS Bar vÃ  Dashboard Admin
                        dispatch(() -> {
                            for (Consumer<Order> l : orderReceivedListeners) l.accept(newOrder);
                        });

                        // 3. Broadcast tráº¡ng thÃ¡i bÃ n sang OCCUPIED cho táº¥t cáº£ cÃ¡c mÃ¡y tráº¡m sÆ¡ Ä‘á»“ bÃ n
                        broadcastTableStatus(orderMsg.table, "OCCUPIED");

                        // 4. Báº¥t Ä‘á»“ng bá»™ lÆ°u bá»n vá»¯ng vÃ o SQL Server qua worker riÃªng, khÃ´ng cháº·n socket
                        final Order orderToSave = newOrder;
                        dbExecutor.submit(() -> {
                            try {
                                boolean saved = DatabaseManager.saveOrder(orderToSave);
                                if (saved) {
                                    log("[PERSISTENCE SUCCESS] Đơn hàng #" + orderToSave.getOrderId() + " đã lưu an toàn vào SQL Server.");
                                } else {
                                    log("[PERSISTENCE WARNING] Không thể lưu đơn #" + orderToSave.getOrderId() + " vào Database. Đã đưa vào In-Memory Retry Queue.");
                                    dbRetryQueue.offer(orderToSave);
                                }
                            } catch (Exception ex) {
                                log("[PERSISTENCE ERROR] Lỗi ngoại lệ khi lưu đơn #" + orderToSave.getOrderId() + ": " + ex.getMessage());
                                dbRetryQueue.offer(orderToSave);
                            }
                        });
                        break;

                    default:
                        break;
                }
            }
        } catch (SocketException ignored) {
            // Client ngáº¯t káº¿t ná»‘i
        } catch (IOException e) {
            log("Lá»—i I/O client [" + sessionId + "]: " + e.getMessage());
        } finally {
            if (session != null) {
                int disconnectedTable = session.currentTable;
                activeSessions.remove(sessionId);
                unbindTableSubscriber(disconnectedTable, sessionId);
                log("Thiáº¿t bá»‹ #" + sessionId + " (BÃ n " + disconnectedTable + ") Ä‘Ã£ rá»i máº¡ng.");
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
            log("Cáº­p nháº­t ÄÆ¡n #" + orderId + " (BÃ n " + order.getTable() + ") -> " + MessageProtocol.translateStatus(newStatus));

            // Báº¯n tÃ­n hiá»‡u vá» táº¥t cáº£ Client Ä‘ang theo dÃµi bÃ n Ä‘Ã³
            MessageProtocol.OrderStatusUpdateMessage updateMsg = new MessageProtocol.OrderStatusUpdateMessage(orderId, order.getTable(), newStatus);
            String updateJson = MessageProtocol.toJson(updateMsg);

            Set<String> subscribers = tableSubscribers.get(order.getTable());
            if (subscribers != null && !subscribers.isEmpty()) {
                for (String sid : subscribers) {
                    ClientSession s = activeSessions.get(sid);
                    if (s != null) s.send(updateJson);
                }
                log("ÄÃ£ thÃ´ng bÃ¡o tráº¡ng thÃ¡i má»›i vá» BÃ n " + order.getTable());
            }

            // KÃ­ch hoáº¡t callback UI
            dispatch(() -> {
                for (BiConsumer<Integer, String> l : orderStatusChangedListeners) l.accept(orderId, newStatus);
            });

            // Náº¿u Barista báº¥m HoÃ n táº¥t (DONE) -> Tá»± Ä‘á»™ng kÃ­ch hoáº¡t trá»« tá»“n kho nguyÃªn váº­t liá»‡u (BOM)
            if ("DONE".equalsIgnoreCase(newStatus)) {
                synchronized (order) {
                    if (!order.isStockDeducted()) {
                        order.setStockDeducted(true);
                        dbExecutor.submit(() -> {
                            try {
                                DatabaseManager.updateOrderStatusInDb(orderId, "DONE");
                                DatabaseManager.deductStockForOrder(order);
                            } catch (Exception ex) {
                                System.err.println("[SERVER] Lỗi trừ kho định lượng: " + ex.getMessage());
                            }
                        });
                    }
                }
            } else {
                dbExecutor.submit(() -> DatabaseManager.updateOrderStatusInDb(orderId, newStatus));
            }

            return true;
        }
        return false;
    }

    public void broadcastTableStatus(int tableNumber, String status) {
        if (tableNumber > 0 && status != null) {
            tableStatuses.put(tableNumber, status.toUpperCase());
            if ("EMPTY".equalsIgnoreCase(status) || "OCCUPIED".equalsIgnoreCase(status) || "SERVING".equalsIgnoreCase(status)) {
                tableReservations.remove(tableNumber);
            }
        }
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

    public String getTableStatus(int tableNumber) {
        return tableStatuses.getOrDefault(tableNumber, "EMPTY");
    }

    public ReservationInfo getReservation(int tableNumber) {
        return tableReservations.get(tableNumber);
    }

    public void reserveTable(int tableNumber, String customerName, String phone, String time, int guests, String note) {
        ReservationInfo info = new ReservationInfo(tableNumber, customerName, phone, time, guests, note);
        tableReservations.put(tableNumber, info);
        broadcastTableStatus(tableNumber, "RESERVED");
        log("ÄÃ£ Ä‘áº·t trÆ°á»›c BÃ n " + tableNumber + " cho khÃ¡ch " + customerName + " (" + time + ").");
    }

    public void cancelReservation(int tableNumber) {
        tableReservations.remove(tableNumber);
        broadcastTableStatus(tableNumber, "EMPTY");
        log("ÄÃ£ há»§y Ä‘áº·t trÆ°á»›c BÃ n " + tableNumber + " -> Trá»‘ng.");
    }

    public java.util.List<Order> getOrdersForTable(int tableNumber) {
        java.util.List<Order> result = new java.util.ArrayList<>();
        for (Order o : activeOrders.values()) {
            if (o.getTable() == tableNumber) {
                String st = o.getStatus() != null ? o.getStatus().toUpperCase() : "QUEUED";
                if (!"PAID".equals(st) && !"CANCELLED".equals(st)) {
                    result.add(o);
                }
            }
        }
        result.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
        return result;
    }

    public Order getCombinedOrderForTable(int tableNumber) {
        java.util.List<Order> orders = getOrdersForTable(tableNumber);
        if (orders.isEmpty()) return null;
        if (orders.size() == 1) return orders.get(0);

        java.util.List<cafe.models.OrderItem> combinedItems = new java.util.ArrayList<>();
        double totalDiscount = 0;
        double totalVat = 0;
        String voucher = null;
        StringBuilder notes = new StringBuilder();
        long earliestTs = orders.get(0).getTimestamp();
        String status = orders.get(orders.size() - 1).getStatus();

        for (Order o : orders) {
            if (o.getItems() != null) {
                combinedItems.addAll(o.getItems());
            }
            totalDiscount += o.getDiscountAmount();
            totalVat += o.getVatAmount();
            if (o.getVoucherCode() != null && !o.getVoucherCode().isEmpty()) {
                voucher = o.getVoucherCode();
            }
            if (o.getNote() != null && !o.getNote().trim().isEmpty()) {
                if (notes.length() > 0) notes.append(" | ");
                notes.append(o.getNote().trim());
            }
            if (o.getTimestamp() < earliestTs) earliestTs = o.getTimestamp();
        }

        return new Order(
            orders.get(0).getOrderId(),
            tableNumber,
            "DINE_IN",
            combinedItems,
            status,
            totalDiscount,
            totalVat,
            voucher,
            notes.toString(),
            earliestTs
        );
    }

    public synchronized boolean transferTable(int fromTable, int toTable) {
        if (fromTable <= 0 || toTable <= 0 || fromTable == toTable) return false;
        java.util.List<Order> fromOrders = getOrdersForTable(fromTable);
        for (Order o : fromOrders) {
            o.setTable(toTable);
        }
        ReservationInfo res = tableReservations.remove(fromTable);
        if (res != null && fromOrders.isEmpty()) {
            tableReservations.put(toTable, new ReservationInfo(toTable, res.customerName, res.phone, res.reservationTime, res.guestCount, res.note));
            broadcastTableStatus(fromTable, "EMPTY");
            broadcastTableStatus(toTable, "RESERVED");
            log("ÄÃ£ chuyá»ƒn lá»‹ch Ä‘áº·t bÃ n tá»« BÃ n " + fromTable + " sang BÃ n " + toTable + ".");
            return true;
        }
        broadcastTableStatus(fromTable, "EMPTY");
        broadcastTableStatus(toTable, "OCCUPIED");
        log("ÄÃ£ chuyá»ƒn toÃ n bá»™ Ä‘Æ¡n tá»« BÃ n " + fromTable + " sang BÃ n " + toTable + ".");
        return true;
    }

    public synchronized boolean mergeTables(int sourceTable, int targetTable) {
        if (sourceTable <= 0 || targetTable <= 0 || sourceTable == targetTable) return false;
        java.util.List<Order> sourceOrders = getOrdersForTable(sourceTable);
        java.util.List<Order> targetOrders = getOrdersForTable(targetTable);

        if (sourceOrders.isEmpty()) {
            broadcastTableStatus(sourceTable, "EMPTY");
            return false;
        }

        if (!targetOrders.isEmpty()) {
            Order primaryTarget = targetOrders.get(0);
            java.util.List<cafe.models.OrderItem> mergedItems = new java.util.ArrayList<>();
            if (primaryTarget.getItems() != null) {
                mergedItems.addAll(primaryTarget.getItems());
            }
            double extraDiscount = 0;
            for (Order src : sourceOrders) {
                if (src.getItems() != null) {
                    mergedItems.addAll(src.getItems());
                }
                extraDiscount += src.getDiscountAmount();
                activeOrders.remove(src.getOrderId());
            }
            primaryTarget.setItems(mergedItems);
            primaryTarget.setDiscountAmount(primaryTarget.getDiscountAmount() + extraDiscount);
        } else {
            for (Order src : sourceOrders) {
                src.setTable(targetTable);
            }
        }

        broadcastTableStatus(sourceTable, "EMPTY");
        broadcastTableStatus(targetTable, "OCCUPIED");
        log("ÄÃ£ gá»™p hÃ³a Ä‘Æ¡n BÃ n " + sourceTable + " vÃ o BÃ n " + targetTable + ".");
        return true;
    }

    public synchronized boolean checkoutTable(int tableNumber) {
        java.util.List<Order> orders = getOrdersForTable(tableNumber);
        for (Order o : orders) {
            o.setStatus("PAID");
            final int oid = o.getOrderId();
            dispatch(() -> {
                for (BiConsumer<Integer, String> l : orderStatusChangedListeners) l.accept(oid, "PAID");
            });
            dbExecutor.submit(() -> {
                try {
                    DatabaseManager.updateOrderStatusInDb(oid, "PAID");
                    DatabaseManager.deductStockForOrder(o);
                } catch (Exception ignored) {}
            });
        }
        tableReservations.remove(tableNumber);
        broadcastTableStatus(tableNumber, "EMPTY");
        log("ÄÃ£ thanh toÃ¡n & giáº£i phÃ³ng BÃ n " + tableNumber + " -> Trá»‘ng.");
        return true;
    }

    public synchronized boolean cancelOrder(int orderId) {
        Order o = activeOrders.get(orderId);
        if (o == null) return false;
        o.setStatus("CANCELLED");
        int tbl = o.getTable();
        dispatch(() -> {
            for (BiConsumer<Integer, String> l : orderStatusChangedListeners) l.accept(orderId, "CANCELLED");
        });
        dbExecutor.submit(() -> DatabaseManager.updateOrderStatusInDb(orderId, "CANCELLED"));
        if (tbl > 0 && getOrdersForTable(tbl).isEmpty()) {
            broadcastTableStatus(tbl, "EMPTY");
        }
        log("ÄÃ£ há»§y ÄÆ¡n #" + orderId + (tbl > 0 ? " (BÃ n " + tbl + ")" : "") + ".");
        return true;
    }

    public synchronized void resetToSeedState() {
        activeOrders.clear();
        tableStatuses.clear();
        tableReservations.clear();
        orderIdCounter.set(1020);
        seedInitialActiveOrders();
        for (Map.Entry<Integer, String> entry : tableStatuses.entrySet()) {
            broadcastTableStatus(entry.getKey(), entry.getValue());
        }
        log("ÄÃ£ khÃ´i phá»¥c dá»¯ liá»‡u máº«u (Seed Data) toÃ n há»‡ thá»‘ng.");
    }

    public static void validateOrderIntegrity(MessageProtocol.NewOrderMessage orderMsg) { validateAndSanitizeOrder(orderMsg); }
    public static void validateAndSanitizeOrder(MessageProtocol.NewOrderMessage orderMsg) {
        if (orderMsg == null || orderMsg.items == null) return;
        if (!(orderMsg.items instanceof java.util.ArrayList)) {
            orderMsg.items = new java.util.ArrayList<>(orderMsg.items);
        }
        List<cafe.models.MenuItem> catalog = cafe.models.MenuRepository.getAllItems();
        Map<Integer, cafe.models.MenuItem> catalogMap = new HashMap<>();
        Map<String, cafe.models.MenuItem> nameMap = new HashMap<>();
        for (cafe.models.MenuItem mi : catalog) {
            catalogMap.put(mi.getId(), mi);
            nameMap.put(mi.getName().toLowerCase().trim(), mi);
        }

        double calculatedSubtotal = 0.0;
        Iterator<cafe.models.OrderItem> it = orderMsg.items.iterator();
        while (it.hasNext()) {
            cafe.models.OrderItem item = it.next();
            if (item == null) {
                it.remove();
                continue;
            }
            if (item.getQty() <= 0) {
                item.setQty(1);
            } else if (item.getQty() > 99) {
                item.setQty(99);
            }
            cafe.models.MenuItem matched = null;
            if (item.getProductId() > 0) {
                matched = catalogMap.get(item.getProductId());
            }
            if (matched == null && item.getName() != null) {
                matched = nameMap.get(item.getName().toLowerCase().trim());
            }

            if (matched != null) {
                if (!matched.isInStock() || !matched.isActive()) {
                    it.remove();
                    continue;
                }
                item.setProductId(matched.getId());
                item.setBasePrice(matched.getPrice());
                if ("S".equalsIgnoreCase(item.getSizeCode())) {
                    item.setSizeDelta(-5000);
                } else if ("L".equalsIgnoreCase(item.getSizeCode())) {
                    item.setSizeDelta(7000);
                } else {
                    item.setSizeDelta(0);
                }

                // Kiểm tra và chuẩn hóa giá Topping từ MenuRepository, loại bỏ Topping giả mạo hoặc giá sai
                if (item.getToppings() != null && !item.getToppings().isEmpty()) {
                    List<cafe.models.ToppingItem> validToppings = new java.util.ArrayList<>();
                    List<cafe.models.ToppingItem> allowedToppings = cafe.models.MenuRepository.getToppingsForItem(matched);
                    Map<Integer, cafe.models.ToppingItem> allowedMap = new HashMap<>();
                    Map<String, cafe.models.ToppingItem> allowedNameMap = new HashMap<>();
                    if (allowedToppings != null) {
                        for (cafe.models.ToppingItem t : allowedToppings) {
                            allowedMap.put(t.getToppingId(), t);
                            allowedNameMap.put(t.getName().toLowerCase().trim(), t);
                        }
                    }
                    for (cafe.models.ToppingItem clientTop : item.getToppings()) {
                        if (clientTop == null) continue;
                        cafe.models.ToppingItem genuineTop = null;
                        if (clientTop.getToppingId() > 0) {
                            genuineTop = allowedMap.get(clientTop.getToppingId());
                        }
                        if (genuineTop == null && clientTop.getName() != null) {
                            genuineTop = allowedNameMap.get(clientTop.getName().toLowerCase().trim());
                        }
                        if (genuineTop != null) {
                            validToppings.add(new cafe.models.ToppingItem(genuineTop.getToppingId(), genuineTop.getName(), genuineTop.getPrice()));
                        }
                    }
                    item.setToppings(validToppings);
                }

                item.recalculateUnitPrice();
            } else {
                it.remove();
                continue;
            }
            calculatedSubtotal += item.getSubtotal();
        }

        if (orderMsg.voucherCode != null && !orderMsg.voucherCode.trim().isEmpty()) {
            cafe.models.Voucher v = cafe.models.MenuRepository.getVoucher(orderMsg.voucherCode.trim());
            if (v != null && calculatedSubtotal >= v.getMinOrderAmount()) {
                orderMsg.discountAmount = v.calculateDiscount(calculatedSubtotal);
            } else {
                orderMsg.discountAmount = 0.0;
            }
        } else {
            orderMsg.discountAmount = 0.0;
        }
    }

    public Map<Integer, Order> getActiveOrders() {
        return activeOrders;
    }

    public java.util.List<Order> getAllOrdersNewestFirst() {
        java.util.List<Order> list = new java.util.ArrayList<>(activeOrders.values());
        list.sort((a, b) -> Integer.compare(b.getOrderId(), a.getOrderId()));
        return list;
    }

    public Map<String, ClientSession> getActiveSessions() {
        return activeSessions;
    }

    public boolean isServerRunning() {
        return isRunning;
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
        log("MÃ¡y chá»§ Ä‘Ã£ dá»«ng.");
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
