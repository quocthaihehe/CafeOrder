package cafe.views;

import cafe.models.CartModel;
import cafe.utils.AnimationHelper;
import cafe.utils.CurrencyFormatter;
import cafe.utils.SvgIcons;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * PaymentDialog - Hộp thoại Thanh toán chuẩn hóa:
 * - Thiết kế 2 cột cân đối, hiển thị trọn vẹn 100% nội dung trên 1 màn hình duy nhất, KHÔNG CẦN CUỘN.
 * - Tích hợp mã VietQR MB Bank sắc nét ở cột bên phải.
 * - Chuẩn hóa toàn bộ số và kiểu chữ đồng nhất (Segoe UI / Inter, chống số to số nhỏ).
 */
public class PaymentDialog extends Stage {
    private static final String FONT_FAMILY = "-fx-font-family: 'Segoe UI', 'Inter', -apple-system, sans-serif;";
    private static final String STYLE_LABEL = FONT_FAMILY + " -fx-font-size: 12.5px; -fx-font-weight: 500; -fx-text-fill: #57534E;";
    private static final String STYLE_VALUE = FONT_FAMILY + " -fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #1C1917;";
    private static final String STYLE_TOTAL_LABEL = FONT_FAMILY + " -fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #1C1917;";
    private static final String STYLE_TOTAL_VALUE = FONT_FAMILY + " -fx-font-size: 17px; -fx-font-weight: 800; -fx-text-fill: #112823;";

    private final int tableNumber;
    private final CartModel cartModel;
    private final Consumer<String> onConfirmed;

    private String selectedMethod = "VIETQR"; // Mặc định chọn VietQR để thấy ngay mã QR
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
        root.setPrefWidth(600);
        root.setMaxWidth(620);
        root.setPrefHeight(470);
        root.setMaxHeight(490);

        AnimationHelper.playDialogEntrance(root);

        // 1. Header: Back ←, "Thanh toán", Badge Bàn 05, Nút đóng [X]
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

        String tblStr = "Bàn " + (tableNumber < 10 ? "0" + tableNumber : tableNumber);
        Label lblTableBadge = new Label(tblStr);
        lblTableBadge.setStyle(FONT_FAMILY + " -fx-background-color: #EBE5DB; -fx-text-fill: #563521; -fx-font-size: 11.5px; -fx-font-weight: 700; -fx-background-radius: 6px; -fx-padding: 3 9 3 9;");

        Button btnClose = new Button();
        btnClose.setGraphic(cafe.utils.IconHelper.getIcon("client", "dong-cua-so.png", SvgIcons.CLOSE, "✕", 12, Color.web("#563521")));
        btnClose.setStyle("-fx-background-color: #F5EFEB; -fx-background-radius: 14px; -fx-min-width: 28px; -fx-min-height: 28px; -fx-padding: 0; -fx-cursor: hand; -fx-alignment: center;");
        btnClose.setOnAction(e -> close());

        headerRow.getChildren().addAll(btnBack, lblTitle, spHeader, lblTableBadge, btnClose);

        // 2. Main 2-Column Body Layout (Cột trái: Chọn phương thức & Tổng tiền | Cột phải: Khung QR & Chi tiết)
        HBox mainColumns = new HBox(16);
        mainColumns.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(mainColumns, Priority.ALWAYS);

        // === CỘT TRÁI (260px) ===
        VBox leftCol = new VBox(10);
        leftCol.setPrefWidth(265);
        leftCol.setMinWidth(260);

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
        lblDiscV.setStyle(cartModel.getDiscountAmount() > 0 ? (FONT_FAMILY + " -fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #15803D;") : STYLE_VALUE);
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

        // === CỘT PHẢI (Chi tiết VietQR / Hướng dẫn thanh toán) ===
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
     * Khung hiển thị chi tiết mã QR VietQR (MB Bank) trơn tru, không thanh cuộn
     */
    private VBox buildVietQrDetailPanel() {
        VBox qrBox = new VBox(7);
        qrBox.setAlignment(Pos.CENTER);
        qrBox.setPadding(new Insets(10, 14, 10, 14));
        qrBox.setStyle("-fx-background-color: #FBF9F5; -fx-background-radius: 12px; -fx-border-color: #E2DDD5; -fx-border-radius: 12px; -fx-border-width: 1px;");
        VBox.setVgrow(qrBox, Priority.ALWAYS);

        // Khung ảnh QR code MB Bank
        StackPane qrImageWrapper = new StackPane();
        qrImageWrapper.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8px; -fx-padding: 6; -fx-border-color: #E8E2D8; -fx-border-radius: 8px; -fx-border-width: 1px;");

        ImageView qrView = new ImageView();
        try {
            InputStream is = getClass().getResourceAsStream("/cafe/resources/images/vietqr_mb.png");
            if (is != null) {
                Image img = new Image(is);
                qrView.setImage(img);
                qrView.setFitWidth(140);
                qrView.setPreserveRatio(true);
                qrView.setSmooth(true);
            }
        } catch (Exception ignored) {}

        Rectangle clip = new Rectangle(140, 172);
        clip.setArcWidth(8);
        clip.setArcHeight(8);
        qrView.setClip(clip);
        qrImageWrapper.getChildren().add(qrView);

        // Bảng thông tin thanh toán chuẩn hóa font
        VBox infoGrid = new VBox(3);
        infoGrid.setPadding(new Insets(2, 6, 2, 6));

        String memoText = "BAN " + (tableNumber < 10 ? "0" + tableNumber : tableNumber) + " CAFE";
        infoGrid.getChildren().addAll(
            createDetailRow("Ngân hàng:", "MB Bank (Quân Đội)"),
            createDetailRow("Chủ tài khoản:", "BREW & BEAN SPECIALTY"),
            createDetailRow("Số tài khoản:", "0909 888 999"),
            createDetailRow("Số tiền chuyển:", CurrencyFormatter.format(cartModel.getTotalAmount())),
            createDetailRow("Nội dung CK:", memoText)
        );

        Label lblGuide = new Label("Mở app Ngân hàng / Ví điện tử bất kỳ để quét mã.");
        lblGuide.setStyle(FONT_FAMILY + " -fx-font-size: 11px; -fx-text-fill: #78716C; -fx-font-style: italic;");

        qrBox.getChildren().addAll(qrImageWrapper, infoGrid, lblGuide);
        return qrBox;
    }

    private VBox buildCashDetailPanel() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20, 16, 20, 16));
        box.setStyle("-fx-background-color: #FBF9F5; -fx-background-radius: 12px; -fx-border-color: #E2DDD5; -fx-border-radius: 12px; -fx-border-width: 1px;");
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
        box.setStyle("-fx-background-color: #FBF9F5; -fx-background-radius: 12px; -fx-border-color: #E2DDD5; -fx-border-radius: 12px; -fx-border-width: 1px;");
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

    private HBox createDetailRow(String label, String value) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(label);
        lbl.setStyle(FONT_FAMILY + " -fx-font-size: 11.5px; -fx-font-weight: 500; -fx-text-fill: #57534E; -fx-min-width: 95px;");

        Label val = new Label(value);
        val.setStyle(FONT_FAMILY + " -fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #1C1917;");

        row.getChildren().addAll(lbl, val);
        return row;
    }
}
