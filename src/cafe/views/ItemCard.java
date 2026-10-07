package cafe.views;

import cafe.models.MenuItem;
import cafe.utils.CurrencyFormatter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
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
        setSpacing(8);
        setMinWidth(180);
        setMaxWidth(Double.MAX_VALUE);

        // Top Badges Row (Bestseller / Mới)
        HBox topBadges = new HBox(6);
        topBadges.setAlignment(Pos.CENTER_LEFT);

        if (item.isBestseller()) {
            Label badgeBest = new Label("BESTSELLER");
            badgeBest.setStyle("-fx-background-color: #E65100; -fx-text-fill: white; -fx-font-size: 9.5px; -fx-font-weight: 800; -fx-background-radius: 6; -fx-padding: 3 8 3 8;");
            topBadges.getChildren().add(badgeBest);
        } else if (item.isNew()) {
            Label badgeNew = new Label("MỚI");
            badgeNew.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-size: 9.5px; -fx-font-weight: 800; -fx-background-radius: 6; -fx-padding: 3 8 3 8;");
            topBadges.getChildren().add(badgeNew);
        }

        // Sub-badges row (Size S/M/L, Nóng / Đá)
        HBox optionsHint = new HBox(6);
        optionsHint.setAlignment(Pos.CENTER_LEFT);
        if (item.isAllowSize()) {
            Label lblSizeHint = new Label("Size S / M / L");
            lblSizeHint.setStyle("-fx-background-color: #F1ECE6; -fx-text-fill: #7D6E68; -fx-font-size: 9.5px; -fx-font-weight: 700; -fx-background-radius: 4; -fx-padding: 2 6 2 6;");
            optionsHint.getChildren().add(lblSizeHint);
        }
        if (item.isHotAvailable()) {
            Label lblHotHint = new Label("Nóng / Đá");
            lblHotHint.setStyle("-fx-background-color: #FBE9E7; -fx-text-fill: #D84315; -fx-font-size: 9.5px; -fx-font-weight: 700; -fx-background-radius: 4; -fx-padding: 2 6 2 6;");
            optionsHint.getChildren().add(lblHotHint);
        }

        // Category Tag
        Label lblCategory = new Label(item.getCategory().toUpperCase());
        lblCategory.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #A89A92; -fx-letter-spacing: 0.5px;");

        // Product Name
        Label lblName = new Label(item.getName());
        lblName.getStyleClass().add("item-card-title");
        lblName.setWrapText(true);
        lblName.setMinHeight(42);
        lblName.setPrefHeight(42);
        lblName.setMaxHeight(42);

        // Description
        Label lblDesc = new Label(item.getDescription());
        lblDesc.getStyleClass().add("item-card-desc");
        lblDesc.setWrapText(true);
        lblDesc.setMinHeight(46);
        lblDesc.setPrefHeight(46);
        lblDesc.setMaxHeight(46);

        // Bottom row: Price + Select Button
        HBox bottomRow = new HBox(8);
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        bottomRow.setPadding(new Insets(6, 0, 0, 0));

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

        // Image Container with smooth rounded corners
        StackPane imageContainer = new StackPane();
        imageContainer.getStyleClass().add("item-image-container");
        imageContainer.setPrefHeight(135);
        imageContainer.setMinHeight(135);
        imageContainer.setMaxHeight(135);

        ImageView imgView = cafe.utils.ImageManager.createProductImageView(item, 220, 135, 12);
        imgView.fitWidthProperty().bind(imageContainer.widthProperty());
        
        Rectangle clip = new Rectangle();
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        clip.widthProperty().bind(imageContainer.widthProperty());
        clip.heightProperty().bind(imageContainer.heightProperty());
        imageContainer.setClip(clip);

        imageContainer.getChildren().add(imgView);

        // Position badges over top-left of image
        if (!topBadges.getChildren().isEmpty()) {
            StackPane.setAlignment(topBadges, Pos.TOP_LEFT);
            topBadges.setPadding(new Insets(8, 0, 0, 8));
            imageContainer.getChildren().add(topBadges);
        }

        // Assemble card
        getChildren().addAll(imageContainer, lblCategory, lblName, optionsHint, lblDesc, bottomRow);


        setOnMouseClicked(e -> {
            if (onSelect != null) onSelect.accept(item);
        });
    }
}
