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
 * LoginView - Màn hình đăng nhập / Chọn bàn theo phong cách Editorial Split Screen.
 * Hallmark Discipline:
 * - Cột trái: Poster nghệ thuật "Good Coffee Better Moments" với nhận diện Brew & Bean CAFE.
 * - Cột phải: Form tối giản, subtle 4px radius, nút Dark Emerald (#183C32), không card-in-card.
 */
public class LoginView extends HBox {

    private final TextField txtTable = new TextField();
    private final Button btnLogin = new Button("Đăng nhập");
    private final Label lblError = new Label();
    private final FlowPane quickTablesPane = new FlowPane(6, 6);

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
        VBox rightFormPane = new VBox(18);
        rightFormPane.setAlignment(Pos.CENTER_LEFT);
        rightFormPane.setPadding(new Insets(36, 44, 36, 44));
        rightFormPane.setPrefWidth(460);
        rightFormPane.setMinWidth(400);
        rightFormPane.setStyle("-fx-background-color: #FBF9F4; -fx-border-color: transparent transparent transparent #D8D1C3; -fx-border-width: 0 0 0 1;");
        HBox.setHgrow(rightFormPane, Priority.ALWAYS);

        // Icon tách cà phê tối giản
        Label iconBean = new Label("☕");
        iconBean.setStyle("-fx-font-size: 26px; -fx-text-fill: #183C32;");

        // Tiêu đề & Lời chào
        VBox titleBox = new VBox(4);
        Label lblWelcome = new Label("Chào mừng trở lại!");
        lblWelcome.getStyleClass().add("editorial-display");

        Label lblDesc = new Label("Đăng nhập để bắt đầu phục vụ khách hàng.");
        lblDesc.getStyleClass().add("editorial-subtitle");
        titleBox.getChildren().addAll(lblWelcome, lblDesc);

        // Input số bàn
        VBox inputGroup = new VBox(6);
        Label lblTableTitle = new Label("SỐ BÀN PHỤC VỤ:");
        lblTableTitle.getStyleClass().add("section-label");

        txtTable.setPromptText("Nhập số bàn (VD: 1, 2, 5...)");
        txtTable.getStyleClass().add("text-input-field");
        txtTable.setPrefHeight(42);
        txtTable.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #183C32;");
        inputGroup.getChildren().addAll(lblTableTitle, txtTable);

        // Quick Table Chips (4px radius)
        VBox quickBox = new VBox(6);
        Label lblQuick = new Label("HOẶC CHỌN NHANH BÀN CÓ SẴN:");
        lblQuick.getStyleClass().add("section-label");

        quickTablesPane.getChildren().clear();
        for (int i = 1; i <= 8; i++) {
            final int t = i;
            Button chip = new Button("Bàn " + (t < 10 ? "0" + t : t));
            chip.getStyleClass().add("btn-option-tag");
            chip.setOnAction(e -> {
                txtTable.setText(String.valueOf(t));
                clearError();
            });
            quickTablesPane.getChildren().add(chip);
        }
        quickBox.getChildren().addAll(lblQuick, quickTablesPane);

        // Thông báo lỗi
        lblError.setStyle("-fx-text-fill: #C62828; -fx-font-size: 11.5px; -fx-font-weight: 600;");
        lblError.setVisible(false);
        lblError.setManaged(false);

        // Nút Đăng nhập chính (Primary Button Dark Emerald)
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setPrefHeight(44);
        btnLogin.getStyleClass().add("btn-primary");

        // Fast role buttons
        VBox roleSection = new VBox(8);
        roleSection.setAlignment(Pos.CENTER);
        Label lblOr = new Label("HOẶC ĐĂNG NHẬP PHÂN QUYỀN:");
        lblOr.getStyleClass().add("section-label");

        HBox roleButtons = new HBox(8);
        roleButtons.setAlignment(Pos.CENTER);
        Button btnStaff = new Button("👤  Nhân viên");
        btnStaff.getStyleClass().add("btn-secondary");
        btnStaff.setOnAction(e -> txtTable.setText("1"));

        Button btnManager = new Button("💼  Quản lý");
        btnManager.getStyleClass().add("btn-secondary");
        btnManager.setOnAction(e -> txtTable.setText("5"));
        roleButtons.getChildren().addAll(btnStaff, btnManager);
        roleSection.getChildren().addAll(lblOr, roleButtons);

        // Footer thương hiệu
        Label lblFooter = new Label("Brew & Bean CAFE © 2026");
        lblFooter.setStyle("-fx-font-size: 10.5px; -fx-text-fill: #817B70; -fx-padding: 8 0 0 0;");

        rightFormPane.getChildren().addAll(
            iconBean,
            titleBox,
            inputGroup,
            quickBox,
            lblError,
            btnLogin,
            new Separator(),
            roleSection,
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
