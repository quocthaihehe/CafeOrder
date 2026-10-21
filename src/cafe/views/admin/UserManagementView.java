package cafe.views.admin;

import cafe.models.Role;
import cafe.models.User;
import cafe.services.AdminUserService;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;
import java.util.Optional;

public class UserManagementView extends VBox {
    private final AdminUserService userService;
    private final String currentLoggedInAdmin;

    private final TableView<User> table = new TableView<>();
    private final TextField txtSearch = new TextField();
    private final ComboBox<String> cbRoleFilter = new ComboBox<>();
    private final ComboBox<String> cbStatusFilter = new ComboBox<>();
    private final Label lblCount = new Label();
    private final VBox emptyState = new VBox(10);

    public UserManagementView(AdminUserService userService, String currentLoggedInAdmin) {
        this.userService = userService;
        this.currentLoggedInAdmin = currentLoggedInAdmin;

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
        Label lblTitle = new Label("Quản Lý Người Dùng & Nhân Viên");
        lblTitle.getStyleClass().add("page-title");
        Label lblSub = new Label("Quản lý tài khoản đăng nhập, phân quyền và trạng thái hoạt động");
        lblSub.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAdd = new Button("➕ Thêm người dùng");
        btnAdd.getStyleClass().add("btn-primary");
        btnAdd.setOnAction(e -> showAddUserDialog());

        header.getChildren().addAll(titleBox, spacer, btnAdd);

        // 2. Filter Bar
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        txtSearch.setPromptText("🔍 Tìm theo username, họ tên...");
        txtSearch.setPrefWidth(260);
        txtSearch.textProperty().addListener((obs, o, n) -> refreshTable());

        cbRoleFilter.getItems().addAll("Tất cả vai trò", "ADMIN", "CASHIER", "BARISTA", "STAFF");
        cbRoleFilter.setValue("Tất cả vai trò");
        cbRoleFilter.setOnAction(e -> refreshTable());

        cbStatusFilter.getItems().addAll("Tất cả trạng thái", "ACTIVE", "DISABLED");
        cbStatusFilter.setValue("Tất cả trạng thái");
        cbStatusFilter.setOnAction(e -> refreshTable());

        Button btnClear = new Button("Đặt lại");
        btnClear.getStyleClass().add("btn-secondary");
        btnClear.setOnAction(e -> {
            txtSearch.clear();
            cbRoleFilter.setValue("Tất cả vai trò");
            cbStatusFilter.setValue("Tất cả trạng thái");
            refreshTable();
        });

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        lblCount.setStyle("-fx-font-weight: 700; -fx-text-fill: #7D6B63;");

        filterBar.getChildren().addAll(txtSearch, cbRoleFilter, cbStatusFilter, btnClear, spacer2, lblCount);

        // 3. Table
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<User, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        colId.setPrefWidth(50);
        colId.setStyle("-fx-alignment: CENTER;");

        TableColumn<User, String> colUsername = new TableColumn<>("Tên đăng nhập");
        colUsername.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUsername()));
        colUsername.setPrefWidth(120);

        TableColumn<User, String> colFullName = new TableColumn<>("Họ và tên");
        colFullName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullName()));
        colFullName.setPrefWidth(160);

        TableColumn<User, String> colRole = new TableColumn<>("Vai trò");
        colRole.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRole().name() + " (" + d.getValue().getRole().getDisplayName() + ")"));
        colRole.setPrefWidth(140);

        TableColumn<User, String> colStatus = new TableColumn<>("Trạng thái");
        colStatus.setPrefWidth(100);
        colStatus.setStyle("-fx-alignment: CENTER;");
        colStatus.setCellFactory(col -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    User u = getTableRow().getItem();
                    Label badge = new Label(u.getStatus());
                    badge.getStyleClass().add(u.isActive() ? "badge-active" : "badge-disabled");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<User, String> colDate = new TableColumn<>("Ngày tạo");
        colDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCreatedAt()));
        colDate.setPrefWidth(130);

        TableColumn<User, Void> colActions = new TableColumn<>("Thao tác");
        colActions.setPrefWidth(210);
        colActions.setStyle("-fx-alignment: CENTER;");
        colActions.setCellFactory(col -> new TableCell<User, Void>() {
            private final Button btnEdit = new Button("Sửa");
            private final Button btnToggle = new Button();
            private final Button btnReset = new Button("Reset MK");
            private final HBox pane = new HBox(6, btnEdit, btnToggle, btnReset);

            {
                pane.setAlignment(Pos.CENTER);
                btnEdit.getStyleClass().add("btn-sm-action");
                btnToggle.getStyleClass().add("btn-sm-action");
                btnReset.getStyleClass().add("btn-sm-action");

                btnEdit.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    showEditUserDialog(u);
                });

                btnToggle.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    confirmToggleStatus(u);
                });

                btnReset.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    showResetPasswordDialog(u);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User u = getTableView().getItems().get(getIndex());
                    btnToggle.setText(u.isActive() ? "Vô hiệu" : "Kích hoạt");
                    if (u.isActive()) {
                        btnToggle.setStyle("-fx-text-fill: #C62828;");
                    } else {
                        btnToggle.setStyle("-fx-text-fill: #2E7D32;");
                    }
                    setGraphic(pane);
                }
            }
        });

        table.getColumns().addAll(colId, colUsername, colFullName, colRole, colStatus, colDate, colActions);

        // Empty state setup
        emptyState.getStyleClass().add("empty-state-box");
        Label lblEmptyTitle = new Label("Không tìm thấy người dùng");
        lblEmptyTitle.getStyleClass().add("empty-state-title");
        Label lblEmptySub = new Label("Hãy thử thay đổi từ khóa tìm kiếm hoặc bộ lọc trạng thái.");
        lblEmptySub.getStyleClass().add("empty-state-sub");
        emptyState.getChildren().addAll(lblEmptyTitle, lblEmptySub);

        StackPane tableContainer = new StackPane(table, emptyState);
        VBox.setVgrow(tableContainer, Priority.ALWAYS);

        getChildren().addAll(header, filterBar, tableContainer);
    }

    private void refreshTable() {
        String kw = txtSearch.getText();
        String roleStr = cbRoleFilter.getValue();
        Role role = null;
        if (roleStr != null && !"Tất cả vai trò".equals(roleStr)) {
            try { role = Role.valueOf(roleStr); } catch (Exception ignored) {}
        }

        String stStr = cbStatusFilter.getValue();
        String status = (stStr == null || "Tất cả trạng thái".equals(stStr)) ? "ALL" : stStr;

        List<User> list = userService.searchUsers(kw, role, status);
        table.getItems().setAll(list);
        lblCount.setText(list.size() + " tài khoản");

        boolean isEmpty = list.isEmpty();
        emptyState.setVisible(isEmpty);
        emptyState.setManaged(isEmpty);
        table.setVisible(!isEmpty);
        table.setManaged(!isEmpty);
    }

    private void showAddUserDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Thêm Người Dùng Mới");
        dialog.setHeaderText("Tạo tài khoản người dùng và phân quyền hệ thống");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField txtU = new TextField();
        txtU.setPromptText("Ví dụ: cashier03");
        TextField txtF = new TextField();
        txtF.setPromptText("Ví dụ: Lê Thị Hoa");
        PasswordField txtP = new PasswordField();
        PasswordField txtCP = new PasswordField();

        ComboBox<Role> cbR = new ComboBox<>();
        cbR.getItems().addAll(Role.ADMIN, Role.CASHIER, Role.BARISTA, Role.STAFF);
        cbR.setValue(Role.STAFF);

        ComboBox<String> cbS = new ComboBox<>();
        cbS.getItems().addAll("ACTIVE", "DISABLED");
        cbS.setValue("ACTIVE");

        Label lblErr = new Label();
        lblErr.setStyle("-fx-text-fill: #C62828; -fx-font-weight: 700;");

        grid.add(new Label("Tên đăng nhập:"), 0, 0);
        grid.add(txtU, 1, 0);
        grid.add(new Label("Họ và tên:"), 0, 1);
        grid.add(txtF, 1, 1);
        grid.add(new Label("Mật khẩu:"), 0, 2);
        grid.add(txtP, 1, 2);
        grid.add(new Label("Xác nhận mật khẩu:"), 0, 3);
        grid.add(txtCP, 1, 3);
        grid.add(new Label("Vai trò:"), 0, 4);
        grid.add(cbR, 1, 4);
        grid.add(new Label("Trạng thái:"), 0, 5);
        grid.add(cbS, 1, 5);
        grid.add(lblErr, 0, 6, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button btnOk = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            String err = userService.createUser(
                    txtU.getText(),
                    txtF.getText(),
                    txtP.getText(),
                    txtCP.getText(),
                    cbR.getValue(),
                    cbS.getValue(),
                    currentLoggedInAdmin
            );
            if (err != null) {
                lblErr.setText("⚠️ " + err);
                e.consume();
            }
        });

        dialog.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) refreshTable();
        });
    }

    private void showEditUserDialog(User user) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Chỉnh Sửa Người Dùng");
        dialog.setHeaderText("Cập nhật thông tin cho tài khoản: " + user.getUsername());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField txtU = new TextField(user.getUsername());
        txtU.setDisable(true); // username immutable
        TextField txtF = new TextField(user.getFullName());

        ComboBox<Role> cbR = new ComboBox<>();
        cbR.getItems().addAll(Role.ADMIN, Role.CASHIER, Role.BARISTA, Role.STAFF);
        cbR.setValue(user.getRole());

        ComboBox<String> cbS = new ComboBox<>();
        cbS.getItems().addAll("ACTIVE", "DISABLED");
        cbS.setValue(user.getStatus());

        Label lblErr = new Label();
        lblErr.setStyle("-fx-text-fill: #C62828; -fx-font-weight: 700;");

        grid.add(new Label("Tên đăng nhập:"), 0, 0);
        grid.add(txtU, 1, 0);
        grid.add(new Label("Họ và tên:"), 0, 1);
        grid.add(txtF, 1, 1);
        grid.add(new Label("Vai trò:"), 0, 2);
        grid.add(cbR, 1, 2);
        grid.add(new Label("Trạng thái:"), 0, 3);
        grid.add(cbS, 1, 3);
        grid.add(lblErr, 0, 4, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button btnOk = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            String err = userService.updateUser(
                    user.getId(),
                    txtF.getText(),
                    cbR.getValue(),
                    cbS.getValue(),
                    currentLoggedInAdmin,
                    currentLoggedInAdmin
            );
            if (err != null) {
                lblErr.setText("⚠️ " + err);
                e.consume();
            }
        });

        dialog.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) refreshTable();
        });
    }

    private void confirmToggleStatus(User user) {
        String actionName = user.isActive() ? "vô hiệu hóa" : "kích hoạt";
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác Nhận Thay Đổi Trạng Thái");
        alert.setHeaderText("Bạn có chắc chắn muốn " + actionName + " tài khoản '" + user.getUsername() + "'?");
        alert.setContentText("Hành động này sẽ cập nhật ngay quyền truy cập của nhân viên.");

        Optional<ButtonType> res = alert.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            String err = userService.toggleUserStatus(user.getId(), currentLoggedInAdmin, currentLoggedInAdmin);
            if (err != null) {
                Alert errAlert = new Alert(Alert.AlertType.ERROR, err, ButtonType.OK);
                errAlert.showAndWait();
            } else {
                refreshTable();
            }
        }
    }

    private void showResetPasswordDialog(User user) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Đặt Lại Mật Khẩu");
        dialog.setHeaderText("Đặt lại mật khẩu bảo mật cho tài khoản: " + user.getUsername());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        PasswordField txtNewPass = new PasswordField();
        txtNewPass.setPromptText("Mật khẩu mới (>= 6 ký tự)");
        PasswordField txtConfirmPass = new PasswordField();
        txtConfirmPass.setPromptText("Nhập lại mật khẩu mới");

        Label lblErr = new Label();
        lblErr.setStyle("-fx-text-fill: #C62828; -fx-font-weight: 700;");

        grid.add(new Label("Mật khẩu mới:"), 0, 0);
        grid.add(txtNewPass, 1, 0);
        grid.add(new Label("Xác nhận mật khẩu:"), 0, 1);
        grid.add(txtConfirmPass, 1, 1);
        grid.add(lblErr, 0, 2, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button btnOk = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            String p1 = txtNewPass.getText();
            String p2 = txtConfirmPass.getText();
            if (p1 == null || p1.trim().length() < 6) {
                lblErr.setText("⚠️ Mật khẩu mới phải có tối thiểu 6 ký tự!");
                e.consume();
                return;
            }
            if (!p1.equals(p2)) {
                lblErr.setText("⚠️ Mật khẩu xác nhận không khớp!");
                e.consume();
                return;
            }
            String err = userService.resetPassword(user.getId(), p1, currentLoggedInAdmin);
            if (err != null) {
                lblErr.setText("⚠️ " + err);
                e.consume();
            }
        });

        dialog.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                Alert infoAlert = new Alert(Alert.AlertType.INFORMATION, "Đã đặt lại mật khẩu thành công cho '" + user.getUsername() + "'!", ButtonType.OK);
                infoAlert.showAndWait();
            }
        });
    }
}
