package cafe.views;

import cafe.models.CartModel;
import cafe.models.MenuItem;
import cafe.models.MenuRepository;
import cafe.models.OrderItem;
import cafe.utils.AnimationHelper;
import cafe.utils.CurrencyFormatter;
import cafe.utils.SvgIcons;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
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
 * CartModalDialog - Giao diện Giỏ hàng thoáng rộng, tối giản viền (chỉ giữ border cho Textbox cần thiết)
 * và đồng nhất 100% quy chuẩn chữ & bố cục với form Thanh toán (PaymentDialog) / Phiếu Thanh Toán (OrderSuccessDialog):
 * - Duy nhất tiêu đề chính "Giỏ hàng" dùng font Playfair Display (20px Bold #112823).
 * - Chữ quan trọng / Tiêu đề / Nút chính: 13px Bold (#112823).
 * - Chữ nội dung / Chi tiết phụ / Tag: 12px Regular (#4A453E).
 */
public class CartModalDialog extends Stage {

    private static final String STYLE_BOLD = "-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #112823;";
    private static final String STYLE_REGULAR = "-fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 400; -fx-text-fill: #4A453E;";

    private final int tableNumber;
    private final CartModel cartModel;
    private final BiConsumer<OrderItem, Integer> onQtyChange;
    private final Consumer<OrderItem> onDelete;
    private final Consumer<String> onApplyVoucher;
    private final Runnable onProceedToPayment;
    private final Runnable onCartUpdated;

    // UI Controls
    private final Label lblTitle = new Label("Giỏ hàng");
    private final Button btnClearAll = new Button("Xóa hết");

    private final VBox cartItemsContainer = new VBox(10);
    private final ScrollPane scrollItems = new ScrollPane();
    private final VBox emptyCartBox = new VBox(8);

    private final TextField txtVoucher = new TextField();
    private final Button btnApplyVoucher = new Button("Áp dụng");
    private final Label lblVoucherFeedback = new Label();

    private final TextField txtOrderNote = new TextField();

    private final Label lblSubtotalTitle = new Label("Tổng tiền (0 món)");
    private final Label lblSubtotalValue = new Label("0 đ");
    private final HBox rowDiscount = new HBox();
    private final Label lblDiscountTitle = new Label("Giảm giá");
    private final Label lblDiscountValue = new Label("0 đ");
    private final Label lblGrandTotalValue = new Label("0 đ");

    private final Button btnContinueOrdering = new Button();
    private final Button btnProceedToPayment = new Button();

    public CartModalDialog(Stage parentStage, CartModel cartModel,
                           BiConsumer<OrderItem, Integer> onQtyChange,
                           Consumer<OrderItem> onDelete,
                           Consumer<String> onApplyVoucher,
                           Runnable onProceedToPayment) {
        this(parentStage, 5, cartModel, onQtyChange, onDelete, onApplyVoucher, onProceedToPayment, null);
    }

    public CartModalDialog(Stage parentStage, int tableNumber, CartModel cartModel,
                           BiConsumer<OrderItem, Integer> onQtyChange,
                           Consumer<OrderItem> onDelete,
                           Consumer<String> onApplyVoucher,
                           Runnable onProceedToPayment,
                           Runnable onCartUpdated) {
        this.tableNumber = tableNumber;
        this.cartModel = cartModel;
        this.onQtyChange = onQtyChange;
        this.onDelete = onDelete;
        this.onApplyVoucher = onApplyVoucher;
        this.onProceedToPayment = onProceedToPayment;
        this.onCartUpdated = onCartUpdated;

        initOwner(parentStage);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.TRANSPARENT);
        setTitle("Giỏ Hàng • Bàn " + (tableNumber < 10 ? "0" + tableNumber : tableNumber));
        setResizable(false);

        initUI();
    }

    private void initUI() {
        // Mở rộng kích thước form (540px) và tăng khoảng cách để bố cục thoáng đãng, không bị bí
        VBox root = new VBox(15);
        root.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-background-radius: 16px; " +
            "-fx-border-color: #E2DDD5; " +
            "-fx-border-radius: 16px; " +
            "-fx-border-width: 1px; " +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.20), 26, 0.14, 0, 8);"
        );
        root.setPadding(new Insets(22, 26, 22, 26));
        root.setPrefWidth(540);
        root.setMinWidth(540);
        root.setMaxWidth(540);
        root.setMinHeight(Region.USE_PREF_SIZE);
        root.setMaxHeight(740);

        AnimationHelper.playDialogEntrance(root);

        // =====================================================================
        // 1. HEADER ĐỒNG NHẤT VỚI FORM THANH TOÁN: "Giỏ hàng" | Badge "Bàn 05" | [Xóa hết] | [✕]
        // =====================================================================
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-cursor: move; -fx-padding: 0 0 2 0;");
        cafe.utils.WindowChromeHelper.makeDraggable(this, header);

        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        // Nút "Xóa hết" phẳng không viền (borderless)
        String clearNormalStyle =
            "-fx-background-color: #F5EFEB; " +
            "-fx-border-color: transparent; " +
            "-fx-background-radius: 6px; " +
            "-fx-font-family: 'Inter', sans-serif; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 400; " +
            "-fx-text-fill: #4A453E; " +
            "-fx-padding: 4 10 4 10; " +
            "-fx-cursor: hand;";
        String clearHoverStyle =
            "-fx-background-color: #FDE8E8; " +
            "-fx-border-color: transparent; " +
            "-fx-background-radius: 6px; " +
            "-fx-font-family: 'Inter', sans-serif; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 700; " +
            "-fx-text-fill: #B91C1C; " +
            "-fx-padding: 4 10 4 10; " +
            "-fx-cursor: hand;";
        btnClearAll.setStyle(clearNormalStyle);
        btnClearAll.setOnMouseEntered(e -> btnClearAll.setStyle(clearHoverStyle));
        btnClearAll.setOnMouseExited(e -> btnClearAll.setStyle(clearNormalStyle));
        btnClearAll.setOnMousePressed(javafx.event.Event::consume);
        btnClearAll.setOnAction(e -> {
            cartModel.clear();
            if (onCartUpdated != null) onCartUpdated.run();
            refreshView();
        });

        Label lblTableBadge = new Label("Bàn " + (tableNumber < 10 ? "0" + tableNumber : tableNumber));
        lblTableBadge.setStyle("-fx-background-color: #EBE5DB; -fx-text-fill: #563521; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 5px; -fx-padding: 4 10 4 10;");

        Button btnClose = new Button();
        btnClose.setGraphic(cafe.utils.IconHelper.getIcon("client", "dong-cua-so.png", SvgIcons.CLOSE, "✕", 12, Color.web("#112823")));
        btnClose.setStyle("-fx-background-color: #F5EFEB; -fx-background-radius: 14px; -fx-min-width: 28px; -fx-min-height: 28px; -fx-pref-width: 28px; -fx-pref-height: 28px; -fx-padding: 0; -fx-cursor: hand; -fx-alignment: center;");
        btnClose.setOnMouseEntered(e -> btnClose.setStyle("-fx-background-color: #EAE4DA; -fx-background-radius: 14px; -fx-min-width: 28px; -fx-min-height: 28px; -fx-pref-width: 28px; -fx-pref-height: 28px; -fx-padding: 0; -fx-cursor: hand; -fx-alignment: center;"));
        btnClose.setOnMouseExited(e -> btnClose.setStyle("-fx-background-color: #F5EFEB; -fx-background-radius: 14px; -fx-min-width: 28px; -fx-min-height: 28px; -fx-pref-width: 28px; -fx-pref-height: 28px; -fx-padding: 0; -fx-cursor: hand; -fx-alignment: center;"));
        btnClose.setOnMousePressed(javafx.event.Event::consume);
        btnClose.setOnAction(e -> close());

        header.getChildren().addAll(lblTitle, sp, btnClearAll, lblTableBadge, btnClose);

        // =====================================================================
        // 2. DANH SÁCH MÓN TRONG GIỎ (KHÔNG VIỀN BAO NGOÀI, KHOẢNG CÁCH THOÁNG)
        // =====================================================================
        cartItemsContainer.setSpacing(10);
        cartItemsContainer.setPadding(new Insets(2, 4, 2, 0));

        scrollItems.setContent(cartItemsContainer);
        scrollItems.setFitToWidth(true);
        scrollItems.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollItems.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollItems.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0;");

        emptyCartBox.setAlignment(Pos.CENTER);
        emptyCartBox.setPadding(new Insets(30, 20, 30, 20));
        SVGPath emptyCartIcon = SvgIcons.createOutlineIcon(SvgIcons.SHOPPING_BAG, 1.5, Color.web("#DED7CD"));
        Label emptyTitle = new Label("Giỏ hàng đang trống");
        emptyTitle.setStyle(STYLE_BOLD);
        Label emptySub = new Label("Hãy chọn món yêu thích từ thực đơn để bắt đầu.");
        emptySub.setStyle(STYLE_REGULAR);
        emptyCartBox.getChildren().addAll(emptyCartIcon, emptyTitle, emptySub);

        // =====================================================================
        // 3. GHI CHÚ & MÃ ƯU ĐÃI (LOẠI BỎ KHUNG VIỀN LỒNG NHAU, CHỈ GIỮ BORDER TEXTBOX)
        // =====================================================================
        VBox extraInputsSection = new VBox(10);
        extraInputsSection.setPadding(new Insets(2, 0, 0, 0));

        // Hàng tag gợi ý nhanh: dùng Pill phẳng không viền (borderless) nhẹ nhàng
        HBox quickChipsRow = new HBox(8);
        quickChipsRow.setAlignment(Pos.CENTER_LEFT);
        String[] quickTags = {"Uống tại bàn", "Mang về", "Ít ngọt", "Không lấy ống hút"};
        for (String tag : quickTags) {
            quickChipsRow.getChildren().add(createQuickChip(tag));
        }

        // Textbox Ghi chú (giữ 1 border mảnh cần thiết cho ô nhập liệu)
        txtOrderNote.setPromptText("Ghi chú thêm cho quán (VD: ít đá, để riêng topping...)");
        txtOrderNote.setText(cartModel.getOrderNote());
        txtOrderNote.setStyle(
            STYLE_REGULAR +
            " -fx-background-color: #FFFFFF; " +
            "-fx-border-color: #E2DDD5; " +
            "-fx-border-radius: 8px; " +
            "-fx-background-radius: 8px; " +
            "-fx-padding: 8 12 8 12;"
        );
        txtOrderNote.textProperty().addListener((obs, oldV, newV) -> cartModel.setOrderNote(newV));

        // Hàng nhập Voucher (bỏ khung viền bao ngoài, chỉ giữ border trên ô nhập mã)
        HBox voucherInputRow = new HBox(8);
        voucherInputRow.setAlignment(Pos.CENTER_LEFT);

        txtVoucher.setPromptText("Mã giảm giá (GIAM10, WELCOME10, GIAM15K)");
        txtVoucher.setStyle(
            STYLE_REGULAR +
            " -fx-background-color: #FFFFFF; " +
            "-fx-border-color: #E2DDD5; " +
            "-fx-border-radius: 8px; " +
            "-fx-background-radius: 8px; " +
            "-fx-padding: 8 12 8 12;"
        );
        HBox.setHgrow(txtVoucher, Priority.ALWAYS);
        if (cartModel.getAppliedVoucher() != null) {
            txtVoucher.setText(cartModel.getAppliedVoucher().getCode());
        }

        btnApplyVoucher.setStyle(
            STYLE_BOLD +
            " -fx-background-color: #F5EFEB; " +
            "-fx-border-color: transparent; " +
            "-fx-background-radius: 8px; " +
            "-fx-padding: 8 16 8 16; " +
            "-fx-cursor: hand;"
        );
        AnimationHelper.applyButtonPress(btnApplyVoucher);
        btnApplyVoucher.setOnAction(e -> {
            if (onApplyVoucher != null) {
                onApplyVoucher.accept(txtVoucher.getText());
            }
        });

        voucherInputRow.getChildren().addAll(txtVoucher, btnApplyVoucher);

        lblVoucherFeedback.setVisible(false);
        lblVoucherFeedback.setManaged(false);
        if (cartModel.getAppliedVoucher() != null) {
            setVoucherFeedback("✓ Đã áp dụng mã " + cartModel.getAppliedVoucher().getCode(), true);
        }

        extraInputsSection.getChildren().addAll(quickChipsRow, txtOrderNote, voucherInputRow, lblVoucherFeedback);

        // =====================================================================
        // 4. BẢNG TÍNH TIỀN ĐỒNG NHẤT VỚI FORM THANH TOÁN (PaymentDialog & OrderSuccessDialog)
        // =====================================================================
        VBox summaryBox = new VBox(8);
        summaryBox.setPadding(new Insets(6, 0, 2, 0));

        HBox rowSubtotal = new HBox();
        rowSubtotal.setAlignment(Pos.CENTER_LEFT);
        lblSubtotalTitle.setStyle(STYLE_REGULAR);
        Region spSub = new Region();
        HBox.setHgrow(spSub, Priority.ALWAYS);
        lblSubtotalValue.setStyle(STYLE_BOLD);
        rowSubtotal.getChildren().addAll(lblSubtotalTitle, spSub, lblSubtotalValue);

        rowDiscount.setAlignment(Pos.CENTER_LEFT);
        lblDiscountTitle.setStyle(STYLE_REGULAR);
        Region spDisc = new Region();
        HBox.setHgrow(spDisc, Priority.ALWAYS);
        lblDiscountValue.setStyle(STYLE_BOLD);
        rowDiscount.getChildren().addAll(lblDiscountTitle, spDisc, lblDiscountValue);

        // Duy nhất 1 đường kẻ mảnh ngăn cách trước dòng Thành tiền (y hệt PaymentDialog)
        HBox rowGrandTotal = new HBox();
        rowGrandTotal.setAlignment(Pos.CENTER_LEFT);
        rowGrandTotal.setPadding(new Insets(8, 0, 0, 0));
        rowGrandTotal.setStyle("-fx-border-color: #EAE4DA transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        Label lblGrandTitle = new Label("Thành tiền");
        lblGrandTitle.setStyle(STYLE_BOLD);
        Region spGrand = new Region();
        HBox.setHgrow(spGrand, Priority.ALWAYS);
        lblGrandTotalValue.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        rowGrandTotal.getChildren().addAll(lblGrandTitle, spGrand, lblGrandTotalValue);

        summaryBox.getChildren().addAll(rowSubtotal, rowDiscount, rowGrandTotal);

        // =====================================================================
        // 5. BỘ ĐÔI NÚT HÀNH ĐỘNG ĐỒNG NHẤT KIỂU CHỮ (38% - 62%)
        // =====================================================================
        HBox bottomActions = new HBox(12);
        bottomActions.setAlignment(Pos.CENTER);
        bottomActions.setPadding(new Insets(4, 0, 0, 0));

        // Nút Phụ: [Thêm món khác]
        btnContinueOrdering.setText(null);
        btnContinueOrdering.setPrefWidth(175);
        btnContinueOrdering.setMinWidth(165);
        btnContinueOrdering.setPrefHeight(44);
        btnContinueOrdering.setStyle(
            "-fx-background-color: #F5EFEB; " +
            "-fx-background-radius: 8px; " +
            "-fx-border-color: transparent; " +
            "-fx-cursor: hand;"
        );
        AnimationHelper.applyButtonPress(btnContinueOrdering);

        HBox addMoreBox = new HBox(6);
        addMoreBox.setAlignment(Pos.CENTER);
        Node addIcon = cafe.utils.IconHelper.getIcon("client", "them-mon.png", null, "+", 13, Color.web("#112823"));
        Label lblAdd = new Label("Thêm món khác");
        lblAdd.setStyle(STYLE_BOLD);
        addMoreBox.getChildren().addAll(addIcon, lblAdd);
        btnContinueOrdering.setGraphic(addMoreBox);
        btnContinueOrdering.setOnAction(e -> close());

        // Nút Chính: [Tiến hành Thanh toán →]
        btnProceedToPayment.setText(null);
        btnProceedToPayment.setMaxWidth(Double.MAX_VALUE);
        btnProceedToPayment.setPrefHeight(44);
        HBox.setHgrow(btnProceedToPayment, Priority.ALWAYS);
        btnProceedToPayment.setStyle(
            "-fx-background-color: #112823; " +
            "-fx-background-radius: 8px; " +
            "-fx-border-color: transparent; " +
            "-fx-cursor: hand;"
        );
        AnimationHelper.applyButtonPress(btnProceedToPayment);

        HBox payBox = new HBox(8);
        payBox.setAlignment(Pos.CENTER);
        Node payIcon = cafe.utils.IconHelper.getIcon("client", "thanh-toan.png", SvgIcons.WALLET, "💳", 16, Color.WHITE);
        Label lblPay = new Label("Tiến hành Thanh toán →");
        lblPay.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #FFFFFF;");
        payBox.getChildren().addAll(payIcon, lblPay);
        btnProceedToPayment.setGraphic(payBox);

        btnProceedToPayment.setOnAction(e -> {
            close();
            if (onProceedToPayment != null) {
                onProceedToPayment.run();
            }
        });

        bottomActions.getChildren().addAll(btnContinueOrdering, btnProceedToPayment);

        root.getChildren().addAll(header, scrollItems, extraInputsSection, summaryBox, bottomActions);

        StackPane sceneRoot = new StackPane(root);
        sceneRoot.setStyle("-fx-background-color: transparent;");
        sceneRoot.setPadding(new Insets(16));

        Scene scene = new Scene(sceneRoot);
        scene.setFill(Color.TRANSPARENT);
        cafe.utils.WindowChromeHelper.applyTheme(scene);

        setScene(scene);
        refreshView();
    }

    /**
     * Tag chọn nhanh phẳng không viền (borderless pill) giúp giao diện thoáng và sạch.
     */
    private Button createQuickChip(String tagText) {
        Button chip = new Button(tagText);
        String defaultStyle =
            STYLE_REGULAR +
            " -fx-background-color: #F5EFEB; " +
            "-fx-border-color: transparent; " +
            "-fx-background-radius: 14px; " +
            "-fx-padding: 4 11 4 11; " +
            "-fx-cursor: hand;";
        String activeStyle =
            "-fx-font-family: 'Inter', sans-serif; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 700; " +
            "-fx-text-fill: #FFFFFF; " +
            "-fx-background-color: #112823; " +
            "-fx-border-color: transparent; " +
            "-fx-background-radius: 14px; " +
            "-fx-padding: 4 11 4 11; " +
            "-fx-cursor: hand;";

        chip.setStyle(defaultStyle);
        chip.setOnAction(e -> {
            String current = txtOrderNote.getText() != null ? txtOrderNote.getText().trim() : "";
            if (tagText.equalsIgnoreCase("Mang về")) {
                cartModel.setOrderType("TAKEAWAY");
            } else if (tagText.equalsIgnoreCase("Uống tại bàn")) {
                cartModel.setOrderType("DINE_IN");
            }

            if (current.contains(tagText)) {
                String updated = current.replace(tagText, "").replaceAll(",\\s*,", ",").replaceAll("^,\\s*|,\\s*$", "").trim();
                txtOrderNote.setText(updated);
                chip.setStyle(defaultStyle);
            } else {
                String updated = current.isEmpty() ? tagText : (current + ", " + tagText);
                txtOrderNote.setText(updated);
                chip.setStyle(activeStyle);
            }
        });
        return chip;
    }

    public void refreshView() {
        cartItemsContainer.getChildren().clear();

        int itemCount = cartModel.getItems().size();
        int totalQty = cartModel.getTotalQuantity();

        lblTitle.setText(totalQty > 0 ? "Giỏ hàng (" + totalQty + " món)" : "Giỏ hàng");
        btnClearAll.setVisible(!cartModel.isEmpty());
        btnClearAll.setManaged(!cartModel.isEmpty());

        if (cartModel.isEmpty()) {
            scrollItems.setContent(emptyCartBox);
            scrollItems.setPrefViewportHeight(140);
            scrollItems.setMinHeight(140);
            scrollItems.setMaxHeight(150);

            lblSubtotalTitle.setText("Tổng tiền (0 món)");
            lblSubtotalValue.setText("0 đ");
            rowDiscount.setVisible(false);
            rowDiscount.setManaged(false);
            lblGrandTotalValue.setText("0 đ");
            btnProceedToPayment.setDisable(true);
            sizeToScene();
            return;
        }

        btnProceedToPayment.setDisable(false);
        scrollItems.setContent(cartItemsContainer);

        for (OrderItem oi : cartModel.getItems()) {
            CartItemRow row = new CartItemRow(
                oi,
                (item, delta) -> {
                    if (onQtyChange != null) {
                        onQtyChange.accept(item, delta);
                        refreshView();
                    }
                },
                item -> {
                    if (onDelete != null) {
                        onDelete.accept(item);
                        refreshView();
                    }
                },
                itemToEdit -> openEditItemDialog(itemToEdit)
            );
            cartItemsContainer.getChildren().add(row);
        }

        // Chiều cao danh sách món rộng rãi hơn (1 món 115px, 2 món 228px, >=3 món 295px)
        double dynamicHeight = itemCount == 1 ? 115 : (itemCount == 2 ? 228 : 295);
        scrollItems.setPrefViewportHeight(dynamicHeight);
        scrollItems.setMinHeight(dynamicHeight);
        scrollItems.setMaxHeight(295);

        lblSubtotalTitle.setText("Tổng tiền (" + totalQty + " món)");
        lblSubtotalValue.setText(CurrencyFormatter.format(cartModel.getSubtotalAmount()));

        if (cartModel.getDiscountAmount() > 0) {
            String code = cartModel.getAppliedVoucher() != null ? " (" + cartModel.getAppliedVoucher().getCode() + ")" : "";
            lblDiscountTitle.setText("Giảm giá" + code);
            lblDiscountValue.setText("- " + CurrencyFormatter.format(cartModel.getDiscountAmount()));
            rowDiscount.setVisible(true);
            rowDiscount.setManaged(true);
        } else {
            rowDiscount.setVisible(false);
            rowDiscount.setManaged(false);
        }

        lblGrandTotalValue.setText(CurrencyFormatter.format(cartModel.getTotalAmount()));
        sizeToScene();
    }

    private void openEditItemDialog(OrderItem itemToEdit) {
        if (itemToEdit == null) return;
        MenuItem menuItem = itemToEdit.getProductId() > 0
            ? MenuRepository.findById(itemToEdit.getProductId())
            : MenuRepository.findByName(itemToEdit.getName());
        if (menuItem == null) return;

        ItemDetailDialog editDialog = new ItemDetailDialog(this, menuItem, itemToEdit, updatedOrderItem -> {
            cartModel.replaceItem(itemToEdit, updatedOrderItem);
            if (onCartUpdated != null) {
                onCartUpdated.run();
            }
            refreshView();
        });
        editDialog.show();
    }

    public void setVoucherFeedback(String text, boolean isSuccess) {
        lblVoucherFeedback.setText(text);
        if (isSuccess) {
            lblVoucherFeedback.setStyle(STYLE_REGULAR + " -fx-text-fill: #112823;");
        } else {
            lblVoucherFeedback.setStyle(STYLE_REGULAR + " -fx-text-fill: #C62828;");
        }
        lblVoucherFeedback.setVisible(true);
        lblVoucherFeedback.setManaged(true);
        sizeToScene();
    }
}

