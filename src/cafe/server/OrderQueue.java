package cafe.server;

import cafe.models.Order;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class OrderQueue {
    // Hàng đợi thread-safe
    private final BlockingQueue<Order> queue;

    public OrderQueue() {
        this.queue = new LinkedBlockingQueue<>();
    }

    // Đẩy order vào hàng chờ
    public void addOrder(Order order) {
        try {
            queue.put(order);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Lấy order ra (nếu hàng đợi rỗng, luồng sẽ bị block (ngủ) chờ ở đây)
    public Order takeOrder() throws InterruptedException {
        return queue.take();
    }
    
    // Tiện ích lấy kích thước hàng đợi
    public int getQueueSize() {
        return queue.size();
    }
}
