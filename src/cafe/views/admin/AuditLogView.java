package cafe.views.admin;

import cafe.models.AuditLog;
import cafe.services.AuditLogService;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;

public class AuditLogView extends VBox {
    private final AuditLogService auditLogService;

    private final TableView<AuditLog> table = new TableView<>();
    private final TextField txtSearch = new TextField();
    private final ComboBox<String> cbActionFilter = new ComboBox<>();
    private final ComboBox<String> cbResultFilter = new ComboBox<>();
    private final Label lblCount = new Label();
    private final VBox emptyState = new VBox(10);

    public AuditLogView(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;

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
        Label lblTitle = new Label("Nhật Ký Kiểm Toán Hệ Thống (Audit Logs)");
        lblTitle.getStyleClass().add("page-title");
        Label lblSub = new Label("Ghi lại toàn bộ thao tác bảo mật, cập nhật giá, đơn hàng và phân quyền (Chỉ đọc)");
        lblSub.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(titleBox, spacer);

        // 2. Filter Bar
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        txtSearch.setPromptText("🔍 Tìm theo người dùng, mục tiêu, hành động...");
        txtSearch.setPrefWidth(280);
        txtSearch.textProperty().addListener((obs, o, n) -> refreshTable());

        cbActionFilter.getItems().addAll("Tất cả hành động", "LOGIN", "CREATE_USER", "UPDATE_USER", "DISABLE_USER", "RESET_PASSWORD", "CREATE_MENU", "UPDATE_MENU", "DISABLE_MENU", "CREATE_TABLE", "UPDATE_TABLE", "RESET_DEVICE_SECRET", "VIEW_ORDER", "PAYMENT");
        cbActionFilter.setValue("Tất cả hành động");
        cbActionFilter.setOnAction(e -> refreshTable());

        cbResultFilter.getItems().addAll("Tất cả kết quả", "SUCCESS", "FAILED");
        cbResultFilter.setValue("Tất cả kết quả");
        cbResultFilter.setOnAction(e -> refreshTable());

        Button btnClear = new Button("Đặt lại");
        btnClear.getStyleClass().add("btn-secondary");
        btnClear.setOnAction(e -> {
            txtSearch.clear();
            cbActionFilter.setValue("Tất cả hành động");
            cbResultFilter.setValue("Tất cả kết quả");
            refreshTable();
        });

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        lblCount.setStyle("-fx-font-weight: 700; -fx-text-fill: #7D6B63;");

        filterBar.getChildren().addAll(txtSearch, cbActionFilter, cbResultFilter, btnClear, spacer2, lblCount);

        // 3. Table (Read-only, no editing or deleting)
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<AuditLog, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        colId.setPrefWidth(55);
        colId.setStyle("-fx-alignment: CENTER;");

        TableColumn<AuditLog, String> colTime = new TableColumn<>("Thời gian ghi nhận");
        colTime.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTimestamp()));
        colTime.setPrefWidth(150);

        TableColumn<AuditLog, String> colUser = new TableColumn<>("Tài khoản tác nhân");
        colUser.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUser()));
        colUser.setPrefWidth(140);
        colUser.setStyle("-fx-font-weight: 700;");

        TableColumn<AuditLog, String> colAction = new TableColumn<>("Hành động thực hiện");
        colAction.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAction()));
        colAction.setPrefWidth(180);

        TableColumn<AuditLog, String> colTarget = new TableColumn<>("Đối tượng bị tác động");
        colTarget.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTarget()));
        colTarget.setPrefWidth(240);

        TableColumn<AuditLog, String> colResult = new TableColumn<>("Kết quả");
        colResult.setPrefWidth(100);
        colResult.setStyle("-fx-alignment: CENTER;");
        colResult.setCellFactory(col -> new TableCell<AuditLog, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    AuditLog log = getTableRow().getItem();
                    Label badge = new Label(log.getResult());
                    badge.getStyleClass().add("SUCCESS".equalsIgnoreCase(log.getResult()) ? "badge-active" : "badge-disabled");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        table.getColumns().addAll(colId, colTime, colUser, colAction, colTarget, colResult);

        // Empty state
        emptyState.getStyleClass().add("empty-state-box");
        Label lblEmptyTitle = new Label("Không tìm thấy nhật ký kiểm toán");
        lblEmptyTitle.getStyleClass().add("empty-state-title");
        Label lblEmptySub = new Label("Không có bản ghi nhật ký nào khớp với bộ lọc.");
        lblEmptySub.getStyleClass().add("empty-state-sub");
        emptyState.getChildren().addAll(lblEmptyTitle, lblEmptySub);

        StackPane tableContainer = new StackPane(table, emptyState);
        VBox.setVgrow(tableContainer, Priority.ALWAYS);

        getChildren().addAll(header, filterBar, tableContainer);
    }

    private void refreshTable() {
        String kw = txtSearch.getText();
        String aStr = cbActionFilter.getValue();
        String action = (aStr == null || "Tất cả hành động".equals(aStr)) ? "ALL" : aStr;

        String rStr = cbResultFilter.getValue();
        String result = (rStr == null || "Tất cả kết quả".equals(rStr)) ? "ALL" : rStr;

        List<AuditLog> list = auditLogService.searchLogs(kw, "ALL", action, result);
        table.getItems().setAll(list);
        lblCount.setText(list.size() + " bản ghi nhật ký");

        boolean isEmpty = list.isEmpty();
        emptyState.setVisible(isEmpty);
        emptyState.setManaged(isEmpty);
        table.setVisible(!isEmpty);
        table.setManaged(!isEmpty);
    }
}
