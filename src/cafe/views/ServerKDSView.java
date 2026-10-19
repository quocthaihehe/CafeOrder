package cafe.views;

import cafe.models.Order;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * ServerKDSView - Kitchen Display System view for order preparation.
 * Hallmark Discipline:
 * - High contrast visual hierarchy between Queued, Preparing, and Done
 * - 4px radius, clear scanning typography, no empty white abyss
 * - Brand consistency: Brew & Bean CAFE
 */
public class ServerKDSView extends BorderPane {
    // Header stat labels
    private final Label lblStatQueued = new Label("Đang chờ: 0");
    private final Label lblStatPreparing = new Label("Đang pha: 0");
    private final Label lblStatDone = new Label("Đã xong: 0");
    private final Label lblClock = new Label();

    // Control bar
    private final TextField txtSearch = new TextField();
    private final HBox filterButtonsBox = new HBox(6);
    private final Button btnClearDone = new Button("🗑 Xóa đơn đã xong");

    // Tickets container
    private final FlowPane ticketsFlowPane = new FlowPane(14, 14);
    private final ScrollPane ticketsScrollPane = new ScrollPane();
    private final VBox emptyTicketsBox = new VBox(8);

    // Bottom log bar
    private final Label lblLatestLog = new Label("Máy chủ sẵn sàng.");

    public ServerKDSView() {
        initUI();
        startClock();
    }

    private void initUI() {
        getStyleClass().add("root");

        // 1. TOP HEADER
        HBox header = buildHeader();
        setTop(header);

        // 2. CENTER - CONTROLS & TICKETS
        VBox centerSection = buildCenterSection();
        setCenter(centerSection);

        // 3. BOTTOM LOG BAR
        HBox bottomLogBar = new HBox(8);
        bottomLogBar.getStyleClass().add("kds-log-drawer");
        bottomLogBar.setAlignment(Pos.CENTER_LEFT);
        Label logIcon = new Label("HỆ THỐNG:");
        logIcon.setStyle("-fx-font-weight: 700; -fx-text-fill: #A66A42; -fx-font-size: 11px;");
        lblLatestLog.getStyleClass().add("kds-log-text");
        HBox.setHgrow(lblLatestLog, Priority.ALWAYS);
        bottomLogBar.getChildren().addAll(logIcon, lblLatestLog);
        setBottom(bottomLogBar);
    }

    private HBox buildHeader() {
        HBox header = new HBox(16);
        header.getStyleClass().add("kds-header");
        header.setAlignment(Pos.CENTER_LEFT);

        // Title
        VBox titleBox = new VBox(2);
        Label lblTitle = new Label("KDS • QUẦY ĐIỀU PHỐI PHA CHẾ");
        lblTitle.getStyleClass().add("kds-brand-title");
        Label lblSub = new Label("Kitchen Display System • Brew & Bean CAFE");
        lblSub.getStyleClass().add("kds-brand-subtitle");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Stat Pills
        lblStatQueued.getStyleClass().addAll("stat-pill", "stat-queued");
        lblStatPreparing.getStyleClass().addAll("stat-pill", "stat-preparing");
        lblStatDone.getStyleClass().addAll("stat-pill", "stat-done");

        // Clock
        lblClock.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 600; -fx-text-fill: #817B70;");

        header.getChildren().addAll(titleBox, spacer, lblStatQueued, lblStatPreparing, lblStatDone, lblClock);
        return header;
    }

    private VBox buildCenterSection() {
        VBox center = new VBox(12);
        center.setPadding(new Insets(14, 20, 14, 20));

        // Filter & Search bar
        HBox controlBar = new HBox(10);
        controlBar.setAlignment(Pos.CENTER_LEFT);

        txtSearch.setPromptText("Tìm theo số bàn hoặc mã đơn (VD: 1, 1002)...");
        txtSearch.getStyleClass().add("text-input-field");
        txtSearch.setPrefWidth(280);
        txtSearch.setPrefHeight(36);

        filterButtonsBox.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnClearDone.getStyleClass().add("kds-clear-btn");
        btnClearDone.setPrefHeight(36);

        controlBar.getChildren().addAll(txtSearch, filterButtonsBox, spacer, btnClearDone);

        // Tickets FlowPane inside ScrollPane
        ticketsFlowPane.setPadding(new Insets(4, 0, 16, 0));

        ticketsScrollPane.setContent(ticketsFlowPane);
        ticketsScrollPane.setFitToWidth(true);
        ticketsScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        ticketsScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(ticketsScrollPane, Priority.ALWAYS);

        // Empty state when no orders
        emptyTicketsBox.setAlignment(Pos.CENTER);
        emptyTicketsBox.setPadding(new Insets(80, 20, 80, 20));
        emptyTicketsBox.setVisible(false);
        emptyTicketsBox.setManaged(false);

        Label emptyIcon = new Label("☕");
        emptyIcon.setStyle("-fx-font-size: 40px; -fx-text-fill: #D8D1C3;");
        Label emptyTitle = new Label("Không có đơn hàng nào cần xử lý");
        emptyTitle.getStyleClass().add("editorial-title");
        Label emptyDesc = new Label("Các đơn hàng mới gửi từ khách sẽ tự động xuất hiện tại đây theo thời gian thực.");
        emptyDesc.getStyleClass().add("editorial-subtitle");

        emptyTicketsBox.getChildren().addAll(emptyIcon, emptyTitle, emptyDesc);

        center.getChildren().addAll(controlBar, ticketsScrollPane, emptyTicketsBox);
        return center;
    }

    public void renderFilters(List<String> filters, String activeFilter, Consumer<String> onFilterSelected) {
        filterButtonsBox.getChildren().clear();
        for (String filter : filters) {
            Button btn = new Button(filter);
            btn.getStyleClass().add("kds-filter-btn");
            if (filter.equalsIgnoreCase(activeFilter)) {
                btn.getStyleClass().add("selected");
            }
            btn.setOnAction(e -> onFilterSelected.accept(filter));
            filterButtonsBox.getChildren().add(btn);
        }
    }

    public void updateStats(int queued, int preparing, int done) {
        lblStatQueued.setText("Đang chờ: " + queued);
        lblStatPreparing.setText("Đang pha: " + preparing);
        lblStatDone.setText("Đã xong: " + done);
    }

    public void renderOrders(List<Order> orders, BiConsumer<Order, String> onStatusChange) {
        ticketsFlowPane.getChildren().clear();

        if (orders.isEmpty()) {
            ticketsScrollPane.setVisible(false);
            ticketsScrollPane.setManaged(false);
            emptyTicketsBox.setVisible(true);
            emptyTicketsBox.setManaged(true);
            return;
        }

        emptyTicketsBox.setVisible(false);
        emptyTicketsBox.setManaged(false);
        ticketsScrollPane.setVisible(true);
        ticketsScrollPane.setManaged(true);

        for (Order order : orders) {
            OrderTicketCard card = new OrderTicketCard(order, onStatusChange);
            ticketsFlowPane.getChildren().add(card);
        }
    }

    public void setLatestLog(String log) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        lblLatestLog.setText("[" + sdf.format(new Date()) + "] " + log);
    }

    private void startClock() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss • dd/MM/yyyy");
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> lblClock.setText(sdf.format(new Date()))), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    public TextField getSearchField() { return txtSearch; }
    public Button getClearDoneButton() { return btnClearDone; }
}
