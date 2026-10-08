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
 * ItemCard (ProductCard) - Thẻ sản phẩm chuẩn Screen 3 trong UI_mong_muon.png:
 * - 6px corner radius, hairline border #DED7CD, nền #FBF9F5
 * - Micro-lift hover animation
 * - Ảnh bo góc 5px chiếm ~67% chiều cao
 * - Quick-Add (+) nút tròn 20px #112823 đặt nổi ở góc dưới bên phải ảnh
 * - Tên món (#292722) và Giá tiền (#563521)
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
        setSpacing(4);
        setPadding(new Insets(6));
        setMinWidth(150);
        setMaxWidth(Double.MAX_VALUE);
        setStyle("-fx-background-color: #FBF9F5; -fx-background-radius: 6px; -fx-border-color: #DED7CD; -fx-border-radius: 6px; -fx-border-width: 1px; -fx-cursor: hand;");

        // Micro-lift card hover animation
        AnimationHelper.applyCardHover(this);

        // 1. Product Image with 5px corner radius
        StackPane imageWrapper = new StackPane();
        imageWrapper.setMinWidth(0);
        imageWrapper.setPrefWidth(Region.USE_COMPUTED_SIZE);
        imageWrapper.setMaxWidth(Double.MAX_VALUE);
        imageWrapper.setMinHeight(125);
        imageWrapper.setPrefHeight(125);
        imageWrapper.setMaxHeight(125);

        ProductImagePane imagePane = new ProductImagePane(item, 125, 5);
        imageWrapper.getChildren().add(imagePane);

        // Circular (+) Quick-Add Button (đường kính 20px, màu #112823)
        Button btnAddCircle = new Button("+");
        btnAddCircle.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-size: 11px; -fx-font-weight: 700; -fx-background-radius: 10px; -fx-min-width: 20px; -fx-min-height: 20px; -fx-max-width: 20px; -fx-max-height: 20px; -fx-padding: 0; -fx-cursor: hand;");
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
        StackPane.setMargin(btnAddCircle, new Insets(0, 4, 4, 0));
        imageWrapper.getChildren().add(btnAddCircle);

        // 2. Product Name
        Label lblName = new Label(item.getName());
        lblName.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        lblName.setWrapText(true);
        lblName.setMinHeight(26);
        lblName.setPrefHeight(26);

        // 3. Price
        Label lblPrice = new Label(CurrencyFormatter.format(item.getPrice()));
        lblPrice.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 700; -fx-text-fill: #563521;");

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
