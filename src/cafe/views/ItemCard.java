package cafe.views;

import cafe.models.MenuItem;
import cafe.utils.AnimationHelper;
import cafe.utils.CurrencyFormatter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import java.util.function.Consumer;

/**
 * ItemCard (ProductCard) - Reusable component for displaying a menu item.
 * Refined per CafeOrder Animation & UI Plan:
 * - 12px corner radius, subtle hairline border #E2DBD0
 * - Micro-lift hover animation via AnimationHelper.applyCardHover
 * - Quick-Add (+) circular button with 90-degree rotation animation
 * - 3 clean essential elements: Cover Image -> Product Name -> Price VND
 */
public class ItemCard extends VBox {
    private final MenuItem item;
    private final Consumer<MenuItem> onSelect;
    private final Consumer<MenuItem> onQuickAdd;

    public ItemCard(MenuItem item, Consumer<MenuItem> onSelect) {
        this(item, onSelect, null);
    }

    public ItemCard(MenuItem item, Consumer<MenuItem> onSelect, Consumer<MenuItem> onQuickAdd) {
        this.item = item;
        this.onSelect = onSelect;
        this.onQuickAdd = onQuickAdd;

        initUI();
    }

    private void initUI() {
        getStyleClass().add("product-card");
        setSpacing(6);
        setPadding(new Insets(8));
        setMinWidth(170);
        setMaxWidth(Double.MAX_VALUE);

        // Micro-lift card hover animation
        AnimationHelper.applyCardHover(this);

        // 1. Product Image with 12px corner radius
        StackPane imageWrapper = new StackPane();
        imageWrapper.setMinWidth(0);
        imageWrapper.setPrefWidth(Region.USE_COMPUTED_SIZE);
        imageWrapper.setMaxWidth(Double.MAX_VALUE);
        imageWrapper.setMinHeight(135);
        imageWrapper.setPrefHeight(135);
        imageWrapper.setMaxHeight(135);

        ProductImagePane imagePane = new ProductImagePane(item, 135, 12);
        imageWrapper.getChildren().add(imagePane);

        // Bestseller / New badges
        HBox topBadges = new HBox(4);
        topBadges.setAlignment(Pos.CENTER_LEFT);
        if (item.isBestseller()) {
            Label badgeBest = new Label("BESTSELLER");
            badgeBest.getStyleClass().add("badge-bestseller");
            topBadges.getChildren().add(badgeBest);
        } else if (item.isNew()) {
            Label badgeNew = new Label("MỚI");
            badgeNew.getStyleClass().add("badge-new");
            topBadges.getChildren().add(badgeNew);
        }

        if (!topBadges.getChildren().isEmpty()) {
            StackPane.setAlignment(topBadges, Pos.TOP_LEFT);
            topBadges.setPadding(new Insets(6, 0, 0, 6));
            imageWrapper.getChildren().add(topBadges);
        }

        // Circular (+) Quick-Add Button
        Button btnAddCircle = new Button("+");
        btnAddCircle.getStyleClass().add("btn-circle-add");
        AnimationHelper.applyButtonPress(btnAddCircle);

        btnAddCircle.setOnAction(e -> {
            e.consume();
            AnimationHelper.rotateQuickAdd(btnAddCircle);
            if (onQuickAdd != null) {
                onQuickAdd.accept(item);
            } else if (onSelect != null) {
                onSelect.accept(item);
            }
        });

        StackPane.setAlignment(btnAddCircle, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(btnAddCircle, new Insets(0, 8, 8, 0));
        imageWrapper.getChildren().add(btnAddCircle);

        // 2. Product Name
        Label lblName = new Label(item.getName());
        lblName.getStyleClass().add("product-card-title");
        lblName.setWrapText(true);
        lblName.setMinHeight(34);
        lblName.setPrefHeight(34);
        lblName.setMaxHeight(34);

        // 3. Price
        Label lblPrice = new Label(CurrencyFormatter.format(item.getPrice()));
        lblPrice.getStyleClass().add("product-card-price");

        getChildren().addAll(imageWrapper, lblName, lblPrice);

        // Clicking card body opens detailed customization dialog
        setOnMouseClicked(e -> {
            if (onSelect != null) onSelect.accept(item);
        });
    }

    public MenuItem getItem() {
        return item;
    }
}
