package cafe.controllers;

import cafe.models.StaffModel;
import cafe.services.ClientSocketService;
import cafe.services.DatabaseManager;
import cafe.views.ClientView;
import cafe.views.FloorPlanView;
import cafe.views.LoginView;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

public class LoginController {
    private final Stage stage;
    private final LoginView view;
    private final ClientSocketService socketService;

    private int activeTableNumber = 5;
    private Timeline serverPollTimeline;

    public LoginController(Stage stage, LoginView view, ClientSocketService socketService) {
        this.stage = stage;
        this.view = view;
        this.socketService = socketService;

        init();
        initServerStatus();
    }

    private void init() {
        view.getLoginButton().setOnAction(e -> handleLogin());
        view.getTableField().setOnAction(e -> handleLogin());
        view.getPasswordField().setOnAction(e -> handleLogin());
    }

    private void initServerStatus() {
        socketService.setOnConnectionStateChanged(connected -> {
            Platform.runLater(() -> view.setServerStatus(connected, "127.0.0.1", 5000));
        });

        view.getBtnRetryConnection().setOnAction(e -> {
            view.getBtnRetryConnection().setDisable(true);
            new Thread(() -> {
                boolean ok = socketService.tryConnectToServer();
                Platform.runLater(() -> {
                    view.setServerStatus(ok, "127.0.0.1", 5000);
                    view.getBtnRetryConnection().setDisable(false);
                });
            }, "Retry-Connect-Thread").start();
        });

        view.setServerStatus(socketService.isConnected(), "127.0.0.1", 5000);
        startServerPoll();
    }

    private void startServerPoll() {
        stopServerPoll();
        serverPollTimeline = new Timeline(
            new KeyFrame(Duration.seconds(3), ev -> {
                if (!socketService.isConnected()) {
                    new Thread(() -> {
                        boolean ok = socketService.tryConnectToServer();
                        Platform.runLater(() -> {
                            view.setServerStatus(ok, "127.0.0.1", 5000);
                        });
                    }, "Poll-Connect-Thread").start();
                } else {
                    view.setServerStatus(true, "127.0.0.1", 5000);
                }
            })
        );
        serverPollTimeline.setCycleCount(Animation.INDEFINITE);
        serverPollTimeline.play();
    }

    private void stopServerPoll() {
        if (serverPollTimeline != null) {
            serverPollTimeline.stop();
            serverPollTimeline = null;
        }
    }

    private void handleLogin() {
        view.clearError();

        // 1. Kiểm tra kết nối Máy chủ trung tâm (Port 5000)
        if (!socketService.isConnected()) {
            boolean ok = socketService.tryConnectToServer();
            if (!ok) {
                view.setServerStatus(false, "127.0.0.1", 5000);
                view.showError("⚠️ Chưa kết nối Máy chủ trung tâm (Port 5000)!\nVui lòng khởi động ServerMain trước rồi bấm [Thử lại].");
                return;
            }
            view.setServerStatus(true, "127.0.0.1", 5000);
        }

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
                stopServerPoll();
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
                stopServerPoll();
                this.activeTableNumber = table;
                if (staff.isManager() || "admin".equalsIgnoreCase(staff.getUsername())) {
                    openAdminDashboard(staff);
                } else {
                    openFloorPlan(table, staff);
                }
            } else {
                view.showError("Không thể kết nối đến quầy pha chế! Vui lòng kiểm tra lại Server.");
            }
            return;
        }

        // Nếu người dùng nhập số bàn nhưng có mật khẩu không khớp nhân viên
        if (potentialTable > 0) {
            boolean connected = socketService.connect(potentialTable);
            if (connected) {
                stopServerPoll();
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
     * MỞ MÀN HÌNH QUẢN TRỊ TRUNG TÂM (ADMIN / MANAGER POS - 10 PHÂN HỆ):
     */
    private void openAdminDashboard(StaffModel staff) {
        cafe.services.CafeServerService serverService = cafe.services.CafeServerService.getInstance();
        cafe.views.ServerKDSView kdsView = new cafe.views.ServerKDSView();
        new ServerController(kdsView, serverService);

        cafe.views.AdminShellView adminShell = new cafe.views.AdminShellView(serverService, kdsView);
        adminShell.attachClientContext(stage, socketService);
        adminShell.setOnLogout(() -> {
            socketService.disconnect();
            socketService.warmupConnectAsync();
            returnToLoginView();
        });

        serverService.setOnOrderReceived(order -> {
            adminShell.notifyNewOrderArrived(order);
        });

        String title = "CoffeePOS • Trung Tâm Quản Trị & Điều Phối (" + (staff != null ? staff.getFullName() : "Quản lý") + ")";
        cafe.utils.WindowChromeHelper.setWindowContent(
            stage,
            adminShell,
            title,
            true,
            () -> {
                socketService.disconnect();
                System.exit(0);
            }
        );
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
        new ClientController(
            stage,
            clientView,
            socketService,
            tableNumber,
            null
        );

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
     * MỞ MÀN HÌNH NHÂN VIÊN (UNIFIED STAFF POS SHELL / SƠ ĐỒ BÀN 3 TẦNG):
     * - Giữ cố định thanh điều hướng 235px bên trái xuyên suốt cả 5 phân hệ (Bàn, Thực đơn, Đơn hàng, Thanh toán, Cài đặt).
     * - Lắng nghe thay đổi trạng thái bàn từ Server để đổi màu bàn thời gian thực.
     */
    private void openFloorPlan(int initialTable, StaffModel staff) {
        this.activeTableNumber = initialTable;

        FloorPlanView floorPlanView = new FloorPlanView(
            activeTableNumber,
            selectedTable -> {
                this.activeTableNumber = selectedTable;
                socketService.switchTable(selectedTable);
            },
            () -> {
                openStaffMenu(activeTableNumber, staff);
            },
            () -> {
                socketService.disconnect();
                socketService.warmupConnectAsync();
                returnToLoginView();
            }
        );

        floorPlanView.attachEmbeddedOrdering(
            stage,
            socketService,
            (staff != null && staff.isManager()) ? () -> openAdminDashboard(staff) : null
        );

        if (staff != null) {
            floorPlanView.setStaffProfile(staff);
        }

        socketService.setOnTableStatusReceived((tableNum, status) -> {
            floorPlanView.setTableStatus(tableNum, status);
        });

        String title = "Sơ Đồ Bàn & Phục Vụ • Brew & Bean CAFE (" + (staff != null ? staff.getFullName() : "Nhân viên") + ")";
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
     * MÀN HÌNH THỰC ĐƠN ĐỘC LẬP (FALLBACK):
     */
    private void openStaffMenu(int tableNumber, StaffModel staff) {
        this.activeTableNumber = tableNumber;

        ClientView clientView = new ClientView();
        new ClientController(
            stage,
            clientView,
            socketService,
            tableNumber,
            () -> openFloorPlan(activeTableNumber, staff)
        );

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
        startServerPoll();
        view.setServerStatus(socketService.isConnected(), "127.0.0.1", 5000);

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

