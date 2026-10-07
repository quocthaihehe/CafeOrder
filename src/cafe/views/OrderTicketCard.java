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

        // Header: Order Type + ID + Time + Status
        HBox header = new HBox(8);
        header.getStyleClass().add("ticket-header");
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(3);

        HBox topTitleRow = new HBox(6);
        topTitleRow.setAlignment(Pos.CENTER_LEFT);

        // Order Type Label
        Label lblType = new Label(order.getOrderTypeLabel());
        if ("TAKEAWAY".equalsIgnoreCase(order.getOrderType())) {
            lblType.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #E65100; -fx-font-weight: 800; -fx-font-size: 11px; -fx-padding: 2 8 2 8; -fx-background-radius: 8;");
        } else {
            lblType.setStyle("-fx-background-color: #EFEBE9; -fx-text-fill: #3E2723; -fx-font-weight: 800; -fx-font-size: 11px; -fx-padding: 2 8 2 8; -fx-background-radius: 8;");
        }

        Label lblId = new Label("#" + order.getOrderId());
        lblId.getStyleClass().add("ticket-order-id");
        topTitleRow.getChildren().addAll(lblType, lblId);

        // Elapsed time calculation
        long elapsedMillis = System.currentTimeMillis() - order.getTimestamp();
        long elapsedMinutes = Math.max(0, elapsedMillis / 60000);

        Label lblTime = new Label(order.getFormattedTime() + " (" + elapsedMinutes + "p trước)");
        if (elapsedMinutes >= 15) {
            lblTime.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 800; -fx-text-fill: #D32F2F;");
        } else if (elapsedMinutes >= 10) {
            lblTime.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 800; -fx-text-fill: #F57C00;");
        } else {
            lblTime.getStyleClass().add("ticket-time");
        }

        titleBox.getChildren().addAll(topTitleRow, lblTime);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        // Status Badge
        Label lblStatus = new Label();
        lblStatus.getStyleClass().add("ticket-status-badge");
        applyStatusBadgeStyle(lblStatus, order.getStatus());

        header.getChildren().addAll(titleBox, lblStatus);
        getChildren().add(header);

        // Items list with full customization
        VBox itemsBox = new VBox(8);
        itemsBox.setPadding(new Insets(4, 0, 8, 0));

        if (order.getItems() != null) {
            for (OrderItem oi : order.getItems()) {
                VBox itemRow = new VBox(2);
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

                // Customization specs line (Size, Ice, Sugar, Toppings)
                String custom = oi.getCustomizationSummary();
                if (!custom.isEmpty()) {
                    Label lblCustom = new Label(custom);
                    lblCustom.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #A0522D;");
                    lblCustom.setWrapText(true);
                    itemRow.getChildren().add(lblCustom);
                }

                if (oi.getNote() != null && !oi.getNote().trim().isEmpty()) {
                    Label lblNote = new Label("Ghi chú: " + oi.getNote().trim());
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
            Button btnPrepare = new Button("BẮT ĐẦU PHA");
            btnPrepare.getStyleClass().add("btn-kds-prepare");
            btnPrepare.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(btnPrepare, Priority.ALWAYS);
            btnPrepare.setOnAction(e -> onStatusChange.accept(order, "PREPARING"));

            Button btnDone = new Button("XONG");
            btnDone.getStyleClass().add("btn-kds-done");
            btnDone.setOnAction(e -> onStatusChange.accept(order, "DONE"));

            actions.getChildren().addAll(btnPrepare, btnDone);
            getChildren().add(actions);
        } else if ("PREPARING".equalsIgnoreCase(st)) {
            Button btnDone = new Button("HOÀN TẤT & PHỤC VỤ");
            btnDone.getStyleClass().add("btn-kds-done");
            btnDone.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(btnDone, Priority.ALWAYS);
            btnDone.setOnAction(e -> onStatusChange.accept(order, "DONE"));

            actions.getChildren().add(btnDone);
            getChildren().add(actions);
        } else if ("DONE".equalsIgnoreCase(st)) {
            Label lblDone = new Label("Đã hoàn tất phục vụ");
            lblDone.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #2E7D32; -fx-padding: 6 0 2 0;");
            actions.getChildren().add(lblDone);
            getChildren().add(actions);
        }
    }

    private void applyStatusBadgeStyle(Label lbl, String status) {
        if (status == null) status = "QUEUED";
        lbl.setText(MessageProtocol.translateStatus(status));

        switch (status.toUpperCase()) {
            case "QUEUED":
                lbl.setStyle("-fx-background-color: #FFF4E5; -fx-text-fill: #B76E00; -fx-font-weight: 700; -fx-font-size: 11px; -fx-padding: 3 8 3 8; -fx-background-radius: 8;");
                break;
            case "PREPARING":
                lbl.setStyle("-fx-background-color: #E3F2FD; -fx-text-fill: #1565C0; -fx-font-weight: 700; -fx-font-size: 11px; -fx-padding: 3 8 3 8; -fx-background-radius: 8;");
                break;
            case "DONE":
                lbl.setStyle("-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32; -fx-font-weight: 700; -fx-font-size: 11px; -fx-padding: 3 8 3 8; -fx-background-radius: 8;");
                break;
            default:
                lbl.setStyle("-fx-background-color: #EEEEEE; -fx-text-fill: #616161; -fx-font-weight: 700; -fx-font-size: 11px; -fx-padding: 3 8 3 8; -fx-background-radius: 8;");
                break;
        }
    }
}
