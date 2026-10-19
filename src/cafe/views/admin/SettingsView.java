package cafe.views.admin;

import cafe.services.AuditLogService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class SettingsView extends VBox {
    // Lưu trữ trạng thái cài đặt trong phiên làm việc để không bị mất khi chuyển tab
    private static String savedCafeName = "L'Amour Artisan Cafe";
    private static String savedCurrency = "VNĐ (Việt Nam Đồng)";
    private static String savedLanguage = "Tiếng Việt";
    private static boolean savedAudioAlert = true;
    private static boolean savedRequireConfirmation = true;

    private final AuditLogService auditLogService;
    private final String currentAdmin;

    public SettingsView() {
        this(null, "admin01");
    }

    public SettingsView(AuditLogService auditLogService, String currentAdmin) {
        this.auditLogService = auditLogService;
        this.currentAdmin = currentAdmin != null ? currentAdmin : "admin01";
        setSpacing(20);
        setPadding(new Insets(4, 8, 20, 8));
        initUI();
    }

    public static String getSavedCafeName() { return savedCafeName; }
    public static String getSavedLanguage() { return savedLanguage; }
    public static boolean isSavedAudioAlert() { return savedAudioAlert; }
    public static boolean isSavedRequireConfirmation() { return savedRequireConfirmation; }

    public static String updateSettings(String cafeName, String language, boolean audioAlert, boolean requireConfirm, AuditLogService auditService, String actor) {
        if (cafeName == null || cafeName.trim().isEmpty()) {
            if (auditService != null) {
                auditService.log(actor, "UPDATE_SETTINGS", "Cafe Name empty", "FAILED (EMPTY_CAFE_NAME)");
            }
            return "Tên quán café không được để trống!";
        }
        savedCafeName = cafeName.trim();
        if (language != null && !language.trim().isEmpty()) {
            savedLanguage = language.trim();
        }
        savedAudioAlert = audioAlert;
        savedRequireConfirmation = requireConfirm;
        if (auditService != null) {
            auditService.log(actor, "UPDATE_SETTINGS", savedCafeName + " (" + savedLanguage + ")", "SUCCESS");
        }
        return null;
    }

    private void initUI() {
        // Header
        VBox titleBox = new VBox(2);
        Label lblTitle = new Label("Cài Đặt Hệ Thống (Settings)");
        lblTitle.getStyleClass().add("page-title");
        Label lblSub = new Label("Cấu hình chung cho quán café, tiền tệ và tùy chọn hệ thống");
        lblSub.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        // Settings Form Card
        VBox card = new VBox(16);
        card.getStyleClass().add("kpi-card");
        card.setMaxWidth(680);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);

        TextField txtName = new TextField(savedCafeName);
        TextField txtCurr = new TextField(savedCurrency);
        txtCurr.setDisable(true);

        ComboBox<String> cbLang = new ComboBox<>();
        cbLang.getItems().addAll("Tiếng Việt", "English");
        cbLang.setValue(savedLanguage);

        CheckBox chkAudio = new CheckBox("Phát âm thanh chuông khi có đơn hàng mới từ Kiosk");
        chkAudio.setSelected(savedAudioAlert);

        CheckBox chkRequireConfirm = new CheckBox("Bắt buộc xác nhận đơn mới (CREATED -> CONFIRMED) trước khi chuyển vào Bếp (KDS)");
        chkRequireConfirm.setSelected(savedRequireConfirmation);

        Label lblErr = new Label();
        lblErr.setStyle("-fx-text-fill: #C62828; -fx-font-weight: 700;");

        grid.add(new Label("Tên quán café:"), 0, 0);
        grid.add(txtName, 1, 0);
        grid.add(new Label("Đơn vị tiền tệ:"), 0, 1);
        grid.add(txtCurr, 1, 1);
        grid.add(new Label("Ngôn ngữ giao diện:"), 0, 2);
        grid.add(cbLang, 1, 2);
        grid.add(new Label("Âm thanh thông báo:"), 0, 3);
        grid.add(chkAudio, 1, 3);
        grid.add(new Label("Quy trình duyệt đơn:"), 0, 4);
        grid.add(chkRequireConfirm, 1, 4);
        grid.add(lblErr, 0, 5, 2, 1);

        Button btnSave = new Button("💾 Lưu cấu hình");
        btnSave.getStyleClass().add("btn-primary");
        btnSave.setOnAction(e -> {
            String err = updateSettings(
                    txtName.getText(),
                    cbLang.getValue(),
                    chkAudio.isSelected(),
                    chkRequireConfirm.isSelected(),
                    auditLogService,
                    currentAdmin
            );
            if (err != null) {
                lblErr.setText("⚠️ " + err);
            } else {
                lblErr.setText("");
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Thông Báo");
                alert.setHeaderText("Đã lưu cấu hình thành công!");
                alert.setContentText("Cấu hình đã được lưu giữ trong phiên làm việc hiện tại và ghi nhận vào Audit Log.");
                alert.showAndWait();
            }
        });

        Label lblNote = new Label("ℹ️ Cấu hình được lưu giữ xuyên suốt phiên làm việc hiện tại (In-Memory Session State).");
        lblNote.setStyle("-fx-font-size: 11px; -fx-text-fill: #9E8D84; -fx-padding: 8 0 0 0;");

        card.getChildren().addAll(grid, new Separator(), btnSave, lblNote);

        getChildren().addAll(titleBox, card);
    }
}
