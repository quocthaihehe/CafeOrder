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
        setSpacing(5);

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

        // Customization line (Size, Ice, Sugar, Toppings)
        String customSummary = item.getCustomizationSummary();
        if (!customSummary.isEmpty()) {
            Label lblCustom = new Label(customSummary);
            lblCustom.setStyle("-fx-font-size: 11px; -fx-text-fill: #947E74; -fx-font-style: italic;");
            lblCustom.setWrapText(true);
            getChildren().add(lblCustom);
        }

        // Stepper row
        HBox bottomRow = new HBox(8);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        Label lblUnit = new Label(CurrencyFormatter.format(item.getUnitPrice()) + "/món");
        lblUnit.setStyle("-fx-font-size: 10.5px; -fx-text-fill: #A89A92;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnMinus = new Button("-");
        btnMinus.getStyleClass().add("stepper-btn");
        btnMinus.setOnAction(e -> onQtyChange.accept(item, -1));

        Label lblQty = new Label(String.valueOf(item.getQty()));
        lblQty.getStyleClass().add("stepper-qty");

        Button btnPlus = new Button("+");
        btnPlus.getStyleClass().add("stepper-btn");
        btnPlus.setOnAction(e -> onQtyChange.accept(item, 1));

        Button btnDelete = new Button("X");
        btnDelete.getStyleClass().add("cart-delete-btn");
        btnDelete.setTooltip(new javafx.scene.control.Tooltip("Xóa món này"));
        btnDelete.setOnAction(e -> onDelete.accept(item));

        bottomRow.getChildren().addAll(lblUnit, spacer, btnMinus, lblQty, btnPlus, btnDelete);
        getChildren().add(bottomRow);
    }
}
