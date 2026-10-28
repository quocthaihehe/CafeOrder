package cafe.views;

import cafe.models.Order;
import cafe.services.CafeServerService;
import cafe.utils.AnimationHelper;
import cafe.utils.CurrencyFormatter;
import cafe.utils.IconHelper;
import cafe.utils.SvgIcons;
import cafe.utils.WindowChromeHelper;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * LoginView - Giao diện Đăng nhập chuẩn thiết kế Brew & Bean (Plan UI Login - Sơ Đồ Bàn):
 * - Cột trái: Hero Poster hiển thị trọn vẹn theo tỉ lệ khung hình.
 * - Cột phải:
 *   + Góc trên bên phải: Nút [ Sơ đồ bàn (X trống) ] cố định, căn đều mép trên & mép phải.
 *   + Giữa: FormCard đăng nhập gọn gàng, thanh thoát.
 *   + Popup Sơ Đồ Bàn (740px): Lưới GridPane 4 cột x 3 hàng với thẻ bàn 2 dòng (162x66px),
 *     tương phản màu chữ tuyệt đối (-fx-text-fill inline), không bao giờ bị cắt chữ "...".
 */
public class LoginView extends HBox {

    private final TextField txtAccount = new TextField();
    private final PasswordField txtPassword = new PasswordField();
    private final TextField txtPasswordVisible = new TextField();
    private final Button btnToggleEye = new Button();
    private final SVGPath eyeSvg = SvgIcons.createOutlineIcon(SvgIcons.EYE, 0.7, Color.web("#817A70"));
    private final Button btnLogin = new Button("Đăng nhập");
    private final Label lblError = new Label();

    // Card thông báo bàn đang có khách trên Form chính (khi người dùng gõ tay số bàn đang bận)
    private final VBox occupiedNoticeCard = new VBox(6);
    private final Label lblOccupiedTitle = new Label();
    private final Label lblOccupiedBadge = new Label();
    private final Label lblOccupiedDetail = new Label();

    // Nút [ Sơ đồ bàn ] cố định góc trên phải
    private final Button btnOpenTableMap = new Button();
    private final Label lblOpenTableMapText = new Label("Sơ đồ bàn");
    private int selectedFloorOnLogin = 1;
    private int selectedTableInPopup = 5;
    private IntConsumer onTableCardClicked;
    private Runnable livePopupRefreshRunner;

    private final Button btnCustomer = new Button("Kiosk Khách");
    private final Button btnStaff = new Button("Nhân viên");
    private final Button btnManager = new Button("Quản lý");

    // Server status badge
    private final HBox serverStatusBadge = new HBox(6);
    private final Circle serverDot = new Circle(4.5, Color.web("#D97706"));
    private final Label lblServerStatusText = new Label("Máy chủ: Đang kết nối...");
    private final Label lblRetryText = new Label("Thử lại");
    private final Button btnRetryConnection = new Button();
    private final Button btnConfigServer = new Button();

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

        URL hero1Url = getClass().getResource("/cafe/resources/images/login_hero.png");
        if (hero1Url != null) {
            imgHeroDefault = new Image(hero1Url.toExternalForm());
        }

        URL hero2Url = getClass().getResource("/cafe/resources/images/login_hero2.png");
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

        widthProperty().addListener((obs, oldV, newV) -> updateHeroLayout());
        heightProperty().addListener((obs, oldV, newV) -> updateHeroLayout());

        sceneProperty().addListener((obsScene, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((obsWin, oldWin, newWin) -> {
                    if (newWin instanceof Stage) {
                        Stage stage = (Stage) newWin;
                        stage.maximizedProperty().addListener((obsMax, wasMax, isNowMax) -> setMaximizedMode(isNowMax));
                        stage.fullScreenProperty().addListener((obsFs, wasFs, isNowFs) -> setMaximizedMode(isNowFs));
                        setMaximizedMode(stage.isMaximized() || stage.isFullScreen());
                    }
                });
                if (newScene.getWindow() instanceof Stage) {
                    Stage stage = (Stage) newScene.getWindow();
                    stage.maximizedProperty().addListener((obsMax, wasMax, isNowMax) -> setMaximizedMode(isNowMax));
                    stage.fullScreenProperty().addListener((obsFs, wasFs, isNowFs) -> setMaximizedMode(isNowFs));
                    setMaximizedMode(stage.isMaximized() || stage.isFullScreen());
                }
            }
        });

        // =====================================================================
        // 2. CỘT PHẢI: STACKPANE GỒM FORM ĐĂNG NHẬP GIỮA & NÚT [SƠ ĐỒ BÀN] GÓC TRÊN PHẢI
        // =====================================================================
        StackPane rightFormPane = new StackPane();
        rightFormPane.setStyle("-fx-background-color: #FBF9F5; -fx-border-color: transparent transparent transparent #DED7CD; -fx-border-width: 0 0 0 1;");
        HBox.setHgrow(rightFormPane, Priority.ALWAYS);

        // Thanh công cụ góc trên bên phải chứa nút [ Sơ đồ bàn ]
        HBox topRightBar = buildTopRightTableMapBar();
        StackPane.setAlignment(topRightBar, Pos.TOP_RIGHT);
        StackPane.setMargin(topRightBar, new Insets(20, 28, 0, 0));

        // Card form đăng nhập căn giữa sang trọng, thanh thoát
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

        // Server Status Badge
        serverStatusBadge.setAlignment(Pos.CENTER_LEFT);
        serverStatusBadge.setPadding(new Insets(6, 10, 6, 10));
        serverStatusBadge.setStyle("-fx-background-color: #FEF3C7; -fx-border-color: #FDE68A; -fx-border-radius: 8px; -fx-background-radius: 8px;");
        lblServerStatusText.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #92400E;");

        HBox retryContent = new HBox(4);
        retryContent.setAlignment(Pos.CENTER);
        Node retryIcon = IconHelper.getIcon("staff", "lam-moi.png", SvgIcons.SETTINGS, "", 12, Color.web("#112823"));
        lblRetryText.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #112823;");
        lblRetryText.setMinWidth(Region.USE_PREF_SIZE);
        retryContent.getChildren().addAll(retryIcon, lblRetryText);
        btnRetryConnection.setGraphic(retryContent);
        btnRetryConnection.setMinWidth(Region.USE_PREF_SIZE);
        btnRetryConnection.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #DED7CD; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 3 8 3 8; -fx-cursor: hand;");
        AnimationHelper.applyButtonPress(btnRetryConnection);
        btnRetryConnection.setVisible(false);
        btnRetryConnection.setManaged(false);

        HBox cfgContent = new HBox(4);
        cfgContent.setAlignment(Pos.CENTER);
        Node cfgIcon = IconHelper.getIcon("admin", "ha-tang-mang.png", SvgIcons.SETTINGS, "", 13, Color.web("#112823"));
        Label lblCfgText = new Label("Cấu hình");
        lblCfgText.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #112823;");
        lblCfgText.setMinWidth(Region.USE_PREF_SIZE);
        cfgContent.getChildren().addAll(cfgIcon, lblCfgText);
        btnConfigServer.setGraphic(cfgContent);
        btnConfigServer.setMinWidth(Region.USE_PREF_SIZE);
        btnConfigServer.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #DED7CD; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 3 9 3 9; -fx-cursor: hand;");
        AnimationHelper.applyButtonPress(btnConfigServer);

        Region statusSpacer = new Region();
        HBox.setHgrow(statusSpacer, Priority.ALWAYS);
        serverStatusBadge.getChildren().addAll(serverDot, lblServerStatusText, statusSpacer, btnRetryConnection, btnConfigServer);

        // 1. Ô Tài khoản / Số bàn
        VBox accountGroup = new VBox(6);
        HBox lblAccBox = new HBox(8);
        lblAccBox.setAlignment(Pos.CENTER_LEFT);
        SVGPath iconUser = SvgIcons.createOutlineIcon(SvgIcons.USER, 0.65, Color.web("#817A70"));
        Label lblAccountTitle = new Label("Tài khoản / Số bàn");
        lblAccountTitle.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #112823;");
        lblAccBox.getChildren().addAll(iconUser, lblAccountTitle);

        txtAccount.setPromptText("Nhập tên đăng nhập hoặc số bàn (bấm [Sơ đồ bàn] góc phải để chọn)");
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

        txtPassword.setPromptText("Nhập mật khẩu (Để trống nếu là Khách gọi món tại bàn)");
        txtPassword.setPrefHeight(50);
        txtPassword.setMinHeight(50);
        txtPassword.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-background-color: #FAF8F5; -fx-border-color: #E2DDD5; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-font-size: 14.5px; -fx-padding: 12 42 12 16; -fx-text-fill: #112823; -fx-font-weight: 600;");

        txtPasswordVisible.setPromptText("Nhập mật khẩu (Để trống nếu là Khách gọi món tại bàn)");
        txtPasswordVisible.setPrefHeight(50);
        txtPasswordVisible.setMinHeight(50);
        txtPasswordVisible.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-background-color: #FAF8F5; -fx-border-color: #E2DDD5; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-font-size: 14.5px; -fx-padding: 12 42 12 16; -fx-text-fill: #112823; -fx-font-weight: 600;");
        txtPasswordVisible.setVisible(false);
        txtPasswordVisible.setManaged(false);

        txtPassword.textProperty().bindBidirectional(txtPasswordVisible.textProperty());

        btnToggleEye.setGraphic(eyeSvg);
        btnToggleEye.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 6 12 6 12;");
        btnToggleEye.setOnAction(e -> togglePasswordVisibility());

        StackPane passWrapper = new StackPane();
        passWrapper.getChildren().addAll(txtPassword, txtPasswordVisible, btnToggleEye);
        StackPane.setAlignment(btnToggleEye, Pos.CENTER_RIGHT);

        passGroup.getChildren().addAll(lblPassBox, passWrapper);

        // 3. CARD THÔNG BÁO BÀN ĐANG CÓ KHÁCH (Kế thừa thiết kế hệ thống Brew & Bean)
        buildOccupiedNoticeCard();

        // Thông báo lỗi / trạng thái chung
        lblError.setWrapText(true);
        lblError.setStyle("-fx-text-fill: #B91C1C; -fx-font-size: 12px; -fx-font-weight: 700;");
        lblError.setVisible(false);
        lblError.setManaged(false);

        // Nút Đăng nhập chính (#112823)
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setPrefHeight(50);
        btnLogin.setMinHeight(50);
        btnLogin.getStyleClass().add("pos-btn-primary");
        btnLogin.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-size: 15px; -fx-font-weight: 800; -fx-background-radius: 8px; -fx-cursor: hand;");
        AnimationHelper.applyButtonPress(btnLogin);

        // Phân cách: "Chọn nhanh vai trò (Demo Quick-Fill)"
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

        // Segmented Pill: [Kiosk Khách] [Thu ngân (POS)] [Quản lý (Admin)]
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
            List<Integer> readyList = CafeServerService.getInstance().getReadyTables();
            int suggested = readyList.contains(5) ? 5 : (!readyList.isEmpty() ? readyList.get(0) : 2);
            this.selectedTableInPopup = suggested;
            txtAccount.setText(String.valueOf(suggested));
            txtPassword.setText("");
            btnCustomer.setStyle(pillActive);
            btnStaff.setStyle(pillIdle);
            btnManager.setStyle(pillIdle);
            clearError();
            String sCode = suggested < 10 ? "0" + suggested : String.valueOf(suggested);
            showInfo("● Đã chọn Bàn " + sCode + " (Sẵn sàng) — Bấm [Đăng nhập] hoặc mở [Sơ đồ bàn] ở góc trên phải để đổi bàn.");
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

        Label lblFooter = new Label("Brew & Bean © 2026 — Hệ thống Quản lý Quán Cà Phê");
        lblFooter.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11px; -fx-text-fill: #6E675F; -fx-padding: 8 0 0 0;");
        lblFooter.setAlignment(Pos.CENTER);

        formCard.getChildren().addAll(
            iconBeanView,
            titleBox,
            serverStatusBadge,
            accountGroup,
            passGroup,
            occupiedNoticeCard,
            lblError,
            btnLogin,
            dividerBox,
            roleButtons,
            lblFooter
        );

        StackPane centerWrap = new StackPane(formCard);
        centerWrap.setAlignment(Pos.CENTER);
        centerWrap.setStyle("-fx-background-color: transparent;");

        rightFormPane.getChildren().addAll(centerWrap, topRightBar);
        getChildren().addAll(leftHeroPane, rightFormPane);

        Platform.runLater(() -> {
            updateHeroLayout();
            refreshTablePickerGrid();
        });
    }

    /**
     * Xây dựng thanh nút [ Sơ đồ bàn (X trống) ] cố định ở góc trên phải của màn hình Login.
     */
    private HBox buildTopRightTableMapBar() {
        HBox bar = new HBox();
        bar.setAlignment(Pos.TOP_RIGHT);
        bar.setPickOnBounds(false);
        bar.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        final String btnIdleStyle =
            "-fx-background-color: #F5F0E8; " +
            "-fx-border-color: #D8CEBE; " +
            "-fx-border-width: 1.2px; " +
            "-fx-border-radius: 10px; " +
            "-fx-background-radius: 10px; " +
            "-fx-padding: 8 16 8 16; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(17,40,35,0.08), 10, 0.1, 0, 2);";

        final String btnHoverStyle =
            "-fx-background-color: #112823; " +
            "-fx-border-color: #112823; " +
            "-fx-border-width: 1.2px; " +
            "-fx-border-radius: 10px; " +
            "-fx-background-radius: 10px; " +
            "-fx-padding: 8 16 8 16; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(17,40,35,0.20), 14, 0.15, 0, 4);";

        HBox content = new HBox(8);
        content.setAlignment(Pos.CENTER);

        StackPane iconHolder = new StackPane();
        Node iconDark = IconHelper.getIcon("staff", "so-do-ban.png", SvgIcons.TABLE, "", 16, Color.web("#112823"));
        iconHolder.getChildren().add(iconDark);

        lblOpenTableMapText.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        lblOpenTableMapText.setMinWidth(Region.USE_PREF_SIZE);

        Circle liveDot = new Circle(4.0, Color.web("#16A34A"));

        content.getChildren().addAll(iconHolder, lblOpenTableMapText, liveDot);
        btnOpenTableMap.setGraphic(content);
        btnOpenTableMap.setMinWidth(Region.USE_PREF_SIZE);
        btnOpenTableMap.setStyle(btnIdleStyle);

        btnOpenTableMap.setOnMouseEntered(e -> {
            btnOpenTableMap.setStyle(btnHoverStyle);
            lblOpenTableMapText.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #FFFFFF;");
            iconHolder.getChildren().setAll(IconHelper.getIcon("staff", "so-do-ban.png", SvgIcons.TABLE, "", 16, Color.WHITE));
        });

        btnOpenTableMap.setOnMouseExited(e -> {
            btnOpenTableMap.setStyle(btnIdleStyle);
            lblOpenTableMapText.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #112823;");
            iconHolder.getChildren().setAll(IconHelper.getIcon("staff", "so-do-ban.png", SvgIcons.TABLE, "", 16, Color.web("#112823")));
        });

        AnimationHelper.applyButtonPress(btnOpenTableMap);
        bar.getChildren().add(btnOpenTableMap);
        return bar;
    }

    private void buildOccupiedNoticeCard() {
        occupiedNoticeCard.setPadding(new Insets(12, 14, 12, 14));
        occupiedNoticeCard.setStyle(
            "-fx-background-color: #FFFBEB; " +
            "-fx-border-color: #F59E0B; " +
            "-fx-border-width: 1.5px; " +
            "-fx-border-radius: 10px; " +
            "-fx-background-radius: 10px;"
        );

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Node tblIcon = IconHelper.getIcon("staff", "so-do-ban.png", SvgIcons.TABLE, "", 16, Color.web("#92400E"));
        lblOccupiedTitle.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #92400E;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        lblOccupiedBadge.setStyle(
            "-fx-background-color: #172E27; " +
            "-fx-text-fill: #FCD34D; " +
            "-fx-font-family: 'Inter', sans-serif; " +
            "-fx-font-size: 10.5px; " +
            "-fx-font-weight: 800; " +
            "-fx-padding: 3 9 3 9; " +
            "-fx-background-radius: 999px;"
        );
        lblOccupiedBadge.setMinWidth(Region.USE_PREF_SIZE);
        topRow.getChildren().addAll(tblIcon, lblOccupiedTitle, sp, lblOccupiedBadge);

        lblOccupiedDetail.setWrapText(true);
        lblOccupiedDetail.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 600; -fx-text-fill: #78350F;");

        occupiedNoticeCard.getChildren().addAll(topRow, lblOccupiedDetail);
        occupiedNoticeCard.setVisible(false);
        occupiedNoticeCard.setManaged(false);
    }

    /**
     * Cập nhật số bàn trống trên nút [ Sơ đồ bàn (X trống) ] và làm mới Popup nếu đang mở.
     */
    public void refreshTablePickerGrid() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::refreshTablePickerGrid);
            return;
        }
        int readyCount = CafeServerService.getInstance().getReadyTables().size();
        lblOpenTableMapText.setText("Sơ đồ bàn (" + readyCount + " trống)");
        if (livePopupRefreshRunner != null) {
            livePopupRefreshRunner.run();
        }
    }

    /**
     * Mở Popup Sơ Đồ Bàn rộng rãi (740px) chuẩn Plan UI Login - Sơ Đồ Bàn:
     * - Cụm nút Tầng 1 / Tầng 2 / Tầng 3 có setMinWidth(Region.USE_PREF_SIZE) không bao giờ bị "...".
     * - Hàng chú thích rõ ràng có setMinWidth(Region.USE_PREF_SIZE).
     * - Lưới GridPane 4 cột x 3 hàng với thẻ bàn 2 dòng (162x66px), chữ trắng #FFFFFF nổi bật trên nền #172E27.
     */
    public void showTableMapPopup(Stage ownerStage, Runnable onConfirmLogin) {
        Stage modal = new Stage();
        if (ownerStage != null) modal.initOwner(ownerStage);
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initStyle(StageStyle.TRANSPARENT);
        modal.setTitle("Sơ Đồ Chọn Bàn Nhanh — Brew & Bean CAFE");

        VBox card = new VBox(15);
        card.setPrefWidth(780);
        card.setMinWidth(780);
        card.setMaxWidth(780);
        card.setPadding(new Insets(22, 24, 22, 24));
        card.setStyle(
            "-fx-background-color: #FBF9F5; " +
            "-fx-background-radius: 18px; " +
            "-fx-border-color: #DFD7CA; " +
            "-fx-border-width: 1.5px; " +
            "-fx-border-radius: 18px; " +
            "-fx-effect: dropshadow(gaussian, rgba(17,40,35,0.25), 28, 0.18, 0, 10);"
        );

        // =====================================================================
        // 1. HEADER: TIÊU ĐỀ + CỤM NÚT CHỌN TẦNG (TẦNG 1, TẦNG 2, TẦNG 3) + NÚT ĐÓNG
        // =====================================================================
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setMaxWidth(Double.MAX_VALUE);

        StackPane iconBadge = new StackPane();
        iconBadge.setPrefSize(42, 42);
        iconBadge.setMinSize(42, 42);
        iconBadge.setMaxSize(42, 42);
        iconBadge.setStyle("-fx-background-color: #112823; -fx-background-radius: 11px;");
        Node headerIcon = IconHelper.getIcon("staff", "so-do-ban.png", SvgIcons.TABLE, "", 20, Color.web("#FCD34D"));
        iconBadge.getChildren().add(headerIcon);

        VBox titleCol = new VBox(2);
        Label lblPopupTitle = new Label("SƠ ĐỒ CHỌN BÀN NHANH");
        lblPopupTitle.setStyle("-fx-font-family: 'Playfair Display', Georgia, serif; -fx-font-size: 19px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        Label lblPopupSub = new Label("Chọn bàn \"Sẵn sàng\" để điền nhanh số bàn đăng nhập");
        lblPopupSub.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #6E675F;");
        titleCol.getChildren().addAll(lblPopupTitle, lblPopupSub);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        HBox floorTabsBox = new HBox(4);
        floorTabsBox.setAlignment(Pos.CENTER_RIGHT);
        floorTabsBox.setPadding(new Insets(3));
        floorTabsBox.setMinWidth(Region.USE_PREF_SIZE);
        floorTabsBox.setMaxWidth(Region.USE_PREF_SIZE);
        floorTabsBox.setStyle("-fx-background-color: #EDE6DB; -fx-background-radius: 9px; -fx-border-color: #DFD7CA; -fx-border-radius: 9px;");

        Button btnFloor1 = new Button("Tầng 1");
        Button btnFloor2 = new Button("Tầng 2");
        Button btnFloor3 = new Button("Tầng 3");
        Button[] floorBtns = {btnFloor1, btnFloor2, btnFloor3};
        for (Button fb : floorBtns) {
            fb.setMinWidth(Region.USE_PREF_SIZE);
            fb.setPrefHeight(30);
            fb.setMinHeight(30);
            AnimationHelper.applyButtonPress(fb);
        }
        floorTabsBox.getChildren().addAll(btnFloor1, btnFloor2, btnFloor3);

        Button btnClosePopup = new Button();
        btnClosePopup.setGraphic(IconHelper.getIcon("client", "dong-cua-so.png", SvgIcons.CLOSE, "", 13, Color.web("#4A453E")));
        btnClosePopup.setPrefSize(34, 34);
        btnClosePopup.setMinSize(34, 34);
        btnClosePopup.setMaxSize(34, 34);
        btnClosePopup.setStyle("-fx-background-color: #EFEAE1; -fx-background-radius: 8px; -fx-cursor: hand;");
        AnimationHelper.applyButtonPress(btnClosePopup);
        btnClosePopup.setOnAction(ev -> {
            livePopupRefreshRunner = null;
            modal.close();
        });

        headerRow.getChildren().addAll(iconBadge, titleCol, headerSpacer, floorTabsBox, btnClosePopup);

        // =====================================================================
        // 2. THANH CHÚ THÍCH TRẠNG THÁI (NẰM GỌN 100% TRONG KHUNG, THẲNG HÀNG LƯỚI BÀN)
        // =====================================================================
        HBox legendRow = new HBox(14);
        legendRow.setAlignment(Pos.CENTER_LEFT);
        legendRow.setPadding(new Insets(9, 14, 9, 14));
        legendRow.setMaxWidth(Double.MAX_VALUE);
        legendRow.setStyle("-fx-background-color: #F4EFE6; -fx-background-radius: 10px; -fx-border-color: #E5DEC9; -fx-border-radius: 10px;");

        Label lblLegendHeader = new Label("Chú thích:");
        lblLegendHeader.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 800; -fx-text-fill: #4A453E;");
        lblLegendHeader.setMinWidth(Region.USE_PREF_SIZE);

        legendRow.getChildren().addAll(
            lblLegendHeader,
            createPopupLegendItem("#16A34A", "Sẵn sàng"),
            createPopupLegendItem("#F59E0B", "Chờ pha"),
            createPopupLegendItem("#38BDF8", "Đang pha"),
            createPopupLegendItem("#22C55E", "Đang dùng"),
            createPopupLegendItem("#D97706", "Giữ bàn hẹn")
        );

        // =====================================================================
        // 3. HỘP CẢNH BÁO TRONG POPUP KHI BẤM VÀO BÀN ĐANG CÓ KHÁCH
        // =====================================================================
        HBox popupOccupiedBanner = new HBox(10);
        popupOccupiedBanner.setAlignment(Pos.CENTER_LEFT);
        popupOccupiedBanner.setPadding(new Insets(10, 14, 10, 14));
        popupOccupiedBanner.setMaxWidth(Double.MAX_VALUE);
        popupOccupiedBanner.setStyle(
            "-fx-background-color: #FFFBEB; " +
            "-fx-border-color: #F59E0B; " +
            "-fx-border-width: 1.5px; " +
            "-fx-border-radius: 10px; " +
            "-fx-background-radius: 10px;"
        );
        Node warnIcon = IconHelper.getIcon("staff", "so-do-ban.png", SvgIcons.TABLE, "", 16, Color.web("#B45309"));
        Label lblPopupWarnText = new Label();
        lblPopupWarnText.setWrapText(true);
        lblPopupWarnText.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #92400E;");
        HBox.setHgrow(lblPopupWarnText, Priority.ALWAYS);
        popupOccupiedBanner.getChildren().addAll(warnIcon, lblPopupWarnText);
        popupOccupiedBanner.setVisible(false);
        popupOccupiedBanner.setManaged(false);

        // =====================================================================
        // 4. LƯỚI BÀN 4 CỘT CHIA ĐỀU 25% MỖI CỘT (THẲNG HÀNG TUYỆT ĐỐI VỚI VIỀN PHẢI)
        // =====================================================================
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.setAlignment(Pos.CENTER);
        for (int c = 0; c < 4; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(25.0);
            cc.setFillWidth(true);
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }

        // =====================================================================
        // 5. FOOTER POPUP: TRẠNG THÁI BÀN ĐANG CHỌN & NÚT XÁC NHẬN ĐĂNG NHẬP NGAY
        // =====================================================================
        HBox footerRow = new HBox(12);
        footerRow.setAlignment(Pos.CENTER_LEFT);
        footerRow.setPadding(new Insets(10, 0, 0, 0));
        footerRow.setStyle("-fx-border-color: #E5DEC9 transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        Circle footerDot = new Circle(4.5, Color.web("#16A34A"));
        Label lblFooterSelected = new Label();
        lblFooterSelected.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        lblFooterSelected.setMinWidth(Region.USE_PREF_SIZE);

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        Button btnCancel = new Button("Đóng");
        btnCancel.setMinWidth(Region.USE_PREF_SIZE);
        btnCancel.setStyle(
            "-fx-background-color: #EDE6DB; " +
            "-fx-text-fill: #4A453E; " +
            "-fx-font-family: 'Inter', sans-serif; " +
            "-fx-font-size: 13px; " +
            "-fx-font-weight: 700; " +
            "-fx-background-radius: 8px; " +
            "-fx-padding: 9 18 9 18; " +
            "-fx-cursor: hand;"
        );
        AnimationHelper.applyButtonPress(btnCancel);
        btnCancel.setOnAction(ev -> {
            livePopupRefreshRunner = null;
            modal.close();
        });

        Button btnConfirmTable = new Button("Chọn Bàn & Đăng Nhập");
        btnConfirmTable.setMinWidth(Region.USE_PREF_SIZE);
        btnConfirmTable.setStyle(
            "-fx-background-color: #112823; " +
            "-fx-text-fill: #FFFFFF; " +
            "-fx-font-family: 'Inter', sans-serif; " +
            "-fx-font-size: 13px; " +
            "-fx-font-weight: 800; " +
            "-fx-background-radius: 8px; " +
            "-fx-padding: 9 20 9 20; " +
            "-fx-cursor: hand;"
        );
        AnimationHelper.applyButtonPress(btnConfirmTable);
        btnConfirmTable.setOnAction(ev -> {
            CafeServerService srv = CafeServerService.getInstance();
            if (srv.isTableReadyForCustomer(selectedTableInPopup)) {
                txtAccount.setText(String.valueOf(selectedTableInPopup));
                txtPassword.setText("");
                livePopupRefreshRunner = null;
                modal.close();
                if (onConfirmLogin != null) {
                    onConfirmLogin.run();
                }
            }
        });

        footerRow.getChildren().addAll(footerDot, lblFooterSelected, footerSpacer, btnCancel, btnConfirmTable);

        // Khởi tạo bàn đang chọn mặc định là bàn Sẵn sàng
        CafeServerService server = CafeServerService.getInstance();
        try {
            int currentTyped = Integer.parseInt(txtAccount.getText().trim().replaceAll("[^0-9]", ""));
            if (server.isTableReadyForCustomer(currentTyped)) {
                selectedTableInPopup = currentTyped;
                if (currentTyped >= 300) selectedFloorOnLogin = 3;
                else if (currentTyped >= 200) selectedFloorOnLogin = 2;
                else selectedFloorOnLogin = 1;
            }
        } catch (Exception ignored) {}
        if (!server.isTableReadyForCustomer(selectedTableInPopup)) {
            List<Integer> readyList = server.getReadyTables();
            if (!readyList.isEmpty()) {
                selectedTableInPopup = readyList.contains(5) ? 5 : readyList.get(0);
            }
        }

        // Hàm render lưới bàn 4 cột trong Popup
        Runnable renderPopupState = () -> {
            // 1. Style 3 nút chọn tầng (padding 6 12 6 12 gọn gàng)
            int[] floorNums = {1, 2, 3};
            for (int i = 0; i < 3; i++) {
                boolean active = (selectedFloorOnLogin == floorNums[i]);
                floorBtns[i].setStyle(active
                    ? "-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 800; -fx-background-radius: 7px; -fx-padding: 6 12 6 12; -fx-cursor: hand;"
                    : "-fx-background-color: transparent; -fx-text-fill: #4A453E; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 7px; -fx-padding: 6 12 6 12; -fx-cursor: hand;"
                );
            }

            // 2. Lấy danh sách bàn theo tầng
            int[] tables;
            if (selectedFloorOnLogin == 2) {
                tables = new int[]{201, 202, 203, 204, 205, 206, 207, 208};
            } else if (selectedFloorOnLogin == 3) {
                tables = new int[]{301, 302, 303, 304, 305, 306};
            } else {
                tables = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
            }

            grid.getChildren().clear();
            for (int idx = 0; idx < tables.length; idx++) {
                int t = tables[idx];
                int col = idx % 4;
                int row = idx / 4;
                Node cardNode = createPopupTableCard(
                    t,
                    modal,
                    popupOccupiedBanner,
                    lblPopupWarnText,
                    onConfirmLogin,
                    () -> {
                        if (livePopupRefreshRunner != null) livePopupRefreshRunner.run();
                    }
                );
                GridPane.setFillWidth(cardNode, true);
                grid.add(cardNode, col, row);
            }

            // 3. Cập nhật Footer
            String selCode = selectedTableInPopup < 10 ? "0" + selectedTableInPopup : String.valueOf(selectedTableInPopup);
            boolean selReady = server.isTableReadyForCustomer(selectedTableInPopup);
            if (selReady) {
                footerDot.setFill(Color.web("#16A34A"));
                lblFooterSelected.setText("Đang chọn: Bàn " + selCode + " (Sẵn sàng đón khách)");
                lblFooterSelected.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #15803D;");
                btnConfirmTable.setText("✓ Chọn Bàn " + selCode + " & Đăng Nhập");
                btnConfirmTable.setDisable(false);
            } else {
                footerDot.setFill(Color.web("#D97706"));
                lblFooterSelected.setText("Vui lòng chọn một bàn ở trạng thái Sẵn sàng");
                lblFooterSelected.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #92400E;");
                btnConfirmTable.setText("Chọn Bàn & Đăng Nhập");
                btnConfirmTable.setDisable(true);
            }
        };

        this.livePopupRefreshRunner = renderPopupState;
        modal.setOnHidden(ev -> this.livePopupRefreshRunner = null);

        btnFloor1.setOnAction(ev -> {
            selectedFloorOnLogin = 1;
            popupOccupiedBanner.setVisible(false);
            popupOccupiedBanner.setManaged(false);
            renderPopupState.run();
        });
        btnFloor2.setOnAction(ev -> {
            selectedFloorOnLogin = 2;
            popupOccupiedBanner.setVisible(false);
            popupOccupiedBanner.setManaged(false);
            renderPopupState.run();
        });
        btnFloor3.setOnAction(ev -> {
            selectedFloorOnLogin = 3;
            popupOccupiedBanner.setVisible(false);
            popupOccupiedBanner.setManaged(false);
            renderPopupState.run();
        });

        renderPopupState.run();

        card.getChildren().addAll(headerRow, legendRow, popupOccupiedBanner, grid, footerRow);

        WindowChromeHelper.makeDraggable(modal, card);
        StackPane wrap = new StackPane(card);
        wrap.setPadding(new Insets(18));
        wrap.setStyle("-fx-background-color: transparent;");
        Scene scene = new Scene(wrap);
        scene.setFill(Color.TRANSPARENT);
        WindowChromeHelper.applyTheme(scene);
        modal.setScene(scene);
        modal.show();
        AnimationHelper.playDialogEntrance(card);
    }

    private HBox createPopupLegendItem(String hexColor, String labelText) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setMinWidth(Region.USE_PREF_SIZE);
        Circle dot = new Circle(4.5, Color.web(hexColor));
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #2C2623;");
        lbl.setMinWidth(Region.USE_PREF_SIZE);
        box.getChildren().addAll(dot, lbl);
        return box;
    }

    /**
     * Tạo thẻ bàn 2 dòng (162x66px) trong Popup Sơ đồ bàn:
     * - Dòng 1: Tên bàn in đậm (sử dụng inline -fx-text-fill: #FFFFFF trên nền #172E27 khi bàn có khách).
     * - Dòng 2: Chấm tròn trạng thái + Mô tả trạng thái rõ ràng.
     */
    private Node createPopupTableCard(
            int tableNum,
            Stage modal,
            HBox popupOccupiedBanner,
            Label lblPopupWarnText,
            Runnable onConfirmLogin,
            Runnable refreshPopup) {

        CafeServerService server = CafeServerService.getInstance();
        String liveStatus = server.getTableLiveDisplayStatus(tableNum);
        boolean isReady = "Sẵn sàng".equals(liveStatus);
        boolean isSelected = isReady && (tableNum == selectedTableInPopup);
        CafeServerService.ReservationInfo resInfo = server.getReservation(tableNum);
        Order activeOrd = server.getCombinedOrderForTable(tableNum);

        String tCode = tableNum < 10 ? "0" + tableNum : String.valueOf(tableNum);
        String tableTitle = "BÀN " + tCode;
        if (tableNum == 11) tableTitle = "BÀN 11 (Sofa)";
        else if (tableNum == 12) tableTitle = "BÀN 12 (Sảnh)";
        else if (tableNum == 207 || tableNum == 208) tableTitle = "BÀN " + tCode + " (VIP)";
        else if (tableNum == 305 || tableNum == 306) tableTitle = "BÀN " + tCode + " (View)";

        VBox card = new VBox(5);
        card.setPrefHeight(66);
        card.setMinHeight(66);
        card.setMaxHeight(66);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setCursor(javafx.scene.Cursor.HAND);

        // Dòng 1: Tên bàn + dấu ✓ nếu đang chọn
        HBox line1 = new HBox(6);
        line1.setAlignment(Pos.CENTER_LEFT);
        Label lblName = new Label(tableTitle);
        lblName.setMinWidth(Region.USE_PREF_SIZE);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label lblCheck = new Label(isSelected ? "✓" : "");
        lblCheck.setMinWidth(Region.USE_PREF_SIZE);
        line1.getChildren().addAll(lblName, sp, lblCheck);

        // Dòng 2: Chấm tròn + Trạng thái
        HBox line2 = new HBox(6);
        line2.setAlignment(Pos.CENTER_LEFT);
        Circle dot = new Circle(4.2);
        Label lblStatus = new Label();
        lblStatus.setMinWidth(Region.USE_PREF_SIZE);
        line2.getChildren().addAll(dot, lblStatus);

        if (isReady) {
            dot.setFill(Color.web("#16A34A"));
            String subText = (resInfo != null && resInfo.reservationTime != null)
                ? "Trống (Hẹn " + resInfo.reservationTime + ")"
                : "Sẵn sàng đón khách";
            lblStatus.setText(subText);

            // Inline -fx-text-fill rõ ràng cho bàn Sẵn sàng
            lblName.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #112823;");
            lblCheck.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 900; -fx-text-fill: #15803D;");
            lblStatus.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #15803D;");

            if (isSelected) {
                card.setStyle(
                    "-fx-background-color: #ECFDF5; " +
                    "-fx-border-color: #16A34A; " +
                    "-fx-border-width: 2px; " +
                    "-fx-border-radius: 12px; " +
                    "-fx-background-radius: 12px; " +
                    "-fx-effect: dropshadow(gaussian, rgba(22,163,74,0.18), 10, 0.15, 0, 3);"
                );
            } else {
                card.setStyle(
                    "-fx-background-color: #FFFFFF; " +
                    "-fx-border-color: #D8CEBE; " +
                    "-fx-border-width: 1.2px; " +
                    "-fx-border-radius: 12px; " +
                    "-fx-background-radius: 12px;"
                );
            }
        } else if ("Giữ bàn hẹn".equals(liveStatus)) {
            dot.setFill(Color.web("#D97706"));
            String timeStr = (resInfo != null && resInfo.reservationTime != null) ? " (" + resInfo.reservationTime + ")" : "";
            lblStatus.setText("Giữ bàn hẹn" + timeStr);
            lblName.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #92400E;");
            lblStatus.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #B45309;");
            card.setStyle(
                "-fx-background-color: #FFFBEB; " +
                "-fx-border-color: #F59E0B; " +
                "-fx-border-width: 1.5px; " +
                "-fx-border-radius: 12px; " +
                "-fx-background-radius: 12px;"
            );
        } else if ("Chờ dọn bàn".equals(liveStatus)) {
            dot.setFill(Color.web("#EF4444"));
            lblStatus.setText("Chờ dọn bàn");
            lblName.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #991B1B;");
            lblStatus.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #DC2626;");
            card.setStyle(
                "-fx-background-color: #FEF2F2; " +
                "-fx-border-color: #EF4444; " +
                "-fx-border-width: 1.5px; " +
                "-fx-border-radius: 12px; " +
                "-fx-background-radius: 12px;"
            );
        } else {
            // BÀN ĐANG CÓ KHÁCH (Chờ pha / Đang pha / Đang dùng):
            // BẮT BUỘC đặt -fx-text-fill: #FFFFFF trực tiếp trong setStyle(), không thêm chữ "Có khách" ở góc phải dòng 1
            lblName.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #FFFFFF;");
            lblCheck.setText("");

            if ("Chờ pha".equals(liveStatus)) {
                dot.setFill(Color.web("#F59E0B"));
                lblStatus.setText("Chờ pha chế");
                lblStatus.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 800; -fx-text-fill: #FDE68A;");
            } else if ("Đang pha".equals(liveStatus)) {
                dot.setFill(Color.web("#38BDF8"));
                lblStatus.setText("Đang pha chế");
                lblStatus.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 800; -fx-text-fill: #93C5FD;");
            } else {
                dot.setFill(Color.web("#22C55E"));
                lblStatus.setText("Đang dùng bữa");
                lblStatus.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 800; -fx-text-fill: #86EFAC;");
            }

            card.setStyle(
                "-fx-background-color: #172E27; " +
                "-fx-border-color: #112823; " +
                "-fx-border-width: 1.5px; " +
                "-fx-border-radius: 12px; " +
                "-fx-background-radius: 12px;"
            );
        }

        card.getChildren().addAll(line1, line2);
        AnimationHelper.applyButtonPress(card);

        card.setOnMouseClicked(ev -> {
            String latestStatus = server.getTableLiveDisplayStatus(tableNum);
            if ("Sẵn sàng".equals(latestStatus)) {
                selectedTableInPopup = tableNum;
                popupOccupiedBanner.setVisible(false);
                popupOccupiedBanner.setManaged(false);
                txtAccount.setText(String.valueOf(tableNum));
                txtPassword.setText("");
                showInfo("● Đã chọn Bàn " + tCode + " (Sẵn sàng) — Bấm [Đăng nhập] để bắt đầu gọi món!");
                refreshPopup.run();

                // Click đúp vào bàn Sẵn sàng -> Đăng nhập thẳng vào bàn đó
                if (ev.getClickCount() >= 2) {
                    livePopupRefreshRunner = null;
                    modal.close();
                    if (onConfirmLogin != null) {
                        onConfirmLogin.run();
                    }
                }
            } else {
                Order curOrd = server.getCombinedOrderForTable(tableNum);
                CafeServerService.ReservationInfo curRes = server.getReservation(tableNum);
                String orderInfo = (curOrd != null)
                    ? " (Đơn #" + curOrd.getOrderId() + " • " + curOrd.getTotalQuantity() + " món • " + CurrencyFormatter.formatVND(curOrd.getTotalAmount()) + ")"
                    : "";
                lblPopupWarnText.setText(
                    "Bàn " + tCode + " hiện đang có khách sử dụng — Trạng thái: " + latestStatus + orderInfo +
                    ". Vui lòng chọn một trong các bàn đang Sẵn sàng (màu trắng) bên cạnh!"
                );
                popupOccupiedBanner.setVisible(true);
                popupOccupiedBanner.setManaged(true);
                showOccupiedTableNotice(tableNum, latestStatus, curOrd, curRes);
            }
        });

        return card;
    }

    public Button getBtnOpenTableMap() {
        return btnOpenTableMap;
    }

    public void setOnTableCardClicked(IntConsumer callback) {
        this.onTableCardClicked = callback;
    }

    /**
     * Hiển thị Card thông báo bàn đang có khách ngay trên giao diện Đăng nhập (kế thừa thiết kế hệ thống).
     */
    public void showOccupiedTableNotice(int tableNumber, String liveStatus, Order activeOrder, CafeServerService.ReservationInfo resInfo) {
        lblError.setVisible(false);
        lblError.setManaged(false);

        String tStr = tableNumber < 10 ? "0" + tableNumber : String.valueOf(tableNumber);
        lblOccupiedTitle.setText("Bàn " + tStr + " hiện đang có khách (Đang sử dụng)");
        lblOccupiedBadge.setText("● " + liveStatus);

        StringBuilder detail = new StringBuilder();
        if ("Chờ pha".equals(liveStatus) || "Đang pha".equals(liveStatus) || "Đang dùng".equals(liveStatus)) {
            detail.append("Bàn ").append(tStr).append(" đang ở trạng thái \"").append(liveStatus).append("\" (Đang có khách ngồi phục vụ");
            if (activeOrder != null) {
                detail.append(" • Đơn #").append(activeOrder.getOrderId())
                      .append(" gồm ").append(activeOrder.getTotalQuantity()).append(" món • ")
                      .append(CurrencyFormatter.formatVND(activeOrder.getTotalAmount()));
            }
            detail.append("). ");
        } else if ("Giữ bàn hẹn".equals(liveStatus) && resInfo != null) {
            detail.append("Bàn ").append(tStr).append(" đang được giữ hẹn cho khách ").append(resInfo.customerName)
                  .append(" lúc ").append(resInfo.reservationTime).append(". ");
        } else {
            detail.append("Bàn ").append(tStr).append(" hiện đang ở trạng thái \"").append(liveStatus).append("\", chưa sẵn sàng đón khách mới. ");
        }
        detail.append("Vui lòng bấm [Sơ đồ bàn] ở góc trên phải để chọn bàn ● Sẵn sàng!");

        lblOccupiedDetail.setText(detail.toString());
        occupiedNoticeCard.setVisible(true);
        occupiedNoticeCard.setManaged(true);
    }

    public void hideOccupiedTableNotice() {
        occupiedNoticeCard.setVisible(false);
        occupiedNoticeCard.setManaged(false);
    }

    public void setMaximizedMode(boolean isMaximized) {
        if (this.isMaximizedMode != isMaximized) {
            this.isMaximizedMode = isMaximized;
            updateHeroLayout();
        }
    }

    private void updateHeroLayout() {
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

        double ratio = imgW / imgH;
        double targetW = Math.round(paneH * ratio);

        if (getWidth() > 0) {
            double maxAllowedW = Math.max(260, getWidth() - 420);
            if (targetW > maxAllowedW) {
                targetW = maxAllowedW;
            }
        }

        leftHeroPane.setPrefWidth(targetW);
        leftHeroPane.setMinWidth(targetW);
        leftHeroPane.setMaxWidth(targetW);

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
        return txtAccount;
    }

    public PasswordField getPasswordField() {
        return txtPassword;
    }

    public Button getLoginButton() {
        return btnLogin;
    }

    public void showError(String message) {
        hideOccupiedTableNotice();
        lblError.setText(message);
        lblError.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #DC2626; -fx-alignment: center;");
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    public void showInfo(String message) {
        hideOccupiedTableNotice();
        lblError.setText(message);
        lblError.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #15803D; -fx-alignment: center;");
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    public void clearError() {
        hideOccupiedTableNotice();
        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    public void setServerStatus(boolean connected, String host, int port) {
        setServerStatus(connected, host, port, -1);
    }

    public void setServerStatus(boolean connected, String host, int port, long rttMs) {
        if (connected) {
            serverDot.setFill(Color.web("#16A34A"));
            String rttStr = (rttMs >= 0) ? " • RTT: " + rttMs + "ms" : "";
            lblServerStatusText.setText("Máy chủ: Sẵn sàng (" + host + ":" + port + rttStr + ")");
            lblServerStatusText.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #15803D;");
            btnRetryConnection.setVisible(false);
            btnRetryConnection.setManaged(false);
            serverStatusBadge.setStyle("-fx-background-color: #DCFCE7; -fx-border-color: #BBF7D0; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 6 10 6 10;");
        } else {
            serverDot.setFill(Color.web("#DC2626"));
            lblServerStatusText.setText("Máy chủ: Chưa kết nối (" + host + ":" + port + ")");
            lblServerStatusText.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #DC2626;");
            btnRetryConnection.setVisible(true);
            btnRetryConnection.setManaged(true);
            serverStatusBadge.setStyle("-fx-background-color: #FEE2E2; -fx-border-color: #FECACA; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 6 10 6 10;");
        }
    }

    public void setRetryButtonText(String text) {
        lblRetryText.setText(text);
    }

    public Button getBtnRetryConnection() {
        return btnRetryConnection;
    }

    public Button getBtnConfigServer() {
        return btnConfigServer;
    }
}
