package cafe.views;

import cafe.utils.AnimationHelper;
import cafe.utils.SvgIcons;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * FloorPlanView - Giao diện Nhân viên / Sơ đồ Bàn (Screen 2 trong UI_mong_muon.png).
 * - Left Navigation Rail (105px): Đủ đúng 5 mục: Bàn (Active #DED5C8), Thực đơn, Đơn hàng, Thanh toán, Cài đặt
 * - Top Bar: Bàn 05 (Playfair Display), Badge "Đang phục vụ" (#DCE8D9 / #597256), Avatar Trần Minh Anh, Nút CTA
 * - Bộ lọc tầng: [ Tầng 1 ] (Active #112823), [ Tầng 2 ], [ Tầng 3 ]
 * - Mặt bằng bàn 2.5D: Các bàn có khối ghế bao quanh (TableWithChairsNode) chuẩn kiến trúc, cây xanh góc phòng
 * - Status Legend Bar: Trống (#D9D0C3), Đang phục vụ (#E5A04D), Đặt trước (#86A88A), Bảo trì (#4B1717)
 */
public class FloorPlanView extends BorderPane {

    private int selectedTable = 5;
    private int currentFloor = 1;

    private final Label lblCurrentTableTitle = new Label("Bàn 05");
    private final Label lblCurrentTableStatus = new Label("● Đang phục vụ");
    private final Button btnGoToMenu = new Button("Vào thực đơn đặt món →");

    private Button tabF1;
    private Button tabF2;
    private Button tabF3;

    private final Pane floorCanvas = new Pane();
    private final List<TableWithChairsNode> tableNodes = new ArrayList<>();

    private final Label lblStaffName = new Label("Trần Minh Anh");
    private final Label lblStaffRole = new Label("Nhân viên");
    private final Label lblStaffAvaText = new Label("NV");
    private final java.util.Map<Integer, String> tableStatusMap = new java.util.concurrent.ConcurrentHashMap<>();
    private String currentTableFilter = "ALL";

    private Consumer<Integer> onTableSelected;
    private Runnable onNavigateToMenu;
    private Runnable onLogout;
    private Runnable onOpenOrders;

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

        initUI();
    }

    public void setOnOpenOrders(Runnable onOpenOrders) {
        this.onOpenOrders = onOpenOrders;
    }

    private void initUI() {
        setStyle("-fx-background-color: #F5F0E8;");

        // 1. LEFT NAVIGATION RAIL (105px)
        VBox leftRail = buildLeftNavigationRail();
        setLeft(leftRail);

        // 2. MAIN FLOOR PLAN AREA
        VBox centerArea = buildCenterFloorArea();
        setCenter(centerArea);

        updateTableTitleDisplay();
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
        lblBrand.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 17.5px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        Label lblSub = new Label("STAFF POS • SƠ ĐỒ BÀN");
        lblSub.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #563521; -fx-letter-spacing: 1.5px;");
        brandBox.getChildren().addAll(lblBrand, lblSub);

        // Chuẩn danh mục 5 mục Staff POS
        VBox navMenu = new VBox(5);

        // 1. Bàn (Active: nền #DED5C8, chữ than #112823)
        Button btnFloor = createNavButton("Bàn", "so-do-ban.png", SvgIcons.TABLE, "🪑", true);

        // 2. Thực đơn
        Button btnMenu = createNavButton("Thực đơn", "thuc-don.png", SvgIcons.MENU, "📋", false);
        btnMenu.setOnAction(e -> {
            if (onNavigateToMenu != null) onNavigateToMenu.run();
        });

        // 3. Đơn hàng
        Button btnOrders = createNavButton("Đơn hàng", "don-hang.png", SvgIcons.ORDERS, "🧾", false);
        btnOrders.setOnAction(e -> {
            if (onOpenOrders != null) {
                onOpenOrders.run();
            } else {
                OrdersDialog dialog = new OrdersDialog();
                dialog.show();
            }
        });

        // 4. Thanh toán
        Button btnPayment = createNavButton("Thanh toán", "thanh-toan.png", SvgIcons.WALLET, "💳", false);
        btnPayment.setOnAction(e -> {
            if (onNavigateToMenu != null) onNavigateToMenu.run();
        });

        // 5. Cài đặt
        Button btnSettings = createNavButton("Cài đặt", "cai-dat.png", SvgIcons.SETTINGS, "⚙️", false);

        navMenu.getChildren().addAll(btnFloor, btnMenu, btnOrders, btnPayment, btnSettings);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Thẻ hồ sơ nhân viên trực ca dưới đáy Sidebar
        HBox profileCard = new HBox(10);
        profileCard.setAlignment(Pos.CENTER_LEFT);
        profileCard.setPadding(new Insets(10, 10, 10, 10));
        profileCard.setStyle("-fx-background-color: #EFE8DD; -fx-background-radius: 8px; -fx-cursor: hand;");

        Circle avatar = new Circle(16, Color.web("#112823"));
        lblStaffAvaText.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #FFFFFF;");
        StackPane avaPane = new StackPane(avatar, lblStaffAvaText);

        VBox profTexts = new VBox(2);
        lblStaffName.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        lblStaffRole.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #6F695F;");
        profTexts.getChildren().addAll(lblStaffName, lblStaffRole);

        profileCard.getChildren().addAll(avaPane, profTexts);

        // Đăng xuất ở dưới cùng
        Button btnExit = createNavButton("Đăng xuất", "dang-xuat.png", SvgIcons.USER, "🚪", false);
        btnExit.setOnAction(e -> {
            if (onLogout != null) onLogout.run();
        });

        rail.getChildren().addAll(brandBox, navMenu, spacer, profileCard, btnExit);
        return rail;
    }

    private Button createNavButton(String title, String iconFileName, String iconSvg, String fallbackEmoji, boolean isActive) {
        Button btn = new Button();
        btn.setFocusTraversable(false);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(10, 14, 10, 14));
        AnimationHelper.applyButtonPress(btn);

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        // Khung chứa icon cố định tuyệt đối 26x26 px -> Thẳng tắp 1 hàng dọc theo trục
        StackPane iconContainer = new StackPane();
        iconContainer.setPrefSize(26, 26);
        iconContainer.setMinSize(26, 26);
        iconContainer.setMaxSize(26, 26);
        iconContainer.setAlignment(Pos.CENTER);

        Node iconNode = getIconNode(iconFileName, iconSvg, fallbackEmoji, isActive);
        iconContainer.getChildren().add(iconNode);

        Label label = new Label(title);
        label.setStyle("-fx-font-size: 13.5px; -fx-font-weight: " + (isActive ? "800" : "700") + "; -fx-text-fill: " + (isActive ? "#112823" : "#3F3931") + ";");

        row.getChildren().addAll(iconContainer, label);
        btn.setGraphic(row);

        if (isActive) {
            btn.setStyle("-fx-background-color: #DED5C8; -fx-background-radius: 8px; -fx-cursor: hand;");
        } else {
            btn.setStyle("-fx-background-color: transparent; -fx-background-radius: 8px; -fx-cursor: hand;");
            btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #EFE8DD; -fx-background-radius: 8px; -fx-cursor: hand;"));
            btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-background-radius: 8px; -fx-cursor: hand;"));
        }

        return btn;
    }

    private Node getIconNode(String iconFileName, String svgFallback, String fallbackEmoji, boolean isActive) {
        Color color = isActive ? Color.web("#112823") : Color.web("#3F3931");
        return cafe.utils.IconHelper.getIcon("staff", iconFileName, svgFallback, fallbackEmoji, 20, color);
    }

    private VBox buildCenterFloorArea() {
        VBox center = new VBox(12);
        center.setPadding(new Insets(16, 22, 16, 22));
        HBox.setHgrow(center, Priority.ALWAYS);

        // 1. Top Bar: Table info, Badge "Đang phục vụ", Avatar Trần Minh Anh, CTA Button
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        lblCurrentTableTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        lblCurrentTableStatus.setStyle("-fx-background-color: #DCE8D9; -fx-text-fill: #597256; -fx-font-size: 10px; -fx-font-weight: 600; -fx-background-radius: 10px; -fx-padding: 2 8 2 8;");
        titleBox.getChildren().addAll(lblCurrentTableTitle, lblCurrentTableStatus);

        Region sp1 = new Region();
        HBox.setHgrow(sp1, Priority.ALWAYS);

        // Profile Avatar (Trần Minh Anh - Nhân viên v)
        HBox profileBox = new HBox(7);
        profileBox.setAlignment(Pos.CENTER_RIGHT);
        Circle avatarCircle = new Circle(14, Color.web("#DED7CD"));
        VBox profileTexts = new VBox(0);
        HBox nameRow = new HBox(3);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        lblStaffName.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        Label lblDown = new Label("▾");
        lblDown.setStyle("-fx-font-size: 9px; -fx-text-fill: #817A70;");
        nameRow.getChildren().addAll(lblStaffName, lblDown);

        lblStaffRole.setStyle("-fx-font-size: 9.5px; -fx-text-fill: #817A70;");
        profileTexts.getChildren().addAll(nameRow, lblStaffRole);
        profileBox.getChildren().addAll(avatarCircle, profileTexts);

        btnGoToMenu.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-size: 11.5px; -fx-font-weight: 600; -fx-background-radius: 5px; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
        AnimationHelper.applyButtonPress(btnGoToMenu);
        btnGoToMenu.setOnAction(e -> {
            if (onNavigateToMenu != null) onNavigateToMenu.run();
        });

        topBar.getChildren().addAll(titleBox, sp1, profileBox, btnGoToMenu);

        // 2. Floor & Table Status Toolbar với các icon Staff mới (Tất cả, Bàn trống, Có khách, Làm mới, Chuyển bàn, Gộp bàn)
        HBox floorToolbar = new HBox(12);
        floorToolbar.setAlignment(Pos.CENTER_LEFT);

        // Floor selector tabs
        HBox floorTabs = new HBox(6);
        floorTabs.setAlignment(Pos.CENTER_LEFT);

        tabF1 = new Button("Tầng 1");
        tabF2 = new Button("Tầng 2");
        tabF3 = new Button("Tầng 3");

        tabF1.setOnAction(e -> selectFloor(1));
        tabF2.setOnAction(e -> selectFloor(2));
        tabF3.setOnAction(e -> selectFloor(3));

        updateFloorTabStyles();
        floorTabs.getChildren().addAll(tabF1, tabF2, tabF3);

        // Table status filter pills (tat-ca-ban.png, ban-trong.png, co-khach.png)
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

        // Quick Table Action Tools (lam-moi.png, chuyen-ban.png, tach-gop-ban.png)
        HBox actionTools = new HBox(8);
        actionTools.setAlignment(Pos.CENTER_RIGHT);

        Button btnRefresh = createToolButton("Làm mới", "lam-moi.png");
        btnRefresh.setOnAction(e -> {
            AnimationHelper.applyButtonPress(btnRefresh);
            renderFloorPlan();
        });

        Button btnTransfer = createToolButton("Chuyển bàn", "chuyen-ban.png");
        btnTransfer.setOnAction(e -> AnimationHelper.applyButtonPress(btnTransfer));

        Button btnMerge = createToolButton("Gộp bàn", "tach-gop-ban.png");
        btnMerge.setOnAction(e -> AnimationHelper.applyButtonPress(btnMerge));

        actionTools.getChildren().addAll(btnRefresh, btnTransfer, btnMerge);

        floorToolbar.getChildren().addAll(floorTabs, statusFilterTabs, tbSp, actionTools);

        // 3. Isometric 2.5D Floor Canvas (#EDE5DC, bo góc 20px chuẩn Phan_tich_CafeOrderv3.md)
        floorCanvas.setPrefHeight(510);
        floorCanvas.setMinHeight(460);
        floorCanvas.setStyle(
            "-fx-background-color: #EDE5DC; " +
            "-fx-background-radius: 20px; " +
            "-fx-border-color: #DFD5C8; " +
            "-fx-border-radius: 20px; " +
            "-fx-border-width: 1px; " +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.04), 16, 0.08, 0, 4);"
        );
        VBox.setVgrow(floorCanvas, Priority.ALWAYS);

        // Responsive auto-centering khi canvas thay đổi chiều rộng
        floorCanvas.widthProperty().addListener((obs, oldW, newW) -> {
            if (newW != null && newW.doubleValue() > 0) {
                renderFloorPlan();
            }
        });

        renderFloorPlan();

        // 4. Status Legend Bar at Bottom
        HBox legendBar = new HBox(16);
        legendBar.setAlignment(Pos.CENTER_LEFT);
        legendBar.setPadding(new Insets(4, 2, 2, 2));

        legendBar.getChildren().addAll(
            createLegendItem("Trống", "#D9D0C3"),
            createLegendItem("Đang phục vụ", "#E5A04D"),
            createLegendItem("Đặt trước", "#86A88A"),
            createLegendItem("Bảo trì", "#4B1717")
        );

        center.getChildren().addAll(topBar, floorToolbar, floorCanvas, legendBar);
        return center;
    }

    private void selectFloor(int floor) {
        this.currentFloor = floor;
        updateFloorTabStyles();
        renderFloorPlan();
    }

    private void updateFloorTabStyles() {
        String activeStyle = "-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-size: 11.5px; -fx-font-weight: 600; -fx-background-radius: 5px; -fx-padding: 5 14 5 14; -fx-cursor: hand;";
        String inactiveStyle = "-fx-background-color: transparent; -fx-text-fill: #6F695F; -fx-font-size: 11.5px; -fx-font-weight: 500; -fx-background-radius: 5px; -fx-padding: 5 14 5 14; -fx-cursor: hand;";

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
        if (isActive) {
            btn.setStyle("-fx-background-color: #112823; -fx-background-radius: 6px;");
        } else {
            btn.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 6px; -fx-border-color: #DED7CD; -fx-border-radius: 6px; -fx-border-width: 1px;");
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
        double offsetX = canvasWidth > 760 ? (canvasWidth - 760) / 2.0 : 24;

        // 1. Chậu cây render 3D góc phòng (plant_3d.png) chuẩn Phan_tich_CafeOrderv3.md
        double plantRightX = offsetX + 650;
        Node plantTopRight = create3DPlantNode(plantRightX, 25, 72);
        Node plantBottomRight = create3DPlantNode(plantRightX, 350, 72);
        floorCanvas.getChildren().addAll(plantTopRight, plantBottomRight);

        double col1 = offsetX + 50;
        double col2 = offsetX + 240;
        double col3 = offsetX + 430;

        if (currentFloor == 1) {
            // TẦNG 1: Khớp bố cục Screen 2 trong UI_mong_muon.png
            // Row 1: 01, 02, 03
            addTable(1, "01", col1, 40, false, "EMPTY");
            addTable(2, "02", col2, 40, false, "EMPTY");
            addTable(3, "03", col3, 40, false, "EMPTY");

            // Row 2: 05 (Active), 09, 07
            addTable(5, "05", col1, 145, false, "SERVING");
            addTable(6, "09", col2, 145, false, "EMPTY");
            addTable(7, "07", col3, 145, false, "EMPTY");

            // Row 3: 08, 10, 09
            addTable(8, "08", col1, 250, false, "EMPTY");
            addTable(10, "10", col2, 250, false, "EMPTY");
            addTable(9, "09", col3, 250, false, "EMPTY");

            // Row 4: Bàn 11 (bàn dài) và Bàn 12 (Sảnh)
            addTable(11, "11", offsetX + 70, 355, true, "EMPTY");
            addTable(12, "Sảnh  12", offsetX + 310, 355, true, "SERVING");
        } else if (currentFloor == 2) {
            // TẦNG 2: Ban công & Phòng đọc sách
            addTable(201, "201", col1, 45, false, "EMPTY");
            addTable(202, "202", col2, 45, false, "RESERVED");
            addTable(203, "203", col3, 45, false, "EMPTY");

            addTable(204, "204", col1, 150, false, "EMPTY");
            addTable(205, "205", col2, 150, false, "SERVING");
            addTable(206, "206", col3, 150, false, "EMPTY");

            addTable(207, "Bàn 207 • Ban Công", offsetX + 70, 275, true, "EMPTY");
            addTable(208, "Bàn 208 • Họp Nhóm", offsetX + 310, 275, true, "EMPTY");
        } else {
            // TẦNG 3: Sân thượng Rooftop Zen Garden
            Node plantTopLeft = create3DPlantNode(offsetX + 20, 35, 68);
            floorCanvas.getChildren().add(plantTopLeft);

            addTable(301, "301", col1, 55, false, "EMPTY");
            addTable(302, "302", col2, 55, false, "SERVING");
            addTable(303, "303", col3, 55, false, "EMPTY");

            addTable(304, "304", col1, 165, false, "EMPTY");
            addTable(305, "305", col2, 165, false, "EMPTY");

            addTable(306, "Bàn 306 • Bar Sunset", offsetX + 190, 280, true, "EMPTY");
        }
    }

    private void addTable(int num, String labelText, double x, double y, boolean isLarge, String status) {
        boolean isSelected = (num == selectedTable);
        String effectiveStatus = tableStatusMap.getOrDefault(num, status);

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
                if (onTableSelected != null) {
                    onTableSelected.accept(num);
                }
            },
            () -> {
                // Double click: vào thẳng thực đơn
                if (onNavigateToMenu != null) {
                    onNavigateToMenu.run();
                }
            }
        );

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
            lblCurrentTableStatus.setStyle("-fx-background-color: #DCE8D9; -fx-text-fill: #597256; -fx-font-size: 10px; -fx-font-weight: 600; -fx-background-radius: 10px; -fx-padding: 2 8 2 8;");
        } else if ("RESERVED".equalsIgnoreCase(st)) {
            lblCurrentTableStatus.setText("● Đặt trước");
            lblCurrentTableStatus.setStyle("-fx-background-color: #E2ECE1; -fx-text-fill: #4B704F; -fx-font-size: 10px; -fx-font-weight: 600; -fx-background-radius: 10px; -fx-padding: 2 8 2 8;");
        } else {
            lblCurrentTableStatus.setText("● Trống");
            lblCurrentTableStatus.setStyle("-fx-background-color: #EFE8DD; -fx-text-fill: #817A70; -fx-font-size: 10px; -fx-font-weight: 600; -fx-background-radius: 10px; -fx-padding: 2 8 2 8;");
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
        });
    }

    public void setStaffProfile(cafe.models.StaffModel staff) {
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

        Circle dot = new Circle(4, Color.web(colorHex));
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size: 10.5px; -fx-text-fill: #817A70;");

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

