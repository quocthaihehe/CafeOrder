package cafe.views;

import cafe.utils.AnimationHelper;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.net.URL;

/**
 * LoginView - Screen 1 trong bản thiết kế UI_mong_muon.png & redesign_login_form.txt:
 * - Cột trái (Hero Poster):
 *   + Ở trạng thái mặc định (Normal): Hiển thị ảnh login_hero.png (821 × 1916). Cột trái
 *     tinh chỉnh theo đúng tỉ lệ ảnh (821/1916 ~ 300px ở chiều cao 700px), hiển thị trọn vẹn
 *     100% không bị crop bất kỳ pixel nào, thấy đầy đủ "Good Coffee Better Moments" và slogan.
 *   + Ở trạng thái phóng to (Maximized/Form to nhất): Hiển thị ảnh login_hero2.png (1058 × 1487).
 *     Cột trái tự động điều chỉnh theo tỉ lệ ảnh (1058/1487 ~ 740px ở chiều cao 1040px), hiển thị
 *     trọn vẹn các phần quan trọng (logo, chữ, bàn cafe, ly sữa, ly cafe, ghế).
 * - Cột phải: Form nền #FBF9F5, formCard căn giữa sang trọng, Icon hạt cafe, "Chào mừng trở lại!",
 *   "Tài khoản", "Mật khẩu" (icon mắt), Nút "Đăng nhập" #112823, [👤 Nhân viên] / [💼 Quản lý].
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

    // Quản lý ảnh Hero theo kích thước form
    private final StackPane leftHeroPane = new StackPane();
    private final ImageView heroView = new ImageView();
    private Image imgHeroDefault;
    private Image imgHeroMax;
    private boolean isMaximizedMode = false;

    public LoginView() {
        initUI();
    }

    private void initUI() {
        setStyle("-fx-background-color: #F5F0E8;");
        setAlignment(Pos.CENTER);

        // =====================================================================
        // 1. CỘT TRÁI: POSTER NGHỆ THUẬT (HERO BANNER)
        // =====================================================================
        leftHeroPane.setStyle("-fx-background-color: #112823;");
        leftHeroPane.setAlignment(Pos.CENTER);

        // Nạp ảnh 1 (mặc định)
        URL hero1Url = getClass().getResource("/cafe/resources/images/login_hero.png");
        if (hero1Url == null) {
            hero1Url = getClass().getResource("/cafe/resources/login_hero.png");
        }
        if (hero1Url != null) {
            imgHeroDefault = new Image(hero1Url.toExternalForm());
        }

        // Nạp ảnh 2 (khi form phóng to to nhất)
        URL hero2Url = getClass().getResource("/cafe/resources/images/login_hero2.png");
        if (hero2Url == null) {
            hero2Url = getClass().getResource("/cafe/resources/login_hero2.png");
        }
        if (hero2Url != null) {
            imgHeroMax = new Image(hero2Url.toExternalForm());
        }

        heroView.setPreserveRatio(true);
        heroView.setSmooth(true);
        heroView.setCache(true);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(leftHeroPane.widthProperty());
        clip.heightProperty().bind(leftHeroPane.heightProperty());
        leftHeroPane.setClip(clip);

        leftHeroPane.getChildren().add(heroView);

        // Gắn listener kích thước để cập nhật bố cục ảnh tức thời
        widthProperty().addListener((obs, oldV, newV) -> updateHeroLayout());
        heightProperty().addListener((obs, oldV, newV) -> updateHeroLayout());

        // Lắng nghe Stage để tự động chuyển đổi ảnh khi người dùng bấm Maximize / Unmaximize
        sceneProperty().addListener((obsScene, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((obsWin, oldWin, newWin) -> {
                    if (newWin instanceof Stage) {
                        Stage stage = (Stage) newWin;
                        stage.maximizedProperty().addListener((obsMax, wasMax, isNowMax) -> {
                            setMaximizedMode(isNowMax);
                        });
                        stage.fullScreenProperty().addListener((obsFs, wasFs, isNowFs) -> {
                            setMaximizedMode(isNowFs);
                        });
                        setMaximizedMode(stage.isMaximized() || stage.isFullScreen());
                    }
                });
                if (newScene.getWindow() instanceof Stage) {
                    Stage stage = (Stage) newScene.getWindow();
                    stage.maximizedProperty().addListener((obsMax, wasMax, isNowMax) -> {
                        setMaximizedMode(isNowMax);
                    });
                    stage.fullScreenProperty().addListener((obsFs, wasFs, isNowFs) -> {
                        setMaximizedMode(isNowFs);
                    });
                    setMaximizedMode(stage.isMaximized() || stage.isFullScreen());
                }
            }
        });

        // =====================================================================
        // 2. CỘT PHẢI: FORM ĐĂNG NHẬP CHUẨN SCREEN 1 (NỀN #FBF9F5)
        // =====================================================================
        VBox rightFormPane = new VBox();
        rightFormPane.setAlignment(Pos.CENTER);
        rightFormPane.setStyle("-fx-background-color: #FBF9F5; -fx-border-color: transparent transparent transparent #DED7CD; -fx-border-width: 0 0 0 1;");
        HBox.setHgrow(rightFormPane, Priority.ALWAYS);

        // Card form đăng nhập căn giữa sang trọng
        VBox formCard = new VBox(14);
        formCard.setAlignment(Pos.CENTER_LEFT);
        formCard.setMaxWidth(440);
        formCard.setMinWidth(360);
        formCard.setPadding(new Insets(32, 40, 32, 40));

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
        txtAccount.setPrefHeight(34);
        txtAccount.setStyle("-fx-background-color: #FBF9F5; -fx-border-color: #DED7CD; -fx-border-radius: 5px; -fx-background-radius: 5px; -fx-font-size: 12px; -fx-padding: 6 10 6 10;");
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
        txtPassword.setPrefHeight(34);
        txtPassword.setStyle("-fx-background-color: #FBF9F5; -fx-border-color: #DED7CD; -fx-border-radius: 5px; -fx-background-radius: 5px; -fx-font-size: 12px; -fx-padding: 6 34 6 10;");

        txtPasswordVisible.setPromptText("Nhập mật khẩu");
        txtPasswordVisible.setPrefHeight(34);
        txtPasswordVisible.setStyle("-fx-background-color: #FBF9F5; -fx-border-color: #DED7CD; -fx-border-radius: 5px; -fx-background-radius: 5px; -fx-font-size: 12px; -fx-padding: 6 34 6 10;");
        txtPasswordVisible.setVisible(false);
        txtPasswordVisible.setManaged(false);

        // Sync text between password fields
        txtPassword.textProperty().bindBidirectional(txtPasswordVisible.textProperty());

        btnToggleEye.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 12px; -fx-padding: 2 6 2 6; -fx-text-fill: #817A70;");
        btnToggleEye.setOnAction(e -> togglePasswordVisibility());

        StackPane passWrapper = new StackPane();
        passWrapper.getChildren().addAll(txtPassword, txtPasswordVisible, btnToggleEye);
        StackPane.setAlignment(btnToggleEye, Pos.CENTER_RIGHT);

        passGroup.getChildren().addAll(lblPassBox, passWrapper);

        // Thông báo lỗi
        lblError.setStyle("-fx-text-fill: #C62828; -fx-font-size: 11px; -fx-font-weight: 600;");
        lblError.setVisible(false);
        lblError.setManaged(false);

        // Nút Đăng nhập chính (#112823, cao 34px, radius 5px)
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setPrefHeight(34);
        btnLogin.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: 600; -fx-background-radius: 5px; -fx-cursor: hand;");
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
        btnStaff.setPrefHeight(32);
        btnStaff.setStyle("-fx-background-color: #FBF9F5; -fx-text-fill: #292722; -fx-font-size: 11px; -fx-font-weight: 500; -fx-background-radius: 5px; -fx-border-color: #DED7CD; -fx-border-radius: 5px; -fx-cursor: hand;");
        HBox.setHgrow(btnStaff, Priority.ALWAYS);
        AnimationHelper.applyButtonPress(btnStaff);
        btnStaff.setOnAction(e -> {
            txtAccount.setText("5");
            txtPassword.setText("123456");
            clearError();
        });

        btnManager.setMaxWidth(Double.MAX_VALUE);
        btnManager.setPrefHeight(32);
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

        formCard.getChildren().addAll(
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

        rightFormPane.getChildren().add(formCard);

        getChildren().addAll(leftHeroPane, rightFormPane);

        // Khởi tạo kích thước ban đầu
        Platform.runLater(this::updateHeroLayout);
    }

    public void setMaximizedMode(boolean isMaximized) {
        if (this.isMaximizedMode != isMaximized) {
            this.isMaximizedMode = isMaximized;
            updateHeroLayout();
        }
    }

    private void updateHeroLayout() {
        // Tự động nhận diện nếu màn hình được kéo quá rộng mà không bấm nút maximize
        boolean isWideScreen = isMaximizedMode || (getWidth() >= 1150);

        Image currentImg = isWideScreen ? (imgHeroMax != null ? imgHeroMax : imgHeroDefault) : imgHeroDefault;
        if (currentImg == null) return;

        if (heroView.getImage() != currentImg) {
            heroView.setImage(currentImg);
        }

        double paneH = getHeight();
        if (paneH <= 0) {
            paneH = isWideScreen ? 900 : 700;
        }

        double imgW = currentImg.getWidth();
        double imgH = currentImg.getHeight();
        if (imgW <= 0 || imgH <= 0) return;

        // Tỉ lệ gốc của ảnh
        double ratio = imgW / imgH; // ~0.4285 (hero1) hoặc ~0.7115 (hero2)
        double targetW = Math.round(paneH * ratio);

        // Giữ không gian tối thiểu 380px cho form bên phải nếu cửa sổ bị thu hẹp
        if (getWidth() > 0) {
            double maxAllowedW = Math.max(260, getWidth() - 380);
            if (targetW > maxAllowedW) {
                targetW = maxAllowedW;
            }
        }

        leftHeroPane.setPrefWidth(targetW);
        leftHeroPane.setMinWidth(targetW);
        leftHeroPane.setMaxWidth(targetW);

        // Contain fit tuyệt đối: Math.min đảm bảo không bao giờ bị crop mất bất kỳ nội dung nào
        double scale = Math.min(targetW / imgW, paneH / imgH);
        heroView.setFitWidth(imgW * scale);
        heroView.setFitHeight(imgH * scale);
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
}
