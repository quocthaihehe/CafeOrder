package cafe.views;

import cafe.models.CartModel;
import cafe.models.OrderItem;
import cafe.models.Voucher;
import cafe.utils.AnimationHelper;
import cafe.utils.CurrencyFormatter;
import cafe.utils.SvgIcons;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * CartModalDialog - Floating Cart Modal Dialog matching Screen 5 in UI_mong_muon.png.
 * Features:
 * - 12px rounded canvas with entrance animation
 * - Dine-in / Takeaway toggle buttons
 * - List of CartItemRow items with thumbnail, size, notes, stepper [- 1 +], delete
 * - Voucher code entry with validation feedback
 * - Bill summary: Subtotal, Discount, Total
 * - Note input for entire order
 * - Two bottom actions: [Thêm món] (Close & continue ordering) and [Thanh toán →] (Proceed to payment)
 */
public class CartModalDialog extends Stage {
    private final CartModel cartModel;
    private final BiConsumer<OrderItem, Integer> onQtyChange;
    private final Consumer<OrderItem> onDelete;
    private final Consumer<String> onApplyVoucher;
    private final Runnable onProceedToPayment;

    // UI Controls
    private final VBox cartItemsContainer = new VBox(8);
    private final ScrollPane scrollItems = new ScrollPane();
    private final VBox emptyCartBox = new VBox(8);

    private final ToggleButton btnDineIn = new ToggleButton("Dùng tại bàn");
    private final ToggleButton btnTakeaway = new ToggleButton("Mang về");

    private final TextField txtVoucher = new TextField();
    private final Button btnApplyVoucher = new Button("Áp dụng");
    private final Label lblVoucherFeedback = new Label();

    private final Label lblSubtotalValue = new Label("0 đ");
    private final Label lblDiscountValue = new Label("-0 đ");
    private final HBox rowDiscount = new HBox();
    private final Label lblGrandTotalValue = new Label("0 đ");

    private final Button btnContinueOrdering = new Button("Thêm món");
    private final Button btnProceedToPayment = new Button("Thanh toán →");

    public CartModalDialog(Stage parentStage, CartModel cartModel,
                           BiConsumer<OrderItem, Integer> onQtyChange,
                           Consumer<OrderItem> onDelete,
                           Consumer<String> onApplyVoucher,
                           Runnable onProceedToPayment) {
        this.cartModel = cartModel;
        this.onQtyChange = onQtyChange;
        this.onDelete = onDelete;
        this.onApplyVoucher = onApplyVoucher;
        this.onProceedToPayment = onProceedToPayment;

        initOwner(parentStage);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.UTILITY);
        setTitle("Giỏ Hàng • Brew & Bean CAFE");
        setResizable(false);

        initUI();
    }

    private void initUI() {
        VBox root = new VBox(12);
        root.getStyleClass().add("cart-modal-pane");
        root.setPrefWidth(480);
        root.setMaxWidth(520);
        root.setPrefHeight(600);

        // Entrance animation
        AnimationHelper.playDialogEntrance(root);

        // 1. Header (Title, Item count, Close button)
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label lblTitle = new Label("ĐƠN HÀNG CỦA BẠN");
        lblTitle.getStyleClass().add("editorial-title");
        HBox.setHgrow(lblTitle, Priority.ALWAYS);

        Label lblCount = new Label(cartModel.getTotalQuantity() + " món");
        lblCount.getStyleClass().add("table-tag");

        Button btnClose = new Button();
        SVGPath closeIcon = SvgIcons.createOutlineIcon(SvgIcons.CLOSE, 0.7, Color.web("#817B70"));
        btnClose.setGraphic(closeIcon);
        btnClose.getStyleClass().add("btn-delete-item");
        btnClose.setOnAction(e -> close());

        header.getChildren().addAll(lblTitle, lblCount, btnClose);

        // 2. Order Type Toggle (Dine-in / Takeaway)
        HBox typeRow = new HBox(8);
        typeRow.setAlignment(Pos.CENTER);

        ToggleGroup typeGroup = new ToggleGroup();
        btnDineIn.setToggleGroup(typeGroup);
        btnTakeaway.setToggleGroup(typeGroup);

        if ("TAKEAWAY".equalsIgnoreCase(cartModel.getOrderType())) {
            btnTakeaway.setSelected(true);
        } else {
            btnDineIn.setSelected(true);
        }

        btnDineIn.getStyleClass().add("btn-option-tag");
        btnTakeaway.getStyleClass().add("btn-option-tag");
        btnDineIn.setMaxWidth(Double.MAX_VALUE);
        btnTakeaway.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnDineIn, Priority.ALWAYS);
        HBox.setHgrow(btnTakeaway, Priority.ALWAYS);

        typeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == btnTakeaway) {
                cartModel.setOrderType("TAKEAWAY");
            } else {
                cartModel.setOrderType("DINE_IN");
            }
        });

        typeRow.getChildren().addAll(btnDineIn, btnTakeaway);

        // 3. Cart Items Scroll List
        cartItemsContainer.setSpacing(6);
        cartItemsContainer.setPadding(new Insets(2, 2, 8, 2));

        scrollItems.setContent(cartItemsContainer);
        scrollItems.setFitToWidth(true);
        scrollItems.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollItems.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollItems, Priority.ALWAYS);

        // Empty cart placeholder
        emptyCartBox.setAlignment(Pos.CENTER);
        emptyCartBox.setPadding(new Insets(60, 20, 60, 20));
        SVGPath emptyCartIcon = SvgIcons.createOutlineIcon(SvgIcons.SHOPPING_BAG, 1.8, Color.web("#D8D1C3"));
        Label emptyTitle = new Label("Giỏ hàng đang trống");
        emptyTitle.getStyleClass().add("editorial-title");
        Label emptySub = new Label("Vui lòng chọn món ngon từ thực đơn.");
        emptySub.getStyleClass().add("editorial-subtitle");
        emptyCartBox.getChildren().addAll(emptyCartIcon, emptyTitle, emptySub);

        // 4. Voucher Row
        VBox voucherSection = new VBox(4);
        HBox voucherRow = new HBox(8);
        voucherRow.setAlignment(Pos.CENTER_LEFT);

        txtVoucher.setPromptText("Nhập mã ưu đãi (VD: BANMOI, CAFE10)...");
        txtVoucher.getStyleClass().add("text-input-field");
        HBox.setHgrow(txtVoucher, Priority.ALWAYS);

        btnApplyVoucher.getStyleClass().add("btn-secondary");
        AnimationHelper.applyButtonPress(btnApplyVoucher);
        btnApplyVoucher.setOnAction(e -> {
            if (onApplyVoucher != null) {
                onApplyVoucher.accept(txtVoucher.getText().trim());
            }
        });

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
        lblSub.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #817B70;");
        Region s1 = new Region();
        HBox.setHgrow(s1, Priority.ALWAYS);
        lblSubtotalValue.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #292722; -fx-font-weight: 600;");
        rowSubtotal.getChildren().addAll(lblSub, s1, lblSubtotalValue);

        Label lblDiscLabel = new Label("Giảm giá (Voucher):");
        lblDiscLabel.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #183C32; -fx-font-weight: 700;");
        Region s2 = new Region();
        HBox.setHgrow(s2, Priority.ALWAYS);
        lblDiscountValue.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #183C32; -fx-font-weight: 700;");
        rowDiscount.getChildren().addAll(lblDiscLabel, s2, lblDiscountValue);
        rowDiscount.setVisible(false);
        rowDiscount.setManaged(false);

        HBox rowTotal = new HBox();
        rowTotal.setAlignment(Pos.CENTER_LEFT);
        rowTotal.setPadding(new Insets(4, 0, 0, 0));
        Label lblTot = new Label("TỔNG THANH TOÁN:");
        lblTot.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #292722;");
        Region s3 = new Region();
        HBox.setHgrow(s3, Priority.ALWAYS);
        lblGrandTotalValue.setStyle("-fx-font-size: 19px; -fx-font-weight: 800; -fx-text-fill: #A66A42;");
        rowTotal.getChildren().addAll(lblTot, s3, lblGrandTotalValue);

        summaryBox.getChildren().addAll(rowSubtotal, rowDiscount, rowTotal);

        // 6. Two Bottom Actions: [Thêm món] and [Thanh toán →]
        HBox bottomActions = new HBox(12);
        bottomActions.setAlignment(Pos.CENTER);
        bottomActions.setPadding(new Insets(6, 0, 0, 0));

        btnContinueOrdering.getStyleClass().add("btn-secondary");
        btnContinueOrdering.setPrefHeight(42);
        btnContinueOrdering.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnContinueOrdering, Priority.ALWAYS);
        AnimationHelper.applyButtonPress(btnContinueOrdering);
        btnContinueOrdering.setOnAction(e -> close());

        btnProceedToPayment.getStyleClass().add("btn-primary");
        btnProceedToPayment.setPrefHeight(42);
        btnProceedToPayment.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnProceedToPayment, Priority.ALWAYS);
        AnimationHelper.applyButtonPress(btnProceedToPayment);
        btnProceedToPayment.setOnAction(e -> {
            close();
            if (onProceedToPayment != null) {
                onProceedToPayment.run();
            }
        });

        bottomActions.getChildren().addAll(btnContinueOrdering, btnProceedToPayment);

        root.getChildren().addAll(header, typeRow, scrollItems, voucherSection, summaryBox, bottomActions);

        Scene scene = new Scene(root);
        try {
            scene.getStylesheets().add(getClass().getResource("/cafe/resources/css/cafe-theme.css").toExternalForm());
        } catch (Exception ignored) {}

        setScene(scene);
        refreshView();
    }

    public void refreshView() {
        cartItemsContainer.getChildren().clear();

        if (cartModel.isEmpty()) {
            scrollItems.setContent(emptyCartBox);
            lblSubtotalValue.setText("0 đ");
            lblGrandTotalValue.setText("0 đ");
            rowDiscount.setVisible(false);
            rowDiscount.setManaged(false);
            btnProceedToPayment.setDisable(true);
            return;
        }

        btnProceedToPayment.setDisable(false);
        scrollItems.setContent(cartItemsContainer);

        for (OrderItem oi : cartModel.getItems()) {
            CartItemRow row = new CartItemRow(oi, (item, delta) -> {
                if (onQtyChange != null) {
                    onQtyChange.accept(item, delta);
                    refreshView();
                }
            }, item -> {
                if (onDelete != null) {
                    onDelete.accept(item);
                    refreshView();
                }
            });
            cartItemsContainer.getChildren().add(row);
        }

        lblSubtotalValue.setText(CurrencyFormatter.format(cartModel.getSubtotalAmount()));
        if (cartModel.getDiscountAmount() > 0) {
            lblDiscountValue.setText("-" + CurrencyFormatter.format(cartModel.getDiscountAmount()));
            rowDiscount.setVisible(true);
            rowDiscount.setManaged(true);
        } else {
            rowDiscount.setVisible(false);
            rowDiscount.setManaged(false);
        }
        lblGrandTotalValue.setText(CurrencyFormatter.format(cartModel.getTotalAmount()));
    }

    public void setVoucherFeedback(String text, boolean isSuccess) {
        lblVoucherFeedback.setText(text);
        if (isSuccess) {
            lblVoucherFeedback.setStyle("-fx-text-fill: #183C32; -fx-font-size: 11px; -fx-font-weight: 700;");
        } else {
            lblVoucherFeedback.setStyle("-fx-text-fill: #C62828; -fx-font-size: 11px; -fx-font-weight: 700;");
        }
        lblVoucherFeedback.setVisible(true);
        lblVoucherFeedback.setManaged(true);
    }
}
