package cafe.views.admin;

import cafe.models.Payment;
import cafe.services.PaymentService;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;

public class PaymentManagementView extends VBox {
    private final PaymentService paymentService;
    private final String currentAdmin;

    private final TableView<Payment> table = new TableView<>();
    private final TextField txtSearch = new TextField();
    private final ComboBox<String> cbMethodFilter = new ComboBox<>();
    private final ComboBox<String> cbStatusFilter = new ComboBox<>();
    private final Label lblCount = new Label();
    private final VBox emptyState = new VBox(10);

    public PaymentManagementView(PaymentService paymentService) {
        this(paymentService, "admin01");
    }

    public PaymentManagementView(PaymentService paymentService, String currentAdmin) {
        this.paymentService = paymentService;
        this.currentAdmin = currentAdmin != null ? currentAdmin : "admin01";

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
        Label lblTitle = new Label("Quản Lý Giao Dịch & Thanh Toán (Payments)");
        lblTitle.getStyleClass().add("page-title");
        Label lblSub = new Label("Theo dõi các khoản thu ngân, giao dịch tiền mặt, quẹt thẻ và chuyển khoản");
        lblSub.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(titleBox, spacer);

        // 2. Filter Bar
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        txtSearch.setPromptText("🔍 Tìm mã thanh toán, mã đơn, mã GD...");
        txtSearch.setPrefWidth(280);
        txtSearch.textProperty().addListener((obs, o, n) -> refreshTable());

        cbMethodFilter.getItems().addAll("Tất cả hình thức", "CASH", "CARD", "BANK_TRANSFER");
        cbMethodFilter.setValue("Tất cả hình thức");
        cbMethodFilter.setOnAction(e -> refreshTable());

        cbStatusFilter.getItems().addAll("Tất cả trạng thái", "SUCCESS", "PENDING", "FAILED");
        cbStatusFilter.setValue("Tất cả trạng thái");
        cbStatusFilter.setOnAction(e -> refreshTable());

        Button btnClear = new Button("Đặt lại");
        btnClear.getStyleClass().add("btn-secondary");
        btnClear.setOnAction(e -> {
            txtSearch.clear();
            cbMethodFilter.setValue("Tất cả hình thức");
            cbStatusFilter.setValue("Tất cả trạng thái");
            refreshTable();
        });

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        lblCount.setStyle("-fx-font-weight: 700; -fx-text-fill: #7D6B63;");

        filterBar.getChildren().addAll(txtSearch, cbMethodFilter, cbStatusFilter, btnClear, spacer2, lblCount);

        // 3. Table
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Payment, String> colId = new TableColumn<>("Mã TT");
        colId.setCellValueFactory(d -> new SimpleStringProperty("#" + d.getValue().getId()));
        colId.setPrefWidth(65);
        colId.setStyle("-fx-alignment: CENTER; -fx-font-weight: 700;");

        TableColumn<Payment, String> colOrder = new TableColumn<>("Đơn hàng");
        colOrder.setCellValueFactory(d -> new SimpleStringProperty("#" + d.getValue().getOrderId()));
        colOrder.setPrefWidth(80);
        colOrder.setStyle("-fx-alignment: CENTER;");

        TableColumn<Payment, String> colAmount = new TableColumn<>("Số tiền");
        colAmount.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedAmount()));
        colAmount.setPrefWidth(110);
        colAmount.setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: 800;");

        TableColumn<Payment, String> colMethod = new TableColumn<>("Phương thức");
        colMethod.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMethod()));
        colMethod.setPrefWidth(110);
        colMethod.setStyle("-fx-alignment: CENTER;");

        TableColumn<Payment, String> colStatus = new TableColumn<>("Trạng thái");
        colStatus.setPrefWidth(110);
        colStatus.setStyle("-fx-alignment: CENTER;");
        colStatus.setCellFactory(col -> new TableCell<Payment, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Payment p = getTableRow().getItem();
                    Label badge = new Label(p.getStatus());
                    if ("SUCCESS".equalsIgnoreCase(p.getStatus())) {
                        badge.getStyleClass().add("badge-active");
                    } else if ("FAILED".equalsIgnoreCase(p.getStatus())) {
                        badge.getStyleClass().add("badge-disabled");
                    } else {
                        badge.getStyleClass().add("badge-warning");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<Payment, String> colTxn = new TableColumn<>("Mã giao dịch (Txn Code)");
        colTxn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTransactionCode()));
        colTxn.setPrefWidth(140);

        TableColumn<Payment, String> colBy = new TableColumn<>("Thu ngân");
        colBy.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProcessedBy()));
        colBy.setPrefWidth(100);

        TableColumn<Payment, String> colPaidAt = new TableColumn<>("Thời gian thanh toán");
        colPaidAt.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPaidAt() != null ? d.getValue().getPaidAt() : d.getValue().getCreatedAt()));
        colPaidAt.setPrefWidth(140);

        TableColumn<Payment, Void> colActions = new TableColumn<>("Xem");
        colActions.setPrefWidth(80);
        colActions.setStyle("-fx-alignment: CENTER;");
        colActions.setCellFactory(col -> new TableCell<Payment, Void>() {
            private final Button btnView = new Button("Chi tiết");
            {
                btnView.getStyleClass().add("btn-sm-action");
                btnView.setOnAction(e -> {
                    Payment p = getTableView().getItems().get(getIndex());
                    showPaymentDetailDialog(p);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(btnView);
            }
        });

        table.getColumns().addAll(colId, colOrder, colAmount, colMethod, colStatus, colTxn, colBy, colPaidAt, colActions);

        // Empty state
        emptyState.getStyleClass().add("empty-state-box");
        Label lblEmptyTitle = new Label("Không tìm thấy giao dịch thanh toán");
        lblEmptyTitle.getStyleClass().add("empty-state-title");
        Label lblEmptySub = new Label("Không có bản ghi thanh toán nào khớp với bộ lọc.");
        lblEmptySub.getStyleClass().add("empty-state-sub");
        emptyState.getChildren().addAll(lblEmptyTitle, lblEmptySub);

        StackPane tableContainer = new StackPane(table, emptyState);
        VBox.setVgrow(tableContainer, Priority.ALWAYS);

        getChildren().addAll(header, filterBar, tableContainer);
    }

    private void refreshTable() {
        String kw = txtSearch.getText();
        String mStr = cbMethodFilter.getValue();
        String method = (mStr == null || "Tất cả hình thức".equals(mStr)) ? "ALL" : mStr;

        String sStr = cbStatusFilter.getValue();
        String status = (sStr == null || "Tất cả trạng thái".equals(sStr)) ? "ALL" : sStr;

        List<Payment> list = paymentService.searchPayments(kw, method, status);
        table.getItems().setAll(list);
        lblCount.setText(list.size() + " giao dịch");

        boolean isEmpty = list.isEmpty();
        emptyState.setVisible(isEmpty);
        emptyState.setManaged(isEmpty);
        table.setVisible(!isEmpty);
        table.setManaged(!isEmpty);
    }

    private void showPaymentDetailDialog(Payment p) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Chi Tiết Giao Dịch #" + p.getId());
        dialog.setHeaderText("Giao dịch cho Đơn hàng #" + p.getOrderId());

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        grid.add(new Label("Mã thanh toán:"), 0, 0);
        grid.add(new Label("#" + p.getId()), 1, 0);
        grid.add(new Label("Mã đơn hàng:"), 0, 1);
        grid.add(new Label("#" + p.getOrderId()), 1, 1);
        grid.add(new Label("Số tiền:"), 0, 2);
        Label lblAmt = new Label(p.getFormattedAmount());
        lblAmt.setStyle("-fx-font-weight: 800; -fx-text-fill: #2E7D32;");
        grid.add(lblAmt, 1, 2);
        grid.add(new Label("Phương thức:"), 0, 3);
        grid.add(new Label(p.getMethod()), 1, 3);
        grid.add(new Label("Trạng thái:"), 0, 4);
        grid.add(new Label(p.getStatus()), 1, 4);
        grid.add(new Label("Mã tham chiếu GD:"), 0, 5);
        grid.add(new Label(p.getTransactionCode()), 1, 5);
        grid.add(new Label("Nhân viên xử lý:"), 0, 6);
        grid.add(new Label(p.getProcessedBy()), 1, 6);
        grid.add(new Label("Thời gian ghi nhận:"), 0, 7);
        grid.add(new Label(p.getCreatedAt()), 1, 7);
        grid.add(new Label("Thời gian thanh toán:"), 0, 8);
        grid.add(new Label(p.getPaidAt() != null ? p.getPaidAt() : "(Chưa hoàn tất)"), 1, 8);

        if ("PENDING".equalsIgnoreCase(p.getStatus())) {
            Button btnConfirm = new Button("✅ Xác nhận Thu tiền (SUCCESS)");
            btnConfirm.getStyleClass().add("btn-primary");
            Button btnFail = new Button("❌ Đánh dấu Lỗi (FAILED)");
            btnFail.getStyleClass().add("btn-secondary");

            btnConfirm.setOnAction(e -> {
                String err = paymentService.confirmPendingPayment(p.getId(), currentAdmin);
                if (err != null) {
                    new Alert(Alert.AlertType.ERROR, err, ButtonType.OK).showAndWait();
                } else {
                    dialog.close();
                    refreshTable();
                }
            });

            btnFail.setOnAction(e -> {
                String err = paymentService.markPaymentFailed(p.getId(), currentAdmin);
                if (err != null) {
                    new Alert(Alert.AlertType.ERROR, err, ButtonType.OK).showAndWait();
                } else {
                    dialog.close();
                    refreshTable();
                }
            });

            HBox actBox = new HBox(10, btnConfirm, btnFail);
            actBox.setPadding(new Insets(10, 0, 0, 0));
            grid.add(actBox, 0, 9, 2, 1);
        }

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }
}
