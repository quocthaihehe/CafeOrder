package cafe.views;

import cafe.controllers.ClientController;
import cafe.models.Order;
import cafe.models.OrderItem;
import cafe.models.StaffModel;
import cafe.services.CafeServerService;
import cafe.services.ClientSocketService;
import cafe.utils.AnimationHelper;
import cafe.utils.CurrencyFormatter;
import cafe.utils.SvgIcons;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * FloorPlanView - Unified Staff POS Shell & Sơ Đồ Bàn 3 Tầng (Sprint 4.1):
 * 1. Unified Staff Shell: Giữ cố định Thanh điều hướng trái (235px) xuyên suốt cả 5 phân hệ:
 *    - [1. Bàn]: Sơ đồ bàn kiến trúc 3 tầng + Ngăn Chi Tiết Bàn bên phải (Right Table Inspector Drawer 295px).
 *    - [2. Thực đơn]: Nhúng trực tiếp Kiosk Gọi món (ClientView + ClientController) ngay trong khung chính.
 *    - [3. Đơn hàng]: Nhúng trực tiếp Quản lý Đơn hàng thời gian thực (OrdersDialog Embedded Panel).
 *    - [4. Thanh toán]: Phân hệ Thu ngân & Thanh toán nhanh theo bàn, áp mã giảm giá, in biên lai và giải phóng bàn.
 *    - [5. Cài đặt]: Thông tin ca trực nhân viên, thống kê hiệu suất ca, kiểm tra máy in nhiệt và chuyển sang Admin (nếu là Quản lý).
 * 2. Chuẩn hóa Sơ đồ Tầng 1 đủ 12 bàn liên tục (01 -> 12), bổ sung kiến trúc (Quầy Pha Chế, Lối Vào Chính, Hành Lang Kính).
 * 3. Tích hợp đầy đủ hộp thoại nghiệp vụ: Chuyển bàn (Transfer), Gộp bàn (Merge), Đặt bàn trước (Reserve) và Trả bàn trống (Checkout).
 */
public class FloorPlanView extends BorderPane {

    private int selectedTable = 5;
    private int currentFloor = 1;
    private String activeStaffTab = "FLOOR"; // FLOOR, MENU, ORDERS, BILLING, SETTINGS

    private final Label lblCurrentTableTitle = new Label("Bàn 05");
    private final Label lblCurrentTableStatus = new Label("● Đặt trước");
    private final Button btnGoToMenu = new Button("+ Gọi món cho bàn này →");

    private Button navBtnFloor;
    private Button navBtnMenu;
    private Button navBtnOrders;
    private Button navBtnBilling;
    private Button navBtnSettings;

    private Button tabF1;
    private Button tabF2;
    private Button tabF3;

    private final StackPane mainContentHolder = new StackPane();
    private HBox floorWorkspaceBox;
    private ClientView embeddedClientView;
    private ClientController embeddedClientController;

    private final Pane floorCanvas = new Pane();
    private final List<TableWithChairsNode> tableNodes = new ArrayList<>();
    private final VBox inspectorContentBox = new VBox(12);

    private final Label lblStaffName = new Label("Nguyễn Minh Luân");
    private final Label lblStaffRole = new Label("Nhân viên");
    private final Label lblStaffAvaText = new Label("NL");
    private StaffModel currentStaff;

    private final Map<Integer, String> tableStatusMap = new ConcurrentHashMap<>();
    private String currentTableFilter = "ALL";

    private Stage parentStage;
    private ClientSocketService socketService;
    private Consumer<Integer> onTableSelected;
    private Runnable onNavigateToMenu;
    private Runnable onLogout;
    private Runnable onOpenOrders;
    private Runnable onOpenAdminDashboard;

    // Floating toast notification inside Staff Shell
    private final HBox staffToastBox = new HBox(8);
    private final Label lblStaffToast = new Label();

    public FloorPlanView(int initialTable, Consumer<Integer> onTableSelected, Runnable onNavigateToMenu, Runnable onLogout) {
        this.selectedTable = initialTable > 0 ? initialTable : 5;
        this.onTableSelected = onTableSelected;
        this.onNavigateToMenu = onNavigateToMenu;
        this.onLogout = onLogout;

        if (this.selectedTable >= 300) {
            this.currentFloor = 3;
        } else if (this.selectedTable >= 200) {
            this.currentFloor = 2;
        } else {
            this.currentFloor = 1;
        }

        syncStatusesFromServer();
        initUI();
    }

    /**
     * Gắn kết Stage và ClientSocketService để nhúng trực tiếp màn hình Thực đơn (Tab 2)
     * ngay bên trong Unified Staff Shell mà không làm mất thanh Sidebar trái.
     */
    public void attachEmbeddedOrdering(Stage stage, ClientSocketService socketService, Runnable onOpenAdminDashboard) {
        this.parentStage = stage;
        this.socketService = socketService;
        this.onOpenAdminDashboard = onOpenAdminDashboard;
    }

    public void setOnOpenOrders(Runnable onOpenOrders) {
        this.onOpenOrders = onOpenOrders;
    }

    private void syncStatusesFromServer() {
        CafeServerService server = CafeServerService.getInstance();
        int[] allTables = {
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12,
            201, 202, 203, 204, 205, 206, 207, 208,
            301, 302, 303, 304, 305, 306
        };
        for (int t : allTables) {
            String st = server.getTableStatus(t);
            if ("OCCUPIED".equalsIgnoreCase(st)) st = "SERVING";
            tableStatusMap.put(t, st);
        }
    }

    private void initUI() {
        setStyle("-fx-background-color: #F5F0E8;");

        // 1. LEFT NAVIGATION RAIL (235px) - Giữ cố định xuyên suốt mọi tab
        VBox leftRail = buildLeftNavigationRail();
        setLeft(leftRail);

        // 2. MAIN WORKSPACE (Center Floor Plan + Right Inspector Drawer)
        floorWorkspaceBox = buildFloorWorkspace();

        // Setup floating toast
        staffToastBox.setAlignment(Pos.CENTER);
        staffToastBox.setPadding(new Insets(10, 20, 10, 20));
        staffToastBox.setStyle("-fx-background-color: #112823; -fx-background-radius: 999px; -fx-effect: dropshadow(gaussian, rgba(17,40,35,0.28), 14, 0.2, 0, 4);");
        staffToastBox.setMaxWidth(Region.USE_PREF_SIZE);
        staffToastBox.setMaxHeight(Region.USE_PREF_SIZE);
        staffToastBox.setVisible(false);
        staffToastBox.setManaged(false);
        lblStaffToast.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 12.5px; -fx-font-weight: 700;");
        staffToastBox.getChildren().add(lblStaffToast);
        StackPane.setAlignment(staffToastBox, Pos.BOTTOM_CENTER);
        StackPane.setMargin(staffToastBox, new Insets(0, 0, 20, 0));

        mainContentHolder.getChildren().addAll(floorWorkspaceBox, staffToastBox);
        setCenter(mainContentHolder);

        updateTableTitleDisplay();
        refreshInspectorDrawer();
    }

    public void showStaffToast(String message) {
        lblStaffToast.setText(message);
        staffToastBox.setVisible(true);
        staffToastBox.setManaged(true);
        staffToastBox.toFront();
        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2.8));
        delay.setOnFinished(e -> {
            staffToastBox.setVisible(false);
            staffToastBox.setManaged(false);
        });
        delay.playFromStart();
    }

    private VBox buildLeftNavigationRail() {
        VBox rail = new VBox(10);
        rail.setPrefWidth(235);
        rail.setMinWidth(220);
        rail.setMaxWidth(255);
        rail.setPadding(new Insets(20, 12, 20, 12));
        rail.setStyle("-fx-background-color: #F7F2EA; -fx-border-color: transparent #DED7CD transparent transparent; -fx-border-width: 0 1 0 0;");

        // Brand Logo
        VBox brandBox = new VBox(3);
        brandBox.setAlignment(Pos.CENTER_LEFT);
        brandBox.setPadding(new Insets(4, 8, 16, 8));

        Label lblBrand = new Label("Brew & Bean");
        lblBrand.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        Label lblSub = new Label("STAFF POS • ĐIỀU PHỐI BÀN");
        lblSub.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #563521; -fx-letter-spacing: 1.5px;");
        brandBox.getChildren().addAll(lblBrand, lblSub);

        // 5 mục điều hướng chuẩn Staff POS
        VBox navMenu = new VBox(6);

        navBtnFloor = createNavButton("Bàn", "so-do-ban.png", SvgIcons.TABLE, "🪑", true);
        navBtnFloor.setOnAction(e -> switchStaffTab("FLOOR"));

        navBtnMenu = createNavButton("Thực đơn", "thuc-don.png", SvgIcons.MENU, "📋", false);
        navBtnMenu.setOnAction(e -> switchStaffTab("MENU"));

        navBtnOrders = createNavButton("Đơn hàng", "don-hang.png", SvgIcons.ORDERS, "🧾", false);
        navBtnOrders.setOnAction(e -> switchStaffTab("ORDERS"));

        navBtnBilling = createNavButton("Thanh toán", "thanh-toan.png", SvgIcons.WALLET, "💳", false);
        navBtnBilling.setOnAction(e -> switchStaffTab("BILLING"));

        navBtnSettings = createNavButton("Cài đặt", "cai-dat.png", SvgIcons.SETTINGS, "⚙️", false);
        navBtnSettings.setOnAction(e -> switchStaffTab("SETTINGS"));

        navMenu.getChildren().addAll(navBtnFloor, navBtnMenu, navBtnOrders, navBtnBilling, navBtnSettings);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Thẻ hồ sơ nhân viên trực ca dưới đáy Sidebar - Tái cấu trúc 2 tầng (Vertical Stacked Profile Card)
        VBox profileCard = new VBox(8);
        profileCard.setPadding(new Insets(10, 12, 10, 12));
        profileCard.setStyle("-fx-background-color: #EFE8DD; -fx-background-radius: 10px; -fx-border-color: #E2DACD; -fx-border-radius: 10px; -fx-cursor: hand;");
        profileCard.setOnMouseClicked(e -> showStaffShiftModal(parentStage, currentStaff));

        // Tầng 1: Avatar 36px + Tên đầy đủ (không co cắt) + Nhãn vai trò
        HBox profTopRow = new HBox(10);
        profTopRow.setAlignment(Pos.CENTER_LEFT);

        Circle avatar = new Circle(18, Color.web("#112823"));
        lblStaffAvaText.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 800; -fx-text-fill: #FFFFFF;");
        StackPane avaPane = new StackPane(avatar, lblStaffAvaText);

        VBox profTexts = new VBox(2);
        HBox.setHgrow(profTexts, Priority.ALWAYS);
        lblStaffName.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13.5px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        lblStaffRole.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #15803D; -fx-background-color: #DCFCE7; -fx-padding: 2 6 2 6; -fx-background-radius: 4px;");
        profTexts.getChildren().addAll(lblStaffName, lblStaffRole);
        profTopRow.getChildren().addAll(avaPane, profTexts);

        // Tầng 2: Nút mở Ca trực & Bàn giao + Nút Thoát
        HBox profBottomRow = new HBox(6);
        profBottomRow.setAlignment(Pos.CENTER_LEFT);

        Button btnShiftManage = new Button("⚙ Ca trực & Bàn giao");
        HBox.setHgrow(btnShiftManage, Priority.ALWAYS);
        btnShiftManage.setMaxWidth(Double.MAX_VALUE);
        btnShiftManage.setStyle("-fx-background-color: #FAF8F5; -fx-text-fill: #112823; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 11px; -fx-font-weight: 700; -fx-border-color: #DED7CD; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 4 8 4 8; -fx-cursor: hand;");
        btnShiftManage.setOnAction(e -> {
            e.consume();
            showStaffShiftModal(parentStage, currentStaff);
        });
        profBottomRow.getChildren().add(btnShiftManage);
        profileCard.getChildren().addAll(profTopRow, profBottomRow);

        // Đăng xuất ở dưới cùng (nổi bật với theme đỏ cảnh báo sang trọng)
        Button btnExit = new Button();
        btnExit.setMaxWidth(Double.MAX_VALUE);
        btnExit.setAlignment(Pos.CENTER);
        btnExit.setPrefHeight(38);
        btnExit.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-border-color: #FECACA; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 700; -fx-cursor: hand; -fx-padding: 8 14 8 14;");
        btnExit.setOnMouseEntered(e -> btnExit.setStyle("-fx-background-color: #FCA5A5; -fx-text-fill: #7F1D1D; -fx-border-color: #F87171; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 700; -fx-cursor: hand; -fx-padding: 8 14 8 14;"));
        btnExit.setOnMouseExited(e -> btnExit.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-border-color: #FECACA; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 700; -fx-cursor: hand; -fx-padding: 8 14 8 14;"));
        AnimationHelper.applyButtonPress(btnExit);

        HBox exitContent = new HBox(8);
        exitContent.setAlignment(Pos.CENTER);
        SVGPath exitIcon = SvgIcons.createOutlineIcon(SvgIcons.LOG_OUT, 0.75, Color.web("#B91C1C"));
        Label lblExit = new Label("Đăng xuất ca trực");
        lblExit.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #B91C1C;");
        exitContent.getChildren().addAll(exitIcon, lblExit);
        btnExit.setGraphic(exitContent);
        btnExit.setOnAction(e -> {
            if (onLogout != null) onLogout.run();
        });

        rail.getChildren().addAll(brandBox, navMenu, spacer, profileCard, btnExit);
        return rail;
    }

    public void switchStaffTab(String tabKey) {
        this.activeStaffTab = tabKey;
        updateNavHighlight();

        mainContentHolder.getChildren().clear();

        if ("FLOOR".equals(tabKey)) {
            syncStatusesFromServer();
            renderFloorPlan();
            updateTableTitleDisplay();
            refreshInspectorDrawer();
            mainContentHolder.getChildren().addAll(floorWorkspaceBox, staffToastBox);
        } else if ("MENU".equals(tabKey)) {
            if (socketService != null) {
                ensureEmbeddedMenuInitialized();
                embeddedClientController.setTableNumber(selectedTable);
                embeddedClientController.refreshCatalog();
                mainContentHolder.getChildren().addAll(embeddedClientView, staffToastBox);
            } else if (onNavigateToMenu != null) {
                onNavigateToMenu.run();
            }
        } else if ("ORDERS".equals(tabKey)) {
            VBox ordersPanel = OrdersDialog.createEmbeddedPanel(
                parentStage,
                () -> switchStaffTab("FLOOR"),
                () -> {
                    syncStatusesFromServer();
                    renderFloorPlan();
                    refreshInspectorDrawer();
                }
            );
            mainContentHolder.getChildren().addAll(ordersPanel, staffToastBox);
        } else if ("BILLING".equals(tabKey)) {
            VBox billingPanel = buildEmbeddedBillingPanel();
            mainContentHolder.getChildren().addAll(billingPanel, staffToastBox);
        } else if ("SETTINGS".equals(tabKey)) {
            VBox settingsPanel = buildEmbeddedStaffSettingsPanel();
            mainContentHolder.getChildren().addAll(settingsPanel, staffToastBox);
        }
    }

    private void ensureEmbeddedMenuInitialized() {
        if (embeddedClientView == null) {
            embeddedClientView = new ClientView();
            embeddedClientView.configureForEmbeddedShell(true);
            embeddedClientController = new ClientController(
                parentStage,
                embeddedClientView,
                socketService,
                selectedTable,
                () -> switchStaffTab("FLOOR")
            );
            if (embeddedClientView.getFloorPlanButton() != null) {
                embeddedClientView.getFloorPlanButton().setVisible(true);
                embeddedClientView.getFloorPlanButton().setManaged(true);
            }
        }
    }

    private void updateNavHighlight() {
        styleNavButton(navBtnFloor, "Bàn", "so-do-ban.png", SvgIcons.TABLE, "🪑", "FLOOR".equals(activeStaffTab));
        styleNavButton(navBtnMenu, "Thực đơn", "thuc-don.png", SvgIcons.MENU, "📋", "MENU".equals(activeStaffTab));
        styleNavButton(navBtnOrders, "Đơn hàng", "don-hang.png", SvgIcons.ORDERS, "🧾", "ORDERS".equals(activeStaffTab));
        styleNavButton(navBtnBilling, "Thanh toán", "thanh-toan.png", SvgIcons.WALLET, "💳", "BILLING".equals(activeStaffTab));
        styleNavButton(navBtnSettings, "Cài đặt", "cai-dat.png", SvgIcons.SETTINGS, "⚙️", "SETTINGS".equals(activeStaffTab));
    }

    private Button createNavButton(String title, String iconFileName, String iconSvg, String fallbackEmoji, boolean isActive) {
        Button btn = new Button();
        btn.setFocusTraversable(false);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(10, 14, 10, 14));
        AnimationHelper.applyButtonPress(btn);
        styleNavButton(btn, title, iconFileName, iconSvg, fallbackEmoji, isActive);
        return btn;
    }

    private void styleNavButton(Button btn, String title, String iconFileName, String iconSvg, String fallbackEmoji, boolean isActive) {
        if (btn == null) return;
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        StackPane iconContainer = new StackPane();
        iconContainer.setPrefSize(26, 26);
        iconContainer.setMinSize(26, 26);
        iconContainer.setMaxSize(26, 26);
        iconContainer.setAlignment(Pos.CENTER);

        Color iconColor = isActive ? Color.WHITE : Color.web("#3F3931");
        Node iconNode = cafe.utils.IconHelper.getIcon("staff", iconFileName, iconSvg, fallbackEmoji, 20, iconColor);
        iconContainer.getChildren().add(iconNode);

        Label label = new Label(title);
        label.setStyle("-fx-font-size: 13.5px; -fx-font-weight: " + (isActive ? "800" : "700") + "; -fx-text-fill: " + (isActive ? "#FFFFFF" : "#3F3931") + ";");

        row.getChildren().addAll(iconContainer, label);
        btn.setGraphic(row);

        if (isActive) {
            btn.setStyle("-fx-background-color: #112823; -fx-background-radius: 10px; -fx-cursor: hand;");
            btn.setOnMouseEntered(null);
            btn.setOnMouseExited(null);
        } else {
            btn.setStyle("-fx-background-color: transparent; -fx-background-radius: 10px; -fx-cursor: hand;");
            btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #EFE8DD; -fx-background-radius: 10px; -fx-cursor: hand;"));
            btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-background-radius: 10px; -fx-cursor: hand;"));
        }
    }

    // =========================================================================
    // PHÂN HỆ 1: SƠ ĐỒ BÀN 3 TẦNG + NGĂN CHI TIẾT BÀN BÊN PHẢI (295PX)
    // =========================================================================
    private HBox buildFloorWorkspace() {
        HBox workspace = new HBox(0);
        HBox.setHgrow(workspace, Priority.ALWAYS);

        VBox centerFloorArea = buildCenterFloorArea();
        HBox.setHgrow(centerFloorArea, Priority.ALWAYS);

        VBox rightInspector = buildRightTableInspectorDrawer();

        workspace.getChildren().addAll(centerFloorArea, rightInspector);
        return workspace;
    }

    private VBox buildCenterFloorArea() {
        VBox center = new VBox(12);
        center.setPadding(new Insets(16, 20, 16, 20));
        HBox.setHgrow(center, Priority.ALWAYS);

        // 1. Top Bar: Table info, Status Badge, Quick Summary, CTA Button
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        lblCurrentTableTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        lblCurrentTableStatus.setStyle("-fx-background-color: #DCE8D9; -fx-text-fill: #597256; -fx-font-size: 11px; -fx-font-weight: 700; -fx-background-radius: 999px; -fx-padding: 4 12 4 12;");
        titleRow.getChildren().addAll(lblCurrentTableTitle, lblCurrentTableStatus);

        Region sp1 = new Region();
        HBox.setHgrow(sp1, Priority.ALWAYS);

        Button btnQuickReserve = new Button("📅 Đặt trước bàn");
        btnQuickReserve.setStyle("-fx-background-color: #FFFBEB; -fx-text-fill: #92400E; -fx-border-color: #F59E0B; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-font-size: 12px; -fx-font-weight: 700; -fx-padding: 8 14 8 14; -fx-cursor: hand;");
        btnQuickReserve.setOnAction(e -> showReservationModal(selectedTable));

        btnGoToMenu.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 8 18 8 18; -fx-cursor: hand;");
        AnimationHelper.applyButtonPress(btnGoToMenu);
        btnGoToMenu.setOnAction(e -> switchStaffTab("MENU"));

        // Nút Đăng xuất ca trực trên TopBar
        Button btnTopLogout = new Button();
        btnTopLogout.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-border-color: #FECACA; -fx-border-radius: 8px; -fx-border-width: 1px; -fx-padding: 8 14 8 14; -fx-cursor: hand;");
        btnTopLogout.setOnMouseEntered(e -> btnTopLogout.setStyle("-fx-background-color: #FCA5A5; -fx-text-fill: #7F1D1D; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-border-color: #F87171; -fx-border-radius: 8px; -fx-border-width: 1px; -fx-padding: 8 14 8 14; -fx-cursor: hand;"));
        btnTopLogout.setOnMouseExited(e -> btnTopLogout.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-border-color: #FECACA; -fx-border-radius: 8px; -fx-border-width: 1px; -fx-padding: 8 14 8 14; -fx-cursor: hand;"));
        AnimationHelper.applyButtonPress(btnTopLogout);

        HBox topLogoutContent = new HBox(6);
        topLogoutContent.setAlignment(Pos.CENTER);
        SVGPath topLogoutIcon = SvgIcons.createOutlineIcon(SvgIcons.LOG_OUT, 0.7, Color.web("#B91C1C"));
        Label lblTopLogoutText = new Label("Đăng xuất");
        lblTopLogoutText.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #B91C1C;");
        topLogoutContent.getChildren().addAll(topLogoutIcon, lblTopLogoutText);
        btnTopLogout.setGraphic(topLogoutContent);
        btnTopLogout.setOnAction(e -> {
            if (onLogout != null) onLogout.run();
        });

        topBar.getChildren().addAll(titleRow, sp1, btnQuickReserve, btnGoToMenu, btnTopLogout);

        // 2. Floor & Table Status Toolbar
        HBox floorToolbar = new HBox(10);
        floorToolbar.setAlignment(Pos.CENTER_LEFT);

        HBox floorTabs = new HBox(6);
        floorTabs.setAlignment(Pos.CENTER_LEFT);

        tabF1 = new Button("Tầng 1");
        tabF1.setTooltip(new javafx.scene.control.Tooltip("Sảnh chính & Barista"));
        tabF2 = new Button("Tầng 2");
        tabF2.setTooltip(new javafx.scene.control.Tooltip("Ban công ngoài trời & Phòng đọc sách"));
        tabF3 = new Button("Tầng 3");
        tabF3.setTooltip(new javafx.scene.control.Tooltip("Sân thượng Rooftop Zen Garden"));

        tabF1.setOnAction(e -> selectFloor(1));
        tabF2.setOnAction(e -> selectFloor(2));
        tabF3.setOnAction(e -> selectFloor(3));

        updateFloorTabStyles();
        floorTabs.getChildren().addAll(tabF1, tabF2, tabF3);

        // Table status filter pills
        HBox statusFilterTabs = new HBox(6);
        statusFilterTabs.setAlignment(Pos.CENTER_LEFT);

        Button btnAllTables = createFilterPill("Tất cả", "tat-ca-ban.png", "ALL".equals(currentTableFilter));
        Button btnEmptyTables = createFilterPill("Bàn trống", "ban-trong.png", "EMPTY".equals(currentTableFilter));
        Button btnServingTables = createFilterPill("Có khách", "co-khach.png", "SERVING".equals(currentTableFilter));

        btnAllTables.setOnAction(e -> setTableFilter("ALL", btnAllTables, btnEmptyTables, btnServingTables));
        btnEmptyTables.setOnAction(e -> setTableFilter("EMPTY", btnAllTables, btnEmptyTables, btnServingTables));
        btnServingTables.setOnAction(e -> setTableFilter("SERVING", btnAllTables, btnEmptyTables, btnServingTables));
        statusFilterTabs.getChildren().addAll(btnAllTables, btnEmptyTables, btnServingTables);

        Region tbSp = new Region();
        HBox.setHgrow(tbSp, Priority.ALWAYS);

        // Quick Table Action Tools - Nhãn ngắn gọn chống co vỡ chữ
        HBox actionTools = new HBox(6);
        actionTools.setAlignment(Pos.CENTER_RIGHT);

        Button btnRefresh = createToolButton("⟳", "lam-moi.png");
        btnRefresh.setTooltip(new javafx.scene.control.Tooltip("Làm mới dữ liệu bàn"));
        btnRefresh.setPrefWidth(36);
        btnRefresh.setOnAction(e -> {
            syncStatusesFromServer();
            renderFloorPlan();
            refreshInspectorDrawer();
            showStaffToast("Đã đồng bộ trạng thái bàn mới nhất từ máy chủ.");
        });

        Button btnTransfer = createToolButton("⇄ Chuyển", "chuyen-ban.png");
        btnTransfer.setOnAction(e -> showTransferTableModal(selectedTable));

        Button btnMerge = createToolButton("⧉ Gộp", "tach-gop-ban.png");
        btnMerge.setOnAction(e -> showMergeTableModal(selectedTable));

        actionTools.getChildren().addAll(btnRefresh, btnTransfer, btnMerge);

        floorToolbar.getChildren().addAll(floorTabs, statusFilterTabs, tbSp, actionTools);

        // 3. Isometric 2.5D Floor Canvas
        floorCanvas.setPrefHeight(510);
        floorCanvas.setMinHeight(450);
        floorCanvas.setStyle(
            "-fx-background-color: #EDE5DC; " +
            "-fx-background-radius: 20px; " +
            "-fx-border-color: #DFD5C8; " +
            "-fx-border-radius: 20px; " +
            "-fx-border-width: 1px; " +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.04), 16, 0.08, 0, 4);"
        );
        VBox.setVgrow(floorCanvas, Priority.ALWAYS);

        floorCanvas.widthProperty().addListener((obs, oldW, newW) -> {
            if (newW != null && newW.doubleValue() > 0) {
                renderFloorPlan();
            }
        });

        renderFloorPlan();

        // 4. Status Legend Bar at Bottom
        HBox legendBar = new HBox(18);
        legendBar.setAlignment(Pos.CENTER_LEFT);
        legendBar.setPadding(new Insets(4, 4, 2, 4));

        legendBar.getChildren().addAll(
            createLegendItem("Bàn trống (Sẵn sàng)", "#16A34A"),
            createLegendItem("Đang chọn (Bàn trống)", "#112823"),
            createLegendItem("Đang phục vụ (Có khách)", "#E2943B"),
            createLegendItem("Đặt trước (Reserved)", "#F59E0B"),
            createLegendItem("Chờ dọn bàn", "#EF4444")
        );

        center.getChildren().addAll(topBar, floorToolbar, floorCanvas, legendBar);
        return center;
    }

    private VBox buildRightTableInspectorDrawer() {
        VBox drawer = new VBox(12);
        drawer.setPrefWidth(305);
        drawer.setMinWidth(290);
        drawer.setMaxWidth(320);
        drawer.setPadding(new Insets(18, 16, 18, 16));
        drawer.setStyle("-fx-background-color: #FCFBF8; -fx-border-color: transparent transparent transparent #DFD7CA; -fx-border-width: 0 0 0 1;");

        ScrollPane scroll = new ScrollPane(inspectorContentBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        drawer.getChildren().add(scroll);
        return drawer;
    }

    private void refreshInspectorDrawer() {
        inspectorContentBox.getChildren().clear();

        CafeServerService server = CafeServerService.getInstance();
        String st = tableStatusMap.getOrDefault(selectedTable, server.getTableStatus(selectedTable));
        Order combinedOrder = server.getCombinedOrderForTable(selectedTable);
        if (combinedOrder != null && !"SERVING".equalsIgnoreCase(st) && !"OCCUPIED".equalsIgnoreCase(st)) {
            st = "SERVING";
            tableStatusMap.put(selectedTable, "SERVING");
        }
        CafeServerService.ReservationInfo resInfo = server.getReservation(selectedTable);

        String tableCode = selectedTable < 10 ? "0" + selectedTable : String.valueOf(selectedTable);
        int floorNum = selectedTable >= 300 ? 3 : (selectedTable >= 200 ? 2 : 1);
        boolean isLargeTable = (selectedTable == 11 || selectedTable == 12 || selectedTable == 207 || selectedTable == 208 || selectedTable == 306);

        // Header Card
        VBox headerCard = new VBox(6);
        headerCard.setPadding(new Insets(14));
        headerCard.setStyle("-fx-background-color: #F4EFE6; -fx-background-radius: 12px; -fx-border-color: #E4DDD2; -fx-border-radius: 12px;");

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label lblTbl = new Label("BÀN " + tableCode);
        lblTbl.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label badge = new Label();
        if ("SERVING".equalsIgnoreCase(st) || "OCCUPIED".equalsIgnoreCase(st)) {
            badge.setText("● Đang phục vụ");
            badge.setStyle("-fx-background-color: #172E27; -fx-text-fill: #E8C798; -fx-font-size: 11px; -fx-font-weight: 700; -fx-background-radius: 999px; -fx-padding: 4 10 4 10;");
        } else if ("RESERVED".equalsIgnoreCase(st)) {
            badge.setText("● Đặt trước");
            badge.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #92400E; -fx-font-size: 11px; -fx-font-weight: 700; -fx-background-radius: 999px; -fx-padding: 4 10 4 10;");
        } else if ("CLEANING".equalsIgnoreCase(st)) {
            badge.setText("● Chờ dọn bàn");
            badge.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-font-size: 11px; -fx-font-weight: 700; -fx-background-radius: 999px; -fx-padding: 4 10 4 10;");
        } else {
            badge.setText("● Bàn trống");
            badge.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-font-size: 11px; -fx-font-weight: 700; -fx-background-radius: 999px; -fx-padding: 4 10 4 10;");
        }
        topRow.getChildren().addAll(lblTbl, sp, badge);

        Label lblSubInfo = new Label("Tầng " + floorNum + "  •  Sức chứa: " + (isLargeTable ? "6 - 8 khách (Bàn dài)" : "4 khách (Bàn tiêu chuẩn)"));
        lblSubInfo.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #6F695F; -fx-font-weight: 600;");
        headerCard.getChildren().addAll(topRow, lblSubInfo);
        inspectorContentBox.getChildren().add(headerCard);

        // Content by Table State
        if (combinedOrder != null && ("SERVING".equalsIgnoreCase(st) || "OCCUPIED".equalsIgnoreCase(st))) {
            long elapsedMins = Math.max(1, (System.currentTimeMillis() - combinedOrder.getTimestamp()) / 60_000L);
            String kdsStatus = combinedOrder.getStatus();
            String kdsText = "DONE".equalsIgnoreCase(kdsStatus) ? "Đã lên đủ món" : ("PREPARING".equalsIgnoreCase(kdsStatus) ? "Barista đang pha" : "Đang chờ pha");

            VBox metaCard = new VBox(6);
            metaCard.setPadding(new Insets(12));
            metaCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10px; -fx-border-color: #E6E0D6; -fx-border-radius: 10px;");

            Label lblOrderNo = new Label("Mã phiếu: #" + combinedOrder.getOrderId() + "  •  ⏱ " + elapsedMins + " phút");
            lblOrderNo.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 800; -fx-text-fill: #112823;");
            Label lblKds = new Label("Tiến độ quầy Bar: " + kdsText);
            lblKds.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 600; -fx-text-fill: #B45309;");
            metaCard.getChildren().addAll(lblOrderNo, lblKds);
            if (combinedOrder.getNote() != null && !combinedOrder.getNote().trim().isEmpty()) {
                Label lblNote = new Label("Ghi chú: " + combinedOrder.getNote());
                lblNote.setStyle("-fx-font-size: 11px; -fx-text-fill: #78716C; -fx-font-style: italic;");
                lblNote.setWrapText(true);
                metaCard.getChildren().add(lblNote);
            }
            inspectorContentBox.getChildren().add(metaCard);

            // Ordered Items List
            VBox itemsBox = new VBox(8);
            itemsBox.setPadding(new Insets(12));
            itemsBox.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10px; -fx-border-color: #E6E0D6; -fx-border-radius: 10px;");

            Label lblListHeader = new Label("DANH SÁCH MÓN ĐÃ GỌI (" + combinedOrder.getTotalQuantity() + ")");
            lblListHeader.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 800; -fx-text-fill: #78716C; -fx-letter-spacing: 0.8px;");
            itemsBox.getChildren().add(lblListHeader);

            for (OrderItem item : combinedOrder.getItems()) {
                VBox itemRow = new VBox(2);
                itemRow.setPadding(new Insets(6, 0, 6, 0));
                itemRow.setStyle("-fx-border-color: transparent transparent #F2ECE4 transparent; -fx-border-width: 0 0 1 0;");

                HBox line1 = new HBox(6);
                line1.setAlignment(Pos.CENTER_LEFT);
                Label lblName = new Label(item.getQty() + "x " + item.getName());
                lblName.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #1C1917;");
                lblName.setWrapText(true);
                HBox.setHgrow(lblName, Priority.ALWAYS);

                Label lblSub = new Label(CurrencyFormatter.formatVND(item.getSubtotal()));
                lblSub.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #112823;");
                line1.getChildren().addAll(lblName, lblSub);
                itemRow.getChildren().add(line1);

                String custom = item.getCustomizationSummary();
                if (custom != null && !custom.isEmpty()) {
                    Label lblCustom = new Label(custom);
                    lblCustom.setStyle("-fx-font-size: 10.5px; -fx-text-fill: #78716C;");
                    lblCustom.setWrapText(true);
                    itemRow.getChildren().add(lblCustom);
                }
                itemsBox.getChildren().add(itemRow);
            }

            // Bill totals
            VBox totalsBox = new VBox(4);
            totalsBox.setPadding(new Insets(6, 0, 0, 0));
            totalsBox.getChildren().add(createSummaryRow("Tạm tính:", CurrencyFormatter.formatVND(combinedOrder.getSubtotalAmount()), false));
            if (combinedOrder.getDiscountAmount() > 0) {
                totalsBox.getChildren().add(createSummaryRow("Giảm giá:", "-" + CurrencyFormatter.formatVND(combinedOrder.getDiscountAmount()), false));
            }
            totalsBox.getChildren().add(createSummaryRow("TỔNG THANH TOÁN:", CurrencyFormatter.formatVND(combinedOrder.getTotalAmount()), true));
            itemsBox.getChildren().add(totalsBox);

            inspectorContentBox.getChildren().add(itemsBox);

            // Action Buttons for SERVING Table
            VBox actionsBox = new VBox(8);
            Button btnAddMore = createInspectorButton("+ Gọi thêm món cho bàn này", "#112823", "#FFFFFF");
            btnAddMore.setOnAction(e -> switchStaffTab("MENU"));

            HBox rowTransferMerge = new HBox(8);
            Button btnTrans = createInspectorButton("⇄ Chuyển bàn", "#F4EFE6", "#112823");
            btnTrans.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(btnTrans, Priority.ALWAYS);
            btnTrans.setOnAction(e -> showTransferTableModal(selectedTable));

            Button btnMrg = createInspectorButton("🔗 Gộp bàn", "#F4EFE6", "#112823");
            btnMrg.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(btnMrg, Priority.ALWAYS);
            btnMrg.setOnAction(e -> showMergeTableModal(selectedTable));
            rowTransferMerge.getChildren().addAll(btnTrans, btnMrg);

            Button btnPrintTemp = createInspectorButton("🖨 In phiếu tạm tính", "#FFFFFF", "#112823");
            btnPrintTemp.setStyle(btnPrintTemp.getStyle() + "-fx-border-color: #D8CEBE; -fx-border-radius: 8px;");
            final Order orderToPrint = combinedOrder;
            btnPrintTemp.setOnAction(e -> OrderSuccessDialog.showReceiptForOrder(parentStage, orderToPrint, lblStaffName.getText()));

            Button btnCheckout = createInspectorButton("💳 Thanh toán & Trả bàn trống", "#15803D", "#FFFFFF");
            btnCheckout.setOnAction(e -> switchStaffTab("BILLING"));

            actionsBox.getChildren().addAll(btnAddMore, rowTransferMerge, btnPrintTemp, btnCheckout);
            inspectorContentBox.getChildren().add(actionsBox);

        } else if ("RESERVED".equalsIgnoreCase(st)) {
            VBox resCard = new VBox(8);
            resCard.setPadding(new Insets(14));
            resCard.setStyle("-fx-background-color: #FFFBEB; -fx-background-radius: 10px; -fx-border-color: #F59E0B; -fx-border-radius: 10px;");

            Label lblResHeader = new Label("THÔNG TIN KHÁCH ĐẶT TRƯỚC");
            lblResHeader.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 800; -fx-text-fill: #B45309;");
            String guestName = resInfo != null ? resInfo.customerName : "Khách đặt qua hotline";
            String phone = resInfo != null ? resInfo.phone : "0909.123.456";
            String time = resInfo != null ? resInfo.reservationTime : "19:30";
            int guests = resInfo != null ? resInfo.guestCount : 4;
            String note = (resInfo != null && resInfo.note != null && !resInfo.note.isEmpty()) ? resInfo.note : "Giữ bàn vị trí đẹp";

            Label lblGuest = new Label("👤 Khách hàng: " + guestName);
            lblGuest.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #78350F;");
            Label lblPhone = new Label("📞 Điện thoại: " + phone);
            lblPhone.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #92400E;");
            Label lblTime = new Label("🕒 Giờ hẹn: " + time + "  •  " + guests + " khách");
            lblTime.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #92400E;");
            Label lblNote = new Label("📝 Ghi chú: " + note);
            lblNote.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #78350F;");
            lblNote.setWrapText(true);

            resCard.getChildren().addAll(lblResHeader, lblGuest, lblPhone, lblTime, lblNote);
            inspectorContentBox.getChildren().add(resCard);

            Button btnCheckIn = createInspectorButton("✓ Khách đã đến • Mở bàn & Gọi món", "#112823", "#FFFFFF");
            btnCheckIn.setOnAction(e -> switchStaffTab("MENU"));

            Button btnTransferRes = createInspectorButton("⇄ Chuyển lịch sang bàn khác", "#F4EFE6", "#112823");
            btnTransferRes.setOnAction(e -> showTransferTableModal(selectedTable));

            Button btnCancelRes = createInspectorButton("✕ Hủy đặt trước (Trả bàn trống)", "#FEE2E2", "#B91C1C");
            btnCancelRes.setOnAction(e -> {
                CafeServerService.getInstance().cancelReservation(selectedTable);
                tableStatusMap.put(selectedTable, "EMPTY");
                renderFloorPlan();
                updateTableTitleDisplay();
                refreshInspectorDrawer();
                showStaffToast("Đã hủy lịch đặt trước Bàn " + tableCode + ".");
            });

            inspectorContentBox.getChildren().addAll(btnCheckIn, btnTransferRes, btnCancelRes);
        } else {
            // EMPTY or CLEANING
            VBox emptyCard = new VBox(8);
            emptyCard.setPadding(new Insets(16));
            emptyCard.setAlignment(Pos.CENTER);
            emptyCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10px; -fx-border-color: #E6E0D6; -fx-border-radius: 10px;");

            Label lblIcon = new Label("CLEANING".equalsIgnoreCase(st) ? "🧹" : "✨");
            lblIcon.setStyle("-fx-font-size: 26px;");
            Label lblEmptyState = new Label("CLEANING".equalsIgnoreCase(st) ? "Bàn đang chờ nhân viên dọn dẹp" : "Bàn trống sạch sẽ • Sẵn sàng đón khách");
            lblEmptyState.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #112823;");
            lblEmptyState.setWrapText(true);
            Label lblHint = new Label("Nhấn đúp vào bàn hoặc bấm nút bên dưới để mở bàn và gọi món hộ khách.");
            lblHint.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #78716C; -fx-text-alignment: center;");
            lblHint.setWrapText(true);

            emptyCard.getChildren().addAll(lblIcon, lblEmptyState, lblHint);
            inspectorContentBox.getChildren().add(emptyCard);

            Button btnOpenOrder = createInspectorButton("+ Mở bàn & Đặt món ngay", "#112823", "#FFFFFF");
            btnOpenOrder.setOnAction(e -> switchStaffTab("MENU"));

            Button btnReserve = createInspectorButton("📅 Đặt bàn trước (Reserve)", "#FFFBEB", "#92400E");
            btnReserve.setStyle(btnReserve.getStyle() + "-fx-border-color: #F59E0B; -fx-border-radius: 8px;");
            btnReserve.setOnAction(e -> showReservationModal(selectedTable));

            String cleanToggleText = "CLEANING".equalsIgnoreCase(st) ? "✓ Đã dọn xong (Chuyển về Bàn trống)" : "🧹 Đánh dấu Bàn chờ dọn";
            Button btnCleanToggle = createInspectorButton(cleanToggleText, "#F4EFE6", "#3F3931");
            final String currentSt = st;
            btnCleanToggle.setOnAction(e -> {
                String nextSt = "CLEANING".equalsIgnoreCase(currentSt) ? "EMPTY" : "CLEANING";
                tableStatusMap.put(selectedTable, nextSt);
                CafeServerService.getInstance().broadcastTableStatus(selectedTable, nextSt);
                renderFloorPlan();
                updateTableTitleDisplay();
                refreshInspectorDrawer();
            });

            inspectorContentBox.getChildren().addAll(btnOpenOrder, btnReserve, btnCleanToggle);
        }
    }

    private HBox createSummaryRow(String label, String value, boolean isBoldTotal) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(label);
        Label v = new Label(value);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        if (isBoldTotal) {
            l.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #112823;");
            v.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #B45309;");
        } else {
            l.setStyle("-fx-font-size: 12px; -fx-text-fill: #57534E;");
            v.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #1C1917;");
        }
        row.getChildren().addAll(l, sp, v);
        return row;
    }

    private Button createInspectorButton(String text, String bgHex, String fgHex) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPadding(new Insets(10, 14, 10, 14));
        btn.setStyle(
            "-fx-background-color: " + bgHex + "; " +
            "-fx-text-fill: " + fgHex + "; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 700; " +
            "-fx-background-radius: 8px; " +
            "-fx-cursor: hand;"
        );
        AnimationHelper.applyButtonPress(btn);
        return btn;
    }

    // =========================================================================
    // PHÂN HỆ 4: THANH TOÁN & THU NGÂN THEO BÀN (EMBEDDED TAB 4)
    // =========================================================================
    private VBox buildEmbeddedBillingPanel() {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(22, 28, 22, 28));
        panel.setStyle("-fx-background-color: #F4EFE6;");
        VBox.setVgrow(panel, Priority.ALWAYS);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Button btnBack = new Button("← Sơ đồ bàn");
        btnBack.setStyle("-fx-background-color: #EAE4DA; -fx-text-fill: #112823; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 8 14 8 14; -fx-cursor: hand;");
        btnBack.setOnAction(e -> switchStaffTab("FLOOR"));

        VBox titleCol = new VBox(2);
        Label lblTitle = new Label("Thu Ngân & Thanh Toán Tại Bàn");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        Label lblSub = new Label("Chọn bàn đang phục vụ để kiểm tra tạm tính, áp dụng khuyến mãi, in phiếu và thu tiền trả bàn trống");
        lblSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #78716C;");
        titleCol.getChildren().addAll(lblTitle, lblSub);
        header.getChildren().addAll(btnBack, titleCol);

        HBox body = new HBox(20);
        VBox.setVgrow(body, Priority.ALWAYS);

        // Cột trái: Danh sách các bàn đang có hóa đơn chờ thanh toán
        VBox leftTablesCard = new VBox(10);
        leftTablesCard.setPrefWidth(320);
        leftTablesCard.setPadding(new Insets(16));
        leftTablesCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 14px; -fx-border-color: #E4DDD3; -fx-border-radius: 14px;");

        Label lblActiveHeader = new Label("CÁC BÀN ĐANG CHỜ THANH TOÁN");
        lblActiveHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #78716C; -fx-letter-spacing: 0.8px;");
        leftTablesCard.getChildren().add(lblActiveHeader);

        VBox servingList = new VBox(8);
        CafeServerService server = CafeServerService.getInstance();
        int[] checkTables = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 201, 202, 203, 204, 205, 206, 207, 208, 301, 302, 303, 304, 305, 306};
        for (int t : checkTables) {
            Order ord = server.getCombinedOrderForTable(t);
            if (ord != null) {
                boolean isCurr = (t == selectedTable);
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(10, 14, 10, 14));
                row.setStyle(
                    "-fx-background-color: " + (isCurr ? "#112823" : "#F8F5F0") + "; " +
                    "-fx-background-radius: 10px; -fx-cursor: hand;"
                );
                Label lName = new Label("Bàn " + (t < 10 ? "0" + t : t) + " (" + ord.getTotalQuantity() + " món)");
                lName.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: " + (isCurr ? "#FFFFFF" : "#112823") + ";");
                Region sp = new Region();
                HBox.setHgrow(sp, Priority.ALWAYS);
                Label lAmt = new Label(CurrencyFormatter.formatVND(ord.getTotalAmount()));
                lAmt.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: " + (isCurr ? "#E8C798" : "#B45309") + ";");
                row.getChildren().addAll(lName, sp, lAmt);
                final int targetTbl = t;
                row.setOnMouseClicked(e -> {
                    this.selectedTable = targetTbl;
                    switchStaffTab("BILLING");
                });
                servingList.getChildren().add(row);
            }
        }

        if (servingList.getChildren().isEmpty()) {
            Label lblNone = new Label("Hiện tất cả các bàn đều đã thanh toán xong.");
            lblNone.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #78716C; -fx-padding: 20 0 20 0;");
            servingList.getChildren().add(lblNone);
        }

        ScrollPane leftScroll = new ScrollPane(servingList);
        leftScroll.setFitToWidth(true);
        leftScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(leftScroll, Priority.ALWAYS);
        leftTablesCard.getChildren().add(leftScroll);

        // Cột phải: Chi tiết hóa đơn & phương thức thanh toán của bàn đang chọn
        VBox rightBillCard = new VBox(14);
        HBox.setHgrow(rightBillCard, Priority.ALWAYS);
        rightBillCard.setPadding(new Insets(20, 24, 20, 24));
        rightBillCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 14px; -fx-border-color: #E4DDD3; -fx-border-radius: 14px;");

        Order currentOrder = server.getCombinedOrderForTable(selectedTable);
        String tblLabel = "Bàn " + (selectedTable < 10 ? "0" + selectedTable : selectedTable);

        if (currentOrder == null) {
            VBox emptyState = new VBox(12);
            emptyState.setAlignment(Pos.CENTER);
            VBox.setVgrow(emptyState, Priority.ALWAYS);
            Label lblEmptyTitle = new Label(tblLabel + " hiện chưa có hóa đơn chờ thanh toán");
            lblEmptyTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #112823;");
            Label lblEmptySub = new Label("Hãy chọn một bàn đang phục vụ ở cột bên trái hoặc mở thực đơn để gọi món mới cho " + tblLabel + ".");
            lblEmptySub.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #78716C;");
            Button btnOrderNow = new Button("+ Gọi món cho " + tblLabel);
            btnOrderNow.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 10 20 10 20; -fx-cursor: hand;");
            btnOrderNow.setOnAction(e -> switchStaffTab("MENU"));
            emptyState.getChildren().addAll(lblEmptyTitle, lblEmptySub, btnOrderNow);
            rightBillCard.getChildren().add(emptyState);
        } else {
            HBox billHeader = new HBox(10);
            billHeader.setAlignment(Pos.CENTER_LEFT);
            Label lblBillTitle = new Label("PHIẾU THANH TOÁN • " + tblLabel.toUpperCase() + " (Đơn #" + currentOrder.getOrderId() + ")");
            lblBillTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #112823;");
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            Button btnPreviewReceipt = new Button("🖨 Xem & In Phiếu Nhiệt");
            btnPreviewReceipt.setStyle("-fx-background-color: #F4EFE6; -fx-text-fill: #112823; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 7 14 7 14; -fx-cursor: hand;");
            btnPreviewReceipt.setOnAction(e -> OrderSuccessDialog.showReceiptForOrder(parentStage, currentOrder, lblStaffName.getText()));
            billHeader.getChildren().addAll(lblBillTitle, sp, btnPreviewReceipt);

            VBox itemRows = new VBox(8);
            for (OrderItem item : currentOrder.getItems()) {
                HBox r = new HBox(10);
                r.setAlignment(Pos.CENTER_LEFT);
                r.setPadding(new Insets(8, 12, 8, 12));
                r.setStyle("-fx-background-color: #FAF8F5; -fx-background-radius: 8px;");
                Label lQtyName = new Label(item.getQty() + "x  " + item.getName() + (item.getCustomizationSummary().isEmpty() ? "" : " (" + item.getCustomizationSummary() + ")"));
                lQtyName.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #1C1917;");
                Region rSp = new Region();
                HBox.setHgrow(rSp, Priority.ALWAYS);
                Label lPrice = new Label(CurrencyFormatter.formatVND(item.getSubtotal()));
                lPrice.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #112823;");
                r.getChildren().addAll(lQtyName, rSp, lPrice);
                itemRows.getChildren().add(r);
            }

            ScrollPane itemScroll = new ScrollPane(itemRows);
            itemScroll.setFitToWidth(true);
            itemScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            VBox.setVgrow(itemScroll, Priority.ALWAYS);

            // Payment method selector
            HBox methodRow = new HBox(10);
            methodRow.setAlignment(Pos.CENTER_LEFT);
            Label lblMethod = new Label("Phương thức thu:");
            lblMethod.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #57534E;");
            ToggleGroup tg = new ToggleGroup();
            RadioButton rbCash = new RadioButton("💵 Tiền mặt");
            rbCash.setToggleGroup(tg);
            rbCash.setSelected(true);
            RadioButton rbQr = new RadioButton("📱 Chuyển khoản VietQR");
            rbQr.setToggleGroup(tg);
            RadioButton rbCard = new RadioButton("💳 Thẻ POS");
            rbCard.setToggleGroup(tg);
            methodRow.getChildren().addAll(lblMethod, rbCash, rbQr, rbCard);

                        VBox qrPosBox = new VBox(6);
            qrPosBox.setAlignment(Pos.CENTER);
            qrPosBox.setPadding(new Insets(8, 12, 8, 12));
            qrPosBox.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8px; -fx-border-color: #E2DDD5; -fx-border-radius: 8px; -fx-border-width: 1px;");
            qrPosBox.setVisible(false);
            qrPosBox.setManaged(false);

            try {
                java.io.InputStream is = getClass().getResourceAsStream("/cafe/resources/images/vietqr_mb.png");
                if (is != null) {
                    javafx.scene.image.ImageView ivQr = new javafx.scene.image.ImageView(new javafx.scene.image.Image(is));
                    ivQr.setFitWidth(130);
                    ivQr.setPreserveRatio(true);
                    ivQr.setSmooth(true);
                    Label lblQrInfo = new Label("MB Bank • STK: 0909 888 999 • " + CurrencyFormatter.formatVND(currentOrder.getTotalAmount()));
                    lblQrInfo.setStyle("-fx-font-family: 'Segoe UI', 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #112823;");
                    qrPosBox.getChildren().addAll(ivQr, lblQrInfo);
                }
            } catch (Exception ignored) {}

            rbQr.setOnAction(e -> { qrPosBox.setVisible(true); qrPosBox.setManaged(true); });
            rbCash.setOnAction(e -> { qrPosBox.setVisible(false); qrPosBox.setManaged(false); });
            rbCard.setOnAction(e -> { qrPosBox.setVisible(false); qrPosBox.setManaged(false); });

VBox summaryBox = new VBox(6);
            summaryBox.setPadding(new Insets(12, 16, 12, 16));
            summaryBox.setStyle("-fx-background-color: #F8F5F0; -fx-background-radius: 10px;");
            summaryBox.getChildren().addAll(
                createSummaryRow("Tổng tiền món (" + currentOrder.getTotalQuantity() + " món):", CurrencyFormatter.formatVND(currentOrder.getSubtotalAmount()), false),
                createSummaryRow("Giảm giá / Voucher:", "-" + CurrencyFormatter.formatVND(currentOrder.getDiscountAmount()), false),
                createSummaryRow("TỔNG CỘNG CẦN THU:", CurrencyFormatter.formatVND(currentOrder.getTotalAmount()), true)
            );

            HBox bottomActions = new HBox(12);
            bottomActions.setAlignment(Pos.CENTER_RIGHT);

            Button btnAddMoreItems = new Button("+ Gọi thêm món");
            btnAddMoreItems.setStyle("-fx-background-color: #EAE4DA; -fx-text-fill: #112823; -fx-font-size: 13px; -fx-font-weight: 700; -fx-background-radius: 10px; -fx-padding: 12 20 12 20; -fx-cursor: hand;");
            btnAddMoreItems.setOnAction(e -> switchStaffTab("MENU"));

            Button btnCompleteCheckout = new Button("✓ Xác nhận Đã thu tiền & Trả bàn trống (" + CurrencyFormatter.formatVND(currentOrder.getTotalAmount()) + ")");
            btnCompleteCheckout.setStyle("-fx-background-color: #15803D; -fx-text-fill: #FFFFFF; -fx-font-size: 13.5px; -fx-font-weight: 800; -fx-background-radius: 10px; -fx-padding: 12 24 12 24; -fx-cursor: hand;");
            btnCompleteCheckout.setOnAction(e -> {
                OrderSuccessDialog.showReceiptForOrder(parentStage, currentOrder, lblStaffName.getText());
                server.checkoutTable(selectedTable);
                tableStatusMap.put(selectedTable, "EMPTY");
                showStaffToast("Đã thanh toán thành công " + tblLabel + " và giải phóng bàn trống!");
                switchStaffTab("FLOOR");
            });

            bottomActions.getChildren().addAll(btnAddMoreItems, btnCompleteCheckout);
            rightBillCard.getChildren().addAll(billHeader, itemScroll, methodRow, summaryBox, bottomActions);
        }

        body.getChildren().addAll(leftTablesCard, rightBillCard);
        panel.getChildren().addAll(header, body);
        return panel;
    }

    // =========================================================================
    // PHÂN HỆ 5: CÀI ĐẶT CA TRỰC & THIẾT BỊ NHÂN VIÊN (EMBEDDED TAB 5)
    // =========================================================================
    private VBox buildEmbeddedStaffSettingsPanel() {
        VBox panel = new VBox(18);
        panel.setPadding(new Insets(24, 30, 24, 30));
        panel.setStyle("-fx-background-color: #F4EFE6;");
        VBox.setVgrow(panel, Priority.ALWAYS);

        Label lblTitle = new Label("Hồ Sơ Ca Trực & Thiết Bị POS Nhân Viên");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        HBox cardsRow = new HBox(18);

        // Card 1: Thông tin nhân sự đang trực
        VBox staffCard = new VBox(10);
        staffCard.setPrefWidth(380);
        staffCard.setPadding(new Insets(20));
        staffCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 14px; -fx-border-color: #E4DDD3; -fx-border-radius: 14px;");

        Label lblCard1 = new Label("NHÂN VIÊN ĐANG TRỰC CA");
        lblCard1.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #78716C;");
        Label lblNameBig = new Label(lblStaffName.getText());
        lblNameBig.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        Label lblRoleBig = new Label("Vai trò: " + lblStaffRole.getText());
        lblRoleBig.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #B45309;");
        Label lblShift = new Label("Ca làm việc: Ca Hành Chính / Phục Vụ Sảnh 3 Tầng\nTrạng thái kết nối KDS: ● Đang trực tuyến (" + cafe.utils.NetworkConfig.getServerHost() + ":" + cafe.utils.NetworkConfig.getServerPort() + ")");
        lblShift.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #57534E;");

        staffCard.getChildren().addAll(lblCard1, lblNameBig, lblRoleBig, lblShift);

        // Card 2: Thao tác nhanh & Chuyển quyền Admin (nếu là Quản lý)
        VBox toolsCard = new VBox(12);
        HBox.setHgrow(toolsCard, Priority.ALWAYS);
        toolsCard.setPadding(new Insets(20));
        toolsCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 14px; -fx-border-color: #E4DDD3; -fx-border-radius: 14px;");

        Label lblCard2 = new Label("CÔNG CỤ VẬN HÀNH CA TRỰC");
        lblCard2.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #78716C;");

        Button btnResetFloor = new Button("↻ Khôi phục dữ liệu mẫu Sơ đồ bàn (Seed Data)");
        btnResetFloor.setStyle("-fx-background-color: #F4EFE6; -fx-text-fill: #112823; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 10 16 10 16; -fx-cursor: hand;");
        btnResetFloor.setOnAction(e -> {
            CafeServerService.getInstance().resetToSeedState();
            syncStatusesFromServer();
            showStaffToast("Đã khôi phục trạng thái mẫu cho toàn bộ 3 tầng bàn!");
            switchStaffTab("FLOOR");
        });

        toolsCard.getChildren().addAll(lblCard2, btnResetFloor);

        if (onOpenAdminDashboard != null) {
            Button btnSwitchAdmin = new Button("⚡ Mở Trung Tâm Quản Trị Admin (10 Phân Hệ Điều Hành)");
            btnSwitchAdmin.setStyle("-fx-background-color: #112823; -fx-text-fill: #E8C798; -fx-font-size: 13px; -fx-font-weight: 800; -fx-background-radius: 8px; -fx-padding: 12 18 12 18; -fx-cursor: hand;");
            btnSwitchAdmin.setOnAction(e -> onOpenAdminDashboard.run());
            toolsCard.getChildren().add(btnSwitchAdmin);
        }

        Button btnLogoutShift = new Button("🚪 Kết thúc ca trực & Đăng xuất");
        btnLogoutShift.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 10 16 10 16; -fx-cursor: hand;");
        btnLogoutShift.setOnAction(e -> {
            if (onLogout != null) onLogout.run();
        });
        toolsCard.getChildren().add(btnLogoutShift);

        cardsRow.getChildren().addAll(staffCard, toolsCard);
        panel.getChildren().addAll(lblTitle, cardsRow);
        return panel;
    }

    // =========================================================================
    // HỘP THOẠI NGHIỆP VỤ BÀN: CHUYỂN BÀN, GỘP BÀN, ĐẶT BÀN TRƯỚC
    // =========================================================================
    private void showTransferTableModal(int fromTable) {
        Stage modal = createStyledModal("Chuyển Bàn Phục Vụ");
        VBox root = new VBox(14);
        root.setPadding(new Insets(22));
        root.setPrefWidth(420);
        root.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16px; -fx-border-color: #DFD7CA; -fx-border-radius: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 24, 0.15, 0, 8);");

        String fromCode = fromTable < 10 ? "0" + fromTable : String.valueOf(fromTable);
        Label lblTitle = new Label("⇄ Chuyển khách từ Bàn " + fromCode);
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 19px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        Label lblSub = new Label("Chọn bàn đích đang trống để chuyển toàn bộ hóa đơn & lịch đặt của Bàn " + fromCode + ":");
        lblSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #57534E;");
        lblSub.setWrapText(true);

        ComboBox<Integer> cboTarget = new ComboBox<>();
        cboTarget.setMaxWidth(Double.MAX_VALUE);
        int[] allTables = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 201, 202, 203, 204, 205, 206, 207, 208, 301, 302, 303, 304, 305, 306};
        for (int t : allTables) {
            String st = tableStatusMap.getOrDefault(t, "EMPTY");
            if (t != fromTable && "EMPTY".equalsIgnoreCase(st)) {
                cboTarget.getItems().add(t);
            }
        }
        if (!cboTarget.getItems().isEmpty()) {
            cboTarget.getSelectionModel().selectFirst();
        }

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button btnCancel = new Button("Hủy");
        btnCancel.setStyle("-fx-background-color: #EAE4DA; -fx-text-fill: #1C1917; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> modal.close());

        Button btnConfirm = new Button("Xác nhận Chuyển bàn");
        btnConfirm.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 8 18 8 18; -fx-cursor: hand;");
        btnConfirm.setOnAction(e -> {
            Integer toTable = cboTarget.getValue();
            if (toTable != null) {
                CafeServerService.getInstance().transferTable(fromTable, toTable);
                syncStatusesFromServer();
                this.selectedTable = toTable;
                renderFloorPlan();
                updateTableTitleDisplay();
                refreshInspectorDrawer();
                modal.close();
                showStaffToast("Đã chuyển thành công từ Bàn " + fromCode + " sang Bàn " + (toTable < 10 ? "0" + toTable : toTable) + "!");
            }
        });

        actions.getChildren().addAll(btnCancel, btnConfirm);
        root.getChildren().addAll(lblTitle, lblSub, cboTarget, actions);
        showModalScene(modal, root);
    }

    private void showMergeTableModal(int currentTbl) {
        Stage modal = createStyledModal("Gộp Hóa Đơn Bàn");
        VBox root = new VBox(14);
        root.setPadding(new Insets(22));
        root.setPrefWidth(430);
        root.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16px; -fx-border-color: #DFD7CA; -fx-border-radius: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 24, 0.15, 0, 8);");

        String code = currentTbl < 10 ? "0" + currentTbl : String.valueOf(currentTbl);
        Label lblTitle = new Label("🔗 Gộp hóa đơn Bàn " + code + " vào bàn khác");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 19px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        Label lblSub = new Label("Chọn bàn đích để cộng dồn toàn bộ các món của Bàn " + code + " sang và giải phóng Bàn " + code + " về trạng thái Trống:");
        lblSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #57534E;");
        lblSub.setWrapText(true);

        ComboBox<Integer> cboTarget = new ComboBox<>();
        cboTarget.setMaxWidth(Double.MAX_VALUE);
        int[] allTables = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 201, 202, 203, 204, 205, 206, 207, 208, 301, 302, 303, 304, 305, 306};
        for (int t : allTables) {
            if (t != currentTbl) {
                cboTarget.getItems().add(t);
            }
        }
        if (!cboTarget.getItems().isEmpty()) {
            cboTarget.getSelectionModel().selectFirst();
        }

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button btnCancel = new Button("Hủy");
        btnCancel.setStyle("-fx-background-color: #EAE4DA; -fx-text-fill: #1C1917; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> modal.close());

        Button btnConfirm = new Button("Xác nhận Gộp bàn");
        btnConfirm.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 8 18 8 18; -fx-cursor: hand;");
        btnConfirm.setOnAction(e -> {
            Integer target = cboTarget.getValue();
            if (target != null) {
                CafeServerService.getInstance().mergeTables(currentTbl, target);
                syncStatusesFromServer();
                this.selectedTable = target;
                renderFloorPlan();
                updateTableTitleDisplay();
                refreshInspectorDrawer();
                modal.close();
                showStaffToast("Đã gộp Bàn " + code + " vào Bàn " + (target < 10 ? "0" + target : target) + "!");
            }
        });

        actions.getChildren().addAll(btnCancel, btnConfirm);
        root.getChildren().addAll(lblTitle, lblSub, cboTarget, actions);
        showModalScene(modal, root);
    }

    private void showReservationModal(int tableNum) {
        Stage modal = createStyledModal("Đặt Bàn Trước (Reservation)");
        VBox root = new VBox(12);
        root.setPadding(new Insets(22));
        root.setPrefWidth(430);
        root.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16px; -fx-border-color: #DFD7CA; -fx-border-radius: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 24, 0.15, 0, 8);");

        String code = tableNum < 10 ? "0" + tableNum : String.valueOf(tableNum);
        Label lblTitle = new Label("📅 Đặt trước Bàn " + code);
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 19px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        TextField txtName = new TextField("Nguyễn Minh Quân");
        txtName.setPromptText("Tên khách đặt bàn...");
        txtName.setStyle("-fx-background-color: #FAF8F5; -fx-border-color: #DFD8CE; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8 12 8 12;");

        TextField txtPhone = new TextField("0903.668.899");
        txtPhone.setPromptText("Số điện thoại liên hệ...");
        txtPhone.setStyle("-fx-background-color: #FAF8F5; -fx-border-color: #DFD8CE; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8 12 8 12;");

        HBox rowTimeGuests = new HBox(10);
        TextField txtTime = new TextField("19:30");
        txtTime.setPromptText("Giờ hẹn (VD: 19:30)");
        txtTime.setStyle("-fx-background-color: #FAF8F5; -fx-border-color: #DFD8CE; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8 12 8 12;");
        HBox.setHgrow(txtTime, Priority.ALWAYS);

        Spinner<Integer> spnGuests = new Spinner<>(1, 16, 4);
        spnGuests.setPrefWidth(120);
        rowTimeGuests.getChildren().addAll(txtTime, spnGuests);

        TextField txtNote = new TextField("Khách yêu cầu bàn yên tĩnh");
        txtNote.setPromptText("Ghi chú đặc biệt...");
        txtNote.setStyle("-fx-background-color: #FAF8F5; -fx-border-color: #DFD8CE; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8 12 8 12;");

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button btnCancel = new Button("Hủy");
        btnCancel.setStyle("-fx-background-color: #EAE4DA; -fx-text-fill: #1C1917; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> modal.close());

        Button btnConfirm = new Button("Lưu Lịch Đặt Bàn");
        btnConfirm.setStyle("-fx-background-color: #B45309; -fx-text-fill: #FFFFFF; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 8 18 8 18; -fx-cursor: hand;");
        btnConfirm.setOnAction(e -> {
            String name = txtName.getText().trim().isEmpty() ? "Khách đặt trước" : txtName.getText().trim();
            String phone = txtPhone.getText().trim();
            String time = txtTime.getText().trim().isEmpty() ? "19:30" : txtTime.getText().trim();
            int guests = spnGuests.getValue();
            String note = txtNote.getText().trim();

            CafeServerService.getInstance().reserveTable(tableNum, name, phone, time, guests, note);
            tableStatusMap.put(tableNum, "RESERVED");
            renderFloorPlan();
            updateTableTitleDisplay();
            refreshInspectorDrawer();
            modal.close();
            showStaffToast("Đã lưu lịch đặt trước Bàn " + code + " lúc " + time + " cho " + name + "!");
        });

        actions.getChildren().addAll(btnCancel, btnConfirm);
        root.getChildren().addAll(
            lblTitle,
            new Label("Họ tên khách hàng:"), txtName,
            new Label("Số điện thoại:"), txtPhone,
            new Label("Giờ nhận bàn & Số lượng khách:"), rowTimeGuests,
            new Label("Ghi chú:"), txtNote,
            actions
        );
        showModalScene(modal, root);
    }

    private Stage createStyledModal(String title) {
        Stage modal = new Stage();
        if (parentStage != null) modal.initOwner(parentStage);
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initStyle(StageStyle.TRANSPARENT);
        modal.setTitle(title);
        return modal;
    }

    private void showModalScene(Stage modal, VBox root) {
        AdminSubViews.styleInputs(root);

        Button btnClose = new Button("✕");
        btnClose.setStyle("-fx-background-color: #F5EFEB; -fx-text-fill: #4A453E; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 999px; -fx-min-width: 28px; -fx-min-height: 28px; -fx-max-width: 28px; -fx-max-height: 28px; -fx-padding: 0; -fx-cursor: hand;");
        btnClose.setOnAction(e -> modal.close());

        cafe.utils.WindowChromeHelper.makeDraggable(modal, root);
        StackPane card = new StackPane(root, btnClose);
        StackPane.setAlignment(btnClose, Pos.TOP_RIGHT);
        StackPane.setMargin(btnClose, new Insets(14, 14, 0, 0));
        card.setStyle("-fx-background-color: transparent;");

        StackPane wrap = new StackPane(card);
        wrap.setPadding(new Insets(16));
        wrap.setStyle("-fx-background-color: transparent;");
        Scene scene = new Scene(wrap);
        scene.setFill(Color.TRANSPARENT);
        cafe.utils.WindowChromeHelper.applyTheme(scene);
        modal.setScene(scene);
        modal.show();
        AnimationHelper.playDialogEntrance(card);
    }

    // =========================================================================
    // DỰNG MẶT BẰNG KIẾN TRÚC 3 TẦNG (CHUẨN HÓA BÀN 01 -> 12 & LANDMARKS)
    // =========================================================================
    private void selectFloor(int floor) {
        this.currentFloor = floor;
        updateFloorTabStyles();
        renderFloorPlan();
    }

    private void updateFloorTabStyles() {
        String activeStyle = "-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-size: 11.5px; -fx-font-weight: 700; -fx-background-radius: 6px; -fx-padding: 6 14 6 14; -fx-cursor: hand;";
        String inactiveStyle = "-fx-background-color: #EAE3D8; -fx-text-fill: #57534E; -fx-font-size: 11.5px; -fx-font-weight: 600; -fx-background-radius: 6px; -fx-padding: 6 14 6 14; -fx-cursor: hand;";

        if (tabF1 != null) tabF1.setStyle(currentFloor == 1 ? activeStyle : inactiveStyle);
        if (tabF2 != null) tabF2.setStyle(currentFloor == 2 ? activeStyle : inactiveStyle);
        if (tabF3 != null) tabF3.setStyle(currentFloor == 3 ? activeStyle : inactiveStyle);
    }

    private void setTableFilter(String filter, Button bAll, Button bEmpty, Button bServing) {
        this.currentTableFilter = filter;
        applyFilterPillStyle(bAll, "ALL".equals(filter));
        applyFilterPillStyle(bEmpty, "EMPTY".equals(filter));
        applyFilterPillStyle(bServing, "SERVING".equals(filter));
        renderFloorPlan();
    }

    private Button createFilterPill(String title, String iconFileName, boolean isActive) {
        Button btn = new Button();
        btn.setPadding(new Insets(5, 12, 5, 12));
        btn.setCursor(javafx.scene.Cursor.HAND);
        AnimationHelper.applyButtonPress(btn);

        HBox content = new HBox(6);
        content.setAlignment(Pos.CENTER);
        Node icon = cafe.utils.IconHelper.getIcon("staff", iconFileName, null, "●", 15, isActive ? Color.WHITE : Color.web("#112823"));
        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: " + (isActive ? "#FFFFFF" : "#292722") + ";");
        content.getChildren().addAll(icon, lbl);
        btn.setGraphic(content);

        applyFilterPillStyle(btn, isActive);
        return btn;
    }

        private void applyFilterPillStyle(Button btn, boolean isActive) {
        if (btn == null) return;
        if (isActive) {
            btn.setStyle(
                "-fx-background-color: #112823; " +
                "-fx-background-radius: 8px; " +
                "-fx-cursor: hand;"
            );
        } else {
            btn.setStyle(
                "-fx-background-color: #FFFFFF; " +
                "-fx-background-radius: 8px; " +
                "-fx-border-color: #DED7CD; " +
                "-fx-border-radius: 8px; " +
                "-fx-border-width: 1px; " +
                "-fx-cursor: hand;"
            );
        }

        if (btn.getGraphic() instanceof HBox) {
            HBox content = (HBox) btn.getGraphic();
            for (Node child : content.getChildren()) {
                if (child instanceof Label) {
                    ((Label) child).setStyle(
                        "-fx-font-size: 12px; -fx-font-weight: 700; " +
                        "-fx-text-fill: " + (isActive ? "#FFFFFF" : "#3F3931") + ";"
                    );
                } else if (child instanceof SVGPath) {
                    ((SVGPath) child).setFill(isActive ? Color.WHITE : Color.web("#112823"));
                    ((SVGPath) child).setStroke(isActive ? Color.WHITE : Color.web("#112823"));
                } else if (child instanceof javafx.scene.Parent) {
                    for (Node inner : ((javafx.scene.Parent) child).getChildrenUnmodifiable()) {
                        if (inner instanceof SVGPath) {
                            ((SVGPath) inner).setFill(isActive ? Color.WHITE : Color.web("#112823"));
                            ((SVGPath) inner).setStroke(isActive ? Color.WHITE : Color.web("#112823"));
                        } else if (inner instanceof Label) {
                            ((Label) inner).setStyle("-fx-text-fill: " + (isActive ? "#FFFFFF" : "#112823") + ";");
                        }
                    }
                }
            }
        }
    }

    private Button createToolButton(String title, String iconFileName) {
        Button btn = new Button();
        btn.setPadding(new Insets(5, 12, 5, 12));
        btn.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 6px; -fx-border-color: #DED7CD; -fx-border-radius: 6px; -fx-border-width: 1px; -fx-cursor: hand;");
        AnimationHelper.applyButtonPress(btn);

        HBox content = new HBox(6);
        content.setAlignment(Pos.CENTER);
        Node icon = cafe.utils.IconHelper.getIcon("staff", iconFileName, null, "⚙", 15, Color.web("#112823"));
        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 600; -fx-text-fill: #292722;");
        content.getChildren().addAll(icon, lbl);
        btn.setGraphic(content);

        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #F8F4EE; -fx-background-radius: 6px; -fx-border-color: #112823; -fx-border-radius: 6px; -fx-border-width: 1px; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 6px; -fx-border-color: #DED7CD; -fx-border-radius: 6px; -fx-border-width: 1px; -fx-cursor: hand;"));

        return btn;
    }

    private void renderFloorPlan() {
        floorCanvas.getChildren().clear();
        tableNodes.clear();

        double canvasWidth = floorCanvas.getWidth();
        double canvasHeight = floorCanvas.getHeight();
        if (canvasWidth < 800) canvasWidth = 850;
        if (canvasHeight < 500) canvasHeight = 550;

        double marginX = 20;
        double availW = canvasWidth - 2 * marginX;
        double colW = availW / 4.0;

        // Kích thước chuẩn sau nâng cấp: Bàn đơn 139x109 (gồm ghế), Bàn dài 234x119 (gồm ghế)
        double tableNodeW = 139;
        double tableNodeH = 109;
        double largeNodeW = 234;
        double largeNodeH = 119;

        // Phân bổ trục X tâm cột
        double col1 = marginX + (colW - tableNodeW) / 2.0;
        double col2 = marginX + colW + (colW - tableNodeW) / 2.0;
        double col3 = marginX + colW * 2 + (colW - tableNodeW) / 2.0;
        double col4 = marginX + colW * 3 + (colW - tableNodeW) / 2.0;

        // Header & Footer Landmark spaces
        double landmarkH = 38;
        double headerY = 14;
        double contentStartY = headerY + landmarkH + 12;
        double footerSpaceH = 46;
        double availContentH = canvasHeight - contentStartY - footerSpaceH;
        double rowStepY = Math.max(115, availContentH / 4.0);

        double row1 = contentStartY + (rowStepY - tableNodeH) / 2.0;
        double row2 = row1 + rowStepY;
        double row3 = row2 + rowStepY;
        double row4 = row3 + rowStepY;

        // 1. Chậu cây render 3D góc phòng (Upscale lên 88x88px)
        double plantRightX = canvasWidth - marginX - 90;
        Node plantTopRight = create3DPlantNode(plantRightX, 12, 88);
        Node plantBottomRight = create3DPlantNode(plantRightX, canvasHeight - 100, 88);
        floorCanvas.getChildren().addAll(plantTopRight, plantBottomRight);

        if (currentFloor == 1) {
            // Kiến trúc Tầng 1: Quầy Pha Chế (68% bề ngang) + Lối Vào Chính (30% bề ngang)
            double counterW = Math.max(380, availW * 0.68);
            double entranceW = Math.max(160, availW - counterW - 12);
            floorCanvas.getChildren().addAll(
                createArchitecturalLandmark("☕ QUẦY PHA CHẾ BARISTA & THU NGÂN POS", marginX, headerY, counterW, landmarkH, "#E2D9CC", "#563521"),
                createArchitecturalLandmark("🚪 LỐI VÀO CHÍNH", marginX + counterW + 12, headerY, entranceW, landmarkH, "#E4ECE2", "#112823")
            );

            // TẦNG 1: Đủ 12 bàn liên tục 01 -> 12 phân bổ cân đối 100% diện tích
            // Hàng 1: Bàn 01, 02, 03, 04
            addTable(1, "01", col1, row1, false, "SERVING");
            addTable(2, "02", col2, row1, false, "EMPTY");
            addTable(3, "03", col3, row1, false, "SERVING");
            addTable(4, "04", col4, row1, false, "EMPTY");

            // Hàng 2: Bàn 05, 06, 07, 08
            addTable(5, "05", col1, row2, false, "RESERVED");
            addTable(6, "06", col2, row2, false, "SERVING");
            addTable(7, "07", col3, row2, false, "EMPTY");
            addTable(8, "08", col4, row2, false, "EMPTY");

            // Hàng 3: Bàn 09, 10
            addTable(9, "09", col1, row3, false, "SERVING");
            addTable(10, "10", col2, row3, false, "RESERVED");

            // Hàng 3: Bàn dài 11 (Sofa) dàn đều 2 cột 3 & 4
            double col34Span = marginX + colW * 2 + (colW * 2 - largeNodeW) / 2.0;
            addTable(11, "Bàn 11 • Sofa", col34Span, row3, true, "EMPTY");

            // Hàng 4: Bàn dài 12 (Sảnh Kính) dàn đều 2 cột 1 & 2
            double col12Span = marginX + (colW * 2 - largeNodeW) / 2.0;
            addTable(12, "Bàn 12 • Sảnh Kính", col12Span, row4, true, "SERVING");

            // Hàng 4: Hành lang kính nhìn ra phố trải rộng phía dưới góc phải
            double glassW = Math.max(220, colW * 2 - 14);
            floorCanvas.getChildren().add(
                createArchitecturalLandmark("🪟 HÀNH LANG KÍNH NHÌN RA PHỐ", marginX + colW * 2, row4 + 30, glassW, 36, "#EAE3D8", "#6F695F")
            );
        } else if (currentFloor == 2) {
            // TẦNG 2: Ban công & Phòng đọc sách
            double readW = Math.max(340, availW * 0.62);
            double balcW = Math.max(180, availW - readW - 12);
            floorCanvas.getChildren().addAll(
                createArchitecturalLandmark("📚 KHU VỰC PHÒNG ĐỌC SÁCH & LÀM VIỆC", marginX, headerY, readW, landmarkH, "#E2D9CC", "#563521"),
                createArchitecturalLandmark("🌿 BAN CÔNG TẦNG 2", marginX + readW + 12, headerY, balcW, landmarkH, "#E4ECE2", "#112823")
            );

            addTable(201, "201", col1, row1, false, "SERVING");
            addTable(202, "202", col2, row1, false, "RESERVED");
            addTable(203, "203", col3, row1, false, "EMPTY");

            addTable(204, "204", col1, row2, false, "EMPTY");
            addTable(205, "205", col2, row2, false, "SERVING");
            addTable(206, "206", col3, row2, false, "EMPTY");

            double col12Span = marginX + (colW * 2 - largeNodeW) / 2.0;
            double col34Span = marginX + colW * 2 + (colW * 2 - largeNodeW) / 2.0;
            addTable(207, "Bàn 207 • Ban Công", col12Span, row3 + 10, true, "EMPTY");
            addTable(208, "Bàn 208 • Họp Nhóm", col34Span, row3 + 10, true, "EMPTY");
        } else {
            // TẦNG 3: Sân thượng Rooftop Zen Garden
            Node plantTopLeft = create3DPlantNode(marginX + 10, headerY + 45, 80);
            floorCanvas.getChildren().add(plantTopLeft);

            double zenW = Math.max(340, availW * 0.62);
            double barW = Math.max(180, availW - zenW - 12);
            floorCanvas.getChildren().addAll(
                createArchitecturalLandmark("🎍 VƯỜN THIỀN ROOFTOP ZEN GARDEN", marginX + 35, headerY, zenW, landmarkH, "#E4ECE2", "#112823"),
                createArchitecturalLandmark("🍹 QUẦY BAR SUNSET", marginX + zenW + 45, headerY, barW, landmarkH, "#E2D9CC", "#563521")
            );

            addTable(301, "301", col1 + 25, row1, false, "EMPTY");
            addTable(302, "302", col2 + 25, row1, false, "SERVING");
            addTable(303, "303", col3 + 25, row1, false, "EMPTY");

            addTable(304, "304", col1 + 25, row2, false, "EMPTY");
            addTable(305, "305", col2 + 25, row2, false, "SERVING");

            double centerSpan = marginX + colW + (colW * 2 - largeNodeW) / 2.0;
            addTable(306, "Bàn 306 • Bar Sunset", centerSpan, row3 + 15, true, "EMPTY");
        }
    }

    // =========================================================================
    // MODAL NGHIỆP VỤ CA TRỰC & BÀN GIAO KÉT TIỀN (PLAN UI VER 6.0)
    // =========================================================================
    public void showStaffShiftModal(Stage ownerStage, StaffModel staff) {
        Stage modal = new Stage();
        if (ownerStage != null) modal.initOwner(ownerStage);
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initStyle(StageStyle.TRANSPARENT);
        modal.setTitle("Hồ Sơ Nhân Viên & Bàn Giao Ca Trực");

        VBox root = new VBox(14);
        root.setPadding(new Insets(24));
        root.setPrefWidth(680);
        root.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16px; -fx-border-color: #DFD7CA; -fx-border-radius: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 24, 0.15, 0, 8);");

        Label lblTitle = new Label("📋 Hồ Sơ Nhân Viên & Quản Lý Ca Trực (Shift Drawer)");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        // Staff Info Card
        HBox staffInfo = new HBox(14);
        staffInfo.setAlignment(Pos.CENTER_LEFT);
        staffInfo.setPadding(new Insets(12, 16, 12, 16));
        staffInfo.setStyle("-fx-background-color: #FAF8F5; -fx-background-radius: 10px; -fx-border-color: #EFEAE2; -fx-border-radius: 10px;");

        Circle ava = new Circle(22, Color.web("#112823"));
        String initials = "NV";
        String sName = (staff != null) ? staff.getFullName() : lblStaffName.getText();
        String sRole = (staff != null) ? staff.getRoleDisplayName() : lblStaffRole.getText();
        int sId = (staff != null) ? staff.getStaffId() : 3;
        String sUser = (staff != null) ? staff.getUsername() : "cashier1";

        if (sName != null && !sName.trim().isEmpty()) {
            String[] p = sName.trim().split("\\s+");
            initials = p.length >= 2 ? ("" + p[0].charAt(0) + p[p.length - 1].charAt(0)).toUpperCase() : sName.substring(0, Math.min(2, sName.length())).toUpperCase();
        }
        Label lblAva = new Label(initials);
        lblAva.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #FFFFFF;");
        StackPane avaP = new StackPane(ava, lblAva);

        VBox nameBox = new VBox(2);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        Label lFullName = new Label(sName + " (Mã NV: NV-0" + sId + ")");
        lFullName.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        Label lRoleShift = new Label("Chức danh: " + sRole + "  •  Tài khoản: " + sUser + "  •  Đăng nhập: 06:45 hôm nay");
        lRoleShift.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #6F695F;");
        nameBox.getChildren().addAll(lFullName, lRoleShift);

        Label badgeActive = new Label("● Đang hoạt động");
        badgeActive.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-font-size: 11px; -fx-font-weight: 800; -fx-background-radius: 6px; -fx-padding: 4 8 4 8;");
        badgeActive.setMinWidth(Region.USE_PREF_SIZE);
        staffInfo.getChildren().addAll(avaP, nameBox, badgeActive);

        // Revenue Audit Section
        CafeServerService server = CafeServerService.getInstance();
        double cashFloat = 1_000_000.0;
        int completedCount = 0;
        double cashSales = 0;
        double transferSales = 0;

        for (Order o : server.getAllOrdersNewestFirst()) {
            String st = o.getStatus() != null ? o.getStatus().toUpperCase() : "";
            if (st.contains("READY") || st.contains("DONE") || st.contains("PAID")) {
                completedCount++;
                String note = (o.getNote() != null) ? o.getNote() : "";
                if (note.contains("VietQR") || note.contains("TRANSFER") || note.contains("Chuyển khoản")) {
                    transferSales += o.getFinalAmount();
                } else {
                    cashSales += o.getFinalAmount();
                }
            }
        }
        if (completedCount == 0) {
            completedCount = 28;
            cashSales = 2_450_000.0;
            transferSales = 3_820_000.0;
        }

        final int finalCompletedCount = completedCount;
        final double finalCashSales = cashSales;
        final double finalTransferSales = transferSales;
        double theoreticalCash = cashFloat + cashSales;

        VBox auditCard = new VBox(6);
        auditCard.setPadding(new Insets(12, 16, 12, 16));
        auditCard.setStyle("-fx-background-color: #FAF8F5; -fx-background-radius: 10px; -fx-border-color: #EFEAE2; -fx-border-radius: 10px;");

        Label lblAuditTitle = new Label("📊 ĐỐI SOÁT DOANH THU CA HIỆN TẠI (CA SÁNG: 06:30 - 14:30)");
        lblAuditTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #78716C; -fx-letter-spacing: 0.6px;");

        auditCard.getChildren().addAll(
            lblAuditTitle,
            createAuditRow("1. Tiền mặt ban đầu trong két (Cash Float):", CurrencyFormatter.formatVND(cashFloat), false),
            createAuditRow("2. Tổng số hóa đơn đã xử lý thành công:", completedCount + " đơn hàng", false),
            createAuditRow("3. Doanh số thanh toán TIỀN MẶT trong ca:", CurrencyFormatter.formatVND(cashSales), false),
            createAuditRow("4. Doanh số thanh toán CHUYỂN KHOẢN (VietQR):", CurrencyFormatter.formatVND(transferSales), false)
        );

        Region line = new Region();
        line.setPrefHeight(1);
        line.setStyle("-fx-background-color: #E2DACD;");
        auditCard.getChildren().add(line);

        HBox theoRow = createAuditRow("=> TỔNG TIỀN MẶT LÝ THUYẾT PHẢI CÓ TRONG KÉT:", CurrencyFormatter.formatVND(theoreticalCash), true);
        auditCard.getChildren().add(theoRow);

        // Handover Box
        VBox handoverBox = new VBox(8);
        handoverBox.setPadding(new Insets(12, 16, 12, 16));
        handoverBox.setStyle("-fx-background-color: #F8F5F0; -fx-background-radius: 10px;");

        Label lblHandoverTitle = new Label("💵 BÀN GIAO TIỀN MẶT KHI RA CA");
        lblHandoverTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #78716C; -fx-letter-spacing: 0.6px;");

        HBox countRow = new HBox(10);
        countRow.setAlignment(Pos.CENTER_LEFT);
        Label lblActual = new Label("Số tiền mặt thực tế kiểm đếm (đ):");
        lblActual.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #112823;");
        TextField txtActualCash = new TextField(String.valueOf((long) theoreticalCash));
        txtActualCash.setPrefWidth(140);
        txtActualCash.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #DFD8CE; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-font-weight: 800; -fx-font-size: 13px; -fx-padding: 6 10 6 10;");

        Label lblDiff = new Label("Chênh lệch: 0 đ (Khớp 100%)");
        lblDiff.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #15803D;");

        final double finalTheoretical = theoreticalCash;
        txtActualCash.textProperty().addListener((obs, oldV, newV) -> {
            try {
                double act = Double.parseDouble(newV.trim().replace(".", "").replace(",", ""));
                double diff = act - finalTheoretical;
                if (diff == 0) {
                    lblDiff.setText("Chênh lệch: 0 đ (Khớp 100%)");
                    lblDiff.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #15803D;");
                } else if (diff > 0) {
                    lblDiff.setText("Chênh lệch: +" + CurrencyFormatter.formatVND(diff) + " (Thừa tiền két)");
                    lblDiff.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #B45309;");
                } else {
                    lblDiff.setText("Chênh lệch: " + CurrencyFormatter.formatVND(diff) + " (Thiếu tiền két!)");
                    lblDiff.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #B91C1C;");
                }
            } catch (Exception ex) {
                lblDiff.setText("Số tiền không hợp lệ");
                lblDiff.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #B91C1C;");
            }
        });
        countRow.getChildren().addAll(lblActual, txtActualCash, lblDiff);

        HBox noteRow = new HBox(10);
        noteRow.setAlignment(Pos.CENTER_LEFT);
        Label lblNote = new Label("Ghi chú ca trực:");
        lblNote.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #57534E;");
        TextField txtNotes = new TextField("Ca sáng hoàn thành tốt, bàn 05 đã đặt trước.");
        txtNotes.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #DFD8CE; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-font-size: 12px; -fx-padding: 6 10 6 10;");
        HBox.setHgrow(txtNotes, Priority.ALWAYS);
        noteRow.getChildren().addAll(lblNote, txtNotes);

        handoverBox.getChildren().addAll(lblHandoverTitle, countRow, noteRow);

        // Actions Row
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnPin = new Button("🔑 Đổi mã PIN");
        btnPin.setStyle("-fx-background-color: #F5EFEB; -fx-text-fill: #112823; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 8 14 8 14; -fx-cursor: hand;");
        btnPin.setOnAction(e -> showChangePinModal(modal, staff));

        Button btnPrintZ = new Button("🖨 In Báo Cáo Ca (Z-Report)");
        btnPrintZ.setStyle("-fx-background-color: #F5EFEB; -fx-text-fill: #112823; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 8 14 8 14; -fx-cursor: hand;");
        btnPrintZ.setOnAction(e -> {
            showStaffToast("Đang in báo cáo Z-Report ca trực ra máy in K80...");
            Order sampleOrder = new Order(8888, 0, "SHIFT_Z_REPORT",
                java.util.Collections.singletonList(new OrderItem("Báo cáo kết ca trực (Z-Report)", 1, "Tổng đơn: " + finalCompletedCount, finalCashSales + finalTransferSales)),
                "PAID", 0, 0, null, "Z-Report ca trực của " + sName, System.currentTimeMillis());
            OrderSuccessDialog.showReceiptForOrder(modal, sampleOrder, sName);
        });

        Button btnLogout = new Button("🚪 KẾT CA & ĐĂNG XUẤT");
        btnLogout.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 800; -fx-background-radius: 8px; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
        btnLogout.setOnAction(e -> {
            modal.close();
            if (onLogout != null) onLogout.run();
        });

        Button btnClose = new Button("Đóng");
        btnClose.setStyle("-fx-background-color: #EAE4DA; -fx-text-fill: #1C1917; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 8 14 8 14; -fx-cursor: hand;");
        btnClose.setOnAction(e -> modal.close());

        actions.setAlignment(Pos.CENTER_LEFT);

        HBox leftActions = new HBox(8);
        leftActions.setAlignment(Pos.CENTER_LEFT);
        leftActions.getChildren().addAll(btnPin, btnPrintZ);

        Region spacerActions = new Region();
        HBox.setHgrow(spacerActions, Priority.ALWAYS);

        HBox rightActions = new HBox(8);
        rightActions.setAlignment(Pos.CENTER_RIGHT);
        rightActions.getChildren().addAll(btnLogout, btnClose);

        actions.getChildren().addAll(leftActions, spacerActions, rightActions);

        root.getChildren().addAll(lblTitle, staffInfo, auditCard, handoverBox, actions);

        cafe.utils.WindowChromeHelper.makeDraggable(modal, root);
        modal.setScene(new javafx.scene.Scene(root));
        modal.getScene().setFill(javafx.scene.paint.Color.TRANSPARENT);
        modal.show();
    }

    private HBox createAuditRow(String label, String value, boolean isBold) {
        HBox r = new HBox(10);
        r.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(label);
        l.setStyle("-fx-font-size: " + (isBold ? "13px" : "12px") + "; -fx-font-weight: " + (isBold ? "800" : "600") + "; -fx-text-fill: " + (isBold ? "#112823" : "#57534E") + ";");
        HBox.setHgrow(l, Priority.ALWAYS);
        Label v = new Label(value);
        v.setStyle("-fx-font-size: " + (isBold ? "14px" : "12.5px") + "; -fx-font-weight: 800; -fx-text-fill: " + (isBold ? "#15803D" : "#112823") + ";");
        r.getChildren().addAll(l, v);
        return r;
    }

    public void showChangePinModal(Stage ownerStage, StaffModel staff) {
        Stage modal = new Stage();
        if (ownerStage != null) modal.initOwner(ownerStage);
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initStyle(StageStyle.TRANSPARENT);
        modal.setTitle("Đổi Mã PIN Cá Nhân");

        VBox root = new VBox(12);
        root.setPadding(new Insets(22));
        root.setPrefWidth(380);
        root.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16px; -fx-border-color: #DFD7CA; -fx-border-radius: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 24, 0.15, 0, 8);");

        Label lblTitle = new Label("🔑 Đổi Mã PIN / Mật Khẩu");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 19px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        Label lblStaff = new Label("Nhân viên: " + ((staff != null) ? staff.getFullName() : lblStaffName.getText()));
        lblStaff.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #57534E;");

        Label lblOld = new Label("Mã PIN / Mật khẩu hiện tại:");
        lblOld.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #112823;");
        javafx.scene.control.PasswordField txtOld = new javafx.scene.control.PasswordField();
        txtOld.setStyle("-fx-background-color: #FAF8F5; -fx-border-color: #DFD8CE; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8 10 8 10; -fx-font-size: 13px;");

        Label lblNew = new Label("Mã PIN mới (tối thiểu 4 số):");
        lblNew.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #112823;");
        javafx.scene.control.PasswordField txtNew = new javafx.scene.control.PasswordField();
        txtNew.setStyle("-fx-background-color: #FAF8F5; -fx-border-color: #DFD8CE; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8 10 8 10; -fx-font-size: 13px;");

        Label lblConfirm = new Label("Xác nhận mã PIN mới:");
        lblConfirm.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #112823;");
        javafx.scene.control.PasswordField txtConfirm = new javafx.scene.control.PasswordField();
        txtConfirm.setStyle("-fx-background-color: #FAF8F5; -fx-border-color: #DFD8CE; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8 10 8 10; -fx-font-size: 13px;");

        Label lblErr = new Label();
        lblErr.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #B91C1C;");
        lblErr.setVisible(false);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button btnCancel = new Button("Hủy");
        btnCancel.setStyle("-fx-background-color: #EAE4DA; -fx-text-fill: #1C1917; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 8 14 8 14; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> modal.close());

        Button btnSave = new Button("Lưu Mã PIN Mới");
        btnSave.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: 800; -fx-background-radius: 8px; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
        btnSave.setOnAction(e -> {
            String oP = txtOld.getText().trim();
            String nP = txtNew.getText().trim();
            String cP = txtConfirm.getText().trim();

            if (oP.isEmpty() || nP.isEmpty()) {
                lblErr.setText("Vui lòng điền đầy đủ thông tin.");
                lblErr.setVisible(true);
                return;
            }
            if (nP.length() < 4) {
                lblErr.setText("Mã PIN mới phải có tối thiểu 4 ký tự.");
                lblErr.setVisible(true);
                return;
            }
            if (!nP.equals(cP)) {
                lblErr.setText("Xác nhận mã PIN không trùng khớp!");
                lblErr.setVisible(true);
                return;
            }
            if (staff != null) {
                cafe.services.DatabaseManager.updateStaffPassword(staff.getStaffId(), nP);
            }
            modal.close();
            showStaffToast("Đã đổi mã PIN thành công cho nhân viên " + ((staff != null) ? staff.getFullName() : ""));
        });
        actions.getChildren().addAll(btnCancel, btnSave);

        root.getChildren().addAll(lblTitle, lblStaff, lblOld, txtOld, lblNew, txtNew, lblConfirm, txtConfirm, lblErr, actions);
        cafe.utils.WindowChromeHelper.makeDraggable(modal, root);
        modal.setScene(new javafx.scene.Scene(root));
        modal.getScene().setFill(javafx.scene.paint.Color.TRANSPARENT);
        modal.show();
    }

    private HBox createArchitecturalLandmark(String text, double x, double y, double w, double h, String bgHex, String fgHex) {
        HBox box = new HBox();
        box.setAlignment(Pos.CENTER);
        box.setLayoutX(x);
        box.setLayoutY(y);
        box.setPrefSize(w, h);
        box.setStyle(
            "-fx-background-color: " + bgHex + "; " +
            "-fx-background-radius: 8px; " +
            "-fx-border-color: rgba(17,40,35,0.14); " +
            "-fx-border-radius: 8px; " +
            "-fx-border-width: 1px;"
        );
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 800; -fx-text-fill: " + fgHex + "; -fx-letter-spacing: 0.6px;");
        box.getChildren().add(lbl);
        return box;
    }

    private void addTable(int num, String labelText, double x, double y, boolean isLarge, String defaultStatus) {
        boolean isSelected = (num == selectedTable);
        CafeServerService server = CafeServerService.getInstance();
        String effectiveStatus = tableStatusMap.getOrDefault(num, defaultStatus);

        Order activeOrd = server.getCombinedOrderForTable(num);
        if (activeOrd != null) {
            effectiveStatus = "SERVING";
            tableStatusMap.put(num, "SERVING");
        }

        TableWithChairsNode node = new TableWithChairsNode(
            num,
            labelText,
            isSelected,
            effectiveStatus,
            isLarge,
            () -> {
                this.selectedTable = num;
                updateTableTitleDisplay();
                renderFloorPlan();
                refreshInspectorDrawer();
                if (onTableSelected != null) {
                    onTableSelected.accept(num);
                }
            },
            () -> {
                // Double click: mở thẳng thực đơn đặt món ngay trong Unified Staff Shell
                this.selectedTable = num;
                if (onTableSelected != null) {
                    onTableSelected.accept(num);
                }
                switchStaffTab("MENU");
            }
        );

        // Gắn thông tin tóm tắt trực tiếp lên mặt bàn (Tổng tiền / Giờ đặt)
        if (activeOrd != null) {
            long kAmount = Math.round(activeOrd.getTotalAmount() / 1000.0);
            node.setLiveSummary(kAmount + "k • " + activeOrd.getTotalQuantity() + " món");
        } else if ("RESERVED".equalsIgnoreCase(effectiveStatus)) {
            CafeServerService.ReservationInfo r = server.getReservation(num);
            if (r != null) {
                node.setLiveSummary("Đặt " + r.reservationTime);
            }
        }

        node.setLayoutX(x);
        node.setLayoutY(y);

        if (!"ALL".equals(currentTableFilter)) {
            if ("EMPTY".equals(currentTableFilter) && !"EMPTY".equalsIgnoreCase(effectiveStatus)) {
                node.setOpacity(0.22);
            } else if ("SERVING".equals(currentTableFilter) && !"SERVING".equalsIgnoreCase(effectiveStatus) && !"OCCUPIED".equalsIgnoreCase(effectiveStatus)) {
                node.setOpacity(0.22);
            } else {
                node.setOpacity(1.0);
            }
        } else {
            node.setOpacity(1.0);
        }

        tableNodes.add(node);
        floorCanvas.getChildren().add(node);
    }

    private void updateTableTitleDisplay() {
        lblCurrentTableTitle.setText("Bàn " + (selectedTable < 10 ? "0" + selectedTable : selectedTable));
        String st = tableStatusMap.getOrDefault(selectedTable, "EMPTY");
        if ("SERVING".equalsIgnoreCase(st) || "OCCUPIED".equalsIgnoreCase(st)) {
            lblCurrentTableStatus.setText("● Đang phục vụ");
            lblCurrentTableStatus.setStyle("-fx-background-color: #172E27; -fx-text-fill: #E8C798; -fx-font-size: 11px; -fx-font-weight: 700; -fx-background-radius: 999px; -fx-padding: 4 12 4 12;");
        } else if ("RESERVED".equalsIgnoreCase(st)) {
            lblCurrentTableStatus.setText("● Đặt trước");
            lblCurrentTableStatus.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #92400E; -fx-font-size: 11px; -fx-font-weight: 700; -fx-background-radius: 999px; -fx-padding: 4 12 4 12;");
        } else if ("CLEANING".equalsIgnoreCase(st)) {
            lblCurrentTableStatus.setText("● Chờ dọn bàn");
            lblCurrentTableStatus.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-font-size: 11px; -fx-font-weight: 700; -fx-background-radius: 999px; -fx-padding: 4 12 4 12;");
        } else {
            lblCurrentTableStatus.setText("● Bàn trống (Đang chọn)");
            lblCurrentTableStatus.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-font-size: 11px; -fx-font-weight: 700; -fx-background-radius: 999px; -fx-padding: 4 12 4 12;");
        }
    }

    public void setTableStatus(int tableNum, String status) {
        if (status == null) return;
        String mapped = status;
        if ("OCCUPIED".equalsIgnoreCase(status)) mapped = "SERVING";
        else if ("AVAILABLE".equalsIgnoreCase(status)) mapped = "EMPTY";
        tableStatusMap.put(tableNum, mapped);
        javafx.application.Platform.runLater(() -> {
            renderFloorPlan();
            updateTableTitleDisplay();
            refreshInspectorDrawer();
        });
    }

    public void setStaffProfile(StaffModel staff) {
        this.currentStaff = staff;
        if (staff != null) {
            lblStaffName.setText(staff.getFullName());
            lblStaffRole.setText(staff.getRoleDisplayName());
            String initials = "NV";
            String[] parts = staff.getFullName().trim().split("\\s+");
            if (parts.length > 0 && !parts[parts.length - 1].isEmpty()) {
                initials = parts[parts.length - 1].substring(0, Math.min(2, parts[parts.length - 1].length())).toUpperCase();
            }
            lblStaffAvaText.setText(initials);
        }
    }

    private HBox createLegendItem(String labelText, String colorHex) {
        HBox item = new HBox(6);
        item.setAlignment(Pos.CENTER_LEFT);

        Circle dot = new Circle(4.5, Color.web(colorHex));
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #6F695F;");

        item.getChildren().addAll(dot, lbl);
        return item;
    }

    public int getSelectedTable() {
        return selectedTable;
    }

    public void setSelectedTable(int tableNum) {
        this.selectedTable = tableNum;
        if (tableNum >= 300) {
            this.currentFloor = 3;
        } else if (tableNum >= 200) {
            this.currentFloor = 2;
        } else {
            this.currentFloor = 1;
        }
        updateFloorTabStyles();
        updateTableTitleDisplay();
        renderFloorPlan();
        refreshInspectorDrawer();
    }

    private static Image cachedPlant3DImage;

    private static synchronized Image getPlant3DImage(double size) {
        if (cachedPlant3DImage != null) {
            return cachedPlant3DImage;
        }
        try {
            java.net.URL url = FloorPlanView.class.getResource("/cafe/resources/images/decor/plant_3d.png");
            if (url == null) {
                url = FloorPlanView.class.getResource("/resources/images/decor/plant_3d.png");
            }
            if (url != null) {
                cachedPlant3DImage = new Image(url.toExternalForm(), size * 2, size * 2, true, true, true);
                return cachedPlant3DImage;
            }
            java.io.File file = new java.io.File("src/cafe/resources/images/decor/plant_3d.png");
            if (file.exists()) {
                cachedPlant3DImage = new Image(file.toURI().toString(), size * 2, size * 2, true, true, true);
                return cachedPlant3DImage;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Node create3DPlantNode(double x, double y, double size) {
        ImageView imgView = new ImageView();
        Image img = getPlant3DImage(size);
        if (img != null) {
            imgView.setImage(img);
        }

        imgView.setFitWidth(size);
        imgView.setFitHeight(size);
        imgView.setPreserveRatio(true);
        imgView.setSmooth(true);
        imgView.setLayoutX(x);
        imgView.setLayoutY(y);

        imgView.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.rgb(0, 0, 0, 0.15), 10, 0.05, 2, 4));

        return imgView;
    }
}
