package cafe.client;

import cafe.models.MessageProtocol;
import cafe.models.MessageType;
import cafe.models.OrderItem;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class NetworkManager {
    public static int tableNumber = 1;
    public static List<OrderItem> cart = new ArrayList<>(); // Giỏ hàng lưu tại đây
    
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;
    
    public static boolean isConnected() {
        return out != null;
    }
    
    // Hàm kết nối khi khách nhập số bàn ở LoginUI
    public static boolean connect(int table) {
        try {
            socket = new Socket("localhost", 5000);
            out = new PrintWriter(socket.getOutputStream(), true, java.nio.charset.StandardCharsets.UTF_8);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            
            tableNumber = table;
            
            // Báo danh với Server
            MessageProtocol.HelloMessage hello = new MessageProtocol.HelloMessage(table);
            out.print(MessageProtocol.toJson(hello));
            out.flush();
            
            // Bật luồng lắng nghe thông báo
            startListener();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    // Hàm gửi nguyên cái giỏ hàng lên Server
    public static void sendOrder() {
        if (cart.isEmpty()) return;
        
        MessageProtocol.NewOrderMessage msg = new MessageProtocol.NewOrderMessage(tableNumber, cart);
        out.print(MessageProtocol.toJson(msg));
        out.flush();
        
        cart.clear(); // Gửi xong thì xóa giỏ
    }
    
    private static void startListener() {
        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    MessageType type = MessageProtocol.getMessageType(line);
                    if (type == MessageType.ORDER_ACK) {
                        MessageProtocol.OrderAckMessage ack = MessageProtocol.fromJson(line, MessageProtocol.OrderAckMessage.class);
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(null, "✅ Đặt món thành công!\nMã đơn: " + ack.orderId + "\nĐang chờ pha chế...");
                        });
                    } else if (type == MessageType.ORDER_STATUS_UPDATE) {
                        MessageProtocol.OrderStatusUpdateMessage update = MessageProtocol.fromJson(line, MessageProtocol.OrderStatusUpdateMessage.class);
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(null, "🔔 LOA THÔNG BÁO 🔔\nĐơn hàng #" + update.orderId + " đã đổi thành: " + MessageProtocol.translateStatus(update.status));
                        });
                    }
                }
            } catch (Exception e) {
                System.out.println("Mất kết nối Server.");
                e.printStackTrace();
            }
        }).start();
    }
}
