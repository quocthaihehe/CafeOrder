package cafe.views;

import cafe.models.MenuItem;
import cafe.models.OrderItem;
import cafe.utils.CurrencyFormatter;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ClientView extends BorderPane {
    // Header controls
    private final Label lblTableBadge = new Label("BÀN 01");
    private final Label lblConnectionStatus = new Label("● Đã kết nối");
    private final Label lblLiveClock = new Label();

    // Catalog controls
    private final TextField txtSearch = new TextField();
    private final HBox categoryTabsBox = new HBox(8);
    private final FlowPane itemsFlowPane = new FlowPane(16, 16);
    private final ScrollPane catalogScrollPane = new ScrollPane();

    // Cart controls
    private final Label lblCartCount = new Label("0 món");
    private final VBox cartItemsContainer = new VBox(10);
    private final ScrollPane cartScrollPane = new ScrollPane();
    private final VBox emptyCartBox = new VBox(8);
    private final Label lblSubtotalValue = new Label("0 đ");
    private final Label lblGrandTotalValue = new Label("0 đ");
    private final Button btnSubmitOrder = new Button("☕ GỬI BẾP ORDER");

    // Live Order Status Tracking Box
    private final VBox activeOrderBox = new VBox(6);
    private final Label lblActiveOrderId = new Label("Đơn gần nhất: Chưa có");
    private final Label lblActiveOrderStatus = new Label("");

    // Notification toast
    private final HBox notificationToast = new HBox(10);
    private final Label lblToastMessage = new Label();

    public ClientView() {
        initUI();
        startClock();
    }

    private void initUI() {
        getStyleClass().add("root");

        // 1. TOP HEADER BAR
        HBox headerBar = buildHeader();
        setTop(headerBar);

        // 2. CENTER - CATALOG
        VBox catalogSection = buildCatalogSection();
        setCenter(catalogSection);

        // 3. RIGHT - CART & RECEIPT SIDEBAR
        VBox cartSidebar = buildCartSidebar();
        setRight(cartSidebar);

        // 4. BOTTOM NOTIFICATION (Hidden by default)
        notificationToast.setAlignment(Pos.CENTER);
        notificationToast.setPadding(new Insets(10, 20, 10, 20));
        notificationToast.setStyle("-fx-background-color: #3E2723; -fx-background-radius: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 10, 0, 0, 4);");
        notificationToast.setVisible(false);
        notificationToast.setManaged(false);
        lblToastMessage.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: 700; -fx-font-size: 13.5px;");
        notificationToast.getChildren().add(lblToastMessage);

        StackPane bottomWrapper = new StackPane(notificationToast);
        bottomWrapper.setPadding(new Insets(0, 0, 16, 0));
        bottomWrapper.setPickOnBounds(false);
        setBottom(bottomWrapper);
    }

    private HBox buildHeader() {
        HBox header = new HBox(16);
        header.getStyleClass().add("header-bar");
        header.setAlignment(Pos.CENTER_LEFT);

        // Brand Badge
        HBox brand = new HBox(10);
        brand.setAlignment(Pos.CENTER_LEFT);
        
        StackPane brandLogo = new StackPane();
        brandLogo.getStyleClass().add("brand-badge");
        Label logoIcon = new Label("☕");
        logoIcon.getStyleClass().add("brand-icon");
        brandLogo.getChildren().add(logoIcon);

        VBox brandText = new VBox(1);
        Label lblBrandName = new Label("L'Amour Artisan Cafe");
        lblBrandName.getStyleClass().add("brand-title");
        Label lblSlogan = new Label("Fine Specialty Coffee & Tea Lounge");
        lblSlogan.getStyleClass().add("brand-subtitle");
        brandText.getChildren().addAll(lblBrandName, lblSlogan);

        brand.getChildren().addAll(brandLogo, brandText);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Table Pill
        StackPane tablePill = new StackPane();
        tablePill.getStyleClass().add("table-pill");
        lblTableBadge.getStyleClass().add("table-pill-text");
        tablePill.getChildren().add(lblTableBadge);

        // Status Pill
        StackPane statusPill = new StackPane();
        statusPill.getStyleClass().add("status-pill-online");
        lblConnectionStatus.getStyleClass().add("status-pill-text");
        statusPill.getChildren().add(lblConnectionStatus);

        // Clock
        lblLiveClock.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #7D6E68;");

        header.getChildren().addAll(brand, spacer, tablePill, statusPill, lblLiveClock);
        return header;
    }

    private VBox buildCatalogSection() {
        VBox section = new VBox(12);
        section.setPadding(new Insets(16, 20, 16, 24));

        // Search & Category Tabs
        VBox filterBar = new VBox(12);

        HBox searchBox = new HBox(8);
        searchBox.getStyleClass().add("search-box");
        searchBox.setAlignment(Pos.CENTER_LEFT);
        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-font-size: 14px; -fx-opacity: 0.6;");
        txtSearch.getStyleClass().add("search-field");
        txtSearch.setPromptText("Tìm kiếm món yêu thích (Cà phê, Trà đào, Matcha...)...");
        HBox.setHgrow(txtSearch, Priority.ALWAYS);
        searchBox.getChildren().addAll(searchIcon, txtSearch);

        categoryTabsBox.setAlignment(Pos.CENTER_LEFT);

        filterBar.getChildren().addAll(searchBox, categoryTabsBox);

        // FlowPane for Menu Item Cards
        itemsFlowPane.setPadding(new Insets(8, 4, 20, 4));
        itemsFlowPane.setAlignment(Pos.TOP_LEFT);

        catalogScrollPane.setContent(itemsFlowPane);
        catalogScrollPane.getStyleClass().add("catalog-scroll-pane");
        catalogScrollPane.setFitToWidth(true);
        VBox.setVgrow(catalogScrollPane, Priority.ALWAYS);

        section.getChildren().addAll(filterBar, catalogScrollPane);
        return section;
    }

    private VBox buildCartSidebar() {
        VBox sidebar = new VBox(14);
        sidebar.getStyleClass().add("cart-sidebar");
        sidebar.setPrefWidth(350);
        sidebar.setMinWidth(320);
        sidebar.setMaxWidth(380);

        // Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label lblCartTitle = new Label("Giỏ hàng của bạn");
        lblCartTitle.getStyleClass().add("cart-header-title");
        HBox.setHgrow(lblCartTitle, Priority.ALWAYS);

        lblCartCount.getStyleClass().add("cart-badge-count");
        header.getChildren().addAll(lblCartTitle, lblCartCount);

        // Items Container with ScrollPane
        cartItemsContainer.setPadding(new Insets(4, 2, 4, 2));

        // Empty state
        emptyCartBox.getStyleClass().add("cart-empty-box");
        Label emptyIcon = new Label("☕");
        emptyIcon.getStyleClass().add("cart-empty-icon");
        Label emptyText = new Label("Chưa có món nào được chọn");
        emptyText.getStyleClass().add("cart-empty-text");
        Label emptySub = new Label("Hãy chạm vào thẻ món để thêm vào giỏ nhé!");
        emptySub.setStyle("-fx-font-size: 11px; -fx-text-fill: #B59F95;");
        emptyCartBox.getChildren().addAll(emptyIcon, emptyText, emptySub);

        StackPane cartContentHolder = new StackPane(emptyCartBox, cartItemsContainer);
        cartScrollPane.setContent(cartContentHolder);
        cartScrollPane.getStyleClass().add("catalog-scroll-pane");
        cartScrollPane.setFitToWidth(true);
        VBox.setVgrow(cartScrollPane, Priority.ALWAYS);

        // Active Order Tracker Box
        activeOrderBox.setStyle("-fx-background-color: #F8EDE5; -fx-background-radius: 12; -fx-padding: 10 12 10 12; -fx-border-color: #E8D5C8; -fx-border-radius: 12;");
        lblActiveOrderId.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #2C1810;");
        lblActiveOrderStatus.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #C97A44;");
        activeOrderBox.getChildren().addAll(lblActiveOrderId, lblActiveOrderStatus);
        activeOrderBox.setVisible(false);
        activeOrderBox.setManaged(false);

        // Bill Summary
        VBox summaryBox = new VBox(8);
        summaryBox.getStyleClass().add("bill-summary-box");

        HBox row1 = new HBox();
        Label lblSubLabel = new Label("Tạm tính:");
        lblSubLabel.getStyleClass().add("bill-label");
        Region sp1 = new Region();
        HBox.setHgrow(sp1, Priority.ALWAYS);
        lblSubtotalValue.getStyleClass().add("bill-value");
        row1.getChildren().addAll(lblSubLabel, sp1, lblSubtotalValue);

        HBox row2 = new HBox();
        Label lblService = new Label("Phí phục vụ:");
        lblService.getStyleClass().add("bill-label");
        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);
        Label lblServiceVal = new Label("Miễn phí");
        lblServiceVal.setStyle("-fx-font-size: 12px; -fx-text-fill: #4A7C59; -fx-font-weight: 600;");
        row2.getChildren().addAll(lblService, sp2, lblServiceVal);

        Separator sep = new Separator();

        HBox rowTotal = new HBox();
        Label lblTotalLabel = new Label("Tổng thanh toán:");
        lblTotalLabel.getStyleClass().add("bill-total-label");
        Region sp3 = new Region();
        HBox.setHgrow(sp3, Priority.ALWAYS);
        lblGrandTotalValue.getStyleClass().add("bill-total-value");
        rowTotal.getChildren().addAll(lblTotalLabel, sp3, lblGrandTotalValue);

        summaryBox.getChildren().addAll(row1, row2, sep, rowTotal);

        // Submit Button
        btnSubmitOrder.getStyleClass().add("btn-checkout");
        btnSubmitOrder.setMaxWidth(Double.MAX_VALUE);
        btnSubmitOrder.setDisable(true);

        sidebar.getChildren().addAll(header, cartScrollPane, activeOrderBox, summaryBox, btnSubmitOrder);
        return sidebar;
    }

    private void startClock() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd/MM • HH:mm:ss");
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            lblLiveClock.setText(sdf.format(new Date()));
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    public void setTableNumber(int table) {
        lblTableBadge.setText("BÀN " + (table < 10 ? "0" + table : table));
    }

    public void setConnectionState(boolean connected) {
        if (connected) {
            lblConnectionStatus.setText("● Máy chủ Trực tuyến");
            lblConnectionStatus.getParent().getStyleClass().setAll("status-pill-online");
            lblConnectionStatus.getStyleClass().setAll("status-pill-text");
        } else {
            lblConnectionStatus.setText("● Mất kết nối");
            lblConnectionStatus.getParent().getStyleClass().setAll("status-pill-offline");
            lblConnectionStatus.getStyleClass().setAll("status-pill-offline-text");
        }
    }

    public void renderCategories(List<String> categories, String selectedCategory, Consumer<String> onSelect) {
        categoryTabsBox.getChildren().clear();
        for (String cat : categories) {
            Button btn = new Button(cat);
            if (cat.equalsIgnoreCase(selectedCategory)) {
                btn.getStyleClass().add("category-tab-active");
            } else {
                btn.getStyleClass().add("category-tab");
            }
            btn.setOnAction(e -> onSelect.accept(cat));
            categoryTabsBox.getChildren().add(btn);
        }
    }

    public void renderMenuItems(List<MenuItem> items, Consumer<MenuItem> onSelect) {
        itemsFlowPane.getChildren().clear();
        for (MenuItem item : items) {
            ItemCard card = new ItemCard(item, onSelect);
            itemsFlowPane.getChildren().add(card);
        }
    }

    public void renderCart(List<OrderItem> items, double totalAmount, int totalCount,
                           BiConsumer<OrderItem, Integer> onQtyChange, Consumer<OrderItem> onDelete) {
        cartItemsContainer.getChildren().clear();
        boolean hasItems = items != null && !items.isEmpty();
        emptyCartBox.setVisible(!hasItems);
        emptyCartBox.setManaged(!hasItems);
        cartItemsContainer.setVisible(hasItems);
        cartItemsContainer.setManaged(hasItems);

        if (hasItems) {
            for (OrderItem oi : items) {
                CartItemRow row = new CartItemRow(oi, onQtyChange, onDelete);
                cartItemsContainer.getChildren().add(row);
            }
        }

        lblCartCount.setText(totalCount + " món");
        String formatted = CurrencyFormatter.format(totalAmount);
        lblSubtotalValue.setText(formatted);
        lblGrandTotalValue.setText(formatted);
        btnSubmitOrder.setDisable(!hasItems);
    }

    public void updateActiveOrderStatus(int orderId, String statusDisplay) {
        activeOrderBox.setVisible(true);
        activeOrderBox.setManaged(true);
        lblActiveOrderId.setText("⚡ Đơn gần nhất: #" + orderId);
        lblActiveOrderStatus.setText("Trạng thái: " + statusDisplay);
    }

    public void showToast(String message) {
        lblToastMessage.setText(message);
        notificationToast.setVisible(true);
        notificationToast.setManaged(true);

        Timeline hideToast = new Timeline(new KeyFrame(Duration.seconds(4), e -> {
            notificationToast.setVisible(false);
            notificationToast.setManaged(false);
        }));
        hideToast.play();
    }

    public TextField getSearchField() { return txtSearch; }
    public Button getSubmitOrderButton() { return btnSubmitOrder; }
}
