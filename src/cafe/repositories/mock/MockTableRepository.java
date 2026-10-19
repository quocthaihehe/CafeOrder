package cafe.repositories.mock;

import cafe.models.CafeTable;
import cafe.repositories.TableRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MockTableRepository implements TableRepository {
    private final List<CafeTable> tables = new ArrayList<>();
    private final AtomicInteger idCounter = new AtomicInteger(12);

    public MockTableRepository() {
        initMockData();
    }

    private void initMockData() {
        tables.add(new CafeTable(1, 1, "DEV-TB01-A9F", "AVAILABLE", "2026-09-01 08:00", "$2a$10$e8gH9A.mockSecretHashForTable01"));
        tables.add(new CafeTable(2, 2, "DEV-TB02-B8E", "OCCUPIED", "2026-09-01 08:00", "$2a$10$e8gH9B.mockSecretHashForTable02"));
        tables.add(new CafeTable(3, 3, "DEV-TB03-C7D", "OCCUPIED", "2026-09-01 08:00", "$2a$10$e8gH9C.mockSecretHashForTable03"));
        tables.add(new CafeTable(4, 4, "DEV-TB04-D6C", "AVAILABLE", "2026-09-01 08:00", "$2a$10$e8gH9D.mockSecretHashForTable04"));
        tables.add(new CafeTable(5, 5, "DEV-TB05-E5B", "OCCUPIED", "2026-09-01 08:00", "$2a$10$e8gH9E.mockSecretHashForTable05"));
        tables.add(new CafeTable(6, 6, "DEV-TB06-F4A", "AVAILABLE", "2026-09-01 08:00", "$2a$10$e8gH9F.mockSecretHashForTable06"));
        tables.add(new CafeTable(7, 7, "DEV-TB07-G39", "OFFLINE", "2026-09-01 08:00", "$2a$10$e8gH9G.mockSecretHashForTable07"));
        tables.add(new CafeTable(8, 8, "DEV-TB08-H28", "AVAILABLE", "2026-09-01 08:00", "$2a$10$e8gH9H.mockSecretHashForTable08"));
        tables.add(new CafeTable(9, 9, "DEV-TB09-J17", "OCCUPIED", "2026-09-01 08:00", "$2a$10$e8gH9J.mockSecretHashForTable09"));
        tables.add(new CafeTable(10, 10, "DEV-TB10-K06", "AVAILABLE", "2026-09-01 08:00", "$2a$10$e8gH9K.mockSecretHashForTable10"));
        tables.add(new CafeTable(11, 11, "DEV-TB11-L95", "AVAILABLE", "2026-09-01 08:00", "$2a$10$e8gH9L.mockSecretHashForTable11"));
        tables.add(new CafeTable(12, 12, "DEV-TB12-M84", "OFFLINE", "2026-09-01 08:00", "$2a$10$e8gH9M.mockSecretHashForTable12"));
    }

    @Override
    public List<CafeTable> findAll() {
        return new ArrayList<>(tables);
    }

    @Override
    public CafeTable findById(int id) {
        return tables.stream().filter(t -> t.getId() == id).findFirst().orElse(null);
    }

    @Override
    public CafeTable findByTableNumber(int tableNumber) {
        return tables.stream().filter(t -> t.getTableNumber() == tableNumber).findFirst().orElse(null);
    }

    @Override
    public boolean existsByTableNumber(int tableNumber) {
        return findByTableNumber(tableNumber) != null;
    }

    @Override
    public synchronized CafeTable save(CafeTable table) {
        table.setId(idCounter.incrementAndGet());
        if (table.getDeviceSecretHash() == null && table.getDeviceId() != null && !table.getDeviceId().isEmpty()) {
            table.setDeviceSecretHash("$2a$10$mockGenSecretHash." + table.getTableNumber());
        }
        tables.add(table);
        return table;
    }

    @Override
    public synchronized boolean update(CafeTable table) {
        for (int i = 0; i < tables.size(); i++) {
            if (tables.get(i).getId() == table.getId()) {
                tables.set(i, table);
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean setStatus(int id, String status) {
        CafeTable t = findById(id);
        if (t != null) {
            t.setStatus(status);
            return true;
        }
        return false;
    }

    @Override
    public List<CafeTable> searchAndFilter(String keyword, String status) {
        return tables.stream().filter(t -> {
            boolean matchKw = keyword == null || keyword.trim().isEmpty() ||
                    String.valueOf(t.getTableNumber()).contains(keyword.trim()) ||
                    (t.getDeviceId() != null && t.getDeviceId().toLowerCase().contains(keyword.toLowerCase().trim()));
            boolean matchStatus = status == null || status.trim().isEmpty() || "ALL".equalsIgnoreCase(status.trim()) || "Tất cả".equalsIgnoreCase(status.trim()) || t.getStatus().equalsIgnoreCase(status.trim());
            return matchKw && matchStatus;
        }).collect(Collectors.toList());
    }
}
