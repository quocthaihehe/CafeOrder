package cafe.utils;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IconHelper - Tiện ích nạp Icon đa tầng (Multi-tier Icon Loader) chuẩn toàn hệ thống:
 * 1. Classpath resource (/cafe/resources/images/icons/{folder}/..., /resources/images/icons/...)
 * 2. Dev disk file (src/cafe/resources/images/icons/{folder}/..., resources/images/icons/...)
 * 3. SvgIcons vector path (sắc nét, không vỡ hạt)
 * 4. Fallback text / emoji
 */
public class IconHelper {

    private static final Map<String, Image> TINT_CACHE = new ConcurrentHashMap<>();

    public static Node getIcon(String folder, String fileName, String svgFallback, String textFallback, double size, Color iconColor) {
        if (fileName != null && !fileName.trim().isEmpty()) {
            String[] resourcePaths = {
                "/cafe/resources/images/icons/" + folder + "/" + fileName,
                "/resources/images/icons/" + folder + "/" + fileName,
                "/cafe/resources/images/icons/" + fileName,
                "/resources/images/icons/" + fileName,
                "/cafe/resources/images/" + fileName,
                "/resources/images/" + fileName
            };

            for (String p : resourcePaths) {
                try {
                    URL url = IconHelper.class.getResource(p);
                    if (url != null) {
                        Image img = loadAndTintIfNeeded(url.toExternalForm(), iconColor);
                        if (img != null && !img.isError() && img.getWidth() > 0) {
                            return createImageView(img, size);
                        }
                    }
                } catch (Exception ignored) {}
            }

            // Fallback đường dẫn đĩa cứng phát triển
            String[] diskPaths = {
                "src/cafe/resources/images/icons/" + folder + "/" + fileName,
                "resources/images/icons/" + folder + "/" + fileName,
                "src/cafe/resources/images/icons/" + fileName,
                "resources/images/icons/" + fileName
            };

            for (String dp : diskPaths) {
                try {
                    File file = new File(dp);
                    if (file.exists()) {
                        Image img = loadAndTintIfNeeded(file.toURI().toString(), iconColor);
                        if (img != null && !img.isError() && img.getWidth() > 0) {
                            return createImageView(img, size);
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        // Tầng 3: Vector SVG nét mảnh chuẩn
        if (svgFallback != null && !svgFallback.trim().isEmpty()) {
            return SvgIcons.createOutlineIcon(svgFallback, size / 24.0, iconColor != null ? iconColor : Color.web("#292722"));
        }

        // Tầng 4: Fallback Text / Emoji
        Label lbl = new Label(textFallback != null ? textFallback : "");
        lbl.setStyle("-fx-font-size: " + Math.max(10, size * 0.75) + "px; -fx-text-fill: " + (iconColor != null ? toHex(iconColor) : "#292722") + ";");
        return lbl;
    }

    private static ImageView createImageView(Image img, double size) {
        ImageView iv = new ImageView(img);
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        return iv;
    }

    /**
     * Nạp ảnh gốc và tự động nhuộm màu (tint) sang iconColor nếu iconColor là màu sáng/trắng
     * (ví dụ khi danh mục trên Sidebar đang được chọn với nền xanh đậm #112823) hoặc màu chỉ định.
     */
    private static Image loadAndTintIfNeeded(String uri, Color iconColor) {
        String colorKey = (iconColor != null && shouldTint(iconColor)) ? toHex(iconColor) : "ORIGINAL";
        String cacheKey = uri + "::" + colorKey;

        Image cached = TINT_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        Image baseImg = TINT_CACHE.computeIfAbsent(uri + "::ORIGINAL", k -> new Image(uri));
        if (baseImg.isError() || baseImg.getWidth() <= 0) {
            return baseImg;
        }

        if ("ORIGINAL".equals(colorKey)) {
            return baseImg;
        }

        Image tinted = tintImage(baseImg, iconColor);
        TINT_CACHE.put(cacheKey, tinted);
        return tinted;
    }

    private static boolean shouldTint(Color c) {
        if (c == null) return false;
        // Nhuộm màu khi yêu cầu hiển thị icon màu trắng / kem sáng trên nền tối (#112823),
        // hoặc các màu sắc trạng thái đặc biệt (đỏ xóa, xanh lá, vàng đồng...)
        return c.getBrightness() > 0.80 || c.getSaturation() > 0.35;
    }

    private static Image tintImage(Image src, Color targetColor) {
        int w = (int) src.getWidth();
        int h = (int) src.getHeight();
        PixelReader reader = src.getPixelReader();
        if (reader == null || w <= 0 || h <= 0) {
            return src;
        }

        WritableImage out = new WritableImage(w, h);
        PixelWriter writer = out.getPixelWriter();

        double tr = targetColor.getRed();
        double tg = targetColor.getGreen();
        double tb = targetColor.getBlue();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Color px = reader.getColor(x, y);
                double alpha = px.getOpacity();
                if (alpha <= 0.01) {
                    writer.setColor(x, y, Color.TRANSPARENT);
                } else {
                    // Giữ nguyên độ mượt khử răng cưa (alpha) của icon gốc và phủ màu đích (예: #FFFFFF)
                    writer.setColor(x, y, new Color(tr, tg, tb, alpha));
                }
            }
        }
        return out;
    }

    private static String toHex(Color c) {
        return String.format("#%02X%02X%02X",
            (int) Math.round(c.getRed() * 255),
            (int) Math.round(c.getGreen() * 255),
            (int) Math.round(c.getBlue() * 255));
    }
}

