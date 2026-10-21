package cafe.services;

import cafe.models.CategorySales;
import cafe.models.Order;
import cafe.models.OrderItem;
import cafe.models.Payment;
import cafe.models.ProductSales;
import cafe.models.ReportSummary;
import cafe.repositories.MenuItemRepository;
import cafe.repositories.OrderRepository;
import cafe.repositories.PaymentRepository;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ReportService {
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final MenuItemRepository menuItemRepository;

    public ReportService(OrderRepository orderRepository, PaymentRepository paymentRepository, MenuItemRepository menuItemRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.menuItemRepository = menuItemRepository;
    }

    private List<Order> filterOrdersByPeriod(List<Order> orders, String period) {
        if (period == null || period.trim().isEmpty() || "ALL".equalsIgnoreCase(period.trim())) {
            return orders;
        }
        String p = period.trim().toUpperCase();
        if (p.contains("ALL") || p.contains("TẤT CẢ")) {
            return orders;
        }
        long now = System.currentTimeMillis();
        long dayMs = 24L * 3600L * 1000L;
        long maxAgeMs;
        if (p.contains("TODAY") || p.contains("HÔM NAY")) {
            maxAgeMs = dayMs;
        } else if (p.contains("WEEK") || p.contains("TUẦN") || p.contains("7 NGÀY")) {
            maxAgeMs = 7L * dayMs;
        } else if (p.contains("MONTH") || p.contains("THÁNG") || p.contains("30 NGÀY")) {
            maxAgeMs = 30L * dayMs;
        } else {
            return orders;
        }
        return orders.stream()
                .filter(o -> Math.max(0L, now - o.getTimestamp()) <= maxAgeMs)
                .collect(Collectors.toList());
    }

    private boolean isRevenueEligibleOrder(Order o) {
        if (o == null) return false;
        if (!"COMPLETED".equalsIgnoreCase(o.getStatus()) && !"PAID".equalsIgnoreCase(o.getStatus())) {
            return false;
        }
        if (o.getTotalAmount() <= 0 || o.getTotalQuantity() <= 0) {
            return false;
        }
        if (paymentRepository != null) {
            List<Payment> payments = paymentRepository.findAllByOrderId(o.getOrderId());
            if (!payments.isEmpty()) {
                return payments.stream().anyMatch(p ->
                        "SUCCESS".equalsIgnoreCase(p.getStatus()) &&
                        Math.abs(p.getAmount() - o.getTotalAmount()) < 0.01
                );
            }
        }
        return true;
    }

    public ReportSummary getReportSummary(String period) {
        List<Order> orders = filterOrdersByPeriod(orderRepository.findAll(), period);
        double totalRev = 0;
        int completed = 0;
        int cancelled = 0;
        int pending = 0;

        for (Order o : orders) {
            if (isRevenueEligibleOrder(o)) {
                completed++;
                totalRev += o.getTotalAmount();
            } else if ("CANCELLED".equalsIgnoreCase(o.getStatus())) {
                cancelled++;
            } else {
                pending++;
            }
        }

        return new ReportSummary(totalRev, orders.size(), completed, cancelled, pending);
    }

    public List<ProductSales> getProductSalesReport(String sortBy) {
        return getProductSalesReport(sortBy, "ALL");
    }

    public List<ProductSales> getProductSalesReport(String sortBy, String period) {
        List<Order> orders = filterOrdersByPeriod(orderRepository.findAll(), period);
        Map<String, Integer> qtyMap = new HashMap<>();
        Map<String, Double> revMap = new HashMap<>();
        Map<String, String> catMap = new HashMap<>();

        for (Order o : orders) {
            // Chỉ tính doanh thu và sản lượng từ các đơn đã thanh toán hợp lệ (PAID / COMPLETED)
            if (!isRevenueEligibleOrder(o)) continue;
            for (OrderItem item : o.getItems()) {
                if (item == null || !item.isBillable() || item.getQty() <= 0) continue;
                qtyMap.put(item.getName(), qtyMap.getOrDefault(item.getName(), 0) + item.getQty());
                revMap.put(item.getName(), revMap.getOrDefault(item.getName(), 0.0) + item.getSubtotal());
            }
        }

        if (menuItemRepository != null) {
            for (cafe.models.MenuItem m : menuItemRepository.findAll()) {
                catMap.put(m.getName(), m.getCategory());
            }
        }

        List<ProductSales> list = new ArrayList<>();
        for (String name : qtyMap.keySet()) {
            String cat = catMap.getOrDefault(name, "Khác");
            list.add(new ProductSales(name, cat, qtyMap.get(name), revMap.get(name)));
        }

        if ("REVENUE".equalsIgnoreCase(sortBy)) {
            list.sort(Comparator.comparingDouble(ProductSales::getRevenue).reversed());
        } else {
            list.sort(Comparator.comparingInt(ProductSales::getQuantitySold).reversed());
        }

        return list;
    }

    public List<CategorySales> getCategorySalesReport() {
        return getCategorySalesReport("ALL");
    }

    public List<CategorySales> getCategorySalesReport(String period) {
        List<Order> orders = filterOrdersByPeriod(orderRepository.findAll(), period);
        Map<String, String> catMap = new HashMap<>();
        if (menuItemRepository != null) {
            for (cafe.models.MenuItem m : menuItemRepository.findAll()) {
                catMap.put(m.getName(), m.getCategory());
            }
        }

        Map<String, Set<Integer>> orderSetByCat = new HashMap<>();
        Map<String, Double> revMap = new HashMap<>();
        double grandTotal = 0;

        for (Order o : orders) {
            if (!isRevenueEligibleOrder(o)) continue;
            for (OrderItem item : o.getItems()) {
                if (item == null || !item.isBillable() || item.getQty() <= 0) continue;
                String cat = catMap.getOrDefault(item.getName(), "Khác");
                orderSetByCat.computeIfAbsent(cat, k -> new HashSet<>()).add(o.getOrderId());
                revMap.put(cat, revMap.getOrDefault(cat, 0.0) + item.getSubtotal());
                grandTotal += item.getSubtotal();
            }
        }

        List<CategorySales> list = new ArrayList<>();
        for (String cat : revMap.keySet()) {
            double rev = revMap.get(cat);
            double pct = grandTotal > 0 ? (rev / grandTotal) * 100.0 : 0.0;
            int distinctOrders = orderSetByCat.containsKey(cat) ? orderSetByCat.get(cat).size() : 0;
            list.add(new CategorySales(cat, distinctOrders, rev, pct));
        }

        list.sort(Comparator.comparingDouble(CategorySales::getRevenue).reversed());
        return list;
    }

    public Map<String, Double> getWeeklyRevenue() {
        Map<String, Double> weekData = new LinkedHashMap<>();
        long now = System.currentTimeMillis();
        long dayMs = 24L * 3600L * 1000L;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM");

        String[] dayKeys = new String[7];
        long[] dayStarts = new long[7];
        for (int i = 6; i >= 0; i--) {
            int idx = 6 - i;
            long ts = now - i * dayMs;
            dayKeys[idx] = (i == 0 ? "Hôm nay (" : "Ngày ") + sdf.format(new Date(ts)) + (i == 0 ? ")" : "");
            dayStarts[idx] = now - (i + 1) * dayMs;
            weekData.put(dayKeys[idx], 0.0);
        }

        for (Order o : orderRepository.findAll()) {
            if (!isRevenueEligibleOrder(o)) continue;
            long ageMs = Math.max(0L, now - o.getTimestamp());
            int dayBucket = (int) (ageMs / dayMs);
            if (dayBucket >= 0 && dayBucket < 7) {
                int idx = 6 - dayBucket;
                String key = dayKeys[idx];
                weekData.put(key, weekData.getOrDefault(key, 0.0) + o.getTotalAmount());
            }
        }

        return weekData;
    }
}
