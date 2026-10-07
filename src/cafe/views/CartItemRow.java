package cafe.views;

import cafe.models.OrderItem;
import cafe.utils.CurrencyFormatter;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CartItemRow extends VBox {
    private final OrderItem item;

    public CartItemRow(OrderItem item, BiConsumer<OrderItem, Integer> onQtyChange, Consumer<OrderItem> onDelete) {
        this.item = item;
        getStyleClass().add("cart-item-row");
        setSpacing(6);

        // Header: Name + Subtotal
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label lblName = new Label(item.getName());
        lblName.getStyleClass().add("cart-item-name");
        HBox.setHgrow(lblName, Priority.ALWAYS);

        Label lblPrice = new Label(CurrencyFormatter.format(item.getSubtotal()));
        lblPrice.getStyleClass().add("cart-item-price");

        topRow.getChildren().addAll(lblName, lblPrice);
        getChildren().add(topRow);

        // Note line (if present)
        if (item.getNote() != null && !item.getNote().trim().isEmpty()) {
            Label lblNote = new Label("📝 " + item.getNote().trim());
            lblNote.getStyleClass().add("cart-item-note");
            lblNote.setWrapText(true);
            getChildren().add(lblNote);
        }

        // Stepper row
        HBox bottomRow = new HBox(8);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        Button btnMinus = new Button("-");
        btnMinus.getStyleClass().add("stepper-btn");
        btnMinus.setOnAction(e -> onQtyChange.accept(item, -1));

        Label lblQty = new Label(String.valueOf(item.getQty()));
        lblQty.getStyleClass().add("stepper-qty");

        Button btnPlus = new Button("+");
        btnPlus.getStyleClass().add("stepper-btn");
        btnPlus.setOnAction(e -> onQtyChange.accept(item, 1));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnDelete = new Button("✕");
        btnDelete.getStyleClass().add("cart-delete-btn");
        btnDelete.setTooltip(new javafx.scene.control.Tooltip("Xóa món này"));
        btnDelete.setOnAction(e -> onDelete.accept(item));

        bottomRow.getChildren().addAll(btnMinus, lblQty, btnPlus, spacer, btnDelete);
        getChildren().add(bottomRow);
    }
}
