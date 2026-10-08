package cafe.views;

import cafe.models.MenuItem;
import cafe.utils.CurrencyFormatter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import java.util.function.Consumer;

/**
 * ItemCard (ProductCard) - Reusable component for displaying a menu item.
 * Hallmark Discipline:
 * - Subtle 4px radius, hairline border #D8D1C3
 * - Clean editorial typography, no AI slop shadows
 * - Responsive cover image, Dark Emerald (+) quick-add action
 */
public class ItemCard extends VBox {
    private final MenuItem item;
    private final Consumer<MenuItem> onSelect;

    public ItemCard(MenuItem item, Consumer<MenuItem> onSelect) {
        this.item = item;
        this.onSelect = onSelect;

        initUI();
    }

    private void initUI() {
        getStyleClass().add("product-card");
        setSpacing(6);
        setPadding(new Insets(8));
        setMinWidth(170);
        setMaxWidth(Double.MAX_VALUE);

        // 1. Product Image with subtle 4px corner radius
        StackPane imageWrapper = new StackPane();
        imageWrapper.setMinWidth(0);
        imageWrapper.setPrefWidth(Region.USE_COMPUTED_SIZE);
        imageWrapper.setMaxWidth(Double.MAX_VALUE);
        imageWrapper.setMinHeight(125);
        imageWrapper.setPrefHeight(125);
        imageWrapper.setMaxHeight(125);

        ProductImagePane imagePane = new ProductImagePane(item, 125, 4);
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

        // Circular (+) Add Button
        Button btnAddCircle = new Button("+");
        btnAddCircle.getStyleClass().add("btn-circle-add");
        btnAddCircle.setOnAction(e -> {
            e.consume();
            if (onSelect != null) onSelect.accept(item);
        });

        StackPane.setAlignment(btnAddCircle, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(btnAddCircle, new Insets(0, 6, 6, 0));
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

        // 4. Subtle options hint
        HBox optionsHint = new HBox(4);
        optionsHint.setAlignment(Pos.CENTER_LEFT);
        if (item.isAllowSize()) {
            Label lblSizeHint = new Label("Size S / M / L");
            lblSizeHint.setStyle("-fx-background-color: #F4F0E8; -fx-text-fill: #817B70; -fx-font-size: 9px; -fx-font-weight: 600; -fx-background-radius: 4px; -fx-padding: 1 5 1 5;");
            optionsHint.getChildren().add(lblSizeHint);
        }
        if (item.isHotAvailable()) {
            Label lblHotHint = new Label("Nóng / Đá");
            lblHotHint.setStyle("-fx-background-color: #FDF4ED; -fx-text-fill: #A66A42; -fx-font-size: 9px; -fx-font-weight: 600; -fx-background-radius: 4px; -fx-padding: 1 5 1 5;");
            optionsHint.getChildren().add(lblHotHint);
        }

        getChildren().addAll(imageWrapper, lblName, lblPrice, optionsHint);

        setOnMouseClicked(e -> {
            if (onSelect != null) onSelect.accept(item);
        });
    }

    public MenuItem getItem() {
        return item;
    }
}
