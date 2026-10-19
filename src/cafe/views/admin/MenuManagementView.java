package cafe.views.admin;

import cafe.models.MenuItem;
import cafe.services.CategoryService;
import cafe.services.MenuService;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;
import java.util.Optional;

public class MenuManagementView extends VBox {
    private final MenuService menuService;
    private final CategoryService categoryService;
    private final String currentAdmin;

    private final TableView<MenuItem> table = new TableView<>();
    private final TextField txtSearch = new TextField();
    private final ComboBox<String> cbCategoryFilter = new ComboBox<>();
    private final ComboBox<String> cbStatusFilter = new ComboBox<>();
    private final Label lblCount = new Label();
    private final VBox emptyState = new VBox(10);

    public MenuManagementView(MenuService menuService, CategoryService categoryService, String currentAdmin) {
        this.menuService = menuService;
        this.categoryService = categoryService;
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
        Label lblTitle = new Label("Quản Lý Thực Đơn (Menu)");
        lblTitle.getStyleClass().add("page-title");
        Label lblSub = new Label("Quản lý danh sách đồ uống, bánh ngọt, giá bán và công thức mô tả");
        lblSub.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAdd = new Button("➕ Thêm món mới");
        btnAdd.getStyleClass().add("btn-primary");
        btnAdd.setOnAction(e -> showAddMenuItemDialog());

        header.getChildren().addAll(titleBox, spacer, btnAdd);

        // 2. Filter Bar
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        txtSearch.setPromptText("🔍 Tìm tên món, mô tả...");
        txtSearch.setPrefWidth(260);
        txtSearch.textProperty().addListener((obs, o, n) -> refreshTable());

        cbCategoryFilter.getItems().add("Tất cả");
        cbCategoryFilter.getItems().addAll(categoryService.getAllCategories().stream().map(c -> c.getName()).toList());
        cbCategoryFilter.setValue("Tất cả");
        cbCategoryFilter.setOnAction(e -> refreshTable());

        cbStatusFilter.getItems().addAll("Tất cả trạng thái", "ACTIVE", "INACTIVE");
        cbStatusFilter.setValue("Tất cả trạng thái");
        cbStatusFilter.setOnAction(e -> refreshTable());

        Button btnClear = new Button("Đặt lại");
        btnClear.getStyleClass().add("btn-secondary");
        btnClear.setOnAction(e -> {
            txtSearch.clear();
            cbCategoryFilter.setValue("Tất cả");
            cbStatusFilter.setValue("Tất cả trạng thái");
            refreshTable();
        });

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        lblCount.setStyle("-fx-font-weight: 700; -fx-text-fill: #7D6B63;");

        filterBar.getChildren().addAll(txtSearch, cbCategoryFilter, cbStatusFilter, btnClear, spacer2, lblCount);

        // 3. Table
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<MenuItem, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        colId.setPrefWidth(50);
        colId.setStyle("-fx-alignment: CENTER;");

        TableColumn<MenuItem, String> colIcon = new TableColumn<>("Icon");
        colIcon.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getIconEmoji()));
        colIcon.setPrefWidth(50);
        colIcon.setStyle("-fx-alignment: CENTER;");

        TableColumn<MenuItem, String> colName = new TableColumn<>("Tên món");
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        colName.setPrefWidth(160);

        TableColumn<MenuItem, String> colCategory = new TableColumn<>("Danh mục");
        colCategory.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory()));
        colCategory.setPrefWidth(120);

        TableColumn<MenuItem, String> colPrice = new TableColumn<>("Giá bán");
        colPrice.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormattedPrice()));
        colPrice.setPrefWidth(100);
        colPrice.setStyle("-fx-alignment: CENTER_RIGHT; -fx-font-weight: 700;");

        TableColumn<MenuItem, String> colStatus = new TableColumn<>("Trạng thái");
        colStatus.setPrefWidth(100);
        colStatus.setStyle("-fx-alignment: CENTER;");
        colStatus.setCellFactory(col -> new TableCell<MenuItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    MenuItem mi = getTableRow().getItem();
                    Label badge = new Label(mi.getStatus());
                    badge.getStyleClass().add(mi.isActive() ? "badge-active" : "badge-disabled");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<MenuItem, String> colDesc = new TableColumn<>("Mô tả hương vị");
        colDesc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription()));
        colDesc.setPrefWidth(210);

        TableColumn<MenuItem, String> colUpdated = new TableColumn<>("Cập nhật");
        colUpdated.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUpdatedAt()));
        colUpdated.setPrefWidth(120);

        TableColumn<MenuItem, Void> colActions = new TableColumn<>("Thao tác");
        colActions.setPrefWidth(160);
        colActions.setStyle("-fx-alignment: CENTER;");
        colActions.setCellFactory(col -> new TableCell<MenuItem, Void>() {
            private final Button btnEdit = new Button("Sửa");
            private final Button btnToggle = new Button();
            private final HBox pane = new HBox(6, btnEdit, btnToggle);

            {
                pane.setAlignment(Pos.CENTER);
                btnEdit.getStyleClass().add("btn-sm-action");
                btnToggle.getStyleClass().add("btn-sm-action");

                btnEdit.setOnAction(e -> {
                    MenuItem mi = getTableView().getItems().get(getIndex());
                    showEditMenuItemDialog(mi);
                });

                btnToggle.setOnAction(e -> {
                    MenuItem mi = getTableView().getItems().get(getIndex());
                    confirmToggleStatus(mi);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    MenuItem mi = getTableView().getItems().get(getIndex());
                    btnToggle.setText(mi.isActive() ? "Ngừng bán" : "Mở bán");
                    btnToggle.setStyle(mi.isActive() ? "-fx-text-fill: #C62828;" : "-fx-text-fill: #2E7D32;");
                    setGraphic(pane);
                }
            }
        });

        table.getColumns().addAll(colId, colIcon, colName, colCategory, colPrice, colStatus, colDesc, colUpdated, colActions);

        // Empty state
        emptyState.getStyleClass().add("empty-state-box");
        Label lblEmptyTitle = new Label("Không tìm thấy món ăn");
        lblEmptyTitle.getStyleClass().add("empty-state-title");
        Label lblEmptySub = new Label("Không có món nào thỏa mãn điều kiện lọc hoặc từ khóa.");
        lblEmptySub.getStyleClass().add("empty-state-sub");
        emptyState.getChildren().addAll(lblEmptyTitle, lblEmptySub);

        StackPane tableContainer = new StackPane(table, emptyState);
        VBox.setVgrow(tableContainer, Priority.ALWAYS);

        getChildren().addAll(header, filterBar, tableContainer);
    }

    private void refreshTable() {
        String kw = txtSearch.getText();
        String cat = cbCategoryFilter.getValue();
        String stStr = cbStatusFilter.getValue();
        String status = (stStr == null || "Tất cả trạng thái".equals(stStr)) ? "ALL" : stStr;

        List<MenuItem> list = menuService.searchMenuItems(kw, cat, status);
        table.getItems().setAll(list);
        lblCount.setText(list.size() + " món");

        boolean isEmpty = list.isEmpty();
        emptyState.setVisible(isEmpty);
        emptyState.setManaged(isEmpty);
        table.setVisible(!isEmpty);
        table.setManaged(!isEmpty);
    }

    private void showAddMenuItemDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Thêm Món Mới");
        dialog.setHeaderText("Tạo món ăn hoặc thức uống mới vào thực đơn");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField txtName = new TextField();
        txtName.setPromptText("Ví dụ: Trà sữa Ô Long Nướng");

        ComboBox<String> cbCat = new ComboBox<>();
        cbCat.getItems().addAll(categoryService.getAllCategories().stream().map(c -> c.getName()).toList());
        if (!cbCat.getItems().isEmpty()) cbCat.setValue(cbCat.getItems().get(0));

        TextField txtPrice = new TextField();
        txtPrice.setPromptText("Ví dụ: 35000");

        TextField txtIcon = new TextField("☕");
        txtIcon.setPrefWidth(60);

        TextArea txtDesc = new TextArea();
        txtDesc.setPromptText("Mô tả thành phần, hương vị đặc trưng...");
        txtDesc.setPrefRowCount(3);

        ComboBox<String> cbS = new ComboBox<>();
        cbS.getItems().addAll("ACTIVE", "INACTIVE");
        cbS.setValue("ACTIVE");

        Label lblErr = new Label();
        lblErr.setStyle("-fx-text-fill: #C62828; -fx-font-weight: 700;");

        grid.add(new Label("Tên món:"), 0, 0);
        grid.add(txtName, 1, 0);
        grid.add(new Label("Danh mục:"), 0, 1);
        grid.add(cbCat, 1, 1);
        grid.add(new Label("Giá bán (VNĐ):"), 0, 2);
        grid.add(txtPrice, 1, 2);
        grid.add(new Label("Icon Emoji:"), 0, 3);
        grid.add(txtIcon, 1, 3);
        grid.add(new Label("Mô tả:"), 0, 4);
        grid.add(txtDesc, 1, 4);
        grid.add(new Label("Trạng thái:"), 0, 5);
        grid.add(cbS, 1, 5);
        grid.add(lblErr, 0, 6, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button btnOk = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            double price;
            try {
                price = Double.parseDouble(txtPrice.getText().trim());
            } catch (Exception ex) {
                lblErr.setText("⚠️ Giá bán không hợp lệ (vui lòng chỉ nhập số)!");
                e.consume();
                return;
            }

            String err = menuService.createMenuItem(
                    txtName.getText(),
                    price,
                    cbCat.getValue(),
                    txtDesc.getText(),
                    txtIcon.getText(),
                    cbS.getValue(),
                    currentAdmin
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

    private void showEditMenuItemDialog(MenuItem item) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Chỉnh Sửa Món");
        dialog.setHeaderText("Cập nhật thông tin món #" + item.getId() + " - " + item.getName());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField txtName = new TextField(item.getName());

        ComboBox<String> cbCat = new ComboBox<>();
        cbCat.getItems().addAll(categoryService.getAllCategories().stream().map(c -> c.getName()).toList());
        cbCat.setValue(item.getCategory());

        TextField txtPrice = new TextField(String.format("%.0f", item.getPrice()));
        TextField txtIcon = new TextField(item.getIconEmoji());
        txtIcon.setPrefWidth(60);

        TextArea txtDesc = new TextArea(item.getDescription());
        txtDesc.setPrefRowCount(3);

        ComboBox<String> cbS = new ComboBox<>();
        cbS.getItems().addAll("ACTIVE", "INACTIVE");
        cbS.setValue(item.getStatus());

        Label lblErr = new Label();
        lblErr.setStyle("-fx-text-fill: #C62828; -fx-font-weight: 700;");

        grid.add(new Label("Tên món:"), 0, 0);
        grid.add(txtName, 1, 0);
        grid.add(new Label("Danh mục:"), 0, 1);
        grid.add(cbCat, 1, 1);
        grid.add(new Label("Giá bán (VNĐ):"), 0, 2);
        grid.add(txtPrice, 1, 2);
        grid.add(new Label("Icon Emoji:"), 0, 3);
        grid.add(txtIcon, 1, 3);
        grid.add(new Label("Mô tả:"), 0, 4);
        grid.add(txtDesc, 1, 4);
        grid.add(new Label("Trạng thái:"), 0, 5);
        grid.add(cbS, 1, 5);
        grid.add(lblErr, 0, 6, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button btnOk = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            double price;
            try {
                price = Double.parseDouble(txtPrice.getText().trim());
            } catch (Exception ex) {
                lblErr.setText("⚠️ Giá bán không hợp lệ (vui lòng chỉ nhập số)!");
                e.consume();
                return;
            }

            String err = menuService.updateMenuItem(
                    item.getId(),
                    txtName.getText(),
                    price,
                    cbCat.getValue(),
                    txtDesc.getText(),
                    txtIcon.getText(),
                    cbS.getValue(),
                    currentAdmin
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

    private void confirmToggleStatus(MenuItem item) {
        String actionName = item.isActive() ? "ngừng bán" : "mở bán";
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác Nhận Thay Đổi Trạng Thái Món");
        alert.setHeaderText("Bạn có chắc chắn muốn " + actionName + " món '" + item.getName() + "'?");
        alert.setContentText("Khi ngừng bán, món này sẽ bị ẩn khỏi thực đơn trên màn hình Kiosk.");

        Optional<ButtonType> res = alert.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            String err = menuService.toggleMenuItemStatus(item.getId(), currentAdmin);
            if (err != null) {
                Alert errAlert = new Alert(Alert.AlertType.ERROR, err, ButtonType.OK);
                errAlert.showAndWait();
            } else {
                refreshTable();
            }
        }
    }
}
