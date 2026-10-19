package cafe.views.admin;

import cafe.models.CategorySales;
import cafe.models.ProductSales;
import cafe.models.ReportSummary;
import cafe.services.ReportService;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;

public class ReportsView extends VBox {
    private final ReportService reportService;

    private final ComboBox<String> cbPeriod = new ComboBox<>();
    private final Label lblRevVal = new Label();
    private final Label lblOrderVal = new Label();
    private final Label lblAvgVal = new Label();
    private final Label lblCompleteVal = new Label();
    private final Label lblCancelVal = new Label();

    private final TableView<ProductSales> tableProducts = new TableView<>();
    private final TableView<CategorySales> tableCategories = new TableView<>();

    public ReportsView(ReportService reportService) {
        this.reportService = reportService;

        setSpacing(16);
        setPadding(new Insets(4, 8, 20, 8));
        initUI();
        refreshData();
    }

    private void initUI() {
        // 1. Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label lblTitle = new Label("Báo Cáo & Thống Kê Hoạt Động (Reports)");
        lblTitle.getStyleClass().add("page-title");
        Label lblSub = new Label("Phân tích doanh thu, sản lượng tiêu thụ và hiệu suất theo danh mục");
        lblSub.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        cbPeriod.getItems().addAll("Hôm nay (Today)", "Tuần này (This Week)", "Tháng này (This Month)", "Tất cả (All Time)");
        cbPeriod.setValue("Tháng này (This Month)");
        cbPeriod.setOnAction(e -> refreshData());

        header.getChildren().addAll(titleBox, spacer, new Label("Kỳ thống kê:"), cbPeriod);

        // 2. Summary KPI Cards
        HBox kpiRow = new HBox(16);
        kpiRow.getChildren().addAll(
                createCard("💰 TỔNG DOANH THU", lblRevVal, "#2E7D32"),
                createCard("📦 TỔNG ĐƠN HÀNG", lblOrderVal, "#1565C0"),
                createCard("🏷️ GIÁ TRỊ TRUNG BÌNH/ĐƠN", lblAvgVal, "#C97A44"),
                createCard("✅ ĐƠN HOÀN TẤT", lblCompleteVal, "#388E3C"),
                createCard("❌ ĐƠN BỊ HỦY", lblCancelVal, "#C62828")
        );
        for (javafx.scene.Node node : kpiRow.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
        }

        // 3. TabPane for Detailed Breakdown
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // Tab 1: Product Sales
        Tab tabProd = new Tab("🏆 Doanh Số Theo Món (Product Sales)");
        VBox boxProd = new VBox(10);
        boxProd.setPadding(new Insets(12));

        HBox prodBar = new HBox(10);
        prodBar.setAlignment(Pos.CENTER_LEFT);
        Label lblSortBy = new Label("Sắp xếp theo:");
        ComboBox<String> cbSort = new ComboBox<>();
        cbSort.getItems().addAll("Số lượng bán nhiều nhất", "Doanh thu cao nhất");
        cbSort.setValue("Số lượng bán nhiều nhất");
        cbSort.setOnAction(e -> {
            boolean byRev = cbSort.getValue().contains("Doanh thu");
            tableProducts.getItems().setAll(reportService.getProductSalesReport(byRev ? "REVENUE" : "QTY", cbPeriod.getValue()));
        });
        prodBar.getChildren().addAll(lblSortBy, cbSort);

        tableProducts.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tableProducts, Priority.ALWAYS);

        TableColumn<ProductSales, String> colPName = new TableColumn<>("Tên món");
        colPName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProductName()));
        TableColumn<ProductSales, String> colPCat = new TableColumn<>("Danh mục");
        colPCat.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory()));
        TableColumn<ProductSales, String> colPQty = new TableColumn<>("Số lượng đã bán");
        colPQty.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getQuantitySold() + " ly/phần"));
        colPQty.setStyle("-fx-alignment: CENTER; -fx-font-weight: 700;");
        TableColumn<ProductSales, String> colPRev = new TableColumn<>("Tổng doanh thu");
        colPRev.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedRevenue()));
        colPRev.setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: 800; -fx-text-fill: #2E7D32;");

        tableProducts.getColumns().addAll(colPName, colPCat, colPQty, colPRev);
        boxProd.getChildren().addAll(prodBar, tableProducts);
        tabProd.setContent(boxProd);

        // Tab 2: Category Sales
        Tab tabCat = new Tab("📊 Doanh Số Theo Danh Mục (Category Sales)");
        VBox boxCat = new VBox(10);
        boxCat.setPadding(new Insets(12));

        tableCategories.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tableCategories, Priority.ALWAYS);

        TableColumn<CategorySales, String> colCName = new TableColumn<>("Danh mục");
        colCName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategoryName()));
        TableColumn<CategorySales, String> colCOrders = new TableColumn<>("Số đơn hàng có món");
        colCOrders.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getOrderCount() + " đơn"));
        colCOrders.setStyle("-fx-alignment: CENTER;");
        TableColumn<CategorySales, String> colCRev = new TableColumn<>("Doanh thu");
        colCRev.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedRevenue()));
        colCRev.setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: 800; -fx-text-fill: #1565C0;");
        TableColumn<CategorySales, String> colCPct = new TableColumn<>("Tỷ trọng đóng góp");
        colCPct.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedPercentage()));
        colCPct.setStyle("-fx-alignment: CENTER; -fx-font-weight: 700;");

        tableCategories.getColumns().addAll(colCName, colCOrders, colCRev, colCPct);
        boxCat.getChildren().addAll(tableCategories);
        tabCat.setContent(boxCat);

        tabPane.getTabs().addAll(tabProd, tabCat);

        getChildren().addAll(header, kpiRow, tabPane);
    }

    private void refreshData() {
        String period = cbPeriod.getValue();
        ReportSummary s = reportService.getReportSummary(period);
        lblRevVal.setText(s.getFormattedTotalRevenue());
        lblOrderVal.setText(s.getTotalOrders() + " đơn");
        lblAvgVal.setText(s.getFormattedAvgOrderValue());
        lblCompleteVal.setText(s.getCompletedOrders() + " đơn");
        lblCancelVal.setText(s.getCancelledOrders() + " đơn");

        tableProducts.getItems().setAll(reportService.getProductSalesReport("QTY", period));
        tableCategories.getItems().setAll(reportService.getCategorySalesReport(period));
    }

    private VBox createCard(String title, Label valLabel, String color) {
        VBox card = new VBox(6);
        card.getStyleClass().add("kpi-card");
        Label t = new Label(title);
        t.getStyleClass().add("kpi-title");
        valLabel.getStyleClass().add("kpi-value");
        valLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 19px;");
        card.getChildren().addAll(t, valLabel);
        return card;
    }
}
