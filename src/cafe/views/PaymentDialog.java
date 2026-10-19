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
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * PaymentDialog - Màn hình Thanh toán của user (Screen 6 trong UI_mong_muon.png):
 * - Header: Nút back ←, Tiêu đề "Thanh toán", Tag "Bàn 05"
 * - 3 thẻ phương thức: Tiền mặt (đang chọn với viền #112823 & dấu tick ✔), Thẻ ngân hàng (>), Ví điện tử (>)
 * - Bảng tính tiền tối giản: Tổng tiền, Giảm giá, Thành tiền (16px bold)
 * - Nút "Xác nhận thanh toán" (#112823, cao 40px, radius 5px)
 */
public class PaymentDialog extends Stage {
    private final int tableNumber;
    private final CartModel cartModel;
    private final Consumer<String> onConfirmed;

    private String selectedMethod = "TIỀN MẶT";
    private final List<HBox> methodCards = new ArrayList<>();

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
        VBox root = new VBox(14);
        root.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16px; -fx-border-color: #E2DDD5; -fx-border-radius: 16px; -fx-border-width: 1px; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.22), 24, 0.12, 0, 8);");
        root.setPadding(new Insets(20, 22, 20, 22));
        root.setPrefWidth(460);
        root.setMaxWidth(480);
        root.setPrefHeight(600);

        // Entrance animation
        AnimationHelper.playDialogEntrance(root);

        // 1. Header: Back ←, "Thanh toán", Badge Bàn 05, Nút đóng [X]
        HBox headerRow = new HBox(10);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setStyle("-fx-cursor: move; -fx-padding: 0 0 4 0;");
        cafe.utils.WindowChromeHelper.makeDraggable(this, headerRow);

        Button btnBack = new Button("←");
        btnBack.setStyle("-fx-background-color: #F5EFEB; -fx-text-fill: #292722; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 14px; -fx-min-width: 28px; -fx-min-height: 28px; -fx-pref-width: 28px; -fx-pref-height: 28px; -fx-padding: 0; -fx-alignment: center;");
        btnBack.setOnMouseEntered(e -> btnBack.setStyle("-fx-background-color: #EAE4DA; -fx-text-fill: #292722; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 14px; -fx-min-width: 28px; -fx-min-height: 28px; -fx-pref-width: 28px; -fx-pref-height: 28px; -fx-padding: 0; -fx-alignment: center;"));
        btnBack.setOnMouseExited(e -> btnBack.setStyle("-fx-background-color: #F5EFEB; -fx-text-fill: #292722; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 14px; -fx-min-width: 28px; -fx-min-height: 28px; -fx-pref-width: 28px; -fx-pref-height: 28px; -fx-padding: 0; -fx-alignment: center;"));
        btnBack.setOnMousePressed(javafx.event.Event::consume);
        btnBack.setOnAction(e -> close());

        Label lblTitle = new Label("Thanh toán");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label lblTableBadge = new Label("Bàn " + (tableNumber < 10 ? "0" + tableNumber : tableNumber));
        lblTableBadge.setStyle("-fx-background-color: #EBE5DB; -fx-text-fill: #563521; -fx-font-size: 11.5px; -fx-font-weight: 700; -fx-background-radius: 5px; -fx-padding: 3 9 3 9;");

        Button btnClose = new Button();
        btnClose.setGraphic(cafe.utils.IconHelper.getIcon("client", "dong-cua-so.png", SvgIcons.CLOSE, "✕", 13, Color.web("#563521")));
        btnClose.setStyle("-fx-background-color: #F5EFEB; -fx-background-radius: 14px; -fx-min-width: 28px; -fx-min-height: 28px; -fx-pref-width: 28px; -fx-pref-height: 28px; -fx-padding: 0; -fx-cursor: hand; -fx-alignment: center;");
        btnClose.setOnMouseEntered(e -> btnClose.setStyle("-fx-background-color: #EAE4DA; -fx-background-radius: 14px; -fx-min-width: 28px; -fx-min-height: 28px; -fx-pref-width: 28px; -fx-pref-height: 28px; -fx-padding: 0; -fx-cursor: hand; -fx-alignment: center;"));
        btnClose.setOnMouseExited(e -> btnClose.setStyle("-fx-background-color: #F5EFEB; -fx-background-radius: 14px; -fx-min-width: 28px; -fx-min-height: 28px; -fx-pref-width: 28px; -fx-pref-height: 28px; -fx-padding: 0; -fx-cursor: hand; -fx-alignment: center;"));
        btnClose.setOnMousePressed(javafx.event.Event::consume);
        btnClose.setOnAction(e -> close());

        headerRow.getChildren().addAll(btnBack, lblTitle, sp, lblTableBadge, btnClose);

        // 2. Section Title
        Label lblSection = new Label("Phương thức thanh toán");
        lblSection.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #112823;");

        // 3. Three Payment Method Cards (nạp icon PNG mới)
        VBox methodsBox = new VBox(10);

        HBox cardCash = createMethodCard("Tiền mặt tại quầy", "tien-mat.png", SvgIcons.CASH, "💵", true, "TIỀN MẶT");
        HBox cardQR = createMethodCard("Quét mã VietQR", "thanh-toan-qr.png", SvgIcons.CREDIT_CARD, "📱", false, "VIETQR");
        HBox cardWallet = createMethodCard("Ví điện tử / Thẻ", "thanh-toan.png", SvgIcons.WALLET, "💳", false, "VÍ ĐIỆN TỬ");

        methodCards.add(cardCash);
        methodCards.add(cardQR);
        methodCards.add(cardWallet);

        methodsBox.getChildren().addAll(cardCash, cardQR, cardWallet);

        // 4. Breakdown Summary Box
        VBox summaryBox = new VBox(8);
        summaryBox.setPadding(new Insets(10, 0, 4, 0));

        HBox rowSub = new HBox();
        Label lblSubT = new Label("Tổng tiền");
        lblSubT.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #817A70;");
        Region spSub = new Region();
        HBox.setHgrow(spSub, Priority.ALWAYS);
        Label lblSubV = new Label(CurrencyFormatter.format(cartModel.getSubtotalAmount()));
        lblSubV.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        rowSub.getChildren().addAll(lblSubT, spSub, lblSubV);

        HBox rowDisc = new HBox();
        Label lblDiscT = new Label("Giảm giá");
        lblDiscT.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #817A70;");
        Region spDisc = new Region();
        HBox.setHgrow(spDisc, Priority.ALWAYS);
        Label lblDiscV = new Label(cartModel.getDiscountAmount() > 0 ? "-" + CurrencyFormatter.format(cartModel.getDiscountAmount()) : "0đ");
        lblDiscV.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: " + (cartModel.getDiscountAmount() > 0 ? "#112823" : "#817A70") + ";");
        rowDisc.getChildren().addAll(lblDiscT, spDisc, lblDiscV);

        HBox rowTotal = new HBox();
        rowTotal.setAlignment(Pos.CENTER_LEFT);
        rowTotal.setPadding(new Insets(6, 0, 0, 0));
        rowTotal.setStyle("-fx-border-color: #EAE4DA transparent transparent transparent; -fx-border-width: 1 0 0 0;");
        Label lblTotalT = new Label("Thành tiền");
        lblTotalT.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        Region spTotal = new Region();
        HBox.setHgrow(spTotal, Priority.ALWAYS);
        Label lblTotalV = new Label(CurrencyFormatter.format(cartModel.getTotalAmount()));
        lblTotalV.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        rowTotal.getChildren().addAll(lblTotalT, spTotal, lblTotalV);

        summaryBox.getChildren().addAll(rowSub, rowDisc, rowTotal);

        // 5. Confirm CTA Button
        Button btnConfirm = new Button();
        btnConfirm.setMaxWidth(Double.MAX_VALUE);
        btnConfirm.setPrefHeight(46);
        btnConfirm.setStyle("-fx-background-color: #112823; -fx-background-radius: 6px; -fx-cursor: hand;");
        AnimationHelper.applyButtonPress(btnConfirm);

        HBox confirmGraphic = new HBox(8);
        confirmGraphic.setAlignment(Pos.CENTER);
        Node confirmIcon = cafe.utils.IconHelper.getIcon("client", "dong-y-dat.png", null, "✔", 20, Color.WHITE);
        Label lblConfirm = new Label("Xác nhận thanh toán");
        lblConfirm.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 14px; -fx-font-weight: 700;");
        confirmGraphic.getChildren().addAll(confirmIcon, lblConfirm);
        btnConfirm.setGraphic(confirmGraphic);

        btnConfirm.setOnAction(e -> {
            if (onConfirmed != null) {
                onConfirmed.accept(selectedMethod);
            }
            close();
        });

        root.getChildren().addAll(headerRow, lblSection, methodsBox, summaryBox, btnConfirm);

        StackPane sceneRoot = new StackPane(root);
        sceneRoot.setStyle("-fx-background-color: transparent;");
        sceneRoot.setPadding(new Insets(16));

        Scene scene = new Scene(sceneRoot);
        scene.setFill(Color.TRANSPARENT);
        cafe.utils.WindowChromeHelper.applyTheme(scene);

        setScene(scene);
    }

    private HBox createMethodCard(String title, String iconFileName, String iconSvg, String fallbackEmoji, boolean isSelected, String methodKey) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12, 14, 12, 14));
        card.setCursor(javafx.scene.Cursor.HAND);
        AnimationHelper.applyButtonPress(card);

        Node icon = cafe.utils.IconHelper.getIcon("client", iconFileName, iconSvg, fallbackEmoji, 22, Color.web("#112823"));
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #1C1917;");
        HBox.setHgrow(lblTitle, Priority.ALWAYS);

        card.getChildren().addAll(icon, lblTitle);

        if (isSelected) {
            Label checkmark = new Label("✔");
            checkmark.setStyle("-fx-font-size: 13px; -fx-text-fill: #112823; -fx-font-weight: 800;");
            card.getChildren().add(checkmark);
            card.setStyle("-fx-background-color: #F8F4EE; -fx-background-radius: 8px; -fx-border-color: #112823; -fx-border-radius: 8px; -fx-border-width: 1.5px;");
        } else {
            Label chevron = new Label("›");
            chevron.setStyle("-fx-font-size: 16px; -fx-text-fill: #817A70;");
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
                        rightIcon.setStyle("-fx-font-size: 13px; -fx-text-fill: #112823; -fx-font-weight: 800;");
                        c.setStyle("-fx-background-color: #F8F4EE; -fx-background-radius: 8px; -fx-border-color: #112823; -fx-border-radius: 8px; -fx-border-width: 1.5px;");
                    } else {
                        rightIcon.setText("›");
                        rightIcon.setStyle("-fx-font-size: 16px; -fx-text-fill: #817A70;");
                        c.setStyle("-fx-background-color: #FDFBF8; -fx-background-radius: 8px; -fx-border-color: #E6E0D8; -fx-border-radius: 8px; -fx-border-width: 1px;");
                    }
                }
            }
        });

        return card;
    }
}
