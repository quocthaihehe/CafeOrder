package cafe.views.admin;

import cafe.models.CategorySales;
import cafe.models.Order;
import cafe.models.ProductSales;
import cafe.models.ReportSummary;
import cafe.services.DeviceService;
import cafe.services.OrderService;
import cafe.services.ReportService;
import cafe.services.TableService;
import cafe.utils.CurrencyFormatter;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;
import java.util.Map;

public class DashboardView extends ScrollPane {
    private final ReportService reportService;
    private final OrderService orderService;
    private final TableService tableService;
    private final DeviceService deviceService;

    public DashboardView(ReportService reportService, OrderService orderService, TableService tableService, DeviceService deviceService) {
        this.reportService = reportService;
        this.orderService = orderService;
        this.tableService = tableService;
        this.deviceService = deviceService;

        initUI();
    }

    private void initUI() {
        setFitToWidth(true);
        setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox root = new VBox(22);
        root.setPadding(new Insets(4, 8, 24, 8));

        // 1. Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox titleBox = new VBox(2);
        Label lblTitle = new Label("Tổng Quan Hoạt Động (Dashboard)");
        lblTitle.getStyleClass().add("page-title");
        Label lblSub = new Label("Dữ liệu vận hành thời gian thực của quán café");
        lblSub.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnRefresh = new Button("🔄 Cập nhật");
        btnRefresh.getStyleClass().add("btn-secondary");
        btnRefresh.setOnAction(e -> initUI());

        header.getChildren().addAll(titleBox, spacer, btnRefresh);

        // 2. KPI Cards
        ReportSummary summary = reportService.getReportSummary("TODAY");
        long activeTablesCount = tableService.getAllTables().stream().filter(t -> "OCCUPIED".equalsIgnoreCase(t.getStatus())).count();
        long onlineDevicesCount = deviceService.getAllDevices().stream().filter(d -> "ONLINE".equalsIgnoreCase(d.getConnectionStatus())).count();

        HBox kpiGrid = new HBox(16);
        kpiGrid.getChildren().addAll(
                createKpiCard("💰 Doanh Thu Hôm Nay", summary.getFormattedTotalRevenue(), "Tổng từ các đơn đã thu tiền", "#2E7D32"),
                createKpiCard("📋 Tổng Số Đơn", String.valueOf(summary.getTotalOrders()) + " đơn", summary.getCompletedOrders() + " đơn hoàn tất", "#1565C0"),
                createKpiCard("🪑 Bàn Đang Phục Vụ", activeTablesCount + " bàn", "Tổng " + tableService.getAllTables().size() + " bàn trong quán", "#E65100"),
                createKpiCard("📱 Thiết Bị Kiosk Online", onlineDevicesCount + " máy", "Sẵn sàng nhận order tại bàn", "#4527A0")
        );
        for (javafx.scene.Node card : kpiGrid.getChildren()) {
            HBox.setHgrow(card, Priority.ALWAYS);
        }

        // 3. Middle Section: Status Overview & Weekly Revenue
        HBox middleBox = new HBox(18);

        // 3.1 Order Status Breakdown
        VBox statusCard = new VBox(14);
        statusCard.getStyleClass().add("kpi-card");
        HBox.setHgrow(statusCard, Priority.ALWAYS);

        Label lblStatusTitle = new Label("Trạng Thái Đơn Hàng Hôm Nay");
        lblStatusTitle.setStyle("-fx-font-weight: 800; -fx-font-size: 14px; -fx-text-fill: #2C1810;");

        GridPane statusGrid = new GridPane();
        statusGrid.setHgap(12);
        statusGrid.setVgap(10);

        List<Order> allOrders = orderService.getAllOrders();
        addStatusBadge(statusGrid, "CREATED", countByStatus(allOrders, "CREATED"), 0, 0, "#FFF8E1", "#F57F17");
        addStatusBadge(statusGrid, "CONFIRMED", countByStatus(allOrders, "CONFIRMED"), 1, 0, "#E3F2FD", "#1976D2");
        addStatusBadge(statusGrid, "PREPARING", countByStatus(allOrders, "PREPARING"), 2, 0, "#EDE7F6", "#512DA8");
        addStatusBadge(statusGrid, "READY", countByStatus(allOrders, "READY"), 0, 1, "#E8F5E9", "#388E3C");
        addStatusBadge(statusGrid, "SERVED", countByStatus(allOrders, "SERVED"), 1, 1, "#E0F2F1", "#00796B");
        addStatusBadge(statusGrid, "WAITING_PAYMENT", countByStatus(allOrders, "WAITING_PAYMENT"), 2, 1, "#FBE9E7", "#D84315");
        addStatusBadge(statusGrid, "PAID / COMPLETED", countByStatus(allOrders, "PAID") + countByStatus(allOrders, "COMPLETED"), 0, 2, "#E8F8F5", "#117A65");
        addStatusBadge(statusGrid, "CANCELLED", countByStatus(allOrders, "CANCELLED"), 1, 2, "#FFEBEE", "#C62828");

        statusCard.getChildren().addAll(lblStatusTitle, statusGrid);

        // 3.2 Weekly Revenue Trends
        VBox revCard = new VBox(14);
        revCard.getStyleClass().add("kpi-card");
        revCard.setPrefWidth(380);

        Label lblRevTitle = new Label("Doanh Thu 7 Ngày Gần Nhất");
        lblRevTitle.setStyle("-fx-font-weight: 800; -fx-font-size: 14px; -fx-text-fill: #2C1810;");

        VBox revList = new VBox(8);
        Map<String, Double> weekly = reportService.getWeeklyRevenue();
        double maxRev = 3500000.0;

        for (Map.Entry<String, Double> entry : weekly.entrySet()) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            Label day = new Label(entry.getKey());
            day.setPrefWidth(65);
            day.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 600; -fx-text-fill: #5D4A41;");

            ProgressBar bar = new ProgressBar(entry.getValue() / maxRev);
            bar.setPrefWidth(180);
            bar.setStyle("-fx-accent: #C97A44;");

            Label val = new Label(CurrencyFormatter.format(entry.getValue()));
            val.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #2C1810;");

            row.getChildren().addAll(day, bar, val);
            revList.getChildren().add(row);
        }

        revCard.getChildren().addAll(lblRevTitle, revList);
        middleBox.getChildren().addAll(statusCard, revCard);

        // 4. Bottom Section: Top Selling Products & Recent Orders
        HBox bottomBox = new HBox(18);

        // 4.1 Top Products
        VBox topBox = new VBox(12);
        topBox.getStyleClass().add("kpi-card");
        topBox.setPrefWidth(420);

        Label lblTop = new Label("🏆 Món Bán Chạy Nhất");
        lblTop.setStyle("-fx-font-weight: 800; -fx-font-size: 14px; -fx-text-fill: #2C1810;");

        TableView<ProductSales> tableTop = new TableView<>();
        tableTop.setPrefHeight(230);
        tableTop.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<ProductSales, String> colName = new TableColumn<>("Tên món");
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProductName()));

        TableColumn<ProductSales, String> colQty = new TableColumn<>("SL Bán");
        colQty.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getQuantitySold())));
        colQty.setStyle("-fx-alignment: CENTER;");

        TableColumn<ProductSales, String> colRev = new TableColumn<>("Doanh thu");
        colRev.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedRevenue()));
        colRev.setStyle("-fx-alignment: CENTER_RIGHT;");

        tableTop.getColumns().addAll(colName, colQty, colRev);
        tableTop.getItems().addAll(reportService.getProductSalesReport("QTY").stream().limit(6).toList());

        topBox.getChildren().addAll(lblTop, tableTop);

        // 4.2 Recent Orders
        VBox recentBox = new VBox(12);
        recentBox.getStyleClass().add("kpi-card");
        HBox.setHgrow(recentBox, Priority.ALWAYS);

        Label lblRecent = new Label("⏱️ Đơn Hàng Gần Đây");
        lblRecent.setStyle("-fx-font-weight: 800; -fx-font-size: 14px; -fx-text-fill: #2C1810;");

        TableView<Order> tableRecent = new TableView<>();
        tableRecent.setPrefHeight(230);
        tableRecent.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Order, String> colId = new TableColumn<>("Mã đơn");
        colId.setCellValueFactory(d -> new SimpleStringProperty("#" + d.getValue().getOrderId()));
        colId.setPrefWidth(70);

        TableColumn<Order, String> colTable = new TableColumn<>("Bàn");
        colTable.setCellValueFactory(d -> new SimpleStringProperty("Bàn " + d.getValue().getTable()));
        colTable.setPrefWidth(65);

        TableColumn<Order, String> colTime = new TableColumn<>("Thời gian");
        colTime.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedTime()));
        colTime.setPrefWidth(80);

        TableColumn<Order, String> colItems = new TableColumn<>("Món");
        colItems.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTotalQuantity() + " món"));
        colItems.setPrefWidth(65);

        TableColumn<Order, String> colTotal = new TableColumn<>("Tổng tiền");
        colTotal.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedTotal()));
        colTotal.setPrefWidth(95);

        TableColumn<Order, String> colSt = new TableColumn<>("Trạng thái");
        colSt.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        colSt.setPrefWidth(110);

        tableRecent.getColumns().addAll(colId, colTable, colTime, colItems, colTotal, colSt);
        tableRecent.getItems().addAll(orderService.getRecentOrders(6));

        recentBox.getChildren().addAll(lblRecent, tableRecent);
        bottomBox.getChildren().addAll(topBox, recentBox);

        root.getChildren().addAll(header, kpiGrid, middleBox, bottomBox);
        setContent(root);
    }

    private VBox createKpiCard(String title, String value, String sub, String accentColor) {
        VBox card = new VBox(6);
        card.getStyleClass().add("kpi-card");

        Label t = new Label(title);
        t.getStyleClass().add("kpi-title");

        Label v = new Label(value);
        v.getStyleClass().add("kpi-value");
        v.setStyle("-fx-text-fill: " + accentColor + ";");

        Label s = new Label(sub);
        s.getStyleClass().add("kpi-sub");

        card.getChildren().addAll(t, v, s);
        return card;
    }

    private void addStatusBadge(GridPane grid, String status, long count, int col, int row, String bg, String fg) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 8; -fx-padding: 8 12;");

        Label lblName = new Label(status);
        lblName.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: " + fg + ";");
        HBox.setHgrow(lblName, Priority.ALWAYS);

        Label lblCount = new Label(String.valueOf(count));
        lblCount.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: " + fg + ";");

        box.getChildren().addAll(lblName, lblCount);
        grid.add(box, col, row);
    }

    private long countByStatus(List<Order> orders, String status) {
        return orders.stream().filter(o -> status.equalsIgnoreCase(o.getStatus())).count();
    }
}
