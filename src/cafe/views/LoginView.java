package cafe.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

import java.net.URL;

/**
 * LoginView - Màn hình đăng nhập / Chọn bàn theo thiết kế Screen 1 (UI_mong_muon.png)
 * Bố cục Split Screen 50/50:
 * - Cột trái: Poster nghệ thuật "Good Coffee Better Moments" với nhận diện Brew & Bean.
 * - Cột phải: Form đăng nhập / Chọn bàn tối giản, nút Primary xanh thẫm (#183C32).
 */
public class LoginView extends HBox {

    private final TextField txtTable = new TextField();
    private final Button btnLogin = new Button("Đăng nhập");
    private final Label lblError = new Label();
    private final FlowPane quickTablesPane = new FlowPane(8, 8);

    public LoginView() {
        initUI();
    }

    private void initUI() {
        setStyle("-fx-background-color: #F4F0E8;");
        setAlignment(Pos.CENTER);

        // =====================================================================
        // 1. CỘT TRÁI: POSTER NGHỆ THUẬT (HERO BANNER)
        // =====================================================================
        StackPane leftHeroPane = new StackPane();
        leftHeroPane.setPrefWidth(420);
        leftHeroPane.setMinWidth(360);
        leftHeroPane.setMaxWidth(460);
        HBox.setHgrow(leftHeroPane, Priority.ALWAYS);

        URL heroUrl = getClass().getResource("/cafe/resources/images/login_hero.png");
        if (heroUrl != null) {
            Image heroImg = new Image(heroUrl.toExternalForm());
            ImageView heroView = new ImageView(heroImg);
            heroView.setPreserveRatio(true);
            heroView.setSmooth(true);
            heroView.fitWidthProperty().bind(leftHeroPane.widthProperty());
            heroView.fitHeightProperty().bind(leftHeroPane.heightProperty());

            Rectangle clip = new Rectangle();
            clip.widthProperty().bind(leftHeroPane.widthProperty());
            clip.heightProperty().bind(leftHeroPane.heightProperty());
            leftHeroPane.setClip(clip);

            leftHeroPane.getChildren().add(heroView);
        } else {
            leftHeroPane.setStyle("-fx-background-color: #183C32;");
        }

        // =====================================================================
        // 2. CỘT PHẢI: FORM ĐĂNG NHẬP / CHỌN BÀN
        // =====================================================================
        VBox rightFormPane = new VBox(20);
        rightFormPane.setAlignment(Pos.CENTER_LEFT);
        rightFormPane.setPadding(new Insets(40, 50, 40, 50));
        rightFormPane.setPrefWidth(460);
        rightFormPane.setMinWidth(400);
        rightFormPane.setStyle("-fx-background-color: #FBF9F4;");
        HBox.setHgrow(rightFormPane, Priority.ALWAYS);

        // Icon tách cà phê tối giản
        Label iconBean = new Label("☕");
        iconBean.setStyle("-fx-font-size: 28px; -fx-text-fill: #183C32;");

        // Tiêu đề & Lời chào
        VBox titleBox = new VBox(6);
        Label lblWelcome = new Label("Chào mừng trở lại!");
        lblWelcome.setStyle("-fx-font-family: 'Playfair Display', 'Georgia', serif; -fx-font-size: 26px; -fx-font-weight: 700; -fx-text-fill: #292722;");

        Label lblDesc = new Label("Đăng nhập để bắt đầu phục vụ khách hàng.");
        lblDesc.setStyle("-fx-font-size: 13px; -fx-text-fill: #817B70;");
        titleBox.getChildren().addAll(lblWelcome, lblDesc);

        // Input số bàn
        VBox inputGroup = new VBox(8);
        Label lblTableTitle = new Label("Số bàn phục vụ:");
        lblTableTitle.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #292722;");

        txtTable.setPromptText("Nhập số bàn (VD: 1, 2, 5...)");
        txtTable.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-background-radius: 8; " +
            "-fx-border-color: #D8D1C3; " +
            "-fx-border-radius: 8; " +
            "-fx-border-width: 1; " +
            "-fx-padding: 10 14 10 14; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: 700; " +
            "-fx-text-fill: #183C32;"
        );
        txtTable.setPrefHeight(44);
        inputGroup.getChildren().addAll(lblTableTitle, txtTable);

        // Quick Table Chips
        VBox quickBox = new VBox(8);
        Label lblQuick = new Label("Hoặc chọn nhanh bàn có sẵn:");
        lblQuick.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 600; -fx-text-fill: #817B70;");

        quickTablesPane.getChildren().clear();
        for (int i = 1; i <= 8; i++) {
            final int t = i;
            Button chip = new Button("Bàn " + (t < 10 ? "0" + t : t));
            chip.setStyle(
                "-fx-background-color: #FFFFFF; " +
                "-fx-background-radius: 8; " +
                "-fx-border-color: #D8D1C3; " +
                "-fx-border-radius: 8; " +
                "-fx-border-width: 1; " +
                "-fx-text-fill: #292722; " +
                "-fx-font-size: 11.5px; " +
                "-fx-font-weight: 600; " +
                "-fx-padding: 5 11 5 11; " +
                "-fx-cursor: hand;"
            );
            chip.setOnAction(e -> {
                txtTable.setText(String.valueOf(t));
                clearError();
            });
            quickTablesPane.getChildren().add(chip);
        }
        quickBox.getChildren().addAll(lblQuick, quickTablesPane);

        // Thông báo lỗi
        lblError.setStyle("-fx-text-fill: #C62828; -fx-font-size: 12px; -fx-font-weight: 600;");
        lblError.setVisible(false);
        lblError.setManaged(false);

        // Nút Đăng nhập chính (Primary Button Dark Emerald)
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setPrefHeight(46);
        btnLogin.setStyle(
            "-fx-background-color: #183C32; " +
            "-fx-background-radius: 8; " +
            "-fx-text-fill: #FFFFFF; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: 700; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(24, 60, 50, 0.25), 6, 0, 0, 2);"
        );
        btnLogin.setOnMouseEntered(e -> btnLogin.setStyle("-fx-background-color: #102C25; -fx-background-radius: 8; -fx-text-fill: #FFFFFF; -fx-font-size: 14px; -fx-font-weight: 700; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(16, 44, 37, 0.35), 8, 0, 0, 3);"));
        btnLogin.setOnMouseExited(e -> btnLogin.setStyle("-fx-background-color: #183C32; -fx-background-radius: 8; -fx-text-fill: #FFFFFF; -fx-font-size: 14px; -fx-font-weight: 700; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(24, 60, 50, 0.25), 6, 0, 0, 2);"));

        // Nút phân quyền nhanh (Nhân viên / Quản lý) theo mockup Screen 1
        HBox roleBox = new HBox(12);
        roleBox.setAlignment(Pos.CENTER);
        Label lblOr = new Label("Hoặc đăng nhập bằng");
        lblOr.setStyle("-fx-font-size: 11px; -fx-text-fill: #A8A297;");

        HBox roleButtons = new HBox(10);
        roleButtons.setAlignment(Pos.CENTER);
        Button btnStaff = new Button("👤 Nhân viên");
        btnStaff.setStyle("-fx-background-color: #E8E2D8; -fx-text-fill: #292722; -fx-font-size: 12px; -fx-font-weight: 600; -fx-background-radius: 8; -fx-padding: 7 16 7 16; -fx-cursor: hand;");
        btnStaff.setOnAction(e -> txtTable.setText("1"));

        Button btnManager = new Button("💼 Quản lý");
        btnManager.setStyle("-fx-background-color: #E8E2D8; -fx-text-fill: #292722; -fx-font-size: 12px; -fx-font-weight: 600; -fx-background-radius: 8; -fx-padding: 7 16 7 16; -fx-cursor: hand;");
        btnManager.setOnAction(e -> txtTable.setText("5"));
        roleButtons.getChildren().addAll(btnStaff, btnManager);

        // Footer thương hiệu
        Label lblFooter = new Label("Brew & Bean © 2026");
        lblFooter.setStyle("-fx-font-size: 11px; -fx-text-fill: #A8A297; -fx-padding: 10 0 0 0;");

        rightFormPane.getChildren().addAll(
            iconBean,
            titleBox,
            inputGroup,
            quickBox,
            lblError,
            btnLogin,
            new Separator(),
            lblOr,
            roleButtons,
            lblFooter
        );

        getChildren().addAll(leftHeroPane, rightFormPane);
    }

    public TextField getTableField() {
        return txtTable;
    }

    public Button getLoginButton() {
        return btnLogin;
    }

    public void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    public void clearError() {
        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);
    }
}
