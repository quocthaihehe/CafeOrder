package cafe.views;

import cafe.utils.AnimationHelper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.net.URL;

/**
 * LoginView - Screen 1 trong bản thiết kế UI_mong_muon.png:
 * - Cột trái (Hero Poster): Nền ảnh ấm, Logo "Brew & Bean CAFE", Tiêu đề "Good Coffee Better Moments"
 *   (Playfair Display #D2A679), Footer "Order tại bàn. Nhanh hơn. Tiện hơn. Trọn vẹn hơn."
 * - Cột phải: Form nền #FBF9F5, Icon hạt cafe, "Chào mừng trở lại!", "Tài khoản", "Mật khẩu" (icon mắt),
 *   Nút "Đăng nhập" #112823 (radius 5px), "Hoặc đăng nhập bằng", [👤 Nhân viên] / [💼 Quản lý], "Brew & Bean © 2025"
 */
public class LoginView extends HBox {

    private final TextField txtAccount = new TextField();
    private final PasswordField txtPassword = new PasswordField();
    private final TextField txtPasswordVisible = new TextField();
    private final Button btnToggleEye = new Button("👁");
    private final Button btnLogin = new Button("Đăng nhập");
    private final Label lblError = new Label();

    private final Button btnStaff = new Button("👤  Nhân viên");
    private final Button btnManager = new Button("💼  Quản lý");

    private boolean isPasswordVisible = false;

    public LoginView() {
        initUI();
    }

    private void initUI() {
        setStyle("-fx-background-color: #F5F0E8;");
        setAlignment(Pos.CENTER);

        // =====================================================================
        // 1. CỘT TRÁI: POSTER NGHỆ THUẬT (HERO BANNER 50%)
        // =====================================================================
        StackPane leftHeroPane = new StackPane();
        leftHeroPane.setPrefWidth(420);
        leftHeroPane.setMinWidth(360);
        leftHeroPane.setMaxWidth(480);
        HBox.setHgrow(leftHeroPane, Priority.ALWAYS);

        URL heroUrl = getClass().getResource("/cafe/resources/images/login_hero.png");
        if (heroUrl == null) {
            heroUrl = getClass().getResource("/cafe/resources/login_hero.png");
        }

        if (heroUrl != null) {
            Image heroImg = new Image(heroUrl.toExternalForm());
            ImageView heroView = new ImageView(heroImg);
            heroView.setPreserveRatio(true);
            heroView.setSmooth(true);

            // Căn chỉnh cover fit theo kích thước panel trái để ảnh sắc nét và bao trọn
            leftHeroPane.widthProperty().addListener((obs, oldV, newV) -> updateCoverFit(heroView, heroImg, newV.doubleValue(), leftHeroPane.getHeight()));
            leftHeroPane.heightProperty().addListener((obs, oldV, newV) -> updateCoverFit(heroView, heroImg, leftHeroPane.getWidth(), newV.doubleValue()));

            Rectangle clip = new Rectangle();
            clip.widthProperty().bind(leftHeroPane.widthProperty());
            clip.heightProperty().bind(leftHeroPane.heightProperty());
            leftHeroPane.setClip(clip);

            leftHeroPane.getChildren().add(heroView);
        } else {
            leftHeroPane.setStyle("-fx-background-color: #112823;");
        }

        // =====================================================================
        // 2. CỘT PHẢI: FORM ĐĂNG NHẬP CHUẨN SCREEN 1 (NỀN #FBF9F5)
        // =====================================================================
        VBox rightFormPane = new VBox(14);
        rightFormPane.setAlignment(Pos.CENTER_LEFT);
        rightFormPane.setPadding(new Insets(32, 40, 32, 40));
        rightFormPane.setPrefWidth(440);
        rightFormPane.setMinWidth(380);
        rightFormPane.setStyle("-fx-background-color: #FBF9F5; -fx-border-color: transparent transparent transparent #DED7CD; -fx-border-width: 0 0 0 1;");
        HBox.setHgrow(rightFormPane, Priority.ALWAYS);

        // Icon hạt cafe đôi ở đỉnh
        Label iconBean = new Label("☕");
        iconBean.setStyle("-fx-font-size: 24px; -fx-text-fill: #112823;");

        // Tiêu đề & Lời chào
        VBox titleBox = new VBox(3);
        Label lblWelcome = new Label("Chào mừng trở lại!");
        lblWelcome.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #292722;");

        Label lblDesc = new Label("Đăng nhập để bắt đầu phục vụ khách hàng.");
        lblDesc.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #817A70;");
        titleBox.getChildren().addAll(lblWelcome, lblDesc);

        // 1. Ô Tài khoản
        VBox accountGroup = new VBox(5);
        HBox lblAccBox = new HBox(6);
        lblAccBox.setAlignment(Pos.CENTER_LEFT);
        Label iconUser = new Label("👤");
        iconUser.setStyle("-fx-font-size: 11px; -fx-text-fill: #817A70;");
        Label lblAccountTitle = new Label("Tài khoản");
        lblAccountTitle.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 600; -fx-text-fill: #292722;");
        lblAccBox.getChildren().addAll(iconUser, lblAccountTitle);

        txtAccount.setPromptText("Nhập tên đăng nhập (hoặc số bàn 1..12)");
        txtAccount.getStyleClass().add("text-input-field");
        txtAccount.setPrefHeight(32);
        txtAccount.setStyle("-fx-background-color: #FBF9F5; -fx-border-color: #DED7CD; -fx-border-radius: 5px; -fx-background-radius: 5px; -fx-font-size: 11.5px; -fx-padding: 6 10 6 10;");
        accountGroup.getChildren().addAll(lblAccBox, txtAccount);

        // 2. Ô Mật khẩu kèm Icon Mắt
        VBox passGroup = new VBox(5);
        HBox lblPassBox = new HBox(6);
        lblPassBox.setAlignment(Pos.CENTER_LEFT);
        Label iconLock = new Label("🔒");
        iconLock.setStyle("-fx-font-size: 11px; -fx-text-fill: #817A70;");
        Label lblPassTitle = new Label("Mật khẩu");
        lblPassTitle.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 600; -fx-text-fill: #292722;");
        lblPassBox.getChildren().addAll(iconLock, lblPassTitle);

        txtPassword.setPromptText("Nhập mật khẩu");
        txtPassword.setPrefHeight(32);
        txtPassword.setStyle("-fx-background-color: #FBF9F5; -fx-border-color: #DED7CD; -fx-border-radius: 5px; -fx-background-radius: 5px; -fx-font-size: 11.5px; -fx-padding: 6 32 6 10;");

        txtPasswordVisible.setPromptText("Nhập mật khẩu");
        txtPasswordVisible.setPrefHeight(32);
        txtPasswordVisible.setStyle("-fx-background-color: #FBF9F5; -fx-border-color: #DED7CD; -fx-border-radius: 5px; -fx-background-radius: 5px; -fx-font-size: 11.5px; -fx-padding: 6 32 6 10;");
        txtPasswordVisible.setVisible(false);
        txtPasswordVisible.setManaged(false);

        // Sync text between password fields
        txtPassword.textProperty().bindBidirectional(txtPasswordVisible.textProperty());

        btnToggleEye.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 2 6 2 6; -fx-text-fill: #817A70;");
        btnToggleEye.setOnAction(e -> togglePasswordVisibility());

        StackPane passWrapper = new StackPane();
        passWrapper.getChildren().addAll(txtPassword, txtPasswordVisible, btnToggleEye);
        StackPane.setAlignment(btnToggleEye, Pos.CENTER_RIGHT);

        passGroup.getChildren().addAll(lblPassBox, passWrapper);

        // Thông báo lỗi
        lblError.setStyle("-fx-text-fill: #C62828; -fx-font-size: 11px; -fx-font-weight: 600;");
        lblError.setVisible(false);
        lblError.setManaged(false);

        // Nút Đăng nhập chính (#112823, cao 32px, radius 5px)
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setPrefHeight(32);
        btnLogin.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-size: 11.5px; -fx-font-weight: 600; -fx-background-radius: 5px; -fx-cursor: hand;");
        AnimationHelper.applyButtonPress(btnLogin);

        // Phân cách: "Hoặc đăng nhập bằng"
        HBox dividerBox = new HBox(8);
        dividerBox.setAlignment(Pos.CENTER);
        dividerBox.setPadding(new Insets(4, 0, 4, 0));

        Line lineLeft = new Line(0, 0, 75, 0);
        lineLeft.setStroke(Color.web("#DED7CD"));
        Line lineRight = new Line(0, 0, 75, 0);
        lineRight.setStroke(Color.web("#DED7CD"));

        Label lblOr = new Label("Hoặc đăng nhập bằng");
        lblOr.setStyle("-fx-font-size: 10px; -fx-text-fill: #817A70;");

        dividerBox.getChildren().addAll(lineLeft, lblOr, lineRight);

        // Nút phân quyền: [👤 Nhân viên] và [💼 Quản lý]
        HBox roleButtons = new HBox(10);
        roleButtons.setAlignment(Pos.CENTER);

        btnStaff.setMaxWidth(Double.MAX_VALUE);
        btnStaff.setPrefHeight(30);
        btnStaff.setStyle("-fx-background-color: #FBF9F5; -fx-text-fill: #292722; -fx-font-size: 11px; -fx-font-weight: 500; -fx-background-radius: 5px; -fx-border-color: #DED7CD; -fx-border-radius: 5px; -fx-cursor: hand;");
        HBox.setHgrow(btnStaff, Priority.ALWAYS);
        AnimationHelper.applyButtonPress(btnStaff);
        btnStaff.setOnAction(e -> {
            txtAccount.setText("5");
            txtPassword.setText("123456");
            clearError();
        });

        btnManager.setMaxWidth(Double.MAX_VALUE);
        btnManager.setPrefHeight(30);
        btnManager.setStyle("-fx-background-color: #FBF9F5; -fx-text-fill: #292722; -fx-font-size: 11px; -fx-font-weight: 500; -fx-background-radius: 5px; -fx-border-color: #DED7CD; -fx-border-radius: 5px; -fx-cursor: hand;");
        HBox.setHgrow(btnManager, Priority.ALWAYS);
        AnimationHelper.applyButtonPress(btnManager);
        btnManager.setOnAction(e -> {
            txtAccount.setText("1");
            txtPassword.setText("admin123");
            clearError();
        });

        roleButtons.getChildren().addAll(btnStaff, btnManager);

        // Footer thương hiệu
        Label lblFooter = new Label("Brew & Bean © 2025");
        lblFooter.setStyle("-fx-font-size: 9.5px; -fx-text-fill: #817A70; -fx-padding: 8 0 0 0;");
        lblFooter.setAlignment(Pos.CENTER);

        rightFormPane.getChildren().addAll(
            iconBean,
            titleBox,
            accountGroup,
            passGroup,
            lblError,
            btnLogin,
            dividerBox,
            roleButtons,
            lblFooter
        );

        getChildren().addAll(leftHeroPane, rightFormPane);
    }

    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            txtPasswordVisible.setText(txtPassword.getText());
            txtPassword.setVisible(false);
            txtPassword.setManaged(false);
            txtPasswordVisible.setVisible(true);
            txtPasswordVisible.setManaged(true);
            btnToggleEye.setText("🔒");
        } else {
            txtPassword.setText(txtPasswordVisible.getText());
            txtPasswordVisible.setVisible(false);
            txtPasswordVisible.setManaged(false);
            txtPassword.setVisible(true);
            txtPassword.setManaged(true);
            btnToggleEye.setText("👁");
        }
    }

    public TextField getAccountField() {
        return txtAccount;
    }

    public TextField getTableField() {
        // Tương thích với LoginController
        return txtAccount;
    }

    public PasswordField getPasswordField() {
        return txtPassword;
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

    private void updateCoverFit(ImageView iv, Image img, double paneW, double paneH) {
        if (img == null || paneW <= 0 || paneH <= 0) return;
        double imgW = img.getWidth();
        double imgH = img.getHeight();
        if (imgW <= 0 || imgH <= 0) return;

        double scale = Math.max(paneW / imgW, paneH / imgH);
        iv.setFitWidth(imgW * scale);
        iv.setFitHeight(imgH * scale);
    }
}
