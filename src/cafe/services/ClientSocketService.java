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
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 5000;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread listenerThread;
    private Thread heartbeatThread;
    private volatile boolean running = false;

    private final String localSessionId = UUID.randomUUID().toString().substring(0, 8);
    private int activeTableNumber = 5;
    private String deviceType = "KIOSK"; // "KIOSK" or "STAFF_PAD"

    // Callbacks
    private Consumer<Boolean> onConnectionStateChanged;
    private Consumer<MessageProtocol.OrderAckMessage> onOrderAckReceived;
    private Consumer<MessageProtocol.OrderStatusUpdateMessage> onStatusUpdateReceived;
    private BiConsumer<Integer, String> onTableStatusReceived;
    private Consumer<String> onErrorOccurred;

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

    /**
     * Kết nối ngầm trước tới Server ngay khi màn hình Login vừa hiển thị.
     * Khi người dùng nhấn Đăng nhập, Socket đã mở sẵn nên thời gian chờ bằng 0ms.
     */
    public void warmupConnectAsync() {
        Thread t = new Thread(() -> {
            synchronized (this) {
                if (!isConnected()) {
                    openSocketInternal(DEFAULT_HOST, DEFAULT_PORT);
                }
            }
        }, "Socket-Warmup");
        t.setDaemon(true);
        t.start();
    }

    private synchronized boolean openSocketInternal(String host, int port) {
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
        return openSocketInternal(DEFAULT_HOST, DEFAULT_PORT);
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
        MessageProtocol.HelloMessage hello = new MessageProtocol.HelloMessage(tableNumber, localSessionId, this.deviceType);
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
        return connect(DEFAULT_HOST, DEFAULT_PORT, tableNumber, "KIOSK");
    }

    public boolean connectStaff(int tableNumber) {
        return connect(DEFAULT_HOST, DEFAULT_PORT, tableNumber, "STAFF_PAD");
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
        try {
            String idempotencyKey = UUID.randomUUID().toString();
            MessageProtocol.NewOrderMessage msg = new MessageProtocol.NewOrderMessage(
                tableNumber, orderType, discount, voucherCode, note, idempotencyKey, customerPhone, customerName, items
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
                            });
                            break;

                        case ORDER_STATUS_UPDATE:
                            MessageProtocol.OrderStatusUpdateMessage update = MessageProtocol.fromJson(rawJson, MessageProtocol.OrderStatusUpdateMessage.class);
                            dispatch(() -> {
                                if (onStatusUpdateReceived != null) onStatusUpdateReceived.accept(update);
                            });
                            break;

                        case TABLE_STATUS_BROADCAST:
                            MessageProtocol.TableStatusBroadcastMessage tBroadcast = MessageProtocol.fromJson(rawJson, MessageProtocol.TableStatusBroadcastMessage.class);
                            dispatch(() -> {
                                if (onTableStatusReceived != null) onTableStatusReceived.accept(tBroadcast.table, tBroadcast.status);
                            });
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
