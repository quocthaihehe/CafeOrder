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
 * FloorPlanView - Màn hình Sơ đồ Bàn (Floor Plan View) phân tầng cho Brew & Bean CAFE.
 * Thiết kế theo chuẩn Phần 2 của Báo cáo Chuyên gia Audit & Thiết kế UI/UX:
 * - Left Navigation Rail (195px): Logo Playfair, Sơ đồ bàn (Active), Thực đơn, Đơn hàng, Đăng xuất
 * - Top Bar: Bàn đang chọn, Badge trạng thái, Avatar nhân viên, Nút "Vào thực đơn đặt món →"
 * - Bộ lọc tầng tương tác: [ Tầng 1 ] (Active), [ Tầng 2 ], [ Tầng 3 (Sân thượng) ]
 * - Mặt bằng bàn 2.5D: Các bàn vuông 76x76px, bàn lớn VIP 160-220px, bàn active màu xanh #183C32 có chấm cam
 * - Cây xanh trang trí góc phòng đậm chất Zen Specialty Cafe Nhật Bản
 * - Status Legend Bar: Trống, Đang phục vụ, Đặt trước, Bảo trì
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
    private final List<Button> tableNodes = new ArrayList<>();

    private Consumer<Integer> onTableSelected;
    private Runnable onNavigateToMenu;
    private Runnable onLogout;

    public FloorPlanView(int initialTable, Consumer<Integer> onTableSelected, Runnable onNavigateToMenu, Runnable onLogout) {
        this.selectedTable = initialTable > 0 ? initialTable : 5;
        this.onTableSelected = onTableSelected;
        this.onNavigateToMenu = onNavigateToMenu;
        this.onLogout = onLogout;

        // Tự động chuyển đến tầng phù hợp nếu số bàn >= 200 hoặc >= 300
        if (this.selectedTable >= 300) {
            this.currentFloor = 3;
        } else if (this.selectedTable >= 200) {
            this.currentFloor = 2;
        } else {
            this.currentFloor = 1;
        }

        initUI();
    }

    private void initUI() {
        setStyle("-fx-background-color: #F5F2EB;");

        // 1. LEFT NAVIGATION RAIL
        VBox leftRail = buildLeftNavigationRail();
        setLeft(leftRail);

        // 2. MAIN FLOOR PLAN AREA
        VBox centerArea = buildCenterFloorArea();
        setCenter(centerArea);

        // Update initial title
        updateTableTitleDisplay();
    }

    private VBox buildLeftNavigationRail() {
        VBox rail = new VBox(14);
        rail.setPrefWidth(195);
        rail.setMinWidth(185);
        rail.setMaxWidth(210);
        rail.setPadding(new Insets(18, 14, 18, 14));
        rail.setStyle("-fx-background-color: #FBF9F4; -fx-border-color: transparent #D8D1C3 transparent transparent; -fx-border-width: 0 1 0 0;");

        // Brand Logo (Playfair Display, chuẩn Zen Specialty)
        VBox brandBox = new VBox(2);
        brandBox.setAlignment(Pos.CENTER_LEFT);
        brandBox.setPadding(new Insets(4, 8, 16, 8));

        HBox brandRow = new HBox(8);
        brandRow.setAlignment(Pos.CENTER_LEFT);
        SVGPath logoIcon = SvgIcons.createOutlineIcon(SvgIcons.COFFEE, 0.9, Color.web("#183C32"));

        VBox brandText = new VBox(0);
        Label lblBrand = new Label("Brew & Bean");
        lblBrand.setStyle("-fx-font-family: 'Playfair Display', 'Georgia', serif; -fx-font-size: 19px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        Label lblSub = new Label("C A F E");
        lblSub.setStyle("-fx-font-size: 9.5px; -fx-font-weight: 700; -fx-text-fill: #5C3D2E; -fx-letter-spacing: 3.5px;");
        brandText.getChildren().addAll(lblBrand, lblSub);
        brandRow.getChildren().addAll(logoIcon, brandText);
        brandBox.getChildren().add(brandRow);

        // Navigation Buttons
        VBox navMenu = new VBox(6);

        Button btnFloor = createNavButton("Sơ đồ bàn", SvgIcons.TABLE, true);
        Button btnMenu = createNavButton("Thực đơn", SvgIcons.MENU, false);
        btnMenu.setOnAction(e -> {
            if (onNavigateToMenu != null) onNavigateToMenu.run();
        });

        Button btnOrders = createNavButton("Đơn hàng", SvgIcons.ORDERS, false);
        btnOrders.setOnAction(e -> {
            if (onNavigateToMenu != null) onNavigateToMenu.run();
        });

        navMenu.getChildren().addAll(btnFloor, btnMenu, btnOrders);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Logout / Switch Table Button at Bottom
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
        btn.setPadding(new Insets(10, 14, 10, 14));
        AnimationHelper.applyButtonPress(btn);

        HBox content = new HBox(10);
        content.setAlignment(Pos.CENTER_LEFT);

        SVGPath icon = SvgIcons.createOutlineIcon(iconSvg, 0.85, isActive ? Color.WHITE : Color.web("#817B70"));
        Label label = new Label(title);
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: " + (isActive ? "700" : "600") + "; -fx-text-fill: " + (isActive ? "#FFFFFF" : "#4A453E") + ";");

        content.getChildren().addAll(icon, label);
        btn.setGraphic(content);

        if (isActive) {
            btn.setStyle("-fx-background-color: #183C32; -fx-background-radius: 10px; -fx-border-color: #183C32; -fx-cursor: hand;");
        } else {
            btn.setStyle("-fx-background-color: transparent; -fx-background-radius: 10px; -fx-cursor: hand;");
            btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #EFE9DE; -fx-background-radius: 10px; -fx-cursor: hand;"));
            btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-background-radius: 10px; -fx-cursor: hand;"));
        }

        return btn;
    }

    private VBox buildCenterFloorArea() {
        VBox center = new VBox(14);
        center.setPadding(new Insets(18, 24, 18, 24));
        HBox.setHgrow(center, Priority.ALWAYS);

        // 1. Top Bar: Table info, Avatar, and Proceed to Menu Button
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        lblCurrentTableTitle.setStyle("-fx-font-family: 'Playfair Display', 'Georgia', serif; -fx-font-size: 24px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        lblCurrentTableStatus.setStyle("-fx-text-fill: #E07A5F; -fx-font-size: 12px; -fx-font-weight: 700;");
        titleBox.getChildren().addAll(lblCurrentTableTitle, lblCurrentTableStatus);

        Region sp1 = new Region();
        HBox.setHgrow(sp1, Priority.ALWAYS);

        // Staff profile avatar
        HBox profileBox = new HBox(8);
        profileBox.setAlignment(Pos.CENTER_RIGHT);
        Circle avatarCircle = new Circle(16, Color.web("#D8D1C3"));
        VBox profileTexts = new VBox(0);
        Label lblStaffName = new Label("Trần Minh Anh");
        lblStaffName.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        Label lblStaffRole = new Label("Nhân viên phục vụ");
        lblStaffRole.setStyle("-fx-font-size: 10.5px; -fx-text-fill: #817B70;");
        profileTexts.getChildren().addAll(lblStaffName, lblStaffRole);
        profileBox.getChildren().addAll(avatarCircle, profileTexts);

        btnGoToMenu.getStyleClass().add("btn-primary");
        btnGoToMenu.setPrefHeight(40);
        btnGoToMenu.setStyle("-fx-background-color: #183C32; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-font-weight: 700; -fx-background-radius: 10px; -fx-padding: 8 18 8 18; -fx-cursor: hand;");
        AnimationHelper.applyButtonPress(btnGoToMenu);
        btnGoToMenu.setOnAction(e -> {
            if (onNavigateToMenu != null) onNavigateToMenu.run();
        });

        topBar.getChildren().addAll(titleBox, sp1, profileBox, btnGoToMenu);

        // 2. Floor Filter Tabs: [ Tầng 1 ], [ Tầng 2 ], [ Tầng 3 (Sân thượng) ]
        HBox floorTabs = new HBox(8);
        floorTabs.setAlignment(Pos.CENTER_LEFT);

        tabF1 = new Button("Tầng 1 (Sảnh chính)");
        tabF2 = new Button("Tầng 2 (Ban công & Yên tĩnh)");
        tabF3 = new Button("Tầng 3 (Sân thượng Rooftop)");

        tabF1.setOnAction(e -> selectFloor(1));
        tabF2.setOnAction(e -> selectFloor(2));
        tabF3.setOnAction(e -> selectFloor(3));

        updateFloorTabStyles();
        floorTabs.getChildren().addAll(tabF1, tabF2, tabF3);

        // 3. Isometric 2.5D Floor Canvas
        floorCanvas.setPrefHeight(490);
        floorCanvas.setMinHeight(430);
        floorCanvas.setStyle(
            "-fx-background-color: #EFECE4; " +
            "-fx-background-radius: 16px; " +
            "-fx-border-color: #D8D1C3; " +
            "-fx-border-radius: 16px; " +
            "-fx-border-width: 1px;"
        );
        VBox.setVgrow(floorCanvas, Priority.ALWAYS);

        renderFloorPlan();

        // 4. Status Legend Bar at Bottom
        HBox legendBar = new HBox(18);
        legendBar.setAlignment(Pos.CENTER_LEFT);
        legendBar.setPadding(new Insets(6, 4, 4, 4));

        legendBar.getChildren().addAll(
            createLegendItem("Trống", "#FFFFFF", "#D8D1C3"),
            createLegendItem("Đang phục vụ", "#E07A5F", "#E07A5F"),
            createLegendItem("Đặt trước", "#2D6A4F", "#2D6A4F"),
            createLegendItem("Bảo trì", "#3D261D", "#3D261D")
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
        String activeStyle = "-fx-background-color: #102C25; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 7 16 7 16; -fx-cursor: hand;";
        String inactiveStyle = "-fx-background-color: #FFFFFF; -fx-text-fill: #817B70; -fx-font-size: 12px; -fx-font-weight: 600; -fx-background-radius: 8px; -fx-border-color: #D8D1C3; -fx-border-radius: 8px; -fx-padding: 7 16 7 16; -fx-cursor: hand;";

        if (tabF1 != null) tabF1.setStyle(currentFloor == 1 ? activeStyle : inactiveStyle);
        if (tabF2 != null) tabF2.setStyle(currentFloor == 2 ? activeStyle : inactiveStyle);
        if (tabF3 != null) tabF3.setStyle(currentFloor == 3 ? activeStyle : inactiveStyle);
    }

    private void renderFloorPlan() {
        floorCanvas.getChildren().clear();
        tableNodes.clear();

        // Zen Plants Decor
        Label plant1 = new Label("🪴");
        plant1.setStyle("-fx-font-size: 28px;");
        plant1.setLayoutX(680);
        plant1.setLayoutY(30);

        Label plant2 = new Label("🪴");
        plant2.setStyle("-fx-font-size: 28px;");
        plant2.setLayoutX(680);
        plant2.setLayoutY(360);

        Label plant3 = new Label("🪴");
        plant3.setStyle("-fx-font-size: 24px;");
        plant3.setLayoutX(40);
        plant3.setLayoutY(360);

        floorCanvas.getChildren().addAll(plant1, plant2, plant3);

        if (currentFloor == 1) {
            // ================= TẦNG 1: SẢNH CHÍNH =================
            // Hàng 1
            addTableNode(1, "01", 100, 45, 76, 76, "EMPTY");
            addTableNode(2, "02", 300, 45, 76, 76, "EMPTY");
            addTableNode(3, "03", 500, 45, 76, 76, "RESERVED");

            // Hàng 2
            addTableNode(5, "05", 100, 160, 84, 84, "SERVING");
            addTableNode(6, "06", 300, 160, 76, 76, "EMPTY");
            addTableNode(7, "07", 500, 160, 76, 76, "EMPTY");

            // Hàng 3
            addTableNode(8, "08", 100, 275, 76, 76, "EMPTY");
            addTableNode(9, "09", 300, 275, 76, 76, "MAINTENANCE");
            addTableNode(10, "10", 500, 275, 76, 76, "EMPTY");

            // Hàng 4: VIP & Quầy Bar Sảnh
            addLargeTableNode(11, "Bàn 11 • Sofa VIP", 100, 385, 180, 72, "EMPTY");
            addLargeTableNode(12, "Bàn 12 • Quầy Bar Sảnh", 340, 385, 240, 72, "SERVING");
        } else if (currentFloor == 2) {
            // ================= TẦNG 2: BAN CÔNG & YÊN TĨNH =================
            addTableNode(201, "201", 100, 60, 76, 76, "EMPTY");
            addTableNode(202, "202", 300, 60, 76, 76, "RESERVED");
            addTableNode(203, "203", 500, 60, 76, 76, "EMPTY");

            addTableNode(204, "204", 100, 180, 76, 76, "EMPTY");
            addTableNode(205, "205", 300, 180, 76, 76, "SERVING");
            addTableNode(206, "206", 500, 180, 76, 76, "EMPTY");

            addLargeTableNode(207, "Bàn 207 • Ban Công Phố", 100, 320, 200, 72, "EMPTY");
            addLargeTableNode(208, "Bàn 208 • Phòng Họp Kín", 360, 320, 220, 72, "EMPTY");
        } else {
            // ================= TẦNG 3: SÂN THƯỢNG ROOFTOP =================
            Label flowerDecor = new Label("🌸");
            flowerDecor.setStyle("-fx-font-size: 22px;");
            flowerDecor.setLayoutX(40);
            flowerDecor.setLayoutY(50);
            floorCanvas.getChildren().add(flowerDecor);

            addTableNode(301, "301", 120, 70, 76, 76, "EMPTY");
            addTableNode(302, "302", 320, 70, 76, 76, "SERVING");
            addTableNode(303, "303", 520, 70, 76, 76, "EMPTY");

            addTableNode(304, "304", 120, 210, 76, 76, "EMPTY");
            addTableNode(305, "305", 320, 210, 76, 76, "EMPTY");

            addLargeTableNode(306, "Bàn 306 • Quầy Bar Sunset", 180, 340, 260, 72, "EMPTY");
        }
    }

    private void addTableNode(int tableNum, String labelText, double x, double y, double w, double h, String status) {
        Button btnTable = new Button();
        btnTable.setLayoutX(x);
        btnTable.setLayoutY(y);
        btnTable.setPrefSize(w, h);
        btnTable.setMinSize(w, h);
        btnTable.setMaxSize(w, h);
        AnimationHelper.applyButtonPress(btnTable);

        boolean isSelected = (tableNum == selectedTable);

        VBox content = new VBox(2);
        content.setAlignment(Pos.CENTER);

        HBox dotRow = new HBox(4);
        dotRow.setAlignment(Pos.CENTER);
        Circle statusDot = new Circle(4);
        applyStatusColor(statusDot, status);
        dotRow.getChildren().add(statusDot);

        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size: 17px; -fx-font-weight: 800; -fx-text-fill: " + (isSelected ? "#FFFFFF" : "#292722") + ";");

        content.getChildren().addAll(dotRow, lbl);
        btnTable.setGraphic(content);

        applyTableStyle(btnTable, isSelected, status);

        btnTable.setOnAction(e -> {
            selectTable(tableNum, status);
        });

        // Double-click to directly open menu
        btnTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && onNavigateToMenu != null) {
                onNavigateToMenu.run();
            }
        });

        tableNodes.add(btnTable);
        floorCanvas.getChildren().add(btnTable);
    }

    private void addLargeTableNode(int tableNum, String labelText, double x, double y, double w, double h, String status) {
        Button btnTable = new Button();
        btnTable.setLayoutX(x);
        btnTable.setLayoutY(y);
        btnTable.setPrefSize(w, h);
        btnTable.setMinSize(w, h);
        btnTable.setMaxSize(w, h);
        AnimationHelper.applyButtonPress(btnTable);

        boolean isSelected = (tableNum == selectedTable);

        HBox content = new HBox(8);
        content.setAlignment(Pos.CENTER);

        Circle statusDot = new Circle(4);
        applyStatusColor(statusDot, status);

        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: " + (isSelected ? "#FFFFFF" : "#292722") + ";");

        content.getChildren().addAll(statusDot, lbl);
        btnTable.setGraphic(content);

        applyTableStyle(btnTable, isSelected, status);

        btnTable.setOnAction(e -> {
            selectTable(tableNum, status);
        });

        btnTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && onNavigateToMenu != null) {
                onNavigateToMenu.run();
            }
        });

        tableNodes.add(btnTable);
        floorCanvas.getChildren().add(btnTable);
    }

    private void selectTable(int tableNum, String status) {
        this.selectedTable = tableNum;
        updateTableTitleDisplay();

        if ("SERVING".equals(status)) {
            lblCurrentTableStatus.setText("● Đang phục vụ");
            lblCurrentTableStatus.setStyle("-fx-text-fill: #E07A5F; -fx-font-size: 12px; -fx-font-weight: 700;");
        } else if ("RESERVED".equals(status)) {
            lblCurrentTableStatus.setText("● Đã đặt trước");
            lblCurrentTableStatus.setStyle("-fx-text-fill: #2D6A4F; -fx-font-size: 12px; -fx-font-weight: 700;");
        } else if ("MAINTENANCE".equals(status)) {
            lblCurrentTableStatus.setText("● Đang bảo trì");
            lblCurrentTableStatus.setStyle("-fx-text-fill: #3D261D; -fx-font-size: 12px; -fx-font-weight: 700;");
        } else {
            lblCurrentTableStatus.setText("● Bàn sẵn sàng");
            lblCurrentTableStatus.setStyle("-fx-text-fill: #1B5E20; -fx-font-size: 12px; -fx-font-weight: 700;");
        }

        renderFloorPlan();

        if (onTableSelected != null) {
            onTableSelected.accept(tableNum);
        }
    }

    private void updateTableTitleDisplay() {
        lblCurrentTableTitle.setText("Bàn " + (selectedTable < 10 ? "0" + selectedTable : selectedTable));
    }

    private void applyTableStyle(Button btn, boolean isSelected, String status) {
        if (isSelected) {
            btn.setStyle(
                "-fx-background-color: #183C32; " +
                "-fx-background-radius: 12px; " +
                "-fx-border-color: #183C32; " +
                "-fx-border-radius: 12px; " +
                "-fx-border-width: 2px; " +
                "-fx-effect: dropshadow(gaussian, rgba(24, 60, 50, 0.35), 10, 0, 0, 3); " +
                "-fx-cursor: hand;"
            );
        } else {
            btn.setStyle(
                "-fx-background-color: #FFFFFF; " +
                "-fx-background-radius: 12px; " +
                "-fx-border-color: #D8D1C3; " +
                "-fx-border-radius: 12px; " +
                "-fx-border-width: 1px; " +
                "-fx-effect: dropshadow(gaussian, rgba(41, 39, 34, 0.05), 6, 0, 0, 2); " +
                "-fx-cursor: hand;"
            );
        }
    }

    private void applyStatusColor(Circle dot, String status) {
        switch (status) {
            case "SERVING":
                dot.setFill(Color.web("#E07A5F")); // Cam rực rỡ
                break;
            case "RESERVED":
                dot.setFill(Color.web("#2D6A4F")); // Xanh lá
                break;
            case "MAINTENANCE":
                dot.setFill(Color.web("#3D261D")); // Nâu đen
                break;
            default:
                dot.setFill(Color.web("#D8D1C3")); // Be nhạt
                break;
        }
    }

    private HBox createLegendItem(String labelText, String fillColor, String borderColor) {
        HBox item = new HBox(6);
        item.setAlignment(Pos.CENTER_LEFT);

        Circle dot = new Circle(4.5, Color.web(fillColor));
        dot.setStroke(Color.web(borderColor));
        dot.setStrokeWidth(1);

        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #817B70;");

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

    public Button getGoToMenuButton() {
        return btnGoToMenu;
    }
}
