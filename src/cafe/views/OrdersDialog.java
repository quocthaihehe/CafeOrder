package cafe.views;

import cafe.models.Order;
import cafe.models.OrderItem;
import cafe.services.CafeServerService;
import cafe.utils.AnimationHelper;
import cafe.utils.CurrencyFormatter;
import cafe.utils.SvgIcons;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * OrdersDialog - Quản lý Đơn hàng Thời gian thực cho Nhân viên (Sprint 4.1):
 * - Hỗ trợ cả chế độ Dialog độc lập và chế độ Nhúng trực tiếp (Embedded Panel) trong Staff Shell.
 * - Đồng bộ trực tiếp với CafeServerService.getInstance().getActiveOrders().
 * - Nút In phiếu (mở OrderSuccessDialog), Thu tiền & Trả bàn, và Hủy đơn hoạt động 100%.
 */
public class OrdersDialog extends Stage {

    private String currentFilter = "SERVING"; // "SERVING", "COMPLETED", "CANCELLED"
    private String searchKeyword = "";
    private final VBox cardsContainer = new VBox(10);
    private final Stage ownerStage;
    private final Runnable onOrderChangedCallback;

    private Button tabServing;
    private Button tabCompleted;
    private Button tabCancelled;
    private Label lblSummaryCount;

    public OrdersDialog() {
        this(null, null);
    }

    public OrdersDialog(Stage parentStage) {
        this(parentStage, null);
    }

    public OrdersDialog(Stage parentStage, Runnable onOrderChangedCallback) {
        this.ownerStage = parentStage;
        this.onOrderChangedCallback = onOrderChangedCallback;
        if (parentStage != null) {
            initOwner(parentStage);
        }
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.TRANSPARENT);
        setTitle("Quản Lý Đơn Hàng • Brew & Bean CAFE");
        setResizable(false);

        initUI();
    }

    /**
     * Tạo Panel Quản lý Đơn hàng nhúng trực tiếp vào vùng Center của FloorPlanView (Unified Staff Shell)
     */
    public static VBox createEmbeddedPanel(Stage parentStage, Runnable onBackToFloorPlan, Runnable onOrderChanged) {
        OrdersDialog helper = new OrdersDialog(parentStage, onOrderChanged);
        return helper.buildPanelContent(true, onBackToFloorPlan);
    }

    private void initUI() {
        VBox root = buildPanelContent(false, this::close);
        root.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16px; -fx-border-color: #E2DDD5; -fx-border-radius: 16px; -fx-border-width: 1px; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.22), 24, 0.12, 0, 8);");
        root.setPrefWidth(560);
        root.setMaxWidth(580);
        root.setPrefHeight(660);

        AnimationHelper.playDialogEntrance(root);

        StackPane sceneRoot = new StackPane(root);
        sceneRoot.setStyle("-fx-background-color: transparent;");
        sceneRoot.setPadding(new Insets(16));

        Scene scene = new Scene(sceneRoot);
        scene.setFill(Color.TRANSPARENT);
        cafe.utils.WindowChromeHelper.applyTheme(scene);

        setScene(scene);
    }

    private VBox buildPanelContent(boolean embedded, Runnable onBackAction) {
        VBox root = new VBox(14);
        root.setPadding(embedded ? new Insets(22, 28, 22, 28) : new Insets(20, 22, 20, 22));
        if (embedded) {
            root.setStyle("-fx-background-color: #F4EFE6;");
            VBox.setVgrow(root, Priority.ALWAYS);
        }

        // 1. Header Row
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        if (!embedded) {
            headerRow.setStyle("-fx-cursor: move; -fx-padding: 0 0 4 0;");
            cafe.utils.WindowChromeHelper.makeDraggable(this, headerRow);
        }

        Button btnBack = new Button("←");
        btnBack.setStyle("-fx-background-color: #EAE4DA; -fx-text-fill: #112823; -fx-font-size: 14px; -fx-font-weight: 800; -fx-cursor: hand; -fx-background-radius: 10px; -fx-min-width: 34px; -fx-min-height: 34px; -fx-pref-width: 34px; -fx-pref-height: 34px; -fx-padding: 0; -fx-alignment: center;");
        btnBack.setOnAction(e -> {
            if (onBackAction != null) onBackAction.run();
        });

        VBox titleCol = new VBox(2);
        Label lblTitle = new Label("Quản lý Đơn hàng tại Quầy & Bàn");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        lblSummaryCount = new Label("Đồng bộ thời gian thực với KDS Pha chế & Sơ đồ bàn");
        lblSummaryCount.setStyle("-fx-font-size: 12px; -fx-text-fill: #78716C; -fx-font-weight: 500;");
        titleCol.getChildren().addAll(lblTitle, lblSummaryCount);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        // Search input
        TextField txtSearch = new TextField();
        txtSearch.setPromptText("Tìm mã đơn (#1001) hoặc số bàn...");
        txtSearch.setPrefWidth(220);
        txtSearch.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #DFD8CE; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 7 12 7 12; -fx-font-size: 12.5px;");
        txtSearch.textProperty().addListener((obs, oldV, newV) -> {
            searchKeyword = newV != null ? newV.trim().toLowerCase() : "";
            renderOrders();
        });

        Button btnRefresh = new Button("↻ Làm mới");
        btnRefresh.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #112823; -fx-font-size: 12px; -fx-font-weight: 700; -fx-border-color: #D8CEBE; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 7 14 7 14; -fx-cursor: hand;");
        btnRefresh.setOnAction(e -> renderOrders());

        headerRow.getChildren().addAll(btnBack, titleCol, sp, txtSearch, btnRefresh);

        if (!embedded) {
            Button btnClose = new Button("✕");
            btnClose.setStyle("-fx-background-color: #F5EFEB; -fx-text-fill: #563521; -fx-font-weight: 800; -fx-background-radius: 14px; -fx-min-width: 30px; -fx-min-height: 30px; -fx-cursor: hand;");
            btnClose.setOnAction(e -> close());
            headerRow.getChildren().add(btnClose);
        }

        // 2. Three Filter Tabs: [ Đang phục vụ ], [ Đã hoàn tất / Thanh toán ], [ Đã hủy ]
        HBox tabsRow = new HBox(12);
        tabsRow.setAlignment(Pos.CENTER_LEFT);
        tabsRow.setPadding(new Insets(0, 0, 8, 0));
        tabsRow.setStyle("-fx-border-color: transparent transparent #E2DCD2 transparent; -fx-border-width: 0 0 1 0;");

        tabServing = new Button("Đang phục vụ");
        tabCompleted = new Button("Hoàn thành / Đã thu tiền");
        tabCancelled = new Button("Đã hủy");

        tabServing.setOnAction(e -> switchFilter("SERVING"));
        tabCompleted.setOnAction(e -> switchFilter("COMPLETED"));
        tabCancelled.setOnAction(e -> switchFilter("CANCELLED"));

        updateTabStyles();
        tabsRow.getChildren().addAll(tabServing, tabCompleted, tabCancelled);

        // 3. Scrollable Order Cards List
        cardsContainer.setSpacing(10);
        cardsContainer.setPadding(new Insets(4, 2, 12, 2));

        ScrollPane scroll = new ScrollPane(cardsContainer);
        scroll.setFitToWidth(true);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().addAll(headerRow, tabsRow, scroll);
        renderOrders();

        CafeServerService srv = CafeServerService.getInstance();
        srv.setOnOrderReceived(o -> javafx.application.Platform.runLater(this::renderOrders));
        srv.setOnOrderStatusChanged((id, st) -> javafx.application.Platform.runLater(this::renderOrders));
        srv.setOnTableStatusBroadcasted((tbl, st) -> javafx.application.Platform.runLater(this::renderOrders));

        return root;
    }

    private void switchFilter(String filter) {
        this.currentFilter = filter;
        updateTabStyles();
        renderOrders();
    }

    private void updateTabStyles() {
        String active = "-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 7 16 7 16; -fx-cursor: hand;";
        String inactive = "-fx-background-color: #EAE4DA; -fx-text-fill: #57534E; -fx-font-size: 12.5px; -fx-font-weight: 600; -fx-background-radius: 8px; -fx-padding: 7 16 7 16; -fx-cursor: hand;";

        if (tabServing != null) tabServing.setStyle("SERVING".equals(currentFilter) ? active : inactive);
        if (tabCompleted != null) tabCompleted.setStyle("COMPLETED".equals(currentFilter) ? active : inactive);
        if (tabCancelled != null) tabCancelled.setStyle("CANCELLED".equals(currentFilter) ? active : inactive);
    }

    public void renderOrders() {
        cardsContainer.getChildren().clear();

        CafeServerService server = CafeServerService.getInstance();
        List<Order> allOrders = new ArrayList<>(server.getActiveOrders().values());
        allOrders.sort(Comparator.comparingLong(Order::getTimestamp).reversed());

        int countServing = 0;
        int countCompleted = 0;
        int countCancelled = 0;

        List<Order> filtered = new ArrayList<>();
        for (Order o : allOrders) {
            String st = o.getStatus() != null ? o.getStatus().toUpperCase() : "QUEUED";
            boolean isCancel = "CANCELLED".equals(st);
            boolean isPaid = "PAID".equals(st);
            boolean isServing = !isCancel && !isPaid;

            if (isServing) countServing++;
            else if (isPaid) countCompleted++;
            else countCancelled++;

            boolean matchTab = ("SERVING".equals(currentFilter) && isServing)
                || ("COMPLETED".equals(currentFilter) && isPaid)
                || ("CANCELLED".equals(currentFilter) && isCancel);

            if (!matchTab) continue;

            if (!searchKeyword.isEmpty()) {
                String idStr = "#" + o.getOrderId();
                String tblStr = o.getOrderTypeLabel().toLowerCase();
                if (!idStr.contains(searchKeyword) && !tblStr.contains(searchKeyword)) {
                    continue;
                }
            }
            filtered.add(o);
        }

        if (tabServing != null) tabServing.setText("Đang phục vụ (" + countServing + ")");
        if (tabCompleted != null) tabCompleted.setText("Đã thanh toán (" + countCompleted + ")");
        if (tabCancelled != null) tabCancelled.setText("Đã hủy (" + countCancelled + ")");

        if (filtered.isEmpty()) {
            showEmptyOrders();
            return;
        }

        for (Order order : filtered) {
            cardsContainer.getChildren().add(createOrderCard(order));
        }
    }

    private HBox createOrderCard(Order order) {
        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14, 18, 14, 18));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12px; -fx-border-color: #E6E0D6; -fx-border-radius: 12px; -fx-border-width: 1px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 10, 0.1, 0, 2);");

        // Icon bàn 🪑
        StackPane iconWrap = new StackPane();
        iconWrap.setPrefSize(44, 44);
        iconWrap.setMinSize(44, 44);
        iconWrap.setStyle("-fx-background-color: #F4EFE6; -fx-background-radius: 10px;");
        javafx.scene.Node tableIcon = cafe.utils.IconHelper.getIcon("staff", "so-do-ban.png", SvgIcons.TABLE, "🪑", 22, Color.web("#112823"));
        iconWrap.getChildren().add(tableIcon);

        // Thông tin bàn, mã đơn & chi tiết món
        VBox infoBox = new VBox(4);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        String tableTitle = order.getTable() > 0
            ? ("Bàn " + (order.getTable() < 10 ? "0" + order.getTable() : order.getTable()) + "  •  Đơn #" + order.getOrderId())
            : ("Mang về  •  Đơn #" + order.getOrderId());
        Label lblTable = new Label(tableTitle);
        lblTable.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        StringBuilder itemsPreview = new StringBuilder();
        if (order.getItems() != null) {
            for (int i = 0; i < order.getItems().size(); i++) {
                OrderItem it = order.getItems().get(i);
                if (i > 0) itemsPreview.append(", ");
                itemsPreview.append(it.getQty()).append("x ").append(it.getName());
            }
        }
        Label lblItems = new Label(itemsPreview.toString());
        lblItems.setStyle("-fx-font-size: 12px; -fx-text-fill: #57534E;");
        lblItems.setWrapText(true);

        String summary = order.getTotalQuantity() + " món  •  Tổng cộng: " + CurrencyFormatter.formatVND(order.getTotalAmount());
        Label lblSummary = new Label(summary);
        lblSummary.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #B45309;");

        infoBox.getChildren().addAll(lblTable, lblItems, lblSummary);

        // Right side: Badge trạng thái + Giờ gọi + Nút thao tác
        VBox rightBox = new VBox(8);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        HBox topStatusRow = new HBox(8);
        topStatusRow.setAlignment(Pos.CENTER_RIGHT);

        String st = order.getStatus() != null ? order.getStatus().toUpperCase() : "QUEUED";
        String badgeLabel;
        String badgeBg;
        String badgeFg;
        if ("QUEUED".equals(st)) {
            badgeLabel = "Chờ pha chế";
            badgeBg = "#FEF3C7";
            badgeFg = "#B45309";
        } else if ("PREPARING".equals(st)) {
            badgeLabel = "Đang pha chế";
            badgeBg = "#DBEAFE";
            badgeFg = "#1D4ED8";
        } else if ("DONE".equals(st)) {
            badgeLabel = "Đã lên món";
            badgeBg = "#DCFCE7";
            badgeFg = "#15803D";
        } else if ("PAID".equals(st)) {
            badgeLabel = "Đã thanh toán";
            badgeBg = "#EAE6DE";
            badgeFg = "#44403C";
        } else {
            badgeLabel = "Đã hủy";
            badgeBg = "#FEE2E2";
            badgeFg = "#B91C1C";
        }

        Label badge = new Label(badgeLabel);
        badge.setStyle("-fx-background-color: " + badgeBg + "; -fx-text-fill: " + badgeFg + "; -fx-font-size: 11px; -fx-font-weight: 700; -fx-background-radius: 999px; -fx-padding: 3 10 3 10;");

        Label lblTime = new Label(order.getFormattedTime());
        lblTime.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 600; -fx-text-fill: #78716C;");

        topStatusRow.getChildren().addAll(badge, lblTime);

        // Action buttons: In hóa đơn, Thu tiền & Trả bàn, Hủy đơn
        HBox actionsRow = new HBox(8);
        actionsRow.setAlignment(Pos.CENTER_RIGHT);

        Button btnPrint = new Button("🖨 In phiếu");
        btnPrint.setStyle("-fx-background-color: #F5EFEB; -fx-text-fill: #112823; -fx-font-size: 11.5px; -fx-font-weight: 700; -fx-background-radius: 6px; -fx-padding: 5 10 5 10; -fx-cursor: hand;");
        btnPrint.setOnAction(e -> {
            OrderSuccessDialog.showReceiptForOrder(ownerStage, order, "Thu ngân POS");
        });

        boolean isActive = !"PAID".equals(st) && !"CANCELLED".equals(st);
        if (isActive) {
            Button btnPay = new Button("💳 Thu tiền");
            btnPay.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-size: 11.5px; -fx-font-weight: 700; -fx-background-radius: 6px; -fx-padding: 5 10 5 10; -fx-cursor: hand;");
            btnPay.setOnAction(e -> {
                if (order.getTable() > 0) {
                    CafeServerService.getInstance().checkoutTable(order.getTable());
                } else {
                    CafeServerService.getInstance().updateOrderStatus(order.getOrderId(), "PAID");
                }
                renderOrders();
                if (onOrderChangedCallback != null) onOrderChangedCallback.run();
            });

            Button btnCancel = new Button("✕ Hủy");
            btnCancel.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-font-size: 11.5px; -fx-font-weight: 700; -fx-background-radius: 6px; -fx-padding: 5 10 5 10; -fx-cursor: hand;");
            btnCancel.setOnAction(e -> {
                CafeServerService.getInstance().cancelOrder(order.getOrderId());
                renderOrders();
                if (onOrderChangedCallback != null) onOrderChangedCallback.run();
            });

            actionsRow.getChildren().addAll(btnPrint, btnPay, btnCancel);
        } else {
            actionsRow.getChildren().add(btnPrint);
        }

        rightBox.getChildren().addAll(topStatusRow, actionsRow);
        card.getChildren().addAll(iconWrap, infoBox, rightBox);
        return card;
    }

    private void showEmptyOrders() {
        VBox emptyBox = new VBox(8);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setPadding(new Insets(60, 20, 60, 20));
        Label lblEmpty = new Label("Không có đơn hàng nào trong mục này.");
        lblEmpty.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #78716C;");
        emptyBox.getChildren().add(lblEmpty);
        cardsContainer.getChildren().add(emptyBox);
    }
}
