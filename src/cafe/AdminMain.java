package cafe;

import cafe.repositories.mock.*;
import cafe.services.*;
import cafe.views.admin.AdminShellView;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class AdminMain {

    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        cafe.utils.WarningSuppressor.suppress();
        Application.launch(AdminApp.class, args);
    }

    public static class AdminApp extends Application {
        // Mock Repositories
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
        private CategoryService categoryService;
        private MenuService menuService;
        private TableService tableService;
        private DeviceService deviceService;
        private OrderService orderService;
        private PaymentService paymentService;
        private ReportService reportService;
        private AuditLogService auditLogService;

        private Stage primaryStage;

        @Override
        public void start(Stage primaryStage) {
            this.primaryStage = primaryStage;

            // Wire dependencies with cross-module repositories
            auditLogService = new AuditLogService(auditRepo);
            userService = new AdminUserService(userRepo, auditRepo);
            categoryService = new CategoryService(categoryRepo, menuRepo, auditRepo);
            menuService = new MenuService(menuRepo, categoryRepo, auditRepo);
            tableService = new TableService(tableRepo, deviceRepo, orderRepo, auditRepo);
            deviceService = new DeviceService(deviceRepo, tableRepo, auditRepo);
            orderService = new OrderService(orderRepo, paymentRepo, tableRepo, auditRepo);
            paymentService = new PaymentService(paymentRepo, orderRepo, auditRepo);
            reportService = new ReportService(orderRepo, paymentRepo, menuRepo);

            showLoginScreen();
        }

        public void showAdminShell(String username) {
            AdminShellView shellView = new AdminShellView(
                    userService,
                    categoryService,
                    menuService,
                    tableService,
                    deviceService,
                    orderService,
                    paymentService,
                    reportService,
                    auditLogService,
                    username,
                    this::showLoginScreen
            );

            Scene scene = new Scene(shellView, 1280, 800);
            try {
                scene.getStylesheets().add(getClass().getResource("/cafe/views/admin/admin.css").toExternalForm());
            } catch (Exception ignored) {}

            primaryStage.setTitle("Hệ Thống Quản Trị • L'Amour Artisan Cafe");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1100);
            primaryStage.setMinHeight(700);
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
            title.setStyle("-fx-font-size: 19px; -fx-font-weight: 800; -fx-text-fill: #2C1810;");

            Label sub = new Label("L'Amour Artisan Cafe Management");
            sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #8D7B74;");

            TextField txtUser = new TextField("admin01");
            txtUser.setPromptText("Tên đăng nhập (admin01)");
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
            btnLogin.setStyle("-fx-background-color: #3E2723; -fx-text-fill: #FFFFFF; -fx-font-weight: 800; -fx-padding: 11; -fx-background-radius: 8; -fx-cursor: hand;");

            btnLogin.setOnAction(e -> {
                String u = txtUser.getText() != null ? txtUser.getText().trim() : "";
                String p = txtPass.getText() != null ? txtPass.getText().trim() : "";
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
            primaryStage.setTitle("Đăng Nhập Quản Trị • L'Amour Artisan Cafe");
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.show();
        }
    }
}
