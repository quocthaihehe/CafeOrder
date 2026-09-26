package cafe.client;

import cafe.models.MenuItem;
import cafe.models.MessageProtocol;
import cafe.models.MessageType;
import cafe.models.OrderItem;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class TableClient {
    private static final String SERVER_IP = "localhost";
    private static final int PORT = 5000;
    
    private static final MenuItem[] menu = {
        new MenuItem("Cà phê đen", 20000),
        new MenuItem("Cà phê sữa", 25000),
        new MenuItem("Bạc xỉu", 25000),
        new MenuItem("Sinh tố dâu", 35000)
    };

    public static void main(String[] args) {
        cafe.utils.ConsoleHelper.setupUTF8();
        Scanner scanner = cafe.utils.ConsoleHelper.getScanner();
        System.out.println("=== ỨNG DỤNG ĐẶT MÓN TẠI BÀN ===");
        
        System.out.print("Vui lòng nhập số bàn của bạn: ");
        int tableNumber = -1;
        try {
            tableNumber = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Số bàn không hợp lệ, mặc định là Bàn 1");
            tableNumber = 1;
        }

        try (
            Socket socket = new Socket(SERVER_IP, PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true, java.nio.charset.StandardCharsets.UTF_8)
        ) {
            System.out.println("[HỆ THỐNG] Đã kết nối tới quầy pha chế thành công!");

            // 1. Gửi gói tin HELLO
            MessageProtocol.HelloMessage helloMsg = new MessageProtocol.HelloMessage(tableNumber);
            out.print(MessageProtocol.toJson(helloMsg));
            out.flush();

            // 2. Chạy luồng ngầm Lắng nghe tin nhắn (Phase 3)
            Thread listenerThread = new Thread(() -> {
                try {
                    String responseLine;
                    while ((responseLine = in.readLine()) != null) {
                        MessageType type = MessageProtocol.getMessageType(responseLine);
                        if (type == MessageType.ORDER_ACK) {
                            MessageProtocol.OrderAckMessage ack = MessageProtocol.fromJson(responseLine, MessageProtocol.OrderAckMessage.class);
                            System.out.println("\n✅ ĐẶT MÓN THÀNH CÔNG!");
                            System.out.println("- Mã Order: " + ack.orderId);
                            System.out.println("- Trạng thái: " + MessageProtocol.translateStatus(ack.status));
                            System.out.println("- Vị trí hàng chờ: " + ack.position);
                            System.out.print("> Chọn tiếp món hoặc thoát: "); 
                        } else if (type == MessageType.ORDER_STATUS_UPDATE) {
                            MessageProtocol.OrderStatusUpdateMessage update = MessageProtocol.fromJson(responseLine, MessageProtocol.OrderStatusUpdateMessage.class);
                            System.out.println("\n🔔 [LOA THÔNG BÁO] Đơn hàng #" + update.orderId + " của bạn đã chuyển sang trạng thái: " + MessageProtocol.translateStatus(update.status) + " 🔔");
                            System.out.print("> Chọn tiếp món hoặc thoát: ");
                        }
                    }
                } catch (IOException e) {
                    System.out.println("\n❌ Mất kết nối mạng hoặc Server đã đóng.");
                    System.exit(0);
                }
            });
            listenerThread.start();

            // 3. Chế độ chọn món (Luồng chính)
            List<OrderItem> cart = new ArrayList<>();
            while (true) {
                System.out.println("\n--- MENU ---");
                for (int i = 0; i < menu.length; i++) {
                    System.out.println((i + 1) + ". " + menu[i].getName() + " - " + String.format("%.0f", menu[i].getPrice()) + "đ");
                }
                System.out.println("0. GỬI ORDER (Kết thúc gọi món)");
                System.out.println("-1. THOÁT KHỎI QUÁN (Đóng app)");
                
                System.out.print("> Chọn món (nhập số): ");
                String input = scanner.nextLine();
                
                if (input.equals("-1")) {
                    System.out.println("Cảm ơn quý khách. Tạm biệt!");
                    break;
                }
                
                if (input.equals("0")) {
                    if (cart.isEmpty()) {
                        System.out.println("Bạn chưa chọn món nào!");
                        continue;
                    }
                    
                    System.out.println("[HỆ THỐNG] Đang gửi Order tới Server...");
                    MessageProtocol.NewOrderMessage orderMsg = new MessageProtocol.NewOrderMessage(tableNumber, cart);
                    out.print(MessageProtocol.toJson(orderMsg));
                    out.flush();
                    
                    cart.clear(); // Xóa giỏ, không gọi readLine() nữa vì đã có Listener lo.
                    continue;
                }
                
                try {
                    int choice = Integer.parseInt(input);
                    if (choice >= 1 && choice <= menu.length) {
                        MenuItem selectedItem = menu[choice - 1];
                        
                        System.out.print("Số lượng: ");
                        int qty = Integer.parseInt(scanner.nextLine());
                        
                        System.out.print("Ghi chú (ít đá, nhiều đường... - có thể bỏ qua): ");
                        String note = scanner.nextLine();
                        
                        cart.add(new OrderItem(selectedItem.getName(), qty, note));
                        System.out.println("=> Đã thêm vào giỏ: " + qty + "x " + selectedItem.getName());
                    } else {
                        System.out.println("Lựa chọn không hợp lệ!");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Vui lòng nhập một số hợp lệ!");
                }
            }

        } catch (ConnectException e) {
            System.err.println("❌ KHÔNG THỂ KẾT NỐI TỚI SERVER.");
        } catch (IOException e) {
            System.err.println("❌ Lỗi mạng: " + e.getMessage());
        }
    }
}
