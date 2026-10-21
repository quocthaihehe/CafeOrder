package cafe.views.admin;

import cafe.services.*;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import java.text.SimpleDateFormat;
import java.util.*;

public class AdminShellView extends BorderPane {
    private final AdminUserService userService;
    private final CategoryService categoryService;
    private final MenuService menuService;
    private final TableService tableService;
    private final DeviceService deviceService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final ReportService reportService;
    private final AuditLogService auditLogService;

    private final String currentLoggedInAdmin;
    private final Runnable onLogout;

    private final StackPane contentArea = new StackPane();
    private final List<Button> navButtons = new ArrayList<>();
    private final Label lblLiveClock = new Label();

    public AdminShellView(
            AdminUserService userService,
            CategoryService categoryService,
            MenuService menuService,
            TableService tableService,
            DeviceService deviceService,
            OrderService orderService,
            PaymentService paymentService,
            ReportService reportService,
            AuditLogService auditLogService,
            String currentLoggedInAdmin,
            Runnable onLogout
    ) {
        this.userService = userService;
        this.categoryService = categoryService;
        this.menuService = menuService;
        this.tableService = tableService;
        this.deviceService = deviceService;
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.reportService = reportService;
        this.auditLogService = auditLogService;
        this.currentLoggedInAdmin = currentLoggedInAdmin;
        this.onLogout = onLogout;

        initUI();
        startClock();
        showDashboard(); // Default view
    }

    private void initUI() {
        setTop(buildTopBar());
        setLeft(buildSidebar());

        contentArea.getStyleClass().add("admin-content-area");
        setCenter(contentArea);
    }

    private HBox buildTopBar() {
        HBox topBar = new HBox(16);
        topBar.getStyleClass().add("admin-topbar");
        topBar.setAlignment(Pos.CENTER_LEFT);

        // Brand
        HBox brand = new HBox(10);
        brand.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("☕");
        icon.getStyleClass().add("brand-icon-admin");

        VBox titleBox = new VBox(1);
        Label title = new Label("L'Amour Artisan Cafe");
        title.getStyleClass().add("brand-title-admin");
        Label sub = new Label("Hệ Thống Quản Trị Trung Tâm (Executive Admin Portal)");
        sub.setStyle("-fx-font-size: 11px; -fx-text-fill: #8D7B74; -fx-font-weight: 500;");
        titleBox.getChildren().addAll(title, sub);

        brand.getChildren().addAll(icon, titleBox);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Clock
        lblLiveClock.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 600; -fx-text-fill: #7D6B63;");

        // User badge
        HBox userBox = new HBox(8);
        userBox.getStyleClass().add("topbar-user-box");
        Label lblUser = new Label("👤 " + currentLoggedInAdmin);
        lblUser.setStyle("-fx-font-weight: 700; -fx-font-size: 12.5px; -fx-text-fill: #2C1810;");

        Label badge = new Label("ADMIN");
        badge.getStyleClass().add("brand-badge-role");

        userBox.getChildren().addAll(lblUser, badge);

        topBar.getChildren().addAll(brand, spacer, lblLiveClock, userBox);
        return topBar;
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(4);
        sidebar.getStyleClass().add("admin-sidebar");

        Label lblMain = new Label("CHÍNH & ĐIỀU HÀNH");
        lblMain.getStyleClass().add("sidebar-section-title");

        Button btnDash = createNavButton("📊  Tổng quan (Dashboard)", this::showDashboard);
        Button btnOrders = createNavButton("📋  Đơn hàng (Orders)", this::showOrders);
        Button btnPayments = createNavButton("💳  Thanh toán (Payments)", this::showPayments);

        Label lblCatalog = new Label("THỰC ĐƠN & VẬN HÀNH");
        lblCatalog.getStyleClass().add("sidebar-section-title");

        Button btnMenu = createNavButton("☕  Thực đơn (Menu)", this::showMenu);
        Button btnCats = createNavButton("📂  Danh mục (Categories)", this::showCategories);
        Button btnTables = createNavButton("🪑  Sơ đồ bàn (Tables)", this::showTables);
        Button btnDevices = createNavButton("📱  Thiết bị (Devices)", this::showDevices);

        Label lblSystem = new Label("QUẢN TRỊ HỆ THỐNG");
        lblSystem.getStyleClass().add("sidebar-section-title");

        Button btnUsers = createNavButton("👥  Người dùng (Users)", this::showUsers);
        Button btnReports = createNavButton("📈  Báo cáo (Reports)", this::showReports);
        Button btnAudit = createNavButton("🛡️  Kiểm toán (Audit Logs)", this::showAuditLogs);
        Button btnSettings = createNavButton("⚙️  Cài đặt (Settings)", this::showSettings);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button btnLogout = new Button("🚪  Đăng xuất");
        btnLogout.getStyleClass().add("sidebar-btn-logout");
        btnLogout.setMaxWidth(Double.MAX_VALUE);
        btnLogout.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Xác Nhận Đăng Xuất");
            alert.setHeaderText("Bạn có chắc chắn muốn đăng xuất khỏi trang Quản trị?");
            alert.setContentText("Phiên làm việc hiện tại của tài khoản '" + currentLoggedInAdmin + "' sẽ kết thúc.");

            Optional<ButtonType> res = alert.showAndWait();
            if (res.isPresent() && res.get() == ButtonType.OK) {
                if (onLogout != null) onLogout.run();
            }
        });

        sidebar.getChildren().addAll(
                lblMain, btnDash, btnOrders, btnPayments,
                lblCatalog, btnMenu, btnCats, btnTables, btnDevices,
                lblSystem, btnUsers, btnReports, btnAudit, btnSettings,
                spacer, new Separator(), btnLogout
        );

        return sidebar;
    }

    private Button createNavButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.getStyleClass().add("sidebar-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> {
            setActiveButton(btn);
            action.run();
        });
        navButtons.add(btn);
        return btn;
    }

    private void setActiveButton(Button activeBtn) {
        for (Button btn : navButtons) {
            btn.getStyleClass().remove("sidebar-btn-active");
        }
        if (!activeBtn.getStyleClass().contains("sidebar-btn-active")) {
            activeBtn.getStyleClass().add("sidebar-btn-active");
        }
    }

    private void switchContent(Node view) {
        contentArea.getChildren().setAll(view);
    }

    public void showDashboard() {
        if (!navButtons.isEmpty()) setActiveButton(navButtons.get(0));
        switchContent(new DashboardView(reportService, orderService, tableService, deviceService));
    }

    public void showOrders() {
        switchContent(new OrderManagementView(orderService, currentLoggedInAdmin));
    }

    public void showPayments() {
        switchContent(new PaymentManagementView(paymentService, currentLoggedInAdmin));
    }

    public void showMenu() {
        switchContent(new MenuManagementView(menuService, categoryService, currentLoggedInAdmin));
    }

    public void showCategories() {
        switchContent(new CategoryManagementView(categoryService, currentLoggedInAdmin));
    }

    public void showTables() {
        switchContent(new TableManagementView(tableService, currentLoggedInAdmin));
    }

    public void showDevices() {
        switchContent(new DeviceManagementView(deviceService, tableService, currentLoggedInAdmin));
    }

    public void showUsers() {
        switchContent(new UserManagementView(userService, currentLoggedInAdmin));
    }

    public void showReports() {
        switchContent(new ReportsView(reportService));
    }

    public void showAuditLogs() {
        switchContent(new AuditLogView(auditLogService));
    }

    public void showSettings() {
        switchContent(new SettingsView(auditLogService, currentLoggedInAdmin));
    }

    private void startClock() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd/MM/yyyy • HH:mm:ss");
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            lblLiveClock.setText(sdf.format(new Date()));
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }
}
