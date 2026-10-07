package cafe.views;

import cafe.models.MenuItem;
import cafe.utils.CurrencyFormatter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import java.util.function.Consumer;

public class ItemCard extends VBox {
    private final MenuItem item;
    private final Consumer<MenuItem> onSelect;

    public ItemCard(MenuItem item, Consumer<MenuItem> onSelect) {
        this.item = item;
        this.onSelect = onSelect;

        initUI();
    }

    private void initUI() {
        getStyleClass().add("item-card");
        setSpacing(10);
        setPrefWidth(220);
        setMaxWidth(260);

        // Glyph Box
        StackPane glyphBox = new StackPane();
        glyphBox.getStyleClass().add("item-glyph-box");
        glyphBox.setPrefHeight(95);

        Label glyph = new Label(item.getIconEmoji());
        glyph.getStyleClass().add("item-glyph");
        glyphBox.getChildren().add(glyph);

        // Title & Category
        Label lblCategory = new Label(item.getCategory().toUpperCase());
        lblCategory.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #A89A92; -fx-letter-spacing: 1px;");

        Label lblName = new Label(item.getName());
        lblName.getStyleClass().add("item-card-title");
        lblName.setWrapText(true);

        Label lblDesc = new Label(item.getDescription());
        lblDesc.getStyleClass().add("item-card-desc");
        lblDesc.setWrapText(true);
        lblDesc.setPrefHeight(38);

        // Bottom row: Price + Button
        HBox bottomRow = new HBox(8);
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        bottomRow.setPadding(new Insets(4, 0, 0, 0));

        Label lblPrice = new Label(CurrencyFormatter.format(item.getPrice()));
        lblPrice.getStyleClass().add("item-card-price");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAdd = new Button("+ Chọn");
        btnAdd.getStyleClass().add("btn-select-item");
        btnAdd.setOnAction(e -> {
            e.consume();
            if (onSelect != null) onSelect.accept(item);
        });

        bottomRow.getChildren().addAll(lblPrice, spacer, btnAdd);

        getChildren().addAll(glyphBox, lblCategory, lblName, lblDesc, bottomRow);

        // Click anywhere on card to select
        setOnMouseClicked(e -> {
            if (onSelect != null) onSelect.accept(item);
        });
    }
}
