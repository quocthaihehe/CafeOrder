package cafe.views;

import cafe.controllers.ServerController;
import cafe.models.MenuItem;
import cafe.models.MenuRepository;
import cafe.models.Order;
import cafe.services.CafeServerService;
import cafe.utils.AnimationHelper;
import cafe.utils.CurrencyFormatter;
import cafe.utils.IconHelper;
import cafe.utils.NetworkConfig;
import cafe.utils.SvgIcons;
import cafe.utils.WindowChromeHelper;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * ServerKDSView - Màn hình Quầy Điều Phối Pha Chế Trung Tâm (Server KDS):
 * - Header trang bị Chỉ báo IP LAN thực tế + Port + Số phiên kết nối trực tuyến.
 * - Nút [Đổi Cổng] & [Báo Tạm Hết Món (86)] dùng bộ icon PNG chuẩn qua IconHelper.
 * - Bộ lọc trạng thái đơn & Bộ lọc trạm pha chế (Tất cả, Cà phê/Espresso, Trà & Đá xay, Bánh ngọt).
 */
public class ServerKDSView extends BorderPane {
    // Header stat labels
    private final Label lblStatQueued = new Label("Đang chờ: 0");
    private final Label lblStatPreparing = new Label("Đang pha: 0");
    private final Label lblStatDone = new Label("Đã xong: 0");
    private final Label lblClock = new Label();

    // Network status badge on Header
    private final Circle netStatusDot = new Circle(4.5, Color.web("#16A34A"));
    private final Label lblNetworkInfo = new Label();
    private final Button btnChangePort = new Button();
    private final Button btnToggleStock86 = new Button();

    // Control bar
    private final TextField txtSearch = new TextField();
    private final HBox filterButtonsBox = new HBox(6);
    private final HBox stationButtonsBox = new HBox(6);
    private final Button btnClearDone = new Button();
    private String activeStation = "ALL";

    // Tickets container
    private final FlowPane ticketsFlowPane = new FlowPane(14, 14);
    private final ScrollPane ticketsScrollPane = new ScrollPane();
    private final VBox emptyTicketsBox = new VBox(8);

    // Bottom log bar
    private final Label lblLatestLog = new Label("Máy chủ sẵn sàng.");

    public ServerKDSView() {
        initUI();
        startClock();
        updateNetworkInfo(CafeServerService.getInstance().getActiveServerPort(),
                          CafeServerService.getInstance().getActiveSessions().size());
    }

    private void initUI() {
        getStyleClass().add("root");

        // 1. TOP HEADER
        HBox header = buildHeader();
        setTop(header);

        // 2. CENTER - CONTROLS & TICKETS
        VBox centerSection = buildCenterSection();
        setCenter(centerSection);

        // 3. BOTTOM LOG BAR
        HBox bottomLogBar = new HBox(8);
        bottomLogBar.getStyleClass().add("kds-log-drawer");
        bottomLogBar.setAlignment(Pos.CENTER_LEFT);
        Circle logDot = new Circle(4, Color.web("#A66A42"));
        Label logIcon = new Label("NHẬT KÝ SOCKET:");
        logIcon.setStyle("-fx-font-weight: 800; -fx-text-fill: #A66A42; -fx-font-size: 11px;");
        lblLatestLog.getStyleClass().add("kds-log-text");
        HBox.setHgrow(lblLatestLog, Priority.ALWAYS);
        bottomLogBar.getChildren().addAll(logDot, logIcon, lblLatestLog);
        setBottom(bottomLogBar);
    }

    private HBox buildHeader() {
        HBox header = new HBox(12);
        header.getStyleClass().add("kds-header");
        header.setAlignment(Pos.CENTER_LEFT);

        // Brand Icon + Title
        StackPane brandIconBox = new StackPane();
        brandIconBox.setPrefSize(38, 38);
        brandIconBox.setMinSize(38, 38);
        brandIconBox.setStyle("-fx-background-color: #EFE8DD; -fx-background-radius: 10px; -fx-border-color: #DED7CD; -fx-border-radius: 10px;");
        Node brandIcon = IconHelper.getIcon("admin", "man-pha-che.png", SvgIcons.COFFEE, "", 20, Color.web("#112823"));
        brandIconBox.getChildren().add(brandIcon);

        VBox titleBox = new VBox(2);
        Label lblTitle = new Label("KDS • QUẦY ĐIỀU PHỐI PHA CHẾ");
        lblTitle.getStyleClass().add("kds-brand-title");
        Label lblSub = new Label("Kitchen Display System • Brew & Bean CAFE");
        lblSub.getStyleClass().add("kds-brand-subtitle");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        // Network IP & Port Badge
        HBox netBadge = new HBox(7);
        netBadge.setAlignment(Pos.CENTER_LEFT);
        netBadge.setPadding(new Insets(5, 12, 5, 12));
        netBadge.setStyle("-fx-background-color: #ECFDF5; -fx-border-color: #A7F3D0; -fx-border-radius: 999px; -fx-background-radius: 999px;");
        lblNetworkInfo.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 800; -fx-text-fill: #065F46;");
        lblNetworkInfo.setMinWidth(Region.USE_PREF_SIZE);
        netBadge.getChildren().addAll(netStatusDot, lblNetworkInfo);

        // Button: Đổi Cổng Mạng tại chỗ
        setupHeaderToolButton(
            btnChangePort,
            "Đổi Cổng",
            IconHelper.getIcon("admin", "ha-tang-mang.png", SvgIcons.SETTINGS, "", 15, Color.web("#112823")),
            "#FFFFFF", "#112823", "#D8CEBE"
        );
        btnChangePort.setOnAction(e -> showServerPortModal());

        // Button: Báo Tạm Hết Món (86)
        setupHeaderToolButton(
            btnToggleStock86,
            "Báo Tạm Hết Món (86)",
            IconHelper.getIcon("admin", "thuc-don-gia.png", SvgIcons.MENU, "", 15, Color.web("#92400E")),
            "#FFFBEB", "#92400E", "#F59E0B"
        );
        btnToggleStock86.setOnAction(e -> showQuickStock86Modal());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Stat Pills
        lblStatQueued.getStyleClass().addAll("stat-pill", "stat-queued");
        lblStatQueued.setMinWidth(Region.USE_PREF_SIZE);
        lblStatPreparing.getStyleClass().addAll("stat-pill", "stat-preparing");
        lblStatPreparing.setMinWidth(Region.USE_PREF_SIZE);
        lblStatDone.getStyleClass().addAll("stat-pill", "stat-done");
        lblStatDone.setMinWidth(Region.USE_PREF_SIZE);

        // Clock
        lblClock.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #6E675F;");
        lblClock.setMinWidth(Region.USE_PREF_SIZE);

        header.getChildren().addAll(
            brandIconBox, titleBox, netBadge, btnChangePort, btnToggleStock86,
            spacer, lblStatQueued, lblStatPreparing, lblStatDone, lblClock
        );
        return header;
    }

    private void setupHeaderToolButton(Button btn, String text, Node iconNode, String bgHex, String fgHex, String borderHex) {
        HBox content = new HBox(6);
        content.setAlignment(Pos.CENTER);
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: " + fgHex + ";");
        lbl.setMinWidth(Region.USE_PREF_SIZE);
        content.getChildren().addAll(iconNode, lbl);
        btn.setGraphic(content);
        btn.setMinWidth(Region.USE_PREF_SIZE);
        String baseStyle = "-fx-background-color: " + bgHex + "; -fx-border-color: " + borderHex + "; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 6 12 6 12; -fx-cursor: hand;";
        btn.setStyle(baseStyle);
        AnimationHelper.applyButtonPress(btn);
    }

    public void updateNetworkInfo(int port, int sessionCount) {
        String lanIp = NetworkConfig.getLocalLanIp();
        lblNetworkInfo.setText("IP LAN: " + lanIp + " • Port: " + port + " • Kết nối: " + sessionCount + " phiên");
    }

    private VBox buildCenterSection() {
        VBox center = new VBox(10);
        center.setPadding(new Insets(12, 20, 12, 20));

        // Filter & Search bar
        HBox controlBar = new HBox(10);
        controlBar.setAlignment(Pos.CENTER_LEFT);

        txtSearch.setPromptText("Tìm số bàn hoặc mã đơn (VD: 1, 1002)...");
        txtSearch.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #DFD7CA; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 7 12 7 12; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px;");
        txtSearch.setPrefWidth(260);
        txtSearch.setPrefHeight(36);

        filterButtonsBox.setSpacing(6);
        filterButtonsBox.setAlignment(Pos.CENTER_LEFT);

        // Station Filter Buttons (Bộ lọc trạm pha chế dùng icon chuẩn)
        stationButtonsBox.setSpacing(6);
        stationButtonsBox.setAlignment(Pos.CENTER_LEFT);
        renderStationButtons();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Clear Done button with vector icon (Zero raw emoji)
        HBox clearContent = new HBox(6);
        clearContent.setAlignment(Pos.CENTER);
        SVGPath trashIcon = SvgIcons.createOutlineIcon(SvgIcons.TRASH, 0.68, Color.web("#57534E"));
        Label lblClear = new Label("Ẩn đơn đã xong");
        lblClear.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #57534E;");
        lblClear.setMinWidth(Region.USE_PREF_SIZE);
        clearContent.getChildren().addAll(trashIcon, lblClear);
        btnClearDone.setGraphic(clearContent);
        btnClearDone.getStyleClass().add("kds-clear-btn");
        btnClearDone.setPrefHeight(36);
        btnClearDone.setMinWidth(Region.USE_PREF_SIZE);

        controlBar.getChildren().addAll(txtSearch, filterButtonsBox, stationButtonsBox, spacer, btnClearDone);

        // Tickets FlowPane inside ScrollPane
        ticketsFlowPane.setAlignment(Pos.TOP_LEFT);
        ticketsFlowPane.setPadding(new Insets(4, 0, 16, 0));

        ticketsScrollPane.setContent(ticketsFlowPane);
        ticketsScrollPane.setFitToWidth(true);
        ticketsScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        ticketsScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(ticketsScrollPane, Priority.ALWAYS);

        ticketsScrollPane.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> updateCardWidths());
        ticketsScrollPane.widthProperty().addListener((obs, oldVal, newVal) -> updateCardWidths());

        // Empty state when no orders (Uses IconHelper asset instead of raw emoji)
        emptyTicketsBox.setAlignment(Pos.CENTER);
        emptyTicketsBox.setPadding(new Insets(80, 20, 80, 20));
        emptyTicketsBox.setVisible(false);
        emptyTicketsBox.setManaged(false);

        Node emptyIcon = IconHelper.getIcon("admin", "man-pha-che.png", SvgIcons.COFFEE, "", 44, Color.web("#C7BEB0"));
        Label emptyTitle = new Label("Không có đơn hàng nào cần xử lý");
        emptyTitle.getStyleClass().add("editorial-title");
        Label emptyDesc = new Label("Các đơn hàng mới gửi từ Kiosk khách hoặc Nhân viên sẽ tự động xuất hiện tại đây theo thời gian thực.");
        emptyDesc.getStyleClass().add("editorial-subtitle");

        emptyTicketsBox.getChildren().addAll(emptyIcon, emptyTitle, emptyDesc);

        center.getChildren().addAll(controlBar, ticketsScrollPane, emptyTicketsBox);
        return center;
    }

    private void renderStationButtons() {
        stationButtonsBox.getChildren().clear();
        String[][] stations = {
            {"ALL", "Tất cả trạm", "don-hang.png"},
            {"ESPRESSO", "Cà phê / Espresso", "man-pha-che.png"},
            {"TEA", "Trà & Đá xay", "thuc-don-gia.png"},
            {"BAKERY", "Bánh ngọt", "mon-va-gia.png"}
        };
        for (String[] st : stations) {
            String code = st[0];
            String title = st[1];
            String iconFile = st[2];
            boolean isSel = code.equalsIgnoreCase(activeStation);

            Button btn = new Button();
            btn.setMinWidth(Region.USE_PREF_SIZE);
            HBox box = new HBox(5);
            box.setAlignment(Pos.CENTER);
            Node ic = IconHelper.getIcon("admin", iconFile, SvgIcons.COFFEE, "", 13, isSel ? Color.WHITE : Color.web("#4A453E"));
            Label lbl = new Label(title);
            lbl.setMinWidth(Region.USE_PREF_SIZE);
            lbl.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: " + (isSel ? "#FFFFFF" : "#4A453E") + ";");
            box.getChildren().addAll(ic, lbl);
            btn.setGraphic(box);
            btn.setStyle(isSel
                ? "-fx-background-color: #563521; -fx-background-radius: 7px; -fx-padding: 6 10 6 10; -fx-cursor: hand;"
                : "-fx-background-color: #EFE8DD; -fx-border-color: #DED7CD; -fx-border-radius: 7px; -fx-background-radius: 7px; -fx-padding: 6 10 6 10; -fx-cursor: hand;");
            btn.setOnAction(e -> {
                activeStation = code;
                renderStationButtons();
                ServerController.setStationFilter(code);
            });
            stationButtonsBox.getChildren().add(btn);
        }
    }

    public void renderFilters(List<String> filters, String activeFilter, Consumer<String> onFilterSelected) {
        filterButtonsBox.getChildren().clear();
        for (String filter : filters) {
            Button btn = new Button(filter);
            btn.setMinWidth(Region.USE_PREF_SIZE);
            btn.getStyleClass().add("kds-filter-btn");
            if (filter.equalsIgnoreCase(activeFilter)) {
                btn.getStyleClass().add("selected");
            }
            btn.setOnAction(e -> onFilterSelected.accept(filter));
            filterButtonsBox.getChildren().add(btn);
        }
    }

    public void updateStats(int queued, int preparing, int done) {
        lblStatQueued.setText("Đang chờ: " + queued);
        lblStatPreparing.setText("Đang pha: " + preparing);
        lblStatDone.setText("Đã xong: " + done);
    }

    // =========================================================================
    // CUSTOM MODAL 1: ĐỔI CỔNG MẠNG NGAY TẠI QUẦY PHA CHẾ SERVER KDS
    // =========================================================================
    private void showServerPortModal() {
        Stage owner = getScene() != null && getScene().getWindow() instanceof Stage ? (Stage) getScene().getWindow() : null;
        Stage modal = new Stage();
        if (owner != null) modal.initOwner(owner);
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initStyle(StageStyle.TRANSPARENT);
        modal.setTitle("Cấu hình Cổng Mạng Server KDS");

        VBox card = new VBox(14);
        card.setPrefWidth(440);
        card.setPadding(new Insets(22));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16px; -fx-border-color: #DFD7CA; -fx-border-radius: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 24, 0.15, 0, 8);");

        HBox headerRow = new HBox(10);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        Node netIcon = IconHelper.getIcon("admin", "ha-tang-mang.png", SvgIcons.SETTINGS, "", 22, Color.web("#112823"));
        VBox titleCol = new VBox(2);
        Label lblTitle = new Label("Cấu Hình Cổng Lắng Nghe (Server TCP Port)");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        Label lblSub = new Label("Địa chỉ IP LAN của máy chủ này: " + NetworkConfig.getLocalLanIp());
        lblSub.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #047857;");
        titleCol.getChildren().addAll(lblTitle, lblSub);
        headerRow.getChildren().addAll(netIcon, titleCol);

        int currentPort = CafeServerService.getInstance().getActiveServerPort();
        Label lblPortPrompt = new Label("Cổng kết nối TCP (1024 - 65535, mặc định: 8888):");
        lblPortPrompt.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #112823;");

        TextField txtPort = new TextField(String.valueOf(currentPort));
        txtPort.setStyle("-fx-background-color: #FAF8F5; -fx-border-color: #DFD8CE; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 9 12 9 12; -fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        HBox quickPorts = new HBox(8);
        quickPorts.setAlignment(Pos.CENTER_LEFT);
        Label lblQuick = new Label("Chọn nhanh:");
        lblQuick.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #6F695F; -fx-font-weight: 600;");
        quickPorts.getChildren().add(lblQuick);
        for (int p : new int[]{8888, 9000, 9999}) {
            Button bQuick = new Button("Port " + p);
            bQuick.setStyle("-fx-background-color: #F4EFE6; -fx-text-fill: #112823; -fx-font-size: 11px; -fx-font-weight: 700; -fx-background-radius: 6px; -fx-padding: 4 10 4 10; -fx-cursor: hand;");
            bQuick.setOnAction(ev -> txtPort.setText(String.valueOf(p)));
            quickPorts.getChildren().add(bQuick);
        }

        Label lblFeedback = new Label("Khi đổi cổng, máy chủ sẽ mở ngay cổng mới. Hãy nhập đúng IP LAN và cổng này tại mục [Cấu hình] trên màn hình Đăng nhập của máy Client.");
        lblFeedback.setWrapText(true);
        lblFeedback.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #065F46; -fx-background-color: #ECFDF5; -fx-padding: 8 10 8 10; -fx-background-radius: 8px;");

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancel = new Button("Đóng");
        btnCancel.setStyle("-fx-background-color: #F4EFE6; -fx-text-fill: #4A453E; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> modal.close());

        Button btnApply = new Button("Lưu & Mở Cổng Mới");
        btnApply.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-size: 12.5px; -fx-font-weight: 800; -fx-background-radius: 8px; -fx-padding: 8 18 8 18; -fx-cursor: hand;");
        btnApply.setOnAction(e -> {
            try {
                int newPort = Integer.parseInt(txtPort.getText().trim());
                if (!NetworkConfig.isValidPort(newPort)) {
                    lblFeedback.setText("Cổng không hợp lệ! Vui lòng nhập số từ 1024 đến 65535.");
                    lblFeedback.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #B91C1C; -fx-background-color: #FEE2E2; -fx-padding: 8 10 8 10; -fx-background-radius: 8px;");
                    return;
                }
                boolean ok = CafeServerService.getInstance().restartServer(newPort);
                if (ok) {
                    updateNetworkInfo(newPort, CafeServerService.getInstance().getActiveSessions().size());
                    setLatestLog("Đã chuyển cổng lắng nghe Server sang Port " + newPort + " (IP LAN: " + NetworkConfig.getLocalLanIp() + ").");
                    modal.close();
                } else {
                    lblFeedback.setText("Không thể mở cổng " + newPort + " (có thể đang bị ứng dụng khác chiếm dụng).");
                    lblFeedback.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #B91C1C; -fx-background-color: #FEE2E2; -fx-padding: 8 10 8 10; -fx-background-radius: 8px;");
                }
            } catch (NumberFormatException ex) {
                lblFeedback.setText("Vui lòng nhập số nguyên hợp lệ cho cổng TCP!");
                lblFeedback.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #B91C1C; -fx-background-color: #FEE2E2; -fx-padding: 8 10 8 10; -fx-background-radius: 8px;");
            }
        });

        actions.getChildren().addAll(btnCancel, btnApply);
        card.getChildren().addAll(headerRow, lblPortPrompt, txtPort, quickPorts, lblFeedback, actions);

        WindowChromeHelper.makeDraggable(modal, card);
        StackPane wrap = new StackPane(card);
        wrap.setPadding(new Insets(16));
        wrap.setStyle("-fx-background-color: transparent;");
        Scene scene = new Scene(wrap);
        scene.setFill(Color.TRANSPARENT);
        WindowChromeHelper.applyTheme(scene);
        modal.setScene(scene);
        modal.show();
        AnimationHelper.playDialogEntrance(card);
    }

    // =========================================================================
    // CUSTOM MODAL 2: BÁO TẠM HẾT MÓN (86-ING) TRỰC TIẾP TẠI QUẦY BAR KDS
    // =========================================================================
    private void showQuickStock86Modal() {
        Stage owner = getScene() != null && getScene().getWindow() instanceof Stage ? (Stage) getScene().getWindow() : null;
        Stage modal = new Stage();
        if (owner != null) modal.initOwner(owner);
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initStyle(StageStyle.TRANSPARENT);
        modal.setTitle("Báo Tạm Hết Món (86) Tại Quầy Pha Chế");

        VBox card = new VBox(12);
        card.setPrefWidth(540);
        card.setPrefHeight(560);
        card.setPadding(new Insets(22));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16px; -fx-border-color: #DFD7CA; -fx-border-radius: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 24, 0.15, 0, 8);");

        HBox headerRow = new HBox(10);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        Node menuIcon = IconHelper.getIcon("admin", "thuc-don-gia.png", SvgIcons.MENU, "", 22, Color.web("#92400E"));
        VBox titleCol = new VBox(2);
        Label lblTitle = new Label("Báo Tạm Hết Món (86-ing) • Đồng Bộ Toàn Mạng");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        Label lblSub = new Label("Khi gạt Tạm hết, mọi màn hình Kiosk Khách & Nhân viên sẽ khóa món ngay lập tức.");
        lblSub.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #6F695F;");
        titleCol.getChildren().addAll(lblTitle, lblSub);
        headerRow.getChildren().addAll(menuIcon, titleCol);

        TextField txtFilter = new TextField();
        txtFilter.setPromptText("Tìm nhanh tên món cần báo hết / mở bán lại...");
        txtFilter.setStyle("-fx-background-color: #FAF8F5; -fx-border-color: #DFD8CE; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8 12 8 12; -fx-font-size: 13px;");

        VBox listContainer = new VBox(6);
        ScrollPane scroll = new ScrollPane(listContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Runnable renderList = () -> {
            listContainer.getChildren().clear();
            String kw = txtFilter.getText() != null ? txtFilter.getText().trim().toLowerCase() : "";
            for (MenuItem item : MenuRepository.getAllItems()) {
                if (!kw.isEmpty() && !item.getName().toLowerCase().contains(kw) && !item.getCategory().toLowerCase().contains(kw)) {
                    continue;
                }
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(8, 12, 8, 12));
                boolean inStock = item.isInStock();
                row.setStyle("-fx-background-color: " + (inStock ? "#FAF8F5" : "#FEF2F2") + "; -fx-background-radius: 8px; -fx-border-color: " + (inStock ? "#EFE9DF" : "#FECACA") + "; -fx-border-radius: 8px;");

                Circle dot = new Circle(4.5, Color.web(inStock ? "#16A34A" : "#DC2626"));
                VBox infoBox = new VBox(2);
                HBox.setHgrow(infoBox, Priority.ALWAYS);
                Label lblName = new Label(item.getName());
                lblName.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #112823;");
                Label lblCat = new Label(item.getCategory() + " • " + CurrencyFormatter.formatVND(item.getPrice()));
                lblCat.setStyle("-fx-font-size: 11px; -fx-text-fill: #6F695F;");
                infoBox.getChildren().addAll(lblName, lblCat);

                Button btnToggle = new Button(inStock ? "Đang bán (Còn hàng)" : "Tạm hết (86)");
                btnToggle.setMinWidth(Region.USE_PREF_SIZE);
                btnToggle.setStyle(inStock
                    ? "-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-font-size: 11.5px; -fx-font-weight: 800; -fx-background-radius: 6px; -fx-padding: 6 12 6 12; -fx-cursor: hand;"
                    : "-fx-background-color: #DC2626; -fx-text-fill: #FFFFFF; -fx-font-size: 11.5px; -fx-font-weight: 800; -fx-background-radius: 6px; -fx-padding: 6 12 6 12; -fx-cursor: hand;");
                btnToggle.setOnAction(ev -> {
                    boolean nextState = !item.isInStock();
                    item.setInStock(nextState);
                    CafeServerService.getInstance().broadcastStockStatus(item.getId(), nextState, item.getName());
                    setLatestLog((nextState ? "Mở bán lại món: " : "Báo tạm hết (86): ") + item.getName());
                    txtFilter.fireEvent(new javafx.event.ActionEvent());
                });

                row.getChildren().addAll(dot, infoBox, btnToggle);
                listContainer.getChildren().add(row);
            }
        };

        txtFilter.textProperty().addListener((obs, o, n) -> renderList.run());
        txtFilter.setOnAction(e -> renderList.run());
        renderList.run();

        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        Button btnClose = new Button("Đóng");
        btnClose.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-size: 12.5px; -fx-font-weight: 800; -fx-background-radius: 8px; -fx-padding: 8 20 8 20; -fx-cursor: hand;");
        btnClose.setOnAction(e -> modal.close());
        footer.getChildren().add(btnClose);

        card.getChildren().addAll(headerRow, txtFilter, scroll, footer);

        WindowChromeHelper.makeDraggable(modal, card);
        StackPane wrap = new StackPane(card);
        wrap.setPadding(new Insets(16));
        wrap.setStyle("-fx-background-color: transparent;");
        Scene scene = new Scene(wrap);
        scene.setFill(Color.TRANSPARENT);
        WindowChromeHelper.applyTheme(scene);
        modal.setScene(scene);
        modal.show();
        AnimationHelper.playDialogEntrance(card);
    }

    private double computeDynamicCardWidth() {
        double w = ticketsScrollPane.getViewportBounds() != null && ticketsScrollPane.getViewportBounds().getWidth() > 0
                ? ticketsScrollPane.getViewportBounds().getWidth()
                : ticketsScrollPane.getWidth();
        double availableWidth = w - 36;
        if (availableWidth <= 0) {
            return 340;
        }
        int cols = Math.max(1, (int) Math.floor((availableWidth + 14) / (330 + 14)));
        return Math.floor((availableWidth - (cols - 1) * 14) / cols);
    }

    private void updateCardWidths() {
        double dynamicCardWidth = computeDynamicCardWidth();
        if (dynamicCardWidth <= 0) return;

        for (javafx.scene.Node node : ticketsFlowPane.getChildren()) {
            if (node instanceof OrderTicketCard) {
                OrderTicketCard card = (OrderTicketCard) node;
                card.setPrefWidth(dynamicCardWidth);
                card.setMinWidth(300);
                card.setMaxWidth(Double.MAX_VALUE);
            }
        }
    }

    public void renderOrders(List<Order> orders, BiConsumer<Order, String> onStatusChange) {
        ticketsFlowPane.getChildren().clear();

        if (orders.isEmpty()) {
            ticketsScrollPane.setVisible(false);
            ticketsScrollPane.setManaged(false);
            emptyTicketsBox.setVisible(true);
            emptyTicketsBox.setManaged(true);
            return;
        }

        emptyTicketsBox.setVisible(false);
        emptyTicketsBox.setManaged(false);
        ticketsScrollPane.setVisible(true);
        ticketsScrollPane.setManaged(true);

        double dynamicCardWidth = computeDynamicCardWidth();

        for (Order order : orders) {
            OrderTicketCard card = new OrderTicketCard(order, onStatusChange);
            if (dynamicCardWidth > 0) {
                card.setPrefWidth(dynamicCardWidth);
                card.setMinWidth(300);
                card.setMaxWidth(Double.MAX_VALUE);
            }
            ticketsFlowPane.getChildren().add(card);
        }

        javafx.application.Platform.runLater(this::updateCardWidths);
    }

    public void setLatestLog(String log) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        lblLatestLog.setText("[" + sdf.format(new Date()) + "] " + log);
    }

    private void startClock() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss • dd/MM/yyyy");
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> lblClock.setText(sdf.format(new Date()))), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    public TextField getSearchField() { return txtSearch; }
    public Button getClearDoneButton() { return btnClearDone; }
}
