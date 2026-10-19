package cafe.repositories;

import cafe.models.Payment;
import java.util.List;

public interface PaymentRepository {
    List<Payment> findAll();
    Payment findById(int id);
    Payment findByOrderId(int orderId);
    List<Payment> findAllByOrderId(int orderId);
    Payment save(Payment payment);
    boolean update(Payment payment);
    List<Payment> searchAndFilter(String keyword, String method, String status);
}
