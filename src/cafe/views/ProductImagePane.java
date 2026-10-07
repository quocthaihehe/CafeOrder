package cafe.views;

import cafe.models.MenuItem;
import cafe.utils.ImageManager;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

/**
 * ProductImagePane - Khung hiển thị hình ảnh sản phẩm chuẩn Responsive Kiosk.
 * Giải quyết triệt để 3 vấn đề:
 * 1. Tự động co giãn vừa vặn 100% độ rộng thẻ (vùng lọt lòng ~225px), tuyệt đối không tràn sang thẻ bên cạnh.
 * 2. Cắt cúp ảnh theo tỉ lệ Cover (Center Crop) không bị méo, không bị dẹt.
 * 3. Bo góc tròn đều đủ 4 phía (Arc 24px = Radius 12px), đồng nhất giữa mọi thẻ trên Grid.
 */
public class ProductImagePane extends Pane {

    private final ImageView imageView;
    private final Rectangle clipRect;
    private final double cornerRadius;
    private final double fixedWidth; // <= 0 nếu responsive theo thẻ

    /**
     * Khởi tạo khung ảnh Responsive (Tự co giãn theo chiều rộng thẻ, chiều cao cố định).
     */
    public ProductImagePane(MenuItem item, double height, double cornerRadius) {
        this(item, -1, height, cornerRadius);
    }

    /**
     * Khởi tạo khung ảnh kích thước cụ thể (Dành cho Dialog / Popup).
     */
    public ProductImagePane(MenuItem item, double width, double height, double cornerRadius) {
        this.fixedWidth = width;
        this.cornerRadius = cornerRadius;

        Image img = ImageManager.getProductImage(item, 400, 400);
        this.imageView = new ImageView(img);
        this.imageView.setPreserveRatio(true);
        this.imageView.setSmooth(true);

        this.clipRect = new Rectangle();
        this.clipRect.setArcWidth(cornerRadius * 2);
        this.clipRect.setArcHeight(cornerRadius * 2);
        setClip(clipRect);

        if (fixedWidth > 0) {
            setMinWidth(fixedWidth);
            setPrefWidth(fixedWidth);
            setMaxWidth(fixedWidth);
        } else {
            setMinWidth(0);
            setPrefWidth(USE_COMPUTED_SIZE);
            setMaxWidth(Double.MAX_VALUE);
        }

        setMinHeight(height);
        setPrefHeight(height);
        setMaxHeight(height);

        setStyle("-fx-background-color: #F8F4F0; -fx-background-radius: " + cornerRadius + "px;");

        getChildren().add(imageView);
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();

        if (w <= 0 || h <= 0) {
            return;
        }

        // Cập nhật khung cắt bo góc 4 phía vừa khít kích thước thực tế
        clipRect.setWidth(w);
        clipRect.setHeight(h);

        Image img = imageView.getImage();
        if (img != null && img.getWidth() > 0 && img.getHeight() > 0) {
            double imgW = img.getWidth();
            double imgH = img.getHeight();

            // Thuật toán Center-Crop (Cover):
            // Phóng to ảnh vừa đủ để lấp đầy toàn bộ khung mà không làm biến dạng tỉ lệ gốc
            double scale = Math.max(w / imgW, h / imgH);
            double targetW = Math.ceil(imgW * scale);
            double targetH = Math.ceil(imgH * scale);

            imageView.setFitWidth(targetW);
            imageView.setFitHeight(targetH);

            // Căn giữa ảnh vào tâm khung
            double x = (w - targetW) / 2.0;
            double y = (h - targetH) / 2.0;
            imageView.relocate(x, y);
        }
    }
}
