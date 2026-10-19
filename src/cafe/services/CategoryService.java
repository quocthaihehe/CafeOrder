package cafe.services;

import cafe.models.AuditLog;
import cafe.models.Category;
import cafe.repositories.AuditLogRepository;
import cafe.repositories.CategoryRepository;
import cafe.repositories.MenuItemRepository;
import java.util.List;

public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final AuditLogRepository auditLogRepository;

    public CategoryService(CategoryRepository categoryRepository, MenuItemRepository menuItemRepository, AuditLogRepository auditLogRepository) {
        this.categoryRepository = categoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public List<Category> searchCategories(String keyword) {
        return categoryRepository.search(keyword);
    }

    private boolean isValidStatus(String status) {
        return status != null && ("ACTIVE".equalsIgnoreCase(status.trim()) || "INACTIVE".equalsIgnoreCase(status.trim()));
    }

    public String createCategory(String name, String status, String actor) {
        if (name == null || name.trim().isEmpty()) {
            auditLogRepository.save(new AuditLog(0, null, actor, "CREATE_CATEGORY", "empty", "FAILED (INVALID_NAME)"));
            return "Tên danh mục không được để trống!";
        }
        if (!isValidStatus(status)) {
            auditLogRepository.save(new AuditLog(0, null, actor, "CREATE_CATEGORY", name.trim(), "FAILED (INVALID_STATUS)"));
            return "Trạng thái danh mục không hợp lệ (chỉ chấp nhận ACTIVE hoặc INACTIVE)!";
        }
        if (categoryRepository.existsByName(name.trim())) {
            auditLogRepository.save(new AuditLog(0, null, actor, "CREATE_CATEGORY", name.trim(), "FAILED (DUPLICATE_NAME)"));
            return "Danh mục '" + name.trim() + "' đã tồn tại!";
        }

        String normalizedStatus = status.trim().toUpperCase();
        Category cat = new Category(0, name.trim(), normalizedStatus, null);
        categoryRepository.save(cat);
        auditLogRepository.save(new AuditLog(0, null, actor, "CREATE_CATEGORY", name.trim(), "SUCCESS"));
        return null;
    }

    public String updateCategory(int id, String name, String status, String actor) {
        Category existing = categoryRepository.findById(id);
        if (existing == null) {
            auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_CATEGORY", "ID #" + id, "FAILED (NOT_FOUND)"));
            return "Không tìm thấy danh mục!";
        }
        if (name == null || name.trim().isEmpty()) {
            auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_CATEGORY", existing.getName(), "FAILED (INVALID_NAME)"));
            return "Tên danh mục không được để trống!";
        }
        if (!isValidStatus(status)) {
            auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_CATEGORY", existing.getName(), "FAILED (INVALID_STATUS)"));
            return "Trạng thái danh mục không hợp lệ (chỉ chấp nhận ACTIVE hoặc INACTIVE)!";
        }

        String newName = name.trim();
        String normalizedStatus = status.trim().toUpperCase();
        String oldName = existing.getName();

        Category duplicate = categoryRepository.findByName(newName);
        if (duplicate != null && duplicate.getId() != id) {
            auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_CATEGORY", newName, "FAILED (DUPLICATE_NAME)"));
            return "Tên danh mục '" + newName + "' đã được sử dụng bởi danh mục khác!";
        }

        // Nếu chuyển từ ACTIVE sang INACTIVE, kiểm tra xem có món nào đang ACTIVE không (theo cả tên cũ và tên mới)
        if ("INACTIVE".equalsIgnoreCase(normalizedStatus) && existing.isActive()) {
            boolean hasActiveItems = menuItemRepository.findAll().stream()
                    .anyMatch(item -> item.getCategory() != null &&
                            (item.getCategory().equalsIgnoreCase(oldName) || item.getCategory().equalsIgnoreCase(newName)) &&
                            item.isActive());
            if (hasActiveItems) {
                auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_CATEGORY", oldName, "FAILED (HAS_ACTIVE_ITEMS)"));
                return "Không thể vô hiệu hóa danh mục đang có món ăn mở bán! Vui lòng vô hiệu hóa hoặc chuyển các món liên quan trước.";
            }
        }

        existing.setName(newName);
        existing.setStatus(normalizedStatus);
        categoryRepository.update(existing);

        // Đồng bộ tên danh mục mới cho toàn bộ món ăn đang tham chiếu tên danh mục cũ
        if (oldName != null && !oldName.equals(newName)) {
            for (cafe.models.MenuItem item : menuItemRepository.findAll()) {
                if (item.getCategory() != null && item.getCategory().equalsIgnoreCase(oldName)) {
                    item.setCategory(newName);
                    menuItemRepository.update(item);
                }
            }
        }

        auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_CATEGORY", oldName.equals(newName) ? newName : (oldName + " -> " + newName), "SUCCESS"));
        return null;
    }

    public String toggleCategoryStatus(int id, String actor) {
        Category cat = categoryRepository.findById(id);
        if (cat == null) {
            auditLogRepository.save(new AuditLog(0, null, actor, "TOGGLE_CATEGORY", "ID #" + id, "FAILED (NOT_FOUND)"));
            return "Không tìm thấy danh mục!";
        }

        // Nếu danh mục đang ACTIVE và muốn chuyển sang INACTIVE
        if (cat.isActive()) {
            boolean hasActiveItems = menuItemRepository.findAll().stream()
                    .anyMatch(item -> item.getCategory() != null &&
                            item.getCategory().equalsIgnoreCase(cat.getName()) &&
                            item.isActive());
            if (hasActiveItems) {
                auditLogRepository.save(new AuditLog(0, null, actor, "DISABLE_CATEGORY", cat.getName(), "FAILED (HAS_ACTIVE_ITEMS)"));
                return "Không thể vô hiệu hóa danh mục đang có món ăn mở bán! Vui lòng vô hiệu hóa hoặc chuyển các món liên quan trước.";
            }
        }

        String newStatus = cat.isActive() ? "INACTIVE" : "ACTIVE";
        categoryRepository.setStatus(id, newStatus);
        String action = newStatus.equals("INACTIVE") ? "DISABLE_CATEGORY" : "ENABLE_CATEGORY";
        auditLogRepository.save(new AuditLog(0, null, actor, action, cat.getName(), "SUCCESS"));
        return null;
    }
}
