package cafe.views;

import cafe.models.CartModel;
import cafe.models.OrderItem;
import cafe.utils.CurrencyFormatter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.function.Consumer;

/**
 * PaymentDialog - Reusable Payment Screen Dialog.
 * Hallmark Discipline:
 * - Editorial receipt layout, subtle 4px radius, hairline borders
 * - Clear total hierarchy, 3 explicit payment options (Cash, QR, Card)
 * - Zero artificial slop gradients, high tactile contrast
 */
public class PaymentDialog extends Stage {
    private final int tableNumber;
    private final CartModel cartModel;
    private final Consumer<String> onConfirmed;

    private String selectedMethod = "TIỀN MẶT";

    public PaymentDialog(Stage parentStage, int tableNumber, CartModel cartModel, Consumer<String> onConfirmed) {
        this.tableNumber = tableNumber;
        this.cartModel = cartModel;
        this.onConfirmed = onConfirmed;

        initOwner(parentStage);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.UTILITY);
        setTitle("Thanh Toán • Brew & Bean CAFE");
        setResizable(false);

        initUI();
    }

    private void initUI() {
        VBox root = new VBox(14);
        root.getStyleClass().add("modal-canvas");
        root.setPadding(new Insets(20, 24, 20, 24));
        root.setPrefWidth(460);

        // Header: Brand & Table
        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Label lblTitle = new Label("XÁC NHẬN THANH TOÁN");
        lblTitle.getStyleClass().add("editorial-title");

        Label lblSub = new Label("Vui lòng kiểm tra lại đơn hàng và chọn hình thức thanh toán.");
        lblSub.getStyleClass().add("editorial-subtitle");

        titleBox.getChildren().addAll(lblTitle, lblSub);

        Label lblTable = new Label("BÀN " + (tableNumber < 10 ? "0" + tableNumber : tableNumber));
        lblTable.getStyleClass().add("table-tag");

        headerRow.getChildren().addAll(titleBox, lblTable);

        // Receipt Summary Box (Editorial Slip)
        VBox receiptBox = new VBox(8);
        receiptBox.getStyleClass().add("receipt-slip");

        Label lblOrderSummaryTitle = new Label("TÓM TẮT ĐƠN HÀNG (" + cartModel.getTotalQuantity() + " món)");
        lblOrderSummaryTitle.getStyleClass().add("section-label");
        receiptBox.getChildren().add(lblOrderSummaryTitle);

        VBox itemsList = new VBox(4);
        for (OrderItem oi : cartModel.getItems()) {
            HBox itemRow = new HBox(8);
            itemRow.setAlignment(Pos.CENTER_LEFT);

            Label lblItemName = new Label(oi.getQty() + "x " + oi.getName() + (oi.getSizeCode() != null ? " (" + oi.getSizeCode() + ")" : ""));
            lblItemName.setStyle("-fx-font-size: 12px; -fx-text-fill: #292722;");
            HBox.setHgrow(lblItemName, Priority.ALWAYS);

            Label lblItemSub = new Label(CurrencyFormatter.format(oi.getSubtotal()));
            lblItemSub.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #292722;");

            itemRow.getChildren().addAll(lblItemName, lblItemSub);
            itemsList.getChildren().add(itemRow);
        }

        ScrollPane scrollItems = new ScrollPane(itemsList);
        scrollItems.setFitToWidth(true);
        scrollItems.setMaxHeight(120);
        scrollItems.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        receiptBox.getChildren().add(scrollItems);

        // Divider
        Region div = new Region();
        div.setStyle("-fx-background-color: #E6DFD4; -fx-pref-height: 1px;");
        receiptBox.getChildren().add(div);

        // Subtotal & Discount rows
        HBox rowSubtotal = new HBox();
        Label lblSubLabel = new Label("Tạm tính:");
        lblSubLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #817B70;");
        Region sp1 = new Region();
        HBox.setHgrow(sp1, Priority.ALWAYS);
        Label lblSubVal = new Label(CurrencyFormatter.format(cartModel.getSubtotalAmount()));
        lblSubVal.setStyle("-fx-font-size: 12px; -fx-text-fill: #292722;");
        rowSubtotal.getChildren().addAll(lblSubLabel, sp1, lblSubVal);
        receiptBox.getChildren().add(rowSubtotal);

        if (cartModel.getDiscountAmount() > 0) {
            HBox rowDiscount = new HBox();
            Label lblDiscLabel = new Label("Giảm giá (Voucher):");
            lblDiscLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #183C32; -fx-font-weight: 700;");
            Region sp2 = new Region();
            HBox.setHgrow(sp2, Priority.ALWAYS);
            Label lblDiscVal = new Label("-" + CurrencyFormatter.format(cartModel.getDiscountAmount()));
            lblDiscVal.setStyle("-fx-font-size: 12px; -fx-text-fill: #183C32; -fx-font-weight: 700;");
            rowDiscount.getChildren().addAll(lblDiscLabel, sp2, lblDiscVal);
            receiptBox.getChildren().add(rowDiscount);
        }

        // Grand Total Row
        HBox rowGrandTotal = new HBox();
        rowGrandTotal.setAlignment(Pos.CENTER_LEFT);
        Label lblTotalLabel = new Label("TỔNG THANH TOÁN:");
        lblTotalLabel.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 800; -fx-text-fill: #292722;");
        Region sp3 = new Region();
        HBox.setHgrow(sp3, Priority.ALWAYS);
        Label lblTotalVal = new Label(CurrencyFormatter.format(cartModel.getTotalAmount()));
        lblTotalVal.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #A66A42;");
        rowGrandTotal.getChildren().addAll(lblTotalLabel, sp3, lblTotalVal);
        receiptBox.getChildren().add(rowGrandTotal);

        // Payment Method Options
        VBox paymentOptionsBox = new VBox(8);
        Label lblSelectPayment = new Label("CHỌN PHƯƠNG THỨC THANH TOÁN");
        lblSelectPayment.getStyleClass().add("section-label");
        paymentOptionsBox.getChildren().add(lblSelectPayment);

        ToggleGroup payGroup = new ToggleGroup();

        RadioButton rbCash = createPaymentOptionRadio("💵  Tiền mặt tại quầy", "Thanh toán trực tiếp khi nhận nước tại quầy thu ngân", "TIỀN MẶT", payGroup, true);
        RadioButton rbQR = createPaymentOptionRadio("📱  Chuyển khoản / Ví điện tử QR", "Quét mã MoMo, ZaloPay, VietQR tiện lợi", "VÍ ĐIỆN TỬ / QR", payGroup, false);
        RadioButton rbCard = createPaymentOptionRadio("💳  Thẻ ngân hàng / Máy POS", "Chạm hoặc quẹt thẻ ATM, Visa, MasterCard", "THẺ NGÂN HÀNG", payGroup, false);

        paymentOptionsBox.getChildren().addAll(rbCash, rbQR, rbCard);

        // Bottom Actions
        HBox actionsRow = new HBox(10);
        actionsRow.setAlignment(Pos.CENTER_RIGHT);
        actionsRow.setPadding(new Insets(8, 0, 0, 0));

        Button btnCancel = new Button("Quay lại giỏ");
        btnCancel.getStyleClass().add("btn-secondary");
        btnCancel.setOnAction(e -> close());

        Button btnConfirm = new Button("Xác nhận & Gửi đơn hàng →");
        btnConfirm.getStyleClass().add("btn-primary");
        btnConfirm.setOnAction(e -> {
            if (onConfirmed != null) {
                onConfirmed.accept(selectedMethod);
            }
            close();
        });

        actionsRow.getChildren().addAll(btnCancel, btnConfirm);

        root.getChildren().addAll(headerRow, receiptBox, paymentOptionsBox, actionsRow);

        Scene scene = new Scene(root);
        try {
            scene.getStylesheets().add(getClass().getResource("/cafe/resources/css/cafe-theme.css").toExternalForm());
        } catch (Exception ignored) {}

        setScene(scene);
    }

    private RadioButton createPaymentOptionRadio(String title, String desc, String methodKey, ToggleGroup group, boolean isSelected) {
        RadioButton rb = new RadioButton();
        rb.setToggleGroup(group);
        rb.setSelected(isSelected);
        rb.setMaxWidth(Double.MAX_VALUE);
        rb.getStyleClass().add("payment-method-card");

        VBox content = new VBox(2);
        Label lblT = new Label(title);
        lblT.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        Label lblD = new Label(desc);
        lblD.setStyle("-fx-font-size: 11px; -fx-text-fill: #817B70;");
        content.getChildren().addAll(lblT, lblD);

        rb.setGraphic(content);
        rb.setOnAction(e -> selectedMethod = methodKey);

        return rb;
    }
}
