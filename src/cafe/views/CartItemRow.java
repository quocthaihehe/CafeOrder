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
 * CartItemRow - Hàng món trong Giỏ hàng theo thiết kế Screen 5 (UI_mong_muon.png).
 * Bố cục:
 * [Ảnh nhỏ 44x44 bo góc]  |  [Tên món (Size) + Đơn giá]  |  [[-] 1 [+]  🗑️]
 */
public class CartItemRow extends HBox {
    private final OrderItem item;

    public CartItemRow(OrderItem item, BiConsumer<OrderItem, Integer> onQtyChange, Consumer<OrderItem> onDelete) {
        this.item = item;
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(10);
        setPadding(new Insets(10, 12, 10, 12));
        setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-background-radius: 8px; " +
            "-fx-border-color: #E6DFD4; " +
            "-fx-border-radius: 8px; " +
            "-fx-border-width: 1px;"
        );

        // 1. Ảnh thumbnail nhỏ 44x44
        MenuItem menuItem = MenuRepository.findById(item.getProductId());
        Image thumb = ImageManager.getProductImage(menuItem, 88, 88);
        ImageView ivThumb = new ImageView(thumb);
        ivThumb.setFitWidth(44);
        ivThumb.setFitHeight(44);
        ivThumb.setPreserveRatio(false);

        Rectangle clip = new Rectangle(44, 44);
        clip.setArcWidth(12);
        clip.setArcHeight(12);
        ivThumb.setClip(clip);

        StackPane thumbWrapper = new StackPane(ivThumb);
        thumbWrapper.setStyle("-fx-background-color: #F8F4F0; -fx-background-radius: 6px;");

        // 2. Thông tin món (Tên, Size, Đơn giá & Tùy chọn)
        VBox infoBox = new VBox(2);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        String displayName = item.getName();
        if (item.getSizeCode() != null && !item.getSizeCode().isEmpty()) {
            displayName += " (" + item.getSizeCode() + ")";
        }
        Label lblName = new Label(displayName);
        lblName.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #292722;");

        Label lblPrice = new Label(CurrencyFormatter.format(item.getSubtotal()));
        lblPrice.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #A66A42;");

        infoBox.getChildren().addAll(lblName, lblPrice);

        // Tùy chọn topping / đường đá thu nhỏ
        String customSummary = item.getCustomizationSummary();
        if (!customSummary.isEmpty()) {
            Label lblCustom = new Label(customSummary);
            lblCustom.setStyle("-fx-font-size: 10px; -fx-text-fill: #817B70;");
            lblCustom.setWrapText(true);
            infoBox.getChildren().add(lblCustom);
        }

        // 3. Bộ tăng giảm số lượng [-] Qty [+] & Nút Xóa
        HBox actionBox = new HBox(6);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        Button btnMinus = new Button("-");
        btnMinus.setStyle(
            "-fx-background-color: #F4F0E8; -fx-text-fill: #292722; -fx-font-size: 12px; -fx-font-weight: 700; " +
            "-fx-background-radius: 6px; -fx-border-color: #D8D1C3; -fx-border-radius: 6px; -fx-padding: 3 8 3 8; -fx-cursor: hand;"
        );
        btnMinus.setOnAction(e -> onQtyChange.accept(item, -1));

        Label lblQty = new Label(String.valueOf(item.getQty()));
        lblQty.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #292722; -fx-padding: 0 4 0 4;");

        Button btnPlus = new Button("+");
        btnPlus.setStyle(
            "-fx-background-color: #F4F0E8; -fx-text-fill: #292722; -fx-font-size: 12px; -fx-font-weight: 700; " +
            "-fx-background-radius: 6px; -fx-border-color: #D8D1C3; -fx-border-radius: 6px; -fx-padding: 3 8 3 8; -fx-cursor: hand;"
        );
        btnPlus.setOnAction(e -> onQtyChange.accept(item, 1));

        Button btnDelete = new Button("🗑");
        btnDelete.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #817B70; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 4 2 4;"
        );
        btnDelete.setOnMouseEntered(e -> btnDelete.setStyle("-fx-background-color: #FDEEEE; -fx-text-fill: #C62828; -fx-font-size: 13px; -fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 2 4 2 4;"));
        btnDelete.setOnMouseExited(e -> btnDelete.setStyle("-fx-background-color: transparent; -fx-text-fill: #817B70; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 2 4 2 4;"));
        btnDelete.setTooltip(new Tooltip("Xóa món này khỏi giỏ"));
        btnDelete.setOnAction(e -> onDelete.accept(item));

        actionBox.getChildren().addAll(btnMinus, lblQty, btnPlus, btnDelete);

        getChildren().addAll(thumbWrapper, infoBox, actionBox);
    }
}
