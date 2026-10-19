package cafe.repositories.mock;

import cafe.models.Order;
import cafe.models.OrderItem;
import cafe.repositories.OrderRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MockOrderRepository implements OrderRepository {
    private final List<Order> orders = new ArrayList<>();
    private final AtomicInteger idCounter = new AtomicInteger(1025);

    public MockOrderRepository() {
        initMockData();
    }

    private void initMockData() {
        long now = System.currentTimeMillis();
        long minute = 60 * 1000L;
        long hour = 60 * minute;

        orders.add(new Order(1025, 2, Arrays.asList(
                new OrderItem("Cà phê Muối Cố Đô", 2, "Ít ngọt", 35000, "CONFIRMED"),
                new OrderItem("Croissant Bơ Pháp", 1, "Hâm nóng", 32000, "CONFIRMED")
        ), "CREATED", now - 2 * minute, "Bàn 02", UUID.nameUUIDFromBytes("ORDER-1025".getBytes()).toString()));

        orders.add(new Order(1024, 5, Arrays.asList(
                new OrderItem("Cold Brew Cam Vàng", 1, "", 42000, "CONFIRMED"),
                new OrderItem("Trà Đào Cam Sả", 1, "Nhiều đá", 39000, "CONFIRMED")
        ), "CONFIRMED", now - 6 * minute, "Bàn 05", UUID.nameUUIDFromBytes("ORDER-1024".getBytes()).toString()));

        orders.add(new Order(1023, 3, Arrays.asList(
                new OrderItem("Caramel Macchiato", 2, "Nóng", 45000, "CONFIRMED"),
                new OrderItem("Tiramisu Cacao Ý", 2, "", 38000, "CONFIRMED")
        ), "PREPARING", now - 12 * minute, "staff01", UUID.nameUUIDFromBytes("ORDER-1023".getBytes()).toString()));

        orders.add(new Order(1022, 9, Arrays.asList(
                new OrderItem("Matcha Đá Xay Uji", 1, "Ít ngọt", 49000, "CONFIRMED"),
                new OrderItem("Cookie & Cream Đá Xay", 1, "", 48000, "CONFIRMED")
        ), "READY", now - 18 * minute, "Bàn 09", UUID.nameUUIDFromBytes("ORDER-1022".getBytes()).toString()));

        orders.add(new Order(1021, 1, Arrays.asList(
                new OrderItem("Cà phê Đen Phin", 1, "Đậm đà", 25000, "CONFIRMED"),
                new OrderItem("Cà phê Sữa Đá", 1, "", 29000, "CONFIRMED")
        ), "SERVED", now - 25 * minute, "Bàn 01", UUID.nameUUIDFromBytes("ORDER-1021".getBytes()).toString()));

        orders.add(new Order(1020, 4, Arrays.asList(
                new OrderItem("Trà Sen Vàng Kem Cheese", 2, "", 45000, "CONFIRMED"),
                new OrderItem("Cheesecake Nướng Basque", 1, "", 45000, "CONFIRMED")
        ), "WAITING_PAYMENT", now - 35 * minute, "Bàn 04", UUID.nameUUIDFromBytes("ORDER-1020".getBytes()).toString()));

        orders.add(new Order(1019, 6, Arrays.asList(
                new OrderItem("Bạc Xỉu Kem Béo", 2, "Nhiều sữa", 32000, "CONFIRMED"),
                new OrderItem("Sinh Tố Bơ Dừa Non", 1, "", 45000, "CONFIRMED")
        ), "PAID", now - 45 * minute, "cashier01", UUID.nameUUIDFromBytes("ORDER-1019".getBytes()).toString()));

        orders.add(new Order(1018, 8, Arrays.asList(
                new OrderItem("Trà Vải Hoa Hồng", 1, "", 42000, "CONFIRMED"),
                new OrderItem("Croissant Bơ Pháp", 2, "", 32000, "CONFIRMED")
        ), "COMPLETED", now - 55 * minute, "cashier01", UUID.nameUUIDFromBytes("ORDER-1018".getBytes()).toString()));

        orders.add(new Order(1017, 3, Arrays.asList(
                new OrderItem("Trà Ô Long Mãng Cầu", 1, "Hết mãng cầu", 42000, "OUT_OF_STOCK")
        ), "CANCELLED", now - 70 * minute, "barista01", UUID.nameUUIDFromBytes("ORDER-1017".getBytes()).toString()));

        orders.add(new Order(1016, 10, Arrays.asList(
                new OrderItem("Cold Brew Cam Vàng", 2, "", 42000, "CONFIRMED"),
                new OrderItem("Cheesecake Nướng Basque", 2, "", 45000, "CONFIRMED")
        ), "COMPLETED", now - 85 * minute, "cashier02", UUID.nameUUIDFromBytes("ORDER-1016".getBytes()).toString()));

        orders.add(new Order(1015, 2, Arrays.asList(
                new OrderItem("Cà phê Sữa Đá", 3, "", 29000, "CONFIRMED")
        ), "COMPLETED", now - 2 * hour, "cashier01", UUID.nameUUIDFromBytes("ORDER-1015".getBytes()).toString()));

        orders.add(new Order(1014, 5, Arrays.asList(
                new OrderItem("Trà Đào Cam Sả", 2, "", 39000, "CONFIRMED"),
                new OrderItem("Tiramisu Cacao Ý", 1, "", 38000, "CONFIRMED")
        ), "COMPLETED", now - 3 * hour, "cashier02", UUID.nameUUIDFromBytes("ORDER-1014".getBytes()).toString()));

        orders.add(new Order(1013, 7, Arrays.asList(
                new OrderItem("Caramel Macchiato", 1, "", 45000, "CONFIRMED")
        ), "CANCELLED", now - 3 * hour - 20 * minute, "staff02", UUID.nameUUIDFromBytes("ORDER-1013".getBytes()).toString()));

        orders.add(new Order(1012, 11, Arrays.asList(
                new OrderItem("Matcha Đá Xay Uji", 2, "", 49000, "CONFIRMED"),
                new OrderItem("Bạc Xỉu Kem Béo", 1, "", 32000, "CONFIRMED")
        ), "COMPLETED", now - 4 * hour, "cashier01", UUID.nameUUIDFromBytes("ORDER-1012".getBytes()).toString()));

        orders.add(new Order(1011, 1, Arrays.asList(
                new OrderItem("Cà phê Đen Phin", 2, "", 25000, "CONFIRMED"),
                new OrderItem("Croissant Bơ Pháp", 2, "", 32000, "CONFIRMED")
        ), "COMPLETED", now - 5 * hour, "cashier01", UUID.nameUUIDFromBytes("ORDER-1011".getBytes()).toString()));

        long day = 24 * hour;

        orders.add(new Order(1010, 4, Arrays.asList(
                new OrderItem("Cà phê Muối Cố Đô", 2, "", 35000, "CONFIRMED"),
                new OrderItem("Sinh Tố Bơ Dừa Non", 1, "", 45000, "CONFIRMED")
        ), "COMPLETED", now - 2 * day - 2 * hour, "cashier02", UUID.nameUUIDFromBytes("ORDER-1010".getBytes()).toString()));

        orders.add(new Order(1009, 8, Arrays.asList(
                new OrderItem("Cookie & Cream Đá Xay", 2, "", 48000, "CONFIRMED")
        ), "COMPLETED", now - 3 * day - 1 * hour, "cashier01", UUID.nameUUIDFromBytes("ORDER-1009".getBytes()).toString()));

        orders.add(new Order(1008, 9, Arrays.asList(
                new OrderItem("Trà Sen Vàng Kem Cheese", 3, "", 45000, "CONFIRMED"),
                new OrderItem("Tiramisu Cacao Ý", 2, "", 38000, "CONFIRMED")
        ), "COMPLETED", now - 4 * day - 3 * hour, "cashier01", UUID.nameUUIDFromBytes("ORDER-1008".getBytes()).toString()));

        orders.add(new Order(1007, 2, Arrays.asList(
                new OrderItem("Bạc Xỉu Kem Béo", 1, "", 32000, "CONFIRMED"),
                new OrderItem("Cà phê Sữa Đá", 1, "", 29000, "CONFIRMED")
        ), "COMPLETED", now - 5 * day - 2 * hour, "cashier02", UUID.nameUUIDFromBytes("ORDER-1007".getBytes()).toString()));

        orders.add(new Order(1006, 6, Arrays.asList(
                new OrderItem("Cold Brew Cam Vàng", 3, "", 42000, "CONFIRMED")
        ), "COMPLETED", now - 6 * day - 4 * hour, "cashier01", UUID.nameUUIDFromBytes("ORDER-1006".getBytes()).toString()));

        orders.add(new Order(1005, 3, Arrays.asList(
                new OrderItem("Trà Vải Hoa Hồng", 2, "", 42000, "CONFIRMED"),
                new OrderItem("Cheesecake Nướng Basque", 1, "", 45000, "CONFIRMED")
        ), "COMPLETED", now - 9 * day, "cashier02", UUID.nameUUIDFromBytes("ORDER-1005".getBytes()).toString()));

        orders.add(new Order(1004, 1, Arrays.asList(
                new OrderItem("Cà phê Đen Phin", 4, "", 25000, "CONFIRMED")
        ), "COMPLETED", now - 11 * day, "cashier01", UUID.nameUUIDFromBytes("ORDER-1004".getBytes()).toString()));

        orders.add(new Order(1003, 5, Arrays.asList(
                new OrderItem("Caramel Macchiato", 2, "", 45000, "CONFIRMED")
        ), "COMPLETED", now - 13 * day, "cashier01", UUID.nameUUIDFromBytes("ORDER-1003".getBytes()).toString()));

        orders.add(new Order(1002, 10, Arrays.asList(
                new OrderItem("Sinh Tố Bơ Dừa Non", 2, "", 45000, "CONFIRMED")
        ), "COMPLETED", now - 16 * day, "cashier02", UUID.nameUUIDFromBytes("ORDER-1002".getBytes()).toString()));

        orders.add(new Order(1001, 7, Arrays.asList(
                new OrderItem("Cà phê Sữa Đá", 2, "", 29000, "CONFIRMED"),
                new OrderItem("Croissant Bơ Pháp", 1, "", 32000, "CONFIRMED")
        ), "COMPLETED", now - 20 * day, "cashier01", UUID.nameUUIDFromBytes("ORDER-1001".getBytes()).toString()));
    }

    @Override
    public List<Order> findAll() {
        List<Order> list = new ArrayList<>(orders);
        list.sort(Comparator.comparingInt(Order::getOrderId).reversed());
        return list;
    }

    @Override
    public Order findById(int orderId) {
        return orders.stream().filter(o -> o.getOrderId() == orderId).findFirst().orElse(null);
    }

    @Override
    public synchronized Order save(Order order) {
        order.setOrderId(idCounter.incrementAndGet());
        if (order.getRequestId() == null || order.getRequestId().trim().isEmpty()) {
            order.setRequestId(UUID.randomUUID().toString());
        }
        orders.add(0, order);
        return order;
    }

    @Override
    public synchronized boolean updateStatus(int orderId, String newStatus) {
        Order o = findById(orderId);
        if (o != null) {
            o.setStatus(newStatus);
            return true;
        }
        return false;
    }

    @Override
    public List<Order> searchAndFilter(String keyword, String status, String dateFilter) {
        long now = System.currentTimeMillis();
        long dayMs = 24L * 3600L * 1000L;

        return orders.stream().filter(o -> {
            boolean matchKw = keyword == null || keyword.trim().isEmpty() ||
                    String.valueOf(o.getOrderId()).contains(keyword.trim()) ||
                    String.valueOf(o.getTable()).contains(keyword.trim()) ||
                    (o.getCreatedBy() != null && o.getCreatedBy().toLowerCase().contains(keyword.toLowerCase().trim()));
            boolean matchStatus = status == null || status.trim().isEmpty() ||
                    "ALL".equalsIgnoreCase(status.trim()) || "Tất cả".equalsIgnoreCase(status.trim()) ||
                    o.getStatus().equalsIgnoreCase(status.trim());

            boolean matchDate = true;
            if (dateFilter != null && !dateFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(dateFilter.trim())) {
                String df = dateFilter.trim().toUpperCase();
                long ageMs = Math.max(0L, now - o.getTimestamp());
                if (df.contains("TODAY") || df.contains("HÔM NAY")) {
                    matchDate = ageMs <= dayMs;
                } else if (df.contains("WEEK") || df.contains("TUẦN") || df.contains("7 NGÀY")) {
                    matchDate = ageMs <= 7L * dayMs;
                } else if (df.contains("MONTH") || df.contains("THÁNG") || df.contains("30 NGÀY")) {
                    matchDate = ageMs <= 30L * dayMs;
                }
            }

            return matchKw && matchStatus && matchDate;
        }).sorted(Comparator.comparingInt(Order::getOrderId).reversed()).collect(Collectors.toList());
    }

    @Override
    public List<Order> getRecentOrders(int limit) {
        return orders.stream()
                .sorted(Comparator.comparingInt(Order::getOrderId).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
}
