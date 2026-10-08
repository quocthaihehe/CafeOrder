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

/**
 * OrderTicketCard - Card phiếu đơn hàng trong quầy KDS.
 * Hallmark Discipline:
 * - Dễ scan nhanh từ xa (high-contrast status, table, item count)
 * - 4px radius, hairline borders #D8D1C3
 * - Action buttons tactile & distinct
 */
public class OrderTicketCard extends VBox {
    private final Order order;
    private final BiConsumer<Order, String> onStatusChange;
    private final Label lblTime = new Label();

    public OrderTicketCard(Order order, BiConsumer<Order, String> onStatusChange) {
        this.order = order;
        this.onStatusChange = onStatusChange;

        initUI();
    }

    public void updateElapsedTime() {
        long elapsedMillis = System.currentTimeMillis() - order.getTimestamp();
        long elapsedMinutes = Math.max(0, elapsedMillis / 60000);
        lblTime.setText(order.getFormattedTime() + " (" + elapsedMinutes + "p trước)");
        if (elapsedMinutes >= 15) {
            lblTime.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 800; -fx-text-fill: #D32F2F;");
        } else if (elapsedMinutes >= 10) {
            lblTime.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 800; -fx-text-fill: #F57C00;");
        } else {
            lblTime.getStyleClass().setAll("ticket-time");
            lblTime.setStyle("");
        }
    }

    private void initUI() {
        getStyleClass().add("ticket-card");
        setSpacing(8);

        // Header: Order Type + ID + Time + Status
        HBox header = new HBox(8);
        header.getStyleClass().add("ticket-header");
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);

        HBox topTitleRow = new HBox(6);
        topTitleRow.setAlignment(Pos.CENTER_LEFT);

        // Order Type Label
        Label lblType = new Label(order.getOrderTypeLabel());
        if ("TAKEAWAY".equalsIgnoreCase(order.getOrderType())) {
            lblType.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #E65100; -fx-font-weight: 800; -fx-font-size: 10px; -fx-padding: 2 6 2 6; -fx-background-radius: 4px;");
        } else {
            lblType.setStyle("-fx-background-color: #EFEBE9; -fx-text-fill: #3E2723; -fx-font-weight: 800; -fx-font-size: 10px; -fx-padding: 2 6 2 6; -fx-background-radius: 4px;");
        }

        Label lblId = new Label("#" + order.getOrderId() + " • BÀN " + order.getTable());
        lblId.getStyleClass().add("ticket-order-id");
        topTitleRow.getChildren().addAll(lblType, lblId);

        updateElapsedTime();

        titleBox.getChildren().addAll(topTitleRow, lblTime);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        // Status Badge
        Label lblStatus = new Label();
        lblStatus.getStyleClass().add("ticket-status-badge");
        applyStatusBadgeStyle(lblStatus, order.getStatus());

        header.getChildren().addAll(titleBox, lblStatus);
        getChildren().add(header);

        // Items list
        VBox itemsBox = new VBox(6);
        itemsBox.setPadding(new Insets(4, 0, 6, 0));

        if (order.getItems() != null) {
            for (OrderItem oi : order.getItems()) {
                VBox itemRow = new VBox(2);
                itemRow.getStyleClass().add("ticket-item-row");

                HBox mainLine = new HBox(8);
                mainLine.setAlignment(Pos.CENTER_LEFT);

                Label lblQty = new Label(oi.getQty() + "x");
                lblQty.getStyleClass().add("ticket-item-qty");

                String nameAndSize = oi.getName();
                if (oi.getSizeCode() != null && !oi.getSizeCode().isEmpty()) {
                    nameAndSize += " (" + oi.getSizeCode() + ")";
                }
                Label lblName = new Label(nameAndSize);
                lblName.getStyleClass().add("ticket-item-name");
                lblName.setWrapText(true);
                HBox.setHgrow(lblName, Priority.ALWAYS);

                mainLine.getChildren().addAll(lblQty, lblName);
                itemRow.getChildren().add(mainLine);

                // Customizations (Đá, Đường, Topping, Ghi chú)
                String custom = oi.getCustomizationSummary();
                if (!custom.isEmpty()) {
                    Label lblCustom = new Label(custom);
                    lblCustom.getStyleClass().add("ticket-item-custom");
                    lblCustom.setWrapText(true);
                    itemRow.getChildren().add(lblCustom);
                }

                if (oi.getNote() != null && !oi.getNote().trim().isEmpty()) {
                    Label lblNote = new Label("✎ " + oi.getNote().trim());
                    lblNote.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 700; -fx-text-fill: #A66A42; -fx-padding: 0 0 0 24;");
                    lblNote.setWrapText(true);
                    itemRow.getChildren().add(lblNote);
                }

                itemsBox.getChildren().add(itemRow);
            }
        }
        getChildren().add(itemsBox);

        // General Note / Payment Method (if any)
        if (order.getNote() != null && !order.getNote().trim().isEmpty()) {
            Label lblOrderNote = new Label("💳 " + order.getNote().trim());
            lblOrderNote.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #183C32; -fx-background-color: #F4F0E8; -fx-padding: 3 6 3 6; -fx-background-radius: 4px;");
            getChildren().add(lblOrderNote);
        }

        // Action Buttons Bar
        HBox actionsBar = new HBox(8);
        actionsBar.getStyleClass().add("ticket-actions-bar");
        actionsBar.setAlignment(Pos.CENTER_RIGHT);

        String st = order.getStatus() != null ? order.getStatus().toUpperCase() : "QUEUED";

        if ("QUEUED".equals(st)) {
            Button btnPrep = new Button("Bắt đầu pha chế");
            btnPrep.getStyleClass().add("btn-primary");
            btnPrep.setStyle("-fx-font-size: 11.5px; -fx-padding: 6 12 6 12;");
            btnPrep.setOnAction(e -> onStatusChange.accept(order, "PREPARING"));
            actionsBar.getChildren().add(btnPrep);
        } else if ("PREPARING".equals(st)) {
            Button btnDone = new Button("✓ Hoàn thành");
            btnDone.setStyle("-fx-background-color: #1B5E20; -fx-text-fill: #FFFFFF; -fx-font-weight: 700; -fx-font-size: 11.5px; -fx-background-radius: 4px; -fx-padding: 6 12 6 12; -fx-cursor: hand;");
            btnDone.setOnAction(e -> onStatusChange.accept(order, "DONE"));
            actionsBar.getChildren().add(btnDone);
        } else {
            Label lblFinished = new Label("Đã xong • Chờ giao");
            lblFinished.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #1B5E20;");
            actionsBar.getChildren().add(lblFinished);
        }

        getChildren().add(actionsBar);
    }

    private void applyStatusBadgeStyle(Label lbl, String status) {
        String st = status != null ? status.toUpperCase() : "QUEUED";
        switch (st) {
            case "QUEUED":
                lbl.setText("ĐANG CHỜ");
                lbl.setStyle("-fx-background-color: #FDF4ED; -fx-text-fill: #C05621; -fx-border-color: #F4D3BC; -fx-border-radius: 4px; -fx-background-radius: 4px;");
                break;
            case "PREPARING":
                lbl.setText("ĐANG PHA CHẾ");
                lbl.setStyle("-fx-background-color: #EBF8FF; -fx-text-fill: #2B6CB0; -fx-border-color: #BEE3F8; -fx-border-radius: 4px; -fx-background-radius: 4px;");
                break;
            case "DONE":
                lbl.setText("ĐÃ HOÀN THÀNH");
                lbl.setStyle("-fx-background-color: #EDF7ED; -fx-text-fill: #1B5E20; -fx-border-color: #C8E6C9; -fx-border-radius: 4px; -fx-background-radius: 4px;");
                break;
            default:
                lbl.setText(st);
                lbl.setStyle("-fx-background-color: #F5F5F5; -fx-text-fill: #616161; -fx-border-radius: 4px; -fx-background-radius: 4px;");
                break;
        }
    }
}
