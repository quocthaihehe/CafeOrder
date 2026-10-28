package cafe.views;

import cafe.models.MenuItem;
import cafe.models.MenuRepository;
import cafe.models.OrderItem;
import cafe.models.ToppingItem;
import cafe.utils.AnimationHelper;
import cafe.utils.CurrencyFormatter;
import cafe.utils.SvgIcons;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * OrderSuccessDialog - Phiếu Thanh Toán tối giản, đồng nhất chữ và màu sắc:
 * - Duy nhất tên quán "Brew & Bean" dùng font Playfair Display.
 * - Toàn bộ chữ tiêu đề / từ quan trọng: thống nhất 1 size (13px), In đậm (Bold), 1 màu (#112823).
 * - Toàn bộ chữ nội dung / chi tiết phụ: thống nhất 1 size (12px), Không in đậm (Regular), 1 màu (#4A453E).
 * - Đã lược bỏ các dòng rườm rà (Barcode, Thu ngân, Badge trạng thái trùng lặp...) để phiếu thoáng và dễ đọc.
 */
public class OrderSuccessDialog extends Stage {

    // Quy chuẩn 2 kiểu chữ duy nhất trên toàn bộ phiếu (ngoài tên quán)
    private static final String STYLE_BOLD = "-fx-font-family: 'Segoe UI', 'Inter', -apple-system, sans-serif; -fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #112823;";
    private static final String STYLE_REGULAR = "-fx-font-family: 'Segoe UI', 'Inter', -apple-system, sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 500; -fx-text-fill: #4A453E;";

    public static class ReceiptData {
        public final int orderId;
        public final int tableNumber;
        public final LocalDateTime orderTime;
        public final List<OrderItem> items;
        public final int totalQuantity;
        public final double subtotal;
        public final double discountAmount;
        public final String voucherCode;
        public final double finalTotal;
        public final String paymentMethod;
        public final String paymentStatus;
        public final String cashierName;

        public ReceiptData(int orderId, int tableNumber, LocalDateTime orderTime,
                           List<OrderItem> items, int totalQuantity,
                           double subtotal, double discountAmount, String voucherCode,
                           double finalTotal, String paymentMethod,
                           String paymentStatus, String cashierName) {
            this.orderId = orderId;
            this.tableNumber = tableNumber;
            this.orderTime = orderTime != null ? orderTime : LocalDateTime.now();
            this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
            this.totalQuantity = totalQuantity;
            this.subtotal = subtotal;
            this.discountAmount = discountAmount;
            this.voucherCode = voucherCode;
            this.finalTotal = finalTotal;
            this.paymentMethod = paymentMethod != null ? paymentMethod : "TIỀN MẶT";
            this.paymentStatus = paymentStatus != null ? paymentStatus : "ĐÃ THANH TOÁN";
            this.cashierName = cashierName != null ? cashierName : "Kiosk #01";
        }
    }

    private final ReceiptData receipt;
    private final Runnable onDismiss;
    private final Runnable onTrackOrder;
    private final Runnable onFeedback;

    public OrderSuccessDialog(Stage parentStage, int orderId, int tableNumber, double totalAmount, String paymentMethod, Runnable onDismiss) {
        this(
            parentStage,
            new ReceiptData(
                orderId,
                tableNumber,
                LocalDateTime.now(),
                new ArrayList<>(),
                1,
                totalAmount,
                0.0,
                null,
                totalAmount,
                paymentMethod,
                "ĐÃ THANH TOÁN",
                "Kiosk #01"
            ),
            onDismiss,
            onDismiss
        );
    }

    public static void showReceiptForOrder(Stage parentStage, cafe.models.Order order, String cashierName) {
        if (order == null) return;
        java.time.LocalDateTime dt = java.time.Instant.ofEpochMilli(order.getTimestamp())
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDateTime();
        String statusLabel = "PAID".equalsIgnoreCase(order.getStatus()) || "DONE".equalsIgnoreCase(order.getStatus())
            ? "ĐÃ THANH TOÁN" : "PHIẾU TẠM TÍNH";
        ReceiptData data = new ReceiptData(
            order.getOrderId(),
            order.getTable(),
            dt,
            order.getItems(),
            order.getTotalQuantity(),
            order.getSubtotalAmount(),
            order.getDiscountAmount(),
            order.getVoucherCode(),
            order.getTotalAmount(),
            "TIỀN MẶT / QR",
            statusLabel,
            cashierName != null ? cashierName : "Thu ngân POS"
        );
        OrderSuccessDialog dlg = new OrderSuccessDialog(parentStage, data, null, null);
        dlg.show();
    }

    public OrderSuccessDialog(Stage parentStage, ReceiptData receipt, Runnable onDismiss, Runnable onTrackOrder) {
        this(parentStage, receipt, onDismiss, onTrackOrder, null);
    }

    public OrderSuccessDialog(Stage parentStage, ReceiptData receipt, Runnable onDismiss, Runnable onTrackOrder, Runnable onFeedback) {
        this.receipt = receipt;
        this.onDismiss = onDismiss;
        this.onTrackOrder = onTrackOrder != null ? onTrackOrder : onDismiss;
        this.onFeedback = onFeedback;

        if (parentStage != null) {
            initOwner(parentStage);
        }
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.TRANSPARENT);
        setTitle("Phiếu Thanh Toán #" + receipt.orderId);
        setResizable(false);

        initUI();
    }

    private void initUI() {
        VBox root = new VBox(12);
        root.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-background-radius: 16px; " +
            "-fx-border-color: #E2DDD5; " +
            "-fx-border-radius: 16px; " +
            "-fx-border-width: 1px; " +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.20), 26, 0.15, 0, 8);"
        );
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(16, 22, 18, 22));
        root.setPrefWidth(450);
        root.setMaxWidth(450);

        AnimationHelper.playDialogEntrance(root);

        // =====================================================================
        // 1. HEADER XÁC NHẬN GỌN GÀNG
        // =====================================================================
        VBox topHeaderBox = new VBox(6);
        topHeaderBox.setAlignment(Pos.CENTER);
        topHeaderBox.setStyle("-fx-cursor: move;");
        cafe.utils.WindowChromeHelper.makeDraggable(this, topHeaderBox);

        HBox closeBar = new HBox();
        closeBar.setAlignment(Pos.CENTER_RIGHT);
        Button btnClose = new Button();
        btnClose.setGraphic(cafe.utils.IconHelper.getIcon("client", "dong-cua-so.png", SvgIcons.CLOSE, "✕", 12, Color.web("#112823")));
        btnClose.setStyle("-fx-background-color: #F5EFEB; -fx-background-radius: 14px; -fx-min-width: 28px; -fx-min-height: 28px; -fx-pref-width: 28px; -fx-pref-height: 28px; -fx-padding: 0; -fx-cursor: hand; -fx-alignment: center;");
        btnClose.setOnMouseEntered(e -> btnClose.setStyle("-fx-background-color: #EAE4DA; -fx-background-radius: 14px; -fx-min-width: 28px; -fx-min-height: 28px; -fx-pref-width: 28px; -fx-pref-height: 28px; -fx-padding: 0; -fx-cursor: hand; -fx-alignment: center;"));
        btnClose.setOnMouseExited(e -> btnClose.setStyle("-fx-background-color: #F5EFEB; -fx-background-radius: 14px; -fx-min-width: 28px; -fx-min-height: 28px; -fx-pref-width: 28px; -fx-pref-height: 28px; -fx-padding: 0; -fx-cursor: hand; -fx-alignment: center;"));
        btnClose.setOnMousePressed(javafx.event.Event::consume);
        btnClose.setOnAction(e -> {
            close();
            if (onDismiss != null) onDismiss.run();
        });
        closeBar.getChildren().add(btnClose);

        StackPane iconCircle = new StackPane();
        iconCircle.setStyle("-fx-background-color: #EDF7ED; -fx-background-radius: 20px; -fx-min-width: 40px; -fx-min-height: 40px; -fx-max-width: 40px; -fx-max-height: 40px;");
        SVGPath checkIcon = SvgIcons.createOutlineIcon(SvgIcons.CHECK, 1.0, Color.web("#112823"));
        iconCircle.getChildren().add(checkIcon);
        VBox.setMargin(iconCircle, new Insets(-12, 0, 0, 0));

        boolean isPostpaid = receipt.paymentStatus != null && receipt.paymentStatus.toUpperCase().contains("TRẢ SAU");
        Label lblSuccessTitle = new Label(isPostpaid ? "GỌI MÓN THÀNH CÔNG (TRẢ SAU)" : "THANH TOÁN THÀNH CÔNG");
        lblSuccessTitle.setStyle(STYLE_BOLD);

        topHeaderBox.getChildren().addAll(closeBar, iconCircle, lblSuccessTitle);

        // =====================================================================
        // 2. KHUNG PHIẾU THU (RECEIPT CANVAS - NỀN GIẤY NGÀ #FBF9F5)
        // =====================================================================
        VBox receiptCanvas = new VBox(10);
        receiptCanvas.setAlignment(Pos.TOP_CENTER);
        receiptCanvas.setPadding(new Insets(16, 18, 16, 18));
        receiptCanvas.setStyle(
            "-fx-background-color: #FBF9F5; " +
            "-fx-background-radius: 12px; " +
            "-fx-border-color: #E2DDD5; " +
            "-fx-border-radius: 12px; " +
            "-fx-border-width: 1px;"
        );

        // --- TÊN QUÁN (Font cố định Playfair Display) & ĐỊA CHỈ ---
        VBox brandHeader = new VBox(3);
        brandHeader.setAlignment(Pos.CENTER);

        Label lblBrandName = new Label("Brew & Bean");
        lblBrandName.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        Label lblAddress = new Label("123 Nguyễn Thị Minh Khai, Quận 3, TP.HCM");
        lblAddress.setStyle(STYLE_REGULAR);

        brandHeader.getChildren().addAll(lblBrandName, lblAddress);

        // --- THÔNG TIN ĐƠN HÀNG (2 DÒNG GỌN GÀNG) ---
        VBox metaSection = new VBox(4);

        HBox metaRow1 = new HBox();
        metaRow1.setAlignment(Pos.CENTER_LEFT);
        Label lblOrderNo = new Label("Mã đơn: #" + receipt.orderId);
        lblOrderNo.setStyle(STYLE_BOLD);
        Region spMeta1 = new Region();
        HBox.setHgrow(spMeta1, Priority.ALWAYS);
        String tableFormatted = "Bàn " + (receipt.tableNumber < 10 ? "0" + receipt.tableNumber : receipt.tableNumber);
        Label lblTable = new Label(tableFormatted);
        lblTable.setStyle(STYLE_BOLD);
        metaRow1.getChildren().addAll(lblOrderNo, spMeta1, lblTable);

        HBox metaRow2 = new HBox();
        metaRow2.setAlignment(Pos.CENTER_LEFT);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        Label lblOrderTime = new Label("Ngày: " + receipt.orderTime.format(dtf));
        lblOrderTime.setStyle(STYLE_REGULAR);
        Region spMeta2 = new Region();
        HBox.setHgrow(spMeta2, Priority.ALWAYS);
        Label lblMethod = new Label("TT: " + formatPaymentMethodDisplay(receipt.paymentMethod));
        lblMethod.setStyle(STYLE_REGULAR);
        metaRow2.getChildren().addAll(lblOrderTime, spMeta2, lblMethod);

        metaSection.getChildren().addAll(metaRow1, metaRow2);

        // --- BẢNG KÊ CHI TIẾT MÓN ---
        HBox tableHeader = new HBox(8);
        tableHeader.setAlignment(Pos.CENTER_LEFT);

        Label lblColName = new Label("TÊN MÓN");
        lblColName.setStyle(STYLE_BOLD);
        HBox.setHgrow(lblColName, Priority.ALWAYS);
        lblColName.setMaxWidth(Double.MAX_VALUE);

        Label lblColQty = new Label("SL");
        lblColQty.setStyle(STYLE_BOLD + " -fx-min-width: 32px; -fx-alignment: center;");

        Label lblColTotal = new Label("THÀNH TIỀN");
        lblColTotal.setStyle(STYLE_BOLD + " -fx-min-width: 86px; -fx-alignment: center-right;");

        tableHeader.getChildren().addAll(lblColName, lblColQty, lblColTotal);

        VBox itemsListContainer = new VBox(8);
        itemsListContainer.setPadding(new Insets(2, 2, 2, 0));

        if (receipt.items != null && !receipt.items.isEmpty()) {
            for (OrderItem item : receipt.items) {
                itemsListContainer.getChildren().add(buildReceiptItemBlock(item));
            }
        } else {
            HBox fallbackRow = new HBox(8);
            fallbackRow.setAlignment(Pos.CENTER_LEFT);
            Label fName = new Label("Đơn hàng tại bàn");
            fName.setStyle(STYLE_BOLD);
            HBox.setHgrow(fName, Priority.ALWAYS);
            Label fQty = new Label(String.valueOf(Math.max(1, receipt.totalQuantity)));
            fQty.setStyle(STYLE_BOLD + " -fx-min-width: 32px; -fx-alignment: center;");
            Label fPrice = new Label(CurrencyFormatter.format(receipt.finalTotal));
            fPrice.setStyle(STYLE_BOLD + " -fx-min-width: 86px; -fx-alignment: center-right;");
            fallbackRow.getChildren().addAll(fName, fQty, fPrice);
            itemsListContainer.getChildren().add(fallbackRow);
        }

        ScrollPane itemsScroll = new ScrollPane(itemsListContainer);
        itemsScroll.setFitToWidth(true);
        itemsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        itemsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        itemsScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 0;");
        itemsScroll.setMinHeight(70);
        itemsScroll.setMaxHeight(200);

        // --- TỔNG KẾT TÀI CHÍNH (CHỌN LỌC, GỌN GÀNG) ---
        VBox financialBox = new VBox(6);

        int countItems = receipt.totalQuantity > 0 ? receipt.totalQuantity : (receipt.items != null ? receipt.items.size() : 1);
        double subVal = receipt.subtotal > 0 ? receipt.subtotal : receipt.finalTotal;

        // Chỉ hiện dòng Tạm tính & Khuyến mãi khi có áp dụng giảm giá, hoặc hiện Tạm tính gọn
        HBox rowSubtotal = createRow("Tạm tính (" + countItems + " món):", CurrencyFormatter.format(subVal), false);
        financialBox.getChildren().add(rowSubtotal);

        if (receipt.discountAmount > 0) {
            String voucherLabel = (receipt.voucherCode != null && !receipt.voucherCode.isEmpty())
                ? "Khuyến mãi (" + receipt.voucherCode + "):"
                : "Khuyến mãi:";
            HBox rowDiscount = createRow(voucherLabel, "- " + CurrencyFormatter.format(receipt.discountAmount), false);
            financialBox.getChildren().add(rowDiscount);
        }

        HBox rowGrandTotal = createRow("TỔNG THANH TOÁN:", CurrencyFormatter.format(receipt.finalTotal), true);
        financialBox.getChildren().add(rowGrandTotal);

        // --- CHÂN PHIẾU (WIFI & LỜI CẢM ƠN NGẮN GỌN) ---
        VBox footerBox = new VBox(3);
        footerBox.setAlignment(Pos.CENTER);

        Label lblWifi = new Label("Wi-Fi: BrewBean_Guest  •  Mật khẩu: 88888888");
        lblWifi.setStyle(STYLE_REGULAR);

        Label lblThankYou = new Label("Cảm ơn quý khách và hẹn gặp lại!");
        lblThankYou.setStyle(STYLE_REGULAR);

        footerBox.getChildren().addAll(lblWifi, lblThankYou);

        receiptCanvas.getChildren().addAll(
            brandHeader,
            createDashedDivider(),
            metaSection,
            createDashedDivider(),
            tableHeader,
            itemsScroll,
            createDashedDivider(),
            financialBox,
            createDashedDivider(),
            footerBox
        );

        // =====================================================================
        // 3. BỘ ĐÔI NÚT HÀNH ĐỘNG
        // =====================================================================
        HBox actionButtonsRow = new HBox(10);
        actionButtonsRow.setAlignment(Pos.CENTER);
        actionButtonsRow.setPadding(new Insets(2, 0, 0, 0));

        Button btnTrack = new Button("Theo dõi tiến độ đơn");
        btnTrack.setPrefHeight(40);
        btnTrack.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnTrack, Priority.ALWAYS);
        btnTrack.setStyle(
            "-fx-background-color: #112823; " +
            "-fx-background-radius: 8px; " +
            "-fx-text-fill: #FFFFFF; " +
            "-fx-font-family: 'Inter', sans-serif; " +
            "-fx-font-size: 13px; " +
            "-fx-font-weight: 700; " +
            "-fx-cursor: hand;"
        );
        AnimationHelper.applyButtonPress(btnTrack);
        btnTrack.setOnAction(e -> {
            close();
            if (onTrackOrder != null) {
                onTrackOrder.run();
            }
        });

        Button btnBackMenu = new Button("Quay lại Thực đơn");
        btnBackMenu.setPrefHeight(40);
        btnBackMenu.setMinWidth(155);
        btnBackMenu.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-border-color: #DED7CD; " +
            "-fx-border-radius: 8px; " +
            "-fx-background-radius: 8px; " +
            "-fx-border-width: 1px; " +
            "-fx-text-fill: #112823; " +
            "-fx-font-family: 'Inter', sans-serif; " +
            "-fx-font-size: 13px; " +
            "-fx-font-weight: 700; " +
            "-fx-cursor: hand;"
        );
        AnimationHelper.applyButtonPress(btnBackMenu);
        btnBackMenu.setOnAction(e -> {
            close();
            if (onDismiss != null) {
                onDismiss.run();
            }
        });

        Button btnFeedback = new Button("⭐ Đánh giá");
        btnFeedback.setPrefHeight(40);
        btnFeedback.setStyle(
            "-fx-background-color: #FEF3C7; " +
            "-fx-text-fill: #92400E; " +
            "-fx-font-family: 'Inter', sans-serif; " +
            "-fx-font-size: 13px; " +
            "-fx-font-weight: 700; " +
            "-fx-background-radius: 8px; " +
            "-fx-cursor: hand; " +
            "-fx-border-color: #FCD34D; " +
            "-fx-border-radius: 8px;"
        );
        AnimationHelper.applyButtonPress(btnFeedback);
        btnFeedback.setOnAction(e -> {
            close();
            if (onFeedback != null) onFeedback.run();
        });

        if (onFeedback != null) {
            actionButtonsRow.getChildren().addAll(btnTrack, btnFeedback, btnBackMenu);
        } else {
            actionButtonsRow.getChildren().addAll(btnTrack, btnBackMenu);
        }

        root.getChildren().addAll(topHeaderBox, receiptCanvas, actionButtonsRow);

        StackPane sceneRoot = new StackPane(root);
        sceneRoot.setStyle("-fx-background-color: transparent;");
        sceneRoot.setPadding(new Insets(14));

        Scene scene = new Scene(sceneRoot);
        scene.setFill(Color.TRANSPARENT);
        cafe.utils.WindowChromeHelper.applyTheme(scene);

        setScene(scene);
    }

    /**
     * Hiển thị từng món:
     * - Dòng chính (Bold 13px): Tên món | SL | Thành tiền món (đã gồm size + topping)
     * - Dòng phụ (Regular 12px): Tùy chọn Size / Đường / Đá và Topping gộp gọn gàng
     */
    private VBox buildReceiptItemBlock(OrderItem item) {
        VBox itemBlock = new VBox(2);

        int qty = Math.max(1, item.getQty());
        double lineSubtotal = item.getSubtotal();

        // 1. Dòng chính (In đậm 13px)
        HBox mainLine = new HBox(8);
        mainLine.setAlignment(Pos.CENTER_LEFT);

        Label lblName = new Label(item.getName());
        lblName.setStyle(STYLE_BOLD);
        lblName.setWrapText(true);
        HBox.setHgrow(lblName, Priority.ALWAYS);
        lblName.setMaxWidth(Double.MAX_VALUE);

        Label lblQty = new Label(String.valueOf(qty));
        lblQty.setStyle(STYLE_BOLD + " -fx-min-width: 32px; -fx-alignment: center;");

        Label lblLineTotal = new Label(CurrencyFormatter.format(lineSubtotal));
        lblLineTotal.setStyle(STYLE_BOLD + " -fx-min-width: 86px; -fx-alignment: center-right;");

        mainLine.getChildren().addAll(lblName, lblQty, lblLineTotal);
        itemBlock.getChildren().add(mainLine);

        // Kiểm tra phân loại món
        MenuItem refMenu = item.getProductId() > 0
            ? MenuRepository.findById(item.getProductId())
            : MenuRepository.findByName(item.getName());

        boolean isDrink = true;
        boolean allowSize = true;
        if (refMenu != null) {
            allowSize = refMenu.isAllowSize();
            String cat = refMenu.getCategory() != null ? refMenu.getCategory() : "";
            isDrink = !cat.equalsIgnoreCase("Bánh ngọt") && !cat.equalsIgnoreCase("Món ăn nhẹ") && !cat.equalsIgnoreCase("Đồ ăn vặt");
        }

        // 2. Dòng phụ 1: Gộp gọn Size + Mức đường + Lượng đá trên 1 dòng duy nhất (Regular 12px)
        List<String> optionSpecs = new ArrayList<>();
        if (allowSize && item.getSizeCode() != null && !item.getSizeCode().isEmpty()) {
            double sizeDelta = item.getSizeDelta();
            if (sizeDelta > 0) {
                optionSpecs.add("Size " + item.getSizeCode() + " (+" + CurrencyFormatter.format(sizeDelta) + ")");
            } else if (sizeDelta < 0) {
                optionSpecs.add("Size " + item.getSizeCode() + " (" + CurrencyFormatter.format(sizeDelta) + ")");
            } else {
                optionSpecs.add("Size " + item.getSizeCode());
            }
        }
        if (isDrink) {
            String sugarStr = item.getSugarPercent() == 0 ? "Không đường" : (item.getSugarPercent() + "% đường");
            String iceStr = "HOT".equalsIgnoreCase(item.getTemperature())
                ? "Dùng nóng"
                : (item.getIcePercent() == 0 ? "Không đá" : (item.getIcePercent() + "% đá"));
            optionSpecs.add(sugarStr);
            optionSpecs.add(iceStr);
        }
        if (item.getNote() != null && !item.getNote().trim().isEmpty()) {
            optionSpecs.add(item.getNote().trim());
        }

        if (!optionSpecs.isEmpty()) {
            Label lblSpecs = new Label("• " + String.join("  |  ", optionSpecs));
            lblSpecs.setStyle(STYLE_REGULAR + " -fx-padding: 0 0 0 10;");
            lblSpecs.setWrapText(true);
            itemBlock.getChildren().add(lblSpecs);
        }

        // 3. Dòng phụ 2: Danh sách Topping (nếu khách có chọn thêm)
        if (item.getToppings() != null && !item.getToppings().isEmpty()) {
            List<String> toppingTexts = new ArrayList<>();
            for (ToppingItem top : item.getToppings()) {
                toppingTexts.add(top.getName() + " (+" + CurrencyFormatter.format(top.getPrice()) + ")");
            }
            Label lblToppings = new Label("+ Topping: " + String.join(", ", toppingTexts));
            lblToppings.setStyle(STYLE_REGULAR + " -fx-padding: 0 0 0 10;");
            lblToppings.setWrapText(true);
            itemBlock.getChildren().add(lblToppings);
        }

        return itemBlock;
    }

    private HBox createRow(String leftText, String rightText, boolean isImportant) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        String style = isImportant ? STYLE_BOLD : STYLE_REGULAR;

        Label lblLeft = new Label(leftText);
        lblLeft.setStyle(style);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label lblRight = new Label(rightText);
        lblRight.setStyle(style);

        row.getChildren().addAll(lblLeft, sp, lblRight);
        return row;
    }

    private Region createDashedDivider() {
        Region divider = new Region();
        divider.setMinHeight(1);
        divider.setPrefHeight(1);
        divider.setMaxWidth(Double.MAX_VALUE);
        divider.setStyle(
            "-fx-border-style: dashed; " +
            "-fx-border-color: #D6CEBF transparent transparent transparent; " +
            "-fx-border-width: 1px 0 0 0;"
        );
        return divider;
    }

    private String formatPaymentMethodDisplay(String raw) {
        if (raw == null) return "Tiền mặt";
        String upper = raw.trim().toUpperCase();
        if (upper.contains("VIETQR") || upper.contains("QR")) return "VietQR";
        if (upper.contains("VÍ") || upper.contains("THẺ") || upper.contains("MOMO")) return "Ví điện tử";
        if (upper.contains("TIỀN MẶT")) return "Tiền mặt";
        return raw;
    }
}
