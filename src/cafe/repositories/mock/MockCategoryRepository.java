package cafe.repositories.mock;

import cafe.models.Category;
import cafe.repositories.CategoryRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MockCategoryRepository implements CategoryRepository {
    private final List<Category> categories = new ArrayList<>();
    private final AtomicInteger idCounter = new AtomicInteger(5);

    public MockCategoryRepository() {
        initMockData();
    }

    private void initMockData() {
        categories.add(new Category(1, "Cà phê", "ACTIVE", "2026-09-01 08:00"));
        categories.add(new Category(2, "Trà & Trái cây", "ACTIVE", "2026-09-01 08:00"));
        categories.add(new Category(3, "Đá xay & Sinh tố", "ACTIVE", "2026-09-01 08:00"));
        categories.add(new Category(4, "Bánh ngọt", "ACTIVE", "2026-09-01 08:00"));
        categories.add(new Category(5, "Đồ ăn nhẹ", "INACTIVE", "2026-09-15 10:00"));
    }

    @Override
    public List<Category> findAll() {
        return new ArrayList<>(categories);
    }

    @Override
    public Category findById(int id) {
        return categories.stream().filter(c -> c.getId() == id).findFirst().orElse(null);
    }

    @Override
    public Category findByName(String name) {
        if (name == null) return null;
        return categories.stream().filter(c -> c.getName().equalsIgnoreCase(name.trim())).findFirst().orElse(null);
    }

    @Override
    public boolean existsByName(String name) {
        return findByName(name) != null;
    }

    @Override
    public synchronized Category save(Category category) {
        category.setId(idCounter.incrementAndGet());
        categories.add(category);
        return category;
    }

    @Override
    public synchronized boolean update(Category category) {
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getId() == category.getId()) {
                categories.set(i, category);
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean setStatus(int id, String status) {
        Category c = findById(id);
        if (c != null) {
            c.setStatus(status);
            return true;
        }
        return false;
    }

    @Override
    public List<Category> search(String keyword) {
        return categories.stream().filter(c -> {
            return keyword == null || keyword.trim().isEmpty() ||
                    c.getName().toLowerCase().contains(keyword.toLowerCase().trim());
        }).collect(Collectors.toList());
    }
}
