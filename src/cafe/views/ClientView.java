package cafe.views;

import cafe.models.MenuItem;
import cafe.utils.AnimationHelper;
import cafe.utils.SvgIcons;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

/**
 * ClientView - Giao diện Kiosk Thực đơn (Screen 3 trong UI_mong_muon.png):
 * - Left Sidebar (110px): Logo, Nút Sơ đồ bàn, 7 danh mục có icon cố định 22px giúp cột chữ thẳng tắp 100%
 * - Top Bar: Bàn 05 (Playfair Display), Badge "Đang phục vụ" (#DCE8D9 / #597256), Thanh tìm kiếm 31px, Nút Giỏ hàng nổi
 * - Lưới thực đơn: 3 cột, card 6px radius, ảnh 5px radius, nút Quick-Add (+) 20px #112823 ở góc dưới ảnh
 */
public class ClientView extends BorderPane {

    private final VBox categoryNavBox = new VBox();
    private final GridPane itemsGridPane = new GridPane();
    private final ScrollPane catalogScrollPane = new ScrollPane();
    private final VBox catalogContainer = new VBox(12);
    private HBox mixiPromoBanner;
    private String currentActiveCategory = "Tất cả";
    private final TextField txtSearch = new TextField();

    private final Button btnFloatingCart = new Button();
    private final Button btnLogout = new Button();
    private final HBox cartButtonContent = new HBox();
    private final Label lblCartBadge = new Label("0");

    private final Label lblTableBadge = new Label("Bàn 05");
    private final Label lblTableStatus = new Label("● Đang phục vụ");
    private final Label lblConnectionStatus = new Label("● Đã kết nối");
    private final Label lblLiveClock = new Label();

    private final Button btnFloorPlan = new Button();
    private final Button btnSidebarLogout = new Button();
    private Runnable onLogoutCallback;

    private final HBox notificationToast = new HBox();
    private final Label lblToastMessage = new Label();

    private final VBox activeOrderBox = new VBox(2);
    private final Label lblActiveOrderId = new Label();
    private final Label lblActiveOrderStatus = new Label();

    private final List<ItemCard> currentCards = new ArrayList<>();
    private int currentColumns = 3;

    public ClientView() {
        initUI();
        startClock();
    }

    private void initUI() {
        setStyle("-fx-background-color: #F5F0E8;");

        // 1. CỘT TRÁI: DANH MỤC DỌC (LEFT SIDEBAR 110px)
        VBox leftSidebar = buildLeftCategorySidebar();
        setLeft(leftSidebar);

        // 2. KHU VỰC TRUNG TÂM TOÀN MÀN HÌNH: HEADER & LƯỚI THỰC ĐƠN
        VBox centerArea = buildCenterCatalogArea();
        setCenter(centerArea);

        // 3. TOAST THÔNG BÁO DƯỚI CÙNG
        notificationToast.setAlignment(Pos.CENTER);
        notificationToast.setPadding(new Insets(8, 18, 8, 18));
        notificationToast.setStyle("-fx-background-color: #112823; -fx-background-radius: 6px; -fx-effect: dropshadow(gaussian, rgba(17, 40, 35, 0.25), 8, 0, 0, 2);");
        notificationToast.setVisible(false);
        notificationToast.setManaged(false);
        lblToastMessage.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: 600; -fx-font-size: 11.5px;");
        notificationToast.getChildren().add(lblToastMessage);

        StackPane bottomWrapper = new StackPane(notificationToast);
        bottomWrapper.setPadding(new Insets(0, 0, 14, 0));
        bottomWrapper.setPickOnBounds(false);
        setBottom(bottomWrapper);
    }

    private VBox leftCategorySidebar;
    private VBox sidebarBrandBox;

    private final HBox categoryPillBar = new HBox(8);
    private final ScrollPane categoryPillScroll = new ScrollPane(categoryPillBar);
    private boolean embeddedMode = false;
    private List<String> lastCategories;
    private String lastSelectedCategory;
    private Consumer<String> lastOnCategorySelect;

    public void configureForEmbeddedShell(boolean embedded) {
        this.embeddedMode = embedded;
        if (embedded) {
            // Ẩn hoàn toàn Sidebar dọc để tránh "2 Sidebar đứng cạnh nhau" -> giải phóng 190-235px chiều ngang
            setLeft(null);
            categoryPillScroll.setVisible(true);
            categoryPillScroll.setManaged(true);
            btnLogout.setVisible(false);
            btnLogout.setManaged(false);
            btnSidebarLogout.setVisible(false);
            btnSidebarLogout.setManaged(false);
            if (lastCategories != null && lastOnCategorySelect != null) {
                renderCategories(lastCategories, lastSelectedCategory, lastOnCategorySelect);
            }
        } else {
            setLeft(leftCategorySidebar);
            categoryPillScroll.setVisible(false);
            categoryPillScroll.setManaged(false);
            btnLogout.setVisible(true);
            btnLogout.setManaged(true);
            btnSidebarLogout.setVisible(true);
            btnSidebarLogout.setManaged(true);
        }
    }

    private void renderCategoryPills(List<String> cleanList, String selectedCategory, Consumer<String> onSelect) {
        categoryPillBar.getChildren().clear();
        for (String rawCat : cleanList) {
            boolean isActive = rawCat.equalsIgnoreCase(selectedCategory);
            Button pill = new Button(getConciseCategoryLabel(rawCat));
            pill.setFocusTraversable(false);
            AnimationHelper.applyButtonPress(pill);
            pill.setStyle(isActive
                ? "-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-background-radius: 999px; -fx-border-color: #112823; -fx-border-radius: 999px; -fx-padding: 6 16 6 16; -fx-cursor: hand;"
                : "-fx-background-color: #FFFFFF; -fx-text-fill: #4A453E; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 600; -fx-background-radius: 999px; -fx-border-color: #E2DDD5; -fx-border-radius: 999px; -fx-padding: 6 16 6 16; -fx-cursor: hand;");
            pill.setOnAction(e -> onSelect.accept(rawCat));
            categoryPillBar.getChildren().add(pill);
        }
    }

    private VBox buildLeftCategorySidebar() {
        VBox sidebar = new VBox(10);
        this.leftCategorySidebar = sidebar;
        sidebar.setPrefWidth(235);
        sidebar.setMinWidth(220);
        sidebar.setMaxWidth(255);
        sidebar.setPadding(new Insets(20, 12, 20, 12));
        sidebar.setStyle("-fx-background-color: #F7F2EA; -fx-border-color: transparent #DED7CD transparent transparent; -fx-border-width: 0 1 0 0;");

        // Brand Logo (Playfair Display)
        VBox brandBox = new VBox(3);
        this.sidebarBrandBox = brandBox;
        brandBox.setAlignment(Pos.CENTER_LEFT);
        brandBox.setPadding(new Insets(4, 8, 16, 8));

        Label lblBrand = new Label("Brew & Bean");
        lblBrand.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 17.5px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        Label lblSub = new Label("KIOSK • GỌI MÓN");
        lblSub.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #563521; -fx-letter-spacing: 1.5px;");
        brandBox.getChildren().addAll(lblBrand, lblSub);

        // Nút quay lại Sơ đồ bàn (cho nhân viên khi đặt món hộ)
        btnFloorPlan.setMaxWidth(Double.MAX_VALUE);
        btnFloorPlan.setFocusTraversable(false);
        btnFloorPlan.setAlignment(Pos.CENTER_LEFT);
        btnFloorPlan.setPadding(new Insets(10, 14, 10, 14));
        HBox floorPlanBox = new HBox(12);
        floorPlanBox.setAlignment(Pos.CENTER_LEFT);

        StackPane iconWrapper = new StackPane();
        iconWrapper.setPrefSize(26, 26);
        iconWrapper.setMinSize(26, 26);
        iconWrapper.setMaxSize(26, 26);
        iconWrapper.setAlignment(Pos.CENTER);
        Node tableIcon = cafe.utils.IconHelper.getIcon("client", "so-do-ban.png", SvgIcons.TABLE, "", 20, Color.web("#3F3931"));
        iconWrapper.getChildren().add(tableIcon);

        Label lblFloorPlan = new Label("Sơ đồ bàn");
        lblFloorPlan.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #3F3931;");
        floorPlanBox.getChildren().addAll(iconWrapper, lblFloorPlan);
        btnFloorPlan.setGraphic(floorPlanBox);
        btnFloorPlan.setStyle("-fx-background-color: transparent; -fx-background-radius: 8px; -fx-cursor: hand;");
        btnFloorPlan.setOnMouseEntered(e -> btnFloorPlan.setStyle("-fx-background-color: #EFE8DD; -fx-background-radius: 8px; -fx-cursor: hand;"));
        btnFloorPlan.setOnMouseExited(e -> btnFloorPlan.setStyle("-fx-background-color: transparent; -fx-background-radius: 8px; -fx-cursor: hand;"));
        AnimationHelper.applyButtonPress(btnFloorPlan);

        // Categories list
        categoryNavBox.setSpacing(5);

        ScrollPane scrollNav = new ScrollPane(categoryNavBox);
        scrollNav.setFitToWidth(true);
        scrollNav.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollNav.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollNav.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollNav, Priority.ALWAYS);

        // Live Order Tracking Card at bottom of sidebar
        activeOrderBox.setPadding(new Insets(10));
        activeOrderBox.setStyle("-fx-background-color: #FBF9F5; -fx-border-color: #DED7CD; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-border-width: 1px;");
        activeOrderBox.setVisible(false);
        activeOrderBox.setManaged(false);

        HBox orderTrackHeader = new HBox(6);
        orderTrackHeader.setAlignment(Pos.CENTER_LEFT);
        Node trackIcon = cafe.utils.IconHelper.getIcon("client", "theo-doi-don.png", SvgIcons.CLOCK, "⏱", 16, Color.web("#E5A04D"));
        Label lblTrackTitle = new Label("Tiến độ đơn");
        lblTrackTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #817A70;");
        orderTrackHeader.getChildren().addAll(trackIcon, lblTrackTitle);

        lblActiveOrderId.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        lblActiveOrderStatus.setStyle("-fx-font-size: 10px; -fx-font-weight: 600; -fx-text-fill: #E5A04D;");
        activeOrderBox.getChildren().addAll(orderTrackHeader, lblActiveOrderId, lblActiveOrderStatus);

        // Nút Đăng xuất ở chân Sidebar
        btnSidebarLogout.setMaxWidth(Double.MAX_VALUE);
        btnSidebarLogout.setPrefHeight(38);
        btnSidebarLogout.setStyle("-fx-background-color: transparent; -fx-text-fill: #B91C1C; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-border-color: #FECACA; -fx-border-radius: 8px; -fx-border-width: 1px; -fx-cursor: hand; -fx-padding: 6 12 6 12;");
        btnSidebarLogout.setOnMouseEntered(e -> btnSidebarLogout.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-border-color: #F87171; -fx-border-radius: 8px; -fx-border-width: 1px; -fx-cursor: hand; -fx-padding: 6 12 6 12;"));
        btnSidebarLogout.setOnMouseExited(e -> btnSidebarLogout.setStyle("-fx-background-color: transparent; -fx-text-fill: #B91C1C; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-border-color: #FECACA; -fx-border-radius: 8px; -fx-border-width: 1px; -fx-cursor: hand; -fx-padding: 6 12 6 12;"));
        AnimationHelper.applyButtonPress(btnSidebarLogout);

        HBox sbLogoutBox = new HBox(8);
        sbLogoutBox.setAlignment(Pos.CENTER);
        SVGPath sbLogoutIcon = SvgIcons.createOutlineIcon(SvgIcons.LOG_OUT, 0.7, Color.web("#B91C1C"));
        Label lblSbLogout = new Label("Đăng xuất bàn");
        lblSbLogout.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #B91C1C;");
        sbLogoutBox.getChildren().addAll(sbLogoutIcon, lblSbLogout);
        btnSidebarLogout.setGraphic(sbLogoutBox);
        btnSidebarLogout.setOnAction(e -> {
            if (onLogoutCallback != null) onLogoutCallback.run();
        });

        sidebar.getChildren().addAll(brandBox, btnFloorPlan, scrollNav, activeOrderBox, btnSidebarLogout);
        return sidebar;
    }

    private VBox buildCenterCatalogArea() {
        VBox center = new VBox(12);
        center.setPadding(new Insets(16, 22, 16, 22));
        HBox.setHgrow(center, Priority.ALWAYS);

        // Top Utility Bar: Table info, status pill, search bar, and Floating Cart Button
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        lblTableBadge.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        lblTableStatus.setStyle("-fx-background-color: #DCE8D9; -fx-text-fill: #597256; -fx-font-size: 10px; -fx-font-weight: 600; -fx-background-radius: 10px; -fx-padding: 2 8 2 8;");
        titleBox.getChildren().addAll(lblTableBadge, lblTableStatus);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Search Input Bar (height 42px, radius 8px, #FBF9F5) with tim-kiem.png icon
        HBox searchBox = new HBox(8);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPrefHeight(42);
        searchBox.setMinHeight(42);
        searchBox.setPrefWidth(320);
        searchBox.setPadding(new Insets(0, 12, 0, 12));
        searchBox.setStyle("-fx-background-color: #FBF9F5; -fx-border-color: #DED7CD; -fx-border-radius: 8px; -fx-background-radius: 8px;");

        Node searchIcon = cafe.utils.IconHelper.getIcon("client", "tim-kiem.png", SvgIcons.SEARCH, "🔍", 18, Color.web("#817A70"));
        txtSearch.setPromptText("Tìm kiếm món ...");
        txtSearch.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 13.5px; -fx-padding: 0;");
        HBox.setHgrow(txtSearch, Priority.ALWAYS);
        searchBox.getChildren().addAll(searchIcon, txtSearch);

        // Floating Cart Button with Shopping Bag Icon + Badge (height 42px, radius 8px)
        btnFloatingCart.setPrefHeight(42);
        btnFloatingCart.setMinHeight(42);
        btnFloatingCart.setStyle("-fx-background-color: #FBF9F5; -fx-background-radius: 8px; -fx-border-color: #DED7CD; -fx-border-radius: 8px; -fx-border-width: 1px; -fx-padding: 6 16 6 14; -fx-cursor: hand;");
        AnimationHelper.applyButtonPress(btnFloatingCart);

        cartButtonContent.setAlignment(Pos.CENTER);
        cartButtonContent.setSpacing(8);
        SVGPath cartIcon = SvgIcons.createOutlineIcon(SvgIcons.SHOPPING_BAG, 0.85, Color.web("#112823"));
        Label lblCartText = new Label("Giỏ hàng");
        lblCartText.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #112823;");

        lblCartBadge.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-size: 10px; -fx-font-weight: 700; -fx-background-radius: 10px; -fx-padding: 2 6 2 6;");
        cartButtonContent.getChildren().addAll(cartIcon, lblCartText, lblCartBadge);
        btnFloatingCart.setGraphic(cartButtonContent);

        // Logout / Switch Table Button (height 42px, radius 8px)
        btnLogout.setPrefHeight(42);
        btnLogout.setMinHeight(42);
        btnLogout.setStyle("-fx-background-color: #FBF9F5; -fx-text-fill: #991B1B; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-border-color: #DED7CD; -fx-border-radius: 8px; -fx-border-width: 1px; -fx-padding: 6 14 6 12; -fx-cursor: hand;");
        btnLogout.setOnMouseEntered(e -> btnLogout.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-border-color: #FECACA; -fx-border-radius: 8px; -fx-border-width: 1px; -fx-padding: 6 14 6 12; -fx-cursor: hand;"));
        btnLogout.setOnMouseExited(e -> btnLogout.setStyle("-fx-background-color: #FBF9F5; -fx-text-fill: #991B1B; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-border-color: #DED7CD; -fx-border-radius: 8px; -fx-border-width: 1px; -fx-padding: 6 14 6 12; -fx-cursor: hand;"));
        AnimationHelper.applyButtonPress(btnLogout);

        HBox logoutContent = new HBox(6);
        logoutContent.setAlignment(Pos.CENTER);
        SVGPath logoutIcon = SvgIcons.createOutlineIcon(SvgIcons.LOG_OUT, 0.75, Color.web("#991B1B"));
        Label lblLogoutText = new Label("Đổi bàn / Thoát");
        lblLogoutText.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #991B1B;");
        logoutContent.getChildren().addAll(logoutIcon, lblLogoutText);
        btnLogout.setGraphic(logoutContent);

        topBar.getChildren().addAll(titleBox, spacer, searchBox, btnFloatingCart, btnLogout);

        // Catalog Grid inside ScrollPane
        itemsGridPane.setHgap(12);
        itemsGridPane.setVgap(14);
        itemsGridPane.setPadding(new Insets(6, 2, 16, 2));

        catalogContainer.setFillWidth(true);
        catalogContainer.getChildren().add(itemsGridPane);
        catalogScrollPane.setContent(catalogContainer);
        catalogScrollPane.setFitToWidth(true);
        catalogScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        catalogScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        catalogScrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(catalogScrollPane, Priority.ALWAYS);

        // Responsive Column Scaling: Tự động tái tính số cột linh hoạt theo kích thước cửa sổ
        catalogScrollPane.widthProperty().addListener((obs, oldW, newW) -> {
            if (newW != null && newW.doubleValue() > 0 && !currentCards.isEmpty()) {
                int cols = calculateOptimalColumns(newW.doubleValue());
                if (cols != currentColumns) {
                    currentColumns = cols;
                    rebuildGridLayout(cols);
                }
            }
        });

        categoryPillBar.setAlignment(Pos.CENTER_LEFT);
        categoryPillBar.setPadding(new Insets(2, 2, 6, 2));
        categoryPillScroll.setFitToHeight(true);
        categoryPillScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        categoryPillScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        categoryPillScroll.setMinHeight(44);
        categoryPillScroll.setPrefHeight(44);
        categoryPillScroll.setMaxHeight(44);
        categoryPillScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        categoryPillScroll.setVisible(embeddedMode);
        categoryPillScroll.setManaged(embeddedMode);

        center.getChildren().addAll(topBar, categoryPillScroll, catalogScrollPane);
        return center;
    }

    private int calculateOptimalColumns(double availableWidth) {
        if (availableWidth <= 0) return 3;
        int cols = (int) ((availableWidth - 32) / 195);
        return Math.max(3, Math.min(cols, 6));
    }

    public void renderMenuItems(List<MenuItem> items, Consumer<MenuItem> onSelect, Consumer<MenuItem> onQuickAdd) {
        itemsGridPane.getChildren().clear();
        currentCards.clear();

        for (MenuItem item : items) {
            ItemCard card = new ItemCard(item, onSelect, onQuickAdd, this::showToast);
            currentCards.add(card);
        }

        if (mixiPromoBanner == null) {
            mixiPromoBanner = buildMixiFoodPromoBanner(onQuickAdd);
        }
        boolean showPromo = (currentActiveCategory == null || currentActiveCategory.equalsIgnoreCase("Tất cả") || currentActiveCategory.toLowerCase().contains("món ăn nhẹ"));
        mixiPromoBanner.setVisible(showPromo);
        mixiPromoBanner.setManaged(showPromo);

        catalogContainer.getChildren().clear();
        catalogContainer.getChildren().addAll(mixiPromoBanner, itemsGridPane);

        double width = catalogScrollPane.getWidth();
        int cols = calculateOptimalColumns(width > 0 ? width : 750);
        this.currentColumns = cols;
        rebuildGridLayout(cols);
    }

    /**
     * Banner Marketing Khô Gà Lá Chanh MixiFood (Độ Mixi) - Best Seller số 1 quán:
     * Tích hợp câu slogan hài hước, viral từ Tộc Trưởng Độ Mixi và nút gọi món nhanh 1 chạm.
     */
    private HBox buildMixiFoodPromoBanner(Consumer<MenuItem> onQuickAdd) {
        HBox banner = new HBox(16);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setPrefHeight(138);
        banner.setMinHeight(138);
        banner.setPadding(new Insets(10, 18, 10, 14));
        banner.setStyle(
            "-fx-background-color: linear-gradient(to right, #1C1917 0%, #292524 45%, #442B1D 100%); " +
            "-fx-background-radius: 14px; " +
            "-fx-border-color: #F59E0B; " +
            "-fx-border-radius: 14px; " +
            "-fx-border-width: 1.5px; " +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.18), 12, 0.1, 0, 4);"
        );

        // 1. Ảnh Độ Mixi cầm hũ Khô gà - Siêu nét Retina x2, chuẩn tỷ lệ 4:3 (152x114), bo góc 14px hoàn hảo
        ImageView imgView = new ImageView();
        try {
            java.io.InputStream is = getClass().getResourceAsStream("/cafe/resources/images/products/kho_ga_do_mixi_banner.png");
            if (is != null) {
                // Nạp decode-time nội suy mịn 320x240 để khử hoàn toàn hiện tượng vỡ hạt/mờ đục
                Image img = new Image(is, 320, 240, true, true);
                imgView.setImage(img);
                imgView.setFitWidth(152);
                imgView.setFitHeight(114);
                imgView.setPreserveRatio(true);
                imgView.setSmooth(true);
            }
        } catch (Exception ignored) {}

        Rectangle imgClip = new Rectangle(152, 114);
        imgClip.setArcWidth(14);
        imgClip.setArcHeight(14);
        imgView.setClip(imgClip);

        StackPane imgWrapper = new StackPane(imgView);
        imgWrapper.setPrefSize(152, 114);
        imgWrapper.setMinSize(152, 114);
        imgWrapper.setMaxSize(152, 114);
        imgWrapper.setStyle("-fx-background-color: transparent; -fx-background-radius: 14px;");

        // 2. Nội dung Text Marketing & Câu nói viral
        VBox textCol = new VBox(4);
        textCol.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textCol, Priority.ALWAYS);

        HBox badgeRow = new HBox(8);
        badgeRow.setAlignment(Pos.CENTER_LEFT);

        Label lblBestSellerBadge = new Label("🔥 BEST SELLER SỐ 1 QUÁN");
        lblBestSellerBadge.setStyle("-fx-background-color: #EA580C; -fx-text-fill: #FFFFFF; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 10px; -fx-font-weight: 800; -fx-background-radius: 6px; -fx-padding: 2 7 2 7;");

        Label lblMixiBadge = new Label("👑 MIXIFOOD x BREW & BEAN");
        lblMixiBadge.setStyle("-fx-background-color: #78350F; -fx-text-fill: #FDE68A; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 10px; -fx-font-weight: 800; -fx-background-radius: 6px; -fx-padding: 2 7 2 7;");

        Label lblRating = new Label("⭐ 4.9/5 (1.8k+ đã bán)");
        lblRating.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 10.5px; -fx-font-weight: 700; -fx-text-fill: #FBBF24;");

        badgeRow.getChildren().addAll(lblBestSellerBadge, lblMixiBadge, lblRating);

        Label lblTitle = new Label("Khô Gà Lá Chanh MixiFood • Cực Phẩm Tộc Trưởng");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', 'Segoe UI', serif; -fx-font-size: 16.5px; -fx-font-weight: 800; -fx-text-fill: #FFFFFF;");

        Label lblSlogan = new Label("“Ăn một miếng là lươn lẹo cả ngày! Cay cay giòn rụm, đượm vị lá chanh — Nhâm nhi cùng cà phê là hết nước chấm anh em ơi!”");
        lblSlogan.setWrapText(true);
        lblSlogan.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-font-style: italic; -fx-text-fill: #FED7AA;");

        Label lblSubDesc = new Label("🍗 Thơm cay đậm vị • Giòn tan từng thớ thịt • 100% Thịt gà tươi chuẩn công thức Độ Mixi");
        lblSubDesc.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 10.5px; -fx-font-weight: 600; -fx-text-fill: #A8A29E;");

        textCol.getChildren().addAll(badgeRow, lblTitle, lblSlogan, lblSubDesc);

        // 3. Cột Giá & Nút Đặt Nhanh
        VBox actionCol = new VBox(6);
        actionCol.setAlignment(Pos.CENTER_RIGHT);
        actionCol.setMinWidth(135);

        Label lblPriceTag = new Label("35.000 đ");
        lblPriceTag.setStyle("-fx-font-family: 'Segoe UI', 'Inter', sans-serif; -fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #F59E0B;");

        Label lblUnit = new Label("Hũ 150g nhâm nhi");
        lblUnit.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 10.5px; -fx-text-fill: #A8A29E;");

        Button btnOrder = new Button("🛒 Gọi Món Ngay");
        btnOrder.setStyle(
            "-fx-background-color: #112823; " +
            "-fx-text-fill: #FFFFFF; " +
            "-fx-font-family: 'Inter', sans-serif; " +
            "-fx-font-size: 13px; " +
            "-fx-font-weight: 800; " +
            "-fx-background-radius: 8px; " +
            "-fx-padding: 8 16 8 16; " +
            "-fx-cursor: hand;"
        );
        btnOrder.setOnMouseEntered(e -> btnOrder.setStyle(
            "-fx-background-color: #1A3B34; " +
            "-fx-text-fill: #FFFFFF; " +
            "-fx-font-family: 'Inter', sans-serif; " +
            "-fx-font-size: 13px; " +
            "-fx-font-weight: 800; " +
            "-fx-background-radius: 8px; " +
            "-fx-padding: 8 16 8 16; " +
            "-fx-cursor: hand;"
        ));
        btnOrder.setOnMouseExited(e -> btnOrder.setStyle(
            "-fx-background-color: #112823; " +
            "-fx-text-fill: #FFFFFF; " +
            "-fx-font-family: 'Inter', sans-serif; " +
            "-fx-font-size: 13px; " +
            "-fx-font-weight: 800; " +
            "-fx-background-radius: 8px; " +
            "-fx-padding: 8 16 8 16; " +
            "-fx-cursor: hand;"
        ));
        AnimationHelper.applyButtonPress(btnOrder);
        btnOrder.setOnAction(e -> {
            MenuItem item = cafe.models.MenuRepository.findById(68);
            if (item != null) {
                if (!item.isInStock()) {
                    showToast("Món '" + item.getName() + "' hiện đang tạm hết nguyên liệu. Quý khách vui lòng chọn món thơm ngon khác nhé!");
                    return;
                }
                if (onQuickAdd != null) {
                    onQuickAdd.accept(item);
                    showToast("🔥 Đã thêm Khô Gà Lá Chanh MixiFood vào giỏ hàng! Nhâm nhi cùng cà phê cực cuốn!");
                }
            }
        });

        actionCol.getChildren().addAll(lblPriceTag, lblUnit, btnOrder);

        banner.getChildren().addAll(imgWrapper, textCol, actionCol);
        return banner;
    }

    private void rebuildGridLayout(int cols) {
        itemsGridPane.getChildren().clear();
        itemsGridPane.getColumnConstraints().clear();

        for (int i = 0; i < cols; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / cols);
            cc.setHgrow(Priority.ALWAYS);
            itemsGridPane.getColumnConstraints().add(cc);
        }

        for (int i = 0; i < currentCards.size(); i++) {
            ItemCard card = currentCards.get(i);
            int col = i % cols;
            int row = i / cols;
            itemsGridPane.add(card, col, row);
        }
    }

    /**
     * Render sidebar categories with Multi-tier Icon loader (PNG -> SVG -> Emoji)
     * Container icon 26x26 cố định đảm bảo toàn bộ chữ thẳng tắp 100% 1 hàng từ trên xuống.
     */
    public void renderCategories(List<String> categories, String selectedCategory, Consumer<String> onSelect) {
        this.lastCategories = categories;
        this.lastSelectedCategory = selectedCategory;
        this.lastOnCategorySelect = onSelect;
        this.currentActiveCategory = (selectedCategory != null) ? selectedCategory : "Tất cả";
        if (mixiPromoBanner != null) {
            boolean showPromo = currentActiveCategory.equalsIgnoreCase("Tất cả") || currentActiveCategory.toLowerCase().contains("món ăn nhẹ");
            mixiPromoBanner.setVisible(showPromo);
            mixiPromoBanner.setManaged(showPromo);
        }
        categoryNavBox.getChildren().clear();

        List<String> cleanList = new ArrayList<>();
        cleanList.add("Tất cả");

        for (String cat : categories) {
            if (!cat.equalsIgnoreCase("Tất cả") && !cleanList.contains(cat)) {
                cleanList.add(cat);
            }
        }

        if (embeddedMode) {
            renderCategoryPills(cleanList, selectedCategory, onSelect);
            return;
        }

        for (String rawCat : cleanList) {
            boolean isActive = rawCat.equalsIgnoreCase(selectedCategory);

            Button btnCat = new Button();
            btnCat.setMaxWidth(Double.MAX_VALUE);
            btnCat.setAlignment(Pos.CENTER_LEFT);
            btnCat.setPadding(new Insets(10, 14, 10, 14));
            AnimationHelper.applyButtonPress(btnCat);

            HBox itemRow = new HBox(12);
            itemRow.setAlignment(Pos.CENTER_LEFT);

            // Container cố định kích thước 26x26 cho icon giúp cột chữ thẳng tắp 100%
            StackPane iconWrapper = new StackPane();
            iconWrapper.setPrefSize(26, 26);
            iconWrapper.setMinSize(26, 26);
            iconWrapper.setMaxSize(26, 26);
            iconWrapper.setAlignment(Pos.CENTER);

            String iconFileName = getCategoryIconFileName(rawCat);
            String svgPath = SvgIcons.getIconForCategory(rawCat);
            String emojiFallback = getCategoryEmoji(rawCat);

            Node iconNode = getIconNode(iconFileName, svgPath, emojiFallback, isActive);
            iconWrapper.getChildren().add(iconNode);

            // Nhãn chữ danh mục chuẩn Typography
            String displayLabel = getConciseCategoryLabel(rawCat);
            Label lblText = new Label(displayLabel);
            lblText.setStyle("-fx-font-size: 13.5px; -fx-font-weight: " + (isActive ? "800" : "700") + "; -fx-text-fill: " + (isActive ? "#FFFFFF" : "#3F3931") + ";");

            itemRow.getChildren().addAll(iconWrapper, lblText);
            btnCat.setGraphic(itemRow);

            btnCat.setFocusTraversable(false);

            if (isActive) {
                btnCat.setStyle(
                    "-fx-background-color: #112823; " +
                    "-fx-background-radius: 8px; " +
                    "-fx-border-color: transparent; " +
                    "-fx-focus-color: transparent; " +
                    "-fx-faint-focus-color: transparent; " +
                    "-fx-cursor: hand;"
                );
            } else {
                btnCat.setStyle(
                    "-fx-background-color: transparent; " +
                    "-fx-background-radius: 8px; " +
                    "-fx-border-color: transparent; " +
                    "-fx-focus-color: transparent; " +
                    "-fx-faint-focus-color: transparent; " +
                    "-fx-cursor: hand;"
                );
                btnCat.setOnMouseEntered(e -> btnCat.setStyle(
                    "-fx-background-color: #EFE8DD; " +
                    "-fx-background-radius: 8px; " +
                    "-fx-border-color: transparent; " +
                    "-fx-focus-color: transparent; " +
                    "-fx-faint-focus-color: transparent; " +
                    "-fx-cursor: hand;"
                ));
                btnCat.setOnMouseExited(e -> btnCat.setStyle(
                    "-fx-background-color: transparent; " +
                    "-fx-background-radius: 8px; " +
                    "-fx-border-color: transparent; " +
                    "-fx-focus-color: transparent; " +
                    "-fx-faint-focus-color: transparent; " +
                    "-fx-cursor: hand;"
                ));
            }

            btnCat.setOnAction(e -> onSelect.accept(rawCat));
            categoryNavBox.getChildren().add(btnCat);
        }
    }

    private String getCategoryIconFileName(String cat) {
        if (cat == null) return null;
        String lower = cat.toLowerCase();
        if (lower.contains("tất cả")) return "tat-ca.png";
        if (lower.contains("cà phê")) return "ca-phe.png";
        if (lower.contains("trà sữa")) return "tra-sua.png";
        if (lower.contains("trà")) return "tra.png";
        if (lower.contains("đá xay") || lower.contains("sinh tố")) return "da-xay.png";
        if (lower.contains("nước ép") || lower.contains("soda")) return "nuoc-ep.png";
        if (lower.contains("matcha") || lower.contains("chocolate")) return "da-xay.png";
        if (lower.contains("bánh ngọt") || lower.contains("bánh")) return "banh-ngot.png";
        if (lower.contains("ăn nhẹ") || lower.contains("ăn vặt") || lower.contains("món ăn")) return "mon-an-vat.png";
        if (lower.contains("topping")) return "topping.png";
        return null;
    }

    private String getCategoryEmoji(String cat) {
        if (cat == null) return "☕";
        String lower = cat.toLowerCase();
        if (lower.contains("tất cả")) return "☕";
        if (lower.contains("cà phê")) return "☕";
        if (lower.contains("trà sữa")) return "🧋";
        if (lower.contains("trà")) return "🍵";
        if (lower.contains("đá xay") || lower.contains("sinh tố")) return "🥤";
        if (lower.contains("nước ép") || lower.contains("soda")) return "🍹";
        if (lower.contains("matcha") || lower.contains("chocolate")) return "🍫";
        if (lower.contains("bánh")) return "🍰";
        if (lower.contains("ăn")) return "🥪";
        if (lower.contains("topping")) return "✨";
        return "☕";
    }

    private Node getIconNode(String iconFileName, String svgFallback, String fallbackEmoji, boolean isActive) {
        Color color = isActive ? Color.WHITE : Color.web("#3F3931");
        return cafe.utils.IconHelper.getIcon("categories", iconFileName, svgFallback, fallbackEmoji, 20, color);
    }

    private String getConciseCategoryLabel(String raw) {
        if (raw == null) return "";
        String lower = raw.trim().toLowerCase();
        if (lower.contains("tất cả")) return "Tất cả";
        if (lower.contains("cà phê")) return "Cà phê";
        if (lower.contains("trà & trái cây") || lower.contains("trà trái cây")) return "Trà";
        if (lower.contains("đá xay")) return "Đá xay";
        if (lower.contains("trà sữa")) return "Trà sữa";
        if (lower.contains("soda") || lower.contains("nước ép")) return "Nước ép";
        if (lower.contains("matcha")) return "Matcha";
        if (lower.contains("bánh ngọt")) return "Bánh ngọt";
        if (lower.contains("món ăn") || lower.contains("ăn nhẹ")) return "Món ăn nhẹ";
        return raw;
    }

    public void updateCartBadge(int totalQty) {
        lblCartBadge.setText(String.valueOf(totalQty));
        AnimationHelper.bumpBadge(lblCartBadge);
    }

    public void setTableNumber(int tableNumber) {
        lblTableBadge.setText("Bàn " + (tableNumber < 10 ? "0" + tableNumber : tableNumber));
    }

    public void setConnectionState(boolean connected) {
        if (connected) {
            lblConnectionStatus.setText("● Đã kết nối");
            lblConnectionStatus.setStyle("-fx-background-color: #DCE8D9; -fx-text-fill: #597256; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 2 6 2 6; -fx-font-size: 9.5px; -fx-font-weight: 600;");
        } else {
            lblConnectionStatus.setText("○ Mất kết nối");
            lblConnectionStatus.setStyle("-fx-background-color: #FDEEEE; -fx-text-fill: #C62828; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 2 6 2 6; -fx-font-size: 9.5px; -fx-font-weight: 600;");
        }
    }

    public void updateActiveOrderStatus(int orderId, String statusText) {
        activeOrderBox.setVisible(true);
        activeOrderBox.setManaged(true);
        lblActiveOrderId.setText("Đơn #" + orderId + " • " + lblTableBadge.getText());
        lblActiveOrderStatus.setText("Trạng thái: " + statusText);
    }

    public void highlightActiveOrderBox() {
        activeOrderBox.setVisible(true);
        activeOrderBox.setManaged(true);
        activeOrderBox.setStyle("-fx-background-color: #EDF7ED; -fx-border-color: #112823; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-border-width: 1.5px;");
        AnimationHelper.bumpBadge(activeOrderBox);
        PauseTransition revert = new PauseTransition(Duration.seconds(3));
        revert.setOnFinished(e -> activeOrderBox.setStyle("-fx-background-color: #FBF9F5; -fx-border-color: #DED7CD; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-border-width: 1px;"));
        revert.play();
    }

    public void updateItemStockStatus(int itemId, boolean inStock) {
        for (ItemCard card : currentCards) {
            if (card.getItem().getId() == itemId) {
                card.updateStockStatus(inStock);
            }
        }
    }

    public void showToast(String message) {
        lblToastMessage.setText(message);
        notificationToast.setVisible(true);
        notificationToast.setManaged(true);

        PauseTransition hideDelay = new PauseTransition(Duration.seconds(3));
        hideDelay.setOnFinished(e -> {
            notificationToast.setVisible(false);
            notificationToast.setManaged(false);
        });
        hideDelay.play();
    }

    private void startClock() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm • dd/MM/yyyy");
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> lblLiveClock.setText(sdf.format(new Date()))), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    // Getters for Controller bindings
    public TextField getSearchField() { return txtSearch; }
    public Button getCartButton() { return btnFloatingCart; }
    public Label getCartBadge() { return lblCartBadge; }
    public Button getFloorPlanButton() { return btnFloorPlan; }
    public Button getLogoutButton() { return btnLogout; }
    public Button getSidebarLogoutButton() { return btnSidebarLogout; }
    public void setOnLogout(Runnable callback) {
        this.onLogoutCallback = callback;
        if (callback != null) {
            btnLogout.setOnAction(e -> callback.run());
            btnSidebarLogout.setOnAction(e -> callback.run());
        }
    }
}
