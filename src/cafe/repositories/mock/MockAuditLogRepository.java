package cafe.repositories.mock;

import cafe.models.AuditLog;
import cafe.repositories.AuditLogRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MockAuditLogRepository implements AuditLogRepository {
    private final List<AuditLog> logs = new ArrayList<>();
    private final AtomicInteger idCounter = new AtomicInteger(32);

    public MockAuditLogRepository() {
        initMockData();
    }

    private void initMockData() {
        logs.add(new AuditLog(32, "2026-10-08 20:45:12", "admin01", "VIEW_ORDER", "Order #1025", "SUCCESS"));
        logs.add(new AuditLog(31, "2026-10-08 20:40:05", "admin01", "UPDATE_MENU", "Cold Brew Cam Vàng (42.000đ)", "SUCCESS"));
        logs.add(new AuditLog(30, "2026-10-08 20:30:10", "cashier01", "PAYMENT", "Order #1021 - 54.000đ (CASH)", "SUCCESS"));
        logs.add(new AuditLog(29, "2026-10-08 20:20:00", "cashier02", "PAYMENT", "Order #1020 - 135.000đ (CARD)", "SUCCESS"));
        logs.add(new AuditLog(28, "2026-10-08 20:05:40", "cashier01", "PAYMENT", "Order #1019 - 109.000đ (BANK_TRANSFER)", "SUCCESS"));
        logs.add(new AuditLog(27, "2026-10-08 19:50:15", "cashier01", "PAYMENT", "Order #1018 - 106.000đ (CASH)", "SUCCESS"));
        logs.add(new AuditLog(26, "2026-10-08 19:10:35", "cashier02", "PAYMENT", "Order #1016 - 174.000đ (CARD)", "SUCCESS"));
        logs.add(new AuditLog(25, "2026-10-08 18:25:20", "cashier01", "PAYMENT", "Order #1015 - 87.000đ (CASH)", "SUCCESS"));
        logs.add(new AuditLog(24, "2026-10-08 18:00:15", "admin01", "RESET_DEVICE_SECRET", "DEV-TB12-M84 (Bàn 12)", "SUCCESS"));
        logs.add(new AuditLog(23, "2026-10-08 17:40:50", "cashier02", "PAYMENT", "Order #1014 - 116.000đ (BANK_TRANSFER)", "SUCCESS"));
        logs.add(new AuditLog(22, "2026-10-08 17:20:00", "cashier02", "PAYMENT", "Order #1013 - 45.000đ (CARD)", "FAILED"));
        logs.add(new AuditLog(21, "2026-10-08 16:55:00", "admin01", "UPDATE_TABLE", "Bàn 07 -> OFFLINE", "SUCCESS"));
        logs.add(new AuditLog(20, "2026-10-08 16:20:45", "cashier01", "PAYMENT", "Order #1012 - 130.000đ (CARD)", "SUCCESS"));
        logs.add(new AuditLog(19, "2026-10-08 15:35:10", "cashier01", "PAYMENT", "Order #1011 - 114.000đ (CASH)", "SUCCESS"));
        logs.add(new AuditLog(18, "2026-10-08 15:00:00", "admin01", "DISABLE_USER", "staff05 (Phan Cẩm Tiên)", "SUCCESS"));
        logs.add(new AuditLog(17, "2026-10-08 14:10:30", "cashier02", "PAYMENT", "Order #1010 - 115.000đ (BANK_TRANSFER)", "SUCCESS"));
        logs.add(new AuditLog(16, "2026-10-08 13:45:00", "admin01", "RESET_PASSWORD", "staff02 (Hoàng Gia Huy)", "SUCCESS"));
        logs.add(new AuditLog(15, "2026-10-08 13:20:25", "cashier01", "PAYMENT", "Order #1009 - 96.000đ (CASH)", "SUCCESS"));
        logs.add(new AuditLog(14, "2026-10-08 12:45:50", "cashier01", "PAYMENT", "Order #1008 - 211.000đ (CARD)", "SUCCESS"));
        logs.add(new AuditLog(13, "2026-10-08 12:00:00", "admin01", "DISABLE_MENU", "Trà Bí Đao Hạt É", "SUCCESS"));
        logs.add(new AuditLog(12, "2026-10-08 11:30:10", "cashier02", "PAYMENT", "Order #1007 - 61.000đ (CASH)", "SUCCESS"));
        logs.add(new AuditLog(11, "2026-10-08 11:00:00", "admin01", "CREATE_TABLE", "Bàn 12 (DEV-TB12-M84)", "SUCCESS"));
        logs.add(new AuditLog(10, "2026-10-08 10:15:55", "cashier01", "PAYMENT", "Order #1006 - 126.000đ (BANK_TRANSFER)", "SUCCESS"));
        logs.add(new AuditLog(9, "2026-10-08 09:40:00", "admin01", "CREATE_USER", "barista02 (Võ Minh Trí)", "SUCCESS"));
        logs.add(new AuditLog(8, "2026-10-08 09:20:30", "cashier02", "PAYMENT", "Order #1005 - 129.000đ (CARD)", "SUCCESS"));
        logs.add(new AuditLog(7, "2026-10-08 08:30:15", "cashier01", "PAYMENT", "Order #1004 - 100.000đ (CASH)", "SUCCESS"));
        logs.add(new AuditLog(6, "2026-10-08 08:00:00", "admin01", "CREATE_MENU", "Cà phê Muối Cố Đô (35.000đ)", "SUCCESS"));
        logs.add(new AuditLog(5, "2026-10-08 07:51:10", "cashier01", "PAYMENT", "Order #1003 - 90.000đ (BANK_TRANSFER)", "SUCCESS"));
        logs.add(new AuditLog(4, "2026-10-08 07:05:40", "cashier02", "PAYMENT", "Order #1002 - 90.000đ (CARD)", "SUCCESS"));
        logs.add(new AuditLog(3, "2026-10-08 06:15:20", "cashier01", "PAYMENT", "Order #1001 - 90.000đ (CASH)", "SUCCESS"));
        logs.add(new AuditLog(2, "2026-10-08 06:00:00", "admin01", "LOGIN", "admin01", "SUCCESS"));
        logs.add(new AuditLog(1, "2026-10-08 05:58:00", "anonymous", "LOGIN", "staff99", "FAILED"));
    }

    @Override
    public List<AuditLog> findAll() {
        List<AuditLog> list = new ArrayList<>(logs);
        list.sort(Comparator.comparingInt(AuditLog::getId).reversed());
        return list;
    }

    @Override
    public synchronized AuditLog save(AuditLog log) {
        log.setId(idCounter.incrementAndGet());
        logs.add(0, log);
        return log;
    }

    @Override
    public List<AuditLog> searchAndFilter(String keyword, String user, String action, String result) {
        return logs.stream().filter(l -> {
            String kw = keyword != null ? keyword.toLowerCase().trim() : "";
            boolean matchKw = kw.isEmpty() ||
                    (l.getAction() != null && l.getAction().toLowerCase().contains(kw)) ||
                    (l.getTarget() != null && l.getTarget().toLowerCase().contains(kw)) ||
                    (l.getUser() != null && l.getUser().toLowerCase().contains(kw)) ||
                    (l.getResult() != null && l.getResult().toLowerCase().contains(kw));
            boolean matchUser = user == null || user.trim().isEmpty() || "ALL".equalsIgnoreCase(user.trim()) || user.trim().toLowerCase().startsWith("tất cả") ||
                    (l.getUser() != null && l.getUser().equalsIgnoreCase(user.trim()));
            boolean matchAct = action == null || action.trim().isEmpty() || "ALL".equalsIgnoreCase(action.trim()) || action.trim().toLowerCase().startsWith("tất cả") ||
                    (l.getAction() != null && l.getAction().equalsIgnoreCase(action.trim()));
            boolean matchRes = result == null || result.trim().isEmpty() || "ALL".equalsIgnoreCase(result.trim()) || result.trim().toLowerCase().startsWith("tất cả") ||
                    (l.getResult() != null && (l.getResult().equalsIgnoreCase(result.trim()) || l.getResult().toUpperCase().startsWith(result.trim().toUpperCase())));
            return matchKw && matchUser && matchAct && matchRes;
        }).sorted(Comparator.comparingInt(AuditLog::getId).reversed()).collect(Collectors.toList());
    }
}
