package cafe.controllers;

import cafe.models.StaffModel;
import cafe.services.ClientSocketService;
import cafe.services.DatabaseManager;
import cafe.views.ClientView;
import cafe.views.FloorPlanView;
import cafe.views.LoginView;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LoginController {
    private final Stage stage;
    private final LoginView view;
    private final ClientSocketService socketService;

    private int activeTableNumber = 5;

    public LoginController(Stage stage, LoginView view, ClientSocketService socketService) {
        this.stage = stage;
        this.view = view;
        this.socketService = socketService;

        init();
    }

    private void init() {
        view.getLoginButton().setOnAction(e -> handleLogin());
        view.getTableField().setOnAction(e -> handleLogin());
        view.getPasswordField().setOnAction(e -> handleLogin());
    }

    private void handleLogin() {
        view.clearError();
        String account = view.getAccountField().getText().trim();
        String password = view.getPasswordField().getText().trim();

        if (account.isEmpty()) {
            view.showError("Vui lòng nhập số bàn (1..12) hoặc tài khoản nhân viên!");
            return;
        }

        int potentialTable = parseTableNumber(account);

        // =====================================================================
        // NHÁNH 1: KHÁCH HÀNG (CUSTOMER KIOSK TẠI BÀN)
        // Điều kiện: Nhập số bàn (VD: 5, 05, Ban 5...) và mật khẩu để trống hoặc gõ số bàn
        // =====================================================================
        if (potentialTable > 0 && (password.isEmpty() || password.equalsIgnoreCase(account))) {
            boolean connected = socketService.connect(potentialTable);
            if (connected) {
                this.activeTableNumber = potentialTable;
                openCustomerMenu(potentialTable);
            } else {
                view.showError("Không thể kết nối đến quầy pha chế! Vui lòng kiểm tra lại Server.");
            }
            return;
        }

        // =====================================================================
        // NHÁNH 2: NHÂN VIÊN / QUẢN TRỊ (STAFF / MANAGER POS)
        // Điều kiện: Đăng nhập tài khoản Staff đối chiếu với SQL Server / Fallback
        // =====================================================================
        StaffModel staff = DatabaseManager.authenticateStaff(account, password);
        if (staff != null) {
            int table = (potentialTable > 0) ? potentialTable : 5;
            boolean connected = socketService.connectStaff(table);
            if (connected) {
                this.activeTableNumber = table;
                openFloorPlan(table, staff);
            } else {
                view.showError("Không thể kết nối đến quầy pha chế! Vui lòng kiểm tra lại Server.");
            }
            return;
        }

        // Nếu người dùng nhập số bàn nhưng có mật khẩu không khớp nhân viên
        if (potentialTable > 0) {
            boolean connected = socketService.connect(potentialTable);
            if (connected) {
                this.activeTableNumber = potentialTable;
                openCustomerMenu(potentialTable);
                return;
            }
        }

        view.showError("Tài khoản hoặc mật khẩu không chính xác!");
    }

    private int parseTableNumber(String input) {
        if (input == null) return -1;
        String clean = input.replaceAll("(?i)(bàn|ban|table|#)", "").trim();
        try {
            int t = Integer.parseInt(clean);
            if (t > 0 && t <= 350) return t;
        } catch (NumberFormatException ignored) {}
        return -1;
    }

    /**
     * MỞ MÀN HÌNH KHÁCH HÀNG (CUSTOMER KIOSK TẠI BÀN):
     * - Khóa cứng số bàn của khách.
     * - ẨN NÚT "SƠ ĐỒ BÀN" (Khách không được xem sơ đồ quán và không thể đổi sang bàn khác).
     * - Mở Live Order Tracker tại góc màn hình để theo dõi tiến độ Barista pha chế.
     */
    private void openCustomerMenu(int tableNumber) {
        this.activeTableNumber = tableNumber;

        ClientView clientView = new ClientView();
        // Không truyền callback quay lại Sơ đồ bàn -> Khách ở chế độ Kiosk cố định
        new ClientController(
            stage,
            clientView,
            socketService,
            tableNumber,
            null
        );

        // Ẩn hoàn toàn nút Sơ đồ bàn
        if (clientView.getFloorPlanButton() != null) {
            clientView.getFloorPlanButton().setVisible(false);
            clientView.getFloorPlanButton().setManaged(false);
        }

        String menuTitle = "Brew & Bean CAFE • Bàn " + (tableNumber < 10 ? "0" + tableNumber : tableNumber);
        cafe.utils.WindowChromeHelper.setWindowContent(
            stage,
            clientView,
            menuTitle,
            true,
            () -> {
                socketService.disconnect();
                System.exit(0);
            }
        );
    }

    /**
     * MỞ MÀN HÌNH NHÂN VIÊN (STAFF POS / SƠ ĐỒ BÀN 3 TẦNG):
     * - Hiển thị hồ sơ nhân viên đang trực ca (VD: Trần Minh Anh · Nhân viên).
     * - Lắng nghe thay đổi trạng thái bàn từ Server để đổi màu bàn thời gian thực (Trống -> Đang phục vụ).
     * - Có thể bấm vào bàn bất kỳ để đặt món hộ khách.
     */
    private void openFloorPlan(int initialTable, StaffModel staff) {
        this.activeTableNumber = initialTable;

        FloorPlanView floorPlanView = new FloorPlanView(
            activeTableNumber,
            selectedTable -> {
                this.activeTableNumber = selectedTable;
                // Báo với Server để Server chuyển tiếp cập nhật đơn của bàn này
                socketService.switchTable(selectedTable);
            },
            () -> {
                // Nhân viên chuyển sang Màn hình Thực đơn để đặt món hộ khách
                openStaffMenu(activeTableNumber, staff);
            },
            () -> {
                // Đăng xuất ca trực: ngắt kết nối và quay lại LoginView, đồng thời kết nối ngầm sẵn cho lần đăng nhập kế tiếp
                socketService.disconnect();
                socketService.warmupConnectAsync();
                returnToLoginView();
            }
        );

        if (staff != null) {
            floorPlanView.setStaffProfile(staff);
        }

        // Lắng nghe broadcast trạng thái bàn từ Server
        socketService.setOnTableStatusReceived((tableNum, status) -> {
            floorPlanView.setTableStatus(tableNum, status);
        });

        String title = "Sơ Đồ Bàn • Brew & Bean CAFE (" + (staff != null ? staff.getFullName() : "Nhân viên") + ")";
        cafe.utils.WindowChromeHelper.setWindowContent(
            stage,
            floorPlanView,
            title,
            true,
            () -> {
                socketService.disconnect();
                System.exit(0);
            }
        );
    }

    /**
     * MÀN HÌNH THỰC ĐƠN DÀNH CHO NHÂN VIÊN ĐẶT MÓN HỘ:
     * - HIỂN THỊ NÚT "← Sơ đồ bàn" để nhân viên sau khi order có thể quay lại quản lý bàn!
     */
    private void openStaffMenu(int tableNumber, StaffModel staff) {
        this.activeTableNumber = tableNumber;

        ClientView clientView = new ClientView();
        new ClientController(
            stage,
            clientView,
            socketService,
            tableNumber,
            () -> openFloorPlan(activeTableNumber, staff) // Quay lại sơ đồ bàn
        );

        // Đảm bảo nút Sơ đồ bàn hiển thị rõ ràng cho nhân viên
        if (clientView.getFloorPlanButton() != null) {
            clientView.getFloorPlanButton().setVisible(true);
            clientView.getFloorPlanButton().setManaged(true);
        }

        String menuTitle = "Brew & Bean CAFE • Đặt món Bàn " + (tableNumber < 10 ? "0" + tableNumber : tableNumber);
        cafe.utils.WindowChromeHelper.setWindowContent(
            stage,
            clientView,
            menuTitle,
            true,
            () -> {
                socketService.disconnect();
                System.exit(0);
            }
        );
    }

    private void returnToLoginView() {
        cafe.utils.WindowChromeHelper.setWindowContent(
            stage,
            view,
            "Đăng Nhập • Brew & Bean CAFE",
            true,
            () -> {
                socketService.disconnect();
                System.exit(0);
            }
        );

        stage.setMinWidth(720);
        stage.setMinHeight(650);
        view.setMaximizedMode(stage.isMaximized());
    }
}

