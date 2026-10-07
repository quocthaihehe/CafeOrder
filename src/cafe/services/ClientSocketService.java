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
import java.util.function.Consumer;

public class ClientSocketService {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5000;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread listenerThread;
    private volatile boolean running = false;

    // Callbacks
    private Consumer<Boolean> onConnectionStateChanged;
    private Consumer<MessageProtocol.OrderAckMessage> onOrderAckReceived;
    private Consumer<MessageProtocol.OrderStatusUpdateMessage> onStatusUpdateReceived;
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

    public void setOnErrorOccurred(Consumer<String> callback) {
        this.onErrorOccurred = callback;
    }

    public boolean connect(String host, int port, int tableNumber) {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            running = true;

            // Báo danh với Server qua gói tin HELLO
            MessageProtocol.HelloMessage hello = new MessageProtocol.HelloMessage(tableNumber);
            out.print(MessageProtocol.toJson(hello));
            out.flush();

            // Khởi động luồng đọc tin nhắn ngầm
            startListening();

            dispatch(() -> {
                if (onConnectionStateChanged != null) onConnectionStateChanged.accept(true);
            });
            return true;
        } catch (IOException e) {
            dispatch(() -> {
                if (onConnectionStateChanged != null) onConnectionStateChanged.accept(false);
                if (onErrorOccurred != null) onErrorOccurred.accept("Không thể kết nối đến máy chủ: " + e.getMessage());
            });
            return false;
        }
    }

    public boolean connect(int tableNumber) {
        return connect(DEFAULT_HOST, DEFAULT_PORT, tableNumber);
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public boolean sendOrder(int tableNumber, List<OrderItem> items) {
        if (!isConnected() || out == null || items == null || items.isEmpty()) {
            return false;
        }
        try {
            MessageProtocol.NewOrderMessage msg = new MessageProtocol.NewOrderMessage(tableNumber, items);
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
                while (running && (line = in.readLine()) != null) {
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
            } finally {
                disconnect();
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void disconnect() {
        running = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
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
