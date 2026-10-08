package cafe.utils;

import cafe.models.MenuItem;
import cafe.models.MenuRepository;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Công cụ sinh ảnh minh họa nghệ thuật tối giản cho sản phẩm và placeholder.
 * KHÔNG in chữ thừa (tên món, danh mục) để tránh trùng lặp với thông tin thẻ món.
 * Tập trung vào biểu tượng đồ uống/món ăn nổi bật trên nền Gradient cao cấp.
 */
public class AssetGenerator {

    private static final int WIDTH = 400;
    private static final int HEIGHT = 400;

    public static void main(String[] args) {
        System.out.println("=== BẮT ĐẦU SINH TÀI NGUYÊN ẢNH NGHỆ THUẬT (KHÔNG CHỮ) CHO CAFEORDER ===");
        File placeholderDir = new File("src/cafe/resources/images/placeholders");
        File productDir = new File("src/cafe/resources/images/products");

        placeholderDir.mkdirs();
        productDir.mkdirs();

        // 1. Sinh 2 ảnh Placeholder mặc định
        generateDefaultPlaceholders(placeholderDir);

        // 2. Sinh ảnh minh họa cho 67 món
        generateProductAssets(productDir);

        // 3. Sinh ảnh poster đăng nhập nghệ thuật (Screen 1)
        generateLoginHero(new File("src/cafe/resources/images"));

        System.out.println("=== HOÀN TẤT SINH ẢNH! TOÀN BỘ ASSETS ĐÃ ĐƯỢC LÀM MỚI SẠCH ĐẸP ===");
    }

    private static void generateLoginHero(File dir) {
        int w = 550;
        int h = 750;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Nền tối cà phê cao cấp (#13251E -> #2D1A12)
        GradientPaint gp = new GradientPaint(0, 0, new Color(0x13, 0x25, 0x1E), w, h, new Color(0x2D, 0x1A, 0x12));
        g.setPaint(gp);
        g.fillRect(0, 0, w, h);

        // Quầng sáng ấm trung tâm
        g.setColor(new Color(230, 160, 100, 25));
        g.fillOval(50, 120, 450, 450);

        // Họa tiết trang trí vòng tròn nghệ thuật
        g.setColor(new Color(255, 255, 255, 12));
        g.drawOval(w/2 - 140, 250, 280, 280);
        g.drawOval(w/2 - 180, 210, 360, 360);

        // Biểu tượng tách cà phê tối giản phong cách Nhật
        g.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 120));
        FontMetrics fm = g.getFontMetrics();
        String emoji = "☕";
        int ew = fm.stringWidth(emoji);
        g.setColor(new Color(255, 255, 255, 220));
        g.drawString(emoji, (w - ew) / 2, 380);

        // Brand Logo top left
        g.setFont(new Font("Playfair Display", Font.BOLD, 22));
        g.setColor(Color.WHITE);
        g.drawString("Brew & Bean", 48, 64);
        g.setFont(new Font("Segoe UI", Font.BOLD, 10));
        g.setColor(new Color(212, 163, 115));
        g.drawString("C A F E", 50, 80);

        // Tiêu đề nghệ thuật lớn: Good Coffee Better Moments
        g.setFont(new Font("Playfair Display", Font.BOLD, 46));
        g.setColor(new Color(244, 240, 232));
        g.drawString("Good", 48, 520);
        g.drawString("Coffee", 48, 570);
        g.setColor(new Color(212, 163, 115));
        g.drawString("Better", 48, 620);
        g.setColor(new Color(244, 240, 232));
        g.drawString("Moments", 48, 670);

        // Bottom quote
        g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g.setColor(new Color(200, 190, 180));
        g.drawString("Order tại bàn. Nhanh hơn, Tiện hơn, Trọn vẹn hơn.", 48, 715);

        g.dispose();
        saveImage(img, new File(dir, "login_hero.png"));
    }

    private static void generateDefaultPlaceholders(File dir) {
        // default_drink.png
        BufferedImage drinkImg = createArtisticBadge(
            "☕",
            new Color(0x2D, 0x1B, 0x14),
            new Color(0x6D, 0x4C, 0x41)
        );
        saveImage(drinkImg, new File(dir, "default_drink.png"));

        // default_food.png
        BufferedImage foodImg = createArtisticBadge(
            "🥐",
            new Color(0xBF, 0x36, 0x0C),
            new Color(0xE6, 0x51, 0x00)
        );
        saveImage(foodImg, new File(dir, "default_food.png"));
    }

    private static void generateProductAssets(File dir) {
        List<MenuItem> items = MenuRepository.getAllItems();
        for (MenuItem item : items) {
            String slugName = ImageManager.getProductImageFileName(item);
            File targetFile = new File(dir, slugName);

            Color topColor;
            Color bottomColor;
            String icon = item.getIconEmoji();

            switch (item.getCategory()) {
                case "Cà phê":
                    topColor = new Color(0x24, 0x14, 0x0E);
                    bottomColor = new Color(0x54, 0x38, 0x2B);
                    break;
                case "Trà & Trái cây":
                    topColor = new Color(0xB3, 0x39, 0x17);
                    bottomColor = new Color(0xE6, 0x6E, 0x32);
                    break;
                case "Đá xay & Sinh tố":
                    topColor = new Color(0x38, 0x22, 0x1C);
                    bottomColor = new Color(0x7D, 0x5C, 0x50);
                    break;
                case "Trà sữa":
                    topColor = new Color(0x4A, 0x32, 0x2B);
                    bottomColor = new Color(0x9E, 0x7E, 0x72);
                    break;
                case "Soda & Nước ép":
                    topColor = new Color(0x00, 0x45, 0x3A);
                    bottomColor = new Color(0x00, 0x82, 0x75);
                    break;
                case "Matcha & Chocolate":
                    topColor = new Color(0x1B, 0x4E, 0x1E);
                    bottomColor = new Color(0x38, 0x8E, 0x3C);
                    break;
                case "Bánh ngọt":
                    topColor = new Color(0x6E, 0x3F, 0x1D);
                    bottomColor = new Color(0xD9, 0x77, 0x24);
                    break;
                case "Món ăn nhẹ":
                    topColor = new Color(0x7B, 0x14, 0x2F);
                    bottomColor = new Color(0xB8, 0x2E, 0x52);
                    break;
                default:
                    topColor = new Color(0x33, 0x33, 0x33);
                    bottomColor = new Color(0x66, 0x66, 0x66);
                    break;
            }

            BufferedImage img = createArtisticBadge(icon, topColor, bottomColor);
            saveImage(img, targetFile);
        }
    }

    private static BufferedImage createArtisticBadge(String emoji, Color topColor, Color bottomColor) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // Bật khử răng cưa và chất lượng vẽ tối đa
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        // 1. Nền Gradient chuyển màu mềm mại
        GradientPaint gp = new GradientPaint(0, 0, topColor, WIDTH, HEIGHT, bottomColor);
        g.setPaint(gp);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 2. Các vòng tròn trang trí ánh sáng mờ nghệ thuật
        g.setColor(new Color(255, 255, 255, 14));
        g.fill(new Ellipse2D.Double(-40, -40, 240, 240));
        g.fill(new Ellipse2D.Double(WIDTH - 200, HEIGHT - 200, 260, 260));
        g.fill(new Ellipse2D.Double(WIDTH - 120, -50, 180, 180));

        // 3. Vòng tròn trung tâm phong cách Frosted Glass (Kính mờ)
        int centerCircle = 190;
        int cx = (WIDTH - centerCircle) / 2;
        int cy = (HEIGHT - centerCircle) / 2;

        // Vầng sáng lan tỏa bên ngoài
        g.setColor(new Color(255, 255, 255, 28));
        g.fill(new Ellipse2D.Double(cx - 16, cy - 16, centerCircle + 32, centerCircle + 32));

        // Nền đĩa tròn trung tâm
        g.setColor(new Color(255, 255, 255, 48));
        g.fill(new Ellipse2D.Double(cx, cy, centerCircle, centerCircle));

        // Viền thanh mảnh tinh tế
        g.setColor(new Color(255, 255, 255, 140));
        g.setStroke(new BasicStroke(2.5f));
        g.draw(new Ellipse2D.Double(cx, cy, centerCircle, centerCircle));

        // Vòng chỉ viền mảnh bên trong
        g.setColor(new Color(255, 255, 255, 70));
        g.setStroke(new BasicStroke(1.2f));
        g.draw(new Ellipse2D.Double(cx + 8, cy + 8, centerCircle - 16, centerCircle - 16));

        // 4. Vẽ Emoji biểu tượng món to, rõ, căn giữa chính xác
        g.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 88));
        FontMetrics fm = g.getFontMetrics();
        int emojiWidth = fm.stringWidth(emoji);
        int emojiY = cy + (centerCircle + fm.getAscent() - fm.getDescent()) / 2;

        // Đổ bóng biểu tượng
        g.setColor(new Color(0, 0, 0, 70));
        g.drawString(emoji, (WIDTH - emojiWidth) / 2 + 2, emojiY + 2);

        // Biểu tượng chính
        g.setColor(Color.WHITE);
        g.drawString(emoji, (WIDTH - emojiWidth) / 2, emojiY);

        g.dispose();
        return image;
    }

    private static void saveImage(BufferedImage img, File file) {
        try {
            ImageIO.write(img, "png", file);
        } catch (Exception e) {
            System.err.println("Lỗi lưu ảnh: " + file.getName() + " - " + e.getMessage());
        }
    }
}
