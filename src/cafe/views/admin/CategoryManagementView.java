package cafe.views.admin;

import cafe.models.Category;
import cafe.services.CategoryService;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;
import java.util.Optional;

public class CategoryManagementView extends VBox {
    private final CategoryService categoryService;
    private final String currentAdmin;

    private final TableView<Category> table = new TableView<>();
    private final TextField txtSearch = new TextField();
    private final Label lblCount = new Label();
    private final VBox emptyState = new VBox(10);

    public CategoryManagementView(CategoryService categoryService, String currentAdmin) {
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
        Label lblTitle = new Label("Quản Lý Danh Mục Món");
        lblTitle.getStyleClass().add("page-title");
        Label lblSub = new Label("Phân nhóm các món ăn và đồ uống trong thực đơn");
        lblSub.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAdd = new Button("➕ Thêm danh mục");
        btnAdd.getStyleClass().add("btn-primary");
        btnAdd.setOnAction(e -> showAddCategoryDialog());

        header.getChildren().addAll(titleBox, spacer, btnAdd);

        // 2. Filter Bar
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        txtSearch.setPromptText("🔍 Tìm danh mục...");
        txtSearch.setPrefWidth(280);
        txtSearch.textProperty().addListener((obs, o, n) -> refreshTable());

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        lblCount.setStyle("-fx-font-weight: 700; -fx-text-fill: #7D6B63;");

        filterBar.getChildren().addAll(txtSearch, spacer2, lblCount);

        // 3. Table
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Category, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        colId.setPrefWidth(60);
        colId.setStyle("-fx-alignment: CENTER;");

        TableColumn<Category, String> colName = new TableColumn<>("Tên danh mục");
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        colName.setPrefWidth(220);

        TableColumn<Category, String> colStatus = new TableColumn<>("Trạng thái");
        colStatus.setPrefWidth(120);
        colStatus.setStyle("-fx-alignment: CENTER;");
        colStatus.setCellFactory(col -> new TableCell<Category, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Category c = getTableRow().getItem();
                    Label badge = new Label(c.getStatus());
                    badge.getStyleClass().add(c.isActive() ? "badge-active" : "badge-disabled");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<Category, String> colDate = new TableColumn<>("Ngày tạo");
        colDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCreatedAt()));
        colDate.setPrefWidth(160);

        TableColumn<Category, Void> colActions = new TableColumn<>("Thao tác");
        colActions.setPrefWidth(180);
        colActions.setStyle("-fx-alignment: CENTER;");
        colActions.setCellFactory(col -> new TableCell<Category, Void>() {
            private final Button btnEdit = new Button("Sửa");
            private final Button btnToggle = new Button();
            private final HBox pane = new HBox(8, btnEdit, btnToggle);

            {
                pane.setAlignment(Pos.CENTER);
                btnEdit.getStyleClass().add("btn-sm-action");
                btnToggle.getStyleClass().add("btn-sm-action");

                btnEdit.setOnAction(e -> {
                    Category c = getTableView().getItems().get(getIndex());
                    showEditCategoryDialog(c);
                });

                btnToggle.setOnAction(e -> {
                    Category c = getTableView().getItems().get(getIndex());
                    confirmToggleStatus(c);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Category c = getTableView().getItems().get(getIndex());
                    btnToggle.setText(c.isActive() ? "Vô hiệu" : "Kích hoạt");
                    btnToggle.setStyle(c.isActive() ? "-fx-text-fill: #C62828;" : "-fx-text-fill: #2E7D32;");
                    setGraphic(pane);
                }
            }
        });

        table.getColumns().addAll(colId, colName, colStatus, colDate, colActions);

        // Empty state
        emptyState.getStyleClass().add("empty-state-box");
        Label lblEmptyTitle = new Label("Không tìm thấy danh mục");
        lblEmptyTitle.getStyleClass().add("empty-state-title");
        Label lblEmptySub = new Label("Không có danh mục nào khớp với từ khóa tìm kiếm.");
        lblEmptySub.getStyleClass().add("empty-state-sub");
        emptyState.getChildren().addAll(lblEmptyTitle, lblEmptySub);

        StackPane tableContainer = new StackPane(table, emptyState);
        VBox.setVgrow(tableContainer, Priority.ALWAYS);

        getChildren().addAll(header, filterBar, tableContainer);
    }

    private void refreshTable() {
        String kw = txtSearch.getText();
        List<Category> list = categoryService.searchCategories(kw);
        table.getItems().setAll(list);
        lblCount.setText(list.size() + " danh mục");

        boolean isEmpty = list.isEmpty();
        emptyState.setVisible(isEmpty);
        emptyState.setManaged(isEmpty);
        table.setVisible(!isEmpty);
        table.setManaged(!isEmpty);
    }

    private void showAddCategoryDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Thêm Danh Mục Mới");
        dialog.setHeaderText("Tạo danh mục đồ uống hoặc món ăn");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField txtName = new TextField();
        txtName.setPromptText("Ví dụ: Trà sữa đặc biệt");

        ComboBox<String> cbS = new ComboBox<>();
        cbS.getItems().addAll("ACTIVE", "INACTIVE");
        cbS.setValue("ACTIVE");

        Label lblErr = new Label();
        lblErr.setStyle("-fx-text-fill: #C62828; -fx-font-weight: 700;");

        grid.add(new Label("Tên danh mục:"), 0, 0);
        grid.add(txtName, 1, 0);
        grid.add(new Label("Trạng thái:"), 0, 1);
        grid.add(cbS, 1, 1);
        grid.add(lblErr, 0, 2, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button btnOk = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            String err = categoryService.createCategory(txtName.getText(), cbS.getValue(), currentAdmin);
            if (err != null) {
                lblErr.setText("⚠️ " + err);
                e.consume();
            }
        });

        dialog.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) refreshTable();
        });
    }

    private void showEditCategoryDialog(Category category) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Chỉnh Sửa Danh Mục");
        dialog.setHeaderText("Cập nhật thông tin danh mục #" + category.getId());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField txtName = new TextField(category.getName());
        ComboBox<String> cbS = new ComboBox<>();
        cbS.getItems().addAll("ACTIVE", "INACTIVE");
        cbS.setValue(category.getStatus());

        Label lblErr = new Label();
        lblErr.setStyle("-fx-text-fill: #C62828; -fx-font-weight: 700;");

        grid.add(new Label("Tên danh mục:"), 0, 0);
        grid.add(txtName, 1, 0);
        grid.add(new Label("Trạng thái:"), 0, 1);
        grid.add(cbS, 1, 1);
        grid.add(lblErr, 0, 2, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button btnOk = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            String err = categoryService.updateCategory(category.getId(), txtName.getText(), cbS.getValue(), currentAdmin);
            if (err != null) {
                lblErr.setText("⚠️ " + err);
                e.consume();
            }
        });

        dialog.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) refreshTable();
        });
    }

    private void confirmToggleStatus(Category category) {
        String actionName = category.isActive() ? "vô hiệu hóa" : "kích hoạt";
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác Nhận Thay Đổi Trạng Thái");
        alert.setHeaderText("Bạn có chắc muốn " + actionName + " danh mục '" + category.getName() + "'?");
        alert.setContentText("Khi vô hiệu hóa, khách hàng sẽ không thấy danh mục này trên Kiosk.");

        Optional<ButtonType> res = alert.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            String err = categoryService.toggleCategoryStatus(category.getId(), currentAdmin);
            if (err != null) {
                Alert errAlert = new Alert(Alert.AlertType.ERROR, err, ButtonType.OK);
                errAlert.showAndWait();
            } else {
                refreshTable();
            }
        }
    }
}
