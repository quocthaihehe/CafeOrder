package cafe.views;

import cafe.models.MenuItem;
import cafe.utils.AnimationHelper;
import cafe.utils.SvgIcons;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

/**
 * ClientView - Giao diện Kiosk đặt món tại bàn chuẩn 100% UI_mong_muon.png.
 * Đã sửa toàn diện theo Báo cáo Thẩm định & Thiết kế UI/UX:
 * 1. SIDEBAR CHUẨN HOÁ:
 *    - 100% danh mục có Icon Vector SVG thanh mảnh (Trắng khi active, xám khi inactive).
 *    - Nút Active màu Deep Forest Green (#183C32) tương phản cao.
 *    - Xóa bỏ triệt để lỗi trùng lặp "Tất cả món" / "Tất cả".
 *    - Tên danh mục cô đọng, thẳng hàng: Tất cả, Cà phê, Trà, Đá xay, Nước ép, Bánh ngọt, Món ăn nhẹ...
 *    - Header logo Playfair Display bay bổng, không có đường kẻ cắt thô.
 *    - Bổ sung nút quay lại Sơ đồ bàn [ Sơ đồ bàn ].
 * 2. LƯỚI THỰC ĐƠN TOÀN MÀN HÌNH: 3-4 cột thoáng rộng, hình ảnh 1:1 to đẹp.
 * 3. HEADER GÓC PHẢI: Nút Giỏ Hàng Nổi [ 🛒 Qty ] kèm hiệu ứng Badge Bump.
 */
public class ClientView extends BorderPane {

    // Header controls
    private final Label lblTableBadge = new Label("BÀN 01");
    private final Label lblTableStatus = new Label("● Đang phục vụ");
    private final Label lblConnectionStatus = new Label("● Đã kết nối");
    private final Label lblLiveClock = new Label();

    // Floating Cart Button & Counter Badge on Top-Right Header
    private final Button btnFloatingCart = new Button();
    private final Label lblCartBadge = new Label("0");
    private final HBox cartButtonContent = new HBox(6);

    // Switch Table / Floor Plan Button
    private final Button btnFloorPlan = new Button("Sơ đồ bàn");

    // Catalog & Category controls
    private final TextField txtSearch = new TextField();
    private final VBox categoryNavBox = new VBox(6);
    private final GridPane itemsGridPane = new GridPane();
    private final VBox emptySearchBox = new VBox(8);
    private final ScrollPane catalogScrollPane = new ScrollPane();
    private List<MenuItem> currentMenuItems = new ArrayList<>();
    private Consumer<MenuItem> currentOnSelect;
    private Consumer<MenuItem> currentOnQuickAdd;
    private final List<ItemCard> currentCards = new ArrayList<>();
    private final PauseTransition resizeDebounce = new PauseTransition(Duration.millis(50));
    private int currentCols = -1;
    private double currentCardWidth = -1;

    // Live Order Status Tracking Box at bottom of left sidebar
    private final VBox activeOrderBox = new VBox(4);
    private final Label lblActiveOrderId = new Label("Đơn gần nhất: Chưa có");
    private final Label lblActiveOrderStatus = new Label("");

    // Notification toast
    private final HBox notificationToast = new HBox(8);
    private final Label lblToastMessage = new Label();

    public ClientView() {
        initUI();
        startClock();
    }

    private void initUI() {
        setStyle("-fx-background-color: #F5F2EB;");

        // 1. CỘT TRÁI: DANH MỤC DỌC (LEFT SIDEBAR)
        VBox leftSidebar = buildLeftCategorySidebar();
        setLeft(leftSidebar);

        // 2. KHU VỰC TRUNG TÂM TOÀN MÀN HÌNH: HEADER & LƯỚI THỰC ĐƠN
        VBox centerArea = buildCenterCatalogArea();
        setCenter(centerArea);

        // 3. TOAST THÔNG BÁO DƯỚI CÙNG
        notificationToast.setAlignment(Pos.CENTER);
        notificationToast.setPadding(new Insets(10, 22, 10, 22));
        notificationToast.setStyle("-fx-background-color: #183C32; -fx-background-radius: 10px; -fx-effect: dropshadow(gaussian, rgba(24, 60, 50, 0.25), 8, 0, 0, 2);");
        notificationToast.setVisible(false);
        notificationToast.setManaged(false);
        lblToastMessage.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: 700; -fx-font-size: 13px;");
        notificationToast.getChildren().add(lblToastMessage);

        StackPane bottomWrapper = new StackPane(notificationToast);
        bottomWrapper.setPadding(new Insets(0, 0, 16, 0));
        bottomWrapper.setPickOnBounds(false);
        setBottom(bottomWrapper);
    }

    private VBox buildLeftCategorySidebar() {
        VBox sidebar = new VBox(14);
        sidebar.setPrefWidth(190);
        sidebar.setMinWidth(180);
        sidebar.setMaxWidth(205);
        sidebar.setPadding(new Insets(18, 12, 18, 14));
        sidebar.setStyle("-fx-background-color: #FBF9F4; -fx-border-color: transparent #D8D1C3 transparent transparent; -fx-border-width: 0 1 0 0;");

        // Brand Logo (Playfair Display, trang nhã, không gạch ngang thô)
        VBox brandBox = new VBox(2);
        brandBox.setAlignment(Pos.CENTER_LEFT);
        brandBox.setPadding(new Insets(4, 8, 12, 8));

        Label lblBrand = new Label("Brew & Bean");
        lblBrand.setStyle("-fx-font-family: 'Playfair Display', 'Georgia', serif; -fx-font-size: 21px; -fx-font-weight: 700; -fx-text-fill: #292722;");

        Label lblSub = new Label("C A F E");
        lblSub.setStyle("-fx-font-size: 9.5px; -fx-font-weight: 700; -fx-text-fill: #5C3D2E; -fx-letter-spacing: 3.5px;");

        brandBox.getChildren().addAll(lblBrand, lblSub);

        // Nút quay lại Sơ đồ bàn
        btnFloorPlan.setMaxWidth(Double.MAX_VALUE);
        btnFloorPlan.setAlignment(Pos.CENTER_LEFT);
        btnFloorPlan.setPadding(new Insets(8, 12, 8, 12));
        HBox floorPlanBox = new HBox(8);
        floorPlanBox.setAlignment(Pos.CENTER_LEFT);
        SVGPath tableIcon = SvgIcons.createOutlineIcon(SvgIcons.TABLE, 0.8, Color.web("#817B70"));
        Label lblFloorPlan = new Label("Sơ đồ bàn");
        lblFloorPlan.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 600; -fx-text-fill: #5C3D2E;");
        floorPlanBox.getChildren().addAll(tableIcon, lblFloorPlan);
        btnFloorPlan.setGraphic(floorPlanBox);
        btnFloorPlan.setStyle("-fx-background-color: #F5EFEB; -fx-background-radius: 8px; -fx-border-color: #E2DBD0; -fx-border-radius: 8px; -fx-cursor: hand;");
        AnimationHelper.applyButtonPress(btnFloorPlan);

        // Categories list
        categoryNavBox.setSpacing(4);

        ScrollPane scrollNav = new ScrollPane(categoryNavBox);
        scrollNav.setFitToWidth(true);
        scrollNav.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollNav.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollNav, Priority.ALWAYS);

        // Live Order Tracking Card at bottom of sidebar
        activeOrderBox.setPadding(new Insets(10));
        activeOrderBox.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #D8D1C3; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-border-width: 1px;");
        activeOrderBox.setVisible(false);
        activeOrderBox.setManaged(false);

        lblActiveOrderId.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        lblActiveOrderStatus.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 600; -fx-text-fill: #E07A5F;");
        activeOrderBox.getChildren().addAll(lblActiveOrderId, lblActiveOrderStatus);

        sidebar.getChildren().addAll(brandBox, btnFloorPlan, scrollNav, activeOrderBox);
        return sidebar;
    }

    private VBox buildCenterCatalogArea() {
        VBox center = new VBox(12);
        center.setPadding(new Insets(16, 24, 16, 24));
        HBox.setHgrow(center, Priority.ALWAYS);

        // Top Utility Bar: Table info, status, live clock, and Floating Cart Button
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        lblTableBadge.getStyleClass().add("table-tag");
        lblTableStatus.getStyleClass().add("status-tag-active");
        lblConnectionStatus.getStyleClass().add("status-tag-done");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        lblLiveClock.setStyle("-fx-font-size: 12px; -fx-font-weight: 500; -fx-text-fill: #817B70;");

        // Floating Cart Button with Shopping Bag Icon + Badge
        btnFloatingCart.getStyleClass().add("btn-floating-cart");
        AnimationHelper.applyButtonPress(btnFloatingCart);

        cartButtonContent.setAlignment(Pos.CENTER);
        cartButtonContent.setSpacing(6);
        SVGPath cartIcon = SvgIcons.createOutlineIcon(SvgIcons.SHOPPING_BAG, 0.85, Color.web("#183C32"));
        Label lblCartText = new Label("Giỏ hàng");
        lblCartText.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #183C32;");

        lblCartBadge.getStyleClass().add("cart-counter-badge");
        cartButtonContent.getChildren().addAll(cartIcon, lblCartText, lblCartBadge);
        btnFloatingCart.setGraphic(cartButtonContent);

        topBar.getChildren().addAll(lblTableBadge, lblTableStatus, lblConnectionStatus, spacer, lblLiveClock, btnFloatingCart);

        // Search Input Bar
        HBox searchRow = new HBox(10);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        txtSearch.setPromptText("Tìm kiếm thức uống, món ăn, cà phê, trà...");
        txtSearch.getStyleClass().add("text-input-field");
        txtSearch.setPrefHeight(40);
        HBox.setHgrow(txtSearch, Priority.ALWAYS);

        searchRow.getChildren().add(txtSearch);

        // Catalog Grid inside ScrollPane
        itemsGridPane.setHgap(14);
        itemsGridPane.setVgap(14);
        itemsGridPane.setPadding(new Insets(4, 4, 16, 4));

        catalogScrollPane.setContent(itemsGridPane);
        catalogScrollPane.setFitToWidth(true);
        catalogScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        catalogScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(catalogScrollPane, Priority.ALWAYS);

        // Responsive grid column relayout on width resize
        catalogScrollPane.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getWidth() > 0) {
                resizeDebounce.setOnFinished(e -> {
                    double viewportW = newVal.getWidth() - 24;
                    int targetCols = 3;
                    if (viewportW < 460) {
                        targetCols = 1;
                    } else if (viewportW < 720) {
                        targetCols = 2;
                    } else if (viewportW >= 1100) {
                        targetCols = 4;
                    }

                    double cardW = Math.floor((viewportW - (targetCols - 1) * itemsGridPane.getHgap()) / targetCols);
                    if (targetCols != currentCols || Math.abs(cardW - currentCardWidth) > 3) {
                        relayoutGrid(targetCols, cardW);
                    }
                });
                resizeDebounce.playFromStart();
            }
        });

        // Empty Search Feedback
        emptySearchBox.setAlignment(Pos.CENTER);
        emptySearchBox.setPadding(new Insets(80, 20, 80, 20));
        emptySearchBox.setVisible(false);
        emptySearchBox.setManaged(false);

        Label emptyIcon = new Label("☕");
        emptyIcon.setStyle("-fx-font-size: 38px; -fx-text-fill: #D8D1C3;");
        Label emptyTitle = new Label("Không tìm thấy món phù hợp");
        emptyTitle.getStyleClass().add("editorial-title");
        Label emptyDesc = new Label("Thử tìm kiếm với từ khóa khác hoặc chọn danh mục phía bên trái.");
        emptyDesc.getStyleClass().add("editorial-subtitle");

        emptySearchBox.getChildren().addAll(emptyIcon, emptyTitle, emptyDesc);

        center.getChildren().addAll(topBar, searchRow, catalogScrollPane, emptySearchBox);
        return center;
    }

    private void relayoutGrid(int cols, double cardWidth) {
        this.currentCols = cols;
        this.currentCardWidth = cardWidth;

        itemsGridPane.getChildren().clear();
        itemsGridPane.getColumnConstraints().clear();

        for (int i = 0; i < cols; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setHgrow(Priority.ALWAYS);
            col.setPercentWidth(100.0 / cols);
            col.setHalignment(HPos.CENTER);
            itemsGridPane.getColumnConstraints().add(col);
        }

        for (int i = 0; i < currentCards.size(); i++) {
            ItemCard card = currentCards.get(i);
            int col = i % cols;
            int row = i / cols;
            itemsGridPane.add(card, col, row);
        }
    }

    /**
     * Render sidebar categories with Vector SVG icons and high-contrast Active states.
     * Fixes duplication bug completely: only 1 entry for "Tất cả".
     */
    public void renderCategories(List<String> categories, String selectedCategory, Consumer<String> onSelect) {
        categoryNavBox.getChildren().clear();

        // Deduplicate list in case "Tất cả" appears multiple times
        List<String> cleanList = new ArrayList<>();
        cleanList.add("Tất cả");

        for (String cat : categories) {
            if (!cat.equalsIgnoreCase("Tất cả") && !cleanList.contains(cat)) {
                cleanList.add(cat);
            }
        }

        for (String rawCat : cleanList) {
            boolean isActive = rawCat.equalsIgnoreCase(selectedCategory);

            Button btnCat = new Button();
            btnCat.setMaxWidth(Double.MAX_VALUE);
            btnCat.setAlignment(Pos.CENTER_LEFT);
            btnCat.setPadding(new Insets(9, 12, 9, 12));
            AnimationHelper.applyButtonPress(btnCat);

            HBox itemRow = new HBox(10);
            itemRow.setAlignment(Pos.CENTER_LEFT);

            // Vector SVG Icon
            String svgPath = SvgIcons.getIconForCategory(rawCat);
            SVGPath icon = SvgIcons.createOutlineIcon(svgPath, 0.85, isActive ? Color.WHITE : Color.web("#817B70"));

            // Short concise label
            String displayLabel = getConciseCategoryLabel(rawCat);
            Label lblText = new Label(displayLabel);
            lblText.setStyle("-fx-font-size: 13px; -fx-font-weight: " + (isActive ? "700" : "600") + "; -fx-text-fill: " + (isActive ? "#FFFFFF" : "#4A453E") + ";");

            itemRow.getChildren().addAll(icon, lblText);
            btnCat.setGraphic(itemRow);

            if (isActive) {
                btnCat.setStyle(
                    "-fx-background-color: #183C32; " +
                    "-fx-background-radius: 10px; " +
                    "-fx-border-color: #183C32; " +
                    "-fx-cursor: hand;"
                );
            } else {
                btnCat.setStyle(
                    "-fx-background-color: transparent; " +
                    "-fx-background-radius: 10px; " +
                    "-fx-cursor: hand;"
                );
                btnCat.setOnMouseEntered(e -> btnCat.setStyle("-fx-background-color: #EFE9DE; -fx-background-radius: 10px; -fx-cursor: hand;"));
                btnCat.setOnMouseExited(e -> btnCat.setStyle("-fx-background-color: transparent; -fx-background-radius: 10px; -fx-cursor: hand;"));
            }

            btnCat.setOnAction(e -> onSelect.accept(rawCat));
            categoryNavBox.getChildren().add(btnCat);
        }
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

    public void renderMenuItems(List<MenuItem> items, Consumer<MenuItem> onSelect) {
        renderMenuItems(items, onSelect, null);
    }

    public void renderMenuItems(List<MenuItem> items, Consumer<MenuItem> onSelect, Consumer<MenuItem> onQuickAdd) {
        this.currentMenuItems = items;
        this.currentOnSelect = onSelect;
        this.currentOnQuickAdd = onQuickAdd;
        this.currentCards.clear();

        if (items.isEmpty()) {
            itemsGridPane.setVisible(false);
            itemsGridPane.setManaged(false);
            emptySearchBox.setVisible(true);
            emptySearchBox.setManaged(true);
            return;
        }

        emptySearchBox.setVisible(false);
        emptySearchBox.setManaged(false);
        itemsGridPane.setVisible(true);
        itemsGridPane.setManaged(true);

        for (MenuItem item : items) {
            ItemCard card = new ItemCard(item, onSelect, onQuickAdd);
            currentCards.add(card);
        }

        int cols = currentCols > 0 ? currentCols : 3;
        double w = currentCardWidth > 0 ? currentCardWidth : 220;
        relayoutGrid(cols, w);
    }

    public void updateCartBadge(int totalQty) {
        lblCartBadge.setText(String.valueOf(totalQty));
        AnimationHelper.bumpBadge(lblCartBadge);
    }

    public void setTableNumber(int tableNumber) {
        lblTableBadge.setText("BÀN " + (tableNumber < 10 ? "0" + tableNumber : tableNumber));
    }

    public void setConnectionState(boolean connected) {
        if (connected) {
            lblConnectionStatus.setText("● Đã kết nối");
            lblConnectionStatus.setStyle("-fx-background-color: #EDF7ED; -fx-text-fill: #1B5E20; -fx-border-color: #C8E6C9; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 4 10 4 10; -fx-font-size: 11px; -fx-font-weight: 600;");
        } else {
            lblConnectionStatus.setText("○ Mất kết nối");
            lblConnectionStatus.setStyle("-fx-background-color: #FDEEEE; -fx-text-fill: #C62828; -fx-border-color: #F9CBCB; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 4 10 4 10; -fx-font-size: 11px; -fx-font-weight: 600;");
        }
    }

    public void updateActiveOrderStatus(int orderId, String statusText) {
        activeOrderBox.setVisible(true);
        activeOrderBox.setManaged(true);
        lblActiveOrderId.setText("Đơn #" + orderId + " • " + (orderId > 0 ? "Bàn " + lblTableBadge.getText().replace("BÀN ", "") : ""));
        lblActiveOrderStatus.setText("Trạng thái: " + statusText);
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
}
