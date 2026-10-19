package cafe.views;

import cafe.models.MenuItem;
import cafe.models.MenuRepository;
import cafe.models.OrderItem;
import cafe.utils.AnimationHelper;
import cafe.utils.CurrencyFormatter;
import cafe.utils.ImageManager;
import cafe.utils.SvgIcons;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * CartItemRow - Dòng món trong Giỏ hàng với thiết kế thoáng, tối giản viền (borderless inner controls)
 * và đồng nhất hệ thống chữ với form Thanh toán / Phiếu thanh toán:
 * - Chữ chính (Tên món, Giá, Số lượng): 13px Bold (#112823)
 * - Chữ phụ (Tùy chọn Size/Đường/Đá/Topping, nút Đổi tùy chọn): 12px Regular (#4A453E)
 */
public class CartItemRow extends HBox {

    private static final String STYLE_BOLD = "-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #112823;";
    private static final String STYLE_REGULAR = "-fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 400; -fx-text-fill: #4A453E;";

    private final OrderItem item;

    public CartItemRow(OrderItem item,
                       BiConsumer<OrderItem, Integer> onQtyChange,
                       Consumer<OrderItem> onDelete) {
        this(item, onQtyChange, onDelete, null);
    }

    public CartItemRow(OrderItem item,
                       BiConsumer<OrderItem, Integer> onQtyChange,
                       Consumer<OrderItem> onDelete,
                       Consumer<OrderItem> onEdit) {
        this.item = item;
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(14);
        setPadding(new Insets(12, 14, 12, 14));
        // Nền kem nhạt dịu mắt, không dùng viền đậm gây rối
        setStyle("-fx-background-color: #FBF9F5; -fx-background-radius: 10px;");

        // 1. Khung ảnh dọc nguyên tỉ lệ (56x76px, PreserveRatio = true, không bóp méo)
        MenuItem menuItem = item.getProductId() > 0
            ? MenuRepository.findById(item.getProductId())
            : MenuRepository.findByName(item.getName());

        Image thumb = ImageManager.getProductImage(menuItem, 168, 228);
        ImageView ivThumb = new ImageView(thumb);
        ivThumb.setFitWidth(56);
        ivThumb.setFitHeight(76);
        ivThumb.setPreserveRatio(true);
        ivThumb.setSmooth(true);

        StackPane thumbWrapper = new StackPane(ivThumb);
        thumbWrapper.setMinSize(56, 76);
        thumbWrapper.setPrefSize(56, 76);
        thumbWrapper.setMaxSize(56, 76);
        thumbWrapper.setAlignment(Pos.CENTER);
        thumbWrapper.setStyle("-fx-background-color: #F2ECE1; -fx-background-radius: 8px;");

        Rectangle clip = new Rectangle(56, 76);
        clip.setArcWidth(14);
        clip.setArcHeight(14);
        thumbWrapper.setClip(clip);

        // 2. Cột thông tin giữa (Tên món, Tùy chọn, Giá + Đổi tùy chọn không viền)
        VBox infoBox = new VBox(5);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label lblName = new Label(item.getName());
        lblName.setStyle(STYLE_BOLD);
        lblName.setWrapText(true);
        infoBox.getChildren().add(lblName);

        String customSummary = item.getCustomizationSummary();
        if (customSummary != null && !customSummary.isEmpty()) {
            Label lblCustom = new Label(customSummary);
            lblCustom.setStyle(STYLE_REGULAR);
            lblCustom.setWrapText(true);
            infoBox.getChildren().add(lblCustom);
        }

        HBox priceAndEditRow = new HBox(12);
        priceAndEditRow.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(priceAndEditRow, new Insets(2, 0, 0, 0));

        Label lblPrice = new Label(CurrencyFormatter.format(item.getSubtotal()));
        lblPrice.setStyle(STYLE_BOLD);
        priceAndEditRow.getChildren().add(lblPrice);

        if (onEdit != null && menuItem != null) {
            // Nút "✎ Đổi tùy chọn" dạng chữ phẳng không viền (borderless) tránh rối mắt
            Button btnEdit = new Button("✎ Đổi tùy chọn");
            String editNormalStyle =
                "-fx-background-color: transparent; " +
                "-fx-border-color: transparent; " +
                "-fx-font-family: 'Inter', sans-serif; " +
                "-fx-font-size: 12px; " +
                "-fx-font-weight: 400; " +
                "-fx-text-fill: #4A453E; " +
                "-fx-underline: true; " +
                "-fx-padding: 1 4 1 4; " +
                "-fx-cursor: hand;";
            String editHoverStyle =
                "-fx-background-color: #EFE8DD; " +
                "-fx-background-radius: 4px; " +
                "-fx-border-color: transparent; " +
                "-fx-font-family: 'Inter', sans-serif; " +
                "-fx-font-size: 12px; " +
                "-fx-font-weight: 400; " +
                "-fx-text-fill: #112823; " +
                "-fx-padding: 1 4 1 4; " +
                "-fx-cursor: hand;";
            btnEdit.setStyle(editNormalStyle);
            btnEdit.setOnMouseEntered(e -> btnEdit.setStyle(editHoverStyle));
            btnEdit.setOnMouseExited(e -> btnEdit.setStyle(editNormalStyle));
            btnEdit.setOnAction(e -> onEdit.accept(item));
            priceAndEditRow.getChildren().add(btnEdit);
        }

        infoBox.getChildren().add(priceAndEditRow);

        // 3. Cột phải: Nút xóa [🗑] ở góc trên & Cụm tăng giảm số lượng phẳng không viền ở góc dưới
        VBox rightActionCol = new VBox(14);
        rightActionCol.setAlignment(Pos.CENTER_RIGHT);

        Button btnDelete = new Button();
        btnDelete.setGraphic(cafe.utils.IconHelper.getIcon("client", "xoa-mon.png", SvgIcons.TRASH, "🗑", 13, Color.web("#817A70")));
        btnDelete.setStyle("-fx-background-color: transparent; -fx-background-radius: 6px; -fx-min-width: 26px; -fx-min-height: 26px; -fx-pref-width: 26px; -fx-pref-height: 26px; -fx-padding: 0; -fx-cursor: hand; -fx-alignment: center;");
        btnDelete.setOnMouseEntered(e -> btnDelete.setStyle("-fx-background-color: #FDE8E8; -fx-background-radius: 6px; -fx-min-width: 26px; -fx-min-height: 26px; -fx-pref-width: 26px; -fx-pref-height: 26px; -fx-padding: 0; -fx-cursor: hand; -fx-alignment: center;"));
        btnDelete.setOnMouseExited(e -> btnDelete.setStyle("-fx-background-color: transparent; -fx-background-radius: 6px; -fx-min-width: 26px; -fx-min-height: 26px; -fx-pref-width: 26px; -fx-pref-height: 26px; -fx-padding: 0; -fx-cursor: hand; -fx-alignment: center;"));
        btnDelete.setTooltip(new Tooltip("Xóa món này"));
        btnDelete.setOnAction(e -> onDelete.accept(item));

        // Pill Stepper không viền (chỉ dùng nền #EFEAE1 dịu mắt)
        HBox pillStepper = new HBox(2);
        pillStepper.setAlignment(Pos.CENTER);
        pillStepper.setPadding(new Insets(2, 5, 2, 5));
        pillStepper.setStyle(
            "-fx-background-color: #EFEAE1; " +
            "-fx-background-radius: 14px; " +
            "-fx-border-color: transparent;"
        );

        Button btnMinus = new Button();
        btnMinus.setGraphic(cafe.utils.IconHelper.getIcon("client", "giam-sl.png", null, "-", 11, Color.web("#112823")));
        btnMinus.setStyle("-fx-background-color: transparent; -fx-min-width: 24px; -fx-min-height: 24px; -fx-pref-width: 24px; -fx-pref-height: 24px; -fx-padding: 0; -fx-cursor: hand; -fx-alignment: center;");
        AnimationHelper.applyButtonPress(btnMinus);
        btnMinus.setOnAction(e -> onQtyChange.accept(item, -1));

        Label lblQty = new Label(String.valueOf(item.getQty()));
        lblQty.setStyle(STYLE_BOLD + " -fx-min-width: 22px; -fx-alignment: center;");

        Button btnPlus = new Button();
        btnPlus.setGraphic(cafe.utils.IconHelper.getIcon("client", "tang-sl.png", null, "+", 11, Color.web("#112823")));
        btnPlus.setStyle("-fx-background-color: transparent; -fx-min-width: 24px; -fx-min-height: 24px; -fx-pref-width: 24px; -fx-pref-height: 24px; -fx-padding: 0; -fx-cursor: hand; -fx-alignment: center;");
        AnimationHelper.applyButtonPress(btnPlus);
        btnPlus.setOnAction(e -> onQtyChange.accept(item, 1));

        pillStepper.getChildren().addAll(btnMinus, lblQty, btnPlus);

        rightActionCol.getChildren().addAll(btnDelete, pillStepper);

        getChildren().addAll(thumbWrapper, infoBox, rightActionCol);
    }

    public OrderItem getItem() {
        return item;
    }
}

