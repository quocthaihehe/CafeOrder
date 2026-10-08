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
 * ClientView - Giao diện Kiosk đặt món tại bàn theo thiết kế Screen 3 & 5 (UI_mong_muon.png).
 * Kiến trúc 3 cột:
 * 1. CỘT TRÁI: Thanh danh mục dọc (Left Category Sidebar) với thương hiệu Brew & Bean CAFE.
 * 2. CỘT GIỮA: Thanh tìm kiếm + Lưới thực đơn 3 cột responsive + Thẻ món tối giản.
 * 3. CỘT PHẢI: Giỏ hàng + Tóm tắt đơn hàng + Mã giảm giá + Nút đặt món Dark Emerald.
 */
public class ClientView extends BorderPane {

    // Header controls
    private final Label lblTableBadge = new Label("BÀN 01");
    private final Label lblTableStatus = new Label("● Đang phục vụ");
    private final Label lblConnectionStatus = new Label("● Đã kết nối");
    private final Label lblLiveClock = new Label();

    // Catalog & Category controls
    private final TextField txtSearch = new TextField();
    private final VBox categoryNavBox = new VBox(6);
    private final GridPane itemsGridPane = new GridPane();
    private final VBox emptySearchBox = new VBox(10);
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

    private final VBox cartItemsContainer = new VBox(8);
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
    private final Button btnSubmitOrder = new Button("Xác nhận thanh toán →");

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
        notificationToast.setPadding(new Insets(10, 24, 10, 24));
        notificationToast.setStyle("-fx-background-color: #183C32; -fx-background-radius: 8px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 10, 0, 0, 4);");
        notificationToast.setVisible(false);
        notificationToast.setManaged(false);
        lblToastMessage.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: 700; -fx-font-size: 13.5px;");
        notificationToast.getChildren().add(lblToastMessage);

        StackPane bottomWrapper = new StackPane(notificationToast);
        bottomWrapper.setPadding(new Insets(0, 0, 16, 0));
        bottomWrapper.setPickOnBounds(false);
        setBottom(bottomWrapper);
    }

    private VBox buildLeftCategorySidebar() {
        VBox sidebar = new VBox(14);
        sidebar.setPrefWidth(185);
        sidebar.setMinWidth(175);
        sidebar.setMaxWidth(200);
        sidebar.setPadding(new Insets(18, 12, 18, 14));
        sidebar.setStyle(
            "-fx-background-color: #FBF9F4; " +
            "-fx-border-color: transparent #D8D1C3 transparent transparent; " +
            "-fx-border-width: 0 1 0 0;"
        );

        // Header Thương hiệu Brew & Bean CAFE
        VBox brandBox = new VBox(2);
        brandBox.setPadding(new Insets(4, 8, 14, 8));
        brandBox.setStyle("-fx-border-color: transparent transparent #E6DFD4 transparent; -fx-border-width: 0 0 1 0;");

        Label lblBrand = new Label("Brew & Bean");
        lblBrand.setStyle("-fx-font-family: 'Playfair Display', 'Georgia', serif; -fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #183C32;");

        Label lblSub = new Label("C A F E");
        lblSub.setStyle("-fx-font-size: 9.5px; -fx-font-weight: 700; -fx-text-fill: #A66A42; -fx-letter-spacing: 2px;");

        brandBox.getChildren().addAll(lblBrand, lblSub);

        // Danh sách chuyên mục dọc
        categoryNavBox.setSpacing(4);

        ScrollPane scrollNav = new ScrollPane(categoryNavBox);
        scrollNav.setFitToWidth(true);
        scrollNav.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        scrollNav.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollNav.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollNav, Priority.ALWAYS);

        sidebar.getChildren().addAll(brandBox, scrollNav);
        return sidebar;
    }

    private VBox buildCenterCatalogArea() {
        VBox centerBox = new VBox(12);
        centerBox.setPadding(new Insets(16, 20, 16, 20));

        // Thanh điều hướng trên cùng (Top Bar)
        HBox topBar = new HBox(14);
        topBar.setAlignment(Pos.CENTER_LEFT);

        // Badge số bàn & trạng thái
        HBox tableBadgeBox = new HBox(8);
        tableBadgeBox.setAlignment(Pos.CENTER_LEFT);
        tableBadgeBox.setStyle("-fx-background-color: #FBF9F4; -fx-background-radius: 8px; -fx-border-color: #D8D1C3; -fx-border-radius: 8px; -fx-border-width: 1px; -fx-padding: 6 12 6 12;");

        lblTableBadge.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #183C32;");
        lblTableStatus.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #A66A42; -fx-background-color: #FDF4ED; -fx-background-radius: 10px; -fx-padding: 2 8 2 8;");
        tableBadgeBox.getChildren().addAll(lblTableBadge, lblTableStatus);

        // Ô tìm kiếm món ăn
        HBox searchBox = new HBox(8);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-background-radius: 8px; " +
            "-fx-border-color: #D8D1C3; " +
            "-fx-border-radius: 8px; " +
            "-fx-border-width: 1px; " +
            "-fx-padding: 4 12 4 12;"
        );
        HBox.setHgrow(searchBox, Priority.ALWAYS);

        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-font-size: 12px; -fx-text-fill: #817B70;");

        txtSearch.setPromptText("Tìm kiếm món yêu thích (Cà phê, Trà đào, Latte...)...");
        txtSearch.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-font-size: 12.5px; -fx-text-fill: #292722;");
        HBox.setHgrow(txtSearch, Priority.ALWAYS);
        searchBox.getChildren().addAll(searchIcon, txtSearch);

        // Trạng thái kết nối & Đồng hồ
        lblConnectionStatus.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 600; -fx-text-fill: #2E7D32; -fx-background-color: #EDF7ED; -fx-background-radius: 12px; -fx-padding: 4 10 4 10;");
        lblLiveClock.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #817B70;");

        topBar.getChildren().addAll(tableBadgeBox, searchBox, lblConnectionStatus, lblLiveClock);

        // Khung danh sách thẻ món
        itemsGridPane.setHgap(14);
        itemsGridPane.setVgap(14);
        itemsGridPane.setPadding(new Insets(6, 6, 20, 0));
        itemsGridPane.setAlignment(Pos.TOP_LEFT);

        // Placeholder tìm kiếm không ra kết quả
        emptySearchBox.setAlignment(Pos.CENTER);
        emptySearchBox.setPadding(new Insets(60, 20, 60, 20));
        Label emptySearchTitle = new Label("Không tìm thấy món ăn phù hợp");
        emptySearchTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        Label emptySearchDesc = new Label("Vui lòng thử từ khóa khác hoặc chọn danh mục bên trái.");
        emptySearchDesc.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #817B70;");
        emptySearchBox.getChildren().addAll(emptySearchTitle, emptySearchDesc);
        emptySearchBox.setVisible(false);
        emptySearchBox.setManaged(false);

        StackPane gridHolder = new StackPane(emptySearchBox, itemsGridPane);
        gridHolder.setAlignment(Pos.TOP_LEFT);

        catalogScrollPane.setContent(gridHolder);
        catalogScrollPane.setFitToWidth(true);
        catalogScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        catalogScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(catalogScrollPane, Priority.ALWAYS);

        catalogScrollPane.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getWidth() > 0) {
                resizeDebounce.setOnFinished(e -> updateGridLayout(newVal.getWidth()));
                resizeDebounce.playFromStart();
            }
        });

        centerBox.getChildren().addAll(topBar, catalogScrollPane);
        return centerBox;
    }

    private VBox buildRightCartSidebar() {
        VBox sidebar = new VBox(12);
        sidebar.setPrefWidth(320);
        sidebar.setMinWidth(300);
        sidebar.setMaxWidth(340);
        sidebar.setPadding(new Insets(18, 16, 18, 16));
        sidebar.setStyle(
            "-fx-background-color: #FBF9F4; " +
            "-fx-border-color: transparent transparent transparent #D8D1C3; " +
            "-fx-border-width: 0 0 0 1;"
        );

        // 1. Header Giỏ hàng
        HBox cartHeader = new HBox(8);
        cartHeader.setAlignment(Pos.CENTER_LEFT);
        Label lblCartTitle = new Label("Giỏ hàng");
        lblCartTitle.setStyle("-fx-font-family: 'Playfair Display', 'Georgia', serif; -fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        HBox.setHgrow(lblCartTitle, Priority.ALWAYS);

        lblCartCount.setStyle("-fx-background-color: #E8E2D8; -fx-background-radius: 10px; -fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #183C32; -fx-padding: 3 8 3 8;");
        cartHeader.getChildren().addAll(lblCartTitle, lblCartCount);

        // 2. Chọn hình thức (Dùng tại bàn / Mang về)
        HBox orderTypeBox = new HBox(6);
        orderTypeBox.setAlignment(Pos.CENTER);
        btnDineIn.setToggleGroup(orderTypeGroup);
        btnTakeaway.setToggleGroup(orderTypeGroup);
        btnDineIn.setSelected(true);
        btnDineIn.setMaxWidth(Double.MAX_VALUE);
        btnTakeaway.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnDineIn, Priority.ALWAYS);
        HBox.setHgrow(btnTakeaway, Priority.ALWAYS);

        applyOrderTypeToggleStyle(btnDineIn, true);
        applyOrderTypeToggleStyle(btnTakeaway, false);

        btnDineIn.selectedProperty().addListener((obs, oldV, isSel) -> applyOrderTypeToggleStyle(btnDineIn, isSel));
        btnTakeaway.selectedProperty().addListener((obs, oldV, isSel) -> applyOrderTypeToggleStyle(btnTakeaway, isSel));
        orderTypeBox.getChildren().addAll(btnDineIn, btnTakeaway);

        // 3. Danh sách món trong giỏ
        emptyCartBox.setAlignment(Pos.CENTER);
        emptyCartBox.setPadding(new Insets(40, 10, 40, 10));
        Label emptyText = new Label("Giỏ hàng đang trống");
        emptyText.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        Label emptySub = new Label("Hãy chọn những món ngon từ thực đơn.");
        emptySub.setStyle("-fx-font-size: 11px; -fx-text-fill: #817B70;");
        emptyCartBox.getChildren().addAll(emptyText, emptySub);

        StackPane cartContentHolder = new StackPane(emptyCartBox, cartItemsContainer);
        cartScrollPane.setContent(cartContentHolder);
        cartScrollPane.setFitToWidth(true);
        cartScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(cartScrollPane, Priority.ALWAYS);

        // 4. Khung trạng thái đơn gần nhất (KDS Tracker)
        activeOrderBox.setStyle("-fx-background-color: #FDF4ED; -fx-background-radius: 8px; -fx-padding: 8 12 8 12; -fx-border-color: #F4D3BC; -fx-border-radius: 8px; -fx-border-width: 1px;");
        lblActiveOrderId.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        lblActiveOrderStatus.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 600; -fx-text-fill: #A66A42;");
        activeOrderBox.getChildren().addAll(lblActiveOrderId, lblActiveOrderStatus);
        activeOrderBox.setVisible(false);
        activeOrderBox.setManaged(false);

        // 5. Ô nhập Voucher
        VBox voucherSection = new VBox(4);
        HBox voucherInputBox = new HBox(6);
        voucherInputBox.setAlignment(Pos.CENTER_LEFT);
        txtVoucher.setPromptText("Mã giảm giá (WELCOME10, GIAM15K)");
        txtVoucher.setStyle("-fx-font-size: 11px; -fx-background-color: #FFFFFF; -fx-background-radius: 6px; -fx-border-color: #D8D1C3; -fx-border-radius: 6px; -fx-border-width: 1px; -fx-padding: 6 8 6 8;");
        HBox.setHgrow(txtVoucher, Priority.ALWAYS);

        btnApplyVoucher.setStyle("-fx-background-color: #183C32; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: 700; -fx-background-radius: 6px; -fx-padding: 6 12 6 12; -fx-cursor: hand;");
        voucherInputBox.getChildren().addAll(txtVoucher, btnApplyVoucher);

        lblVoucherFeedback.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 600; -fx-padding: 0 0 0 2;");
        lblVoucherFeedback.setVisible(false);
        lblVoucherFeedback.setManaged(false);
        voucherSection.getChildren().addAll(voucherInputBox, lblVoucherFeedback);

        // 6. Bảng tính tiền (Bill Summary)
        VBox summaryBox = new VBox(6);
        summaryBox.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8px; -fx-border-color: #E6DFD4; -fx-border-radius: 8px; -fx-border-width: 1px; -fx-padding: 12;");

        HBox rowSubtotal = new HBox();
        Label lblSubLabel = new Label("Tạm tính:");
        lblSubLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #817B70;");
        Region sp1 = new Region();
        HBox.setHgrow(sp1, Priority.ALWAYS);
        lblSubtotalValue.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        rowSubtotal.getChildren().addAll(lblSubLabel, sp1, lblSubtotalValue);

        rowDiscount.setAlignment(Pos.CENTER_LEFT);
        lblDiscountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #1B5E20; -fx-font-weight: 600;");
        Region spDisc = new Region();
        HBox.setHgrow(spDisc, Priority.ALWAYS);
        lblDiscountValue.setStyle("-fx-font-size: 12px; -fx-text-fill: #1B5E20; -fx-font-weight: 700;");
        rowDiscount.getChildren().addAll(lblDiscountLabel, spDisc, lblDiscountValue);
        rowDiscount.setVisible(false);
        rowDiscount.setManaged(false);

        HBox rowService = new HBox();
        Label lblServLabel = new Label("Phí phục vụ:");
        lblServLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #817B70;");
        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);
        Label lblServVal = new Label("Miễn phí");
        lblServVal.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #1B5E20;");
        rowService.getChildren().addAll(lblServLabel, sp2, lblServVal);

        Separator sep = new Separator();

        HBox rowTotal = new HBox();
        Label lblTotalLabel = new Label("Tổng cộng:");
        lblTotalLabel.setStyle("-fx-font-family: 'Playfair Display', 'Georgia', serif; -fx-font-size: 14.5px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        Region sp3 = new Region();
        HBox.setHgrow(sp3, Priority.ALWAYS);
        lblGrandTotalValue.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #183C32;");
        rowTotal.getChildren().addAll(lblTotalLabel, sp3, lblGrandTotalValue);

        summaryBox.getChildren().addAll(rowSubtotal, rowDiscount, rowService, sep, rowTotal);

        // 7. Nút Gửi đơn / Thanh toán (Primary Button #183C32)
        btnSubmitOrder.setMaxWidth(Double.MAX_VALUE);
        btnSubmitOrder.setPrefHeight(44);
        btnSubmitOrder.setStyle(
            "-fx-background-color: #183C32; " +
            "-fx-background-radius: 8px; " +
            "-fx-text-fill: #FFFFFF; " +
            "-fx-font-size: 13.5px; " +
            "-fx-font-weight: 700; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(24, 60, 50, 0.25), 6, 0, 0, 2);"
        );
        btnSubmitOrder.setOnMouseEntered(e -> {
            if (!btnSubmitOrder.isDisable()) {
                btnSubmitOrder.setStyle("-fx-background-color: #102C25; -fx-background-radius: 8px; -fx-text-fill: #FFFFFF; -fx-font-size: 13.5px; -fx-font-weight: 700; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(16, 44, 37, 0.35), 8, 0, 0, 3);");
            }
        });
        btnSubmitOrder.setOnMouseExited(e -> {
            if (!btnSubmitOrder.isDisable()) {
                btnSubmitOrder.setStyle("-fx-background-color: #183C32; -fx-background-radius: 8px; -fx-text-fill: #FFFFFF; -fx-font-size: 13.5px; -fx-font-weight: 700; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(24, 60, 50, 0.25), 6, 0, 0, 2);");
            }
        });

        sidebar.getChildren().addAll(
            cartHeader,
            orderTypeBox,
            cartScrollPane,
            activeOrderBox,
            voucherSection,
            summaryBox,
            btnSubmitOrder
        );

        return sidebar;
    }

    private void applyOrderTypeToggleStyle(ToggleButton tb, boolean isSelected) {
        if (isSelected) {
            tb.setStyle("-fx-background-color: #183C32; -fx-text-fill: #FFFFFF; -fx-font-weight: 700; -fx-font-size: 11.5px; -fx-background-radius: 6px; -fx-padding: 6 10 6 10; -fx-cursor: hand;");
        } else {
            tb.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #817B70; -fx-font-weight: 600; -fx-font-size: 11.5px; -fx-background-radius: 6px; -fx-border-color: #D8D1C3; -fx-border-radius: 6px; -fx-padding: 6 10 6 10; -fx-cursor: hand;");
        }
    }

    private void startClock() {
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
            lblLiveClock.setText(df.format(new Date()));
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    public void setTableNumber(int number) {
        lblTableBadge.setText("BÀN " + (number < 10 ? "0" + number : number));
    }

    public void setConnectionState(boolean connected) {
        if (connected) {
            lblConnectionStatus.setText("● Đã kết nối");
            lblConnectionStatus.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 600; -fx-text-fill: #1B5E20; -fx-background-color: #EDF7ED; -fx-background-radius: 12px; -fx-padding: 4 10 4 10;");
        } else {
            lblConnectionStatus.setText("● Mất kết nối");
            lblConnectionStatus.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 600; -fx-text-fill: #C62828; -fx-background-color: #FDEEEE; -fx-background-radius: 12px; -fx-padding: 4 10 4 10;");
        }
    }

    public void renderCategories(List<String> categories, String selectedCategory, Consumer<String> onSelect) {
        categoryNavBox.getChildren().clear();
        for (String cat : categories) {
            boolean isSelected = cat.equalsIgnoreCase(selectedCategory);
            Button btn = new Button(getCategoryWithIcon(cat));
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setAlignment(Pos.CENTER_LEFT);

            if (isSelected) {
                btn.setStyle(
                    "-fx-background-color: #183C32; -fx-text-fill: #FFFFFF; -fx-font-weight: 700; " +
                    "-fx-font-size: 12.5px; -fx-background-radius: 8px; -fx-padding: 9 12 9 12; -fx-cursor: hand;"
                );
            } else {
                btn.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #817B70; -fx-font-weight: 600; " +
                    "-fx-font-size: 12.5px; -fx-background-radius: 8px; -fx-padding: 9 12 9 12; -fx-cursor: hand;"
                );
                btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #F0EAE0; -fx-text-fill: #292722; -fx-font-weight: 600; -fx-font-size: 12.5px; -fx-background-radius: 8px; -fx-padding: 9 12 9 12; -fx-cursor: hand;"));
                btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #817B70; -fx-font-weight: 600; -fx-font-size: 12.5px; -fx-background-radius: 8px; -fx-padding: 9 12 9 12; -fx-cursor: hand;"));
            }

            btn.setOnAction(e -> onSelect.accept(cat));
            categoryNavBox.getChildren().add(btn);
        }
    }

    private String getCategoryWithIcon(String cat) {
        switch (cat) {
            case "Tất cả": return "📋 Tất cả";
            case "Cà phê": return "☕ Cà phê";
            case "Trà & Trái cây": return "🍵 Trà trái cây";
            case "Đá xay & Sinh tố": return "🥤 Đá xay";
            case "Trà sữa": return "🧋 Trà sữa";
            case "Soda & Nước ép": return "🍹 Nước ép";
            case "Matcha & Chocolate": return "🍫 Matcha";
            case "Bánh ngọt": return "🥐 Bánh ngọt";
            case "Món ăn nhẹ": return "🥪 Món ăn nhẹ";
            default: return "🍴 " + cat;
        }
    }

    public void renderMenuItems(List<MenuItem> items, Consumer<MenuItem> onSelect) {
        renderItems(items, onSelect);
    }

    public void renderItems(List<MenuItem> items, Consumer<MenuItem> onSelect) {
        this.currentMenuItems = (items != null) ? items : new ArrayList<>();
        this.currentOnSelect = onSelect;

        emptySearchBox.setVisible(currentMenuItems.isEmpty());
        emptySearchBox.setManaged(currentMenuItems.isEmpty());
        itemsGridPane.setVisible(!currentMenuItems.isEmpty());
        itemsGridPane.setManaged(!currentMenuItems.isEmpty());

        currentCards.clear();
        for (MenuItem item : currentMenuItems) {
            currentCards.add(new ItemCard(item, onSelect));
        }

        double curW = catalogScrollPane.getViewportBounds() != null ? catalogScrollPane.getViewportBounds().getWidth() : 600;
        updateGridLayout(curW);
    }

    private void updateGridLayout(double viewportW) {
        if (currentCards.isEmpty()) {
            itemsGridPane.getChildren().clear();
            return;
        }

        double availableWidth = (viewportW > 100) ? (viewportW - 16) : 600.0;
        final double MIN_CARD_WIDTH = 195.0;
        final double GAP = 14.0;

        int cols = Math.max(1, (int) ((availableWidth + GAP) / (MIN_CARD_WIDTH + GAP)));
        // Ưu tiên 3 cột như Screen 3 nếu đủ bề rộng
        if (cols > 3) cols = 3;
        double cardWidth = Math.floor((availableWidth - GAP * (cols - 1)) / cols);

        if (cols == currentCols && Math.abs(cardWidth - currentCardWidth) < 1.0 && !itemsGridPane.getChildren().isEmpty()) {
            return;
        }

        currentCols = cols;
        currentCardWidth = cardWidth;

        itemsGridPane.getChildren().clear();
        itemsGridPane.getColumnConstraints().clear();

        for (int c = 0; c < cols; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPrefWidth(cardWidth);
            cc.setMinWidth(cardWidth);
            cc.setMaxWidth(cardWidth);
            cc.setHgrow(Priority.NEVER);
            cc.setHalignment(HPos.LEFT);
            itemsGridPane.getColumnConstraints().add(cc);
        }

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
