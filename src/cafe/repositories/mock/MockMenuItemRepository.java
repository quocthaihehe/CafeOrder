package cafe.repositories.mock;

import cafe.models.MenuItem;
import cafe.repositories.MenuItemRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MockMenuItemRepository implements MenuItemRepository {
    private final List<MenuItem> items = new ArrayList<>();
    private final AtomicInteger idCounter = new AtomicInteger(18);

    public MockMenuItemRepository() {
        initMockData();
    }

    private void initMockData() {
        // Cà phê
        items.add(new MenuItem(1, "Cà phê Đen Phin", 25000, "Cà phê", "Robusta Đắk Lắk rang mộc đậm đà, hậu vị đắng thanh dịu nhẹ.", "☕", "ACTIVE", "2026-09-01 08:00"));
        items.add(new MenuItem(2, "Cà phê Sữa Đá", 29000, "Cà phê", "Cà phê phin truyền thống phối hợp sữa đặc béo ngậy hảo hạng.", "☕", "ACTIVE", "2026-09-01 08:00"));
        items.add(new MenuItem(3, "Bạc Xỉu Kem Béo", 32000, "Cà phê", "Ba tầng nghệ thuật với sữa tươi béo thơm và chút nhấn cà phê.", "🥛", "ACTIVE", "2026-09-01 08:00"));
        items.add(new MenuItem(4, "Cà phê Muối Cố Đô", 35000, "Cà phê", "Lớp kem muối mặn mòi sánh mịn phủ trên nền cà phê đậm đà.", "🧂", "ACTIVE", "2026-09-01 08:00"));
        items.add(new MenuItem(5, "Cold Brew Cam Vàng", 42000, "Cà phê", "Cà phê ủ lạnh 18h thơm nồng hoa quả kết hợp tép cam mọng nước.", "🍊", "ACTIVE", "2026-09-01 08:00"));
        items.add(new MenuItem(6, "Caramel Macchiato", 45000, "Cà phê", "Espresso thơm lừng hòa quyện sữa nóng và sốt caramel óng ánh.", "🍮", "ACTIVE", "2026-09-01 08:00"));

        // Trà & Trái cây
        items.add(new MenuItem(7, "Trà Đào Cam Sả", 39000, "Trà & Trái cây", "Hương sả thanh khiết, cam tươi mọng nước cùng miếng đào giòn ngọt.", "🍑", "ACTIVE", "2026-09-01 08:00"));
        items.add(new MenuItem(8, "Trà Vải Hoa Hồng", 42000, "Trà & Trái cây", "Hương hoa hồng dịu mát phối hợp trái vải giòn tan thơm mát mùa hè.", "🌹", "ACTIVE", "2026-09-01 08:00"));
        items.add(new MenuItem(9, "Trà Sen Vàng Kem Cheese", 45000, "Trà & Trái cây", "Hạt sen bùi thơm, trân châu ngọc trai và lớp kem cheese béo mặn.", "🪷", "ACTIVE", "2026-09-01 08:00"));
        items.add(new MenuItem(10, "Trà Ô Long Mãng Cầu", 42000, "Trà & Trái cây", "Vị chua ngọt bùng nổ từ mãng cầu xiêm tươi cùng nền trà ô long thanh vị.", "🍹", "ACTIVE", "2026-09-01 08:00"));

        // Đá xay & Sinh tố
        items.add(new MenuItem(11, "Matcha Đá Xay Uji", 49000, "Đá xay & Sinh tố", "Bột trà xanh Uji Kyoto chuẩn Nhật Bản xay cùng sữa và kem tươi.", "🍵", "ACTIVE", "2026-09-01 08:00"));
        items.add(new MenuItem(12, "Cookie & Cream Đá Xay", 48000, "Đá xay & Sinh tố", "Oreo giòn rụm kết hợp sốt sôcôla Bỉ ngọt ngào và kem whipping bông tuyết.", "🍪", "ACTIVE", "2026-09-01 08:00"));
        items.add(new MenuItem(13, "Sinh Tố Bơ Dừa Non", 45000, "Đá xay & Sinh tố", "Bơ sáp Đắk Lắk dẻo thơm hòa quyện nước cốt dừa béo bùi thanh mát.", "🥑", "ACTIVE", "2026-09-01 08:00"));

        // Bánh ngọt
        items.add(new MenuItem(14, "Croissant Bơ Pháp", 32000, "Bánh ngọt", "Bánh sừng bò ngàn lớp nướng vàng giòn rụm, nồng nàn hương bơ Pháp.", "🥐", "ACTIVE", "2026-09-01 08:00"));
        items.add(new MenuItem(15, "Tiramisu Cacao Ý", 38000, "Bánh ngọt", "Bánh bông lan cà phê phủ phô mai mascarpone mềm tan cùng bột cacao.", "🍰", "ACTIVE", "2026-09-01 08:00"));
        items.add(new MenuItem(16, "Cheesecake Nướng Basque", 45000, "Bánh ngọt", "Lớp mặt cháy caramel đặc trưng, lõi phô mai tan chảy béo ngậy.", "🧀", "ACTIVE", "2026-09-01 08:00"));

        // Inactive items
        items.add(new MenuItem(17, "Bánh Mì Chảo Đặc Biệt", 49000, "Đồ ăn nhẹ", "Bánh mì giòn kèm pate, trứng ốp la xúc xích nướng.", "🍳", "INACTIVE", "2026-09-20 09:00"));
        items.add(new MenuItem(18, "Trà Bí Đao Hạt É", 28000, "Trà & Trái cây", "Thanh mát giải nhiệt mùa hè.", "🧋", "INACTIVE", "2026-09-22 14:00"));
    }

    @Override
    public List<MenuItem> findAll() {
        return new ArrayList<>(items);
    }

    @Override
    public MenuItem findById(int id) {
        return items.stream().filter(i -> i.getId() == id).findFirst().orElse(null);
    }

    @Override
    public MenuItem findByName(String name) {
        if (name == null) return null;
        return items.stream().filter(i -> i.getName().equalsIgnoreCase(name.trim())).findFirst().orElse(null);
    }

    @Override
    public boolean existsByName(String name) {
        return findByName(name) != null;
    }

    @Override
    public synchronized MenuItem save(MenuItem item) {
        item.setId(idCounter.incrementAndGet());
        items.add(item);
        return item;
    }

    @Override
    public synchronized boolean update(MenuItem item) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId() == item.getId()) {
                items.set(i, item);
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean setStatus(int id, String status) {
        MenuItem item = findById(id);
        if (item != null) {
            item.setStatus(status);
            return true;
        }
        return false;
    }

    @Override
    public List<MenuItem> searchAndFilter(String keyword, String category, String status) {
        return items.stream().filter(i -> {
            boolean matchKw = keyword == null || keyword.trim().isEmpty() ||
                    i.getName().toLowerCase().contains(keyword.toLowerCase().trim()) ||
                    i.getDescription().toLowerCase().contains(keyword.toLowerCase().trim());
            boolean matchCat = category == null || "ALL".equalsIgnoreCase(category) || "Tất cả".equalsIgnoreCase(category) ||
                    i.getCategory().equalsIgnoreCase(category);
            boolean matchStatus = status == null || status.trim().isEmpty() || "ALL".equalsIgnoreCase(status.trim()) || "Tất cả".equalsIgnoreCase(status.trim()) || i.getStatus().equalsIgnoreCase(status.trim());
            return matchKw && matchCat && matchStatus;
        }).collect(Collectors.toList());
    }

    @Override
    public List<String> getCategoryNames() {
        List<String> list = new ArrayList<>();
        list.add("Tất cả");
        for (MenuItem item : items) {
            if (!list.contains(item.getCategory())) {
                list.add(item.getCategory());
            }
        }
        return list;
    }
}
