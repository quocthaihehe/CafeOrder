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
 * ItemCard - Thẻ món ăn / đồ uống theo chuẩn thiết kế Screen 3 (UI_mong_muon.png).
 * Phong cách tối giản, thanh lịch:
 * - Ảnh bo góc tròn 8px với tỷ lệ Cover chuẩn xác.
 * - Nút tròn (+) thêm nhanh màu Dark Emerald (#183C32) ở góc phải dưới ảnh.
 * - Tên món, đơn giá màu Caramel, phân cấp thị giác rõ ràng.
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
        setSpacing(6);
        setPadding(new Insets(10));
        setMinWidth(180);
        setMaxWidth(Double.MAX_VALUE);
        setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-background-radius: 10px; " +
            "-fx-border-color: #E2DBD0; " +
            "-fx-border-radius: 10px; " +
            "-fx-border-width: 1px; " +
            "-fx-effect: dropshadow(gaussian, rgba(41, 39, 34, 0.04), 8, 0, 0, 2); " +
            "-fx-cursor: hand;"
        );

        setOnMouseEntered(e -> setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-background-radius: 10px; " +
            "-fx-border-color: #183C32; " +
            "-fx-border-radius: 10px; " +
            "-fx-border-width: 1px; " +
            "-fx-effect: dropshadow(gaussian, rgba(24, 60, 50, 0.12), 12, 0, 0, 4); " +
            "-fx-cursor: hand;"
        ));

        setOnMouseExited(e -> setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-background-radius: 10px; " +
            "-fx-border-color: #E2DBD0; " +
            "-fx-border-radius: 10px; " +
            "-fx-border-width: 1px; " +
            "-fx-effect: dropshadow(gaussian, rgba(41, 39, 34, 0.04), 8, 0, 0, 2); " +
            "-fx-cursor: hand;"
        ));

        // 1. Khung hình ảnh + Nút tròn (+) nổi ở góc phải
        StackPane imageWrapper = new StackPane();
        imageWrapper.setMinWidth(0);
        imageWrapper.setPrefWidth(Region.USE_COMPUTED_SIZE);
        imageWrapper.setMaxWidth(Double.MAX_VALUE);
        imageWrapper.setMinHeight(130);
        imageWrapper.setPrefHeight(130);
        imageWrapper.setMaxHeight(130);

        ProductImagePane imagePane = new ProductImagePane(item, 130, 8);
        imageWrapper.getChildren().add(imagePane);

        // Huy hiệu BESTSELLER / MỚI (nếu có)
        HBox topBadges = new HBox(6);
        topBadges.setAlignment(Pos.CENTER_LEFT);
        if (item.isBestseller()) {
            Label badgeBest = new Label("BESTSELLER");
            badgeBest.setStyle("-fx-background-color: #183C32; -fx-text-fill: white; -fx-font-size: 8.5px; -fx-font-weight: 800; -fx-background-radius: 4; -fx-padding: 2 6 2 6;");
            topBadges.getChildren().add(badgeBest);
        } else if (item.isNew()) {
            Label badgeNew = new Label("MỚI");
            badgeNew.setStyle("-fx-background-color: #A66A42; -fx-text-fill: white; -fx-font-size: 8.5px; -fx-font-weight: 800; -fx-background-radius: 4; -fx-padding: 2 6 2 6;");
            topBadges.getChildren().add(badgeNew);
        }

        if (!topBadges.getChildren().isEmpty()) {
            StackPane.setAlignment(topBadges, Pos.TOP_LEFT);
            topBadges.setPadding(new Insets(6, 0, 0, 6));
            imageWrapper.getChildren().add(topBadges);
        }

        // Nút tròn (+) theo mockup Screen 3
        Button btnAddCircle = new Button("+");
        btnAddCircle.setStyle(
            "-fx-background-color: #183C32; " +
            "-fx-text-fill: #FFFFFF; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: 700; " +
            "-fx-background-radius: 20px; " +
            "-fx-min-width: 28px; -fx-min-height: 28px; " +
            "-fx-max-width: 28px; -fx-max-height: 28px; " +
            "-fx-padding: 0; -fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 4, 0, 0, 1);"
        );
        btnAddCircle.setOnMouseEntered(e -> btnAddCircle.setStyle("-fx-background-color: #102C25; -fx-text-fill: #FFFFFF; -fx-font-size: 14px; -fx-font-weight: 700; -fx-background-radius: 20px; -fx-min-width: 28px; -fx-min-height: 28px; -fx-max-width: 28px; -fx-max-height: 28px; -fx-padding: 0; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 6, 0, 0, 2);"));
        btnAddCircle.setOnMouseExited(e -> btnAddCircle.setStyle("-fx-background-color: #183C32; -fx-text-fill: #FFFFFF; -fx-font-size: 14px; -fx-font-weight: 700; -fx-background-radius: 20px; -fx-min-width: 28px; -fx-min-height: 28px; -fx-max-width: 28px; -fx-max-height: 28px; -fx-padding: 0; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 4, 0, 0, 1);"));
        btnAddCircle.setOnAction(e -> {
            e.consume();
            if (onSelect != null) onSelect.accept(item);
        });

        StackPane.setAlignment(btnAddCircle, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(btnAddCircle, new Insets(0, 8, 8, 0));
        imageWrapper.getChildren().add(btnAddCircle);

        // 2. Tên sản phẩm
        Label lblName = new Label(item.getName());
        lblName.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #292722;");
        lblName.setWrapText(true);
        lblName.setMinHeight(36);
        lblName.setPrefHeight(36);
        lblName.setMaxHeight(36);

        // 3. Đơn vị giá tiền nổi bật
        Label lblPrice = new Label(CurrencyFormatter.format(item.getPrice()));
        lblPrice.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #A66A42;");

        // 4. Ghi chú tùy chọn thu nhỏ
        HBox optionsHint = new HBox(6);
        optionsHint.setAlignment(Pos.CENTER_LEFT);
        if (item.isAllowSize()) {
            Label lblSizeHint = new Label("Size S / M / L");
            lblSizeHint.setStyle("-fx-background-color: #F4F0E8; -fx-text-fill: #817B70; -fx-font-size: 9px; -fx-font-weight: 600; -fx-background-radius: 4; -fx-padding: 1 5 1 5;");
            optionsHint.getChildren().add(lblSizeHint);
        }
        if (item.isHotAvailable()) {
            Label lblHotHint = new Label("Nóng / Đá");
            lblHotHint.setStyle("-fx-background-color: #FDF4ED; -fx-text-fill: #C05621; -fx-font-size: 9px; -fx-font-weight: 600; -fx-background-radius: 4; -fx-padding: 1 5 1 5;");
            optionsHint.getChildren().add(lblHotHint);
        }

        getChildren().addAll(imageWrapper, lblName, lblPrice, optionsHint);

        setOnMouseClicked(e -> {
            if (onSelect != null) onSelect.accept(item);
        });
    }
}
