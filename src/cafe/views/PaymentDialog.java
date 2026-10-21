package cafe.views;

import cafe.models.CartModel;
import cafe.utils.AnimationHelper;
import cafe.utils.CurrencyFormatter;
import cafe.utils.SvgIcons;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * PaymentDialog - Hộp thoại Thanh toán chuẩn hóa:
 * - Thiết kế Fintech VietQR 2 cột cân đối, không cần cuộn, tương thích mọi màn hình.
 * - Mã VietQR MB Bank sắc nét, viền bo sạch đẹp, không viền lem.
 * - Tích hợp nút 1-click sao chép Số tài khoản và Nội dung chuyển khoản.
 * - Chuẩn hóa toàn bộ số và kiểu chữ đồng nhất (Segoe UI / Inter, chống số to số nhỏ).
 */
public class PaymentDialog extends Stage {
    private static final String FONT_FAMILY = "-fx-font-family: 'Segoe UI', 'Inter', -apple-system, sans-serif;";
    private static final String STYLE_LABEL = FONT_FAMILY + " -fx-font-size: 12px; -fx-font-weight: 500; -fx-text-fill: #57534E;";
    private static final String STYLE_VALUE = FONT_FAMILY + " -fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #1C1917;";
    private static final String STYLE_TOTAL_LABEL = FONT_FAMILY + " -fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #1C1917;";
    private static final String STYLE_TOTAL_VALUE = FONT_FAMILY + " -fx-font-size: 17px; -fx-font-weight: 800; -fx-text-fill: #112823;";

    private final int tableNumber;
    private final CartModel cartModel;
    private final Consumer<String> onConfirmed;

    private String selectedMethod = "VIETQR"; // Mặc định chọn VietQR
    private final List<HBox> methodCards = new ArrayList<>();
    private final StackPane rightDetailPane = new StackPane();

    public PaymentDialog(Stage parentStage, int tableNumber, CartModel cartModel, Consumer<String> onConfirmed) {
        this.tableNumber = tableNumber;
        this.cartModel = cartModel;
        this.onConfirmed = onConfirmed;

        initOwner(parentStage);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.TRANSPARENT);
        setTitle("Thanh Toán • Bàn " + tableNumber);
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
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.22), 24, 0.12, 0, 8);"
        );
        root.setPadding(new Insets(16, 20, 18, 20));
        root.setPrefWidth(660);
        root.setMaxWidth(680);
        root.setPrefHeight(510);
        root.setMaxHeight(525);

        AnimationHelper.playDialogEntrance(root);

        // 1. Header: Back ←, "Thanh toán đơn hàng", Badge Bàn 05, Nút đóng [X]
        HBox headerRow = new HBox(10);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setStyle("-fx-cursor: move;");
        cafe.utils.WindowChromeHelper.makeDraggable(this, headerRow);

        Button btnBack = new Button("←");
        btnBack.setStyle(FONT_FAMILY + " -fx-background-color: #F5EFEB; -fx-text-fill: #292722; -fx-font-size: 13px; -fx-font-weight: 700; -fx-cursor: hand; -fx-background-radius: 14px; -fx-min-width: 28px; -fx-min-height: 28px; -fx-padding: 0; -fx-alignment: center;");
        btnBack.setOnAction(e -> close());

        Label lblTitle = new Label("Thanh toán đơn hàng");
        lblTitle.setStyle(FONT_FAMILY + " -fx-font-size: 17px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        Region spHeader = new Region();
        HBox.setHgrow(spHeader, Priority.ALWAYS);

        String tblStr = (tableNumber <= 0 ? "Mang về" : "Bàn " + (tableNumber < 10 ? "0" + tableNumber : tableNumber));
        Label lblTableBadge = new Label(tblStr);
        lblTableBadge.setStyle(FONT_FAMILY + " -fx-background-color: #EBE5DB; -fx-text-fill: #563521; -fx-font-size: 11.5px; -fx-font-weight: 700; -fx-background-radius: 6px; -fx-padding: 3 9 3 9;");

        Button btnClose = new Button();
        btnClose.setGraphic(cafe.utils.IconHelper.getIcon("client", "dong-cua-so.png", SvgIcons.CLOSE, "✕", 12, Color.web("#563521")));
        btnClose.setStyle("-fx-background-color: #F5EFEB; -fx-background-radius: 14px; -fx-min-width: 28px; -fx-min-height: 28px; -fx-padding: 0; -fx-cursor: hand; -fx-alignment: center;");
        btnClose.setOnAction(e -> close());

        headerRow.getChildren().addAll(btnBack, lblTitle, spHeader, lblTableBadge, btnClose);

        // 2. Main 2-Column Body Layout (Cột trái: Chọn phương thức & Tổng tiền | Cột phải: VietQR Fintech Card)
        HBox mainColumns = new HBox(16);
        mainColumns.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(mainColumns, Priority.ALWAYS);

        // === CỘT TRÁI (285px) ===
        VBox leftCol = new VBox(10);
        leftCol.setPrefWidth(285);
        leftCol.setMinWidth(280);

        Label lblSection = new Label("Chọn phương thức thanh toán");
        lblSection.setStyle(FONT_FAMILY + " -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #112823;");

        VBox methodsBox = new VBox(6);
        HBox cardQR = createMethodCard("Quét mã VietQR (MB Bank)", "thanh-toan-qr.png", SvgIcons.CREDIT_CARD, "📱", true, "VIETQR");
        HBox cardCash = createMethodCard("Tiền mặt tại quầy", "tien-mat.png", SvgIcons.CASH, "💵", false, "TIỀN MẶT");
        HBox cardWallet = createMethodCard("Ví điện tử / Thẻ POS", "thanh-toan.png", SvgIcons.WALLET, "💳", false, "VÍ ĐIỆN TỬ");

        methodCards.add(cardQR);
        methodCards.add(cardCash);
        methodCards.add(cardWallet);
        methodsBox.getChildren().addAll(cardQR, cardCash, cardWallet);

        // Bảng tính tiền chuẩn hóa font số
        VBox summaryBox = new VBox(6);
        summaryBox.setPadding(new Insets(10, 12, 10, 12));
        summaryBox.setStyle("-fx-background-color: #FAF8F5; -fx-background-radius: 8px; -fx-border-color: #EDE8E0; -fx-border-radius: 8px; -fx-border-width: 1px;");

        HBox rowSub = new HBox();
        rowSub.setAlignment(Pos.CENTER_LEFT);
        Label lblSubT = new Label("Tổng tiền (" + cartModel.getTotalQuantity() + " món)");
        lblSubT.setStyle(STYLE_LABEL);
        Region spSub = new Region();
        HBox.setHgrow(spSub, Priority.ALWAYS);
        Label lblSubV = new Label(CurrencyFormatter.format(cartModel.getSubtotalAmount()));
        lblSubV.setStyle(STYLE_VALUE);
        rowSub.getChildren().addAll(lblSubT, spSub, lblSubV);

        HBox rowDisc = new HBox();
        rowDisc.setAlignment(Pos.CENTER_LEFT);
        Label lblDiscT = new Label("Giảm giá");
        lblDiscT.setStyle(STYLE_LABEL);
        Region spDisc = new Region();
        HBox.setHgrow(spDisc, Priority.ALWAYS);
        String discText = cartModel.getDiscountAmount() > 0 ? "- " + CurrencyFormatter.format(cartModel.getDiscountAmount()) : "0 đ";
        Label lblDiscV = new Label(discText);
        lblDiscV.setStyle(cartModel.getDiscountAmount() > 0 ? (FONT_FAMILY + " -fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #15803D;") : STYLE_VALUE);
        rowDisc.getChildren().addAll(lblDiscT, spDisc, lblDiscV);

        HBox rowTotal = new HBox();
        rowTotal.setAlignment(Pos.CENTER_LEFT);
        rowTotal.setPadding(new Insets(5, 0, 0, 0));
        rowTotal.setStyle("-fx-border-color: #E6E0D8 transparent transparent transparent; -fx-border-width: 1 0 0 0;");
        Label lblTotalT = new Label("Thành tiền:");
        lblTotalT.setStyle(STYLE_TOTAL_LABEL);
        Region spTotal = new Region();
        HBox.setHgrow(spTotal, Priority.ALWAYS);
        Label lblTotalV = new Label(CurrencyFormatter.format(cartModel.getTotalAmount()));
        lblTotalV.setStyle(STYLE_TOTAL_VALUE);
        rowTotal.getChildren().addAll(lblTotalT, spTotal, lblTotalV);

        summaryBox.getChildren().addAll(rowSub, rowDisc, rowTotal);

        // Nút xác nhận thanh toán
        Button btnConfirm = new Button();
        btnConfirm.setMaxWidth(Double.MAX_VALUE);
        btnConfirm.setPrefHeight(42);
        btnConfirm.setStyle("-fx-background-color: #112823; -fx-background-radius: 8px; -fx-cursor: hand;");
        AnimationHelper.applyButtonPress(btnConfirm);

        HBox confirmGraphic = new HBox(8);
        confirmGraphic.setAlignment(Pos.CENTER);
        Node confirmIcon = cafe.utils.IconHelper.getIcon("client", "dong-y-dat.png", null, "✔", 16, Color.WHITE);
        Label lblConfirm = new Label("Xác nhận thanh toán");
        lblConfirm.setStyle(FONT_FAMILY + " -fx-text-fill: #FFFFFF; -fx-font-size: 13.5px; -fx-font-weight: 700;");
        confirmGraphic.getChildren().addAll(confirmIcon, lblConfirm);
        btnConfirm.setGraphic(confirmGraphic);

        btnConfirm.setOnAction(e -> {
            if (onConfirmed != null) {
                onConfirmed.accept(selectedMethod);
            }
            close();
        });

        leftCol.getChildren().addAll(lblSection, methodsBox, summaryBox, btnConfirm);

        // === CỘT PHẢI (VietQR Fintech Card & Chi tiết) ===
        rightDetailPane.setAlignment(Pos.CENTER);
        HBox.setHgrow(rightDetailPane, Priority.ALWAYS);
        rightDetailPane.getChildren().add(buildVietQrDetailPanel());

        mainColumns.getChildren().addAll(leftCol, rightDetailPane);
        root.getChildren().addAll(headerRow, mainColumns);

        StackPane sceneRoot = new StackPane(root);
        sceneRoot.setStyle("-fx-background-color: transparent;");
        sceneRoot.setPadding(new Insets(10));

        Scene scene = new Scene(sceneRoot);
        scene.setFill(Color.TRANSPARENT);
        cafe.utils.WindowChromeHelper.applyTheme(scene);

        setScene(scene);
    }

    private HBox createMethodCard(String title, String iconFileName, String iconSvg, String fallbackEmoji, boolean isSelected, String methodKey) {
        HBox card = new HBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(8, 12, 8, 12));
        card.setCursor(javafx.scene.Cursor.HAND);
        AnimationHelper.applyButtonPress(card);

        Node icon = cafe.utils.IconHelper.getIcon("client", iconFileName, iconSvg, fallbackEmoji, 18, Color.web("#112823"));
        Label lblTitle = new Label(title);
        lblTitle.setStyle(FONT_FAMILY + " -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #1C1917;");
        HBox.setHgrow(lblTitle, Priority.ALWAYS);

        card.getChildren().addAll(icon, lblTitle);

        if (isSelected) {
            Label checkmark = new Label("✔");
            checkmark.setStyle(FONT_FAMILY + " -fx-font-size: 12px; -fx-text-fill: #112823; -fx-font-weight: 800;");
            card.getChildren().add(checkmark);
            card.setStyle("-fx-background-color: #F8F4EE; -fx-background-radius: 8px; -fx-border-color: #112823; -fx-border-radius: 8px; -fx-border-width: 1.5px;");
        } else {
            Label chevron = new Label("›");
            chevron.setStyle(FONT_FAMILY + " -fx-font-size: 14px; -fx-text-fill: #817A70;");
            card.getChildren().add(chevron);
            card.setStyle("-fx-background-color: #FDFBF8; -fx-background-radius: 8px; -fx-border-color: #E6E0D8; -fx-border-radius: 8px; -fx-border-width: 1px;");
        }

        card.setOnMouseClicked(e -> {
            selectedMethod = methodKey;
            for (HBox c : methodCards) {
                boolean active = (c == card);
                if (c.getChildren().size() > 2) {
                    Label rightIcon = (Label) c.getChildren().get(2);
                    if (active) {
                        rightIcon.setText("✔");
                        rightIcon.setStyle(FONT_FAMILY + " -fx-font-size: 12px; -fx-text-fill: #112823; -fx-font-weight: 800;");
                        c.setStyle("-fx-background-color: #F8F4EE; -fx-background-radius: 8px; -fx-border-color: #112823; -fx-border-radius: 8px; -fx-border-width: 1.5px;");
                    } else {
                        rightIcon.setText("›");
                        rightIcon.setStyle(FONT_FAMILY + " -fx-font-size: 14px; -fx-text-fill: #817A70;");
                        c.setStyle("-fx-background-color: #FDFBF8; -fx-background-radius: 8px; -fx-border-color: #E6E0D8; -fx-border-radius: 8px; -fx-border-width: 1px;");
                    }
                }
            }

            rightDetailPane.getChildren().clear();
            if ("VIETQR".equalsIgnoreCase(methodKey)) {
                rightDetailPane.getChildren().add(buildVietQrDetailPanel());
            } else if ("VÍ ĐIỆN TỬ".equalsIgnoreCase(methodKey)) {
                rightDetailPane.getChildren().add(buildWalletDetailPanel());
            } else {
                rightDetailPane.getChildren().add(buildCashDetailPanel());
            }
        });

        return card;
    }

    /**
     * Khung hiển thị chi tiết mã QR VietQR phong cách Fintech Card sang trọng, sắc nét
     */
    private VBox buildVietQrDetailPanel() {
        VBox card = new VBox(0);
        card.setAlignment(Pos.TOP_CENTER);
        card.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-background-radius: 12px; " +
            "-fx-border-color: #DDE2E5; " +
            "-fx-border-radius: 12px; " +
            "-fx-border-width: 1px; " +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.06), 10, 0.08, 0, 3);"
        );
        VBox.setVgrow(card, Priority.ALWAYS);

        // 1. Header Banner MB Bank Gradient
        HBox bankHeader = new HBox(8);
        bankHeader.setAlignment(Pos.CENTER_LEFT);
        bankHeader.setPadding(new Insets(8, 12, 8, 12));
        bankHeader.setStyle("-fx-background-color: linear-gradient(to right, #001A5E, #003399); -fx-background-radius: 11px 11px 0 0;");

        Label mbLogoBadge = new Label("MB");
        mbLogoBadge.setStyle(FONT_FAMILY + " -fx-background-color: #FFFFFF; -fx-text-fill: #003399; -fx-font-size: 11px; -fx-font-weight: 900; -fx-padding: 1 5; -fx-background-radius: 4px;");

        Label lblBankTitle = new Label("NGÂN HÀNG QUÂN ĐỘI");
        lblBankTitle.setStyle(FONT_FAMILY + " -fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #FFFFFF; -fx-letter-spacing: 0.5px;");

        Region spH = new Region();
        HBox.setHgrow(spH, Priority.ALWAYS);

        Label lblVietQrBadge = new Label("VietQR • Napas 24/7");
        lblVietQrBadge.setStyle(FONT_FAMILY + " -fx-background-color: rgba(255,255,255,0.18); -fx-text-fill: #FFFFFF; -fx-font-size: 10px; -fx-font-weight: 700; -fx-padding: 2 6; -fx-background-radius: 4px;");

        bankHeader.getChildren().addAll(mbLogoBadge, lblBankTitle, spH, lblVietQrBadge);

        // 2. Body Card
        VBox body = new VBox(6);
        body.setAlignment(Pos.CENTER);
        body.setPadding(new Insets(8, 12, 8, 12));
        VBox.setVgrow(body, Priority.ALWAYS);

        // SỐ TIỀN CẦN THANH TOÁN (To, rõ, màu xanh ngọc sang trọng)
        HBox amountRow = new HBox(6);
        amountRow.setAlignment(Pos.BASELINE_CENTER);
        Label lblAmtTitle = new Label("Số tiền:");
        lblAmtTitle.setStyle(FONT_FAMILY + " -fx-font-size: 11.5px; -fx-font-weight: 600; -fx-text-fill: #6B7280;");
        Label lblAmtVal = new Label(CurrencyFormatter.format(cartModel.getTotalAmount()));
        lblAmtVal.setStyle(FONT_FAMILY + " -fx-font-size: 17px; -fx-font-weight: 900; -fx-text-fill: #047857;");
        amountRow.getChildren().addAll(lblAmtTitle, lblAmtVal);

        // KHUNG MÃ QR SẮC NÉT (Bọc trong container trắng viền mỏng bo góc)
        StackPane qrFrame = new StackPane();
        qrFrame.setAlignment(Pos.CENTER);
        qrFrame.setPadding(new Insets(4));
        qrFrame.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2DDD5; -fx-border-radius: 8px; -fx-background-radius: 8px;");

        ImageView qrView = new ImageView();
        try {
            InputStream is = getClass().getResourceAsStream("/cafe/resources/images/vietqr_clean.png");
            if (is == null) {
                is = getClass().getResourceAsStream("/cafe/resources/images/vietqr_mb.png");
            }
            if (is != null) {
                Image img = new Image(is);
                qrView.setImage(img);
                qrView.setFitWidth(138);
                qrView.setFitHeight(138);
                qrView.setPreserveRatio(true);
                qrView.setSmooth(true);
            }
        } catch (Exception ignored) {}

        qrFrame.getChildren().add(qrView);

        // Huy hiệu trạng thái quét mã
        Label lblScanStatus = new Label("🟢 Sẵn sàng quét mã • Tự động đối soát");
        lblScanStatus.setStyle(FONT_FAMILY + " -fx-font-size: 10.5px; -fx-font-weight: 700; -fx-text-fill: #065F46; -fx-background-color: #ECFDF5; -fx-background-radius: 20px; -fx-padding: 2 8;");

        // BẢNG THÔNG TIN THANH TOÁN KÈM NÚT 1-CLICK SAO CHÉP
        VBox infoGrid = new VBox(3);
        infoGrid.setPadding(new Insets(6, 8, 6, 8));
        infoGrid.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 8px; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-border-width: 1px;");

        String memoText = (tableNumber <= 0 ? "CAFE MANG VE" : "BAN " + (tableNumber < 10 ? "0" + tableNumber : tableNumber) + " CAFE");

        infoGrid.getChildren().addAll(
            createDetailRowPlain("Chủ tài khoản:", "BREW & BEAN SPECIALTY"),
            createDetailRowWithCopy("Số tài khoản:", "0909 888 999", "0909888999"),
            createDetailRowWithCopy("Nội dung CK:", memoText, memoText)
        );

        Label lblGuide = new Label("Mở app MB Bank, VCB, MoMo... bất kỳ để quét mã");
        lblGuide.setStyle(FONT_FAMILY + " -fx-font-size: 10px; -fx-text-fill: #64748B; -fx-font-style: italic;");

        body.getChildren().addAll(amountRow, qrFrame, lblScanStatus, infoGrid, lblGuide);
        card.getChildren().addAll(bankHeader, body);
        return card;
    }

    private VBox buildCashDetailPanel() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 16, 20, 16));
        box.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12px; -fx-border-color: #E2DDD5; -fx-border-radius: 12px; -fx-border-width: 1px;");
        VBox.setVgrow(box, Priority.ALWAYS);

        Label lblIcon = new Label("💵");
        lblIcon.setStyle("-fx-font-size: 38px;");

        Label lblTipTitle = new Label("Thanh toán tiền mặt tại quầy");
        lblTipTitle.setStyle(FONT_FAMILY + " -fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #112823;");

        Label lblTipDesc = new Label("Quý khách vui lòng thanh toán trực tiếp tại quầy thu ngân hoặc cho nhân viên phục vụ khi nhận đồ uống.");
        lblTipDesc.setWrapText(true);
        lblTipDesc.setAlignment(Pos.CENTER);
        lblTipDesc.setStyle(STYLE_LABEL + " -fx-text-alignment: center;");

        box.getChildren().addAll(lblIcon, lblTipTitle, lblTipDesc);
        return box;
    }

    private VBox buildWalletDetailPanel() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 16, 20, 16));
        box.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12px; -fx-border-color: #E2DDD5; -fx-border-radius: 12px; -fx-border-width: 1px;");
        VBox.setVgrow(box, Priority.ALWAYS);

        Label lblIcon = new Label("💳");
        lblIcon.setStyle("-fx-font-size: 38px;");

        Label lblTipTitle = new Label("Thanh toán Thẻ & Ví điện tử");
        lblTipTitle.setStyle(FONT_FAMILY + " -fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #112823;");

        Label lblTipDesc = new Label("Quán hỗ trợ quẹt thẻ POS (Visa, Master, Napas) hoặc quét ví MoMo, ZaloPay trực tiếp tại quầy thu ngân.");
        lblTipDesc.setWrapText(true);
        lblTipDesc.setAlignment(Pos.CENTER);
        lblTipDesc.setStyle(STYLE_LABEL + " -fx-text-alignment: center;");

        box.getChildren().addAll(lblIcon, lblTipTitle, lblTipDesc);
        return box;
    }

    private HBox createDetailRowPlain(String label, String value) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(label);
        lbl.setStyle(FONT_FAMILY + " -fx-font-size: 11px; -fx-font-weight: 500; -fx-text-fill: #64748B; -fx-min-width: 80px;");

        Label val = new Label(value);
        val.setStyle(FONT_FAMILY + " -fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #1E293B;");

        row.getChildren().addAll(lbl, val);
        return row;
    }

    private HBox createDetailRowWithCopy(String label, String value, String copyValue) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(label);
        lbl.setStyle(FONT_FAMILY + " -fx-font-size: 11px; -fx-font-weight: 500; -fx-text-fill: #64748B; -fx-min-width: 80px;");

        Label val = new Label(value);
        val.setStyle(FONT_FAMILY + " -fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #1E293B;");
        HBox.setHgrow(val, Priority.ALWAYS);

        Button btnCopy = new Button("📋 Chép");
        btnCopy.setStyle(FONT_FAMILY + " -fx-background-color: #E2E8F0; -fx-text-fill: #334155; -fx-font-size: 10px; -fx-font-weight: 700; -fx-background-radius: 4px; -fx-padding: 2 6; -fx-cursor: hand;");
        btnCopy.setOnAction(e -> copyToClipboard(copyValue, btnCopy));

        row.getChildren().addAll(lbl, val, btnCopy);
        return row;
    }

    private void copyToClipboard(String text, Button btn) {
        try {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            clipboard.setContent(content);

            String orig = btn.getText();
            btn.setText("✓ Đã chép");
            btn.setStyle(FONT_FAMILY + " -fx-background-color: #ECFDF5; -fx-text-fill: #065F46; -fx-font-size: 10px; -fx-font-weight: 700; -fx-background-radius: 4px; -fx-padding: 2 6;");

            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.setOnFinished(ev -> {
                btn.setText(orig);
                btn.setStyle(FONT_FAMILY + " -fx-background-color: #E2E8F0; -fx-text-fill: #334155; -fx-font-size: 10px; -fx-font-weight: 700; -fx-background-radius: 4px; -fx-padding: 2 6; -fx-cursor: hand;");
            });
            pause.play();
        } catch (Exception ignored) {}
    }
}
