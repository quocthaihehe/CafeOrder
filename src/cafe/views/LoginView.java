package cafe.views;

import cafe.utils.AnimationHelper;
import cafe.utils.SvgIcons;
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
import javafx.scene.shape.SVGPath;
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
 *   "Tài khoản", "Mật khẩu" (icon mắt SVG), Nút "Đăng nhập" #112823, [Nhân viên] / [Quản lý].
 */
public class LoginView extends HBox {

    private final TextField txtAccount = new TextField();
    private final PasswordField txtPassword = new PasswordField();
    private final TextField txtPasswordVisible = new TextField();
    private final Button btnToggleEye = new Button();
    private final SVGPath eyeSvg = SvgIcons.createOutlineIcon(SvgIcons.EYE, 0.7, Color.web("#817A70"));
    private final Button btnLogin = new Button("Đăng nhập");
    private final Label lblError = new Label();

    private final Button btnCustomer = new Button("🍹  Khách (Bàn 05)");
    private final Button btnStaff = new Button("Nhân viên");
    private final Button btnManager = new Button("Quản lý");

    // Server status badge
    private final HBox serverStatusBadge = new HBox(6);
    private final Label lblServerDot = new Label("●");
    private final Label lblServerStatusText = new Label("Máy chủ: Đang kết nối...");
    private final Button btnRetryConnection = new Button("↻ Thử lại");

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

        // Card form đăng nhập căn giữa sang trọng (Plan UI ver 7.0 - Upscaled & Responsive)
        VBox formCard = new VBox(18);
        formCard.setAlignment(Pos.CENTER_LEFT);
        formCard.setMaxWidth(540);
        formCard.setMinWidth(400);
        formCard.setPadding(new Insets(36, 44, 36, 44));

        // Icon hạt cafe chất lượng cao (coffee-beans.png 48x48px)
        ImageView iconBeanView = new ImageView();
        try {
            URL beanUrl = getClass().getResource("/cafe/resources/images/placeholders/coffee-beans.png");
            if (beanUrl == null) beanUrl = getClass().getResource("/resources/images/placeholders/coffee-beans.png");
            if (beanUrl != null) {
                iconBeanView.setImage(new Image(beanUrl.toExternalForm(), 96, 96, true, true));
            }
        } catch (Exception ignored) {}
        iconBeanView.setFitWidth(48);
        iconBeanView.setFitHeight(48);
        iconBeanView.setPreserveRatio(true);
        iconBeanView.setSmooth(true);

        // Tiêu đề & Lời chào
        VBox titleBox = new VBox(4);
        Label lblWelcome = new Label("Chào mừng trở lại!");
        lblWelcome.setStyle("-fx-font-family: 'Playfair Display', Georgia, serif; -fx-font-size: 28px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        Label lblDesc = new Label("Đăng nhập để bắt đầu phục vụ khách hàng.");
        lblDesc.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13.5px; -fx-text-fill: #78716C;");
        titleBox.getChildren().addAll(lblWelcome, lblDesc);

        // Server Status Badge (Kiểm soát trạng thái kết nối máy chủ KDS / Port 5000)
        serverStatusBadge.setAlignment(Pos.CENTER_LEFT);
        serverStatusBadge.setPadding(new Insets(5, 10, 5, 10));
        serverStatusBadge.setStyle("-fx-background-color: #FEF3C7; -fx-border-color: #FDE68A; -fx-border-radius: 6px; -fx-background-radius: 6px;");
        lblServerDot.setStyle("-fx-text-fill: #D97706; -fx-font-size: 11px;");
        lblServerStatusText.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #92400E;");
        btnRetryConnection.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #112823; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 11px; -fx-font-weight: 700; -fx-border-color: #DED7CD; -fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 2 8 2 8; -fx-cursor: hand;");
        AnimationHelper.applyButtonPress(btnRetryConnection);
        btnRetryConnection.setVisible(false);
        btnRetryConnection.setManaged(false);

        Region statusSpacer = new Region();
        HBox.setHgrow(statusSpacer, Priority.ALWAYS);
        serverStatusBadge.getChildren().addAll(lblServerDot, lblServerStatusText, statusSpacer, btnRetryConnection);

        // 1. Ô Tài khoản
        VBox accountGroup = new VBox(6);
        HBox lblAccBox = new HBox(8);
        lblAccBox.setAlignment(Pos.CENTER_LEFT);
        SVGPath iconUser = SvgIcons.createOutlineIcon(SvgIcons.USER, 0.65, Color.web("#817A70"));
        Label lblAccountTitle = new Label("Tài khoản");
        lblAccountTitle.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #112823;");
        lblAccBox.getChildren().addAll(iconUser, lblAccountTitle);

        txtAccount.setPromptText("Nhập tên đăng nhập (hoặc số bàn 1..12)");
        txtAccount.getStyleClass().addAll("text-input-field", "pos-text-input");
        txtAccount.setPrefHeight(50);
        txtAccount.setMinHeight(50);
        txtAccount.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-background-color: #FAF8F5; -fx-border-color: #E2DDD5; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-font-size: 14.5px; -fx-padding: 12 16 12 16; -fx-text-fill: #112823; -fx-font-weight: 600;");
        accountGroup.getChildren().addAll(lblAccBox, txtAccount);

        // 2. Ô Mật khẩu kèm Icon Mắt
        VBox passGroup = new VBox(6);
        HBox lblPassBox = new HBox(8);
        lblPassBox.setAlignment(Pos.CENTER_LEFT);
        SVGPath iconLock = SvgIcons.createOutlineIcon(SvgIcons.LOCK, 0.65, Color.web("#817A70"));
        Label lblPassTitle = new Label("Mật khẩu");
        lblPassTitle.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #112823;");
        lblPassBox.getChildren().addAll(iconLock, lblPassTitle);

        txtPassword.setPromptText("Nhập mật khẩu");
        txtPassword.setPrefHeight(50);
        txtPassword.setMinHeight(50);
        txtPassword.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-background-color: #FAF8F5; -fx-border-color: #E2DDD5; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-font-size: 14.5px; -fx-padding: 12 42 12 16; -fx-text-fill: #112823; -fx-font-weight: 600;");

        txtPasswordVisible.setPromptText("Nhập mật khẩu");
        txtPasswordVisible.setPrefHeight(50);
        txtPasswordVisible.setMinHeight(50);
        txtPasswordVisible.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-background-color: #FAF8F5; -fx-border-color: #E2DDD5; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-font-size: 14.5px; -fx-padding: 12 42 12 16; -fx-text-fill: #112823; -fx-font-weight: 600;");
        txtPasswordVisible.setVisible(false);
        txtPasswordVisible.setManaged(false);

        // Sync text between password fields
        txtPassword.textProperty().bindBidirectional(txtPasswordVisible.textProperty());

        btnToggleEye.setGraphic(eyeSvg);
        btnToggleEye.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 6 12 6 12;");
        btnToggleEye.setOnAction(e -> togglePasswordVisibility());

        StackPane passWrapper = new StackPane();
        passWrapper.getChildren().addAll(txtPassword, txtPasswordVisible, btnToggleEye);
        StackPane.setAlignment(btnToggleEye, Pos.CENTER_RIGHT);

        passGroup.getChildren().addAll(lblPassBox, passWrapper);

        // Thông báo lỗi
        lblError.setStyle("-fx-text-fill: #B91C1C; -fx-font-size: 11.5px; -fx-font-weight: 700;");
        lblError.setVisible(false);
        lblError.setManaged(false);

        // Nút Đăng nhập chính (#112823, cao 42px, radius 8px)
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setPrefHeight(50);
        btnLogin.setMinHeight(50);
        btnLogin.getStyleClass().add("pos-btn-primary");
        btnLogin.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-size: 15px; -fx-font-weight: 800; -fx-background-radius: 8px; -fx-cursor: hand;");
        AnimationHelper.applyButtonPress(btnLogin);

        // Phân cách: "Chọn nhanh chế độ truy cập"
        HBox dividerBox = new HBox(8);
        dividerBox.setAlignment(Pos.CENTER);
        dividerBox.setPadding(new Insets(4, 0, 4, 0));

        Line lineLeft = new Line(0, 0, 65, 0);
        lineLeft.setStroke(Color.web("#E2DDD5"));
        Line lineRight = new Line(0, 0, 65, 0);
        lineRight.setStroke(Color.web("#E2DDD5"));

        Label lblOr = new Label("Chọn nhanh vai trò (Demo Quick-Fill)");
        lblOr.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #6E675F;");

        dividerBox.getChildren().addAll(lineLeft, lblOr, lineRight);

        // Segmented Pill: [Kiosk Khách] [Thu ngân (Staff)] [Quản lý (Admin)]
        HBox roleButtons = new HBox(6);
        roleButtons.setAlignment(Pos.CENTER);
        roleButtons.setPadding(new Insets(4));
        roleButtons.setStyle("-fx-background-color: #EDE6DB; -fx-background-radius: 10px; -fx-border-color: #DFD7CA; -fx-border-radius: 10px;");

        final String pillIdle = "-fx-font-family: 'Inter', sans-serif; -fx-background-color: transparent; -fx-text-fill: #4A453E; -fx-font-size: 13px; -fx-font-weight: 700; -fx-background-radius: 7px; -fx-cursor: hand;";
        final String pillActive = "-fx-font-family: 'Inter', sans-serif; -fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-font-weight: 700; -fx-background-radius: 7px; -fx-cursor: hand;";

        btnCustomer.setText("Kiosk Khách");
        btnCustomer.setMaxWidth(Double.MAX_VALUE);
        btnCustomer.setPrefHeight(40);
        btnCustomer.setMinHeight(40);
        btnCustomer.setStyle(pillIdle);
        HBox.setHgrow(btnCustomer, Priority.ALWAYS);
        AnimationHelper.applyButtonPress(btnCustomer);

        btnStaff.setText("Thu ngân (POS)");
        btnStaff.setMaxWidth(Double.MAX_VALUE);
        btnStaff.setPrefHeight(40);
        btnStaff.setMinHeight(40);
        btnStaff.setStyle(pillIdle);
        HBox.setHgrow(btnStaff, Priority.ALWAYS);
        AnimationHelper.applyButtonPress(btnStaff);

        btnManager.setText("Quản lý (Admin)");
        btnManager.setMaxWidth(Double.MAX_VALUE);
        btnManager.setPrefHeight(40);
        btnManager.setMinHeight(40);
        btnManager.setStyle(pillIdle);
        HBox.setHgrow(btnManager, Priority.ALWAYS);
        AnimationHelper.applyButtonPress(btnManager);

        btnCustomer.setOnAction(e -> {
            txtAccount.setText("5");
            txtPassword.setText("");
            btnCustomer.setStyle(pillActive);
            btnStaff.setStyle(pillIdle);
            btnManager.setStyle(pillIdle);
            clearError();
        });

        btnStaff.setOnAction(e -> {
            txtAccount.setText("cashier1");
            txtPassword.setText("cafe123");
            btnStaff.setStyle(pillActive);
            btnCustomer.setStyle(pillIdle);
            btnManager.setStyle(pillIdle);
            clearError();
        });

        btnManager.setOnAction(e -> {
            txtAccount.setText("admin");
            txtPassword.setText("admin123");
            btnManager.setStyle(pillActive);
            btnCustomer.setStyle(pillIdle);
            btnStaff.setStyle(pillIdle);
            clearError();
        });

        roleButtons.getChildren().addAll(btnCustomer, btnStaff, btnManager);

        // Footer thương hiệu (Chuẩn hóa 2026)
        Label lblFooter = new Label("Brew & Bean © 2026 — Hệ thống Quản lý Quán Cà Phê");
        lblFooter.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11px; -fx-text-fill: #6E675F; -fx-padding: 8 0 0 0;");
        lblFooter.setAlignment(Pos.CENTER);

        formCard.getChildren().addAll(
            iconBeanView,
            titleBox,
            serverStatusBadge,
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
            eyeSvg.setContent(SvgIcons.EYE_OFF);
        } else {
            txtPassword.setText(txtPasswordVisible.getText());
            txtPasswordVisible.setVisible(false);
            txtPasswordVisible.setManaged(false);
            txtPassword.setVisible(true);
            txtPassword.setManaged(true);
            eyeSvg.setContent(SvgIcons.EYE);
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
        lblError.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #DC2626; -fx-alignment: center;");
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    public void showInfo(String message) {
        lblError.setText(message);
        lblError.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #15803D; -fx-alignment: center;");
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    public void clearError() {
        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    public void setServerStatus(boolean connected, String host, int port) {
        setServerStatus(connected, host, port, -1);
    }

    public void setServerStatus(boolean connected, String host, int port, long rttMs) {
        if (connected) {
            lblServerDot.setStyle("-fx-text-fill: #15803D; -fx-font-size: 11px;");
            String rttStr = (rttMs >= 0) ? " • RTT: " + rttMs + "ms" : "";
            lblServerStatusText.setText("Máy chủ: Sẵn sàng (" + host + ":" + port + rttStr + ")");
            lblServerStatusText.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #15803D;");
            btnRetryConnection.setVisible(false);
            btnRetryConnection.setManaged(false);
            serverStatusBadge.setStyle("-fx-background-color: #DCFCE7; -fx-border-color: #BBF7D0; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 5 10 5 10;");
        } else {
            lblServerDot.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px;");
            lblServerStatusText.setText("Máy chủ: Chưa kết nối (" + host + ":" + port + ")");
            lblServerStatusText.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #DC2626;");
            btnRetryConnection.setVisible(true);
            btnRetryConnection.setManaged(true);
            serverStatusBadge.setStyle("-fx-background-color: #FEE2E2; -fx-border-color: #FECACA; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 5 10 5 10;");
        }
    }

    public Button getBtnRetryConnection() {
        return btnRetryConnection;
    }
}
