package cafe.views;

import cafe.models.MenuItem;
import cafe.models.OrderItem;
import cafe.utils.CurrencyFormatter;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.HPos;
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
    private final GridPane itemsGridPane = new GridPane();
    private final VBox emptySearchBox = new VBox(10);
    private final StackPane catalogContentHolder = new StackPane();
    private final ScrollPane catalogScrollPane = new ScrollPane();
    private List<MenuItem> currentMenuItems = new java.util.ArrayList<>();
    private Consumer<MenuItem> currentOnSelect;
    private final List<ItemCard> currentCards = new java.util.ArrayList<>();
    private final javafx.animation.PauseTransition resizeDebounce = new javafx.animation.PauseTransition(Duration.millis(60));
    private int currentCols = -1;
    private double currentCardWidth = -1;

    // Cart controls
    private final Label lblCartCount = new Label("0 món");
    private final ToggleGroup orderTypeGroup = new ToggleGroup();
    private final ToggleButton btnDineIn = new ToggleButton("Dùng tại bàn");
    private final ToggleButton btnTakeaway = new ToggleButton("Mang về");

    private final VBox cartItemsContainer = new VBox(10);
    private final ScrollPane cartScrollPane = new ScrollPane();
    private final VBox emptyCartBox = new VBox(8);

    // Voucher controls
    private final TextField txtVoucher = new TextField();
    private final Button btnApplyVoucher = new Button("Áp dụng");
    private final Label lblVoucherFeedback = new Label();
    private final Label lblDiscountLabel = new Label("Giảm giá (Voucher):");
    private final Label lblDiscountValue = new Label("-0 đ");
    private final HBox rowDiscount = new HBox();

    private final Label lblSubtotalValue = new Label("0 đ");
    private final Label lblGrandTotalValue = new Label("0 đ");
    private final Button btnSubmitOrder = new Button("GỬI BẾP ORDER");

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

        VBox brandText = new VBox(1);
        Label lblBrandName = new Label("L'Amour Artisan Cafe");
        lblBrandName.getStyleClass().add("brand-title");
        Label lblSlogan = new Label("Fine Specialty Coffee & Tea Lounge");
        lblSlogan.getStyleClass().add("brand-subtitle");
        brandText.getChildren().addAll(lblBrandName, lblSlogan);

        brand.getChildren().add(brandText);

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
        // Padding bên phải = 0 để ScrollBar nằm sát mép tiếp giáp Giỏ hàng (Requirement 6)
        section.setPadding(new Insets(16, 0, 16, 24));

        // Search & Category Tabs
        VBox filterBar = new VBox(12);
        filterBar.setPadding(new Insets(0, 20, 0, 0)); // Chừa lề phải cho thanh search và tabs

        HBox searchBox = new HBox(8);
        searchBox.getStyleClass().add("search-box");
        searchBox.setAlignment(Pos.CENTER_LEFT);
        txtSearch.getStyleClass().add("search-field");
        txtSearch.setPromptText("Tìm kiếm món yêu thích (Cà phê, Trà đào, Matcha...)...");
        HBox.setHgrow(txtSearch, Priority.ALWAYS);
        searchBox.getChildren().addAll(txtSearch);

        categoryTabsBox.setAlignment(Pos.CENTER_LEFT);

        filterBar.getChildren().addAll(searchBox, categoryTabsBox);

        // Responsive GridPane for Menu Item Cards
        itemsGridPane.setHgap(16);
        itemsGridPane.setVgap(16);
        itemsGridPane.setPadding(new Insets(8, 16, 20, 0));
        itemsGridPane.setAlignment(Pos.TOP_LEFT);

        // Empty Search Results Placeholder (Requirement: thông báo khi 0 kết quả)
        emptySearchBox.setAlignment(Pos.CENTER);
        emptySearchBox.setPadding(new Insets(70, 20, 70, 20));
        Label emptySearchTitle = new Label("Không tìm thấy món ăn phù hợp");
        emptySearchTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #5D4037;");
        Label emptySearchDesc = new Label("Vui lòng thử từ khóa khác hoặc chọn nhóm danh mục bên trên.");
        emptySearchDesc.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #9E8E87;");
        emptySearchBox.getChildren().addAll(emptySearchTitle, emptySearchDesc);
        emptySearchBox.setVisible(false);
        emptySearchBox.setManaged(false);

        catalogContentHolder.getChildren().addAll(itemsGridPane, emptySearchBox);
        catalogContentHolder.setAlignment(Pos.TOP_LEFT);

        catalogScrollPane.setContent(catalogContentHolder);
        catalogScrollPane.getStyleClass().add("catalog-scroll-pane");
        catalogScrollPane.setFitToWidth(true);
        catalogScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        catalogScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(catalogScrollPane, Priority.ALWAYS);

        // Tự động tính toán lại số cột và co dãn thẻ theo chiều rộng viewport với debounce mượt mà
        resizeDebounce.setOnFinished(e -> relayoutMenuItems());
        catalogScrollPane.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getWidth() > 0) {
                resizeDebounce.playFromStart();
            }
        });

        section.getChildren().addAll(filterBar, catalogScrollPane);
        return section;
    }

    private VBox buildCartSidebar() {
        VBox sidebar = new VBox(12);
        sidebar.getStyleClass().add("cart-sidebar");
        // Giữ nguyên cố định 350px để vùng Center chiếm trọn vẹn phần còn lại (Requirement 5)
        sidebar.setPrefWidth(350);
        sidebar.setMinWidth(350);
        sidebar.setMaxWidth(350);

        // Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label lblCartTitle = new Label("Giỏ hàng của bạn");
        lblCartTitle.getStyleClass().add("cart-header-title");
        HBox.setHgrow(lblCartTitle, Priority.ALWAYS);

        lblCartCount.getStyleClass().add("cart-badge-count");
        header.getChildren().addAll(lblCartTitle, lblCartCount);

        // Order Type Selector (DINE_IN vs TAKEAWAY)
        HBox orderTypeBox = new HBox(8);
        orderTypeBox.setAlignment(Pos.CENTER);
        btnDineIn.setToggleGroup(orderTypeGroup);
        btnDineIn.setSelected(true);
        btnDineIn.getStyleClass().add("category-tab");
        btnDineIn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnDineIn, Priority.ALWAYS);

        btnTakeaway.setToggleGroup(orderTypeGroup);
        btnTakeaway.getStyleClass().add("category-tab");
        btnTakeaway.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnTakeaway, Priority.ALWAYS);

        orderTypeBox.getChildren().addAll(btnDineIn, btnTakeaway);

        // Items Container with ScrollPane
        cartItemsContainer.setPadding(new Insets(4, 2, 4, 2));

        // Empty state
        emptyCartBox.getStyleClass().add("cart-empty-box");
        Label emptyText = new Label("Chưa có món nào được chọn");
        emptyText.getStyleClass().add("cart-empty-text");
        Label emptySub = new Label("Hãy chạm vào thẻ món để thêm vào giỏ nhé!");
        emptySub.setStyle("-fx-font-size: 11px; -fx-text-fill: #B59F95;");
        emptyCartBox.getChildren().addAll(emptyText, emptySub);

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

        // Voucher Input Section
        VBox voucherSection = new VBox(4);
        HBox voucherInputBox = new HBox(6);
        voucherInputBox.setAlignment(Pos.CENTER_LEFT);
        txtVoucher.setPromptText("Mã giảm giá (WELCOME10, GIAM15K)");
        txtVoucher.setStyle("-fx-font-size: 11px; -fx-background-color: #FFFFFF; -fx-background-radius: 8; -fx-border-color: #E0D3C9; -fx-border-radius: 8; -fx-padding: 5 8 5 8;");
        HBox.setHgrow(txtVoucher, Priority.ALWAYS);

        btnApplyVoucher.setStyle("-fx-background-color: #5D4037; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: 700; -fx-background-radius: 8; -fx-padding: 5 10 5 10; -fx-cursor: hand;");
        voucherInputBox.getChildren().addAll(txtVoucher, btnApplyVoucher);

        lblVoucherFeedback.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 600; -fx-padding: 0 0 0 2;");
        lblVoucherFeedback.setVisible(false);
        lblVoucherFeedback.setManaged(false);
        voucherSection.getChildren().addAll(voucherInputBox, lblVoucherFeedback);

        // Bill Summary
        VBox summaryBox = new VBox(7);
        summaryBox.getStyleClass().add("bill-summary-box");

        HBox row1 = new HBox();
        Label lblSubLabel = new Label("Tạm tính:");
        lblSubLabel.getStyleClass().add("bill-label");
        Region sp1 = new Region();
        HBox.setHgrow(sp1, Priority.ALWAYS);
        lblSubtotalValue.getStyleClass().add("bill-value");
        row1.getChildren().addAll(lblSubLabel, sp1, lblSubtotalValue);

        // Voucher Discount row
        rowDiscount.setAlignment(Pos.CENTER_LEFT);
        lblDiscountLabel.getStyleClass().add("bill-label");
        Region spDisc = new Region();
        HBox.setHgrow(spDisc, Priority.ALWAYS);
        lblDiscountValue.setStyle("-fx-font-size: 12px; -fx-text-fill: #2E7D32; -fx-font-weight: 700;");
        rowDiscount.getChildren().addAll(lblDiscountLabel, spDisc, lblDiscountValue);
        rowDiscount.setVisible(false);
        rowDiscount.setManaged(false);

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

        summaryBox.getChildren().addAll(row1, rowDiscount, row2, sep, rowTotal);

        // Submit Button
        btnSubmitOrder.getStyleClass().add("btn-checkout");
        btnSubmitOrder.setMaxWidth(Double.MAX_VALUE);
        btnSubmitOrder.setDisable(true);

        sidebar.getChildren().addAll(header, orderTypeBox, cartScrollPane, activeOrderBox, voucherSection, summaryBox, btnSubmitOrder);
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
        this.currentMenuItems = items != null ? items : new java.util.ArrayList<>();
        this.currentOnSelect = onSelect;

        currentCards.clear();
        for (MenuItem item : currentMenuItems) {
            currentCards.add(new ItemCard(item, currentOnSelect));
        }
        currentCols = -1; // Force layout recalculation
        relayoutMenuItems();
    }

    private void relayoutMenuItems() {
        if (currentMenuItems == null || currentMenuItems.isEmpty() || currentCards.isEmpty()) {
            itemsGridPane.getChildren().clear();
            itemsGridPane.getColumnConstraints().clear();
            emptySearchBox.setVisible(true);
            emptySearchBox.setManaged(true);
            return;
        }

        emptySearchBox.setVisible(false);
        emptySearchBox.setManaged(false);

        // Chiều rộng thực tế của viewport (loại trừ thanh cuộn dọc nếu có)
        double viewportW = catalogScrollPane.getViewportBounds().getWidth();
        // Trừ padding phải (16px) và biên an toàn (4px)
        double availableWidth = (viewportW > 100) ? (viewportW - 20) : 850.0;

        final double MIN_CARD_WIDTH = 210.0;
        final double GAP = 16.0;

        // Thuật toán Responsive chuẩn theo yêu cầu:
        // Số cột = max(1, (availableWidth + GAP) / (MIN_CARD_WIDTH + GAP))
        int cols = Math.max(1, (int) ((availableWidth + GAP) / (MIN_CARD_WIDTH + GAP)));
        // Chiều rộng mỗi thẻ = (availableWidth - GAP * (cột - 1)) / cột
        double cardWidth = Math.floor((availableWidth - GAP * (cols - 1)) / cols);

        // Nếu số cột và bề rộng không đổi đáng kể, bỏ qua việc render lại DOM
        if (cols == currentCols && Math.abs(cardWidth - currentCardWidth) < 1.0 && !itemsGridPane.getChildren().isEmpty()) {
            return;
        }

        currentCols = cols;
        currentCardWidth = cardWidth;

        itemsGridPane.getChildren().clear();
        itemsGridPane.getColumnConstraints().clear();

        // Thiết lập ràng buộc kích thước từng cột
        for (int c = 0; c < cols; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPrefWidth(cardWidth);
            cc.setMinWidth(cardWidth);
            cc.setMaxWidth(cardWidth);
            cc.setHgrow(Priority.NEVER);
            cc.setHalignment(HPos.LEFT);
            itemsGridPane.getColumnConstraints().add(cc);
        }

        // Tái sử dụng các thẻ đã được tạo sẵn trong cache
        for (int i = 0; i < currentCards.size(); i++) {
            ItemCard card = currentCards.get(i);
            card.setPrefWidth(cardWidth);
            card.setMinWidth(cardWidth);
            card.setMaxWidth(Double.MAX_VALUE);
            int col = i % cols;
            int row = i / cols;
            itemsGridPane.add(card, col, row);
        }
    }

    public void renderCart(List<OrderItem> items, double subtotal, double discount, double grandTotal, int totalCount,
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
        lblSubtotalValue.setText(CurrencyFormatter.format(subtotal));

        boolean hasDiscount = discount > 0;
        rowDiscount.setVisible(hasDiscount);
        rowDiscount.setManaged(hasDiscount);
        lblDiscountValue.setText("-" + CurrencyFormatter.format(discount));

        lblGrandTotalValue.setText(CurrencyFormatter.format(grandTotal));
        btnSubmitOrder.setDisable(!hasItems);
    }

    public ToggleGroup getOrderTypeGroup() { return orderTypeGroup; }
    public ToggleButton getBtnDineIn() { return btnDineIn; }
    public ToggleButton getBtnTakeaway() { return btnTakeaway; }
    public TextField getVoucherField() { return txtVoucher; }
    public Button getApplyVoucherButton() { return btnApplyVoucher; }
    public Label getVoucherFeedbackLabel() { return lblVoucherFeedback; }

    public void updateActiveOrderStatus(int orderId, String statusDisplay) {
        activeOrderBox.setVisible(true);
        activeOrderBox.setManaged(true);
        lblActiveOrderId.setText("Đơn gần nhất: #" + orderId);
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
