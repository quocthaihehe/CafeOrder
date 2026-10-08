package cafe.views;

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

/**
 * OrderSuccessDialog - Calm, editorial order confirmation slip dialog.
 * Refined per CafeOrder Animation & UI Plan:
 * - 12px rounded canvas with sheet entrance animation via AnimationHelper.playDialogEntrance
 * - Crisp SVG checkmark icon
 * - Clear order number, table, status, and back-to-menu action with tactile press
 */
public class OrderSuccessDialog extends Stage {
    private final int orderId;
    private final int tableNumber;
    private final double totalAmount;
    private final String paymentMethod;
    private final Runnable onDismiss;

    public OrderSuccessDialog(Stage parentStage, int orderId, int tableNumber, double totalAmount, String paymentMethod, Runnable onDismiss) {
        this.orderId = orderId;
        this.tableNumber = tableNumber;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod != null ? paymentMethod : "TIỀN MẶT";
        this.onDismiss = onDismiss;

        initOwner(parentStage);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.UTILITY);
        setTitle("Xác Nhận Đơn Hàng • Brew & Bean CAFE");
        setResizable(false);

        initUI();
    }

    private void initUI() {
        VBox root = new VBox(16);
        root.getStyleClass().add("modal-canvas");
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(28, 32, 28, 32));
        root.setPrefWidth(420);

        // Entrance animation
        AnimationHelper.playDialogEntrance(root);

        // Subtle Icon / Checkmark with SVG
        StackPane iconCircle = new StackPane();
        iconCircle.setStyle("-fx-background-color: #EDF7ED; -fx-background-radius: 24px; -fx-min-width: 48px; -fx-min-height: 48px; -fx-max-width: 48px; -fx-max-height: 48px;");
        SVGPath checkIcon = SvgIcons.createOutlineIcon(SvgIcons.CHECK, 1.1, Color.web("#183C32"));
        iconCircle.getChildren().add(checkIcon);

        // Titles
        Label lblTitle = new Label("ĐƠN HÀNG ĐÃ XÁC NHẬN");
        lblTitle.getStyleClass().add("editorial-title");

        Label lblSub = new Label("Quầy pha chế đang chuẩn bị thức uống của bạn.");
        lblSub.getStyleClass().add("editorial-subtitle");

        // Receipt Slip (10px radius)
        VBox slip = new VBox(10);
        slip.getStyleClass().add("receipt-slip");
        slip.setAlignment(Pos.CENTER);
        slip.setMaxWidth(Double.MAX_VALUE);

        Label lblOrderTag = new Label("MÃ ĐƠN #" + orderId + " • BÀN " + (tableNumber < 10 ? "0" + tableNumber : tableNumber));
        lblOrderTag.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #183C32;");

        Region sep = new Region();
        sep.setStyle("-fx-background-color: #E6DFD4; -fx-pref-height: 1px;");

        HBox rowMethod = new HBox();
        Label lblMText = new Label("Hình thức:");
        lblMText.setStyle("-fx-font-size: 12px; -fx-text-fill: #817B70;");
        Region sp1 = new Region();
        HBox.setHgrow(sp1, Priority.ALWAYS);
        Label lblMVal = new Label(paymentMethod);
        lblMVal.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        rowMethod.getChildren().addAll(lblMText, sp1, lblMVal);

        HBox rowTotal = new HBox();
        Label lblTText = new Label("Tổng cộng:");
        lblTText.setStyle("-fx-font-size: 12px; -fx-text-fill: #817B70;");
        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);
        Label lblTVal = new Label(CurrencyFormatter.format(totalAmount));
        lblTVal.setStyle("-fx-font-size: 14.5px; -fx-font-weight: 800; -fx-text-fill: #A66A42;");
        rowTotal.getChildren().addAll(lblTText, sp2, lblTVal);

        slip.getChildren().addAll(lblOrderTag, sep, rowMethod, rowTotal);

        // Back to Menu Button
        Button btnBack = new Button("Quay lại Menu thực đơn");
        btnBack.getStyleClass().add("btn-primary");
        btnBack.setMaxWidth(Double.MAX_VALUE);
        AnimationHelper.applyButtonPress(btnBack);
        btnBack.setOnAction(e -> {
            close();
            if (onDismiss != null) {
                onDismiss.run();
            }
        });

        root.getChildren().addAll(iconCircle, lblTitle, lblSub, slip, btnBack);

        Scene scene = new Scene(root);
        try {
            scene.getStylesheets().add(getClass().getResource("/cafe/resources/css/cafe-theme.css").toExternalForm());
        } catch (Exception ignored) {}

        setScene(scene);
    }
}
