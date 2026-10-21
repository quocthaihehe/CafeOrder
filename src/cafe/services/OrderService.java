package cafe.services;

import cafe.models.AuditLog;
import cafe.models.CafeTable;
import cafe.models.Order;
import cafe.models.Payment;
import cafe.repositories.AuditLogRepository;
import cafe.repositories.OrderRepository;
import cafe.repositories.PaymentRepository;
import cafe.repositories.TableRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class OrderService {
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final TableRepository tableRepository;
    private final AuditLogRepository auditLogRepository;

    public OrderService(OrderRepository orderRepository, AuditLogRepository auditLogRepository) {
        this(orderRepository, null, null, auditLogRepository);
    }

    public OrderService(OrderRepository orderRepository, PaymentRepository paymentRepository, AuditLogRepository auditLogRepository) {
        this(orderRepository, paymentRepository, null, auditLogRepository);
    }

    public OrderService(OrderRepository orderRepository, PaymentRepository paymentRepository, TableRepository tableRepository, AuditLogRepository auditLogRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.tableRepository = tableRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(int orderId) {
        return orderRepository.findById(orderId);
    }

    public List<Order> searchOrders(String keyword, String status, String dateFilter) {
        return orderRepository.searchAndFilter(keyword, status, dateFilter);
    }

    public List<Order> getRecentOrders(int limit) {
        return orderRepository.getRecentOrders(limit);
    }

    public List<String> getAllowedNextStatuses(String currentStatus) {
        if (currentStatus == null) return Collections.emptyList();
        switch (currentStatus.trim().toUpperCase()) {
            case "CREATED":
                return Arrays.asList("CONFIRMED", "CANCELLED");
            case "CONFIRMED":
                return Arrays.asList("PREPARING", "CANCELLED");
            case "PREPARING":
                return Arrays.asList("READY", "CANCELLED");
            case "READY":
                return Arrays.asList("SERVED", "CANCELLED");
            case "SERVED":
                return Collections.singletonList("WAITING_PAYMENT");
            case "WAITING_PAYMENT":
                return Collections.singletonList("PAID");
            case "PAID":
                return Collections.singletonList("COMPLETED");
            case "COMPLETED":
            case "CANCELLED":
            default:
                return Collections.emptyList();
        }
    }

    public boolean isValidTransition(String currentStatus, String newStatus) {
        if (currentStatus == null || newStatus == null) return false;
        List<String> allowed = getAllowedNextStatuses(currentStatus);
        return allowed.contains(newStatus.trim().toUpperCase());
    }

    public String updateOrderStatus(int orderId, String newStatus, String actor) {
        Order o = orderRepository.findById(orderId);
        if (o == null) {
            auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_ORDER_STATUS", "Order #" + orderId, "FAILED (NOT_FOUND)"));
            return "Không tìm thấy đơn hàng #" + orderId;
        }

        String oldStatus = o.getStatus();
        if (newStatus == null || newStatus.trim().isEmpty()) {
            auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_ORDER_STATUS", "Order #" + orderId, "FAILED (EMPTY_STATUS)"));
            return "Trạng thái mới không được để trống!";
        }

        String targetStatus = newStatus.trim().toUpperCase();
        if (!isValidTransition(oldStatus, targetStatus)) {
            List<String> allowed = getAllowedNextStatuses(oldStatus);
            String allowedStr = allowed.isEmpty() ? "Không có (Đơn hàng đã kết thúc)" : String.join(", ", allowed);
            auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_ORDER_STATUS", "Order #" + orderId + " (" + oldStatus + " -> " + targetStatus + ")", "FAILED (INVALID_TRANSITION)"));
            return "Chuyển trạng thái không hợp lệ: Không thể chuyển từ '" + oldStatus + "' sang '" + targetStatus + "'. Các trạng thái tiếp theo được phép: [" + allowedStr + "].";
        }

        // Không cho phép xử lý tiếp đơn hàng không có món hợp lệ (tất cả món đều OUT_OF_STOCK hoặc CANCELLED) ngoại trừ hủy đơn
        if (!"CANCELLED".equalsIgnoreCase(targetStatus)) {
            if (o.getTotalQuantity() <= 0 || o.getTotalAmount() <= 0) {
                auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_ORDER_STATUS", "Order #" + orderId + " (" + oldStatus + " -> " + targetStatus + ")", "FAILED (NO_BILLABLE_ITEMS)"));
                return "Đơn hàng #" + orderId + " không có món ăn hợp lệ nào để tính tiền (tổng tiền = 0 đ). Chỉ có thể chuyển sang trạng thái CANCELLED!";
            }
        }

        // Quy tắc bắt buộc: Chỉ cho phép chuyển sang PAID khi đã có giao dịch Payment ở trạng thái SUCCESS khớp số tiền
        if ("PAID".equalsIgnoreCase(targetStatus)) {
            if (paymentRepository == null) {
                auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_ORDER_STATUS", "Order #" + orderId + " (" + oldStatus + " -> PAID)", "FAILED (PAYMENT_REPO_MISSING)"));
                return "Không thể chuyển đơn #" + orderId + " sang PAID: Chưa xác minh được giao dịch thanh toán thành công!";
            }
            List<Payment> orderPayments = paymentRepository.findAllByOrderId(orderId);
            boolean hasValidSuccessPayment = orderPayments.stream().anyMatch(p ->
                    "SUCCESS".equalsIgnoreCase(p.getStatus()) &&
                    Math.abs(p.getAmount() - o.getTotalAmount()) < 0.01
            );
            if (!hasValidSuccessPayment) {
                auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_ORDER_STATUS", "Order #" + orderId + " (" + oldStatus + " -> PAID)", "FAILED (PAYMENT_NOT_VERIFIED)"));
                return "Không thể chuyển đơn #" + orderId + " sang PAID: Chưa có giao dịch thanh toán ở trạng thái SUCCESS khớp với tổng tiền đơn (" + o.getFormattedTotal() + ")!";
            }
        }

        orderRepository.updateStatus(orderId, targetStatus);

        // Đồng bộ trạng thái bàn nếu có TableRepository
        if (tableRepository != null) {
            CafeTable tbl = tableRepository.findByTableNumber(o.getTable());
            if (tbl != null) {
                if ("COMPLETED".equalsIgnoreCase(targetStatus) || "CANCELLED".equalsIgnoreCase(targetStatus)) {
                    boolean hasOtherActiveOrders = orderRepository.findAll().stream().anyMatch(other ->
                            other.getOrderId() != orderId &&
                            other.getTable() == o.getTable() &&
                            !Arrays.asList("COMPLETED", "CANCELLED").contains(other.getStatus().toUpperCase())
                    );
                    if (!hasOtherActiveOrders && "OCCUPIED".equalsIgnoreCase(tbl.getStatus())) {
                        tbl.setStatus("AVAILABLE");
                        tableRepository.update(tbl);
                    }
                } else if ("AVAILABLE".equalsIgnoreCase(tbl.getStatus())) {
                    tbl.setStatus("OCCUPIED");
                    tableRepository.update(tbl);
                }
            }
        }

        auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_ORDER_STATUS", "Order #" + orderId + " (" + oldStatus + " -> " + targetStatus + ")", "SUCCESS"));
        return null;
    }
}
