package cafe.server;

import cafe.models.MessageProtocol;
import cafe.models.Order;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class CafeServer {
    private static final int PORT = 5000;
    public static AtomicInteger orderIdCounter = new AtomicInteger(1000);
    public static OrderQueue orderQueue = new OrderQueue();
    
    // Cuốn sổ lưu vết các bàn đang kết nối (Key: Số bàn, Value: Luồng gửi tin)
    public static Map<Integer, PrintWriter> clientMap = new ConcurrentHashMap<>();
    
    // Lưu tạm danh sách order để Chủ quán dễ cập nhật trạng thái
    public static Map<Integer, Order> activeOrders = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        cafe.utils.ConsoleHelper.setupUTF8();
        System.out.println("=== Server Quầy Pha Chế đang khởi động ===");
        
        // 1. Luồng Băng chuyền xử lý Order
        Thread processorThread = new Thread(() -> {
            System.out.println("[HỆ THỐNG] Băng chuyền đã sẵn sàng.");
            while (true) {
                try {
                    Order order = orderQueue.takeOrder();
                    activeOrders.put(order.getOrderId(), order); // Lưu vào bộ nhớ để quản lý
                    
                    System.out.println("\n--- [NHẬN ORDER #" + order.getOrderId() + " TỪ BÀN " + order.getTable() + "] ---");
                    for (int i = 0; i < order.getItems().size(); i++) {
                        System.out.println(" - " + order.getItems().get(i).toString());
                    }
                    System.out.println("Trạng thái: " + MessageProtocol.translateStatus(order.getStatus()));
                    System.out.println("-------------------------------------------------");
                    System.out.print("> Nhập lệnh (MãOrder SốTrạngThái) hoặc gõ 0 xem danh sách: ");
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        processorThread.start();
        
        // 2. Luồng gõ lệnh cho Chủ Quán (Phase 3)
        Thread adminConsoleThread = new Thread(() -> {
            Scanner scanner = cafe.utils.ConsoleHelper.getScanner();
            while (true) {
                String input = scanner.nextLine();
                if (input != null && !input.trim().isEmpty()) {
                    String[] parts = input.split(" ");
                    
                    // TÍNH NĂNG MỚI: Bảng tổng sắp Dashboard
                    if (parts[0].equalsIgnoreCase("list") || parts[0].equals("0")) {
                        System.out.println("\n=== DANH SÁCH ĐƠN HÀNG ĐANG CHỜ XỬ LÝ ===");
                        boolean hasActive = false;
                        for (Order o : activeOrders.values()) {
                            if (!o.getStatus().equals("DONE")) {
                                hasActive = true;
                                // Gộp nhanh tên các món lại thành 1 chuỗi để dễ đọc
                                String itemSummary = o.getItems().stream()
                                    .map(i -> i.getQty() + "x " + i.getName())
                                    .collect(Collectors.joining(", "));
                                    
                                System.out.printf("[Mã: %d] - Bàn %d | %s | %s\n", 
                                    o.getOrderId(), o.getTable(), itemSummary, MessageProtocol.translateStatus(o.getStatus()));
                            }
                        }
                        if (!hasActive) {
                            System.out.println("(Không có đơn hàng nào đang chờ)");
                        }
                        System.out.println("=========================================");
                        System.out.print("> Nhập lệnh (MãOrder SốTrạngThái) hoặc gõ 0 xem danh sách: ");
                        continue;
                    }
                    
                    // Cập nhật trạng thái
                    if (parts.length >= 2) {
                        try {
                            int orderId = Integer.parseInt(parts[0]);
                            int statusChoice = Integer.parseInt(parts[1]);
                            String newStatus = "";
                            
                            if (statusChoice == 1) newStatus = "QUEUED";
                            else if (statusChoice == 2) newStatus = "PREPARING";
                            else if (statusChoice == 3) newStatus = "DONE";
                            else {
                                System.out.println("Lỗi: Mã trạng thái không hợp lệ (1: CHỜ, 2: PHA CHẾ, 3: HOÀN THÀNH)");
                                System.out.print("> Nhập lệnh (MãOrder SốTrạngThái) hoặc gõ 0 xem danh sách: ");
                                continue;
                            }
                            
                            Order order = activeOrders.get(orderId);
                            if (order != null) {
                                order.setStatus(newStatus);
                                System.out.println("[SERVER] Đã cập nhật Order #" + orderId + " thành: " + MessageProtocol.translateStatus(newStatus));
                                
                                // Nếu đã HOÀN THÀNH thì xóa khỏi danh sách hiển thị cho sạch sẽ
                                if (newStatus.equals("DONE")) {
                                    activeOrders.remove(orderId);
                                }
                                
                                // Bắn thông báo về đúng bàn đó
                                PrintWriter out = clientMap.get(order.getTable());
                                if (out != null) {
                                    MessageProtocol.OrderStatusUpdateMessage updateMsg = new MessageProtocol.OrderStatusUpdateMessage(orderId, newStatus);
                                    out.print(MessageProtocol.toJson(updateMsg));
                                    out.flush();
                                    System.out.println("[SERVER] Đã bắn loa thông báo về Bàn " + order.getTable());
                                } else {
                                    System.out.println("[SERVER] Không thể gửi tin, Bàn " + order.getTable() + " đã ngắt mạng.");
                                }
                            } else {
                                System.out.println("Lỗi: Không tìm thấy Order #" + orderId + " (Hoặc đơn này đã hoàn thành).");
                            }
                        } catch (Exception e) {
                            System.out.println("Lỗi cú pháp. Vui lòng nhập: <Mã_Order> <Mã_Trạng_Thái(1|2|3)>");
                        }
                    } else {
                         System.out.println("Vui lòng gõ theo cú pháp: <MãOrder> <MãTrạngThái> hoặc gõ 0 để xem danh sách.");
                    }
                    System.out.print("> Nhập lệnh (MãOrder SốTrạngThái) hoặc gõ 0 xem danh sách: ");
                }
            }
        });
        adminConsoleThread.start();
        
        // 3. Luồng chính: Đón khách kết nối
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server đang chờ kết nối tại port " + PORT + "...");
            System.out.println("--- HƯỚNG DẪN CHỦ QUÁN ---");
            System.out.println("1. Gõ [0] hoặc [list] rồi Enter để Xem danh sách đơn đang chờ.");
            System.out.println("2. Gõ [MãOrder SốTrạngThái] để Cập nhật đơn.");
            System.out.println("   (Số 1: ĐANG CHỜ, Số 2: ĐANG PHA CHẾ, Số 3: ĐÃ HOÀN THÀNH)");
            System.out.println("   Ví dụ gõ: 1001 2 (Đổi đơn 1001 sang Đang pha chế)");
            System.out.println("--------------------------");
            System.out.print("> Nhập lệnh (MãOrder SốTrạngThái) hoặc gõ 0 xem danh sách: ");
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                Thread thread = new Thread(handler);
                thread.start();
            }
        } catch (IOException e) {
            System.err.println("[LỖI SERVER] Khởi tạo Server thất bại: " + e.getMessage());
        }
    }
}
