package cafe.views;

import cafe.utils.AnimationHelper;
import cafe.utils.SvgIcons;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
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
        rail.setPrefWidth(110);
        rail.setMinWidth(100);
        rail.setMaxWidth(120);
        rail.setPadding(new Insets(16, 8, 16, 8));
        rail.setStyle("-fx-background-color: #F7F2EA; -fx-border-color: transparent #DED7CD transparent transparent; -fx-border-width: 0 1 0 0;");

        // Brand Logo
        VBox brandBox = new VBox(1);
        brandBox.setAlignment(Pos.CENTER);
        brandBox.setPadding(new Insets(0, 0, 14, 0));

        Label lblBrand = new Label("Brew & Bean");
        lblBrand.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        Label lblSub = new Label("C A F E");
        lblSub.setStyle("-fx-font-size: 8px; -fx-font-weight: 700; -fx-text-fill: #563521; -fx-letter-spacing: 2px;");
        brandBox.getChildren().addAll(lblBrand, lblSub);

        // Đúng chuẩn 5 mục của Screen 2 trong UI_mong_muon.png:
        VBox navMenu = new VBox(5);

        // 1. Bàn (Active: nền #DED5C8, chữ than #292722)
        Button btnFloor = createNavButton("Bàn", SvgIcons.TABLE, true);

        // 2. Thực đơn
        Button btnMenu = createNavButton("Thực đơn", SvgIcons.MENU, false);
        btnMenu.setOnAction(e -> {
            if (onNavigateToMenu != null) onNavigateToMenu.run();
        });

        // 3. Đơn hàng
        Button btnOrders = createNavButton("Đơn hàng", SvgIcons.ORDERS, false);
        btnOrders.setOnAction(e -> {
            if (onOpenOrders != null) {
                onOpenOrders.run();
            } else {
                OrdersDialog dialog = new OrdersDialog();
                dialog.show();
            }
        });

        // 4. Thanh toán
        Button btnPayment = createNavButton("Thanh toán", SvgIcons.WALLET, false);
        btnPayment.setOnAction(e -> {
            if (onNavigateToMenu != null) onNavigateToMenu.run();
        });

        // 5. Cài đặt
        Button btnSettings = createNavButton("Cài đặt", SvgIcons.SETTINGS, false);

        navMenu.getChildren().addAll(btnFloor, btnMenu, btnOrders, btnPayment, btnSettings);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Đăng xuất ở dưới cùng
        Button btnExit = createNavButton("Đăng xuất", SvgIcons.USER, false);
        btnExit.setOnAction(e -> {
            if (onLogout != null) onLogout.run();
        });

        rail.getChildren().addAll(brandBox, navMenu, spacer, btnExit);
        return rail;
    }

    private Button createNavButton(String title, String iconSvg, boolean isActive) {
        Button btn = new Button();
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(8, 10, 8, 10));
        AnimationHelper.applyButtonPress(btn);

        HBox content = new HBox(7);
        content.setAlignment(Pos.CENTER_LEFT);

        SVGPath icon = SvgIcons.createOutlineIcon(iconSvg, 0.78, isActive ? Color.web("#292722") : Color.web("#6F695F"));
        Label label = new Label(title);
        label.setStyle("-fx-font-size: 11.5px; -fx-font-weight: " + (isActive ? "700" : "500") + "; -fx-text-fill: " + (isActive ? "#292722" : "#6F695F") + ";");

        content.getChildren().addAll(icon, label);
        btn.setGraphic(content);

        if (isActive) {
            btn.setStyle("-fx-background-color: #DED5C8; -fx-background-radius: 6px; -fx-cursor: hand;");
        } else {
            btn.setStyle("-fx-background-color: transparent; -fx-background-radius: 6px; -fx-cursor: hand;");
            btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #EFE8DD; -fx-background-radius: 6px; -fx-cursor: hand;"));
            btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-background-radius: 6px; -fx-cursor: hand;"));
        }

        return btn;
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
        Label lblStaffName = new Label("Trần Minh Anh");
        lblStaffName.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        Label lblDown = new Label("▾");
        lblDown.setStyle("-fx-font-size: 9px; -fx-text-fill: #817A70;");
        nameRow.getChildren().addAll(lblStaffName, lblDown);

        Label lblStaffRole = new Label("Nhân viên");
        lblStaffRole.setStyle("-fx-font-size: 9.5px; -fx-text-fill: #817A70;");
        profileTexts.getChildren().addAll(nameRow, lblStaffRole);
        profileBox.getChildren().addAll(avatarCircle, profileTexts);

        btnGoToMenu.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-size: 11.5px; -fx-font-weight: 600; -fx-background-radius: 5px; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
        AnimationHelper.applyButtonPress(btnGoToMenu);
        btnGoToMenu.setOnAction(e -> {
            if (onNavigateToMenu != null) onNavigateToMenu.run();
        });

        topBar.getChildren().addAll(titleBox, sp1, profileBox, btnGoToMenu);

        // 2. Floor Filter Tabs: [ Tầng 1 ] (Active), [ Tầng 2 ], [ Tầng 3 ]
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

        // 3. Isometric 2.5D Floor Canvas
        floorCanvas.setPrefHeight(490);
        floorCanvas.setMinHeight(430);
        floorCanvas.setStyle(
            "-fx-background-color: #F1EADF; " +
            "-fx-background-radius: 12px; " +
            "-fx-border-color: #DED7CD; " +
            "-fx-border-radius: 12px; " +
            "-fx-border-width: 1px;"
        );
        VBox.setVgrow(floorCanvas, Priority.ALWAYS);

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

        center.getChildren().addAll(topBar, floorTabs, floorCanvas, legendBar);
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

    private void renderFloorPlan() {
        floorCanvas.getChildren().clear();
        tableNodes.clear();

        // Chậu cây trang trí góc phòng đậm chất Zen Specialty
        Label plant1 = new Label("🪴");
        plant1.setStyle("-fx-font-size: 32px;");
        plant1.setLayoutX(680);
        plant1.setLayoutY(28);

        Label plant2 = new Label("🪴");
        plant2.setStyle("-fx-font-size: 32px;");
        plant2.setLayoutX(680);
        plant2.setLayoutY(360);

        Label plant3 = new Label("🪴");
        plant3.setStyle("-fx-font-size: 26px;");
        plant3.setLayoutX(36);
        plant3.setLayoutY(360);

        floorCanvas.getChildren().addAll(plant1, plant2, plant3);

        if (currentFloor == 1) {
            // TẦNG 1: Khớp bố cục Screen 2 trong UI_mong_muon.png
            // Row 1: 01, 02, 03
            addTable(1, "01", 120, 50, false, "EMPTY");
            addTable(2, "02", 310, 50, false, "EMPTY");
            addTable(3, "03", 500, 50, false, "EMPTY");

            // Row 2: 05 (Active), 09, 07
            addTable(5, "05", 120, 160, false, "SERVING");
            addTable(6, "09", 310, 160, false, "EMPTY");
            addTable(7, "07", 500, 160, false, "EMPTY");

            // Row 3: 08, 10, 09
            addTable(8, "08", 120, 270, false, "EMPTY");
            addTable(10, "10", 310, 270, false, "EMPTY");
            addTable(9, "09", 500, 270, false, "EMPTY");

            // Row 4: Bàn 11 (bàn dài) và Bàn 12 (Sảnh)
            addTable(11, "11", 140, 380, true, "EMPTY");
            addTable(12, "Sảnh  12", 360, 380, true, "SERVING");
        } else if (currentFloor == 2) {
            // TẦNG 2: Ban công & Phòng đọc sách
            addTable(201, "201", 120, 60, false, "EMPTY");
            addTable(202, "202", 310, 60, false, "RESERVED");
            addTable(203, "203", 500, 60, false, "EMPTY");

            addTable(204, "204", 120, 180, false, "EMPTY");
            addTable(205, "205", 310, 180, false, "SERVING");
            addTable(206, "206", 500, 180, false, "EMPTY");

            addTable(207, "Bàn 207 • Ban Công", 140, 320, true, "EMPTY");
            addTable(208, "Bàn 208 • Họp Nhóm", 380, 320, true, "EMPTY");
        } else {
            // TẦNG 3: Sân thượng Rooftop Zen Garden
            Label flower = new Label("🌸");
            flower.setStyle("-fx-font-size: 24px;");
            flower.setLayoutX(36);
            flower.setLayoutY(50);
            floorCanvas.getChildren().add(flower);

            addTable(301, "301", 140, 70, false, "EMPTY");
            addTable(302, "302", 330, 70, false, "SERVING");
            addTable(303, "303", 520, 70, false, "EMPTY");

            addTable(304, "304", 140, 200, false, "EMPTY");
            addTable(305, "305", 330, 200, false, "EMPTY");

            addTable(306, "Bàn 306 • Bar Sunset", 230, 330, true, "EMPTY");
        }
    }

    private void addTable(int num, String labelText, double x, double y, boolean isLarge, String status) {
        boolean isSelected = (num == selectedTable);

        TableWithChairsNode node = new TableWithChairsNode(
            num,
            labelText,
            isSelected,
            status,
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

        tableNodes.add(node);
        floorCanvas.getChildren().add(node);
    }

    private void updateTableTitleDisplay() {
        lblCurrentTableTitle.setText("Bàn " + (selectedTable < 10 ? "0" + selectedTable : selectedTable));
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
}
