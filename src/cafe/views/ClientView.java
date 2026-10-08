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
 * ClientView - Giao diện Kiosk Thực đơn (Screen 3 trong UI_mong_muon.png):
 * - Left Sidebar (110px): Logo, Nút Sơ đồ bàn, 7 danh mục có icon cố định 22px giúp cột chữ thẳng tắp 100%
 * - Top Bar: Bàn 05 (Playfair Display), Badge "Đang phục vụ" (#DCE8D9 / #597256), Thanh tìm kiếm 31px, Nút Giỏ hàng nổi
 * - Lưới thực đơn: 3 cột, card 6px radius, ảnh 5px radius, nút Quick-Add (+) 20px #112823 ở góc dưới ảnh
 */
public class ClientView extends BorderPane {

    private final VBox categoryNavBox = new VBox();
    private final GridPane itemsGridPane = new GridPane();
    private final ScrollPane catalogScrollPane = new ScrollPane();
    private final TextField txtSearch = new TextField();

    private final Button btnFloatingCart = new Button();
    private final HBox cartButtonContent = new HBox();
    private final Label lblCartBadge = new Label("0");

    private final Label lblTableBadge = new Label("Bàn 05");
    private final Label lblTableStatus = new Label("● Đang phục vụ");
    private final Label lblConnectionStatus = new Label("● Đã kết nối");
    private final Label lblLiveClock = new Label();

    private final Button btnFloorPlan = new Button();

    private final HBox notificationToast = new HBox();
    private final Label lblToastMessage = new Label();

    private final VBox activeOrderBox = new VBox(2);
    private final Label lblActiveOrderId = new Label();
    private final Label lblActiveOrderStatus = new Label();

    private final List<ItemCard> currentCards = new ArrayList<>();

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

    private VBox buildLeftCategorySidebar() {
        VBox sidebar = new VBox(10);
        sidebar.setPrefWidth(110);
        sidebar.setMinWidth(100);
        sidebar.setMaxWidth(120);
        sidebar.setPadding(new Insets(16, 6, 16, 8));
        sidebar.setStyle("-fx-background-color: #F7F2EA; -fx-border-color: transparent #DED7CD transparent transparent; -fx-border-width: 0 1 0 0;");

        // Brand Logo (Playfair Display)
        VBox brandBox = new VBox(1);
        brandBox.setAlignment(Pos.CENTER);
        brandBox.setPadding(new Insets(0, 0, 8, 0));

        Label lblBrand = new Label("Brew & Bean");
        lblBrand.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        Label lblSub = new Label("C A F E");
        lblSub.setStyle("-fx-font-size: 8px; -fx-font-weight: 700; -fx-text-fill: #563521; -fx-letter-spacing: 2px;");
        brandBox.getChildren().addAll(lblBrand, lblSub);

        // Nút quay lại Sơ đồ bàn
        btnFloorPlan.setMaxWidth(Double.MAX_VALUE);
        btnFloorPlan.setAlignment(Pos.CENTER_LEFT);
        btnFloorPlan.setPadding(new Insets(6, 8, 6, 8));
        HBox floorPlanBox = new HBox(6);
        floorPlanBox.setAlignment(Pos.CENTER_LEFT);
        SVGPath tableIcon = SvgIcons.createOutlineIcon(SvgIcons.TABLE, 0.75, Color.web("#6F695F"));
        Label lblFloorPlan = new Label("Sơ đồ bàn");
        lblFloorPlan.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #563521;");
        floorPlanBox.getChildren().addAll(tableIcon, lblFloorPlan);
        btnFloorPlan.setGraphic(floorPlanBox);
        btnFloorPlan.setStyle("-fx-background-color: #F1EADF; -fx-background-radius: 5px; -fx-border-color: #DED7CD; -fx-border-radius: 5px; -fx-cursor: hand;");
        AnimationHelper.applyButtonPress(btnFloorPlan);

        // Categories list
        categoryNavBox.setSpacing(4);

        ScrollPane scrollNav = new ScrollPane(categoryNavBox);
        scrollNav.setFitToWidth(true);
        scrollNav.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollNav.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollNav.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollNav, Priority.ALWAYS);

        // Live Order Tracking Card at bottom of sidebar
        activeOrderBox.setPadding(new Insets(8));
        activeOrderBox.setStyle("-fx-background-color: #FBF9F5; -fx-border-color: #DED7CD; -fx-border-radius: 5px; -fx-background-radius: 5px; -fx-border-width: 1px;");
        activeOrderBox.setVisible(false);
        activeOrderBox.setManaged(false);

        lblActiveOrderId.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        lblActiveOrderStatus.setStyle("-fx-font-size: 9.5px; -fx-font-weight: 600; -fx-text-fill: #E5A04D;");
        activeOrderBox.getChildren().addAll(lblActiveOrderId, lblActiveOrderStatus);

        sidebar.getChildren().addAll(brandBox, btnFloorPlan, scrollNav, activeOrderBox);
        return sidebar;
    }

    private VBox buildCenterCatalogArea() {
        VBox center = new VBox(10);
        center.setPadding(new Insets(14, 20, 14, 20));
        HBox.setHgrow(center, Priority.ALWAYS);

        // Top Utility Bar: Table info, status pill, search bar, and Floating Cart Button
        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(1);
        lblTableBadge.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        lblTableStatus.setStyle("-fx-background-color: #DCE8D9; -fx-text-fill: #597256; -fx-font-size: 9.5px; -fx-font-weight: 600; -fx-background-radius: 10px; -fx-padding: 2 6 2 6;");
        titleBox.getChildren().addAll(lblTableBadge, lblTableStatus);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Search Input Bar (height 31px, radius 5px, #FBF9F5)
        txtSearch.setPromptText("Tìm kiếm món ...");
        txtSearch.setPrefHeight(31);
        txtSearch.setPrefWidth(220);
        txtSearch.setStyle("-fx-background-color: #FBF9F5; -fx-border-color: #DED7CD; -fx-border-radius: 5px; -fx-background-radius: 5px; -fx-font-size: 11.5px; -fx-padding: 4 10 4 10;");

        // Floating Cart Button with Shopping Bag Icon + Badge
        btnFloatingCart.setStyle("-fx-background-color: #FBF9F5; -fx-background-radius: 5px; -fx-border-color: #DED7CD; -fx-border-radius: 5px; -fx-border-width: 1px; -fx-padding: 5 10 5 8; -fx-cursor: hand;");
        AnimationHelper.applyButtonPress(btnFloatingCart);

        cartButtonContent.setAlignment(Pos.CENTER);
        cartButtonContent.setSpacing(6);
        SVGPath cartIcon = SvgIcons.createOutlineIcon(SvgIcons.SHOPPING_BAG, 0.8, Color.web("#112823"));
        Label lblCartText = new Label("Giỏ hàng");
        lblCartText.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 600; -fx-text-fill: #112823;");

        lblCartBadge.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-size: 9.5px; -fx-font-weight: 700; -fx-background-radius: 10px; -fx-padding: 1 5 1 5;");
        cartButtonContent.getChildren().addAll(cartIcon, lblCartText, lblCartBadge);
        btnFloatingCart.setGraphic(cartButtonContent);

        topBar.getChildren().addAll(titleBox, spacer, txtSearch, btnFloatingCart);

        // Catalog Grid inside ScrollPane
        itemsGridPane.setHgap(7);
        itemsGridPane.setVgap(7);
        itemsGridPane.setPadding(new Insets(2, 2, 14, 2));

        catalogScrollPane.setContent(itemsGridPane);
        catalogScrollPane.setFitToWidth(true);
        catalogScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        catalogScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        catalogScrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(catalogScrollPane, Priority.ALWAYS);

        center.getChildren().addAll(topBar, catalogScrollPane);
        return center;
    }

    public void renderMenuItems(List<MenuItem> items, Consumer<MenuItem> onSelect, Consumer<MenuItem> onQuickAdd) {
        itemsGridPane.getChildren().clear();
        currentCards.clear();

        for (MenuItem item : items) {
            ItemCard card = new ItemCard(item, onSelect, onQuickAdd);
            currentCards.add(card);
        }

        rebuildGridLayout(3);
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
     * Render sidebar categories with Vector SVG icons and high-contrast Active states.
     * Container icon 22px cố định đảm bảo toàn bộ chữ thẳng tắp 1 hàng từ trên xuống.
     */
    public void renderCategories(List<String> categories, String selectedCategory, Consumer<String> onSelect) {
        categoryNavBox.getChildren().clear();

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
            btnCat.setPadding(new Insets(6, 6, 6, 6));
            AnimationHelper.applyButtonPress(btnCat);

            HBox itemRow = new HBox(6);
            itemRow.setAlignment(Pos.CENTER_LEFT);

            // Container cố định kích thước 22px cho icon giúp cột chữ thẳng tắp
            StackPane iconWrapper = new StackPane();
            iconWrapper.setPrefSize(22, 22);
            iconWrapper.setMinSize(22, 22);
            iconWrapper.setMaxSize(22, 22);
            iconWrapper.setAlignment(Pos.CENTER);

            String svgPath = SvgIcons.getIconForCategory(rawCat);
            SVGPath icon = SvgIcons.createOutlineIcon(svgPath, 0.78, isActive ? Color.WHITE : Color.web("#6F695F"));
            iconWrapper.getChildren().add(icon);

            // Nhãn chữ danh mục cô đọng
            String displayLabel = getConciseCategoryLabel(rawCat);
            Label lblText = new Label(displayLabel);
            lblText.setStyle("-fx-font-size: 11px; -fx-font-weight: " + (isActive ? "600" : "500") + "; -fx-text-fill: " + (isActive ? "#FFFFFF" : "#292722") + ";");

            itemRow.getChildren().addAll(iconWrapper, lblText);
            btnCat.setGraphic(itemRow);

            if (isActive) {
                btnCat.setStyle(
                    "-fx-background-color: #112823; " +
                    "-fx-background-radius: 5px; " +
                    "-fx-border-color: #112823; " +
                    "-fx-cursor: hand;"
                );
            } else {
                btnCat.setStyle(
                    "-fx-background-color: transparent; " +
                    "-fx-background-radius: 5px; " +
                    "-fx-cursor: hand;"
                );
                btnCat.setOnMouseEntered(e -> btnCat.setStyle("-fx-background-color: #EFE8DD; -fx-background-radius: 5px; -fx-cursor: hand;"));
                btnCat.setOnMouseExited(e -> btnCat.setStyle("-fx-background-color: transparent; -fx-background-radius: 5px; -fx-cursor: hand;"));
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
