package cafe.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class LoginView extends StackPane {
    private final TextField txtTable = new TextField();
    private final Button btnLogin = new Button("BẮT ĐẦU GỌI MÓN →");
    private final Label lblError = new Label();
    private final FlowPane quickTablesPane = new FlowPane(8, 8);

    public LoginView() {
        initUI();
    }

    private void initUI() {
        setStyle("-fx-background-color: #FAF7F2;");

        VBox card = new VBox(18);
        card.setMaxWidth(420);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(36, 32, 36, 32));
        card.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-background-radius: 24; " +
            "-fx-border-color: #EADBCE; " +
            "-fx-border-radius: 24; " +
            "-fx-border-width: 1; " +
            "-fx-effect: dropshadow(gaussian, rgba(62, 39, 35, 0.08), 24, 0, 0, 8);"
        );

        // Logo
        StackPane logoBox = new StackPane();
        logoBox.setStyle("-fx-background-color: #F8EDE5; -fx-background-radius: 20;");
        logoBox.setPrefSize(72, 72);
        logoBox.setMaxSize(72, 72);
        Label lblIcon = new Label("☕");
        lblIcon.setStyle("-fx-font-size: 34px;");
        logoBox.getChildren().add(lblIcon);

        Label lblTitle = new Label("L'Amour Artisan Cafe");
        lblTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #2C1810;");

        Label lblSubtitle = new Label("Kính chào Quý khách! Vui lòng chọn hoặc nhập số bàn để trải nghiệm thực đơn.");
        lblSubtitle.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #7D6E68; -fx-text-alignment: center;");
        lblSubtitle.setWrapText(true);

        // Input Field
        VBox inputGroup = new VBox(6);
        inputGroup.setAlignment(Pos.CENTER_LEFT);
        Label lblInputHeader = new Label("Số bàn của bạn:");
        lblInputHeader.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #2C1810;");

        txtTable.setStyle(
            "-fx-background-color: #FAF7F2; " +
            "-fx-background-radius: 12; " +
            "-fx-border-color: #E2D5CA; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1; " +
            "-fx-font-size: 18px; " +
            "-fx-font-weight: 800; " +
            "-fx-alignment: center; " +
            "-fx-pref-height: 48; " +
            "-fx-text-fill: #C97A44;"
        );
        txtTable.setPromptText("Nhập số bàn (VD: 1, 2, 5)");
        inputGroup.getChildren().addAll(lblInputHeader, txtTable);

        // Quick Table Chips
        VBox quickGroup = new VBox(6);
        quickGroup.setAlignment(Pos.CENTER_LEFT);
        Label lblQuick = new Label("Chọn nhanh:");
        lblQuick.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 600; -fx-text-fill: #9E8D84;");
        quickTablesPane.setAlignment(Pos.CENTER);

        for (int i = 1; i <= 8; i++) {
            final int t = i;
            Button chip = new Button("Bàn " + (t < 10 ? "0" + t : t));
            chip.setStyle(
                "-fx-background-color: #F5EEE8; " +
                "-fx-background-radius: 14; " +
                "-fx-text-fill: #5C4840; " +
                "-fx-font-size: 11.5px; " +
                "-fx-font-weight: 600; " +
                "-fx-padding: 4 10 4 10; " +
                "-fx-cursor: hand;"
            );
            chip.setOnAction(e -> txtTable.setText(String.valueOf(t)));
            quickTablesPane.getChildren().add(chip);
        }
        quickGroup.getChildren().addAll(lblQuick, quickTablesPane);

        // Error message
        lblError.setStyle("-fx-text-fill: #D32F2F; -fx-font-size: 12px; -fx-font-weight: 600;");
        lblError.setVisible(false);
        lblError.setManaged(false);

        // Login Button
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setStyle(
            "-fx-background-color: linear-gradient(to right, #C97A44, #B86733); " +
            "-fx-background-radius: 14; " +
            "-fx-text-fill: #FFFFFF; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: 800; " +
            "-fx-pref-height: 46; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(201, 122, 68, 0.35), 10, 0, 0, 4);"
        );

        card.getChildren().addAll(logoBox, lblTitle, lblSubtitle, inputGroup, quickGroup, lblError, btnLogin);
        getChildren().add(card);
    }

    public TextField getTableField() { return txtTable; }
    public Button getLoginButton() { return btnLogin; }

    public void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    public void clearError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }
}
