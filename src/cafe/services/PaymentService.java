package cafe.services;

import cafe.models.AuditLog;
import cafe.models.Order;
import cafe.models.Payment;
import cafe.repositories.AuditLogRepository;
import cafe.repositories.OrderRepository;
import cafe.repositories.PaymentRepository;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final AuditLogRepository auditLogRepository;

    public PaymentService(PaymentRepository paymentRepository, AuditLogRepository auditLogRepository) {
        this(paymentRepository, null, auditLogRepository);
    }

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository, AuditLogRepository auditLogRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Payment getPaymentById(int id) {
        return paymentRepository.findById(id);
    }

    public Payment getPaymentByOrderId(int orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    public List<Payment> getPaymentsByOrderId(int orderId) {
        return paymentRepository.findAllByOrderId(orderId);
    }

    public List<Payment> searchPayments(String keyword, String method, String status) {
        return paymentRepository.searchAndFilter(keyword, method, status);
    }

    public String confirmPendingPayment(int paymentId, String actor) {
        Payment p = paymentRepository.findById(paymentId);
        if (p == null) {
            return "Không tìm thấy giao dịch thanh toán #" + paymentId + "!";
        }
        if (!"PENDING".equalsIgnoreCase(p.getStatus())) {
            return "Chỉ có thể xác nhận giao dịch đang ở trạng thái PENDING (Hiện tại: " + p.getStatus() + ")!";
        }

        if (orderRepository != null) {
            Order o = orderRepository.findById(p.getOrderId());
            if (o == null) {
                auditLogRepository.save(new AuditLog(0, null, actor, "CONFIRM_PAYMENT", "Payment #" + paymentId, "FAILED (ORDER_NOT_FOUND)"));
                return "Không tìm thấy đơn hàng #" + p.getOrderId() + " liên kết với giao dịch này!";
            }
            if ("CANCELLED".equalsIgnoreCase(o.getStatus())) {
                auditLogRepository.save(new AuditLog(0, null, actor, "CONFIRM_PAYMENT", "Payment #" + paymentId + " (Order #" + o.getOrderId() + ")", "FAILED (ORDER_CANCELLED)"));
                return "Không thể xác nhận thanh toán cho đơn hàng #" + o.getOrderId() + " đã bị hủy (CANCELLED)!";
            }
            if (o.getTotalAmount() <= 0 || Math.abs(p.getAmount() - o.getTotalAmount()) > 0.01) {
                auditLogRepository.save(new AuditLog(0, null, actor, "CONFIRM_PAYMENT", "Payment #" + paymentId + " (Order #" + o.getOrderId() + ")", "FAILED (AMOUNT_MISMATCH)"));
                return "Số tiền giao dịch (" + p.getFormattedAmount() + ") không khớp với tổng tiền hợp lệ của đơn #" + o.getOrderId() + " (" + o.getFormattedTotal() + ")!";
            }
        }

        String nowStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        p.setStatus("SUCCESS");
        p.setPaidAt(nowStr);
        if (actor != null && !actor.trim().isEmpty()) {
            p.setProcessedBy(actor.trim());
        }
        paymentRepository.update(p);
        auditLogRepository.save(new AuditLog(0, null, actor, "CONFIRM_PAYMENT", "Payment #" + paymentId + " (Order #" + p.getOrderId() + " - " + p.getFormattedAmount() + ")", "SUCCESS"));
        return null;
    }

    public String markPaymentFailed(int paymentId, String actor) {
        Payment p = paymentRepository.findById(paymentId);
        if (p == null) {
            return "Không tìm thấy giao dịch thanh toán #" + paymentId + "!";
        }
        if (!"PENDING".equalsIgnoreCase(p.getStatus())) {
            return "Chỉ có thể đánh dấu lỗi cho giao dịch đang ở trạng thái PENDING!";
        }

        p.setStatus("FAILED");
        if (actor != null && !actor.trim().isEmpty()) {
            p.setProcessedBy(actor.trim());
        }
        paymentRepository.update(p);
        auditLogRepository.save(new AuditLog(0, null, actor, "FAIL_PAYMENT", "Payment #" + paymentId + " (Order #" + p.getOrderId() + ")", "SUCCESS"));
        return null;
    }
}
