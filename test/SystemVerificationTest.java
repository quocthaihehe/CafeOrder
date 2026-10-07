package test;

import cafe.models.*;
import cafe.services.CafeServerService;
import cafe.services.ClientSocketService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SystemVerificationTest {
    public static void main(String[] args) throws Exception {
        System.out.println("=== BẮT ĐẦU KIỂM THỬ HỆ THỐNG CAFE ORDER MVC ===");

        // 1. Kiểm tra Model: MenuRepository
        List<MenuItem> menu = MenuRepository.getAllItems();
        System.out.println("[TEST 1] MenuRepository có " + menu.size() + " món ăn.");
        if (menu.isEmpty()) throw new RuntimeException("Menu rỗng!");

        List<String> categories = MenuRepository.getCategories();
        System.out.println("[TEST 1] Danh mục: " + categories);

        // 2. Kiểm tra Model: CartModel
        CartModel cart = new CartModel();
        MenuItem item1 = menu.get(0);
        MenuItem item2 = menu.get(1);

        cart.addItem(item1, 2, "Ít ngọt");
        cart.addItem(item2, 1, "Nhiều đá");
        System.out.println("[TEST 2] Cart total quantity: " + cart.getTotalQuantity() + " (Kỳ vọng: 3)");
        System.out.println("[TEST 2] Cart total amount: " + cart.getTotalAmount());
        if (cart.getTotalQuantity() != 3) throw new RuntimeException("Cart quantity sai!");

        // 3. Kiểm tra Socket & Network Service (Port 5055 để không đụng cổng chính)
        final int testPort = 5055;
        CafeServerService server = new CafeServerService();
        CountDownLatch orderLatch = new CountDownLatch(1);
        CountDownLatch ackLatch = new CountDownLatch(1);
        CountDownLatch statusLatch = new CountDownLatch(1);

        server.setOnOrderReceived(order -> {
            System.out.println("[TEST 3 - SERVER] Nhận đơn #" + order.getOrderId() + " từ Bàn " + order.getTable());
            orderLatch.countDown();

            // Cập nhật trạng thái sang PREPARING
            new Thread(() -> {
                try { Thread.sleep(200); } catch (Exception ignored) {}
                server.updateOrderStatus(order.getOrderId(), "PREPARING");
            }).start();
        });

        server.start(testPort);
        Thread.sleep(500); // chờ server bind

        ClientSocketService client = new ClientSocketService();
        client.setOnOrderAckReceived(ack -> {
            System.out.println("[TEST 3 - CLIENT] Nhận ACK cho đơn #" + ack.orderId + ", Trạng thái: " + ack.status);
            ackLatch.countDown();
        });

        client.setOnStatusUpdateReceived(update -> {
            System.out.println("[TEST 3 - CLIENT] Nhận Status Update: Đơn #" + update.orderId + " -> " + update.status);
            statusLatch.countDown();
        });

        boolean connected = client.connect("localhost", testPort, 5);
        System.out.println("[TEST 3] Client kết nối bàn 5: " + connected);
        if (!connected) throw new RuntimeException("Client không kết nối được!");

        // Gửi đơn hàng
        List<OrderItem> orderItems = new ArrayList<>(cart.getItems());
        boolean sent = client.sendOrder(5, orderItems);
        System.out.println("[TEST 3] Gửi đơn thành công: " + sent);

        boolean orderReceived = orderLatch.await(3, TimeUnit.SECONDS);
        boolean ackReceived = ackLatch.await(3, TimeUnit.SECONDS);
        boolean statusReceived = statusLatch.await(3, TimeUnit.SECONDS);

        client.disconnect();
        server.stop();

        if (orderReceived && ackReceived && statusReceived) {
            System.out.println("\n🎉 TẤT CẢ CÁC BÀI KIỂM THỬ ĐÃ VƯỢT QUA XUẤT SẮC! 🎉");
        } else {
            System.err.println("❌ Có bài kiểm thử thất bại! Order: " + orderReceived + ", ACK: " + ackReceived + ", Status: " + statusReceived);
            System.exit(1);
        }
    }
}
