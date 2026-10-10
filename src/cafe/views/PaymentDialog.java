package cafe.views;

import cafe.models.CartModel;
import cafe.utils.AnimationHelper;
import cafe.utils.CurrencyFormatter;
import cafe.utils.SvgIcons;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
        initStyle(StageStyle.UNDECORATED);
        setTitle("Thanh Toán • Bàn " + tableNumber);
        setResizable(false);

        initUI();
    }

    private void initUI() {
        VBox root = new VBox(12);
        root.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12px; -fx-border-color: #E7E2DA; -fx-border-radius: 12px; -fx-border-width: 1px; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.18), 24, 0.2, 0, 8);");
        root.setPadding(new Insets(18, 20, 18, 20));
        root.setPrefWidth(380);

        // Entrance animation
        AnimationHelper.playDialogEntrance(root);

        // 1. Header: Back ←, "Thanh toán", Badge Bàn 05
        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        cafe.utils.WindowChromeHelper.makeDraggable(this, headerRow);

        Button btnBack = new Button("←");
        btnBack.setStyle("-fx-background-color: transparent; -fx-text-fill: #292722; -fx-font-size: 15px; -fx-cursor: hand; -fx-padding: 0 4 0 0;");
        btnBack.setOnAction(e -> close());

        Label lblTitle = new Label("Thanh toán");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 17px; -fx-font-weight: 700; -fx-text-fill: #292722;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label lblTableBadge = new Label("Bàn " + (tableNumber < 10 ? "0" + tableNumber : tableNumber));
        lblTableBadge.setStyle("-fx-background-color: #EBE5DB; -fx-text-fill: #563521; -fx-font-size: 10.5px; -fx-font-weight: 700; -fx-background-radius: 4px; -fx-padding: 3 8 3 8;");

        headerRow.getChildren().addAll(btnBack, lblTitle, sp, lblTableBadge);

        // 2. Section Title
        Label lblSection = new Label("Phương thức thanh toán");
        lblSection.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #292722;");

        // 3. Three Payment Method Cards
        VBox methodsBox = new VBox(8);

        HBox cardCash = createMethodCard("Tiền mặt", SvgIcons.CASH, true, "TIỀN MẶT");
        HBox cardBank = createMethodCard("Thẻ ngân hàng", SvgIcons.CREDIT_CARD, false, "THẺ NGÂN HÀNG");
        HBox cardWallet = createMethodCard("Ví điện tử", SvgIcons.WALLET, false, "VÍ ĐIỆN TỬ");

        methodCards.add(cardCash);
        methodCards.add(cardBank);
        methodCards.add(cardWallet);

        methodsBox.getChildren().addAll(cardCash, cardBank, cardWallet);

        // 4. Breakdown Summary Box
        VBox summaryBox = new VBox(6);
        summaryBox.setPadding(new Insets(10, 0, 4, 0));

        HBox rowSub = new HBox();
        Label lblSubT = new Label("Tổng tiền");
        lblSubT.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #817A70;");
        Region spSub = new Region();
        HBox.setHgrow(spSub, Priority.ALWAYS);
        Label lblSubV = new Label(CurrencyFormatter.format(cartModel.getSubtotalAmount()));
        lblSubV.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 600; -fx-text-fill: #292722;");
        rowSub.getChildren().addAll(lblSubT, spSub, lblSubV);

        HBox rowDisc = new HBox();
        Label lblDiscT = new Label("Giảm giá");
        lblDiscT.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #817A70;");
        Region spDisc = new Region();
        HBox.setHgrow(spDisc, Priority.ALWAYS);
        Label lblDiscV = new Label(cartModel.getDiscountAmount() > 0 ? "-" + CurrencyFormatter.format(cartModel.getDiscountAmount()) : "0đ");
        lblDiscV.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 600; -fx-text-fill: " + (cartModel.getDiscountAmount() > 0 ? "#112823" : "#817A70") + ";");
        rowDisc.getChildren().addAll(lblDiscT, spDisc, lblDiscV);

        HBox rowTotal = new HBox();
        rowTotal.setAlignment(Pos.CENTER_LEFT);
        rowTotal.setPadding(new Insets(4, 0, 0, 0));
        Label lblTotalT = new Label("Thành tiền");
        lblTotalT.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        Region spTotal = new Region();
        HBox.setHgrow(spTotal, Priority.ALWAYS);
        Label lblTotalV = new Label(CurrencyFormatter.format(cartModel.getTotalAmount()));
        lblTotalV.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #292722;");
        rowTotal.getChildren().addAll(lblTotalT, spTotal, lblTotalV);

        summaryBox.getChildren().addAll(rowSub, rowDisc, rowTotal);

        // 5. Confirm CTA Button (#112823, cao 40px, radius 5px)
        Button btnConfirm = new Button("Xác nhận thanh toán");
        btnConfirm.setMaxWidth(Double.MAX_VALUE);
        btnConfirm.setPrefHeight(40);
        btnConfirm.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: 600; -fx-background-radius: 5px; -fx-cursor: hand;");
        AnimationHelper.applyButtonPress(btnConfirm);

        btnConfirm.setOnAction(e -> {
            if (onConfirmed != null) {
                onConfirmed.accept(selectedMethod);
            }
            close();
        });

        root.getChildren().addAll(headerRow, lblSection, methodsBox, summaryBox, btnConfirm);

        Scene scene = new Scene(root);
        cafe.utils.WindowChromeHelper.applyTheme(scene);

        setScene(scene);
    }

    private HBox createMethodCard(String title, String iconSvg, boolean isSelected, String methodKey) {
        HBox card = new HBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setCursor(javafx.scene.Cursor.HAND);
        AnimationHelper.applyButtonPress(card);

        SVGPath icon = SvgIcons.createOutlineIcon(iconSvg, 0.85, Color.web("#292722"));
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 600; -fx-text-fill: #292722;");
        HBox.setHgrow(lblTitle, Priority.ALWAYS);

        card.getChildren().addAll(icon, lblTitle);

        if (isSelected) {
            Label checkmark = new Label("✔");
            checkmark.setStyle("-fx-font-size: 11px; -fx-text-fill: #112823; -fx-font-weight: 700;");
            card.getChildren().add(checkmark);
            card.setStyle("-fx-background-color: #FBF9F5; -fx-background-radius: 5px; -fx-border-color: #112823; -fx-border-radius: 5px; -fx-border-width: 1.5px;");
        } else {
            Label chevron = new Label("›");
            chevron.setStyle("-fx-font-size: 14px; -fx-text-fill: #817A70;");
            card.getChildren().add(chevron);
            card.setStyle("-fx-background-color: #FBF9F5; -fx-background-radius: 5px; -fx-border-color: #DED7CD; -fx-border-radius: 5px; -fx-border-width: 1px;");
        }

        card.setOnMouseClicked(e -> {
            selectedMethod = methodKey;
            for (HBox c : methodCards) {
                boolean active = (c == card);
                if (c.getChildren().size() > 2) {
                    Label rightIcon = (Label) c.getChildren().get(2);
                    if (active) {
                        rightIcon.setText("✔");
                        rightIcon.setStyle("-fx-font-size: 11px; -fx-text-fill: #112823; -fx-font-weight: 700;");
                        c.setStyle("-fx-background-color: #FBF9F5; -fx-background-radius: 5px; -fx-border-color: #112823; -fx-border-radius: 5px; -fx-border-width: 1.5px;");
                    } else {
                        rightIcon.setText("›");
                        rightIcon.setStyle("-fx-font-size: 14px; -fx-text-fill: #817A70;");
                        c.setStyle("-fx-background-color: #FBF9F5; -fx-background-radius: 5px; -fx-border-color: #DED7CD; -fx-border-radius: 5px; -fx-border-width: 1px;");
                    }
                }
            }
        });

        return card;
    }
}
