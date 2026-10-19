package cafe.services;

import cafe.models.AuditLog;
import cafe.models.Role;
import cafe.models.User;
import cafe.repositories.AuditLogRepository;
import cafe.repositories.UserRepository;
import java.util.List;

public class AdminUserService {
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminUserService(UserRepository userRepository, AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> searchUsers(String keyword, Role role, String status) {
        return userRepository.searchAndFilter(keyword, role, status);
    }

    private boolean isValidStatus(String status) {
        return status != null && ("ACTIVE".equalsIgnoreCase(status.trim()) || "DISABLED".equalsIgnoreCase(status.trim()));
    }

    private String verifyAdminActor(String actor, String action, String target) {
        if (actor == null || actor.trim().isEmpty()) {
            auditLogRepository.save(new AuditLog(0, null, "anonymous", action, target, "FAILED (UNAUTHENTICATED)"));
            return "Không xác định được tài khoản thực hiện thao tác!";
        }
        User actorUser = userRepository.findByUsername(actor.trim());
        if (actorUser == null || !actorUser.isActive() || actorUser.getRole() != Role.ADMIN) {
            auditLogRepository.save(new AuditLog(0, null, actor.trim(), action, target, "FAILED (FORBIDDEN)"));
            return "Từ chối truy cập: Chỉ tài khoản ADMIN đang hoạt động mới có quyền thực hiện thao tác này!";
        }
        return null;
    }

    private long countOtherActiveAdmins(int excludeUserId) {
        return userRepository.findAll().stream()
                .filter(u -> u.getId() != excludeUserId && u.getRole() == Role.ADMIN && u.isActive())
                .count();
    }

    public String authenticateAdmin(String username, String password) {
        String u = username != null ? username.trim() : "";
        String p = password != null ? password.trim() : "";
        if (u.isEmpty() || p.isEmpty()) {
            auditLogRepository.save(new AuditLog(0, null, u.isEmpty() ? "anonymous" : u, "LOGIN", "Admin Portal", "FAILED (EMPTY_CREDENTIALS)"));
            return "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!";
        }
        User user = userRepository.findByUsername(u);
        if (user == null || !user.verifyPassword(p)) {
            auditLogRepository.save(new AuditLog(0, null, u, "LOGIN", "Admin Portal", "FAILED (INVALID_CREDENTIALS)"));
            return "Sai tên đăng nhập hoặc mật khẩu!";
        }
        if (!user.isActive()) {
            auditLogRepository.save(new AuditLog(0, null, u, "LOGIN", "Admin Portal", "FAILED (ACCOUNT_DISABLED)"));
            return "Tài khoản này đang bị vô hiệu hóa!";
        }
        if (user.getRole() != Role.ADMIN) {
            auditLogRepository.save(new AuditLog(0, null, u, "LOGIN", "Admin Portal", "FAILED (FORBIDDEN_ROLE)"));
            return "Chỉ tài khoản có vai trò ADMIN mới có quyền truy cập!";
        }
        auditLogRepository.save(new AuditLog(0, null, user.getUsername(), "LOGIN", "Admin Portal", "SUCCESS"));
        return null;
    }

    public String createUser(String username, String fullName, String password, String confirmPassword, Role role, String status, String actor) {
        String target = (username != null && !username.trim().isEmpty()) ? username.trim() : "unknown";
        String permErr = verifyAdminActor(actor, "CREATE_USER", target);
        if (permErr != null) return permErr;

        if (username == null || username.trim().isEmpty()) {
            auditLogRepository.save(new AuditLog(0, null, actor, "CREATE_USER", "empty", "FAILED (INVALID_USERNAME)"));
            return "Tên đăng nhập không được để trống!";
        }
        String trimmedUser = username.trim();
        if (!trimmedUser.matches("^[a-zA-Z0-9_]{4,30}$")) {
            auditLogRepository.save(new AuditLog(0, null, actor, "CREATE_USER", trimmedUser, "FAILED (INVALID_USERNAME_FORMAT)"));
            return "Tên đăng nhập phải từ 4-30 ký tự, chỉ gồm chữ cái không dấu, chữ số hoặc dấu gạch dưới (_) và không chứa khoảng trắng!";
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            auditLogRepository.save(new AuditLog(0, null, actor, "CREATE_USER", trimmedUser, "FAILED (EMPTY_FULLNAME)"));
            return "Họ và tên không được để trống!";
        }
        if (password == null || password.trim().length() < 6) {
            auditLogRepository.save(new AuditLog(0, null, actor, "CREATE_USER", trimmedUser, "FAILED (WEAK_PASSWORD)"));
            return "Mật khẩu phải có tối thiểu 6 ký tự!";
        }
        if (!password.equals(confirmPassword)) {
            auditLogRepository.save(new AuditLog(0, null, actor, "CREATE_USER", trimmedUser, "FAILED (PASSWORD_MISMATCH)"));
            return "Mật khẩu xác nhận không khớp!";
        }
        if (role == null) {
            auditLogRepository.save(new AuditLog(0, null, actor, "CREATE_USER", trimmedUser, "FAILED (NULL_ROLE)"));
            return "Vui lòng chọn vai trò!";
        }
        if (!isValidStatus(status)) {
            auditLogRepository.save(new AuditLog(0, null, actor, "CREATE_USER", trimmedUser, "FAILED (INVALID_STATUS)"));
            return "Trạng thái tài khoản không hợp lệ (chỉ chấp nhận ACTIVE hoặc DISABLED)!";
        }
        if (userRepository.existsByUsername(trimmedUser)) {
            auditLogRepository.save(new AuditLog(0, null, actor, "CREATE_USER", trimmedUser, "FAILED (DUPLICATE_USERNAME)"));
            return "Tên đăng nhập '" + trimmedUser + "' đã tồn tại!";
        }

        String normalizedStatus = status.trim().toUpperCase();
        User newUser = new User(0, trimmedUser, fullName.trim(), User.hashPassword(password), role, normalizedStatus, null);
        userRepository.save(newUser);
        auditLogRepository.save(new AuditLog(0, null, actor, "CREATE_USER", trimmedUser + " (" + role.name() + ")", "SUCCESS"));
        return null; // success
    }

    public String updateUser(int id, String fullName, Role role, String status, String actor, String currentLoggedInUsername) {
        User existing = userRepository.findById(id);
        String target = existing != null ? existing.getUsername() : ("ID #" + id);
        String permErr = verifyAdminActor(actor, "UPDATE_USER", target);
        if (permErr != null) return permErr;

        if (existing == null) {
            auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_USER", target, "FAILED (NOT_FOUND)"));
            return "Không tìm thấy người dùng!";
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_USER", existing.getUsername(), "FAILED (EMPTY_FULLNAME)"));
            return "Họ và tên không được để trống!";
        }
        if (role == null) {
            auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_USER", existing.getUsername(), "FAILED (NULL_ROLE)"));
            return "Vui lòng chọn vai trò!";
        }
        if (!isValidStatus(status)) {
            auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_USER", existing.getUsername(), "FAILED (INVALID_STATUS)"));
            return "Trạng thái tài khoản không hợp lệ (chỉ chấp nhận ACTIVE hoặc DISABLED)!";
        }

        String normalizedStatus = status.trim().toUpperCase();
        boolean isSelf = (currentLoggedInUsername != null && existing.getUsername().equalsIgnoreCase(currentLoggedInUsername.trim()))
                || (actor != null && existing.getUsername().equalsIgnoreCase(actor.trim()));

        // Rule 1: Không thể tự khóa hoặc tự hạ quyền của chính mình
        if (isSelf) {
            if ("DISABLED".equalsIgnoreCase(normalizedStatus)) {
                auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_USER", existing.getUsername(), "FAILED (SELF_DISABLE)"));
                return "Không thể vô hiệu hóa chính tài khoản Admin đang đăng nhập!";
            }
            if (role != Role.ADMIN) {
                auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_USER", existing.getUsername(), "FAILED (SELF_DEMOTE)"));
                return "Không thể tự hạ quyền quản trị (ADMIN) của chính tài khoản đang đăng nhập!";
            }
        }

        // Rule 2: Không cho phép vô hiệu hóa hoặc hạ quyền tài khoản ADMIN hoạt động cuối cùng
        if (existing.getRole() == Role.ADMIN && existing.isActive()) {
            if (role != Role.ADMIN || !"ACTIVE".equalsIgnoreCase(normalizedStatus)) {
                if (countOtherActiveAdmins(existing.getId()) == 0) {
                    auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_USER", existing.getUsername(), "FAILED (LAST_ADMIN_PROTECTION)"));
                    return "Không thể hạ quyền hoặc vô hiệu hóa tài khoản ADMIN hoạt động cuối cùng của hệ thống!";
                }
            }
        }

        existing.setFullName(fullName.trim());
        existing.setRole(role);
        existing.setStatus(normalizedStatus);
        userRepository.update(existing);

        auditLogRepository.save(new AuditLog(0, null, actor, "UPDATE_USER", existing.getUsername() + " (" + role.name() + ", " + normalizedStatus + ")", "SUCCESS"));
        return null;
    }

    public String toggleUserStatus(int id, String actor, String currentLoggedInUsername) {
        User u = userRepository.findById(id);
        String target = u != null ? u.getUsername() : ("ID #" + id);
        String permErr = verifyAdminActor(actor, "TOGGLE_USER_STATUS", target);
        if (permErr != null) return permErr;

        if (u == null) {
            auditLogRepository.save(new AuditLog(0, null, actor, "TOGGLE_USER_STATUS", target, "FAILED (NOT_FOUND)"));
            return "Không tìm thấy người dùng!";
        }

        boolean isSelf = (currentLoggedInUsername != null && u.getUsername().equalsIgnoreCase(currentLoggedInUsername.trim()))
                || (actor != null && u.getUsername().equalsIgnoreCase(actor.trim()));
        if (isSelf && u.isActive()) {
            auditLogRepository.save(new AuditLog(0, null, actor, "DISABLE_USER", u.getUsername(), "FAILED (SELF_DISABLE)"));
            return "Không thể vô hiệu hóa chính tài khoản Admin đang đăng nhập!";
        }

        if (u.isActive() && u.getRole() == Role.ADMIN && countOtherActiveAdmins(u.getId()) == 0) {
            auditLogRepository.save(new AuditLog(0, null, actor, "DISABLE_USER", u.getUsername(), "FAILED (LAST_ADMIN_PROTECTION)"));
            return "Không thể vô hiệu hóa tài khoản ADMIN hoạt động cuối cùng của hệ thống!";
        }

        String newStatus = u.isActive() ? "DISABLED" : "ACTIVE";
        userRepository.setStatus(id, newStatus);
        String action = newStatus.equals("DISABLED") ? "DISABLE_USER" : "ENABLE_USER";
        auditLogRepository.save(new AuditLog(0, null, actor, action, u.getUsername(), "SUCCESS"));
        return null;
    }

    public String resetPassword(int id, String newPassword, String actor) {
        User u = userRepository.findById(id);
        String target = u != null ? u.getUsername() : ("ID #" + id);
        String permErr = verifyAdminActor(actor, "RESET_PASSWORD", target);
        if (permErr != null) return permErr;

        if (u == null) {
            auditLogRepository.save(new AuditLog(0, null, actor, "RESET_PASSWORD", target, "FAILED (NOT_FOUND)"));
            return "Không tìm thấy người dùng!";
        }
        if (newPassword == null || newPassword.trim().length() < 6) {
            auditLogRepository.save(new AuditLog(0, null, actor, "RESET_PASSWORD", u.getUsername(), "FAILED (WEAK_PASSWORD)"));
            return "Mật khẩu mới phải có tối thiểu 6 ký tự!";
        }

        userRepository.resetPassword(id, User.hashPassword(newPassword.trim()));
        auditLogRepository.save(new AuditLog(0, null, actor, "RESET_PASSWORD", u.getUsername(), "SUCCESS"));
        return null;
    }
}
