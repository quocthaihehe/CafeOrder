package cafe.views;

import cafe.models.Order;
import cafe.models.OrderItem;
import cafe.services.CafeServerService;
import cafe.services.DatabaseManager;
import cafe.utils.AnimationHelper;
import cafe.utils.CurrencyFormatter;
import cafe.utils.SvgIcons;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * AdminShellView - Giao diện Quản trị & Điều phối Quầy Trung tâm (CoffeePOS)
 * Chuẩn hóa 100% theo mẫu nhaminh-coffee.vn/quay:
 * - Sidebar 8 phân hệ bên trái (#F7F2EA, 175px)
 * - Profile "Minh Anh · Quản lý" ở chân Sidebar
 * - Tab 1: Tổng quan (Dashboard KPI, Xu hướng 7 ngày, Món bán chạy, Bảng đơn gần đây nhảy số thời gian thực)
 * - Tab 4: Màn pha chế (Nhúng trực tiếp ServerKDSView)
 * - Tab 3: Đơn hàng (Lịch sử đơn, Hủy/hoàn tiền)
 * - Tab 5: Món & giá (Menu & Recipes BOM)
 * - Tab 7: Báo cáo & Kho (Nguyên liệu Ingredients & Vouchers)
 * - Tab 8: Cài đặt (Hệ thống & Kết nối)
 */
public class AdminShellView extends BorderPane {

    private final CafeServerService serverService;
    private final ServerKDSView embeddedKdsView;

    private final VBox centerContent = new VBox();
    private int currentTab = 1; // 1..8

    // KPI Labels
    private final Label lblKpiRevenue = new Label("8.420.000đ");
    private final Label lblKpiOrders = new Label("126 đơn");
    private final Label lblKpiAov = new Label("66.800đ");
    private final Label lblKpiMembers = new Label("41%");

    // Recent orders table
    private final VBox recentOrdersContainer = new VBox(6);
    private final List<Button> navButtons = new ArrayList<>();

    public AdminShellView(CafeServerService serverService, ServerKDSView kdsView) {
        this.serverService = serverService;
        this.embeddedKdsView = kdsView;

        initUI();
        registerLiveListeners();
    }

    private void initUI() {
        setStyle("-fx-background-color: #F5F0E8;");

        // 1. LEFT SIDEBAR (175px)
        VBox sidebar = buildSidebar();
        setLeft(sidebar);

        // 2. CENTER CONTENT AREA
        centerContent.setPadding(new Insets(16, 24, 16, 24));
        centerContent.setStyle("-fx-background-color: #F5F0E8;");
        setCenter(centerContent);

        // Hiển thị Tab 1 mặc định (Tổng quan)
        switchTab(1);
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.setPrefWidth(235);
        sidebar.setMinWidth(220);
        sidebar.setMaxWidth(255);
        sidebar.setPadding(new Insets(20, 12, 20, 12));
        sidebar.setStyle("-fx-background-color: #F7F2EA; -fx-border-color: transparent #DED7CD transparent transparent; -fx-border-width: 0 1 0 0;");

        // Brand Logo
        VBox brandBox = new VBox(3);
        brandBox.setAlignment(Pos.CENTER_LEFT);
        brandBox.setPadding(new Insets(4, 8, 16, 8));

        Label lblBrand = new Label("Brew & Bean");
        lblBrand.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 17.5px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        Label lblSub = new Label("COFFEE POS • QUẦY");
        lblSub.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #563521; -fx-letter-spacing: 1.5px;");
        brandBox.getChildren().addAll(lblBrand, lblSub);

        // Sidebar 9 Phân Hệ (Tách riêng Tồn kho & Báo cáo)
        VBox navList = new VBox(5);
        navButtons.clear();

        navButtons.add(createSidebarBtn(1, "Tổng quan", "tong-quan.png", SvgIcons.TABLE, "📊"));
        navButtons.add(createSidebarBtn(2, "Gọi món", "goi-mon.png", SvgIcons.SHOPPING_BAG, "🛒"));
        navButtons.add(createSidebarBtn(3, "Đơn hàng", "don-hang.png", SvgIcons.ORDERS, "🧾"));
        navButtons.add(createSidebarBtn(4, "Màn pha chế", "man-pha-che.png", SvgIcons.COFFEE, "☕"));
        navButtons.add(createSidebarBtn(5, "Món & giá", "mon-va-gia.png", SvgIcons.MENU, "🏷️"));
        navButtons.add(createSidebarBtn(6, "Nhân viên", "nhan-vien.png", SvgIcons.USER, "👥"));
        navButtons.add(createSidebarBtn(7, "Tồn kho", "kho.png", SvgIcons.SETTINGS, "📦"));
        navButtons.add(createSidebarBtn(8, "Báo cáo", "bao-cao.png", SvgIcons.ORDERS, "📈"));
        navButtons.add(createSidebarBtn(9, "Cài đặt", "cai-dat.png", SvgIcons.SETTINGS, "⚙️"));

        navList.getChildren().addAll(navButtons);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Bottom Manager Profile Card (Minh Anh · Quản lý)
        HBox profileCard = new HBox(10);
        profileCard.setAlignment(Pos.CENTER_LEFT);
        profileCard.setPadding(new Insets(12, 10, 12, 10));
        profileCard.setStyle("-fx-background-color: #EFE8DD; -fx-background-radius: 8px; -fx-cursor: hand;");

        Circle avatar = new Circle(16, Color.web("#112823"));
        Label lblAvaText = new Label("M");
        lblAvaText.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #FFFFFF;");
        StackPane avaPane = new StackPane(avatar, lblAvaText);

        VBox profTexts = new VBox(2);
        Label lblName = new Label("Minh Anh");
        lblName.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        Label lblRole = new Label("Quản lý · Q.3");
        lblRole.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #6F695F;");
        profTexts.getChildren().addAll(lblName, lblRole);

        profileCard.getChildren().addAll(avaPane, profTexts);

        sidebar.getChildren().addAll(brandBox, navList, spacer, profileCard);
        return sidebar;
    }

    private Button createSidebarBtn(int tabIndex, String labelText, String iconFileName, String svgFallback, String fallbackEmoji) {
        Button btn = new Button();
        btn.setFocusTraversable(false);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(10, 14, 10, 14));
        AnimationHelper.applyButtonPress(btn);

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        // Khung chứa icon cố định tuyệt đối 26x26 px -> Khắc phục triệt để lỗi icon thụt ra thụt vô, thẳng tắp 1 hàng dọc theo trục
        StackPane iconContainer = new StackPane();
        iconContainer.setPrefSize(26, 26);
        iconContainer.setMinSize(26, 26);
        iconContainer.setMaxSize(26, 26);
        iconContainer.setAlignment(Pos.CENTER);

        Node iconNode = getIconNode(iconFileName, svgFallback, fallbackEmoji, tabIndex == currentTab);
        iconContainer.getChildren().add(iconNode);

        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #3F3931;");

        row.getChildren().addAll(iconContainer, label);
        btn.setGraphic(row);

        btn.setOnAction(e -> switchTab(tabIndex));
        updateSidebarBtnStyle(btn, tabIndex == currentTab);
        return btn;
    }

    private Node getIconNode(String iconFileName, String svgFallback, String fallbackEmoji, boolean isActive) {
        // 1. Kiểm tra ảnh PNG tải về trong các thư mục tài nguyên dự án
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
                            iv.setFitWidth(20);
                            iv.setFitHeight(20);
                            iv.setPreserveRatio(true);
                            iv.setSmooth(true);
                            iv.setOpacity(isActive ? 1.0 : 0.85);
                            return iv;
                        }
                    }
                } catch (Exception ignored) {}
            }

            // Fallback đọc trực tiếp từ thư mục trên đĩa (khi chạy trong môi trường Dev)
            String[] diskPaths = {
                "src/cafe/resources/images/icons/admin/" + iconFileName,
                "resources/images/icons/admin/" + iconFileName,
                "resources/images/" + iconFileName
            };
            for (String dp : diskPaths) {
                try {
                    java.io.File file = new java.io.File(dp);
                    if (file.exists()) {
                        Image img = new Image(file.toURI().toString());
                        if (!img.isError() && img.getWidth() > 0) {
                            ImageView iv = new ImageView(img);
                            iv.setFitWidth(20);
                            iv.setFitHeight(20);
                            iv.setPreserveRatio(true);
                            iv.setSmooth(true);
                            iv.setOpacity(isActive ? 1.0 : 0.85);
                            return iv;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        // 2. Dự phòng bằng Vector SVG (không bao giờ bị lỗi ô vuông hay thụt lề)
        if (svgFallback != null && !svgFallback.isEmpty()) {
            Color color = isActive ? Color.web("#112823") : Color.web("#3F3931");
            return SvgIcons.createOutlineIcon(svgFallback, 0.82, color);
        }

        // 3. Fallback emoji sạch
        Label lbl = new Label(fallbackEmoji != null ? fallbackEmoji : "•");
        lbl.setStyle("-fx-font-size: 15px;");
        return lbl;
    }

    private void updateSidebarBtnStyle(Button btn, boolean isActive) {
        if (isActive) {
            btn.setStyle("-fx-background-color: #DED5C8; -fx-background-radius: 8px; -fx-cursor: hand;");
            if (btn.getGraphic() instanceof HBox) {
                HBox box = (HBox) btn.getGraphic();
                if (box.getChildren().size() > 0 && box.getChildren().get(0) instanceof StackPane) {
                    StackPane iconBox = (StackPane) box.getChildren().get(0);
                    if (!iconBox.getChildren().isEmpty()) {
                        Node n = iconBox.getChildren().get(0);
                        if (n instanceof SVGPath) {
                            ((SVGPath) n).setStroke(Color.web("#112823"));
                        } else if (n instanceof ImageView) {
                            n.setOpacity(1.0);
                        }
                    }
                }
                if (box.getChildren().size() > 1 && box.getChildren().get(1) instanceof Label) {
                    ((Label) box.getChildren().get(1)).setStyle("-fx-font-size: 13.5px; -fx-font-weight: 800; -fx-text-fill: #112823;");
                }
            }
        } else {
            btn.setStyle("-fx-background-color: transparent; -fx-background-radius: 8px; -fx-cursor: hand;");
            if (btn.getGraphic() instanceof HBox) {
                HBox box = (HBox) btn.getGraphic();
                if (box.getChildren().size() > 0 && box.getChildren().get(0) instanceof StackPane) {
                    StackPane iconBox = (StackPane) box.getChildren().get(0);
                    if (!iconBox.getChildren().isEmpty()) {
                        Node n = iconBox.getChildren().get(0);
                        if (n instanceof SVGPath) {
                            ((SVGPath) n).setStroke(Color.web("#3F3931"));
                        } else if (n instanceof ImageView) {
                            n.setOpacity(0.85);
                        }
                    }
                }
                if (box.getChildren().size() > 1 && box.getChildren().get(1) instanceof Label) {
                    ((Label) box.getChildren().get(1)).setStyle("-fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #3F3931;");
                }
            }
            btn.setOnMouseEntered(e -> {
                if (tabIndexFromButton(btn) != currentTab) {
                    btn.setStyle("-fx-background-color: #EFE8DD; -fx-background-radius: 8px; -fx-cursor: hand;");
                }
            });
            btn.setOnMouseExited(e -> {
                if (tabIndexFromButton(btn) != currentTab) {
                    btn.setStyle("-fx-background-color: transparent; -fx-background-radius: 8px; -fx-cursor: hand;");
                }
            });
        }
    }

    private int tabIndexFromButton(Button btn) {
        return navButtons.indexOf(btn) + 1;
    }

    private void switchTab(int tabIndex) {
        this.currentTab = tabIndex;
        for (int i = 0; i < navButtons.size(); i++) {
            updateSidebarBtnStyle(navButtons.get(i), (i + 1) == currentTab);
        }

        centerContent.getChildren().clear();

        switch (tabIndex) {
            case 1:
                renderDashboardTab();
                break;
            case 4:
                renderKdsTab();
                break;
            case 3:
                renderOrderHistoryTab();
                break;
            case 5:
                renderMenuManagementTab();
                break;
            case 6:
                renderStaffTab();
                break;
            case 7:
                renderInventoryTab();
                break;
            case 8:
                renderReportsTab();
                break;
            case 9:
                renderSettingsTab();
                break;
            case 2:
            default:
                renderCounterPosTab();
                break;
        }
    }

    // =========================================================================
    // TAB 1: EXECUTIVE DASHBOARD (TỔNG QUAN HÔM NAY)
    // =========================================================================
    private void renderDashboardTab() {
        centerContent.getChildren().clear();
        centerContent.setSpacing(14);

        // 1. Header Row
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label lblTitle = new Label("Tổng quan hôm nay");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #292722;");

        String todayStr = new SimpleDateFormat("EEEE dd/MM/yyyy", new Locale("vi", "VN")).format(new Date());
        Label lblSub = new Label("Quán Nhà Mình · Q.3 · " + todayStr);
        lblSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #817A70;");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        TextField txtSearch = new TextField();
        txtSearch.setPromptText("Tìm kiếm... ⌘K");
        txtSearch.setPrefWidth(220);
        txtSearch.setPrefHeight(38);
        txtSearch.setMinHeight(38);
        txtSearch.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-background-color: #FBF9F5; -fx-border-color: #DED7CD; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-font-size: 12px; -fx-padding: 8 12 8 12;");

        Button btnCreate = new Button("+ Tạo mới");
        btnCreate.setPrefHeight(38);
        btnCreate.setMinHeight(38);
        btnCreate.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
        AnimationHelper.applyButtonPress(btnCreate);
        btnCreate.setOnAction(e -> switchTab(2)); // Chuyển sang Gọi món

        headerRow.getChildren().addAll(titleBox, sp, txtSearch, btnCreate);

        // 2. 4 Thẻ KPI Sống (Row 1)
        HBox kpiGrid = new HBox(12);
        kpiGrid.setAlignment(Pos.CENTER);

        kpiGrid.getChildren().addAll(
            createKpiCard("DOANH THU", lblKpiRevenue, "+12,4%", true),
            createKpiCard("SỐ ĐƠN HÀNG", lblKpiOrders, "+9 đơn", true),
            createKpiCard("GIÁ TRỊ TB / ĐƠN", lblKpiAov, "+3,1%", true),
            createKpiCard("KHÁCH THÀNH VIÊN", lblKpiMembers, "+5%", true)
        );

        // 3. Biểu đồ Xu hướng & Bảng Món bán chạy (Row 2)
        HBox chartsRow = new HBox(14);
        chartsRow.setAlignment(Pos.TOP_LEFT);

        // Box Xu hướng 7 ngày (Left)
        VBox trendBox = buildTrendBox();
        HBox.setHgrow(trendBox, Priority.ALWAYS);

        // Box Món bán chạy (Right)
        VBox bestsellersBox = buildBestsellersBox();
        bestsellersBox.setPrefWidth(300);
        bestsellersBox.setMinWidth(280);

        chartsRow.getChildren().addAll(trendBox, bestsellersBox);

        // 4. Bảng Đơn gần đây (Row 3)
        VBox recentOrdersBox = buildRecentOrdersBox();
        VBox.setVgrow(recentOrdersBox, Priority.ALWAYS);

        centerContent.getChildren().addAll(headerRow, kpiGrid, chartsRow, recentOrdersBox);
        refreshKpiNumbers();
    }

    private VBox createKpiCard(String title, Label valLabel, String changePercent, boolean isPositive) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10px; -fx-border-color: #E7E2DA; -fx-border-radius: 10px; -fx-border-width: 1px; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.04), 8, 0.1, 0, 2);");
        HBox.setHgrow(card, Priority.ALWAYS);

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 10.5px; -fx-font-weight: 700; -fx-text-fill: #817A70; -fx-letter-spacing: 0.5px;");

        valLabel.setStyle("-fx-font-family: 'Playfair Display', Georgia, serif; -fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: #1C1917;");

        HBox footer = new HBox(4);
        footer.setAlignment(Pos.CENTER_LEFT);
        Label lblChange = new Label((isPositive ? "↑ " : "↓ ") + changePercent);
        lblChange.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: " + (isPositive ? "#4B704F" : "#C62828") + ";");
        Label lblSub = new Label("so với hôm qua");
        lblSub.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 10px; -fx-text-fill: #A8A196;");
        footer.getChildren().addAll(lblChange, lblSub);

        card.getChildren().addAll(lblTitle, valLabel, footer);
        return card;
    }

    private VBox buildTrendBox() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(14, 16, 14, 16));
        box.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8px; -fx-border-color: #E7E2DA; -fx-border-radius: 8px; -fx-border-width: 1px;");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label lblTitle = new Label("Xu hướng 7 ngày");
        lblTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label lblLegend = new Label("● Tuần này  ○ Tuần trước");
        lblLegend.setStyle("-fx-font-size: 10.5px; -fx-text-fill: #817A70;");
        header.getChildren().addAll(lblTitle, sp, lblLegend);

        // Biểu đồ cột minh họa 7 ngày (T2..CN)
        HBox barChart = new HBox(16);
        barChart.setAlignment(Pos.BOTTOM_CENTER);
        barChart.setPrefHeight(100);
        barChart.setPadding(new Insets(10, 0, 4, 0));

        String[] days = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
        double[] heights = {45, 60, 52, 70, 85, 95, 88};

        for (int i = 0; i < days.length; i++) {
            VBox col = new VBox(4);
            col.setAlignment(Pos.BOTTOM_CENTER);
            HBox.setHgrow(col, Priority.ALWAYS);

            Region bar = new Region();
            bar.setPrefWidth(20);
            bar.setPrefHeight(heights[i]);
            bar.setStyle("-fx-background-color: " + (i == 5 ? "#112823" : "#DED5C8") + "; -fx-background-radius: 4px 4px 0 0;");

            Label lblDay = new Label(days[i]);
            lblDay.setStyle("-fx-font-size: 10px; -fx-text-fill: #817A70; -fx-font-weight: " + (i == 5 ? "700" : "500") + ";");

            col.getChildren().addAll(bar, lblDay);
            barChart.getChildren().add(col);
        }

        box.getChildren().addAll(header, barChart);
        return box;
    }

    private VBox buildBestsellersBox() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(14, 16, 14, 16));
        box.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8px; -fx-border-color: #E7E2DA; -fx-border-radius: 8px; -fx-border-width: 1px;");

        Label lblTitle = new Label("Món bán chạy");
        lblTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        box.getChildren().add(lblTitle);

        box.getChildren().addAll(
            createBestsellerRow("Cà phê sữa đá", "48 ly", "#563521"),
            createBestsellerRow("Trà đào cam sả", "31 ly", "#C87D32"),
            createBestsellerRow("Bạc xỉu", "27 ly", "#8D6E63"),
            createBestsellerRow("Matcha latte", "19 ly", "#4B704F")
        );

        return box;
    }

    private HBox createBestsellerRow(String name, String qty, String dotColor) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 0, 2, 0));

        Circle dot = new Circle(3.5, Color.web(dotColor));
        Label lblName = new Label(name);
        lblName.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #292722; -fx-font-weight: 500;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label lblQty = new Label(qty);
        lblQty.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #112823;");

        row.getChildren().addAll(dot, lblName, sp, lblQty);
        return row;
    }

    private VBox buildRecentOrdersBox() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(14, 16, 14, 16));
        box.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8px; -fx-border-color: #E7E2DA; -fx-border-radius: 8px; -fx-border-width: 1px;");

        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        Label lblTitle = new Label("Đơn gần đây");
        lblTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #292722;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnViewAll = new Button("Xem tất cả →");
        btnViewAll.setStyle("-fx-background-color: transparent; -fx-text-fill: #112823; -fx-font-size: 11px; -fx-font-weight: 600; -fx-cursor: hand;");
        btnViewAll.setOnAction(e -> switchTab(3)); // Chuyển tab Đơn hàng
        top.getChildren().addAll(lblTitle, sp, btnViewAll);

        // Header Table
        HBox tblHeader = new HBox(10);
        tblHeader.setPadding(new Insets(6, 10, 6, 10));
        tblHeader.setStyle("-fx-background-color: #F7F2EA; -fx-background-radius: 6px;");

        Label colCode = createColHeader("MÃ ĐƠN", 100);
        Label colTable = createColHeader("BÀN / THU NGÂN", 160);
        colTable.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(colTable, Priority.ALWAYS);
        Label colItems = createColHeader("SỐ MÓN", 90);
        Label colTotal = createColHeader("TỔNG TIỀN", 120);
        Label colStatus = createColHeader("TRẠNG THÁI", 110);

        tblHeader.getChildren().addAll(colCode, colTable, colItems, colTotal, colStatus);

        recentOrdersContainer.getChildren().clear();
        populateRecentOrders();

        ScrollPane scroll = new ScrollPane(recentOrdersContainer);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(130);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        box.getChildren().addAll(top, tblHeader, scroll);
        return box;
    }

    private Label createColHeader(String text, double width) {
        Label lbl = new Label(text);
        lbl.setPrefWidth(width);
        lbl.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #817A70;");
        return lbl;
    }

    private void populateRecentOrders() {
        recentOrdersContainer.getChildren().clear();
        Map<Integer, Order> active = serverService.getActiveOrders();
        if (active != null && !active.isEmpty()) {
            List<Order> list = new ArrayList<>(active.values());
            Collections.reverse(list);
            for (Order o : list) {
                recentOrdersContainer.getChildren().add(createOrderRow(o));
            }
        } else {
            // Mẫu ban đầu theo ảnh CoffeePOS nhaminh-coffee.vn/quay
            recentOrdersContainer.getChildren().add(createMockOrderRow("#CF-2481", "Bàn 05 · Thu Hà", "4 món", "142.200đ", "PREPARING"));
            recentOrdersContainer.getChildren().add(createMockOrderRow("#CF-2480", "Bàn 12 · Quốc Bảo", "2 món", "61.000đ", "DONE"));
            recentOrdersContainer.getChildren().add(createMockOrderRow("#CF-2479", "Bàn 02 · Thu Hà", "6 món", "215.000đ", "DONE"));
        }
    }

    private HBox createOrderRow(Order order) {
        String code = "#CF-" + order.getOrderId();
        String tableStaff = "Bàn " + (order.getTable() < 10 ? "0" + order.getTable() : order.getTable()) + " · Thu Hà";
        String itemCount = order.getTotalQuantity() + " món";
        String total = CurrencyFormatter.format(order.getFinalAmount());
        String status = order.getStatus();

        return createMockOrderRow(code, tableStaff, itemCount, total, status);
    }

    private HBox createMockOrderRow(String code, String tableStaff, String itemCount, String total, String status) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 10, 8, 10));
        row.setStyle("-fx-border-color: transparent transparent #EFE8DD transparent; -fx-border-width: 0 0 1 0;");

        Label lblCode = new Label(code);
        lblCode.setPrefWidth(100);
        lblCode.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #1C1917;");

        Label lblTable = new Label(tableStaff);
        lblTable.setMinWidth(140);
        lblTable.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(lblTable, Priority.ALWAYS);
        lblTable.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-text-fill: #6F695F;");

        Label lblItems = new Label(itemCount);
        lblItems.setPrefWidth(90);
        lblItems.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-text-fill: #6F695F;");

        Label lblTotal = new Label(total);
        lblTotal.setPrefWidth(120);
        lblTotal.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #112823;");

        Label badge = new Label();
        badge.setPrefWidth(100);
        if ("PREPARING".equalsIgnoreCase(status) || "ĐANG PHA".equalsIgnoreCase(status)) {
            badge.setText("● Đang pha");
            badge.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #E65100; -fx-font-size: 10px; -fx-font-weight: 600; -fx-background-radius: 8px; -fx-padding: 2 6 2 6;");
        } else if ("DONE".equalsIgnoreCase(status) || "HOÀN TẤT".equalsIgnoreCase(status)) {
            badge.setText("● Hoàn tất");
            badge.setStyle("-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32; -fx-font-size: 10px; -fx-font-weight: 600; -fx-background-radius: 8px; -fx-padding: 2 6 2 6;");
        } else if ("CANCELLED".equalsIgnoreCase(status)) {
            badge.setText("● Đã hủy");
            badge.setStyle("-fx-background-color: #FFEBEE; -fx-text-fill: #C62828; -fx-font-size: 10px; -fx-font-weight: 600; -fx-background-radius: 8px; -fx-padding: 2 6 2 6;");
        } else {
            badge.setText("● Đang chờ");
            badge.setStyle("-fx-background-color: #FFFDE7; -fx-text-fill: #F57F17; -fx-font-size: 10px; -fx-font-weight: 600; -fx-background-radius: 8px; -fx-padding: 2 6 2 6;");
        }

        row.getChildren().addAll(lblCode, lblTable, lblItems, lblTotal, badge);
        return row;
    }

    private void refreshKpiNumbers() {
        Map<String, Object> stats = DatabaseManager.getDashboardStats(serverService.getActiveOrders());
        if (stats != null) {
            double rev = (double) stats.getOrDefault("totalRevenue", 8420000.0);
            int count = (int) stats.getOrDefault("orderCount", 126);
            double aov = (double) stats.getOrDefault("aov", 66800.0);

            lblKpiRevenue.setText(CurrencyFormatter.format(rev));
            lblKpiOrders.setText(count + " đơn");
            lblKpiAov.setText(CurrencyFormatter.format(aov));
        }
    }

    // =========================================================================
    // TAB 4: MÀN PHA CHẾ (EMBEDDED KDS VIEW)
    // =========================================================================
    private void renderKdsTab() {
        centerContent.getChildren().clear();
        centerContent.setSpacing(0);
        VBox.setVgrow(embeddedKdsView, Priority.ALWAYS);
        centerContent.getChildren().add(embeddedKdsView);
    }

    // =========================================================================
    // TAB 3: ĐƠN HÀNG (ORDER AUDIT)
    // =========================================================================
    private void renderOrderHistoryTab() {
        centerContent.getChildren().clear();
        centerContent.setSpacing(12);

        Label lbl = new Label("Quản Lý & Kiểm Toán Đơn Hàng");
        lbl.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #292722;");

        OrdersDialog ordersDialog = new OrdersDialog();
        // Lấy nội dung bên trong của OrdersDialog để nhúng thẳng vào tab
        if (ordersDialog.getScene() != null && ordersDialog.getScene().getRoot() != null) {
            Node root = ordersDialog.getScene().getRoot();
            VBox.setVgrow(root, Priority.ALWAYS);
            centerContent.getChildren().addAll(lbl, root);
        } else {
            VBox box = buildRecentOrdersBox();
            VBox.setVgrow(box, Priority.ALWAYS);
            centerContent.getChildren().addAll(lbl, box);
        }
    }

    // =========================================================================
    // TAB 5: MÓN & GIÁ (MENU & RECIPES BOM)
    // =========================================================================
    private void renderMenuManagementTab() {
        centerContent.getChildren().clear();
        centerContent.setSpacing(12);

        Label lbl = new Label("Quản Lý Thực Đơn, Giá Bán & Công Thức Định Lượng (BOM)");
        lbl.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #292722;");

        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8px; -fx-border-color: #E7E2DA; -fx-border-radius: 8px;");

        Label desc = new Label("Hệ thống đồng bộ trực tiếp 67 sản phẩm, 8 danh mục và bảng Recipes từ SQL Server.");
        desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #6F695F;");

        Label tip = new Label("✓ Tự động trừ tồn kho theo gram cà phê và ml sữa mỗi khi Barista bấm 'Hoàn tất'.");
        tip.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 600; -fx-text-fill: #112823;");

        card.getChildren().addAll(desc, tip);
        centerContent.getChildren().addAll(lbl, card);
    }

    // =========================================================================
    // TAB 6: NHÂN VIÊN (STAFF & SHIFTS)
    // =========================================================================
    private void renderStaffTab() {
        centerContent.getChildren().clear();
        centerContent.setSpacing(12);

        Label lbl = new Label("Quản Lý Nhân Viên & Phân Quyền (RBAC)");
        lbl.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #292722;");

        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8px; -fx-border-color: #E7E2DA; -fx-border-radius: 8px;");

        card.getChildren().addAll(
            createStaffItem("admin", "Trần Minh Anh", "Quản lý / Chủ quán (MANAGER/OWNER)", "ACTIVE"),
            createStaffItem("cashier1", "Trần Minh Anh", "Thu ngân quầy (CASHIER)", "ACTIVE"),
            createStaffItem("waiter", "Lê Quốc Bảo", "Nhân viên phục vụ sảnh (WAITER)", "ACTIVE"),
            createStaffItem("barista1", "Phạm Thu Hà", "Nhân viên pha chế (BARISTA)", "ACTIVE")
        );

        centerContent.getChildren().addAll(lbl, card);
    }

    private HBox createStaffItem(String user, String name, String role, String status) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 10, 8, 10));
        row.setStyle("-fx-border-color: transparent transparent #EFE8DD transparent; -fx-border-width: 0 0 1 0;");

        Label lblUser = new Label(user);
        lblUser.setPrefWidth(100);
        lblUser.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #112823;");

        Label lblName = new Label(name);
        lblName.setPrefWidth(140);
        lblName.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-text-fill: #1C1917;");

        Label lblRole = new Label(role);
        lblRole.setMinWidth(200);
        lblRole.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(lblRole, Priority.ALWAYS);
        lblRole.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-text-fill: #6F695F;");

        Label badge = new Label("● " + status);
        badge.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32; -fx-font-size: 10px; -fx-font-weight: 600; -fx-background-radius: 6px; -fx-padding: 3 8 3 8;");

        row.getChildren().addAll(lblUser, lblName, lblRole, badge);
        return row;
    }

    // =========================================================================
    // TAB 7: TỒN KHO & ĐỊNH LƯỢNG (INVENTORY & RECIPE BOM)
    // =========================================================================
    private void renderInventoryTab() {
        centerContent.getChildren().clear();
        centerContent.setSpacing(14);

        // 1. Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label lblTitle = new Label("Quản Lý Tồn Kho & Định Lượng (Inventory / BOM)");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        Label lblSub = new Label("Kiểm soát lượng nguyên liệu thực tế, định mức cảnh báo & hao hụt pha chế");
        lblSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #817A70;");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnRestock = new Button("+ Nhập kho nguyên liệu");
        btnRestock.setPrefHeight(38);
        btnRestock.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
        AnimationHelper.applyButtonPress(btnRestock);
        btnRestock.setOnAction(e -> showToastNotification("Đã mở phiếu nhập kho nguyên liệu mới!"));

        header.getChildren().addAll(titleBox, sp, btnRestock);

        // 2. 4 Thẻ KPI Tồn kho
        HBox kpiRow = new HBox(12);
        kpiRow.setAlignment(Pos.CENTER);
        kpiRow.getChildren().addAll(
            createInventoryKpiCard("TỔNG NGUYÊN LIỆU", "32 loại", "8 nhóm danh mục", "#1C1917"),
            createInventoryKpiCard("TỒN KHO AN TOÀN", "27 loại", "Đủ bán 7 - 10 ngày", "#2E7D32"),
            createInventoryKpiCard("CẢNH BÁO THIẾU", "5 loại", "Cần nhập gấp trong 48h", "#C62828"),
            createInventoryKpiCard("GIÁ TRỊ TỒN KHO", "18.650.000đ", "Đơn giá nhập kho gần nhất", "#8C5835")
        );

        // 3. Toolbar: Tìm kiếm & Lọc nhóm
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        TextField txtSearch = new TextField();
        txtSearch.setPromptText("Tìm kiếm nguyên vật liệu...");
        txtSearch.setPrefWidth(260);
        txtSearch.setPrefHeight(36);
        txtSearch.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-background-color: #FFFFFF; -fx-border-color: #DED7CD; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-font-size: 12px; -fx-padding: 6 12 6 12;");

        HBox filterChips = new HBox(6);
        filterChips.setAlignment(Pos.CENTER_LEFT);
        filterChips.getChildren().addAll(
            createChipBtn("Tất cả (32)", true),
            createChipBtn("Cà phê & Trà (8)", false),
            createChipBtn("Sữa & Kem (6)", false),
            createChipBtn("Siro & Mứt (10)", false),
            createChipBtn("Topping (8)", false),
            createChipBtn("⚠️ Sắp hết (5)", false)
        );

        toolbar.getChildren().addAll(txtSearch, filterChips);

        // 4. Bảng danh sách nguyên liệu
        VBox tableCard = new VBox(6);
        tableCard.setPadding(new Insets(14, 16, 14, 16));
        tableCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10px; -fx-border-color: #E7E2DA; -fx-border-radius: 10px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.03), 8, 0, 0, 2);");
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        // Header Table
        HBox tblHeader = new HBox(10);
        tblHeader.setAlignment(Pos.CENTER_LEFT);
        tblHeader.setPadding(new Insets(0, 8, 8, 8));
        tblHeader.setStyle("-fx-border-color: transparent transparent #EFE8DD transparent; -fx-border-width: 0 0 1 0;");

        tblHeader.getChildren().addAll(
            createColHeader("MÃ NL", 80),
            createColHeader("TÊN NGUYÊN LIỆU", 240),
            createColHeader("PHÂN NHÓM", 130),
            createColHeader("TỒN THỰC TẾ", 110),
            createColHeader("ĐỊNH MỨC TỐI THIỂU", 140),
            createColHeader("TRẠNG THÁI", 120),
            createColHeader("THAO TÁC", 100)
        );

        VBox rowsContainer = new VBox(4);
        rowsContainer.getChildren().addAll(
            createInventoryRow("NL-01", "Cà phê hạt Arabica Cầu Đất", "Cà phê & Trà", "8.5 kg", "Tối thiểu 5.0 kg", "SAFE"),
            createInventoryRow("NL-02", "Cà phê hạt Robusta Buôn Ma Thuột", "Cà phê & Trà", "14.2 kg", "Tối thiểu 10.0 kg", "SAFE"),
            createInventoryRow("NL-03", "Sữa đặc Ngôi Sao Phương Nam", "Sữa & Kem", "28 lon", "Tối thiểu 15 lon", "SAFE"),
            createInventoryRow("NL-04", "Sữa tươi thanh trùng Dalat Milk", "Sữa & Kem", "6 hộp", "Tối thiểu 10 hộp", "WARNING"),
            createInventoryRow("NL-05", "Trà xanh Oolong Mộc Châu", "Cà phê & Trà", "3.2 kg", "Tối thiểu 2.0 kg", "SAFE"),
            createInventoryRow("NL-06", "Trà Sen Vàng Thượng Hạng", "Cà phê & Trà", "1.8 kg", "Tối thiểu 1.5 kg", "SAFE"),
            createInventoryRow("NL-07", "Kem sữa béo Rich's Non-Dairy", "Sữa & Kem", "12 hộp", "Tối thiểu 8 hộp", "SAFE"),
            createInventoryRow("NL-08", "Siro Đào Pomona Cao Cấp", "Siro & Mứt", "0.8 chai", "Tối thiểu 2.0 chai", "DANGER"),
            createInventoryRow("NL-09", "Siro Vải Monin Pháp", "Siro & Mứt", "1.1 chai", "Tối thiểu 2.0 chai", "WARNING"),
            createInventoryRow("NL-10", "Hạt sen tươi ngâm đường", "Topping", "4.5 hũ", "Tối thiểu 3.0 hũ", "SAFE"),
            createInventoryRow("NL-11", "Trân châu hoàng kim 3Q", "Topping", "7.0 túi", "Tối thiểu 5.0 túi", "SAFE"),
            createInventoryRow("NL-12", "Bột Matcha Uji Nhật Bản", "Matcha", "0.6 kg", "Tối thiểu 1.0 kg", "DANGER")
        );

        ScrollPane scroll = new ScrollPane(rowsContainer);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(280);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        tableCard.getChildren().addAll(tblHeader, scroll);

        centerContent.getChildren().addAll(header, kpiRow, toolbar, tableCard);
    }

    private VBox createInventoryKpiCard(String title, String val, String sub, String valColor) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10px; -fx-border-color: #E7E2DA; -fx-border-radius: 10px; -fx-border-width: 1px; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.03), 8, 0, 0, 2);");
        HBox.setHgrow(card, Priority.ALWAYS);

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 10.5px; -fx-font-weight: 700; -fx-text-fill: #817A70; -fx-letter-spacing: 0.5px;");

        Label lblVal = new Label(val);
        lblVal.setStyle("-fx-font-family: 'Playfair Display', Georgia, serif; -fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: " + valColor + ";");

        Label lblSub = new Label(sub);
        lblSub.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 10.5px; -fx-text-fill: #A8A196;");

        card.getChildren().addAll(lblTitle, lblVal, lblSub);
        return card;
    }

    private Button createChipBtn(String text, boolean isActive) {
        Button btn = new Button(text);
        btn.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-font-weight: " + (isActive ? "700" : "500") + "; "
            + "-fx-background-color: " + (isActive ? "#112823" : "#FFFFFF") + "; "
            + "-fx-text-fill: " + (isActive ? "#FFFFFF" : "#4A443C") + "; "
            + "-fx-border-color: " + (isActive ? "#112823" : "#DED7CD") + "; "
            + "-fx-border-radius: 20px; -fx-background-radius: 20px; -fx-padding: 5 12 5 12; -fx-cursor: hand;");
        AnimationHelper.applyButtonPress(btn);
        return btn;
    }

    private HBox createInventoryRow(String code, String name, String category, String stock, String minAlert, String status) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 8, 8, 8));
        row.setStyle("-fx-border-color: transparent transparent #EFE8DD transparent; -fx-border-width: 0 0 1 0;");

        Label lblCode = new Label(code);
        lblCode.setPrefWidth(80);
        lblCode.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #112823;");

        Label lblName = new Label(name);
        lblName.setPrefWidth(240);
        lblName.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #292722;");

        Label lblCat = new Label(category);
        lblCat.setPrefWidth(130);
        lblCat.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #6F695F;");

        Label lblStock = new Label(stock);
        lblStock.setPrefWidth(110);
        lblStock.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #1C1917;");

        Label lblMin = new Label(minAlert);
        lblMin.setPrefWidth(140);
        lblMin.setStyle("-fx-font-size: 11px; -fx-text-fill: #817A70;");

        Label badge = new Label();
        badge.setPrefWidth(120);
        if ("SAFE".equalsIgnoreCase(status)) {
            badge.setText("● Đủ dùng");
            badge.setStyle("-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32; -fx-font-size: 10.5px; -fx-font-weight: 600; -fx-background-radius: 8px; -fx-padding: 3 8 3 8;");
        } else if ("WARNING".equalsIgnoreCase(status)) {
            badge.setText("⚠️ Sắp hết");
            badge.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #E65100; -fx-font-size: 10.5px; -fx-font-weight: 600; -fx-background-radius: 8px; -fx-padding: 3 8 3 8;");
        } else {
            badge.setText("🔴 Cần nhập gấp");
            badge.setStyle("-fx-background-color: #FFEBEE; -fx-text-fill: #C62828; -fx-font-size: 10.5px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 3 8 3 8;");
        }

        Button btnAction = new Button("Nhập thêm");
        btnAction.setStyle("-fx-background-color: #F1EADF; -fx-text-fill: #563521; -fx-font-size: 10.5px; -fx-font-weight: 700; -fx-background-radius: 5px; -fx-padding: 4 8 4 8; -fx-cursor: hand;");
        AnimationHelper.applyButtonPress(btnAction);
        btnAction.setOnAction(e -> showToastNotification("Đã thêm " + name + " vào danh sách đề xuất nhập hàng!"));

        row.getChildren().addAll(lblCode, lblName, lblCat, lblStock, lblMin, badge, btnAction);
        return row;
    }

    // =========================================================================
    // TAB 8: BÁO CÁO DOANH THU & PHÂN TÍCH (FINANCIAL REPORTS & ANALYTICS)
    // =========================================================================
    private void renderReportsTab() {
        centerContent.getChildren().clear();
        centerContent.setSpacing(14);

        // 1. Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label lblTitle = new Label("Báo Cáo Doanh Thu & Hiệu Quả Kinh Doanh");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        Label lblSub = new Label("Phân tích doanh số bán hàng, tỷ suất lợi nhuận & cơ cấu sản phẩm thời gian thực");
        lblSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #817A70;");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        HBox periodBox = new HBox(6);
        periodBox.getChildren().addAll(
            createChipBtn("Hôm nay", false),
            createChipBtn("7 ngày qua", false),
            createChipBtn("Tháng này", true),
            createChipBtn("Quý này", false)
        );

        Button btnExport = new Button("📥 Xuất Excel");
        btnExport.setPrefHeight(36);
        btnExport.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 6 14 6 14; -fx-cursor: hand;");
        AnimationHelper.applyButtonPress(btnExport);
        btnExport.setOnAction(e -> showToastNotification("Đã xuất báo cáo doanh thu ra file Excel thành công!"));

        header.getChildren().addAll(titleBox, sp, periodBox, btnExport);

        // 2. 4 Thẻ KPI Doanh thu & Lợi nhuận
        HBox kpiRow = new HBox(12);
        kpiRow.setAlignment(Pos.CENTER);
        kpiRow.getChildren().addAll(
            createInventoryKpiCard("DOANH THU THÁNG NÀY", "128.450.000đ", "+14.2% so với tháng trước", "#1C1917"),
            createInventoryKpiCard("LỢI NHUẬN GỘP", "78.320.000đ", "Tỷ suất lợi nhuận 60.9%", "#2E7D32"),
            createInventoryKpiCard("TỔNG SỐ ĐƠN HÀNG", "2.140 đơn", "Trung bình 71 đơn / ngày", "#112823"),
            createInventoryKpiCard("GIÁ TRỊ TB / ĐƠN (AOV)", "60.020đ", "+4.5% so với tháng trước", "#8C5835")
        );

        // 3. Hai Panel phân tích song song: Doanh thu 7 ngày & Cơ cấu món
        HBox chartsRow = new HBox(14);
        chartsRow.setAlignment(Pos.TOP_LEFT);

        // Panel Trái: Bảng kê chi tiết 7 ngày gần nhất
        VBox dailyPanel = buildDailyRevenuePanel();
        HBox.setHgrow(dailyPanel, Priority.ALWAYS);

        // Panel Phải: Cơ cấu doanh thu theo nhóm món
        VBox categorySharePanel = buildCategorySharePanel();
        categorySharePanel.setPrefWidth(360);
        categorySharePanel.setMinWidth(340);

        chartsRow.getChildren().addAll(dailyPanel, categorySharePanel);

        // 4. Panel Khung giờ vàng bán chạy trong ngày
        VBox peakHoursPanel = buildPeakHoursPanel();
        VBox.setVgrow(peakHoursPanel, Priority.ALWAYS);

        centerContent.getChildren().addAll(header, kpiRow, chartsRow, peakHoursPanel);
    }

    private VBox buildDailyRevenuePanel() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(14, 16, 14, 16));
        box.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10px; -fx-border-color: #E7E2DA; -fx-border-radius: 10px; -fx-border-width: 1px;");

        HBox head = new HBox(8);
        head.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Doanh Số & Lợi Nhuận 7 Ngày Gần Nhất");
        title.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        head.getChildren().add(title);

        HBox tblHead = new HBox(8);
        tblHead.setAlignment(Pos.CENTER_LEFT);
        tblHead.setPadding(new Insets(4, 6, 6, 6));
        tblHead.setStyle("-fx-border-color: transparent transparent #EFE8DD transparent; -fx-border-width: 0 0 1 0;");
        tblHead.getChildren().addAll(
            createColHeader("NGÀY", 85),
            createColHeader("SỐ ĐƠN", 75),
            createColHeader("DOANH THU", 105),
            createColHeader("GIÁ VỐN", 95),
            createColHeader("LỢI NHUẬN", 100),
            createColHeader("TỶ SUẤT", 70)
        );

        VBox list = new VBox(2);
        list.getChildren().addAll(
            createDailyRow("Hôm nay", "126 đơn", "8.420.000đ", "3.280.000đ", "+5.140.000đ", "61.0%"),
            createDailyRow("Hôm qua", "118 đơn", "7.850.000đ", "3.100.000đ", "+4.750.000đ", "60.5%"),
            createDailyRow("13/10/2026", "135 đơn", "9.120.000đ", "3.550.000đ", "+5.570.000đ", "61.1%"),
            createDailyRow("12/10/2026", "142 đơn", "9.680.000đ", "3.720.000đ", "+5.960.000đ", "61.5%"),
            createDailyRow("11/10/2026", "105 đơn", "6.940.000đ", "2.750.000đ", "+4.190.000đ", "60.3%"),
            createDailyRow("10/10/2026", "98 đơn", "6.420.000đ", "2.560.000đ", "+3.860.000đ", "60.1%"),
            createDailyRow("09/10/2026", "112 đơn", "7.480.000đ", "2.920.000đ", "+4.560.000đ", "60.9%")
        );

        box.getChildren().addAll(head, tblHead, list);
        return box;
    }

    private HBox createDailyRow(String date, String orders, String rev, String cost, String profit, String margin) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(5, 6, 5, 6));
        row.setStyle("-fx-border-color: transparent transparent #F7F2EA transparent; -fx-border-width: 0 0 1 0;");

        Label l1 = new Label(date); l1.setPrefWidth(85); l1.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 600; -fx-text-fill: #292722;");
        Label l2 = new Label(orders); l2.setPrefWidth(75); l2.setStyle("-fx-font-size: 11px; -fx-text-fill: #6F695F;");
        Label l3 = new Label(rev); l3.setPrefWidth(105); l3.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #1C1917;");
        Label l4 = new Label(cost); l4.setPrefWidth(95); l4.setStyle("-fx-font-size: 11px; -fx-text-fill: #817A70;");
        Label l5 = new Label(profit); l5.setPrefWidth(100); l5.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #2E7D32;");
        Label l6 = new Label(margin); l6.setPrefWidth(70); l6.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #112823;");

        row.getChildren().addAll(l1, l2, l3, l4, l5, l6);
        return row;
    }

    private VBox buildCategorySharePanel() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(14, 16, 14, 16));
        box.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10px; -fx-border-color: #E7E2DA; -fx-border-radius: 10px; -fx-border-width: 1px;");

        Label title = new Label("Cơ Cấu Doanh Thu Theo Nhóm Món");
        title.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #292722;");

        VBox items = new VBox(10);
        items.getChildren().addAll(
            createCategoryShareItem("Cà phê pha máy & truyền thống", 42, "53.949.000đ", "#112823"),
            createCategoryShareItem("Trà hoa quả tươi & Trà sen", 28, "35.966.000đ", "#4B704F"),
            createCategoryShareItem("Đá xay sinh tố & Trà sữa", 16, "20.552.000đ", "#D97706"),
            createCategoryShareItem("Bánh ngọt & Món ăn nhẹ", 14, "17.983.000đ", "#8C5835")
        );

        box.getChildren().addAll(title, items);
        return box;
    }

    private VBox createCategoryShareItem(String catName, int percent, String amount, String colorHex) {
        VBox box = new VBox(4);

        HBox labels = new HBox(6);
        labels.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(catName);
        name.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 600; -fx-text-fill: #292722;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label val = new Label(amount + " (" + percent + "%)");
        val.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: " + colorHex + ";");
        labels.getChildren().addAll(name, sp, val);

        // Progress track
        StackPane track = new StackPane();
        track.setPrefHeight(7);
        track.setMaxHeight(7);
        track.setStyle("-fx-background-color: #EFE8DD; -fx-background-radius: 4px;");

        Region bar = new Region();
        bar.setPrefHeight(7);
        bar.setMaxHeight(7);
        bar.setStyle("-fx-background-color: " + colorHex + "; -fx-background-radius: 4px;");
        bar.setPrefWidth(percent * 3.0);
        StackPane.setAlignment(bar, Pos.CENTER_LEFT);

        track.getChildren().add(bar);
        box.getChildren().addAll(labels, track);
        return box;
    }

    private VBox buildPeakHoursPanel() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(14, 16, 14, 16));
        box.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10px; -fx-border-color: #E7E2DA; -fx-border-radius: 10px; -fx-border-width: 1px;");

        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Phân Khúc Hiệu Suất Theo Khung Giờ Trong Ngày");
        title.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #292722;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        HBox actions = new HBox(8);
        Button btnPrint = new Button("🖨️ In Tổng Kết Ca");
        btnPrint.setStyle("-fx-background-color: #F1EADF; -fx-text-fill: #563521; -fx-font-size: 11px; -fx-font-weight: 700; -fx-background-radius: 6px; -fx-padding: 5 12 5 12; -fx-cursor: hand;");
        AnimationHelper.applyButtonPress(btnPrint);
        btnPrint.setOnAction(e -> showToastNotification("Lệnh in phiếu tổng kết doanh thu ca trực đã gửi đến máy in POS!"));

        Button btnPdf = new Button("📄 Báo Cáo Tài Chính (PDF)");
        btnPdf.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-size: 11px; -fx-font-weight: 700; -fx-background-radius: 6px; -fx-padding: 5 12 5 12; -fx-cursor: hand;");
        AnimationHelper.applyButtonPress(btnPdf);
        btnPdf.setOnAction(e -> showToastNotification("Đã kết xuất báo cáo tài chính PDF hoàn tất!"));

        actions.getChildren().addAll(btnPrint, btnPdf);
        top.getChildren().addAll(title, sp, actions);

        HBox grid = new HBox(12);
        grid.getChildren().addAll(
            createPeakHourItem("07:00 - 09:30", "45 đơn (36%)", "Cao điểm sáng", "Khách công sở mua cà phê & bánh"),
            createPeakHourItem("11:30 - 13:30", "28 đơn (22%)", "Nghỉ trưa", "Trà trái cây mát & cơm trưa nhẹ"),
            createPeakHourItem("14:30 - 17:00", "32 đơn (25%)", "Thư giãn chiều", "Trà sữa, sinh tố & đá xay"),
            createPeakHourItem("18:30 - 21:30", "21 đơn (17%)", "Buổi tối", "Gặp gỡ bạn bè & đồ uống ấm")
        );

        box.getChildren().addAll(top, grid);
        return box;
    }

    private VBox createPeakHourItem(String time, String count, String tag, String note) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setStyle("-fx-background-color: #FBF9F5; -fx-background-radius: 8px; -fx-border-color: #EFE8DD; -fx-border-radius: 8px;");
        HBox.setHgrow(card, Priority.ALWAYS);

        HBox r1 = new HBox(6);
        r1.setAlignment(Pos.CENTER_LEFT);
        Label lblTime = new Label(time);
        lblTime.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #112823;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label lblTag = new Label(tag);
        lblTag.setStyle("-fx-font-size: 9.5px; -fx-font-weight: 700; -fx-background-color: #EFE8DD; -fx-text-fill: #563521; -fx-background-radius: 4px; -fx-padding: 2 5 2 5;");
        r1.getChildren().addAll(lblTime, sp, lblTag);

        Label lblCount = new Label(count);
        lblCount.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 800; -fx-text-fill: #292722;");

        Label lblNote = new Label(note);
        lblNote.setStyle("-fx-font-size: 10px; -fx-text-fill: #817A70;");

        card.getChildren().addAll(r1, lblCount, lblNote);
        return card;
    }

    private void showToastNotification(String msg) {
        System.out.println("[COFFEPOS ADMIN] " + msg);
    }

    // =========================================================================
    // TAB 8: CÀI ĐẶT (SETTINGS)
    // =========================================================================
    private void renderSettingsTab() {
        centerContent.getChildren().clear();
        centerContent.setSpacing(12);

        Label lbl = new Label("Cài Đặt Hệ Thống & Trạng Thái Mạng");
        lbl.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #292722;");

        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8px; -fx-border-color: #E7E2DA; -fx-border-radius: 8px;");

        Label portInfo = new Label("• Cổng TCP Socket Máy Chủ: Port 5000 (Đang mở)");
        Label clientsInfo = new Label("• Thiết bị kết nối hiện tại: " + serverService.getActiveSessions().size() + " thiết bị");
        Label dbInfo = new Label("• Trạng thái SQL Server: " + (DatabaseManager.isAvailable() ? "Đã kết nối (CafeOrderDB)" : "Ngoại tuyến / Sử dụng In-Memory"));

        portInfo.setStyle("-fx-font-size: 12px; -fx-text-fill: #292722;");
        clientsInfo.setStyle("-fx-font-size: 12px; -fx-text-fill: #292722;");
        dbInfo.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (DatabaseManager.isAvailable() ? "#2E7D32" : "#E65100") + "; -fx-font-weight: 600;");

        card.getChildren().addAll(portInfo, clientsInfo, dbInfo);
        centerContent.getChildren().addAll(lbl, card);
    }

    // =========================================================================
    // TAB 2: GỌI MÓN (COUNTER POS)
    // =========================================================================
    private void renderCounterPosTab() {
        centerContent.getChildren().clear();
        centerContent.setSpacing(12);

        Label lbl = new Label("Gọi Món Trực Tiếp Tại Quầy Thu Ngân");
        lbl.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #292722;");

        ClientView counterView = new ClientView();
        VBox.setVgrow(counterView, Priority.ALWAYS);
        centerContent.getChildren().addAll(lbl, counterView);
    }

    // =========================================================================
    // LIVE EVENT LISTENERS (REAL-TIME DASHBOARD SYNC)
    // =========================================================================
    private void registerLiveListeners() {
        serverService.setOnOrderStatusChanged((orderId, status) -> {
            Platform.runLater(() -> {
                if (currentTab == 1) {
                    populateRecentOrders();
                    refreshKpiNumbers();
                }
            });
        });
    }

    public void notifyNewOrderArrived(Order order) {
        Platform.runLater(() -> {
            if (currentTab == 1) {
                populateRecentOrders();
                refreshKpiNumbers();
            }
        });
    }
}
