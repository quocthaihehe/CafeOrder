package cafe.views;

import cafe.models.CustomerFeedback;
import cafe.models.StaffModel;
import cafe.services.ClientSocketService;
import cafe.services.DatabaseManager;
import cafe.utils.AnimationHelper;
import cafe.utils.SvgIcons;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * CustomerFeedbackDialog - Hộp thoại Đánh giá Dịch vụ & Chấm điểm KPI Nhân viên từ Khách hàng:
 * - Chấm điểm trực quan từ 1 đến 5 sao có hiệu ứng phản hồi.
 * - Danh sách thẻ nhận xét nhanh (Pha chế ngon, Phục vụ nhanh nhẹn, Thân thiện...).
 * - Nhập nhận xét tự do gửi thẳng về hệ thống quản lý.
 * - Tự động ghi nhận vào hồ sơ KPI của nhân viên trong ca trực.
 */
public class CustomerFeedbackDialog extends Stage {
    private static final String FONT_FAMILY = "-fx-font-family: 'Segoe UI', 'Inter', -apple-system, sans-serif;";

    private final int tableNumber;
    private final int orderId;
    private final ClientSocketService socketService;
    private final Consumer<String> toastCallback;

    private int selectedRating = 5;
    private final List<Label> starLabels = new ArrayList<>();
    private final Label lblRatingDesc = new Label("🌟 Rất hài lòng & Tuyệt vời!");
    private final List<String> selectedTags = new ArrayList<>();
    private final TextArea txtComment = new TextArea();
    private final ComboBox<String> cboStaff = new ComboBox<>();

    public CustomerFeedbackDialog(Stage parentStage, int tableNumber, int orderId,
                                  ClientSocketService socketService, Consumer<String> toastCallback) {
        this.tableNumber = tableNumber;
        this.orderId = orderId;
        this.socketService = socketService;
        this.toastCallback = toastCallback;

        initOwner(parentStage);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.TRANSPARENT);
        setTitle("Đánh giá dịch vụ • Bàn " + tableNumber);
        setResizable(false);

        initUI();
    }

    private void initUI() {
        VBox root = new VBox(14);
        root.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-background-radius: 16px; " +
            "-fx-border-color: #E2DDD5; " +
            "-fx-border-radius: 16px; " +
            "-fx-border-width: 1px; " +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.22), 24, 0.12, 0, 8);"
        );
        root.setPadding(new Insets(18, 22, 20, 22));
        root.setPrefWidth(480);
        root.setMaxWidth(500);

        AnimationHelper.playDialogEntrance(root);

        // 1. Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        cafe.utils.WindowChromeHelper.makeDraggable(this, header);

        VBox titleBox = new VBox(2);
        Label lblTitle = new Label("Đánh Giá Trải Nghiệm Dịch Vụ");
        lblTitle.setStyle(FONT_FAMILY + " -fx-font-size: 16.5px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        Label lblSub = new Label("Ý kiến của bạn giúp chúng tôi nâng cao chất lượng phục vụ mỗi ngày");
        lblSub.setStyle(FONT_FAMILY + " -fx-font-size: 11.5px; -fx-text-fill: #78716C;");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        String tblStr = tableNumber <= 0 ? "Mang về" : "Bàn " + (tableNumber < 10 ? "0" + tableNumber : tableNumber);
        Label lblTblBadge = new Label(tblStr);
        lblTblBadge.setStyle(FONT_FAMILY + " -fx-background-color: #EFEBE4; -fx-text-fill: #563521; -fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 3 8; -fx-background-radius: 6px;");

        Button btnClose = new Button();
        btnClose.setGraphic(cafe.utils.IconHelper.getIcon("client", "dong-cua-so.png", SvgIcons.CLOSE, "✕", 11, Color.web("#78716C")));
        btnClose.setStyle("-fx-background-color: #F5EFEB; -fx-background-radius: 12px; -fx-min-width: 26px; -fx-min-height: 26px; -fx-padding: 0; -fx-cursor: hand;");
        btnClose.setOnAction(e -> close());

        header.getChildren().addAll(titleBox, sp, lblTblBadge, btnClose);

        // 2. Interactive Star Rating Bar (1 đến 5 sao)
        VBox starBox = new VBox(6);
        starBox.setAlignment(Pos.CENTER);
        starBox.setPadding(new Insets(10, 0, 4, 0));

        HBox starsRow = new HBox(8);
        starsRow.setAlignment(Pos.CENTER);

        for (int i = 1; i <= 5; i++) {
            final int starVal = i;
            Label star = new Label("★");
            star.setStyle("-fx-font-size: 38px; -fx-cursor: hand; -fx-text-fill: #F59E0B;");
            star.setOnMouseEntered(e -> previewStars(starVal));
            star.setOnMouseExited(e -> updateStarsDisplay());
            star.setOnMouseClicked(e -> {
                selectedRating = starVal;
                updateStarsDisplay();
            });
            starLabels.add(star);
            starsRow.getChildren().add(star);
        }

        lblRatingDesc.setStyle(FONT_FAMILY + " -fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #D97706;");
        starBox.getChildren().addAll(starsRow, lblRatingDesc);

        // 3. Nhân viên phục vụ đánh giá (Dropdown)
        VBox staffSelectBox = new VBox(4);
        Label lblStaffPick = new Label("Gửi đánh giá tới:");
        lblStaffPick.setStyle(FONT_FAMILY + " -fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #4B5563;");

        cboStaff.setMaxWidth(Double.MAX_VALUE);
        cboStaff.setStyle(FONT_FAMILY + " -fx-font-size: 12px; -fx-background-color: #F8F5EE; -fx-background-radius: 8px; -fx-border-color: #E2DDD5; -fx-border-radius: 8px;");
        cboStaff.getItems().add("Toàn bộ đội ngũ ca trực (Chung)");
        List<StaffModel> staffList = DatabaseManager.getAllStaff();
        for (StaffModel s : staffList) {
            if ("WAITER".equalsIgnoreCase(s.getRole()) || "BARISTA".equalsIgnoreCase(s.getRole()) || "CASHIER".equalsIgnoreCase(s.getRole())) {
                cboStaff.getItems().add(s.getFullName() + " (" + s.getRoleDisplayName() + " - @" + s.getUsername() + ")");
            }
        }
        cboStaff.getSelectionModel().selectFirst();
        staffSelectBox.getChildren().addAll(lblStaffPick, cboStaff);

        // 4. Quick Tags
        VBox tagsBox = new VBox(6);
        Label lblTagsTitle = new Label("Điểm bạn ấn tượng nhất (chọn nhanh):");
        lblTagsTitle.setStyle(FONT_FAMILY + " -fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #4B5563;");

        FlowPane flowTags = new FlowPane(6, 6);
        List<String> presetTags = Arrays.asList(
            "🍹 Pha chế ngon chuẩn vị",
            "⚡ Phục vụ nhanh nhẹn",
            "😊 Thái độ thân thiện, niềm nở",
            "✨ Quán sạch đẹp, thoáng mát",
            "🎶 Nhạc hay, không gian êm",
            "💯 Giá cả hợp lý"
        );

        for (String tagText : presetTags) {
            Button btnTag = new Button(tagText);
            btnTag.setStyle(FONT_FAMILY + " -fx-font-size: 11px; -fx-background-color: #F3F4F6; -fx-text-fill: #4B5563; -fx-background-radius: 16px; -fx-padding: 4 10; -fx-cursor: hand;");
            btnTag.setOnAction(e -> {
                if (selectedTags.contains(tagText)) {
                    selectedTags.remove(tagText);
                    btnTag.setStyle(FONT_FAMILY + " -fx-font-size: 11px; -fx-background-color: #F3F4F6; -fx-text-fill: #4B5563; -fx-background-radius: 16px; -fx-padding: 4 10; -fx-cursor: hand;");
                } else {
                    selectedTags.add(tagText);
                    btnTag.setStyle(FONT_FAMILY + " -fx-font-size: 11px; -fx-background-color: #DEF7EC; -fx-text-fill: #03543F; -fx-font-weight: 700; -fx-background-radius: 16px; -fx-padding: 4 10; -fx-cursor: hand; -fx-border-color: #31C48D; -fx-border-radius: 16px;");
                }
            });
            flowTags.getChildren().add(btnTag);
        }
        tagsBox.getChildren().addAll(lblTagsTitle, flowTags);

        // 5. Text comment
        VBox commentBox = new VBox(4);
        Label lblCommentTitle = new Label("Lời nhắn hoặc góp ý thêm (tùy chọn):");
        lblCommentTitle.setStyle(FONT_FAMILY + " -fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #4B5563;");

        txtComment.setPromptText("Chia sẻ cảm nhận chân thật của bạn về đồ uống hoặc nhân viên phục vụ...");
        txtComment.setPrefRowCount(3);
        txtComment.setWrapText(true);
        txtComment.setStyle(FONT_FAMILY + " -fx-font-size: 12px; -fx-background-color: #FAFAF9; -fx-border-color: #E2DDD5; -fx-border-radius: 8px; -fx-background-radius: 8px;");
        commentBox.getChildren().addAll(lblCommentTitle, txtComment);

        // 6. Action Buttons
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(6, 0, 0, 0));

        Button btnSkip = new Button("Để sau");
        btnSkip.setStyle(FONT_FAMILY + " -fx-background-color: #F3F4F6; -fx-text-fill: #4B5563; -fx-font-size: 12.5px; -fx-font-weight: 600; -fx-background-radius: 8px; -fx-padding: 7 16; -fx-cursor: hand;");
        btnSkip.setOnAction(e -> close());

        Button btnSubmit = new Button("Gửi đánh giá ⭐");
        btnSubmit.setStyle(FONT_FAMILY + " -fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-size: 13px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 7 20; -fx-cursor: hand;");
        AnimationHelper.applyButtonPress(btnSubmit);
        btnSubmit.setOnAction(e -> submitFeedback());

        actions.getChildren().addAll(btnSkip, btnSubmit);

        root.getChildren().addAll(header, starBox, staffSelectBox, tagsBox, commentBox, actions);

        StackPane sceneRoot = new StackPane(root);
        sceneRoot.setStyle("-fx-background-color: transparent;");
        sceneRoot.setPadding(new Insets(10));

        Scene scene = new Scene(sceneRoot);
        scene.setFill(Color.TRANSPARENT);
        cafe.utils.WindowChromeHelper.applyTheme(scene);
        setScene(scene);

        updateStarsDisplay();
    }

    private void previewStars(int count) {
        for (int i = 0; i < starLabels.size(); i++) {
            if (i < count) {
                starLabels.get(i).setStyle("-fx-font-size: 38px; -fx-cursor: hand; -fx-text-fill: #F59E0B;");
            } else {
                starLabels.get(i).setStyle("-fx-font-size: 38px; -fx-cursor: hand; -fx-text-fill: #E5E7EB;");
            }
        }
        updateDescText(count);
    }

    private void updateStarsDisplay() {
        for (int i = 0; i < starLabels.size(); i++) {
            if (i < selectedRating) {
                starLabels.get(i).setStyle("-fx-font-size: 38px; -fx-cursor: hand; -fx-text-fill: #F59E0B;");
            } else {
                starLabels.get(i).setStyle("-fx-font-size: 38px; -fx-cursor: hand; -fx-text-fill: #E5E7EB;");
            }
        }
        updateDescText(selectedRating);
    }

    private void updateDescText(int count) {
        switch (count) {
            case 1:
                lblRatingDesc.setText("😞 Rất không hài lòng (Cần cải thiện)");
                lblRatingDesc.setStyle(FONT_FAMILY + " -fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #DC2626;");
                break;
            case 2:
                lblRatingDesc.setText("🙁 Chưa hài lòng");
                lblRatingDesc.setStyle(FONT_FAMILY + " -fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #EA580C;");
                break;
            case 3:
                lblRatingDesc.setText("😐 Bình thường / Tạm ổn");
                lblRatingDesc.setStyle(FONT_FAMILY + " -fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #D97706;");
                break;
            case 4:
                lblRatingDesc.setText("😊 Hài lòng & Phục vụ tốt");
                lblRatingDesc.setStyle(FONT_FAMILY + " -fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #059669;");
                break;
            case 5:
            default:
                lblRatingDesc.setText("🌟 Rất hài lòng & Tuyệt vời!");
                lblRatingDesc.setStyle(FONT_FAMILY + " -fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #047857;");
                break;
        }
    }

    private void submitFeedback() {
        String staffPick = cboStaff.getValue();
        String targetUsername = "all";
        String targetFullName = "Đội ngũ quán";

        if (staffPick != null && staffPick.contains("@")) {
            int atIdx = staffPick.lastIndexOf('@');
            int closeParen = staffPick.lastIndexOf(')');
            if (atIdx != -1 && closeParen > atIdx) {
                targetUsername = staffPick.substring(atIdx + 1, closeParen).trim();
            }
            int parenIdx = staffPick.indexOf(" (");
            if (parenIdx != -1) {
                targetFullName = staffPick.substring(0, parenIdx).trim();
            }
        }

        CustomerFeedback fb = new CustomerFeedback(
            "FB-" + UUID.randomUUID().toString().substring(0, 8),
            orderId,
            tableNumber,
            targetUsername,
            targetFullName,
            selectedRating,
            new ArrayList<>(selectedTags),
            txtComment.getText() != null ? txtComment.getText().trim() : "",
            System.currentTimeMillis()
        );

        if (socketService != null && socketService.isConnected()) {
            socketService.sendCustomerFeedback(fb);
        } else {
            DatabaseManager.recordCustomerFeedback(fb);
        }

        if (toastCallback != null) {
            toastCallback.accept("Cảm ơn bạn đã gửi đánh giá " + selectedRating + " sao cho quán!");
        }

        close();
    }
}
