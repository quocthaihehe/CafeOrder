package cafe.services;

import cafe.models.AuditLog;
import cafe.models.Category;
import cafe.models.MenuItem;
import cafe.repositories.AuditLogRepository;
import cafe.repositories.CategoryRepository;
import cafe.repositories.MenuItemRepository;
import java.util.ArrayList;
import java.util.List;

public class MenuService {
    private static final double MAX_MENU_PRICE = 10_000_000.0;

    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;
    private final AuditLogRepository auditLogRepository;

    public MenuService(MenuItemRepository menuItemRepository, AuditLogRepository auditLogRepository) {
        this(menuItemRepository, null, auditLogRepository);
    }

    public MenuService(MenuItemRepository menuItemRepository, CategoryRepository categoryRepository, AuditLogRepository auditLogRepository) {
        this.menuItemRepository = menuItemRepository;
        this.categoryRepository = categoryRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public List<MenuItem> getAllMenuItems() {
        return menuItemRepository.findAll();
    }

    public List<MenuItem> searchMenuItems(String keyword, String category, String status) {
        return menuItemRepository.searchAndFilter(keyword, category, status);
    }

    public List<String> getCategoryNames() {
        if (categoryRepository != null) {
            List<String> names = new ArrayList<>();
            names.add("Tất cả");
            for (Category c : categoryRepository.findAll()) {
                if (!names.contains(c.getName())) {
                    names.add(c.getName());
                }
            }
            return names;
        }
        return menuItemRepository.getCategoryNames();
    }

    private boolean isValidStatus(String status) {
        return status != null && ("ACTIVE".equalsIgnoreCase(status.trim()) || "INACTIVE".equalsIgnoreCase(status.trim()));
    }

    private String validateCategoryForMenu(String categoryName, String targetItemStatus) {
        String trimmedCat = categoryName.trim();
        if (categoryRepository != null) {
            Category cat = categoryRepository.findByName(trimmedCat);
            if (cat == null) {
                return "Danh mục '" + trimmedCat + "' không tồn tại trong hệ thống!";
            }
            if ("ACTIVE".equalsIgnoreCase(targetItemStatus) && !cat.isActive()) {
                return "Không thể mở bán (ACTIVE) món ăn thuộc danh mục '" + cat.getName() + "' đang ngừng hoạt động (INACTIVE)!";
            }
            return null;
        }
        // Fallback khi khởi tạo không truyền CategoryRepository: kiểm tra dữ liệu món hiện có thuộc danh mục
        if ("ACTIVE".equalsIgnoreCase(targetItemStatus)) {
            List<MenuItem> sameCatItems = menuItemRepository.findAll().stream()
                    .filter(i -> i.getCategory() != null && i.getCategory().equalsIgnoreCase(trimmedCat))
                    .toList();
            if (!sameCatItems.isEmpty() && sameCatItems.stream().noneMatch(MenuItem::isActive)) {
                return "Không thể mở bán (ACTIVE) món ăn thuộc danh mục '" + trimmedCat + "' đang ngừng hoạt động (INACTIVE)!";
            }
        }
        return null;
    }

    public String createMenuItem(String name, double price, String category, String description, String iconEmoji, String status, String actor) {
        if (name == null || name.trim().isEmpty()) {
            auditLogRepository.save(new AuditLog(0, null, actor, "CREATE_MENU", "empty", "FAILED (INVALID_NAME)"));
            return "Tên món không được để trống!";
        }
        if (!Double.isFinite(price) || price <= 0 || price > MAX_MENU_PRICE) {
            auditLogRepository.save(new AuditLog(0, null, actor, "CREATE_MENU", name.trim(), "FAILED (INVALID_PRICE)"));
            return "Giá bán phải là số hữu hạn hợp lệ từ 1 đ đến 10,000,000 đ!";
        }
        if (category == null || category.trim().isEmpty()) {
            auditLogRepository.save(new AuditLog(0, null, actor, "CREATE_MENU", name.trim(), "FAILED (EMPTY_CATEGORY)"));
            return "Vui lòng chọn danh mục!";
        }
        if (!isValidStatus(status)) {
            auditLogRepository.save(new AuditLog(0, null, actor, "CREATE_MENU", name.trim(), "FAILED (INVALID_STATUS)"));
            return "Trạng thái món không hợp lệ (chỉ chấp nhận ACTIVE hoặc INACTIVE)!";
        }
        if (menuItemRepository.existsByName(name.trim())) {
            auditLogRepository.save(new AuditLog(0, null, actor, "CREATE_MENU", name.trim(), "FAILED (DUPLICATE_NAME)"));
            return "Món '" + name.trim() + "' đã tồn tại!";
        }

        String normalizedStatus = status.trim().toUpperCase();
        String catErr = validateCategoryForMenu(category, normalizedStatus);
        if (catErr != null) {
            auditLogRepository.save(new AuditLog(0, null, actor, "CREATE_MENU", name.trim() + " [" + category.trim() + "]", "FAILED (CATEGORY_INACTIVE_OR_INVALID)"));
            return catErr;
        }

        MenuItem item = new MenuItem(0, name.trim(), price, category.trim(), description, iconEmoji, normalizedStatus, null);
        menuItemRepository.save(item);
        auditLogRepository.save(new AuditLog(0, null, actor, "CREATE_MENU", name.trim() + " (" + String.format("%,.0f đ", price) + ")", "SUCCESS"));
        return null;
    }

    public String updateMenuItem(int id, String name, double price, String category, String description, String iconEmoji, String status, String actor) {
        MenuItem existing = menuItemRepository.findById(id);
        if (existing == null) {
            auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_MENU", "ID #" + id, "FAILED (NOT_FOUND)"));
            return "Không tìm thấy món ăn!";
        }
        if (name == null || name.trim().isEmpty()) {
            auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_MENU", existing.getName(), "FAILED (INVALID_NAME)"));
            return "Tên món không được để trống!";
        }
        if (!Double.isFinite(price) || price <= 0 || price > MAX_MENU_PRICE) {
            auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_MENU", name.trim(), "FAILED (INVALID_PRICE)"));
            return "Giá bán phải là số hữu hạn hợp lệ từ 1 đ đến 10,000,000 đ!";
        }
        if (category == null || category.trim().isEmpty()) {
            auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_MENU", name.trim(), "FAILED (EMPTY_CATEGORY)"));
            return "Vui lòng chọn danh mục!";
        }
        if (!isValidStatus(status)) {
            auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_MENU", name.trim(), "FAILED (INVALID_STATUS)"));
            return "Trạng thái món không hợp lệ (chỉ chấp nhận ACTIVE hoặc INACTIVE)!";
        }

        MenuItem duplicate = menuItemRepository.findByName(name.trim());
        if (duplicate != null && duplicate.getId() != id) {
            auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_MENU", name.trim(), "FAILED (DUPLICATE_NAME)"));
            return "Tên món '" + name.trim() + "' đã được sử dụng!";
        }

        String normalizedStatus = status.trim().toUpperCase();
        String catErr = validateCategoryForMenu(category, normalizedStatus);
        if (catErr != null) {
            auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_MENU", name.trim() + " [" + category.trim() + "]", "FAILED (CATEGORY_INACTIVE_OR_INVALID)"));
            return catErr;
        }

        existing.setName(name.trim());
        existing.setPrice(price);
        existing.setCategory(category.trim());
        existing.setDescription(description);
        if (iconEmoji != null && !iconEmoji.trim().isEmpty()) {
            existing.setIconEmoji(iconEmoji.trim());
        }
        existing.setStatus(normalizedStatus);
        menuItemRepository.update(existing);

        auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_MENU", name.trim() + " (" + String.format("%,.0f đ", price) + ")", "SUCCESS"));
        return null;
    }

    public String toggleMenuItemStatus(int id, String actor) {
        MenuItem item = menuItemRepository.findById(id);
        if (item == null) {
            auditLogRepository.save(new AuditLog(0, null, actor, "TOGGLE_MENU", "ID #" + id, "FAILED (NOT_FOUND)"));
            return "Không tìm thấy món ăn!";
        }

        String newStatus = item.isActive() ? "INACTIVE" : "ACTIVE";
        if ("ACTIVE".equalsIgnoreCase(newStatus)) {
            String catErr = validateCategoryForMenu(item.getCategory(), "ACTIVE");
            if (catErr != null) {
                auditLogRepository.save(new AuditLog(0, null, actor, "ENABLE_MENU", item.getName(), "FAILED (CATEGORY_INACTIVE)"));
                return catErr;
            }
        }

        menuItemRepository.setStatus(id, newStatus);
        String action = newStatus.equals("INACTIVE") ? "DISABLE_MENU" : "ENABLE_MENU";
        auditLogRepository.save(new AuditLog(0, null, actor, action, item.getName(), "SUCCESS"));
        return null;
    }
}
