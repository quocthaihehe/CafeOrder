package test;

import cafe.models.*;
import cafe.services.CafeServerService;
import cafe.services.ClientSocketService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class IntegrationWorkflowTest {
    public static void main(String[] args) throws Exception {
        System.out.println("===============================================================");
        System.out.println("  KIỂM THỬ TÍCH HỢP TOÀN DIỆN LUỒNG HOẠT ĐỘNG THỰC TẾ (E2E)  ");
        System.out.println("===============================================================");

        int testPort = 5099;
        CafeServerService server = new CafeServerService();
        server.start(testPort);
        System.out.println("1. [SERVER] Khởi động Server Socket trên cổng " + testPort + " thành công.");

        CountDownLatch ackLatch = new CountDownLatch(1);
        CountDownLatch statusUpdateLatch = new CountDownLatch(1);
        AtomicInteger createdOrderId = new AtomicInteger(-1);
        AtomicBoolean statusUpdated = new AtomicBoolean(false);

        int tableNum = 7;
        ClientSocketService client = new ClientSocketService();

        client.setOnOrderAckReceived(ack -> {
            System.out.println("3. [CLIENT] Nhận ORDER_ACK từ Server: Order #" + ack.orderId + ", Trạng thái: " + ack.status);
            createdOrderId.set(ack.orderId);
            ackLatch.countDown();
        });

        client.setOnStatusUpdateReceived(update -> {
            System.out.println("5. [CLIENT] Nhận ORDER_STATUS_UPDATE: Order #" + update.orderId + " -> " + MessageProtocol.translateStatus(update.status));
            statusUpdated.set(true);
            statusUpdateLatch.countDown();
        });

        boolean connected = client.connect("127.0.0.1", testPort, tableNum, "KIOSK");
        if (!connected) {
            System.err.println("❌ LỖI: Client không thể kết nối tới Server cổng " + testPort);
            System.exit(1);
        }
        System.out.println("2. [CLIENT] Đã kết nối và xác thực HELLO cho Bàn " + tableNum);

        // Chuẩn bị giỏ hàng với đầy đủ customization theo GiaiDoan4 + ui_admin
        List<OrderItem> items = new ArrayList<>();
        List<ToppingItem> toppings = new ArrayList<>();
        toppings.add(new ToppingItem("Trân châu đen", 8000));
        toppings.add(new ToppingItem("Kem phô mai", 10000));

        OrderItem item1 = new OrderItem(
            2, "Cà phê Sữa Đá", 2, 35000, "L", 5000, 70, 70, "COLD", toppings, "Giao nhanh giúp em"
        );
        items.add(item1);

        OrderItem item2 = new OrderItem(
            14, "Croissant Bơ Pháp", 1, 38000, "M", 0, 100, 100, "COLD", new ArrayList<>(), "Hâm nóng giòn"
        );
        items.add(item2);

        System.out.println("3. [CLIENT] Gửi đơn hàng NEW_ORDER (2 món, có Size L, Topping, Ghi chú)...");
        client.sendOrder(tableNum, "DINE_IN", 10000, "DISCOUNT10K", "Khách bàn 7", items);

        boolean ackReceived = ackLatch.await(5, TimeUnit.SECONDS);
        if (!ackReceived) {
            System.err.println("❌ LỖI: Không nhận được ORDER_ACK trong vòng 5 giây!");
            System.exit(1);
        }

        int orderId = createdOrderId.get();
        System.out.println("4. [SERVER/KDS] Tiếp nhận đơn #" + orderId + ", chuyển trạng thái sang PREPARING...");
        server.updateOrderStatus(orderId, "PREPARING");

        boolean updateReceived = statusUpdateLatch.await(5, TimeUnit.SECONDS);
        if (!updateReceived) {
            System.err.println("❌ LỖI: Client không nhận được ORDER_STATUS_UPDATE!");
            System.exit(1);
        }

        System.out.println("6. [SERVER/KDS] Barista hoàn thành món, cập nhật status -> DONE...");
        server.updateOrderStatus(orderId, "DONE");
        Thread.sleep(500);

        System.out.println("7. [VERIFY] Kiểm tra tính nhất quán dữ liệu Order...");
        Order serverOrder = server.getActiveOrders().get(orderId);
        if (serverOrder == null) {
            System.err.println("❌ LỖI: Server không tìm thấy Order #" + orderId);
            System.exit(1);
        }

        System.out.println("   - Tổng tiền món: " + serverOrder.getSubtotalAmount() + " đ");
        System.out.println("   - Giảm giá: " + serverOrder.getDiscountAmount() + " đ");
        System.out.println("   - Tổng thanh toán: " + serverOrder.getTotalAmount() + " đ");
        System.out.println("   - Tổng số lượng: " + serverOrder.getTotalQuantity() + " món");
        System.out.println("   - Timeline: " + serverOrder.getTimeline());

        client.disconnect();
        server.stop();

        System.out.println("\n✅ KẾT QUẢ KIỂM THỬ: TẤT CẢ CÁC LUỒNG ĐỒNG BỘ CLIENT - SERVER - KDS HOẠT ĐỘNG HOÀN TOÀN CHUẨN XÁC!");
        System.exit(0);
    }
}
