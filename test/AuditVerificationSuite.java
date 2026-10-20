package test;

import cafe.models.*;
import cafe.services.CafeServerService;
import cafe.services.ClientSocketService;
import cafe.services.DatabaseManager;

import java.sql.Connection;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * BỘ TEST KIỂM CHỨNG THỰC TẾ (REALITY-CHECK AUDIT VERIFICATION SUITE)
 * Không dùng reflection để qua mặt, kiểm tra hành vi thực tế của code.
 */
public class AuditVerificationSuite {
    private static final int VERIFY_PORT = 5077;

    public static void main(String[] args) {
        System.out.println("===============================================================================");
        System.out.println("  BẮT ĐẦU CHẠY BỘ KIỂM CHỨNG THỰC TẾ (CODE & BEHAVIORAL VERIFICATION)");
        System.out.println("===============================================================================\n");

        CafeServerService server = new CafeServerService();
        server.start(VERIFY_PORT);
        try { Thread.sleep(400); } catch (Exception ignored) {}

        // ---------------------------------------------------------------------
        // 1. KIỂM TRA IDEMPOTENCY KEY TRÊN SOCKET THẬT
        // ---------------------------------------------------------------------
        System.out.println("--- 1. KIỂM CHỨNG XỬ LÝ TRÙNG LẶP (IDEMPOTENCY KEY) ---");
        try {
            ClientSocketService client = new ClientSocketService();
            client.connect("127.0.0.1", VERIFY_PORT, 8, "KIOSK");

            String key = "IDEMP-VERIFY-" + System.currentTimeMillis();
            CountDownLatch latch1 = new CountDownLatch(1);
            CountDownLatch latch2 = new CountDownLatch(1);
            AtomicInteger oid1 = new AtomicInteger(-1);
            AtomicInteger oid2 = new AtomicInteger(-1);

            client.setOnOrderAckReceived(ack -> {
                if (oid1.get() == -1) {
                    oid1.set(ack.orderId);
                    latch1.countDown();
                } else {
                    oid2.set(ack.orderId);
                    latch2.countDown();
                }
            });

            List<OrderItem> items = new ArrayList<>(Collections.singletonList(new OrderItem(1, "Cà phê Đen Phin", 1, 25000, "M", 0, 100, 100, "COLD", null, "")));
            // Gửi lần 1
            client.sendOrderWithKey(8, "DINE_IN", 0, null, "Lần 1", key, null, null, items);
            latch1.await(2, TimeUnit.SECONDS);

            // Gửi lần 2 với cùng key
            client.sendOrderWithKey(8, "DINE_IN", 0, null, "Lần 2", key, null, null, items);
            latch2.await(2, TimeUnit.SECONDS);

            System.out.println("   [Socket Sequential] Lần 1 OrderId: " + oid1.get() + " | Lần 2 OrderId: " + oid2.get());
            if (oid1.get() > 0 && oid1.get() == oid2.get()) {
                System.out.println("   -> Kết quả: PASSED (Gửi tuần tự cùng key trả về đúng OrderId cũ, không sinh đơn mới)");
            } else {
                System.out.println("   -> Kết quả: FAILED (Sinh đơn mới khi gửi lặp key!)");
            }
            client.disconnect();
        } catch (Exception e) {
            System.out.println("   -> Lỗi: " + e.getMessage());
        }

        // ---------------------------------------------------------------------
        // 2. KIỂM TRA ĐUA LỆNH ĐỒNG THỜI (CONCURRENT RACE CONDITION TRÊN IDEMPOTENCY)
        // ---------------------------------------------------------------------
        System.out.println("\n--- 2. KIỂM CHỨNG ĐUA LỆNH ĐỒNG THỜI (CONCURRENT IDEMPOTENCY RACE) ---");
        try {
            String raceKey = "RACE-KEY-" + System.currentTimeMillis();
            ExecutorService pool = Executors.newFixedThreadPool(2);
            CountDownLatch startGate = new CountDownLatch(1);
            CountDownLatch endGate = new CountDownLatch(2);
            AtomicInteger raceOid1 = new AtomicInteger(-1);
            AtomicInteger raceOid2 = new AtomicInteger(-1);

            pool.submit(() -> {
                try {
                    ClientSocketService c1 = new ClientSocketService();
                    c1.connect("127.0.0.1", VERIFY_PORT, 9, "KIOSK");
                    c1.setOnOrderAckReceived(ack -> { raceOid1.set(ack.orderId); endGate.countDown(); });
                    startGate.await();
                    c1.sendOrderWithKey(9, "DINE_IN", 0, null, "Đua lệnh 1", raceKey, null, null,
                            new ArrayList<>(Collections.singletonList(new OrderItem(1, "Cà phê Đen Phin", 1, 25000, "M", 0, 100, 100, "COLD", null, ""))));
                } catch (Exception ignored) {}
            });

            pool.submit(() -> {
                try {
                    ClientSocketService c2 = new ClientSocketService();
                    c2.connect("127.0.0.1", VERIFY_PORT, 9, "KIOSK");
                    c2.setOnOrderAckReceived(ack -> { raceOid2.set(ack.orderId); endGate.countDown(); });
                    startGate.await();
                    c2.sendOrderWithKey(9, "DINE_IN", 0, null, "Đua lệnh 2", raceKey, null, null,
                            new ArrayList<>(Collections.singletonList(new OrderItem(1, "Cà phê Đen Phin", 1, 25000, "M", 0, 100, 100, "COLD", null, ""))));
                } catch (Exception ignored) {}
            });

            startGate.countDown(); // Bắn 2 socket đồng thời
            endGate.await(3, TimeUnit.SECONDS);
            pool.shutdown();

            System.out.println("   [Socket Concurrent] Thread 1 OrderId: " + raceOid1.get() + " | Thread 2 OrderId: " + raceOid2.get());
            if (raceOid1.get() > 0 && raceOid1.get() == raceOid2.get()) {
                System.out.println("   -> Kết quả: PASSED (An toàn tuyệt đối dưới tải đồng thời, 2 luồng chỉ tạo 1 đơn)");
            } else {
                System.out.println("   -> Kết quả: FAILED / RACE CONDITION DETECTED! (Tồn tại race condition: 2 luồng đồng thời tạo ra 2 đơn khác nhau)");
            }
        } catch (Exception e) {
            System.out.println("   -> Lỗi: " + e.getMessage());
        }

        // ---------------------------------------------------------------------
        // 3. KIỂM TRA XÁC THỰC GIÁ VÀ VOUCHER PHÍA SERVER TRÊN SOCKET THẬT
        // ---------------------------------------------------------------------
        System.out.println("\n--- 3. KIỂM CHỨNG XÁC THỰC GIÁ & VOUCHER PHÍA SERVER ---");
        try {
            ClientSocketService client = new ClientSocketService();
            client.connect("127.0.0.1", VERIFY_PORT, 10, "KIOSK");

            CountDownLatch ackLatch = new CountDownLatch(1);
            AtomicInteger createdOrderId = new AtomicInteger(-1);
            client.setOnOrderAckReceived(ack -> {
                createdOrderId.set(ack.orderId);
                ackLatch.countDown();
            });

            // Client cố tình gửi giá 0 đ cho món ID 1 (giá niêm yết 25,000 đ) và voucher bịa "GIAM100K"
            OrderItem tamperedItem = new OrderItem(1, "Cà phê Đen Phin", 2, 0.0, "L", 0.0, 100, 100, "COLD", null, "");
            client.sendOrder(10, "DINE_IN", 100000.0, "GIAM100K", "Gian lận giá", Collections.singletonList(tamperedItem));
            ackLatch.await(2, TimeUnit.SECONDS);

            int oid = createdOrderId.get();
            Order serverOrder = server.getActiveOrders().get(oid);
            if (serverOrder != null) {
                // Giá niêm yết 25,000 + Size L 7,000 = 32,000 đ x 2 = 64,000 đ.
                // Voucher FAKE không tồn tại -> discount = 0.
                double actualSubtotal = serverOrder.getSubtotalAmount();
                double actualDiscount = serverOrder.getDiscountAmount();
                double actualTotal = serverOrder.getTotalAmount();
                System.out.println("   [Server Tamper Check] Subtotal thực: " + actualSubtotal + " đ | Discount thực: " + actualDiscount + " đ | Total: " + actualTotal + " đ");
                if (actualSubtotal == 64000.0 && actualDiscount == 0.0 && actualTotal == 64000.0) {
                    System.out.println("   -> Kết quả: PASSED (Server đã tự động sửa giá 0 đ thành giá niêm yết 64,000 đ và hủy voucher lậu)");
                } else {
                    System.out.println("   -> Kết quả: FAILED (Server chấp nhận giá sai lệch hoặc voucher lậu!)");
                }
            } else {
                System.out.println("   -> Kết quả: FAILED (Không tìm thấy Order trên Server)");
            }
            client.disconnect();
        } catch (Exception e) {
            System.out.println("   -> Lỗi: " + e.getMessage());
        }

        // ---------------------------------------------------------------------
        // 4. KIỂM TRA MÓN HẾT HÀNG TRÊN SERVER
        // ---------------------------------------------------------------------
        System.out.println("\n--- 4. KIỂM CHỨNG CHẶN MÓN HẾT HÀNG TẠI SERVER ---");
        try {
            MenuItem itemToDisable = MenuRepository.findById(1);
            itemToDisable.setInStock(false);

            MessageProtocol.NewOrderMessage msg = new MessageProtocol.NewOrderMessage(
                11, "DINE_IN", 0, null, "Đặt món hết", "KEY-OOS", null, null,
                new ArrayList<>(Collections.singletonList(new OrderItem(1, "Cà phê Đen Phin", 1, 25000, "M", 0, 100, 100, "COLD", null, "")))
            );
            CafeServerService.validateOrderIntegrity(msg);
            itemToDisable.setInStock(true); // reset

            boolean hasOutOfStockItem = false;
            for (OrderItem it : msg.items) {
                if (it.getProductId() == 1) {
                    hasOutOfStockItem = true;
                }
            }
            if (!hasOutOfStockItem) {
                System.out.println("   -> Kết quả: PASSED (Server đã lọc bỏ món hết hàng khỏi đơn)");
            } else {
                System.out.println("   -> Kết quả: FAILED (Server vẫn chấp nhận món đã hết hàng inStock=false vào đơn!)");
            }
        } catch (Exception e) {
            System.out.println("   -> Lỗi: " + e.getMessage());
        }

        // ---------------------------------------------------------------------
        // 5. KIỂM TRA TRỪ KHO BOM KHI CHUYỂN 'DONE' LẶP LẠI
        // ---------------------------------------------------------------------
        System.out.println("\n--- 5. KIỂM CHỨNG TRỪ KHO BOM KHI CHUYỂN 'DONE' LẶP LẠI ---");
        try {
            Order testOrder = new Order(7777, 12, "DINE_IN",
                Collections.singletonList(new OrderItem(2, "Cà phê Sữa Đá", 1, 29000, "M", 0, 100, 100, "COLD", null, "")),
                "PREPARING", 0, 0, null, "", System.currentTimeMillis()
            );
            server.getActiveOrders().put(7777, testOrder);

            DatabaseManager.IngredientItem ing = null;
            for (DatabaseManager.IngredientItem i : DatabaseManager.getAllIngredients()) {
                if (i.id == 1) { ing = i; break; }
            }
            double stockBefore = ing != null ? ing.currentStock : -1;

            // Bấm DONE lần 1
            server.updateOrderStatus(7777, "DONE");
            Thread.sleep(200);
            double stockAfterFirst = ing != null ? ing.currentStock : -1;

            // Bấm DONE lần 2 (nhân viên bấm nhầm hoặc thao tác lại)
            server.updateOrderStatus(7777, "DONE");
            Thread.sleep(200);
            double stockAfterSecond = ing != null ? ing.currentStock : -1;

            System.out.println("   [BOM Deduct] Ban đầu: " + stockBefore + "g | Sau lần 1: " + stockAfterFirst + "g | Sau lần 2: " + stockAfterSecond + "g");
            if (stockAfterFirst == stockAfterSecond) {
                System.out.println("   -> Kết quả: PASSED (Có cờ chặn, không trừ kho lặp lại)");
            } else {
                System.out.println("   -> Kết quả: FAILED / DOUBLE DEDUCTION BUG! (Bấm DONE lần 2 bị trừ kho lần 2: " + stockAfterFirst + "g -> " + stockAfterSecond + "g)");
            }
        } catch (Exception e) {
            System.out.println("   -> Lỗi: " + e.getMessage());
        }

        // ---------------------------------------------------------------------
        // 6. KIỂM TRA KẾT NỐI DATABASE THẬT (REAL SQL SERVER CHECK)
        // ---------------------------------------------------------------------
        System.out.println("\n--- 6. KIỂM CHỨNG KẾT NỐI CƠ SỞ DỮ LIỆU SQL SERVER THẬT ---");
        try {
            boolean available = DatabaseManager.isAvailable();
            System.out.println("   [DB Check] DatabaseManager.isAvailable(): " + available);
            if (available) {
                try (Connection c = DatabaseManager.getConnection()) {
                    System.out.println("   -> Kết quả: VERIFIED (SQL Server đang chạy và kết nối thành công: " + c.getMetaData().getDatabaseProductName() + ")");
                }
            } else {
                System.out.println("   -> Kết quả: NOT RUNNING / FALLBACK MODE (SQL Server localhost:1433 không bật. Hệ thống đang chạy 100% trên In-Memory Seed!)");
            }
        } catch (Exception e) {
            System.out.println("   -> Lỗi DB: " + e.getMessage());
        }

        server.stop();
        System.out.println("\n===============================================================================");
        System.out.println("  HOÀN THÀNH BỘ KIỂM CHỨNG THỰC TẾ");
        System.out.println("===============================================================================");
    }
}
