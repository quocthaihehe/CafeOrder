package cafe.repositories.mock;

import cafe.models.Payment;
import cafe.repositories.PaymentRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MockPaymentRepository implements PaymentRepository {
    private final List<Payment> payments = new ArrayList<>();
    private final AtomicInteger idCounter = new AtomicInteger(20);

    public MockPaymentRepository() {
        initMockData();
    }

    private void initMockData() {
        payments.add(new Payment(1, 1001, 90000, "CASH", "SUCCESS", "TXN1001-CASH", "cashier01", "2026-10-08 06:15:00", "2026-10-08 06:15:20"));
        payments.add(new Payment(2, 1002, 90000, "CARD", "SUCCESS", "TXN1002-VNPAY", "cashier02", "2026-10-08 07:05:00", "2026-10-08 07:05:40"));
        payments.add(new Payment(3, 1003, 90000, "BANK_TRANSFER", "SUCCESS", "TXN1003-MBBANK", "cashier01", "2026-10-08 07:50:00", "2026-10-08 07:51:10"));
        payments.add(new Payment(4, 1004, 100000, "CASH", "SUCCESS", "TXN1004-CASH", "cashier01", "2026-10-08 08:30:00", "2026-10-08 08:30:15"));
        payments.add(new Payment(5, 1005, 129000, "CARD", "SUCCESS", "TXN1005-VISA", "cashier02", "2026-10-08 09:20:00", "2026-10-08 09:20:30"));
        payments.add(new Payment(6, 1006, 126000, "BANK_TRANSFER", "SUCCESS", "TXN1006-VCB", "cashier01", "2026-10-08 10:15:00", "2026-10-08 10:15:55"));
        payments.add(new Payment(7, 1007, 61000, "CASH", "SUCCESS", "TXN1007-CASH", "cashier02", "2026-10-08 11:30:00", "2026-10-08 11:30:10"));
        payments.add(new Payment(8, 1008, 211000, "CARD", "SUCCESS", "TXN1008-MASTER", "cashier01", "2026-10-08 12:45:00", "2026-10-08 12:45:50"));
        payments.add(new Payment(9, 1009, 96000, "CASH", "SUCCESS", "TXN1009-CASH", "cashier01", "2026-10-08 13:20:00", "2026-10-08 13:20:25"));
        payments.add(new Payment(10, 1010, 115000, "BANK_TRANSFER", "SUCCESS", "TXN1010-MOMO", "cashier02", "2026-10-08 14:10:00", "2026-10-08 14:10:30"));
        payments.add(new Payment(11, 1011, 114000, "CASH", "SUCCESS", "TXN1011-CASH", "cashier01", "2026-10-08 15:35:00", "2026-10-08 15:35:10"));
        payments.add(new Payment(12, 1012, 130000, "CARD", "SUCCESS", "TXN1012-VISA", "cashier01", "2026-10-08 16:20:00", "2026-10-08 16:20:45"));
        payments.add(new Payment(13, 1014, 116000, "BANK_TRANSFER", "SUCCESS", "TXN1014-VCB", "cashier02", "2026-10-08 17:40:00", "2026-10-08 17:40:50"));
        payments.add(new Payment(14, 1015, 87000, "CASH", "SUCCESS", "TXN1015-CASH", "cashier01", "2026-10-08 18:25:00", "2026-10-08 18:25:20"));
        payments.add(new Payment(15, 1016, 174000, "CARD", "SUCCESS", "TXN1016-NAPAS", "cashier02", "2026-10-08 19:10:00", "2026-10-08 19:10:35"));
        payments.add(new Payment(16, 1018, 106000, "CASH", "SUCCESS", "TXN1018-CASH", "cashier01", "2026-10-08 19:50:00", "2026-10-08 19:50:15"));
        payments.add(new Payment(17, 1019, 109000, "BANK_TRANSFER", "SUCCESS", "TXN1019-ZALOPAY", "cashier01", "2026-10-08 20:05:00", "2026-10-08 20:05:40"));
        payments.add(new Payment(18, 1020, 135000, "CARD", "PENDING", "TXN1020-WAIT", "cashier02", "2026-10-08 20:20:00", null));
        payments.add(new Payment(19, 1013, 45000, "CARD", "FAILED", "TXN1013-DECLINED", "cashier02", "2026-10-08 17:20:00", null));
        payments.add(new Payment(20, 1021, 54000, "CASH", "PENDING", "TXN1021-PENDING", "cashier01", "2026-10-08 20:30:00", null));
    }

    @Override
    public List<Payment> findAll() {
        List<Payment> list = new ArrayList<>(payments);
        list.sort(Comparator.comparingInt(Payment::getId).reversed());
        return list;
    }

    @Override
    public Payment findById(int id) {
        return payments.stream().filter(p -> p.getId() == id).findFirst().orElse(null);
    }

    @Override
    public Payment findByOrderId(int orderId) {
        return payments.stream()
                .filter(p -> p.getOrderId() == orderId && "SUCCESS".equalsIgnoreCase(p.getStatus()))
                .findFirst()
                .orElseGet(() -> payments.stream()
                        .filter(p -> p.getOrderId() == orderId)
                        .max(Comparator.comparingInt(Payment::getId))
                        .orElse(null));
    }

    @Override
    public List<Payment> findAllByOrderId(int orderId) {
        return payments.stream()
                .filter(p -> p.getOrderId() == orderId)
                .sorted(Comparator.comparingInt(Payment::getId).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public synchronized Payment save(Payment payment) {
        payment.setId(idCounter.incrementAndGet());
        payments.add(0, payment);
        return payment;
    }

    @Override
    public synchronized boolean update(Payment payment) {
        if (payment == null) return false;
        for (int i = 0; i < payments.size(); i++) {
            if (payments.get(i).getId() == payment.getId()) {
                payments.set(i, payment);
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Payment> searchAndFilter(String keyword, String method, String status) {
        return payments.stream().filter(p -> {
            boolean matchKw = keyword == null || keyword.trim().isEmpty() ||
                    String.valueOf(p.getId()).contains(keyword.trim()) ||
                    String.valueOf(p.getOrderId()).contains(keyword.trim()) ||
                    p.getTransactionCode().toLowerCase().contains(keyword.toLowerCase().trim()) ||
                    p.getProcessedBy().toLowerCase().contains(keyword.toLowerCase().trim());
            boolean matchMethod = method == null || method.trim().isEmpty() || "ALL".equalsIgnoreCase(method.trim()) || "Tất cả".equalsIgnoreCase(method.trim()) || p.getMethod().equalsIgnoreCase(method.trim());
            boolean matchStatus = status == null || status.trim().isEmpty() || "ALL".equalsIgnoreCase(status.trim()) || "Tất cả".equalsIgnoreCase(status.trim()) || p.getStatus().equalsIgnoreCase(status.trim());
            return matchKw && matchMethod && matchStatus;
        }).sorted(Comparator.comparingInt(Payment::getId).reversed()).collect(Collectors.toList());
    }
}
