package cafe.views;

import cafe.models.Order;
import cafe.utils.AnimationHelper;
import cafe.utils.CurrencyFormatter;
import cafe.utils.SvgIcons;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * OrdersDialog - Màn hình Quản lý Đơn hàng cho Nhân viên (Screen 7 trong UI_mong_muon.png):
 * - Header: Nút back ←, Tiêu đề "Đơn hàng", Icon xóa/clear 🗑️
 * - 3 Tabs lọc trạng thái: [ Đang phục vụ ] (Active), [ Hoàn thành ], [ Hủy ]
 * - Danh sách thẻ đơn hàng: chiều cao 64px, bo góc 6px, nền #FBF9F5, viền #DED7CD
 * - Mỗi thẻ có: Icon bàn 🪑, Số bàn ("Bàn 03"), Tóm tắt ("2 món · 120.000đ"), Giờ gọi ("10:24"), Badge "Đang phục vụ"
 */
public class OrdersDialog extends Stage {

    private String currentFilter = "SERVING"; // "SERVING", "COMPLETED", "CANCELLED"
    private final VBox cardsContainer = new VBox(8);

    private Button tabServing;
    private Button tabCompleted;
    private Button tabCancelled;

    public OrdersDialog() {
        this(null);
    }

    public OrdersDialog(Stage parentStage) {
        if (parentStage != null) {
            initOwner(parentStage);
        }
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.TRANSPARENT);
        setTitle("Quản Lý Đơn Hàng • Brew & Bean CAFE");
        setResizable(false);

        initUI();
    }

    private void initUI() {
        VBox root = new VBox(14);
        root.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16px; -fx-border-color: #E2DDD5; -fx-border-radius: 16px; -fx-border-width: 1px; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.22), 24, 0.12, 0, 8);");
        root.setPadding(new Insets(20, 22, 20, 22));
        root.setPrefWidth(460);
        root.setMaxWidth(480);
        root.setPrefHeight(620);

        AnimationHelper.playDialogEntrance(root);

        // 1. Header: Back ←, "Đơn hàng", Trash Icon, Nút đóng [X]
        HBox headerRow = new HBox(10);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setStyle("-fx-cursor: move; -fx-padding: 0 0 4 0;");
        cafe.utils.WindowChromeHelper.makeDraggable(this, headerRow);

        Button btnBack = new Button("←");
        btnBack.setStyle("-fx-background-color: #F5EFEB; -fx-text-fill: #292722; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 14px; -fx-min-width: 28px; -fx-min-height: 28px; -fx-pref-width: 28px; -fx-pref-height: 28px; -fx-padding: 0; -fx-alignment: center;");
        btnBack.setOnMouseEntered(e -> btnBack.setStyle("-fx-background-color: #EAE4DA; -fx-text-fill: #292722; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 14px; -fx-min-width: 28px; -fx-min-height: 28px; -fx-pref-width: 28px; -fx-pref-height: 28px; -fx-padding: 0; -fx-alignment: center;"));
        btnBack.setOnMouseExited(e -> btnBack.setStyle("-fx-background-color: #F5EFEB; -fx-text-fill: #292722; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-radius: 14px; -fx-min-width: 28px; -fx-min-height: 28px; -fx-pref-width: 28px; -fx-pref-height: 28px; -fx-padding: 0; -fx-alignment: center;"));
        btnBack.setOnMousePressed(javafx.event.Event::consume);
        btnBack.setOnAction(e -> close());

        Label lblTitle = new Label("Đơn hàng");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnTrash = new Button();
        btnTrash.setGraphic(cafe.utils.IconHelper.getIcon("client", "xoa-mon.png", SvgIcons.TRASH, "🗑", 15, Color.web("#817A70")));
        btnTrash.setStyle("-fx-background-color: #F5EFEB; -fx-cursor: hand; -fx-padding: 0; -fx-background-radius: 14px; -fx-min-width: 28px; -fx-min-height: 28px; -fx-pref-width: 28px; -fx-pref-height: 28px; -fx-alignment: center;");
        btnTrash.setOnMousePressed(javafx.event.Event::consume);
        btnTrash.setOnAction(e -> {
            cardsContainer.getChildren().clear();
            showEmptyOrders();
        });

        Button btnClose = new Button();
        btnClose.setGraphic(cafe.utils.IconHelper.getIcon("client", "dong-cua-so.png", SvgIcons.CLOSE, "✕", 13, Color.web("#563521")));
        btnClose.setStyle("-fx-background-color: #F5EFEB; -fx-background-radius: 14px; -fx-min-width: 28px; -fx-min-height: 28px; -fx-pref-width: 28px; -fx-pref-height: 28px; -fx-padding: 0; -fx-cursor: hand; -fx-alignment: center;");
        btnClose.setOnMouseEntered(e -> btnClose.setStyle("-fx-background-color: #EAE4DA; -fx-background-radius: 14px; -fx-min-width: 28px; -fx-min-height: 28px; -fx-pref-width: 28px; -fx-pref-height: 28px; -fx-padding: 0; -fx-cursor: hand; -fx-alignment: center;"));
        btnClose.setOnMouseExited(e -> btnClose.setStyle("-fx-background-color: #F5EFEB; -fx-background-radius: 14px; -fx-min-width: 28px; -fx-min-height: 28px; -fx-pref-width: 28px; -fx-pref-height: 28px; -fx-padding: 0; -fx-cursor: hand; -fx-alignment: center;"));
        btnClose.setOnMousePressed(javafx.event.Event::consume);
        btnClose.setOnAction(e -> close());

        headerRow.getChildren().addAll(btnBack, lblTitle, sp, btnTrash, btnClose);

        // 2. Three Filter Tabs: [ Đang phục vụ ], [ Hoàn thành ], [ Hủy ]
        HBox tabsRow = new HBox(10);
        tabsRow.setAlignment(Pos.CENTER_LEFT);
        tabsRow.setPadding(new Insets(0, 0, 6, 0));
        tabsRow.setStyle("-fx-border-color: transparent transparent #EAE4DA transparent; -fx-border-width: 0 0 1 0;");

        tabServing = new Button("Đang phục vụ");
        tabCompleted = new Button("Hoàn thành");
        tabCancelled = new Button("Hủy");

        tabServing.setOnAction(e -> switchFilter("SERVING"));
        tabCompleted.setOnAction(e -> switchFilter("COMPLETED"));
        tabCancelled.setOnAction(e -> switchFilter("CANCELLED"));

        updateTabStyles();
        tabsRow.getChildren().addAll(tabServing, tabCompleted, tabCancelled);

        // 3. Scrollable Order Cards List
        cardsContainer.setSpacing(10);
        cardsContainer.setPadding(new Insets(4, 2, 4, 2));

        ScrollPane scroll = new ScrollPane(cardsContainer);
        scroll.setFitToWidth(true);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().addAll(headerRow, tabsRow, scroll);

        StackPane sceneRoot = new StackPane(root);
        sceneRoot.setStyle("-fx-background-color: transparent;");
        sceneRoot.setPadding(new Insets(16));

        Scene scene = new Scene(sceneRoot);
        scene.setFill(Color.TRANSPARENT);
        cafe.utils.WindowChromeHelper.applyTheme(scene);

        setScene(scene);
        renderOrders();
    }

    private void switchFilter(String filter) {
        this.currentFilter = filter;
        updateTabStyles();
        renderOrders();
    }

    private void updateTabStyles() {
        String active = "-fx-background-color: transparent; -fx-text-fill: #112823; -fx-font-size: 13px; -fx-font-weight: 800; -fx-border-color: transparent transparent #112823 transparent; -fx-border-width: 0 0 2.5 0; -fx-padding: 4 10 8 10; -fx-cursor: hand;";
        String inactive = "-fx-background-color: transparent; -fx-text-fill: #817A70; -fx-font-size: 13px; -fx-font-weight: 600; -fx-padding: 4 10 8 10; -fx-cursor: hand;";

        if (tabServing != null) tabServing.setStyle("SERVING".equals(currentFilter) ? active : inactive);
        if (tabCompleted != null) tabCompleted.setStyle("COMPLETED".equals(currentFilter) ? active : inactive);
        if (tabCancelled != null) tabCancelled.setStyle("CANCELLED".equals(currentFilter) ? active : inactive);
    }

    private void renderOrders() {
        cardsContainer.getChildren().clear();

        if ("SERVING".equals(currentFilter)) {
            // Dữ liệu mẫu chuẩn Screen 7 trong UI_mong_muon.png
            cardsContainer.getChildren().add(createOrderCard("Bàn 03", "2 món · 120.000đ", "10:24", "Đang phục vụ", "#DCE8D9", "#597256"));
            cardsContainer.getChildren().add(createOrderCard("Bàn 05", "3 món · 175.000đ", "10:18", "Đang phục vụ", "#DCE8D9", "#597256"));
            cardsContainer.getChildren().add(createOrderCard("Bàn 08", "1 món · 65.000đ", "10:15", "Đang phục vụ", "#DCE8D9", "#597256"));
            cardsContainer.getChildren().add(createOrderCard("Bàn 12", "2 món · 90.000đ", "10:05", "Đang phục vụ", "#DCE8D9", "#597256"));
        } else if ("COMPLETED".equals(currentFilter)) {
            cardsContainer.getChildren().add(createOrderCard("Bàn 01", "2 món · 110.000đ", "09:45", "Hoàn thành", "#EAE6DE", "#6F695F"));
            cardsContainer.getChildren().add(createOrderCard("Bàn 02", "4 món · 230.000đ", "09:20", "Hoàn thành", "#EAE6DE", "#6F695F"));
        } else {
            cardsContainer.getChildren().add(createOrderCard("Bàn 07", "1 món · 55.000đ", "09:05", "Đã hủy", "#FDEEEE", "#C62828"));
        }
    }

    private HBox createOrderCard(String tableName, String summary, String timeStr, String badgeLabel, String badgeBg, String badgeFg) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setPrefHeight(70);
        card.setStyle("-fx-background-color: #FDFBF8; -fx-background-radius: 8px; -fx-border-color: #E6E0D8; -fx-border-radius: 8px; -fx-border-width: 1px;");
        AnimationHelper.applyButtonPress(card);

        // Icon bàn 🪑 từ staff/so-do-ban.png
        javafx.scene.Node tableIcon = cafe.utils.IconHelper.getIcon("staff", "so-do-ban.png", SvgIcons.TABLE, "🪑", 22, Color.web("#112823"));

        // Thông tin bàn & tóm tắt
        VBox infoBox = new VBox(3);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label lblTable = new Label(tableName);
        lblTable.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #1C1917;");

        Label lblSummary = new Label(summary);
        lblSummary.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #563521;");

        infoBox.getChildren().addAll(lblTable, lblSummary);

        // Right side: Badge trạng thái + Giờ gọi + Nút in/hủy
        VBox rightBox = new VBox(4);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        HBox topStatusRow = new HBox(6);
        topStatusRow.setAlignment(Pos.CENTER_RIGHT);

        Label badge = new Label(badgeLabel);
        badge.setStyle("-fx-background-color: " + badgeBg + "; -fx-text-fill: " + badgeFg + "; -fx-font-size: 10.5px; -fx-font-weight: 700; -fx-background-radius: 10px; -fx-padding: 3 8 3 8;");

        Label lblTime = new Label(timeStr);
        lblTime.setStyle("-fx-font-size: 11px; -fx-text-fill: #817A70;");

        topStatusRow.getChildren().addAll(badge, lblTime);

        // Quick action buttons: In hóa đơn & Hủy đơn
        HBox actionsRow = new HBox(6);
        actionsRow.setAlignment(Pos.CENTER_RIGHT);

        Button btnPrint = new Button();
        btnPrint.setGraphic(cafe.utils.IconHelper.getIcon("staff", "in-hoa-don.png", SvgIcons.EDIT, "🖨", 14, Color.web("#112823")));
        btnPrint.setStyle("-fx-background-color: #F5EFEB; -fx-background-radius: 4px; -fx-padding: 3 6 3 6; -fx-cursor: hand;");
        btnPrint.setTooltip(new javafx.scene.control.Tooltip("In hóa đơn / Tạm tính"));
        btnPrint.setOnMousePressed(javafx.event.Event::consume);

        if ("Đang phục vụ".equalsIgnoreCase(badgeLabel)) {
            Button btnCancel = new Button();
            btnCancel.setGraphic(cafe.utils.IconHelper.getIcon("staff", "huy-mon.png", SvgIcons.CLOSE, "✕", 12, Color.web("#C62828")));
            btnCancel.setStyle("-fx-background-color: #FDEEEE; -fx-background-radius: 4px; -fx-padding: 3 6 3 6; -fx-cursor: hand;");
            btnCancel.setTooltip(new javafx.scene.control.Tooltip("Hủy đơn này"));
            btnCancel.setOnMousePressed(javafx.event.Event::consume);
            actionsRow.getChildren().addAll(btnPrint, btnCancel);
        } else {
            actionsRow.getChildren().add(btnPrint);
        }

        rightBox.getChildren().addAll(topStatusRow, actionsRow);

        card.getChildren().addAll(tableIcon, infoBox, rightBox);
        return card;
    }

    private void showEmptyOrders() {
        VBox emptyBox = new VBox(8);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setPadding(new Insets(60, 20, 60, 20));
        Label lblEmpty = new Label("Không có đơn hàng nào.");
        lblEmpty.setStyle("-fx-font-size: 12px; -fx-text-fill: #817A70;");
        emptyBox.getChildren().add(lblEmpty);
        cardsContainer.getChildren().add(emptyBox);
    }
}
