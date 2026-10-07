package cafe.views;

import cafe.models.MessageProtocol;
import cafe.models.Order;
import cafe.models.OrderItem;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import java.util.function.BiConsumer;

public class OrderTicketCard extends VBox {
    private final Order order;
    private final BiConsumer<Order, String> onStatusChange;

    public OrderTicketCard(Order order, BiConsumer<Order, String> onStatusChange) {
        this.order = order;
        this.onStatusChange = onStatusChange;

        initUI();
    }

    private void initUI() {
        getStyleClass().add("ticket-card");
        setSpacing(10);

        // Header: Table + ID + Status
        HBox header = new HBox(8);
        header.getStyleClass().add("ticket-header");
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label lblTable = new Label("BÀN " + (order.getTable() < 10 ? "0" + order.getTable() : order.getTable()));
        lblTable.getStyleClass().add("ticket-table-name");

        HBox metaRow = new HBox(6);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        Label lblId = new Label("#" + order.getOrderId());
        lblId.getStyleClass().add("ticket-order-id");

        Label lblTime = new Label("• " + order.getFormattedTime());
        lblTime.getStyleClass().add("ticket-time");
        metaRow.getChildren().addAll(lblId, lblTime);

        titleBox.getChildren().addAll(lblTable, metaRow);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        // Status Badge
        Label lblStatus = new Label();
        lblStatus.getStyleClass().add("ticket-status-badge");
        applyStatusBadgeStyle(lblStatus, order.getStatus());

        header.getChildren().addAll(titleBox, lblStatus);
        getChildren().add(header);

        // Items list
        VBox itemsBox = new VBox(8);
        itemsBox.setPadding(new Insets(4, 0, 8, 0));

        if (order.getItems() != null) {
            for (OrderItem oi : order.getItems()) {
                VBox itemRow = new VBox(3);
                itemRow.getStyleClass().add("ticket-item-row");

                HBox mainLine = new HBox(8);
                mainLine.setAlignment(Pos.CENTER_LEFT);

                Label lblQty = new Label(oi.getQty() + "x");
                lblQty.getStyleClass().add("ticket-item-qty");

                Label lblName = new Label(oi.getName());
                lblName.getStyleClass().add("ticket-item-name");
                lblName.setWrapText(true);
                HBox.setHgrow(lblName, Priority.ALWAYS);

                mainLine.getChildren().addAll(lblQty, lblName);
                itemRow.getChildren().add(mainLine);

                if (oi.getNote() != null && !oi.getNote().trim().isEmpty()) {
                    Label lblNote = new Label("📝 " + oi.getNote().trim());
                    lblNote.getStyleClass().add("ticket-item-note");
                    lblNote.setWrapText(true);
                    itemRow.getChildren().add(lblNote);
                }

                itemsBox.getChildren().add(itemRow);
            }
        }
        getChildren().add(itemsBox);

        // Spacer to push actions to bottom
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        getChildren().add(spacer);

        // Actions
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        String st = order.getStatus();
        if ("QUEUED".equalsIgnoreCase(st)) {
            Button btnPrepare = new Button("▶ PHA CHẾ");
            btnPrepare.getStyleClass().add("btn-kds-prepare");
            btnPrepare.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(btnPrepare, Priority.ALWAYS);
            btnPrepare.setOnAction(e -> onStatusChange.accept(order, "PREPARING"));

            Button btnDone = new Button("✓ XONG");
            btnDone.getStyleClass().add("btn-kds-done");
            btnDone.setOnAction(e -> onStatusChange.accept(order, "DONE"));

            actions.getChildren().addAll(btnPrepare, btnDone);
            getChildren().add(actions);
        } else if ("PREPARING".equalsIgnoreCase(st)) {
            Button btnDone = new Button("✓ HOÀN TẤT & GỬI BÀN");
            btnDone.getStyleClass().add("btn-kds-done");
            btnDone.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(btnDone, Priority.ALWAYS);
            btnDone.setOnAction(e -> onStatusChange.accept(order, "DONE"));

            actions.getChildren().add(btnDone);
            getChildren().add(actions);
        } else {
            Label lblCompleted = new Label("✓ Đã hoàn tất phục vụ");
            lblCompleted.setStyle("-fx-text-fill: #4ADE80; -fx-font-weight: 700; -fx-font-size: 12px;");
            actions.getChildren().add(lblCompleted);
            getChildren().add(actions);
        }
    }

    private void applyStatusBadgeStyle(Label label, String rawStatus) {
        label.getStyleClass().removeAll("badge-queued", "badge-preparing", "badge-done");
        if ("QUEUED".equalsIgnoreCase(rawStatus)) {
            label.setText("ĐANG CHỜ");
            label.getStyleClass().add("badge-queued");
        } else if ("PREPARING".equalsIgnoreCase(rawStatus)) {
            label.setText("ĐANG PHA CHẾ");
            label.getStyleClass().add("badge-preparing");
        } else if ("DONE".equalsIgnoreCase(rawStatus)) {
            label.setText("ĐÃ XONG");
            label.getStyleClass().add("badge-done");
        } else {
            label.setText(MessageProtocol.translateStatus(rawStatus));
        }
    }
}
