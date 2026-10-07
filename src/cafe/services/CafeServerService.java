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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CafeServerService {
    private static final int DEFAULT_PORT = 5000;

    private ServerSocket serverSocket;
    private final AtomicInteger orderIdCounter = new AtomicInteger(1000);
    private final Map<Integer, PrintWriter> clientMap = new ConcurrentHashMap<>();
    private final Map<Integer, Order> activeOrders = new ConcurrentHashMap<>();
    private volatile boolean isRunning = false;

    // Callbacks
    private Consumer<Order> onOrderReceived;
    private BiConsumer<Integer, Boolean> onTableConnectionChanged;
    private Consumer<String> onServerLog;

    public void setOnOrderReceived(Consumer<Order> callback) {
        this.onOrderReceived = callback;
    }

    public void setOnTableConnectionChanged(BiConsumer<Integer, Boolean> callback) {
        this.onTableConnectionChanged = callback;
    }

    public void setOnServerLog(Consumer<String> callback) {
        this.onServerLog = callback;
    }

    public void start(int port) {
        if (isRunning) return;
        isRunning = true;

        Thread serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                log("Máy chủ KDS pha chế đã mở cổng " + port + ". Đang sẵn sàng đón nhận đơn.");

                while (isRunning) {
                    Socket clientSocket = serverSocket.accept();
                    handleClientConnection(clientSocket);
                }
            } catch (IOException e) {
                if (isRunning) {
                    log("Lỗi Server Socket: " + e.getMessage());
                }
            } finally {
                stop();
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
    }

    public void start() {
        start(DEFAULT_PORT);
    }

    private void handleClientConnection(Socket socket) {
        Thread clientThread = new Thread(() -> {
            int currentTable = -1;
            PrintWriter out = null;
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)
            ) {
                out = writer;
                String line;
                while ((line = in.readLine()) != null) {
                    MessageType type = MessageProtocol.getMessageType(line);
                    if (type == null) continue;

                    switch (type) {
                        case HELLO:
                            MessageProtocol.HelloMessage hello = MessageProtocol.fromJson(line, MessageProtocol.HelloMessage.class);
                            currentTable = hello.table;
                            clientMap.put(currentTable, out);
                            log("Bàn số " + currentTable + " đã kết nối mạng.");
                            final int tConnect = currentTable;
                            dispatch(() -> {
                                if (onTableConnectionChanged != null) onTableConnectionChanged.accept(tConnect, true);
                            });
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
                            log("Nhận Đơn hàng #" + newOrderId + " (" + newOrder.getOrderTypeLabel() + ") - " + newOrder.getTotalQuantity() + " món.");

                            // Tự động lưu bền vững vào SQL Server Database CafeOrderDB
                            DatabaseManager.saveOrder(newOrder);

                            // Phản hồi ORDER_ACK
                            MessageProtocol.OrderAckMessage ack = new MessageProtocol.OrderAckMessage(newOrderId, "QUEUED", activeOrders.size());
                            out.print(MessageProtocol.toJson(ack));
                            out.flush();

                            // Bắn sự kiện lên giao diện KDS
                            dispatch(() -> {
                                if (onOrderReceived != null) onOrderReceived.accept(newOrder);
                            });
                            break;

                        default:
                            break;
                    }
                }
            } catch (SocketException e) {
                // Client ngắt kết nối
            } catch (IOException e) {
                log("Lỗi I/O client bàn " + currentTable + ": " + e.getMessage());
            } finally {
                if (currentTable != -1) {
                    clientMap.remove(currentTable);
                    log("Bàn số " + currentTable + " đã ngắt kết nối.");
                    final int tDisconnect = currentTable;
                    dispatch(() -> {
                        if (onTableConnectionChanged != null) onTableConnectionChanged.accept(tDisconnect, false);
                    });
                }
                try {
                    if (socket != null && !socket.isClosed()) socket.close();
                } catch (IOException ignored) {}
            }
        });
        clientThread.setDaemon(true);
        clientThread.start();
    }

    public boolean updateOrderStatus(int orderId, String newStatus) {
        Order order = activeOrders.get(orderId);
        if (order != null) {
            order.setStatus(newStatus);
            log("Cập nhật Đơn #" + orderId + " (Bàn " + order.getTable() + ") -> " + MessageProtocol.translateStatus(newStatus));

            // Bắn tín hiệu về Client bàn đó
            PrintWriter out = clientMap.get(order.getTable());
            if (out != null) {
                MessageProtocol.OrderStatusUpdateMessage updateMsg = new MessageProtocol.OrderStatusUpdateMessage(orderId, newStatus);
                out.print(MessageProtocol.toJson(updateMsg));
                out.flush();
                log("Đã thông báo trạng thái mới về Bàn " + order.getTable());
            } else {
                log("Không tìm thấy kết nối Bàn " + order.getTable() + " (có thể đã rời mạng).");
            }
            return true;
        }
        return false;
    }

    public Map<Integer, Order> getActiveOrders() {
        return activeOrders;
    }

    public Map<Integer, PrintWriter> getClientMap() {
        return clientMap;
    }

    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}
        log("Máy chủ đã dừng.");
    }

    private void log(String message) {
        dispatch(() -> {
            if (onServerLog != null) onServerLog.accept(message);
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
            // Hỗ trợ kiểm thử tự động / môi trường không có JavaFX Toolkit
            action.run();
        }
    }
}
