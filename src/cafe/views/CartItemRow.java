package cafe.views;

import cafe.models.MenuItem;
import cafe.models.MenuRepository;
import cafe.models.OrderItem;
import cafe.utils.CurrencyFormatter;
import cafe.utils.ImageManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * CartItemRow - Reusable Cart Item component.
 * Hallmark Discipline:
 * - Subtle 4px radius, hairline borders
 * - Consistent editorial typography and price hierarchy
 * - Tactile stepper [-] Qty [+] and delete action
 */
public class CartItemRow extends HBox {
    private final OrderItem item;

    public CartItemRow(OrderItem item, BiConsumer<OrderItem, Integer> onQtyChange, Consumer<OrderItem> onDelete) {
        this.item = item;
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(10);
        setPadding(new Insets(8, 10, 8, 10));
        getStyleClass().add("cart-item-row");

        // 1. Thumbnail 44x44 with 4px subtle rounded corners
        MenuItem menuItem = MenuRepository.findById(item.getProductId());
        Image thumb = ImageManager.getProductImage(menuItem, 88, 88);
        ImageView ivThumb = new ImageView(thumb);
        ivThumb.setFitWidth(44);
        ivThumb.setFitHeight(44);
        ivThumb.setPreserveRatio(false);

        Rectangle clip = new Rectangle(44, 44);
        clip.setArcWidth(8);
        clip.setArcHeight(8);
        ivThumb.setClip(clip);

        StackPane thumbWrapper = new StackPane(ivThumb);
        thumbWrapper.setStyle("-fx-background-color: #F8F4F0; -fx-background-radius: 4px;");

        // 2. Info Box (Name, Size, Price, Customization)
        VBox infoBox = new VBox(2);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        String displayName = item.getName();
        if (item.getSizeCode() != null && !item.getSizeCode().isEmpty()) {
            displayName += " (" + item.getSizeCode() + ")";
        }
        Label lblName = new Label(displayName);
        lblName.getStyleClass().add("cart-item-title");

        Label lblPrice = new Label(CurrencyFormatter.format(item.getSubtotal()));
        lblPrice.getStyleClass().add("cart-item-price");

        infoBox.getChildren().addAll(lblName, lblPrice);

        String customSummary = item.getCustomizationSummary();
        if (!customSummary.isEmpty()) {
            Label lblCustom = new Label(customSummary);
            lblCustom.setStyle("-fx-font-size: 10px; -fx-text-fill: #817B70;");
            lblCustom.setWrapText(true);
            infoBox.getChildren().add(lblCustom);
        }

        // 3. Stepper [-] Qty [+] and Delete Button
        HBox actionBox = new HBox(4);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        Button btnMinus = new Button("-");
        btnMinus.getStyleClass().add("stepper-btn");
        btnMinus.setOnAction(e -> onQtyChange.accept(item, -1));

        Label lblQty = new Label(String.valueOf(item.getQty()));
        lblQty.getStyleClass().add("stepper-value");

        Button btnPlus = new Button("+");
        btnPlus.getStyleClass().add("stepper-btn");
        btnPlus.setOnAction(e -> onQtyChange.accept(item, 1));

        Button btnDelete = new Button("✕");
        btnDelete.getStyleClass().add("btn-delete-item");
        btnDelete.setTooltip(new Tooltip("Xóa món khỏi giỏ hàng"));
        btnDelete.setOnAction(e -> onDelete.accept(item));

        actionBox.getChildren().addAll(btnMinus, lblQty, btnPlus, btnDelete);

        getChildren().addAll(thumbWrapper, infoBox, actionBox);
    }

    public OrderItem getItem() {
        return item;
    }
}
