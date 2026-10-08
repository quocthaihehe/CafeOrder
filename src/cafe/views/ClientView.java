package cafe.views;

import cafe.models.MenuItem;
import cafe.models.OrderItem;
import cafe.utils.CurrencyFormatter;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * ClientView - Giao diện Kiosk đặt món tại bàn theo phong cách Editorial Minimalism.
 * Triển khai theo Hallmark Design:
 * 1. CỘT TRÁI: Danh mục dọc với nhận diện Brew & Bean CAFE, nút danh mục 4px radius.
 * 2. CỘT GIỮA: Thanh tìm kiếm + Lưới thực đơn 3 cột responsive + ProductCard tối giản.
 * 3. CỘT PHẢI: Giỏ hàng cố định, phân cấp thị giác mạnh, Voucher, Tổng thanh toán, Nút Dark Emerald.
 */
public class ClientView extends BorderPane {

    // Header controls
    private final Label lblTableBadge = new Label("BÀN 01");
    private final Label lblTableStatus = new Label("● Đang phục vụ");
    private final Label lblConnectionStatus = new Label("● Đã kết nối");
    private final Label lblLiveClock = new Label();

    // Catalog & Category controls
    private final TextField txtSearch = new TextField();
    private final VBox categoryNavBox = new VBox(4);
    private final GridPane itemsGridPane = new GridPane();
    private final VBox emptySearchBox = new VBox(8);
    private final ScrollPane catalogScrollPane = new ScrollPane();
    private List<MenuItem> currentMenuItems = new ArrayList<>();
    private Consumer<MenuItem> currentOnSelect;
    private final List<ItemCard> currentCards = new ArrayList<>();
    private final PauseTransition resizeDebounce = new PauseTransition(Duration.millis(50));
    private int currentCols = -1;
    private double currentCardWidth = -1;

    // Cart controls
    private final Label lblCartCount = new Label("0 món");
    private final ToggleGroup orderTypeGroup = new ToggleGroup();
    private final ToggleButton btnDineIn = new ToggleButton("Dùng tại bàn");
    private final ToggleButton btnTakeaway = new ToggleButton("Mang về");

    private final VBox cartItemsContainer = new VBox(6);
    private final ScrollPane cartScrollPane = new ScrollPane();
    private final VBox emptyCartBox = new VBox(6);

    // Voucher controls
    private final TextField txtVoucher = new TextField();
    private final Button btnApplyVoucher = new Button("Áp dụng");
    private final Label lblVoucherFeedback = new Label();
    private final Label lblDiscountLabel = new Label("Giảm giá (Voucher):");
    private final Label lblDiscountValue = new Label("-0 đ");
    private final HBox rowDiscount = new HBox();

    private final Label lblSubtotalValue = new Label("0 đ");
    private final Label lblGrandTotalValue = new Label("0 đ");
    private final Button btnSubmitOrder = new Button("Thanh toán đơn hàng →");

    // Live Order Status Tracking Box
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
        setStyle("-fx-background-color: #F4F0E8;");

        // 1. CỘT TRÁI: DANH MỤC DỌC (LEFT SIDEBAR)
        VBox leftSidebar = buildLeftCategorySidebar();
        setLeft(leftSidebar);

        // 2. KHU VỰC TRUNG TÂM: HEADER & LƯỚI THỰC ĐƠN
        VBox centerArea = buildCenterCatalogArea();
        setCenter(centerArea);

        // 3. CỘT PHẢI: GIỎ HÀNG & THANH TOÁN
        VBox rightSidebar = buildRightCartSidebar();
        setRight(rightSidebar);

        // 4. TOAST THÔNG BÁO DƯỚI CÙNG
        notificationToast.setAlignment(Pos.CENTER);
        notificationToast.setPadding(new Insets(8, 20, 8, 20));
        notificationToast.setStyle("-fx-background-color: #183C32; -fx-background-radius: 4px;");
        notificationToast.setVisible(false);
        notificationToast.setManaged(false);
        lblToastMessage.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: 700; -fx-font-size: 13px;");
        notificationToast.getChildren().add(lblToastMessage);

        StackPane bottomWrapper = new StackPane(notificationToast);
        bottomWrapper.setPadding(new Insets(0, 0, 14, 0));
        bottomWrapper.setPickOnBounds(false);
        setBottom(bottomWrapper);
    }

    private VBox buildLeftCategorySidebar() {
        VBox sidebar = new VBox(12);
        sidebar.setPrefWidth(185);
        sidebar.setMinWidth(175);
        sidebar.setMaxWidth(200);
        sidebar.getStyleClass().add("sidebar-category-pane");

        // Brand Header
        VBox brandBox = new VBox(2);
        brandBox.getStyleClass().add("brand-header-box");

        Label lblBrand = new Label("Brew & Bean");
        lblBrand.getStyleClass().add("brand-header-title");

        Label lblSub = new Label("C A F E");
        lblSub.getStyleClass().add("brand-header-subtitle");

        brandBox.getChildren().addAll(lblBrand, lblSub);

        // Categories list
        categoryNavBox.setSpacing(4);

        ScrollPane scrollNav = new ScrollPane(categoryNavBox);
        scrollNav.setFitToWidth(true);
        scrollNav.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollNav.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollNav, Priority.ALWAYS);

        // Live Order Tracking Card at bottom of sidebar
        activeOrderBox.setPadding(new Insets(10));
        activeOrderBox.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #D8D1C3; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-border-width: 1px;");
        activeOrderBox.setVisible(false);
        activeOrderBox.setManaged(false);

        lblActiveOrderId.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        lblActiveOrderStatus.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 600; -fx-text-fill: #C05621;");
        activeOrderBox.getChildren().addAll(lblActiveOrderId, lblActiveOrderStatus);

        sidebar.getChildren().addAll(brandBox, scrollNav, activeOrderBox);
        return sidebar;
    }

    private VBox buildCenterCatalogArea() {
        VBox center = new VBox(12);
        center.setPadding(new Insets(16, 20, 16, 20));
        HBox.setHgrow(center, Priority.ALWAYS);

        // Top Utility Bar: Table info, status & live clock
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        lblTableBadge.getStyleClass().add("table-tag");
        lblTableStatus.getStyleClass().add("status-tag-active");
        lblConnectionStatus.getStyleClass().add("status-tag-done");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        lblLiveClock.setStyle("-fx-font-size: 12px; -fx-font-weight: 500; -fx-text-fill: #817B70;");

        topBar.getChildren().addAll(lblTableBadge, lblTableStatus, lblConnectionStatus, spacer, lblLiveClock);

        // Search Input Bar
        HBox searchRow = new HBox(10);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        txtSearch.setPromptText("Tìm kiếm thức uống, món ăn, cà phê, trà...");
        txtSearch.getStyleClass().add("text-input-field");
        txtSearch.setPrefHeight(38);
        HBox.setHgrow(txtSearch, Priority.ALWAYS);

        searchRow.getChildren().add(txtSearch);

        // Catalog Grid inside ScrollPane
        itemsGridPane.setHgap(12);
        itemsGridPane.setVgap(12);
        itemsGridPane.setPadding(new Insets(2, 4, 16, 4));

        catalogScrollPane.setContent(itemsGridPane);
        catalogScrollPane.setFitToWidth(true);
        catalogScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        catalogScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(catalogScrollPane, Priority.ALWAYS);

        // Responsive grid column relayout on width resize
        catalogScrollPane.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getWidth() > 0) {
                resizeDebounce.setOnFinished(e -> {
                    double viewportW = newVal.getWidth() - 20;
                    int targetCols = 3;
                    if (viewportW < 420) {
                        targetCols = 1;
                    } else if (viewportW < 640) {
                        targetCols = 2;
                    } else if (viewportW >= 960) {
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
        emptySearchBox.setPadding(new Insets(60, 20, 60, 20));
        emptySearchBox.setVisible(false);
        emptySearchBox.setManaged(false);

        Label emptyIcon = new Label("☕");
        emptyIcon.setStyle("-fx-font-size: 36px; -fx-text-fill: #D8D1C3;");
        Label emptyTitle = new Label("Không tìm thấy món phù hợp");
        emptyTitle.getStyleClass().add("editorial-title");
        Label emptyDesc = new Label("Thử tìm kiếm với từ khóa khác hoặc chọn danh mục phía bên trái.");
        emptyDesc.getStyleClass().add("editorial-subtitle");

        emptySearchBox.getChildren().addAll(emptyIcon, emptyTitle, emptyDesc);

        center.getChildren().addAll(topBar, searchRow, catalogScrollPane, emptySearchBox);
        return center;
    }

    private VBox buildRightCartSidebar() {
        VBox right = new VBox(12);
        right.setPrefWidth(330);
        right.setMinWidth(300);
        right.setMaxWidth(350);
        right.getStyleClass().add("cart-sidebar-pane");

        // 1. Cart Header
        HBox cartHeader = new HBox(8);
        cartHeader.setAlignment(Pos.CENTER_LEFT);

        Label lblCartTitle = new Label("ĐƠN HÀNG CỦA BẠN");
        lblCartTitle.getStyleClass().add("editorial-title");
        HBox.setHgrow(lblCartTitle, Priority.ALWAYS);

        lblCartCount.getStyleClass().add("table-tag");
        cartHeader.getChildren().addAll(lblCartTitle, lblCartCount);

        // 2. Dine-In / Takeaway Toggle Buttons
        HBox typeSelector = new HBox(8);
        typeSelector.setAlignment(Pos.CENTER);

        btnDineIn.setToggleGroup(orderTypeGroup);
        btnDineIn.setSelected(true);
        btnDineIn.getStyleClass().add("btn-option-tag");
        btnDineIn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnDineIn, Priority.ALWAYS);

        btnTakeaway.setToggleGroup(orderTypeGroup);
        btnTakeaway.getStyleClass().add("btn-option-tag");
        btnTakeaway.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnTakeaway, Priority.ALWAYS);

        typeSelector.getChildren().addAll(btnDineIn, btnTakeaway);

        // 3. Cart Items Scroll List
        cartItemsContainer.setSpacing(6);
        cartItemsContainer.setPadding(new Insets(2, 2, 8, 2));

        cartScrollPane.setContent(cartItemsContainer);
        cartScrollPane.setFitToWidth(true);
        cartScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        cartScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(cartScrollPane, Priority.ALWAYS);

        // Empty Cart Placeholder
        emptyCartBox.setAlignment(Pos.CENTER);
        emptyCartBox.setPadding(new Insets(40, 16, 40, 16));
        Label lblEmptyCartIcon = new Label("🛒");
        lblEmptyCartIcon.setStyle("-fx-font-size: 32px; -fx-text-fill: #D8D1C3;");
        Label lblEmptyCartMsg = new Label("Chưa có món nào");
        lblEmptyCartMsg.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        Label lblEmptyCartSub = new Label("Vui lòng chọn món ngon từ thực đơn bên cạnh.");
        lblEmptyCartSub.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #817B70; -fx-text-alignment: center;");
        lblEmptyCartSub.setWrapText(true);

        emptyCartBox.getChildren().addAll(lblEmptyCartIcon, lblEmptyCartMsg, lblEmptyCartSub);

        // 4. Voucher Input Row
        VBox voucherSection = new VBox(4);
        HBox voucherRow = new HBox(6);
        voucherRow.setAlignment(Pos.CENTER_LEFT);

        txtVoucher.setPromptText("Nhập mã ưu đãi...");
        txtVoucher.getStyleClass().add("text-input-field");
        HBox.setHgrow(txtVoucher, Priority.ALWAYS);

        btnApplyVoucher.getStyleClass().add("btn-secondary");
        voucherRow.getChildren().addAll(txtVoucher, btnApplyVoucher);

        lblVoucherFeedback.setVisible(false);
        lblVoucherFeedback.setManaged(false);
        voucherSection.getChildren().addAll(voucherRow, lblVoucherFeedback);

        // 5. Bill Summary Breakdown
        VBox summaryBox = new VBox(6);
        summaryBox.setPadding(new Insets(10, 0, 4, 0));
        summaryBox.setStyle("-fx-border-color: #E6DFD4 transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        HBox rowSubtotal = new HBox();
        Label lblSub = new Label("Tạm tính:");
        lblSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #817B70;");
        Region s1 = new Region();
        HBox.setHgrow(s1, Priority.ALWAYS);
        lblSubtotalValue.setStyle("-fx-font-size: 12px; -fx-text-fill: #292722; -fx-font-weight: 600;");
        rowSubtotal.getChildren().addAll(lblSub, s1, lblSubtotalValue);

        lblDiscountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #183C32; -fx-font-weight: 700;");
        Region s2 = new Region();
        HBox.setHgrow(s2, Priority.ALWAYS);
        lblDiscountValue.setStyle("-fx-font-size: 12px; -fx-text-fill: #183C32; -fx-font-weight: 700;");
        rowDiscount.getChildren().addAll(lblDiscountLabel, s2, lblDiscountValue);
        rowDiscount.setVisible(false);
        rowDiscount.setManaged(false);

        HBox rowTotal = new HBox();
        rowTotal.setAlignment(Pos.CENTER_LEFT);
        rowTotal.setPadding(new Insets(4, 0, 0, 0));
        Label lblTot = new Label("TỔNG CỘNG:");
        lblTot.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 800; -fx-text-fill: #292722;");
        Region s3 = new Region();
        HBox.setHgrow(s3, Priority.ALWAYS);
        lblGrandTotalValue.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #A66A42;");
        rowTotal.getChildren().addAll(lblTot, s3, lblGrandTotalValue);

        summaryBox.getChildren().addAll(rowSubtotal, rowDiscount, rowTotal);

        // 6. Primary Checkout Button
        btnSubmitOrder.getStyleClass().add("btn-primary");
        btnSubmitOrder.setMaxWidth(Double.MAX_VALUE);
        btnSubmitOrder.setPrefHeight(42);

        right.getChildren().addAll(cartHeader, typeSelector, cartScrollPane, voucherSection, summaryBox, btnSubmitOrder);
        return right;
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

    public void renderCategories(List<String> categories, String selectedCategory, Consumer<String> onSelect) {
        categoryNavBox.getChildren().clear();

        Button btnAll = new Button("Tất cả món");
        btnAll.setMaxWidth(Double.MAX_VALUE);
        btnAll.getStyleClass().add("category-nav-btn");
        if ("Tất cả".equalsIgnoreCase(selectedCategory)) {
            btnAll.getStyleClass().add("selected");
        }
        btnAll.setOnAction(e -> onSelect.accept("Tất cả"));
        categoryNavBox.getChildren().add(btnAll);

        for (String cat : categories) {
            Button btnCat = new Button(cat);
            btnCat.setMaxWidth(Double.MAX_VALUE);
            btnCat.getStyleClass().add("category-nav-btn");
            if (cat.equalsIgnoreCase(selectedCategory)) {
                btnCat.getStyleClass().add("selected");
            }
            btnCat.setOnAction(e -> onSelect.accept(cat));
            categoryNavBox.getChildren().add(btnCat);
        }
    }

    public void renderMenuItems(List<MenuItem> items, Consumer<MenuItem> onSelect) {
        this.currentMenuItems = items;
        this.currentOnSelect = onSelect;
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
            ItemCard card = new ItemCard(item, onSelect);
            currentCards.add(card);
        }

        int cols = currentCols > 0 ? currentCols : 3;
        double w = currentCardWidth > 0 ? currentCardWidth : 220;
        relayoutGrid(cols, w);
    }

    public void renderCart(List<OrderItem> items, double subtotal, double discount, double total, int totalQty,
                           BiConsumer<OrderItem, Integer> onQtyChange, Consumer<OrderItem> onDelete) {
        cartItemsContainer.getChildren().clear();

        if (items.isEmpty()) {
            cartScrollPane.setContent(emptyCartBox);
            lblCartCount.setText("0 món");
            lblSubtotalValue.setText("0 đ");
            lblGrandTotalValue.setText("0 đ");
            rowDiscount.setVisible(false);
            rowDiscount.setManaged(false);
            btnSubmitOrder.setDisable(true);
            return;
        }

        btnSubmitOrder.setDisable(false);
        lblCartCount.setText(totalQty + " món");
        cartScrollPane.setContent(cartItemsContainer);

        for (OrderItem oi : items) {
            CartItemRow row = new CartItemRow(oi, onQtyChange, onDelete);
            cartItemsContainer.getChildren().add(row);
        }

        lblSubtotalValue.setText(CurrencyFormatter.format(subtotal));
        if (discount > 0) {
            lblDiscountValue.setText("-" + CurrencyFormatter.format(discount));
            rowDiscount.setVisible(true);
            rowDiscount.setManaged(true);
        } else {
            rowDiscount.setVisible(false);
            rowDiscount.setManaged(false);
        }
        lblGrandTotalValue.setText(CurrencyFormatter.format(total));
    }

    public void setTableNumber(int tableNumber) {
        lblTableBadge.setText("BÀN " + (tableNumber < 10 ? "0" + tableNumber : tableNumber));
    }

    public void setConnectionState(boolean connected) {
        if (connected) {
            lblConnectionStatus.setText("● Đã kết nối");
            lblConnectionStatus.setStyle("-fx-background-color: #EDF7ED; -fx-text-fill: #1B5E20; -fx-border-color: #C8E6C9; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 3 8 3 8; -fx-font-size: 11px; -fx-font-weight: 600;");
        } else {
            lblConnectionStatus.setText("○ Mất kết nối");
            lblConnectionStatus.setStyle("-fx-background-color: #FDEEEE; -fx-text-fill: #C62828; -fx-border-color: #F9CBCB; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 3 8 3 8; -fx-font-size: 11px; -fx-font-weight: 600;");
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
    public ToggleGroup getOrderTypeGroup() { return orderTypeGroup; }
    public ToggleButton getBtnTakeaway() { return btnTakeaway; }
    public Button getApplyVoucherButton() { return btnApplyVoucher; }
    public Button getSubmitOrderButton() { return btnSubmitOrder; }
    public TextField getVoucherField() { return txtVoucher; }
    public Label getVoucherFeedbackLabel() { return lblVoucherFeedback; }
}
