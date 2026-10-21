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
    private final Consumer<String> toastNotifier;

    public ItemCard(MenuItem item, Consumer<MenuItem> onSelect) {
        this(item, onSelect, null, null);
    }

    public ItemCard(MenuItem item, Consumer<MenuItem> onSelect, Consumer<MenuItem> onQuickAdd) {
        this(item, onSelect, onQuickAdd, null);
    }

    public ItemCard(MenuItem item, Consumer<MenuItem> onSelect, Consumer<MenuItem> onQuickAdd, Consumer<String> toastNotifier) {
        this.item = item;
        this.onSelect = onSelect;
        this.onQuickAdd = onQuickAdd;
        this.toastNotifier = toastNotifier;

        initUI();
    }

    public void updateStockStatus(boolean inStock) {
        item.setInStock(inStock);
        getChildren().clear();
        initUI();
    }

    private void initUI() {
        getStyleClass().add("product-card");
        setSpacing(4);
        setPadding(new Insets(6));
        setMinWidth(150);
        setMaxWidth(Double.MAX_VALUE);

        boolean available = item.isInStock();

        if (!available) {
            setStyle(
                "-fx-background-color: #F7F5F2; " +
                "-fx-background-radius: 8px; " +
                "-fx-border-color: #D1D5DB; " +
                "-fx-border-radius: 8px; " +
                "-fx-border-width: 1.5px; " +
                "-fx-cursor: hand;"
            );
            setOpacity(0.65);
        } else if (item.isBestseller()) {
            setStyle(
                "-fx-background-color: #FFFDF9; " +
                "-fx-background-radius: 8px; " +
                "-fx-border-color: linear-gradient(to bottom right, #EF4444 0%, #F59E0B 50%, #EF4444 100%); " +
                "-fx-border-radius: 8px; " +
                "-fx-border-width: 2px; " +
                "-fx-effect: dropshadow(gaussian, rgba(239, 68, 68, 0.32), 12, 0.22, 0, 3); " +
                "-fx-cursor: hand;"
            );
            setOpacity(1.0);
        } else {
            setStyle("-fx-background-color: #FBF9F5; -fx-background-radius: 6px; -fx-border-color: #DED7CD; -fx-border-radius: 6px; -fx-border-width: 1px; -fx-cursor: hand;");
            setOpacity(1.0);
        }

        // Micro-lift card hover animation
        if (available) {
            AnimationHelper.applyCardHover(this);
        }

        // 1. Product Image with 8px corner radius
        StackPane imageWrapper = new StackPane();
        imageWrapper.setMinWidth(0);
        imageWrapper.setPrefWidth(Region.USE_COMPUTED_SIZE);
        imageWrapper.setMaxWidth(Double.MAX_VALUE);
        imageWrapper.setMinHeight(175);
        imageWrapper.setPrefHeight(175);
        imageWrapper.setMaxHeight(175);

        ProductImagePane imagePane = new ProductImagePane(item, 175, 8);
        imageWrapper.getChildren().add(imagePane);

        if (!available) {
            Label badgeSoldOut = new Label("🚫 TẠM HẾT");
            badgeSoldOut.setStyle(
                "-fx-background-color: #4B5563; " +
                "-fx-text-fill: #FFFFFF; " +
                "-fx-font-family: 'Inter', sans-serif; " +
                "-fx-font-size: 10px; " +
                "-fx-font-weight: 800; " +
                "-fx-padding: 3 8 3 8; " +
                "-fx-background-radius: 6px;"
            );
            StackPane.setAlignment(badgeSoldOut, Pos.TOP_RIGHT);
            StackPane.setMargin(badgeSoldOut, new Insets(6, 6, 0, 0));
            imageWrapper.getChildren().add(badgeSoldOut);
        } else if (item.isBestseller()) {
            Label badgeHot = new Label("🔥 BÁN CHẠY");
            badgeHot.setStyle(
                "-fx-background-color: linear-gradient(to right, #DC2626, #EA580C); " +
                "-fx-text-fill: #FFFFFF; " +
                "-fx-font-family: 'Segoe UI', 'Inter', sans-serif; " +
                "-fx-font-size: 10px; " +
                "-fx-font-weight: 900; " +
                "-fx-padding: 3 8 3 8; " +
                "-fx-background-radius: 6px; " +
                "-fx-effect: dropshadow(gaussian, rgba(220, 38, 38, 0.45), 6, 0.3, 0, 2);"
            );
            StackPane.setAlignment(badgeHot, Pos.TOP_LEFT);
            StackPane.setMargin(badgeHot, new Insets(6, 0, 0, 6));
            imageWrapper.getChildren().add(badgeHot);

            javafx.animation.ScaleTransition pulse = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(900), badgeHot);
            pulse.setFromX(1.0);
            pulse.setFromY(1.0);
            pulse.setToX(1.07);
            pulse.setToY(1.07);
            pulse.setAutoReverse(true);
            pulse.setCycleCount(javafx.animation.Animation.INDEFINITE);
            pulse.play();
        }

        // 2. Product Name (13px Bold)
        Label lblName = new Label(item.getName());
        lblName.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: " + (available ? "#112823" : "#6B7280") + ";");
        lblName.setWrapText(true);
        lblName.setMinHeight(34);
        lblName.setPrefHeight(34);

        // 3. Bottom Row: Price / Out-of-Stock notice + [+ Thêm] Button
        HBox bottomRow = new HBox(6);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        Label lblPrice = new Label(available ? CurrencyFormatter.format(item.getPrice()) : "Tạm ngưng");
        lblPrice.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: " + (available ? "#563521" : "#6B7280") + ";");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        bottomRow.getChildren().addAll(lblPrice, sp);

        if (available) {
            Button btnAdd = new Button("+ Thêm");
            btnAdd.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-size: 11.5px; -fx-font-weight: 700; -fx-background-radius: 6px; -fx-padding: 5 10 5 10; -fx-cursor: hand;");
            AnimationHelper.applyButtonPress(btnAdd);
            btnAdd.setOnAction(e -> {
                e.consume();
                if (onQuickAdd != null) {
                    onQuickAdd.accept(item);
                } else if (onSelect != null) {
                    onSelect.accept(item);
                }
            });
            bottomRow.getChildren().add(btnAdd);
        }

        getChildren().addAll(imageWrapper, lblName, bottomRow);

        // Clicking card body opens detailed customization dialog if in stock, else shows toast warning
        setOnMouseClicked(e -> {
            if (available) {
                if (onSelect != null) onSelect.accept(item);
            } else {
                String msg = "Món '" + item.getName() + "' hiện đang tạm hết nguyên liệu. Quý khách vui lòng chọn món thơm ngon khác nhé!";
                if (toastNotifier != null) {
                    toastNotifier.accept(msg);
                }
            }
        });
    }

    public MenuItem getItem() {
        return item;
    }
}
