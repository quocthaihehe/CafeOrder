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
        initStyle(StageStyle.UTILITY);
        setTitle("Quản Lý Đơn Hàng • Brew & Bean CAFE");
        setResizable(false);

        initUI();
    }

    private void initUI() {
        VBox root = new VBox(12);
        root.setStyle("-fx-background-color: #FBF9F5; -fx-background-radius: 6px; -fx-border-color: #DED7CD; -fx-border-radius: 6px; -fx-border-width: 1px;");
        root.setPadding(new Insets(18, 20, 18, 20));
        root.setPrefWidth(380);
        root.setPrefHeight(530);

        AnimationHelper.playDialogEntrance(root);

        // 1. Header: Back ←, "Đơn hàng", Trash Icon 🗑️
        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Button btnBack = new Button("←");
        btnBack.setStyle("-fx-background-color: transparent; -fx-text-fill: #292722; -fx-font-size: 15px; -fx-cursor: hand; -fx-padding: 0 4 0 0;");
        btnBack.setOnAction(e -> close());

        Label lblTitle = new Label("Đơn hàng");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #292722;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnTrash = new Button("🗑️");
        btnTrash.setStyle("-fx-background-color: transparent; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 0;");
        btnTrash.setOnAction(e -> {
            cardsContainer.getChildren().clear();
            showEmptyOrders();
        });

        headerRow.getChildren().addAll(btnBack, lblTitle, sp, btnTrash);

        // 2. Three Filter Tabs: [ Đang phục vụ ], [ Hoàn thành ], [ Hủy ]
        HBox tabsRow = new HBox(8);
        tabsRow.setAlignment(Pos.CENTER_LEFT);
        tabsRow.setPadding(new Insets(0, 0, 4, 0));
        tabsRow.setStyle("-fx-border-color: transparent transparent #E6DFD4 transparent; -fx-border-width: 0 0 1 0;");

        tabServing = new Button("Đang phục vụ");
        tabCompleted = new Button("Hoàn thành");
        tabCancelled = new Button("Hủy");

        tabServing.setOnAction(e -> switchFilter("SERVING"));
        tabCompleted.setOnAction(e -> switchFilter("COMPLETED"));
        tabCancelled.setOnAction(e -> switchFilter("CANCELLED"));

        updateTabStyles();
        tabsRow.getChildren().addAll(tabServing, tabCompleted, tabCancelled);

        // 3. Scrollable Order Cards List
        cardsContainer.setSpacing(8);
        cardsContainer.setPadding(new Insets(4, 2, 4, 2));

        ScrollPane scroll = new ScrollPane(cardsContainer);
        scroll.setFitToWidth(true);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().addAll(headerRow, tabsRow, scroll);

        Scene scene = new Scene(root);
        try {
            scene.getStylesheets().add(getClass().getResource("/cafe/resources/css/cafe-theme.css").toExternalForm());
        } catch (Exception ignored) {}

        setScene(scene);
        renderOrders();
    }

    private void switchFilter(String filter) {
        this.currentFilter = filter;
        updateTabStyles();
        renderOrders();
    }

    private void updateTabStyles() {
        String active = "-fx-background-color: transparent; -fx-text-fill: #112823; -fx-font-size: 11.5px; -fx-font-weight: 700; -fx-border-color: transparent transparent #112823 transparent; -fx-border-width: 0 0 2 0; -fx-padding: 4 8 6 8; -fx-cursor: hand;";
        String inactive = "-fx-background-color: transparent; -fx-text-fill: #817A70; -fx-font-size: 11.5px; -fx-font-weight: 500; -fx-padding: 4 8 6 8; -fx-cursor: hand;";

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
        HBox card = new HBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setPrefHeight(64);
        card.setStyle("-fx-background-color: #FBF9F5; -fx-background-radius: 6px; -fx-border-color: #DED7CD; -fx-border-radius: 6px; -fx-border-width: 1px;");
        AnimationHelper.applyButtonPress(card);

        // Icon bàn 🪑
        SVGPath tableIcon = SvgIcons.createOutlineIcon(SvgIcons.TABLE, 0.8, Color.web("#292722"));

        // Thông tin bàn & tóm tắt
        VBox infoBox = new VBox(2);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label lblTable = new Label(tableName);
        lblTable.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #292722;");

        Label lblSummary = new Label(summary);
        lblSummary.setStyle("-fx-font-size: 10.5px; -fx-text-fill: #817A70;");

        infoBox.getChildren().addAll(lblTable, lblSummary);

        // Right side: Badge trạng thái + Giờ gọi
        VBox rightBox = new VBox(3);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        Label badge = new Label(badgeLabel);
        badge.setStyle("-fx-background-color: " + badgeBg + "; -fx-text-fill: " + badgeFg + "; -fx-font-size: 9.5px; -fx-font-weight: 600; -fx-background-radius: 10px; -fx-padding: 2 6 2 6;");

        Label lblTime = new Label(timeStr);
        lblTime.setStyle("-fx-font-size: 9.5px; -fx-text-fill: #817A70;");

        rightBox.getChildren().addAll(badge, lblTime);

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
