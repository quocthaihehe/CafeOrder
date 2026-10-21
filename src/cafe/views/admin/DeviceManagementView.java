package cafe.views.admin;

import cafe.models.CafeTable;
import cafe.models.Device;
import cafe.services.DeviceService;
import cafe.services.TableService;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;
import java.util.Optional;

public class DeviceManagementView extends VBox {
    private final DeviceService deviceService;
    private final TableService tableService;
    private final String currentAdmin;

    private final TableView<Device> table = new TableView<>();
    private final TextField txtSearch = new TextField();
    private final ComboBox<String> cbStatusFilter = new ComboBox<>();
    private final Label lblCount = new Label();
    private final VBox emptyState = new VBox(10);

    public DeviceManagementView(DeviceService deviceService, TableService tableService, String currentAdmin) {
        this.deviceService = deviceService;
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
        Label lblTitle = new Label("Quản Lý Thiết Bị Kiosk Bàn (Devices)");
        lblTitle.getStyleClass().add("page-title");
        Label lblSub = new Label("Quản lý kết nối mạng, định danh thiết bị máy tính bảng tại từng bàn");
        lblSub.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(titleBox, spacer);

        // 2. Filter Bar
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        txtSearch.setPromptText("🔍 Tìm mã thiết bị, số bàn...");
        txtSearch.setPrefWidth(260);
        txtSearch.textProperty().addListener((obs, o, n) -> refreshTable());

        cbStatusFilter.getItems().addAll("Tất cả trạng thái", "ONLINE", "OFFLINE");
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

        TableColumn<Device, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        colId.setPrefWidth(50);
        colId.setStyle("-fx-alignment: CENTER;");

        TableColumn<Device, String> colDevId = new TableColumn<>("Mã thiết bị (Device ID)");
        colDevId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDeviceId()));
        colDevId.setPrefWidth(160);
        colDevId.setStyle("-fx-font-weight: 700;");

        TableColumn<Device, String> colTable = new TableColumn<>("Bàn được gán");
        colTable.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAssignedTableDisplay()));
        colTable.setPrefWidth(110);

        TableColumn<Device, String> colStatus = new TableColumn<>("Kết nối");
        colStatus.setPrefWidth(100);
        colStatus.setStyle("-fx-alignment: CENTER;");
        colStatus.setCellFactory(col -> new TableCell<Device, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Device d = getTableRow().getItem();
                    Label badge = new Label(d.getConnectionStatus());
                    badge.getStyleClass().add("ONLINE".equalsIgnoreCase(d.getConnectionStatus()) ? "badge-active" : "badge-disabled");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<Device, String> colCred = new TableColumn<>("Mật mã thiết bị");
        colCred.setPrefWidth(130);
        colCred.setStyle("-fx-alignment: CENTER;");
        colCred.setCellFactory(col -> new TableCell<Device, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Device d = getTableRow().getItem();
                    Label badge = new Label("•••••••••• (" + d.getCredentialStatus() + ")");
                    badge.getStyleClass().add("ACTIVE".equalsIgnoreCase(d.getCredentialStatus()) ? "badge-info" : "badge-warning");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<Device, String> colSeen = new TableColumn<>("Lần cuối hoạt động");
        colSeen.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getLastSeen()));
        colSeen.setPrefWidth(150);

        TableColumn<Device, Void> colActions = new TableColumn<>("Thao tác");
        colActions.setPrefWidth(220);
        colActions.setStyle("-fx-alignment: CENTER;");
        colActions.setCellFactory(col -> new TableCell<Device, Void>() {
            private final Button btnAssign = new Button("Gán bàn");
            private final Button btnUnassign = new Button("Gỡ bàn");
            private final Button btnReset = new Button("Reset Pass");
            private final HBox pane = new HBox(6, btnAssign, btnUnassign, btnReset);

            {
                pane.setAlignment(Pos.CENTER);
                btnAssign.getStyleClass().add("btn-sm-action");
                btnUnassign.getStyleClass().add("btn-sm-action");
                btnReset.getStyleClass().add("btn-sm-action");

                btnAssign.setOnAction(e -> {
                    Device d = getTableView().getItems().get(getIndex());
                    showAssignDialog(d);
                });

                btnUnassign.setOnAction(e -> {
                    Device d = getTableView().getItems().get(getIndex());
                    confirmUnassign(d);
                });

                btnReset.setOnAction(e -> {
                    Device d = getTableView().getItems().get(getIndex());
                    confirmResetCredential(d);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Device d = getTableView().getItems().get(getIndex());
                    btnUnassign.setDisable(d.getTableNumber() <= 0);
                    setGraphic(pane);
                }
            }
        });

        table.getColumns().addAll(colId, colDevId, colTable, colStatus, colCred, colSeen, colActions);

        // Empty state
        emptyState.getStyleClass().add("empty-state-box");
        Label lblEmptyTitle = new Label("Không tìm thấy thiết bị");
        lblEmptyTitle.getStyleClass().add("empty-state-title");
        Label lblEmptySub = new Label("Không có thiết bị nào phù hợp với bộ lọc tìm kiếm.");
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

        List<Device> list = deviceService.searchDevices(kw, status);
        table.getItems().setAll(list);
        lblCount.setText(list.size() + " thiết bị");

        boolean isEmpty = list.isEmpty();
        emptyState.setVisible(isEmpty);
        emptyState.setManaged(isEmpty);
        table.setVisible(!isEmpty);
        table.setManaged(!isEmpty);
    }

    private void showAssignDialog(Device d) {
        ChoiceDialog<Integer> dialog = new ChoiceDialog<>();
        dialog.setTitle("Gán Thiết Bị Vào Bàn");
        dialog.setHeaderText("Chọn bàn để liên kết thiết bị " + d.getDeviceId());
        dialog.setContentText("Chọn số bàn:");

        List<CafeTable> tables = tableService.getAllTables();
        for (CafeTable t : tables) {
            dialog.getItems().add(t.getTableNumber());
        }
        if (!dialog.getItems().isEmpty()) {
            dialog.setSelectedItem(dialog.getItems().get(0));
        }

        dialog.showAndWait().ifPresent(tblNum -> {
            String err = deviceService.assignDevice(d.getId(), tblNum, currentAdmin);
            if (err != null) {
                new Alert(Alert.AlertType.ERROR, err, ButtonType.OK).showAndWait();
            } else {
                refreshTable();
            }
        });
    }

    private void confirmUnassign(Device d) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Gỡ Thiết Bị");
        alert.setHeaderText("Gỡ thiết bị " + d.getDeviceId() + " khỏi Bàn " + d.getTableNumber() + "?");
        alert.setContentText("Thiết bị này sẽ chuyển sang trạng thái tự do (Unassigned).");

        Optional<ButtonType> res = alert.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            String err = deviceService.unassignDevice(d.getId(), currentAdmin);
            if (err != null) {
                new Alert(Alert.AlertType.ERROR, err, ButtonType.OK).showAndWait();
            } else {
                refreshTable();
            }
        }
    }

    private void confirmResetCredential(Device d) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Reset Passcode Thiết Bị");
        alert.setHeaderText("Bạn có chắc chắn muốn cấp lại mật mã bảo mật cho thiết bị " + d.getDeviceId() + "?");
        alert.setContentText("Thiết bị sẽ yêu cầu cấu hình lại mã xác thực trước khi cho phép khách đặt món.");

        Optional<ButtonType> res = alert.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            String err = deviceService.resetDeviceCredential(d.getId(), currentAdmin);
            if (err != null) {
                new Alert(Alert.AlertType.ERROR, err, ButtonType.OK).showAndWait();
            } else {
                refreshTable();
            }
        }
    }
}
