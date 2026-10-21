package cafe.repositories;

import cafe.models.Order;
import java.util.List;

public interface OrderRepository {
    List<Order> findAll();
    Order findById(int orderId);
    Order save(Order order);
    boolean updateStatus(int orderId, String newStatus);
    List<Order> searchAndFilter(String keyword, String status, String dateFilter);
    List<Order> getRecentOrders(int limit);
}
