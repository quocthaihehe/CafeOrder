package cafe.models;

public enum Role {
    ADMIN("Quản trị viên"),
    CASHIER("Thu ngân"),
    BARISTA("Pha chế"),
    STAFF("Phục vụ");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
