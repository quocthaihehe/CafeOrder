package cafe.repositories.mock;

import cafe.models.Role;
import cafe.models.User;
import cafe.repositories.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MockUserRepository implements UserRepository {
    private final List<User> users = new ArrayList<>();
    private final AtomicInteger idCounter = new AtomicInteger(10);

    public MockUserRepository() {
        initMockData();
    }

    private void initMockData() {
        users.add(new User(1, "admin01", "Nguyễn Văn Admin", "admin123", Role.ADMIN, "ACTIVE", "2026-09-01 08:00"));
        users.add(new User(2, "cashier01", "Trần Thị Thu Ngân", "cashier123", Role.CASHIER, "ACTIVE", "2026-09-05 09:30"));
        users.add(new User(3, "cashier02", "Lê Hoàng Phúc", "cashier123", Role.CASHIER, "ACTIVE", "2026-09-10 14:15"));
        users.add(new User(4, "barista01", "Phạm Quốc Thái", "barista123", Role.BARISTA, "ACTIVE", "2026-09-12 07:45"));
        users.add(new User(5, "barista02", "Võ Minh Trí", "barista123", Role.BARISTA, "ACTIVE", "2026-09-15 11:20"));
        users.add(new User(6, "staff01", "Đặng Thị Thảo", "staff123", Role.STAFF, "ACTIVE", "2026-09-20 08:30"));
        users.add(new User(7, "staff02", "Hoàng Gia Huy", "staff123", Role.STAFF, "ACTIVE", "2026-09-22 13:00"));
        users.add(new User(8, "staff03", "Bùi Kim Ngân", "staff123", Role.STAFF, "ACTIVE", "2026-09-25 15:40"));
        users.add(new User(9, "staff04", "Trương Tuấn Kiệt", "staff123", Role.STAFF, "DISABLED", "2026-09-28 10:10"));
        users.add(new User(10, "staff05", "Phan Cẩm Tiên", "staff123", Role.STAFF, "DISABLED", "2026-10-01 16:25"));
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users);
    }

    @Override
    public User findById(int id) {
        return users.stream().filter(u -> u.getId() == id).findFirst().orElse(null);
    }

    @Override
    public User findByUsername(String username) {
        if (username == null) return null;
        return users.stream().filter(u -> u.getUsername().equalsIgnoreCase(username.trim())).findFirst().orElse(null);
    }

    @Override
    public boolean existsByUsername(String username) {
        return findByUsername(username) != null;
    }

    @Override
    public synchronized User save(User user) {
        user.setId(idCounter.incrementAndGet());
        users.add(user);
        return user;
    }

    @Override
    public synchronized boolean update(User user) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId() == user.getId()) {
                users.set(i, user);
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean setStatus(int id, String status) {
        User u = findById(id);
        if (u != null) {
            u.setStatus(status);
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean resetPassword(int id, String newPassword) {
        User u = findById(id);
        if (u != null) {
            u.setPassword(newPassword);
            return true;
        }
        return false;
    }

    @Override
    public List<User> searchAndFilter(String keyword, Role role, String status) {
        return users.stream().filter(u -> {
            boolean matchKw = keyword == null || keyword.trim().isEmpty() ||
                    u.getUsername().toLowerCase().contains(keyword.toLowerCase().trim()) ||
                    u.getFullName().toLowerCase().contains(keyword.toLowerCase().trim());
            boolean matchRole = role == null || u.getRole() == role;
            boolean matchStatus = status == null || status.trim().isEmpty() || "ALL".equalsIgnoreCase(status.trim()) || "Tất cả".equalsIgnoreCase(status.trim()) || u.getStatus().equalsIgnoreCase(status.trim());
            return matchKw && matchRole && matchStatus;
        }).collect(Collectors.toList());
    }
}
