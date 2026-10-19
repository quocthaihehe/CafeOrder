package cafe.views.admin;

import cafe.models.Order;
import cafe.models.OrderItem;
import cafe.services.OrderService;
import cafe.utils.CurrencyFormatter;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;
import java.util.Optional;

public class OrderManagementView extends VBox {
    private final OrderService orderService;
    private final String currentAdmin;

    private final TableView<Order> table = new TableView<>();
    private final TextField txtSearch = new TextField();
    private final ComboBox<String> cbStatusFilter = new ComboBox<>();
    private final ComboBox<String> cbDateFilter = new ComboBox<>();
    private final Label lblCount = new Label();
    private final VBox emptyState = new VBox(10);

    public OrderManagementView(OrderService orderService, String currentAdmin) {
        this.orderService = orderService;
        this.currentAdmin = currentAdmin;

        setSpacing(16);
        setPadding(new Insets(4, 8, 16, 8));
        initUI();
        refreshTable();
    }

    private void initUI() {
        // 1. Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label lblTitle = new Label("Quản Lý Đơn Hàng (Orders)");
        lblTitle.getStyleClass().add("page-title");
        Label lblSub = new Label("Theo dõi danh sách hóa đơn, tiến trình chế biến và dòng thời gian của đơn");
        lblSub.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(titleBox, spacer);

        // 2. Filter Bar
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        txtSearch.setPromptText("🔍 Tìm mã đơn (#1025), số bàn...");
        txtSearch.setPrefWidth(260);
        txtSearch.textProperty().addListener((obs, o, n) -> refreshTable());

        cbStatusFilter.getItems().addAll("Tất cả trạng thái", "CREATED", "CONFIRMED", "PREPARING", "READY", "SERVED", "WAITING_PAYMENT", "PAID", "COMPLETED", "CANCELLED");
        cbStatusFilter.setValue("Tất cả trạng thái");
        cbStatusFilter.setOnAction(e -> refreshTable());

        cbDateFilter.getItems().addAll("Tất cả thời gian", "Hôm nay (Today)", "Tuần này (This Week)", "Tháng này (This Month)");
        cbDateFilter.setValue("Tất cả thời gian");
        cbDateFilter.setOnAction(e -> refreshTable());

        Button btnClear = new Button("Đặt lại");
        btnClear.getStyleClass().add("btn-secondary");
        btnClear.setOnAction(e -> {
            txtSearch.clear();
            cbStatusFilter.setValue("Tất cả trạng thái");
            cbDateFilter.setValue("Tất cả thời gian");
            refreshTable();
        });

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        lblCount.setStyle("-fx-font-weight: 700; -fx-text-fill: #7D6B63;");

        filterBar.getChildren().addAll(txtSearch, cbStatusFilter, cbDateFilter, btnClear, spacer2, lblCount);

        // 3. Table
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Order, String> colId = new TableColumn<>("Mã đơn");
        colId.setCellValueFactory(d -> new SimpleStringProperty("#" + d.getValue().getOrderId()));
        colId.setPrefWidth(70);
        colId.setStyle("-fx-alignment: CENTER; -fx-font-weight: 700;");

        TableColumn<Order, String> colTable = new TableColumn<>("Bàn");
        colTable.setCellValueFactory(d -> new SimpleStringProperty("Bàn " + d.getValue().getTable()));
        colTable.setPrefWidth(70);
        colTable.setStyle("-fx-alignment: CENTER;");

        TableColumn<Order, String> colTime = new TableColumn<>("Thời gian đặt");
        colTime.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedDateTime()));
        colTime.setPrefWidth(130);

        TableColumn<Order, String> colBy = new TableColumn<>("Người tạo");
        colBy.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCreatedBy()));
        colBy.setPrefWidth(110);

        TableColumn<Order, String> colItems = new TableColumn<>("Số lượng món");
        colItems.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTotalQuantity() + " món"));
        colItems.setPrefWidth(100);
        colItems.setStyle("-fx-alignment: CENTER;");

        TableColumn<Order, String> colTotal = new TableColumn<>("Tổng tiền");
        colTotal.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedTotal()));
        colTotal.setPrefWidth(110);
        colTotal.setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: 800;");

        TableColumn<Order, String> colStatus = new TableColumn<>("Trạng thái");
        colStatus.setPrefWidth(130);
        colStatus.setStyle("-fx-alignment: CENTER;");
        colStatus.setCellFactory(col -> new TableCell<Order, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Order o = getTableRow().getItem();
                    Label badge = new Label(o.getStatus());
                    String st = o.getStatus().toUpperCase();
                    if (st.equals("COMPLETED") || st.equals("PAID")) {
                        badge.getStyleClass().add("badge-active");
                    } else if (st.equals("CANCELLED")) {
                        badge.getStyleClass().add("badge-disabled");
                    } else if (st.equals("PREPARING") || st.equals("CONFIRMED")) {
                        badge.getStyleClass().add("badge-info");
                    } else {
                        badge.getStyleClass().add("badge-warning");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<Order, Void> colActions = new TableColumn<>("Thao tác");
        colActions.setPrefWidth(160);
        colActions.setStyle("-fx-alignment: CENTER;");
        colActions.setCellFactory(col -> new TableCell<Order, Void>() {
            private final Button btnDetail = new Button("Chi tiết");
            private final Button btnChangeSt = new Button("Đổi TT");
            private final HBox pane = new HBox(6, btnDetail, btnChangeSt);

            {
                pane.setAlignment(Pos.CENTER);
                btnDetail.getStyleClass().add("btn-sm-action");
                btnChangeSt.getStyleClass().add("btn-sm-action");

                btnDetail.setOnAction(e -> {
                    Order o = getTableView().getItems().get(getIndex());
                    showOrderDetailDialog(o);
                });

                btnChangeSt.setOnAction(e -> {
                    Order o = getTableView().getItems().get(getIndex());
                    showChangeStatusDialog(o);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(pane);
            }
        });

        table.getColumns().addAll(colId, colTable, colTime, colBy, colItems, colTotal, colStatus, colActions);

        // Empty state
        emptyState.getStyleClass().add("empty-state-box");
        Label lblEmptyTitle = new Label("Không tìm thấy đơn hàng");
        lblEmptyTitle.getStyleClass().add("empty-state-title");
        Label lblEmptySub = new Label("Không có đơn hàng nào thỏa mãn điều kiện lọc.");
        lblEmptySub.getStyleClass().add("empty-state-sub");
        emptyState.getChildren().addAll(lblEmptyTitle, lblEmptySub);

        StackPane tableContainer = new StackPane(table, emptyState);
        VBox.setVgrow(tableContainer, Priority.ALWAYS);

        getChildren().addAll(header, filterBar, tableContainer);
    }

    private void refreshTable() {
        String kw = txtSearch.getText();
        String st = cbStatusFilter.getValue();
        String status = (st == null || "Tất cả trạng thái".equals(st)) ? "ALL" : st;
        String dt = cbDateFilter.getValue();
        String dateFilter = (dt == null || "Tất cả thời gian".equals(dt)) ? "ALL" : dt;

        List<Order> list = orderService.searchOrders(kw, status, dateFilter);
        table.getItems().setAll(list);
        lblCount.setText(list.size() + " đơn hàng");

        boolean isEmpty = list.isEmpty();
        emptyState.setVisible(isEmpty);
        emptyState.setManaged(isEmpty);
        table.setVisible(!isEmpty);
        table.setManaged(!isEmpty);
    }

    private void showOrderDetailDialog(Order order) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Chi Tiết Đơn Hàng #" + order.getOrderId());
        dialog.setHeaderText("Bàn " + order.getTable() + " • Người tạo: " + order.getCreatedBy() + " • " + order.getFormattedDateTime());

        VBox content = new VBox(14);
        content.setPadding(new Insets(16));
        content.setPrefWidth(580);

        // Status Badge Header & Request ID
        HBox statusRow = new HBox(8);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        Label lblStText = new Label("Trạng thái hiện tại: ");
        lblStText.setStyle("-fx-font-weight: 700;");
        Label badge = new Label(order.getStatus());
        badge.getStyleClass().add("badge-info");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblReqId = new Label("Request ID: " + (order.getRequestId() != null ? order.getRequestId() : "N/A"));
        lblReqId.setStyle("-fx-font-size: 11px; -fx-text-fill: #8D7B73; -fx-font-family: monospace;");

        statusRow.getChildren().addAll(lblStText, badge, spacer, lblReqId);

        // Items Table
        TableView<OrderItem> itemsTable = new TableView<>();
        itemsTable.setPrefHeight(180);
        itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<OrderItem, String> colName = new TableColumn<>("Món");
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName() + (d.getValue().getNote() != null && !d.getValue().getNote().isEmpty() ? " (" + d.getValue().getNote() + ")" : "")));

        TableColumn<OrderItem, String> colQty = new TableColumn<>("SL");
        colQty.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getQty())));
        colQty.setStyle("-fx-alignment: CENTER;");
        colQty.setPrefWidth(45);

        TableColumn<OrderItem, String> colPrice = new TableColumn<>("Đơn giá");
        colPrice.setCellValueFactory(d -> new SimpleStringProperty(CurrencyFormatter.format(d.getValue().getUnitPrice())));
        colPrice.setStyle("-fx-alignment: CENTER_RIGHT;");
        colPrice.setPrefWidth(90);

        TableColumn<OrderItem, String> colSub = new TableColumn<>("Thành tiền");
        colSub.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedSubtotal()));
        colSub.setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: 700;");
        colSub.setPrefWidth(95);

        TableColumn<OrderItem, String> colItemStatus = new TableColumn<>("Trạng thái");
        colItemStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        colItemStatus.setPrefWidth(105);
        colItemStatus.setStyle("-fx-alignment: CENTER;");
        colItemStatus.setCellFactory(col -> new TableCell<OrderItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label itemBadge = new Label(item);
                    if ("CONFIRMED".equalsIgnoreCase(item)) {
                        itemBadge.getStyleClass().add("badge-active");
                    } else if ("OUT_OF_STOCK".equalsIgnoreCase(item)) {
                        itemBadge.getStyleClass().add("badge-warning");
                    } else {
                        itemBadge.getStyleClass().add("badge-disabled");
                    }
                    setGraphic(itemBadge);
                    setText(null);
                }
            }
        });

        itemsTable.getColumns().addAll(colName, colQty, colPrice, colSub, colItemStatus);
        itemsTable.getItems().addAll(order.getItems());

        // Total
        HBox totalRow = new HBox(8);
        totalRow.setAlignment(Pos.CENTER_RIGHT);
        Label lblTotal = new Label("Tổng thanh toán: " + order.getFormattedTotal());
        lblTotal.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #C97A44;");
        totalRow.getChildren().add(lblTotal);

        // Timeline
        VBox timelineBox = new VBox(6);
        timelineBox.setStyle("-fx-background-color: #FAF4EF; -fx-background-radius: 8; -fx-padding: 10;");
        Label lblTimeHeader = new Label("⏱️ Dòng thời gian tiến trình (Timeline):");
        lblTimeHeader.setStyle("-fx-font-weight: 700; -fx-font-size: 12px; -fx-text-fill: #5D4A41;");
        timelineBox.getChildren().add(lblTimeHeader);

        for (String step : order.getTimeline()) {
            Label lblStep = new Label("• " + step);
            lblStep.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #7D6B63;");
            timelineBox.getChildren().add(lblStep);
        }

        content.getChildren().addAll(statusRow, itemsTable, totalRow, new Separator(), timelineBox);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void showChangeStatusDialog(Order order) {
        List<String> allowedStatuses = orderService.getAllowedNextStatuses(order.getStatus());
        if (allowedStatuses.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Không Thể Chuyển Trạng Thái");
            alert.setHeaderText("Đơn hàng #" + order.getOrderId() + " đã ở trạng thái kết thúc (" + order.getStatus() + ").");
            alert.setContentText("Theo quy trình State Machine, đơn hàng đã hoàn tất hoặc đã hủy không thể chuyển sang bất kỳ trạng thái nào khác.");
            alert.showAndWait();
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(allowedStatuses.get(0), allowedStatuses);
        dialog.setTitle("Cập Nhật Trạng Thái Đơn Hàng");
        dialog.setHeaderText("Đổi trạng thái cho đơn hàng #" + order.getOrderId() + " (Hiện tại: " + order.getStatus() + ")");
        dialog.setContentText("Chọn trạng thái tiếp theo hợp lệ:");

        dialog.showAndWait().ifPresent(newStatus -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Xác Nhận Đổi Trạng Thái");
            alert.setHeaderText("Bạn có chắc muốn chuyển Đơn #" + order.getOrderId() + " từ " + order.getStatus() + " sang " + newStatus + "?");
            alert.setContentText("Hành động này sẽ tuân thủ State Machine và được ghi nhận vào Audit Log hệ thống.");

            Optional<ButtonType> res = alert.showAndWait();
            if (res.isPresent() && res.get() == ButtonType.OK) {
                String err = orderService.updateOrderStatus(order.getOrderId(), newStatus, currentAdmin);
                if (err != null) {
                    Alert errAlert = new Alert(Alert.AlertType.ERROR, err, ButtonType.OK);
                    errAlert.showAndWait();
                } else {
                    refreshTable();
                }
            }
        });
    }
}
