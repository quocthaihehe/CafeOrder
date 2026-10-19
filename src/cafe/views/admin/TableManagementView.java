package cafe.views.admin;

import cafe.models.CafeTable;
import cafe.services.TableService;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;

public class TableManagementView extends VBox {
    private final TableService tableService;
    private final String currentAdmin;

    private final TableView<CafeTable> table = new TableView<>();
    private final TextField txtSearch = new TextField();
    private final ComboBox<String> cbStatusFilter = new ComboBox<>();
    private final Label lblCount = new Label();
    private final VBox emptyState = new VBox(10);

    public TableManagementView(TableService tableService, String currentAdmin) {
        this.tableService = tableService;
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
        Label lblTitle = new Label("Quản Lý Sơ Đồ Bàn (Tables)");
        lblTitle.getStyleClass().add("page-title");
        Label lblSub = new Label("Quản lý vị trí bàn ngồi, thiết bị gắn kèm và trạng thái phục vụ");
        lblSub.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAdd = new Button("➕ Thêm bàn");
        btnAdd.getStyleClass().add("btn-primary");
        btnAdd.setOnAction(e -> showAddTableDialog());

        header.getChildren().addAll(titleBox, spacer, btnAdd);

        // 2. Filter Bar
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        txtSearch.setPromptText("🔍 Tìm số bàn, mã thiết bị...");
        txtSearch.setPrefWidth(260);
        txtSearch.textProperty().addListener((obs, o, n) -> refreshTable());

        cbStatusFilter.getItems().addAll("Tất cả trạng thái", "AVAILABLE", "OCCUPIED", "OFFLINE");
        cbStatusFilter.setValue("Tất cả trạng thái");
        cbStatusFilter.setOnAction(e -> refreshTable());

        Button btnClear = new Button("Đặt lại");
        btnClear.getStyleClass().add("btn-secondary");
        btnClear.setOnAction(e -> {
            txtSearch.clear();
            cbStatusFilter.setValue("Tất cả trạng thái");
            refreshTable();
        });

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        lblCount.setStyle("-fx-font-weight: 700; -fx-text-fill: #7D6B63;");

        filterBar.getChildren().addAll(txtSearch, cbStatusFilter, btnClear, spacer2, lblCount);

        // 3. Table
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<CafeTable, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        colId.setPrefWidth(50);
        colId.setStyle("-fx-alignment: CENTER;");

        TableColumn<CafeTable, String> colNum = new TableColumn<>("Số bàn");
        colNum.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDisplayName()));
        colNum.setPrefWidth(120);
        colNum.setStyle("-fx-font-weight: 700;");

        TableColumn<CafeTable, String> colDevice = new TableColumn<>("Mã thiết bị (Device ID)");
        colDevice.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDeviceId().isEmpty() ? "(Chưa gán)" : d.getValue().getDeviceId()));
        colDevice.setPrefWidth(180);

        TableColumn<CafeTable, String> colStatus = new TableColumn<>("Trạng thái");
        colStatus.setPrefWidth(130);
        colStatus.setStyle("-fx-alignment: CENTER;");
        colStatus.setCellFactory(col -> new TableCell<CafeTable, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    CafeTable t = getTableRow().getItem();
                    Label badge = new Label(t.getStatus());
                    if ("AVAILABLE".equalsIgnoreCase(t.getStatus())) {
                        badge.getStyleClass().add("badge-active");
                        badge.setText("Trống (Available)");
                    } else if ("OCCUPIED".equalsIgnoreCase(t.getStatus())) {
                        badge.getStyleClass().add("badge-warning");
                        badge.setText("Có khách (Occupied)");
                    } else {
                        badge.getStyleClass().add("badge-disabled");
                        badge.setText("Ngoại tuyến (Offline)");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<CafeTable, String> colDate = new TableColumn<>("Ngày tạo");
        colDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCreatedAt()));
        colDate.setPrefWidth(140);

        TableColumn<CafeTable, Void> colActions = new TableColumn<>("Thao tác");
        colActions.setPrefWidth(120);
        colActions.setStyle("-fx-alignment: CENTER;");
        colActions.setCellFactory(col -> new TableCell<CafeTable, Void>() {
            private final Button btnEdit = new Button("Chỉnh sửa");

            {
                btnEdit.getStyleClass().add("btn-sm-action");
                btnEdit.setOnAction(e -> {
                    CafeTable t = getTableView().getItems().get(getIndex());
                    showEditTableDialog(t);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(btnEdit);
            }
        });

        table.getColumns().addAll(colId, colNum, colDevice, colStatus, colDate, colActions);

        // Empty state
        emptyState.getStyleClass().add("empty-state-box");
        Label lblEmptyTitle = new Label("Không tìm thấy bàn");
        lblEmptyTitle.getStyleClass().add("empty-state-title");
        Label lblEmptySub = new Label("Không có bàn nào phù hợp với bộ lọc tìm kiếm.");
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

        List<CafeTable> list = tableService.searchTables(kw, status);
        table.getItems().setAll(list);
        lblCount.setText(list.size() + " bàn");

        boolean isEmpty = list.isEmpty();
        emptyState.setVisible(isEmpty);
        emptyState.setManaged(isEmpty);
        table.setVisible(!isEmpty);
        table.setManaged(!isEmpty);
    }

    private void showAddTableDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Thêm Bàn Mới");
        dialog.setHeaderText("Tạo bàn ăn / bàn cà phê vào sơ đồ quán");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField txtNum = new TextField();
        txtNum.setPromptText("Ví dụ: 15");

        TextField txtDevice = new TextField();
        txtDevice.setPromptText("Ví dụ: DEV-TB15-X99");

        ComboBox<String> cbS = new ComboBox<>();
        cbS.getItems().addAll("AVAILABLE", "OCCUPIED", "OFFLINE");
        cbS.setValue("AVAILABLE");

        Label lblErr = new Label();
        lblErr.setStyle("-fx-text-fill: #C62828; -fx-font-weight: 700;");

        grid.add(new Label("Số bàn (Số nguyên):"), 0, 0);
        grid.add(txtNum, 1, 0);
        grid.add(new Label("Mã thiết bị (Device ID):"), 0, 1);
        grid.add(txtDevice, 1, 1);
        grid.add(new Label("Trạng thái:"), 0, 2);
        grid.add(cbS, 1, 2);
        grid.add(lblErr, 0, 3, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button btnOk = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            int num;
            try {
                num = Integer.parseInt(txtNum.getText().trim());
            } catch (Exception ex) {
                lblErr.setText("⚠️ Số bàn phải là số nguyên dương hợp lệ!");
                e.consume();
                return;
            }

            String err = tableService.createTable(num, txtDevice.getText(), cbS.getValue(), currentAdmin);
            if (err != null) {
                lblErr.setText("⚠️ " + err);
                e.consume();
            }
        });

        dialog.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) refreshTable();
        });
    }

    private void showEditTableDialog(CafeTable t) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Chỉnh Sửa Bàn");
        dialog.setHeaderText("Cập nhật thông tin Bàn #" + t.getTableNumber());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField txtNum = new TextField(String.valueOf(t.getTableNumber()));
        TextField txtDevice = new TextField(t.getDeviceId());

        ComboBox<String> cbS = new ComboBox<>();
        cbS.getItems().addAll("AVAILABLE", "OCCUPIED", "OFFLINE");
        cbS.setValue(t.getStatus());

        Label lblErr = new Label();
        lblErr.setStyle("-fx-text-fill: #C62828; -fx-font-weight: 700;");

        grid.add(new Label("Số bàn:"), 0, 0);
        grid.add(txtNum, 1, 0);
        grid.add(new Label("Mã thiết bị (Device ID):"), 0, 1);
        grid.add(txtDevice, 1, 1);
        grid.add(new Label("Trạng thái:"), 0, 2);
        grid.add(cbS, 1, 2);
        grid.add(lblErr, 0, 3, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button btnOk = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            int num;
            try {
                num = Integer.parseInt(txtNum.getText().trim());
            } catch (Exception ex) {
                lblErr.setText("⚠️ Số bàn phải là số nguyên dương hợp lệ!");
                e.consume();
                return;
            }

            String err = tableService.updateTable(t.getId(), num, txtDevice.getText(), cbS.getValue(), currentAdmin);
            if (err != null) {
                lblErr.setText("⚠️ " + err);
                e.consume();
            }
        });

        dialog.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) refreshTable();
        });
    }
}
