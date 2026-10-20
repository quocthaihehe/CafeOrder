package cafe;

import cafe.controllers.ServerController;
import cafe.models.StaffModel;
import cafe.repositories.mock.*;
import cafe.services.*;
import cafe.utils.FontLoader;
import cafe.views.ServerKDSView;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/**
 * AdminMain - Điểm khởi chạy Phân hệ Quản trị Trung tâm:
 * - Đồng bộ hóa với CafeServerService và DatabaseManager thực tế.
 * - Hiển thị toàn diện 10 phân hệ: Tổng quan BI, POS, Bếp KDS, Menu, Bàn phòng, Đơn hàng, Kho BOM, Nhân sự, Báo cáo & Cài đặt.
 * - Hỗ trợ đăng xuất chuyển tài khoản hoặc thoát phiên làm việc an toàn.
 */
public class AdminMain {

    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        cafe.utils.WarningSuppressor.suppress();
        Application.launch(AdminApp.class, args);
    }

    public static class AdminApp extends Application {
        // Mock Repositories (Duy trì tính tương thích ngược cho các bài test giai đoạn 4)
        private final MockUserRepository userRepo = new MockUserRepository();
        private final MockCategoryRepository categoryRepo = new MockCategoryRepository();
        private final MockMenuItemRepository menuRepo = new MockMenuItemRepository();
        private final MockTableRepository tableRepo = new MockTableRepository();
        private final MockDeviceRepository deviceRepo = new MockDeviceRepository();
        private final MockOrderRepository orderRepo = new MockOrderRepository();
        private final MockPaymentRepository paymentRepo = new MockPaymentRepository();
        private final MockAuditLogRepository auditRepo = new MockAuditLogRepository();

        // Services
        private AdminUserService userService;

        private Stage primaryStage;

        @Override
        public void start(Stage primaryStage) {
            this.primaryStage = primaryStage;

            // Nạp font nhận diện thương hiệu
            FontLoader.loadFonts();

            userService = new AdminUserService(userRepo, auditRepo);

            showLoginScreen();
        }

        /**
         * Mở giao diện Quản trị Trung tâm Thực tế (Live CoffeePOS Shell):
         * Sử dụng chung dữ liệu thời gian thực với Máy chủ Socket và KDS.
         */
        public void showAdminShell(String username) {
            CafeServerService serverService = CafeServerService.getInstance();
            serverService.start();

            ServerKDSView kdsView = new ServerKDSView();
            new ServerController(kdsView, serverService);

            // Giao diện Quản trị & Điều phối Quầy Trung tâm (Bản chuẩn thực tế 10 phân hệ)
            cafe.views.AdminShellView shellView = new cafe.views.AdminShellView(serverService, kdsView);

            // Xử lý đăng xuất quay về màn hình Login
            shellView.setOnLogout(this::showLoginScreen);

            // Đồng bộ live đơn hàng mới
            serverService.setOnOrderReceived(order -> {
                shellView.notifyNewOrderArrived(order);
            });

            Scene scene = new Scene(shellView, 1280, 800);
            try {
                scene.getStylesheets().add(getClass().getResource("/cafe/styles/theme.css").toExternalForm());
                scene.getStylesheets().add(getClass().getResource("/cafe/styles/cafe-theme.css").toExternalForm());
            } catch (Exception ignored) {}

            primaryStage.setTitle("CoffeePOS • Quản Trị & Quầy Điều Phối Trung Tâm (" + username + ")");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1100);
            primaryStage.setMinHeight(700);
            primaryStage.setWidth(1280);
            primaryStage.setHeight(820);
            primaryStage.centerOnScreen();
            primaryStage.show();
        }

        public void showLoginScreen() {
            VBox root = new VBox(16);
            root.setAlignment(Pos.CENTER);
            root.setPadding(new Insets(30));
            root.setStyle("-fx-background-color: #FAF7F2;");

            VBox card = new VBox(14);
            card.setAlignment(Pos.CENTER);
            card.setMaxWidth(380);
            card.setPadding(new Insets(32, 28, 32, 28));
            card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16; -fx-border-color: #EADBCE; -fx-border-radius: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 20, 0, 0, 4);");

            Label icon = new Label("☕");
            icon.setStyle("-fx-font-size: 36px; -fx-background-color: #F8EDE5; -fx-background-radius: 14; -fx-padding: 8 16;");

            Label title = new Label("Đăng Nhập Quản Trị");
            title.setStyle("-fx-font-family: 'Segoe UI', 'Inter', sans-serif; -fx-font-size: 19px; -fx-font-weight: 800; -fx-text-fill: #2C1810;");

            Label sub = new Label("CoffeePOS • Brew & Bean Central Management");
            sub.setStyle("-fx-font-family: 'Segoe UI', 'Inter', sans-serif; -fx-font-size: 12px; -fx-text-fill: #8D7B74;");

            TextField txtUser = new TextField("admin");
            txtUser.setPromptText("Tên đăng nhập (admin / admin01)");
            txtUser.setStyle("-fx-padding: 10 12; -fx-border-color: #D7C4B7; -fx-border-radius: 8; -fx-background-radius: 8;");

            PasswordField txtPass = new PasswordField();
            txtPass.setText("admin123");
            txtPass.setPromptText("Mật khẩu");
            txtPass.setStyle("-fx-padding: 10 12; -fx-border-color: #D7C4B7; -fx-border-radius: 8; -fx-background-radius: 8;");

            Label lblErr = new Label();
            lblErr.setWrapText(true);
            lblErr.setStyle("-fx-text-fill: #C62828; -fx-font-weight: 700; -fx-font-size: 12px;");

            Button btnLogin = new Button("ĐĂNG NHẬP VÀO HỆ THỐNG");
            btnLogin.setMaxWidth(Double.MAX_VALUE);
            btnLogin.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-family: 'Segoe UI', 'Inter', sans-serif; -fx-font-weight: 800; -fx-padding: 12; -fx-background-radius: 8; -fx-cursor: hand;");

            btnLogin.setOnAction(e -> {
                String u = txtUser.getText() != null ? txtUser.getText().trim() : "";
                String p = txtPass.getText() != null ? txtPass.getText().trim() : "";

                // 1. Kiểm tra xác thực Database thực tế / InMemory Staff
                StaffModel staff = DatabaseManager.authenticateStaff(u, p);
                if (staff != null && staff.isManager()) {
                    showAdminShell(staff.getFullName());
                    return;
                }

                // 2. Fallback xác thực tài khoản quản trị
                if (("admin".equalsIgnoreCase(u) && "admin123".equals(p)) ||
                    ("admin01".equalsIgnoreCase(u) && "admin123".equals(p))) {
                    showAdminShell(u);
                    return;
                }

                String authErr = userService.authenticateAdmin(u, p);
                if (authErr != null) {
                    lblErr.setText("⚠️ " + authErr);
                } else {
                    showAdminShell(u);
                }
            });

            card.getChildren().addAll(icon, title, sub, new Separator(), txtUser, txtPass, lblErr, btnLogin);
            root.getChildren().add(card);

            Scene scene = new Scene(root, 540, 560);
            primaryStage.setTitle("Đăng Nhập Quản Trị • CoffeePOS");
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.show();
        }
    }
}
