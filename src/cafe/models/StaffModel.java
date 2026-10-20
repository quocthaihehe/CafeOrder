package cafe.models;

public class StaffModel {
    private int staffId;
    private String username;
    private String fullName;
    private String role; // "OWNER", "MANAGER", "CASHIER", "BARISTA", "WAITER"
    private String status;

    public StaffModel(int staffId, String username, String fullName, String role, String status) {
        this.staffId = staffId;
        this.username = username;
        this.fullName = fullName;
        this.role = role != null ? role.toUpperCase() : "CASHIER";
        this.status = status != null ? status : "ACTIVE";
    }

    public int getStaffId() {
        return staffId;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    public String getRoleDisplayName() {
        switch (role) {
            case "OWNER": return "Chủ quán";
            case "MANAGER": return "Quản lý";
            case "CASHIER": return "Thu ngân";
            case "BARISTA": return "Pha chế";
            case "WAITER": return "Nhân viên phục vụ";
            default: return "Nhân viên";
        }
    }

    public String getStatus() {
        return status;
    }

    public boolean isManagerOrOwner() {
        return "OWNER".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role);
    }

    public boolean isManager() {
        return isManagerOrOwner();
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setRole(String role) {
        if (role != null) this.role = role.toUpperCase();
    }

    public void setStatus(String status) {
        if (status != null) this.status = status;
    }
}
