package cafe.views;

import cafe.models.CartModel;
import cafe.models.OrderItem;
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
 * CartModalDialog - Floating Cart Modal Dialog matching Screen 5 in UI_mong_muon.png:
 * - Header: Tiêu đề "Giỏ hàng", Badge "Bàn 05", Nút đóng ✕
 * - Danh sách món: Thumbnail, Tên món, Giá, Stepper [- 1 +], Nút xóa
 * - Ghi chú đơn hàng: Ô nhập "Ví dụ: Không đá, ít đường,..."
 * - Tổng cộng: "Tổng cộng (2 món)" ... "110.000đ"
 * - 2 Nút hành động đáy: [Thêm món] và [Thanh toán →]
 */
public class CartModalDialog extends Stage {
    private final CartModel cartModel;
    private final BiConsumer<OrderItem, Integer> onQtyChange;
    private final Consumer<OrderItem> onDelete;
    private final Consumer<String> onApplyVoucher;
    private final Runnable onProceedToPayment;

    // UI Controls
    private final VBox cartItemsContainer = new VBox(6);
    private final ScrollPane scrollItems = new ScrollPane();
    private final VBox emptyCartBox = new VBox(8);

    private final TextField txtOrderNote = new TextField();
    private final TextField txtVoucher = new TextField();
    private final Button btnApplyVoucher = new Button("Áp dụng");
    private final Label lblVoucherFeedback = new Label();

    private final Label lblSummaryCount = new Label("Tổng cộng (0 món)");
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
        root.setStyle("-fx-background-color: #FBF9F5; -fx-background-radius: 6px; -fx-border-color: #DED7CD; -fx-border-radius: 6px; -fx-border-width: 1px;");
        root.setPadding(new Insets(18, 20, 18, 20));
        root.setPrefWidth(380);
        root.setMaxWidth(420);
        root.setPrefHeight(530);

        // Entrance animation
        AnimationHelper.playDialogEntrance(root);

        // 1. Header (Title "Giỏ hàng", Badge "Bàn 05", Close button ✕)
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label lblTitle = new Label("Giỏ hàng");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #292722;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label lblTableBadge = new Label("Bàn 05");
        lblTableBadge.setStyle("-fx-background-color: #EBE5DB; -fx-text-fill: #563521; -fx-font-size: 10.5px; -fx-font-weight: 700; -fx-background-radius: 4px; -fx-padding: 3 8 3 8;");

        Button btnClose = new Button("✕");
        btnClose.setStyle("-fx-background-color: transparent; -fx-text-fill: #817A70; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 0 0 0 6;");
        btnClose.setOnAction(e -> close());

        header.getChildren().addAll(lblTitle, sp, lblTableBadge, btnClose);

        // 2. Cart Items Scroll List
        cartItemsContainer.setSpacing(6);
        cartItemsContainer.setPadding(new Insets(2, 2, 6, 2));

        scrollItems.setContent(cartItemsContainer);
        scrollItems.setFitToWidth(true);
        scrollItems.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollItems.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollItems.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollItems, Priority.ALWAYS);

        // Empty cart placeholder
        emptyCartBox.setAlignment(Pos.CENTER);
        emptyCartBox.setPadding(new Insets(40, 20, 40, 20));
        SVGPath emptyCartIcon = SvgIcons.createOutlineIcon(SvgIcons.SHOPPING_BAG, 1.6, Color.web("#DED7CD"));
        Label emptyTitle = new Label("Giỏ hàng đang trống");
        emptyTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        Label emptySub = new Label("Vui lòng chọn món ngon từ thực đơn.");
        emptySub.setStyle("-fx-font-size: 11px; -fx-text-fill: #817A70;");
        emptyCartBox.getChildren().addAll(emptyCartIcon, emptyTitle, emptySub);

        // 3. Bill Summary Row
        HBox rowSummary = new HBox();
        rowSummary.setAlignment(Pos.CENTER_LEFT);
        rowSummary.setPadding(new Insets(8, 0, 4, 0));
        rowSummary.setStyle("-fx-border-color: #E6DFD4 transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        lblSummaryCount.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        Region spSum = new Region();
        HBox.setHgrow(spSum, Priority.ALWAYS);
        lblGrandTotalValue.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #292722;");

        rowSummary.getChildren().addAll(lblSummaryCount, spSum, lblGrandTotalValue);

        // 4. Order Note Input
        VBox noteBox = new VBox(4);
        Label lblNote = new Label("Ghi chú đơn hàng");
        lblNote.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 600; -fx-text-fill: #292722;");

        txtOrderNote.setPromptText("Ví dụ: Không đá, ít đường,...");
        txtOrderNote.setStyle("-fx-background-color: #FBF9F5; -fx-border-color: #DED7CD; -fx-border-radius: 5px; -fx-background-radius: 5px; -fx-font-size: 11px; -fx-padding: 6 10 6 10;");

        noteBox.getChildren().addAll(lblNote, txtOrderNote);

        // 5. Two Bottom Actions: [Thêm món] and [Thanh toán →]
        HBox bottomActions = new HBox(10);
        bottomActions.setAlignment(Pos.CENTER);
        bottomActions.setPadding(new Insets(6, 0, 0, 0));

        btnContinueOrdering.setStyle("-fx-background-color: #FBF9F5; -fx-text-fill: #292722; -fx-font-size: 11.5px; -fx-font-weight: 500; -fx-background-radius: 5px; -fx-border-color: #DED7CD; -fx-border-radius: 5px; -fx-pref-height: 36px; -fx-cursor: hand;");
        btnContinueOrdering.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnContinueOrdering, Priority.ALWAYS);
        AnimationHelper.applyButtonPress(btnContinueOrdering);
        btnContinueOrdering.setOnAction(e -> close());

        btnProceedToPayment.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-size: 11.5px; -fx-font-weight: 600; -fx-background-radius: 5px; -fx-pref-height: 36px; -fx-cursor: hand;");
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

        root.getChildren().addAll(header, scrollItems, rowSummary, noteBox, bottomActions);

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
            lblSummaryCount.setText("Tổng cộng (0 món)");
            lblGrandTotalValue.setText("0 đ");
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

        lblSummaryCount.setText("Tổng cộng (" + cartModel.getTotalQuantity() + " món)");
        lblGrandTotalValue.setText(CurrencyFormatter.format(cartModel.getTotalAmount()));
    }

    public void setVoucherFeedback(String text, boolean isSuccess) {
        lblVoucherFeedback.setText(text);
        if (isSuccess) {
            lblVoucherFeedback.setStyle("-fx-text-fill: #112823; -fx-font-size: 10.5px; -fx-font-weight: 700;");
        } else {
            lblVoucherFeedback.setStyle("-fx-text-fill: #C62828; -fx-font-size: 10.5px; -fx-font-weight: 700;");
        }
        lblVoucherFeedback.setVisible(true);
        lblVoucherFeedback.setManaged(true);
    }
}
