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

public class ServerKDSView extends BorderPane {
    // Header stat labels
    private final Label lblStatQueued = new Label("Đang chờ: 0");
    private final Label lblStatPreparing = new Label("Đang pha: 0");
    private final Label lblStatDone = new Label("Đã xong: 0");
    private final Label lblClock = new Label();

    // Control bar
    private final TextField txtSearch = new TextField();
    private final HBox filterButtonsBox = new HBox(8);
    private final Button btnClearDone = new Button("Xóa đơn đã xong");

    // Tickets container
    private final FlowPane ticketsFlowPane = new FlowPane(16, 16);
    private final ScrollPane ticketsScrollPane = new ScrollPane();
    private final VBox emptyTicketsBox = new VBox(10);

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
        HBox bottomLogBar = new HBox(10);
        bottomLogBar.getStyleClass().add("kds-log-drawer");
        bottomLogBar.setAlignment(Pos.CENTER_LEFT);
        Label logIcon = new Label("Log:");
        logIcon.setStyle("-fx-font-weight: 700; -fx-text-fill: #C97A44; -fx-font-size: 11px;");
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
        Label lblSub = new Label("Kitchen Display System • L'Amour Artisan Cafe");
        lblSub.getStyleClass().add("kds-brand-subtitle");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Stat Pills
        lblStatQueued.getStyleClass().addAll("stat-pill", "stat-queued");
        lblStatPreparing.getStyleClass().addAll("stat-pill", "stat-preparing");
        lblStatDone.getStyleClass().addAll("stat-pill", "stat-done");

        // Clock
        lblClock.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #9E8D84;");

        header.getChildren().addAll(titleBox, spacer, lblStatQueued, lblStatPreparing, lblStatDone, lblClock);
        return header;
    }

    private VBox buildCenterSection() {
        VBox center = new VBox(12);
        center.setPadding(new Insets(16, 24, 16, 24));

        // Filter & Search bar
        HBox controlBar = new HBox(12);
        controlBar.setAlignment(Pos.CENTER_LEFT);

        txtSearch.setPromptText("Tìm theo số bàn hoặc mã đơn (VD: 1, 1002)...");
        txtSearch.setStyle(
            "-fx-background-color: #281F1A; " +
            "-fx-background-radius: 16; " +
            "-fx-border-color: #42342D; " +
            "-fx-border-radius: 16; " +
            "-fx-border-width: 1; " +
            "-fx-text-fill: #FAF7F2; " +
            "-fx-prompt-text-fill: #7D6E68; " +
            "-fx-font-size: 12.5px; " +
            "-fx-pref-width: 280; " +
            "-fx-padding: 6 12 6 12;"
        );

        filterButtonsBox.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnClearDone.setStyle(
            "-fx-background-color: #2D231E; " +
            "-fx-background-radius: 14; " +
            "-fx-border-color: #42342D; " +
            "-fx-border-radius: 14; " +
            "-fx-text-fill: #A89A92; " +
            "-fx-font-size: 12px; " +
            "-fx-padding: 6 12 6 12; " +
            "-fx-cursor: hand;"
        );

        controlBar.getChildren().addAll(txtSearch, filterButtonsBox, spacer, btnClearDone);

        // Tickets container
        ticketsFlowPane.setPadding(new Insets(8, 4, 16, 4));
        ticketsFlowPane.setAlignment(Pos.TOP_LEFT);

        emptyTicketsBox.setAlignment(Pos.CENTER);
        emptyTicketsBox.setPadding(new Insets(80, 20, 80, 20));
        Label emptyText = new Label("Hiện không có đơn hàng nào trong mục này.");
        emptyText.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #7D6E68;");
        emptyTicketsBox.getChildren().addAll(emptyText);

        StackPane contentStack = new StackPane(emptyTicketsBox, ticketsFlowPane);
        ticketsScrollPane.setContent(contentStack);
        ticketsScrollPane.getStyleClass().add("ticket-scroll-pane");
        ticketsScrollPane.setFitToWidth(true);
        VBox.setVgrow(ticketsScrollPane, Priority.ALWAYS);

        center.getChildren().addAll(controlBar, ticketsScrollPane);
        return center;
    }

    private void startClock() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss • EEE, dd/MM/yyyy");
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            lblClock.setText(sdf.format(new Date()));
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    public void renderFilters(List<String> filters, String activeFilter, Consumer<String> onFilterChange) {
        filterButtonsBox.getChildren().clear();
        for (String f : filters) {
            Button btn = new Button(f);
            if (f.equalsIgnoreCase(activeFilter)) {
                btn.getStyleClass().add("kds-filter-btn-active");
            } else {
                btn.getStyleClass().add("kds-filter-btn");
            }
            btn.setOnAction(e -> onFilterChange.accept(f));
            filterButtonsBox.getChildren().add(btn);
        }
    }

    public void renderOrders(List<Order> orders, BiConsumer<Order, String> onStatusChange) {
        ticketsFlowPane.getChildren().clear();
        boolean hasOrders = orders != null && !orders.isEmpty();
        emptyTicketsBox.setVisible(!hasOrders);
        emptyTicketsBox.setManaged(!hasOrders);
        ticketsFlowPane.setVisible(hasOrders);
        ticketsFlowPane.setManaged(hasOrders);

        if (hasOrders) {
            for (Order o : orders) {
                OrderTicketCard card = new OrderTicketCard(o, onStatusChange);
                ticketsFlowPane.getChildren().add(card);
            }
        }
    }

    public void updateStats(int queued, int preparing, int done) {
        lblStatQueued.setText("Đang chờ: " + queued);
        lblStatPreparing.setText("Đang pha: " + preparing);
        lblStatDone.setText("Đã xong: " + done);
    }

    public void setLatestLog(String log) {
        lblLatestLog.setText(log);
    }

    public TextField getSearchField() { return txtSearch; }
    public Button getClearDoneButton() { return btnClearDone; }
}
