package cafe.views;

import cafe.controllers.ClientController;
import cafe.models.Order;
import cafe.services.CafeServerService;
import cafe.services.ClientSocketService;
import cafe.services.DatabaseManager;
import cafe.utils.AnimationHelper;
import cafe.utils.CurrencyFormatter;
import cafe.utils.SvgIcons;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * AdminShellView - Trung Tâm Quản Trị & Điều Phối Đa Phân Hệ (Sprint 4.2, 4.3, 4.4):
 * - Sidebar 10 phân hệ cố định bên trái (235px) + Nút Đăng xuất trực tiếp.
 * - Tab 1: Tổng quan BI tương tác (Cross-Component Data Sync: Nhấn vào từng cột biểu đồ Ngày/Tháng/Giờ cập nhật đồng bộ 4 thẻ KPI & Top 5 món bán chạy + Nhấn vào dòng Đơn gần đây mở phiếu OrderSuccessDialog).
 * - Tab 2: Gọi món & Bàn (Gắn đầy đủ ClientController vào ClientView nhúng + Bộ chọn Bàn/Mang đi).
 * - Tab 3: Đơn hàng (Enterprise DataGrid full-width + Lọc trạng thái/loại đơn + In phiếu, Hoàn tất, Thu tiền, Hủy đơn).
 * - Tab 4: Màn pha chế KDS (Bộ lọc trạm Barista / Trà & Đá xay / Bánh ngọt + SLA Timer).
 * - Tabs 5..10: Ủy quyền sang AdminSubViews (Thực đơn & BOM, Tồn kho 25 NL, Khuyến mãi Voucher, Nhân sự RBAC, Báo cáo Tài chính CSV, Cài đặt 2 cột).
 * - Hệ thống Floating Toast Notification nổi góc phải trên.
 */
public class AdminShellView extends BorderPane {

    private final CafeServerService serverService;
    private final ServerKDSView embeddedKdsView;

    private final StackPane centerWrapper = new StackPane();
    private final VBox centerContent = new VBox();
    private final HBox toastBanner = new HBox(10);
    private final Label lblToastMessage = new Label();

    private int currentTab = 1; // 1..10

    // KPI Labels on Tab 1
    private final Label lblKpiRevenue = new Label("14.800.000đ");
    private final Label lblKpiOrders = new Label("212 đơn");
    private final Label lblKpiAov = new Label("69.800đ");
    private final Label lblKpiMembers = new Label("44%");
    private final Label lblKpiRevSub = new Label("↑ +14,2% so với kỳ trước");
    private final Label lblKpiOrdSub = new Label("↑ +18 đơn so với trung bình");
    private final Label lblBestsellersSubtitle = new Label("Đồng bộ theo cột đang chọn");
    private final VBox bestsellersRowsBox = new VBox(8);

    // Interactive BI Chart State (Tab 1)
    private String currentChartMode = "7_DAYS"; // "7_DAYS", "MONTHLY", "HOURLY"
    private int selectedBarIndex = 5; // Mặc định chọn Thứ Bảy (cao điểm)

    private final VBox recentOrdersContainer = new VBox(6);
    private final List<Button> navButtons = new ArrayList<>();
    private Runnable onLogoutCallback;
    private Stage parentStage;
    private ClientSocketService clientSocketService;

    // Embedded Counter POS (Tab 2)
    private ClientView posEmbeddedMenuView;
    private ClientController posEmbeddedMenuController;
    private int selectedPosTable = 1;

    public AdminShellView(CafeServerService serverService, ServerKDSView kdsView) {
        this.serverService = serverService != null ? serverService : CafeServerService.getInstance();
        this.embeddedKdsView = kdsView;

        initUI();
        registerLiveListeners();
    }

    public void setOnLogout(Runnable onLogout) {
        this.onLogoutCallback = onLogout;
    }

    public void attachClientContext(Stage stage, ClientSocketService socketService) {
        this.parentStage = stage;
        this.clientSocketService = socketService;
        if (this.clientSocketService != null) {
            this.clientSocketService.requestAdminTelemetry();
            this.clientSocketService.addOrderStatusListener(upd -> Platform.runLater(() -> {
                if (currentTab == 1 || currentTab == 3 || currentTab == 9) switchTab(currentTab);
            }));
            this.clientSocketService.addOrderAckListener(ack -> Platform.runLater(() -> {
                if (currentTab == 1 || currentTab == 3 || currentTab == 9) switchTab(currentTab);
            }));
        }
        ensureEmbeddedCounterPosReady();
    }

    private void ensureEmbeddedCounterPosReady() {
        if (posEmbeddedMenuView == null) {
            posEmbeddedMenuView = new ClientView();
            posEmbeddedMenuView.configureForEmbeddedShell(true);
            ClientSocketService sock = clientSocketService != null ? clientSocketService : new ClientSocketService();
            if (!sock.isConnected()) {
                sock.connectStaff(selectedPosTable, "admin");
            }
            Stage st = parentStage != null ? parentStage : new Stage();
            posEmbeddedMenuController = new ClientController(st, posEmbeddedMenuView, sock, selectedPosTable, () -> switchTab(1));
        } else if (clientSocketService != null && !clientSocketService.isConnected()) {
            clientSocketService.connectStaff(selectedPosTable, "admin");
        }
    }

    private void initUI() {
        setStyle("-fx-background-color: #F5F0E8;");

        // 1. LEFT SIDEBAR (235px, 10 Phân hệ)
        VBox sidebar = buildSidebar();
        setLeft(sidebar);

        // 2. CENTER CONTENT + FLOATING TOAST OVERLAY
        centerContent.setPadding(new Insets(16, 24, 16, 24));
        centerContent.setStyle("-fx-background-color: #F5F0E8;");

        setupFloatingToast();
        centerWrapper.getChildren().addAll(centerContent, toastBanner);
        StackPane.setAlignment(toastBanner, Pos.TOP_RIGHT);
        StackPane.setMargin(toastBanner, new Insets(18, 28, 0, 0));

        setCenter(centerWrapper);

        // Hiển thị Tab 1 mặc định
        switchTab(1);
    }

    private void setupFloatingToast() {
        toastBanner.setAlignment(Pos.CENTER_LEFT);
        toastBanner.setPadding(new Insets(10, 18, 10, 18));
        toastBanner.setMaxWidth(480);
        toastBanner.setMaxHeight(46);
        toastBanner.setStyle(
            "-fx-background-color: #112823; -fx-background-radius: 10px; " +
            "-fx-border-color: #C89B3C; -fx-border-radius: 10px; -fx-border-width: 1.2px; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 18, 0.15, 0, 5);"
        );
        Label icon = new Label("✓");
        icon.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: #FDE68A;");
        lblToastMessage.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #FFFFFF;");
        toastBanner.getChildren().addAll(icon, lblToastMessage);
        toastBanner.setVisible(false);
        toastBanner.setManaged(false);
    }

    public void showToastNotification(String msg) {
        Platform.runLater(() -> {
            lblToastMessage.setText(msg);
            toastBanner.setVisible(true);
            toastBanner.toFront();
            PauseTransition delay = new PauseTransition(Duration.seconds(3.5));
            delay.setOnFinished(e -> toastBanner.setVisible(false));
            delay.play();
        });
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(8);
        sidebar.setPrefWidth(235);
        sidebar.setMinWidth(225);
        sidebar.setMaxWidth(250);
        sidebar.setPadding(new Insets(18, 12, 16, 12));
        sidebar.setStyle("-fx-background-color: #F7F2EA; -fx-border-color: transparent #DED7CD transparent transparent; -fx-border-width: 0 1 0 0;");

        // Brand Logo
        VBox brandBox = new VBox(2);
        brandBox.setAlignment(Pos.CENTER_LEFT);
        brandBox.setPadding(new Insets(2, 8, 10, 8));

        Label lblBrand = new Label("Brew & Bean");
        lblBrand.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        Label lblSub = new Label("ENTERPRISE POS • ADMIN");
        lblSub.setStyle("-fx-font-size: 9.5px; -fx-font-weight: 800; -fx-text-fill: #563521; -fx-letter-spacing: 1.4px;");
        brandBox.getChildren().addAll(lblBrand, lblSub);

        // 10 Phân Hệ Quản Trị
        VBox navList = new VBox(4);
        navButtons.clear();

        navButtons.add(createSidebarBtn(1, "Tổng quan BI", "tong-quan.png", SvgIcons.TABLE, "📊"));
        navButtons.add(createSidebarBtn(2, "Gọi món & Bàn", "goi-mon.png", SvgIcons.SHOPPING_BAG, "🛒"));
        navButtons.add(createSidebarBtn(3, "Đơn hàng", "don-hang.png", SvgIcons.ORDERS, "🧾"));
        navButtons.add(createSidebarBtn(4, "Bếp & Bar (KDS)", "man-pha-che.png", SvgIcons.COFFEE, "☕"));
        navButtons.add(createSidebarBtn(5, "Thực đơn & Giá", "thuc-don-gia.png", SvgIcons.MENU, "🏷️"));
        navButtons.add(createSidebarBtn(6, "Kho & Định lượng", "kho.png", SvgIcons.SETTINGS, "📦"));
        navButtons.add(createSidebarBtn(7, "Khuyến mãi", "mon-va-gia.png", SvgIcons.MENU, "🎁"));
        navButtons.add(createSidebarBtn(8, "Nhân sự & Phân quyền", "nhan-vien.png", SvgIcons.USER, "👥"));
        navButtons.add(createSidebarBtn(9, "Báo cáo Tài chính", "bao-cao.png", SvgIcons.ORDERS, "📈"));
        navButtons.add(createSidebarBtn(10, "Cài đặt hệ thống", "cai-dat.png", SvgIcons.SETTINGS, "⚙️"));
        navButtons.add(createSidebarBtn(11, "Hạ tầng & Mạng", "ha-tang-mang.png", SvgIcons.SETTINGS, "🌐"));

        navList.getChildren().addAll(navButtons);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Bottom Manager Profile Card + Quick Logout
        VBox profileCard = new VBox(8);
        profileCard.setPadding(new Insets(10, 10, 10, 10));
        profileCard.setStyle("-fx-background-color: #EFE8DD; -fx-background-radius: 10px;");

        HBox profInfoRow = new HBox(8);
        profInfoRow.setAlignment(Pos.CENTER_LEFT);

        Circle avatar = new Circle(15, Color.web("#112823"));
        Label lblAvaText = new Label("NL");
        lblAvaText.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 800; -fx-text-fill: #FFFFFF;");
        StackPane avaPane = new StackPane(avatar, lblAvaText);

        VBox profTexts = new VBox(1);
        HBox.setHgrow(profTexts, Priority.ALWAYS);
        Label lblName = new Label("Nguyễn Minh Luân");
        lblName.setStyle("-fx-font-family: 'Segoe UI', 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        Label lblRole = new Label("Quản lý · Toàn quyền");
        lblRole.setStyle("-fx-font-family: 'Segoe UI', 'Inter', sans-serif; -fx-font-size: 10.5px; -fx-font-weight: 600; -fx-text-fill: #6F695F;");
        profTexts.getChildren().addAll(lblName, lblRole);

        profInfoRow.getChildren().addAll(avaPane, profTexts);
        profileCard.getChildren().add(profInfoRow);

        // Nút Đăng xuất Quản trị chuyên biệt ở chân Sidebar
        Button btnAdminLogout = new Button();
        btnAdminLogout.setMaxWidth(Double.MAX_VALUE);
        btnAdminLogout.setAlignment(Pos.CENTER);
        btnAdminLogout.setPrefHeight(38);
        btnAdminLogout.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #B91C1C; -fx-border-color: #FECACA; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 700; -fx-cursor: hand; -fx-padding: 8 14 8 14;");
        btnAdminLogout.setOnMouseEntered(e -> btnAdminLogout.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-border-color: #F87171; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 700; -fx-cursor: hand; -fx-padding: 8 14 8 14;"));
        btnAdminLogout.setOnMouseExited(e -> btnAdminLogout.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #B91C1C; -fx-border-color: #FECACA; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 700; -fx-cursor: hand; -fx-padding: 8 14 8 14;"));
        AnimationHelper.applyButtonPress(btnAdminLogout);

        HBox adminLogoutContent = new HBox(8);
        adminLogoutContent.setAlignment(Pos.CENTER);
        SVGPath adminLogoutIcon = SvgIcons.createOutlineIcon(SvgIcons.LOG_OUT, 0.75, Color.web("#B91C1C"));
        Label lblAdminLogout = new Label("Đăng xuất Quản trị");
        lblAdminLogout.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #B91C1C;");
        adminLogoutContent.getChildren().addAll(adminLogoutIcon, lblAdminLogout);
        btnAdminLogout.setGraphic(adminLogoutContent);
        btnAdminLogout.setOnAction(e -> {
            if (onLogoutCallback != null) onLogoutCallback.run();
        });

        sidebar.getChildren().addAll(brandBox, navList, spacer, profileCard, btnAdminLogout);
        return sidebar;
    }

    private Button createSidebarBtn(int tabIndex, String labelText, String iconFileName, String svgFallback, String fallbackEmoji) {
        Button btn = new Button();
        btn.setFocusTraversable(false);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(8, 12, 8, 12));
        AnimationHelper.applyButtonPress(btn);

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        StackPane iconContainer = new StackPane();
        iconContainer.setPrefSize(26, 26);
        iconContainer.setMinSize(26, 26);
        iconContainer.setMaxSize(26, 26);
        iconContainer.setAlignment(Pos.CENTER);

        Node iconNode = getIconNode(iconFileName, svgFallback, fallbackEmoji, tabIndex == currentTab);
        iconContainer.getChildren().add(iconNode);

        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 600; -fx-text-fill: #3F3931;");

        row.getChildren().addAll(iconContainer, label);
        btn.setGraphic(row);

        btn.setOnAction(e -> switchTab(tabIndex));
        updateSidebarBtnStyle(btn, tabIndex == currentTab);
        return btn;
    }

    private Node getIconNode(String iconFileName, String svgFallback, String fallbackEmoji, boolean isActive) {
        if (iconFileName != null && !iconFileName.isEmpty()) {
            String[] resourcePaths = {
                "/cafe/resources/images/icons/admin/" + iconFileName,
                "/resources/images/icons/admin/" + iconFileName,
                "/resources/images/" + iconFileName,
                "/cafe/resources/images/" + iconFileName
            };
            for (String p : resourcePaths) {
                try {
                    URL url = getClass().getResource(p);
                    if (url != null) {
                        Image img = new Image(url.toExternalForm());
                        if (!img.isError() && img.getWidth() > 0) {
                            ImageView iv = new ImageView(img);
                            iv.setFitWidth(18);
                            iv.setFitHeight(18);
                            iv.setPreserveRatio(true);
                            iv.setSmooth(true);
                            iv.setOpacity(isActive ? 1.0 : 0.85);
                            return iv;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        if (svgFallback != null && !svgFallback.isEmpty()) {
            Color color = isActive ? Color.web("#112823") : Color.web("#3F3931");
            return SvgIcons.createOutlineIcon(svgFallback, 0.78, color);
        }
        Label lbl = new Label(fallbackEmoji != null ? fallbackEmoji : "•");
        lbl.setStyle("-fx-font-size: 14px;");
        return lbl;
    }

    private void applySidebarIconColor(Node n, boolean isActive) {
        if (n instanceof SVGPath) {
            ((SVGPath) n).setStroke(isActive ? Color.WHITE : Color.web("#3F3931"));
        } else if (n instanceof ImageView) {
            n.setOpacity(isActive ? 1.0 : 0.85);
            if (isActive) {
                javafx.scene.effect.ColorAdjust white = new javafx.scene.effect.ColorAdjust();
                white.setBrightness(1.0);
                n.setEffect(white);
            } else {
                n.setEffect(null);
            }
        }
    }

    private void updateSidebarBtnStyle(Button btn, boolean isActive) {
        btn.setStyle(isActive
            ? "-fx-background-color: #112823; -fx-background-radius: 8px; -fx-cursor: hand;"
            : "-fx-background-color: transparent; -fx-background-radius: 8px; -fx-cursor: hand;");
        if (btn.getGraphic() instanceof HBox) {
            HBox box = (HBox) btn.getGraphic();
            if (box.getChildren().size() > 0 && box.getChildren().get(0) instanceof StackPane) {
                StackPane iconBox = (StackPane) box.getChildren().get(0);
                if (!iconBox.getChildren().isEmpty()) {
                    applySidebarIconColor(iconBox.getChildren().get(0), isActive);
                }
            }
            if (box.getChildren().size() > 1 && box.getChildren().get(1) instanceof Label) {
                ((Label) box.getChildren().get(1)).setStyle(isActive
                    ? "-fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #FFFFFF;"
                    : "-fx-font-size: 13.5px; -fx-font-weight: 600; -fx-text-fill: #3F3931;");
            }
        }
        btn.setOnMouseEntered(e -> {
            if (navButtons.indexOf(btn) + 1 != currentTab) {
                btn.setStyle("-fx-background-color: #EFE8DD; -fx-background-radius: 8px; -fx-cursor: hand;");
            }
        });
        btn.setOnMouseExited(e -> {
            if (navButtons.indexOf(btn) + 1 != currentTab) {
                btn.setStyle("-fx-background-color: transparent; -fx-background-radius: 8px; -fx-cursor: hand;");
            }
        });
    }

    public void switchTab(int tabIndex) {
        this.currentTab = tabIndex;
        for (int i = 0; i < navButtons.size(); i++) {
            updateSidebarBtnStyle(navButtons.get(i), (i + 1) == currentTab);
        }

        centerContent.getChildren().clear();
        Stage st = parentStage != null ? parentStage : (getScene() != null && getScene().getWindow() instanceof Stage ? (Stage) getScene().getWindow() : null);

        switch (tabIndex) {
            case 1:
                renderDashboardTab();
                break;
            case 2:
                renderCounterPosTab();
                break;
            case 3:
                renderOrderHistoryTab();
                break;
            case 4:
                renderKdsTab();
                break;
            case 5:
                centerContent.getChildren().add(AdminSubViews.styleInputs(AdminSubViews.buildMenuAndBomView(st, this::showToastNotification, () -> {
                    if (posEmbeddedMenuController != null) posEmbeddedMenuController.refreshCatalog();
                })));
                break;
            case 6:
                centerContent.getChildren().add(AdminSubViews.styleInputs(AdminSubViews.buildInventoryView(st, this::showToastNotification)));
                break;
            case 7:
                centerContent.getChildren().add(AdminSubViews.styleInputs(AdminSubViews.buildPromotionsView(st, this::showToastNotification)));
                break;
            case 8:
                centerContent.getChildren().add(AdminSubViews.styleInputs(AdminSubViews.buildStaffRbacView(st, this::showToastNotification)));
                break;
            case 9:
                centerContent.getChildren().add(AdminSubViews.styleInputs(AdminSubViews.buildFinancialReportsView(st, this::showToastNotification)));
                break;
            case 10:
                centerContent.getChildren().add(AdminSubViews.styleInputs(AdminSubViews.buildSystemSettingsView(st, this::showToastNotification, () -> {
                    if (posEmbeddedMenuController != null) posEmbeddedMenuController.refreshCatalog();
                }, onLogoutCallback)));
                break;
            case 11:
                centerContent.getChildren().add(AdminSubViews.styleInputs(AdminSubViews.buildNetworkTelemetryView(st, this::showToastNotification)));
                break;
            default:
                renderDashboardTab();
                break;
        }
    }

    // =========================================================================
    // TAB 1: INTERACTIVE BI DASHBOARD (CROSS-COMPONENT DATA SYNC)
    // =========================================================================
    private void renderDashboardTab() {
        centerContent.getChildren().clear();
        centerContent.setSpacing(14);

        // 1. Header Row
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label lblTitle = new Label("Tổng Quan Kinh Doanh & Phân Tích BI Tương Tác");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        String todayStr = new SimpleDateFormat("EEEE dd/MM/yyyy", new Locale("vi", "VN")).format(new Date());
        Label lblSub = new Label(DatabaseManager.brandName + " · " + todayStr + " · Nhấn vào từng cột biểu đồ để xem chi tiết KPI & Top món bán chạy của kỳ đó");
        lblSub.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #78716C;");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnGoKds = new Button("☕ Màn pha chế KDS (" + serverService.getActiveOrders().size() + ")");
        btnGoKds.setStyle("-fx-background-color: #F5EFEB; -fx-text-fill: #112823; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 38px; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
        btnGoKds.setOnAction(e -> switchTab(4));

        Button btnCreate = new Button("+ Tạo đơn tại quầy");
        btnCreate.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 38px; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
        AnimationHelper.applyButtonPress(btnCreate);
        btnCreate.setOnAction(e -> switchTab(2));

        headerRow.getChildren().addAll(titleBox, sp, btnGoKds, btnCreate);

        // 2. 4 Thẻ KPI Sống (Đồng bộ khi click cột biểu đồ)
        HBox kpiGrid = new HBox(12);
        kpiGrid.setAlignment(Pos.CENTER);
        kpiGrid.getChildren().addAll(
            createKpiCard("DOANH THU KỲ ĐANG CHỌN", lblKpiRevenue, lblKpiRevSub),
            createKpiCard("TỔNG SỐ ĐƠN HÀNG", lblKpiOrders, lblKpiOrdSub),
            createKpiCard("GIÁ TRỊ TRUNG BÌNH / ĐƠN (AOV)", lblKpiAov, new Label("↑ +4,2% khách gọi thêm bánh")),
            createKpiCard("TỶ LỆ KHÁCH THÀNH VIÊN & VOUCHER", lblKpiMembers, new Label("↑ Áp dụng GIAM10 / VIP20"))
        );

        // 3. Biểu đồ BI tương tác (Left) & Top 5 Món bán chạy (Right)
        HBox chartsRow = new HBox(14);
        chartsRow.setAlignment(Pos.TOP_LEFT);

        VBox trendBox = buildInteractiveBiChartBox();
        HBox.setHgrow(trendBox, Priority.ALWAYS);

        VBox bestsellersBox = buildBestsellersBox();
        bestsellersBox.setPrefWidth(335);
        bestsellersBox.setMinWidth(315);

        chartsRow.getChildren().addAll(trendBox, bestsellersBox);

        // 4. Bảng Đơn gần đây (Click từng dòng để mở Phiếu Thanh Toán)
        VBox recentOrdersBox = buildRecentOrdersBox();
        VBox.setVgrow(recentOrdersBox, Priority.ALWAYS);

        centerContent.getChildren().addAll(headerRow, kpiGrid, chartsRow, recentOrdersBox);
        syncDashboardDataWithSelectedBar();
    }

    private VBox createKpiCard(String title, Label valLabel, Label subLabel) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(13, 16, 13, 16));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10px; -fx-border-color: #E7E2DA; -fx-border-radius: 10px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.03), 8, 0.1, 0, 2);");
        HBox.setHgrow(card, Priority.ALWAYS);

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #78716C; -fx-letter-spacing: 0.5px;");

        valLabel.setStyle("-fx-font-family: 'Segoe UI', 'Inter', -apple-system, sans-serif; -fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        subLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #15803D;");

        card.getChildren().addAll(lblTitle, valLabel, subLabel);
        return card;
    }

    private VBox buildInteractiveBiChartBox() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(14, 18, 14, 18));
        box.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10px; -fx-border-color: #E7E2DA; -fx-border-radius: 10px;");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox titleCol = new VBox(2);
        Label lblTitle = new Label("Biểu Đồ Doanh Thu Đa Chiều (Interactive BI)");
        lblTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        Label lblHint = new Label("Nhấn vào bất kỳ cột nào để lọc 4 thẻ KPI và Top món bán chạy tương ứng");
        lblHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #78716C;");
        titleCol.getChildren().addAll(lblTitle, lblHint);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        HBox modeSwitch = new HBox(6);
        Button btn7Days = createModeBtn("7 ngày qua", "7_DAYS");
        Button btnMonthly = createModeBtn("Theo tháng (T1-T12)", "MONTHLY");
        Button btnHourly = createModeBtn("Theo khung giờ", "HOURLY");
        modeSwitch.getChildren().addAll(btn7Days, btnMonthly, btnHourly);

        header.getChildren().addAll(titleCol, sp, modeSwitch);

        HBox barChart = new HBox(12);
        barChart.setAlignment(Pos.BOTTOM_CENTER);
        barChart.setPrefHeight(138);
        barChart.setPadding(new Insets(12, 4, 4, 4));

        String[] labels = getChartLabels(currentChartMode);
        double[] revenues = getChartRevenues(currentChartMode);
        int[] orders = getChartOrders(currentChartMode);

        double maxRev = 1;
        for (double r : revenues) if (r > maxRev) maxRev = r;

        if (selectedBarIndex >= labels.length) selectedBarIndex = labels.length - 1;

        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            boolean isSelected = (idx == selectedBarIndex);

            VBox col = new VBox(4);
            col.setAlignment(Pos.BOTTOM_CENTER);
            HBox.setHgrow(col, Priority.ALWAYS);
            col.setStyle("-fx-cursor: hand;");

            Label lblTopVal = new Label(formatShortMoney(revenues[i]));
            lblTopVal.setStyle("-fx-font-size: 10px; -fx-font-weight: " + (isSelected ? "800" : "600") + "; -fx-text-fill: " + (isSelected ? "#112823" : "#78716C") + ";");

            double barH = Math.max(18, (revenues[i] / maxRev) * 92.0);
            Region bar = new Region();
            bar.setPrefWidth(labels.length > 8 ? 22 : 34);
            bar.setMaxWidth(42);
            bar.setPrefHeight(barH);
            bar.setStyle(
                "-fx-background-color: " + (isSelected ? "#112823" : "#D6CEC2") + "; " +
                "-fx-background-radius: 6px 6px 2px 2px;"
            );

            Label lblAxis = new Label(labels[i]);
            lblAxis.setStyle("-fx-font-size: 10.5px; -fx-font-weight: " + (isSelected ? "800" : "600") + "; -fx-text-fill: " + (isSelected ? "#112823" : "#57534E") + ";");

            Tooltip tp = new Tooltip(labels[i] + "\nDoanh thu: " + CurrencyFormatter.formatVND(revenues[i]) + "\nSố đơn: " + orders[i] + " đơn");
            Tooltip.install(col, tp);

            col.setOnMouseClicked(e -> {
                selectedBarIndex = idx;
                renderDashboardTab();
            });

            col.getChildren().addAll(lblTopVal, bar, lblAxis);
            barChart.getChildren().add(col);
        }

        box.getChildren().addAll(header, barChart);
        return box;
    }

    private Button createModeBtn(String text, String modeCode) {
        boolean active = modeCode.equals(currentChartMode);
        Button btn = new Button(text);
        btn.setStyle(
            "-fx-background-color: " + (active ? "#112823" : "#F5EFEB") + "; " +
            "-fx-text-fill: " + (active ? "#FFFFFF" : "#57534E") + "; " +
            "-fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; " +
            "-fx-background-radius: 8px; -fx-padding: 6 12 6 12; -fx-pref-height: 34px; -fx-cursor: hand;"
        );
        btn.setOnAction(e -> {
            currentChartMode = modeCode;
            selectedBarIndex = modeCode.equals("7_DAYS") ? 5 : (modeCode.equals("MONTHLY") ? 9 : 0);
            renderDashboardTab();
        });
        return btn;
    }

    private String formatShortMoney(double amount) {
        if (amount >= 1_000_000) {
            return String.format(Locale.US, "%.1ftr", amount / 1_000_000.0);
        }
        return String.format(Locale.US, "%.0fk", amount / 1_000.0);
    }

    private String[] getChartLabels(String mode) {
        if ("MONTHLY".equals(mode)) {
            return new String[]{"T1", "T2", "T3", "T4", "T5", "T6", "T7", "T8", "T9", "T10", "T11", "T12"};
        } else if ("HOURLY".equals(mode)) {
            return new String[]{"07-09h", "09-11h", "11-13h", "13-15h", "15-17h", "17-19h", "19-22h"};
        }
        return new String[]{"T2 (13/10)", "T3 (14/10)", "T4 (15/10)", "T5 (16/10)", "T6 (17/10)", "T7 (18/10)", "CN (19/10)"};
    }

    private double[] getChartRevenues(String mode) {
        if ("MONTHLY".equals(mode)) {
            return new double[]{195_000_000, 210_000_000, 204_000_000, 218_000_000, 232_000_000, 240_000_000, 242_500_000, 256_800_000, 251_200_000, 262_700_000, 248_000_000, 275_000_000};
        } else if ("HOURLY".equals(mode)) {
            return new double[]{3_450_000, 2_180_000, 2_690_000, 1_920_000, 2_480_000, 1_850_000, 3_120_000};
        }
        return new double[]{7_850_000, 8_240_000, 8_890_000, 8_620_000, 11_250_000, 14_800_000, 13_650_000};
    }

    private int[] getChartOrders(String mode) {
        if ("MONTHLY".equals(mode)) {
            return new int[]{2910, 3120, 3040, 3250, 3410, 3550, 3620, 3810, 3740, 3892, 3680, 4050};
        } else if ("HOURLY".equals(mode)) {
            return new int[]{52, 34, 41, 29, 36, 27, 46};
        }
        return new int[]{118, 124, 132, 129, 164, 212, 198};
    }

    private void syncDashboardDataWithSelectedBar() {
        String[] labels = getChartLabels(currentChartMode);
        double[] revs = getChartRevenues(currentChartMode);
        int[] ords = getChartOrders(currentChartMode);

        int idx = Math.min(selectedBarIndex, labels.length - 1);
        double rev = revs[idx];
        int ordCount = ords[idx];
        double aov = ordCount > 0 ? rev / ordCount : 68000;

        lblKpiRevenue.setText(CurrencyFormatter.formatVND(rev));
        lblKpiOrders.setText(ordCount + " đơn");
        lblKpiAov.setText(CurrencyFormatter.formatVND(aov));
        lblKpiMembers.setText((38 + (idx * 3) % 15) + "%");
        lblKpiRevSub.setText("● Đang xem kỳ: " + labels[idx]);
        lblKpiOrdSub.setText("↑ Tỷ lệ hoàn tất 98,4%");

        lblBestsellersSubtitle.setText("Kỳ đang chọn: " + labels[idx]);
        bestsellersRowsBox.getChildren().clear();

        int scale = Math.max(1, ordCount / 30);
        if (idx % 3 == 0) {
            bestsellersRowsBox.getChildren().addAll(
                createBestsellerRow("1. Cà phê Sữa Đá Đặc Biệt", (18 * scale) + " ly", "#112823"),
                createBestsellerRow("2. Bạc Xỉu Cốt Dừa", (14 * scale) + " ly", "#8C5835"),
                createBestsellerRow("3. Trà Đào Cam Sả", (12 * scale) + " ly", "#D97706"),
                createBestsellerRow("4. Bánh Croissant Bơ Pháp", (9 * scale) + " phần", "#4B704F"),
                createBestsellerRow("5. Matcha Latte Nhật Bản", (8 * scale) + " ly", "#15803D")
            );
        } else if (idx % 3 == 1) {
            bestsellersRowsBox.getChildren().addAll(
                createBestsellerRow("1. Trà Đào Cam Sả", (19 * scale) + " ly", "#D97706"),
                createBestsellerRow("2. Cà phê Sữa Đá", (16 * scale) + " ly", "#112823"),
                createBestsellerRow("3. Trà Sen Vàng Kem Cheese", (13 * scale) + " ly", "#8C5835"),
                createBestsellerRow("4. Matcha Đá Xay", (11 * scale) + " ly", "#15803D"),
                createBestsellerRow("5. Bánh Tiramisu Ý", (8 * scale) + " phần", "#4B704F")
            );
        } else {
            bestsellersRowsBox.getChildren().addAll(
                createBestsellerRow("1. Cold Brew Cam Vàng", (21 * scale) + " ly", "#112823"),
                createBestsellerRow("2. Cà phê Muối Huế", (17 * scale) + " ly", "#8C5835"),
                createBestsellerRow("3. Trà Vải Hoa Hồng", (14 * scale) + " ly", "#D97706"),
                createBestsellerRow("4. Bạc Xỉu Đá", (12 * scale) + " ly", "#4B704F"),
                createBestsellerRow("5. Bánh Phô Mai Cháy", (10 * scale) + " phần", "#15803D")
            );
        }
    }

    private VBox buildBestsellersBox() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(14, 16, 14, 16));
        box.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10px; -fx-border-color: #E7E2DA; -fx-border-radius: 10px;");

        Label lblTitle = new Label("Top 5 Món Bán Chạy Nhất");
        lblTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        lblBestsellersSubtitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #B45309;");

        box.getChildren().addAll(lblTitle, lblBestsellersSubtitle, bestsellersRowsBox);
        return box;
    }

    private HBox createBestsellerRow(String name, String qty, String dotColor) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 6, 4, 6));
        row.setStyle("-fx-background-color: #FAF8F5; -fx-background-radius: 6px;");

        Circle dot = new Circle(4, Color.web(dotColor));
        Label lblName = new Label(name);
        lblName.setStyle("-fx-font-size: 12px; -fx-text-fill: #1C1917; -fx-font-weight: 600;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label lblQty = new Label(qty);
        lblQty.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        row.getChildren().addAll(dot, lblName, sp, lblQty);
        return row;
    }

    private VBox buildRecentOrdersBox() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(14, 16, 14, 16));
        box.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10px; -fx-border-color: #E7E2DA; -fx-border-radius: 10px;");

        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        Label lblTitle = new Label("Đơn Hàng Thời Gian Thực Tại Các Tầng (Nhấn vào từng đơn để xem & in phiếu)");
        lblTitle.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnViewAll = new Button("Quản lý toàn bộ đơn hàng →");
        btnViewAll.setStyle("-fx-background-color: transparent; -fx-text-fill: #112823; -fx-font-size: 11.5px; -fx-font-weight: 700; -fx-cursor: hand;");
        btnViewAll.setOnAction(e -> switchTab(3));
        top.getChildren().addAll(lblTitle, sp, btnViewAll);

        HBox tblHeader = new HBox(10);
        tblHeader.setPadding(new Insets(7, 12, 7, 12));
        tblHeader.setStyle("-fx-background-color: #F4EFE6; -fx-background-radius: 6px;");

        Label colCode = createColHeader("MÃ ĐƠN", 100);
        Label colTable = createColHeader("BÀN / LOẠI PHỤC VỤ", 190);
        Label colItems = createColHeader("CHI TIẾT MÓN", 280);
        HBox.setHgrow(colItems, Priority.ALWAYS);
        Label colTotal = createColHeader("TỔNG THANH TOÁN", 140);
        Label colStatus = createColHeader("TRẠNG THÁI", 120);

        tblHeader.getChildren().addAll(colCode, colTable, colItems, colTotal, colStatus);

        populateRecentOrders();

        ScrollPane scroll = new ScrollPane(recentOrdersContainer);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(165);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        box.getChildren().addAll(top, tblHeader, scroll);
        return box;
    }

    private Label createColHeader(String text, double width) {
        Label lbl = new Label(text);
        lbl.setPrefWidth(width);
        lbl.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 800; -fx-text-fill: #57534E;");
        return lbl;
    }

    private void populateRecentOrders() {
        recentOrdersContainer.getChildren().clear();
        List<Order> list = serverService.getAllOrdersNewestFirst();
        for (Order o : list) {
            recentOrdersContainer.getChildren().add(createInteractiveOrderRow(o));
        }
    }

    private HBox createInteractiveOrderRow(Order order) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 12, 8, 12));
        row.setStyle("-fx-background-color: #FAF8F5; -fx-background-radius: 7px; -fx-border-color: #EFEAE2; -fx-border-radius: 7px; -fx-cursor: hand;");

        Label lblCode = new Label("#" + order.getOrderId());
        lblCode.setPrefWidth(100);
        lblCode.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        String tableText = ("TAKEAWAY".equalsIgnoreCase(order.getOrderType()) || order.getTable() <= 0)
            ? "🛍 Mang đi (Takeaway)"
            : "🍽 Bàn " + String.format("%02d", order.getTable());
        Label lblTable = new Label(tableText);
        lblTable.setPrefWidth(190);
        lblTable.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #57534E;");

        Label lblItems = new Label(order.getTotalQuantity() + " món · " + order.getItemsSummaryText());
        lblItems.setPrefWidth(280);
        lblItems.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(lblItems, Priority.ALWAYS);
        lblItems.setStyle("-fx-font-size: 12px; -fx-text-fill: #1C1917;");

        Label lblTotal = new Label(CurrencyFormatter.formatVND(order.getFinalAmount()));
        lblTotal.setPrefWidth(140);
        lblTotal.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        Label badge = createStatusBadge(order.getStatus());

        row.setOnMouseClicked(e -> {
            Stage st = parentStage != null ? parentStage : (getScene() != null && getScene().getWindow() instanceof Stage ? (Stage) getScene().getWindow() : null);
            OrderSuccessDialog.showReceiptForOrder(st, order, "Tiền mặt / VietQR");
        });

        row.getChildren().addAll(lblCode, lblTable, lblItems, lblTotal, badge);
        return row;
    }

    private Label createStatusBadge(String status) {
        Label badge = new Label();
        badge.setMinWidth(115);
        badge.setPrefWidth(125);
        String st = status != null ? status.toUpperCase() : "PENDING";
        if (st.contains("PREPARING") || st.contains("ĐANG PHA")) {
            badge.setText("● Đang pha chế");
            badge.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #B45309; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 11px; -fx-font-weight: 800; -fx-background-radius: 6px; -fx-padding: 4 8 4 8; -fx-alignment: center;");
        } else if (st.contains("READY") || st.contains("DONE") || st.contains("PAID") || st.contains("HOÀN")) {
            badge.setText("● Đã hoàn tất");
            badge.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 11px; -fx-font-weight: 800; -fx-background-radius: 6px; -fx-padding: 4 8 4 8; -fx-alignment: center;");
        } else if (st.contains("CANCEL")) {
            badge.setText("✕ Đã hủy");
            badge.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 11px; -fx-font-weight: 800; -fx-background-radius: 6px; -fx-padding: 4 8 4 8; -fx-alignment: center;");
        } else {
            badge.setText("● Chờ pha chế");
            badge.setStyle("-fx-background-color: #E0F2FE; -fx-text-fill: #0369A1; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 11px; -fx-font-weight: 800; -fx-background-radius: 6px; -fx-padding: 4 8 4 8; -fx-alignment: center;");
        }
        return badge;
    }

    // =========================================================================
    // TAB 2: GỌI MÓN & BÀN TẠI QUẦY (COUNTER POS WITH CLIENTCONTROLLER)
    // =========================================================================
    private void renderCounterPosTab() {
        centerContent.getChildren().clear();
        centerContent.setSpacing(10);
        ensureEmbeddedCounterPosReady();

        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10, 16, 10, 16));
        topBar.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10px; -fx-border-color: #E6E0D6; -fx-border-radius: 10px;");

        Label lblTitle = new Label("POS Tạo Đơn Trực Tiếp Tại Quầy");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label lblSelect = new Label("Chọn bàn / hình thức phục vụ:");
        lblSelect.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #57534E;");

        ComboBox<String> cboTable = new ComboBox<>();
        cboTable.getItems().add("Mang đi (Takeaway)");
        for (int i = 1; i <= 12; i++) cboTable.getItems().add("Tầng 1 - Bàn " + String.format("%02d", i));
        for (int i = 201; i <= 210; i++) cboTable.getItems().add("Tầng 2 - Bàn " + i);
        for (int i = 301; i <= 308; i++) cboTable.getItems().add("Tầng 3 - Bàn " + i);
        cboTable.getSelectionModel().select(1); // Bàn 01 mặc định
        cboTable.setPrefHeight(36);
        cboTable.setStyle(
            "-fx-background-color: #FAF8F5; -fx-background-radius: 8px; -fx-border-color: #DFD8CE; -fx-border-radius: 8px; " +
            "-fx-border-width: 1px; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 700; " +
            "-fx-text-fill: #112823; -fx-cursor: hand; -fx-padding: 4 8 4 10;"
        );
        cboTable.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: #FAF8F5;");
                } else {
                    setText(item);
                    setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-text-fill: #112823; -fx-padding: 6 12 6 12;");
                }
            }
        });

        cboTable.setOnAction(e -> {
            String val = cboTable.getValue();
            if (val != null && val.contains("Mang đi")) {
                selectedPosTable = 0;
            } else if (val != null) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("Bàn\\s+(\\d+)").matcher(val);
                if (m.find()) {
                    selectedPosTable = Integer.parseInt(m.group(1));
                }
            }
            if (posEmbeddedMenuController != null) {
                posEmbeddedMenuController.setTableNumber(selectedPosTable);
            }
        });

        topBar.getChildren().addAll(lblTitle, sp, lblSelect, cboTable);

        VBox.setVgrow(posEmbeddedMenuView, Priority.ALWAYS);
        centerContent.getChildren().addAll(topBar, posEmbeddedMenuView);
    }

    // =========================================================================
    // TAB 3: QUẢN LÝ & KIỂM TOÁN ĐƠN HÀNG (FULL-WIDTH ENTERPRISE DATAGRID)
    // =========================================================================
    private void renderOrderHistoryTab() {
        centerContent.getChildren().clear();
        centerContent.setSpacing(14);

        final String[] statusFilter = new String[]{"ALL"};
        final String[] keywordFilter = new String[]{""};

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox titleCol = new VBox(2);
        Label lblTitle = new Label("Quản Lý & Kiểm Toán Đơn Hàng Toàn Hệ Thống");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        Label lblSub = new Label("Tra cứu thời gian thực tất cả đơn hàng tại 3 tầng và Mang đi · In lại hóa đơn, xác nhận hoàn tất pha chế hoặc hủy đơn");
        lblSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #78716C;");
        titleCol.getChildren().addAll(lblTitle, lblSub);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        TextField txtSearch = new TextField();
        txtSearch.setPromptText("Tìm mã đơn (#1001), số bàn, tên món...");
        txtSearch.setPrefWidth(260);
        txtSearch.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #DFD8CE; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 7 12 7 12; -fx-font-size: 12px;");

        header.getChildren().addAll(titleCol, sp, txtSearch);

        // Status filter chips
        HBox chipBar = new HBox(8);
        chipBar.setAlignment(Pos.CENTER_LEFT);
        String[][] filters = {
            {"ALL", "Tất cả đơn"},
            {"PENDING", "Chờ pha chế"},
            {"PREPARING", "Đang pha chế"},
            {"READY", "Đã hoàn tất / Đã thu"},
            {"CANCELLED", "Đã hủy"}
        };
        List<Button> fBtns = new ArrayList<>();
        VBox rowsBox = new VBox(8);
        final Runnable[] renderRows = new Runnable[1];

        for (String[] f : filters) {
            Button b = new Button(f[1]);
            b.setOnAction(e -> {
                statusFilter[0] = f[0];
                for (int i = 0; i < fBtns.size(); i++) {
                    boolean act = filters[i][0].equals(statusFilter[0]);
                    fBtns.get(i).setStyle(act
                        ? "-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 34px; -fx-padding: 6 14 6 14; -fx-cursor: hand;"
                        : "-fx-background-color: #F5EFEB; -fx-text-fill: #57534E; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 600; -fx-background-radius: 8px; -fx-pref-height: 34px; -fx-padding: 6 14 6 14; -fx-cursor: hand;");
                }
                if (renderRows[0] != null) renderRows[0].run();
            });
            fBtns.add(b);
            chipBar.getChildren().add(b);
        }

        VBox gridCard = new VBox(10);
        VBox.setVgrow(gridCard, Priority.ALWAYS);
        gridCard.setPadding(new Insets(16));
        gridCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12px; -fx-border-color: #E6E0D6; -fx-border-radius: 12px;");

        HBox tblHead = new HBox(10);
        tblHead.setPadding(new Insets(8, 12, 8, 12));
        tblHead.setStyle("-fx-background-color: #F4EFE6; -fx-background-radius: 8px;");
        Label hItems = createColHeader("CHI TIẾT CÁC MÓN TRONG ĐƠN", 280);
        HBox.setHgrow(hItems, Priority.ALWAYS);
        hItems.setMaxWidth(Double.MAX_VALUE);

        tblHead.getChildren().addAll(
            createColHeader("MÃ ĐƠN", 85),
            createColHeader("THỜI GIAN", 85),
            createColHeader("BÀN / HÌNH THỨC", 145),
            hItems,
            createColHeader("TỔNG TIỀN", 125),
            createColHeader("TRẠNG THÁI", 125),
            createColHeader("THAO TÁC XỬ LÝ", 240)
        );

        ScrollPane scroll = new ScrollPane(rowsBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        gridCard.getChildren().addAll(tblHead, scroll);

        renderRows[0] = () -> {
            rowsBox.getChildren().clear();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            for (Order o : serverService.getAllOrdersNewestFirst()) {
                String st = o.getStatus() != null ? o.getStatus().toUpperCase() : "PENDING";
                if (!"ALL".equals(statusFilter[0])) {
                    if ("READY".equals(statusFilter[0])) {
                        if (!(st.contains("READY") || st.contains("DONE") || st.contains("PAID"))) continue;
                    } else if (!st.contains(statusFilter[0])) {
                        continue;
                    }
                }
                if (!keywordFilter[0].isEmpty()) {
                    String blob = ("#" + o.getOrderId() + " ban " + o.getTable() + " " + o.getItemsSummaryText()).toLowerCase();
                    if (!blob.contains(keywordFilter[0])) continue;
                }

                HBox r = new HBox(10);
                r.setAlignment(Pos.CENTER_LEFT);
                r.setPadding(new Insets(10, 12, 10, 12));
                r.setStyle("-fx-background-color: #FAF8F5; -fx-background-radius: 8px; -fx-border-color: #EFEAE2; -fx-border-radius: 8px;");

                Label lId = new Label("#" + o.getOrderId());
                lId.setPrefWidth(85);
                lId.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #112823;");

                Label lTime = new Label(sdf.format(new Date(o.getCreatedAtEpochMs())));
                lTime.setPrefWidth(85);
                lTime.setStyle("-fx-font-size: 12px; -fx-text-fill: #78716C;");

                String tblStr = (o.getTable() <= 0 || "TAKEAWAY".equalsIgnoreCase(o.getOrderType()))
                    ? "🛍 Mang đi"
                    : "🍽 Bàn " + String.format("%02d", o.getTable());
                Label lTbl = new Label(tblStr);
                lTbl.setPrefWidth(145);
                lTbl.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #1C1917;");

                Label lItems = new Label(o.getItemsSummaryText());
                lItems.setPrefWidth(280);
                lItems.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(lItems, Priority.ALWAYS);
                lItems.setStyle("-fx-font-size: 12px; -fx-text-fill: #57534E;");

                Label lTotal = new Label(CurrencyFormatter.formatVND(o.getFinalAmount()));
                lTotal.setPrefWidth(125);
                lTotal.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #112823;");

                Label badge = createStatusBadge(st);

                HBox actions = new HBox(4);
                actions.setPrefWidth(240);
                actions.setAlignment(Pos.CENTER_LEFT);

                Button btnPrint = new Button("🖨 In vé");
                btnPrint.setStyle("-fx-background-color: #F5EFEB; -fx-text-fill: #112823; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 32px; -fx-padding: 4 8 4 8; -fx-cursor: hand;");
                btnPrint.setOnMouseEntered(e -> btnPrint.setStyle("-fx-background-color: #EAE4DA; -fx-text-fill: #112823; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 32px; -fx-padding: 4 8 4 8; -fx-cursor: hand;"));
                btnPrint.setOnMouseExited(e -> btnPrint.setStyle("-fx-background-color: #F5EFEB; -fx-text-fill: #112823; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 32px; -fx-padding: 4 8 4 8; -fx-cursor: hand;"));
                btnPrint.setOnAction(ev -> {
                    Stage stg = parentStage != null ? parentStage : null;
                    OrderSuccessDialog.showReceiptForOrder(stg, o, "Tiền mặt / VietQR");
                });

                Button btnDone = new Button("✓ Xong");
                btnDone.setStyle("-fx-background-color: #F5EFEB; -fx-text-fill: #112823; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 32px; -fx-padding: 4 8 4 8; -fx-cursor: hand;");
                btnDone.setOnMouseEntered(e -> btnDone.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 32px; -fx-padding: 4 8 4 8; -fx-cursor: hand;"));
                btnDone.setOnMouseExited(e -> btnDone.setStyle("-fx-background-color: #F5EFEB; -fx-text-fill: #112823; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 32px; -fx-padding: 4 8 4 8; -fx-cursor: hand;"));
                btnDone.setOnAction(ev -> {
                    serverService.updateOrderStatus(o.getOrderId(), "READY");
                    renderRows[0].run();
                    showToastNotification("Đã hoàn tất & trừ kho tự động cho đơn #" + o.getOrderId());
                });

                Button btnCancel = new Button("✕ Hủy");
                btnCancel.setStyle("-fx-background-color: #F5EFEB; -fx-text-fill: #57534E; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 32px; -fx-padding: 4 8 4 8; -fx-cursor: hand;");
                btnCancel.setOnMouseEntered(e -> btnCancel.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 32px; -fx-padding: 4 8 4 8; -fx-cursor: hand;"));
                btnCancel.setOnMouseExited(e -> btnCancel.setStyle("-fx-background-color: #F5EFEB; -fx-text-fill: #57534E; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 32px; -fx-padding: 4 8 4 8; -fx-cursor: hand;"));
                btnCancel.setOnAction(ev -> {
                    serverService.cancelOrder(o.getOrderId());
                    renderRows[0].run();
                    showToastNotification("Đã hủy đơn hàng #" + o.getOrderId());
                });

                actions.getChildren().addAll(btnPrint, btnDone, btnCancel);
                r.getChildren().addAll(lId, lTime, lTbl, lItems, lTotal, badge, actions);
                rowsBox.getChildren().add(r);
            }
        };

        fBtns.get(0).fire();
        txtSearch.textProperty().addListener((obs, o, n) -> {
            keywordFilter[0] = n != null ? n.trim().toLowerCase() : "";
            renderRows[0].run();
        });

        centerContent.getChildren().addAll(header, chipBar, gridCard);
    }

    // =========================================================================
    // TAB 4: MÀN PHA CHẾ KDS (STATION FILTER + EMBEDDED KDS VIEW)
    // =========================================================================
    private void renderKdsTab() {
        centerContent.getChildren().clear();
        centerContent.setSpacing(10);

        HBox stationBar = new HBox(10);
        stationBar.setAlignment(Pos.CENTER_LEFT);
        stationBar.setPadding(new Insets(10, 16, 10, 16));
        stationBar.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10px; -fx-border-color: #E6E0D6; -fx-border-radius: 10px;");

        Label lblStation = new Label("BỘ LỌC TRẠM PHA CHẾ (KDS STATION):");
        lblStation.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #78716C;");

        String[] stations = {"Tất cả các trạm", "☕ Quầy Máy Pha Espresso", "🍹 Quầy Trà & Đá Xay", "🥐 Tủ Bánh Ngọt & Đồ Ăn Nhẹ"};
        List<Button> stBtns = new ArrayList<>();
        final String[] stationCodes = {"ALL", "ESPRESSO", "TEA", "BAKERY"};
        for (int i = 0; i < stations.length; i++) {
            final String stName = stations[i];
            final String stCode = stationCodes[i];
            Button b = new Button(stName);
            b.setStyle(i == 0
                ? "-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 34px; -fx-padding: 6 14 6 14; -fx-cursor: hand;"
                : "-fx-background-color: #F5EFEB; -fx-text-fill: #57534E; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 600; -fx-background-radius: 8px; -fx-pref-height: 34px; -fx-padding: 6 14 6 14; -fx-cursor: hand;");
            b.setOnAction(e -> {
                for (Button btn : stBtns) {
                    boolean act = btn == b;
                    btn.setStyle(act
                        ? "-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 34px; -fx-padding: 6 14 6 14; -fx-cursor: hand;"
                        : "-fx-background-color: #F5EFEB; -fx-text-fill: #57534E; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 600; -fx-background-radius: 8px; -fx-pref-height: 34px; -fx-padding: 6 14 6 14; -fx-cursor: hand;");
                }
                cafe.controllers.ServerController.setStationFilter(stCode);
                showToastNotification("Đang lọc KDS theo trạm: " + stName);
            });
            stBtns.add(b);
        }

        stationBar.getChildren().add(lblStation);
        stationBar.getChildren().addAll(stBtns);

        VBox.setVgrow(embeddedKdsView, Priority.ALWAYS);
        centerContent.getChildren().addAll(stationBar, embeddedKdsView);
    }

    // =========================================================================
    // LIVE EVENT LISTENERS (REAL-TIME DASHBOARD & TOAST SYNC)
    // =========================================================================
    private void registerLiveListeners() {
        serverService.setOnOrderStatusChanged((orderId, status) -> {
            Platform.runLater(() -> {
                if (currentTab == 1) {
                    populateRecentOrders();
                } else if (currentTab == 3) {
                    renderOrderHistoryTab();
                }
            });
        });
        serverService.setOnTableStatusBroadcasted((tableNum, status) -> {
            Platform.runLater(() -> {
                if (currentTab == 1) {
                    populateRecentOrders();
                } else if (currentTab == 3) {
                    renderOrderHistoryTab();
                }
            });
        });
    }

    public void notifyNewOrderArrived(Order order) {
        Platform.runLater(() -> {
            showToastNotification("🔔 Đơn mới #" + order.getOrderId() + " (" + (order.getTable() > 0 ? "Bàn " + order.getTable() : "Mang đi") + ") vừa gửi xuống quầy!");
            if (currentTab == 1) {
                populateRecentOrders();
            } else if (currentTab == 3) {
                renderOrderHistoryTab();
            }
        });
    }
}
