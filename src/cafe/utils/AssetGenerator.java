package cafe.utils;

import cafe.models.MenuItem;
import cafe.models.MenuRepository;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Công cụ sinh ảnh tự động (Placeholder & Default Category Assets)
 * Phục vụ nhóm làm việc: Khi thêm món mới chưa có ảnh chụp thực tế,
 * hệ thống tự động sinh ảnh định dạng chuẩn 400x400 với màu sắc chủ đề tinh tế.
 */
public class AssetGenerator {

    private static final int WIDTH = 400;
    private static final int HEIGHT = 400;

    public static void main(String[] args) {
        System.out.println("=== BẮT ĐẦU SINH TÀI NGUYÊN ẢNH CHO CAFEORDER ===");
        File placeholderDir = new File("src/cafe/resources/images/placeholders");
        File productDir = new File("src/cafe/resources/images/products");

        placeholderDir.mkdirs();
        productDir.mkdirs();

        // 1. Sinh 2 ảnh Placeholder dự phòng mặc định
        generateDefaultPlaceholders(placeholderDir);

        // 2. Sinh ảnh mẫu chuẩn phong cách cafe cho toàn bộ 67 món
        generateProductAssets(productDir);

        System.out.println("=== HOÀN TẤT SINH ẢNH! TOÀN BỘ ASSETS ĐÃ SẴN SÀNG ===");
    }

    private static void generateDefaultPlaceholders(File dir) {
        // default_drink.png (Mẫu đồ uống mặc định)
        BufferedImage drinkImg = createThemedImage(
            "L'Amour Artisan",
            "Đồ Uống Đặc Biệt",
            "☕",
            new Color(0x3E, 0x27, 0x23),
            new Color(0x6D, 0x4C, 0x41),
            new Color(0xD7, 0xCC, 0xC8)
        );
        saveImage(drinkImg, new File(dir, "default_drink.png"));

        // default_food.png (Mẫu món ăn / bánh ngọt mặc định)
        BufferedImage foodImg = createThemedImage(
            "L'Amour Bakery",
            "Bánh & Món Ăn Nhẹ",
            "🥐",
            new Color(0xBF, 0x36, 0x0C),
            new Color(0xE6, 0x51, 0x00),
            new Color(0xFF, 0xEB, 0xEE)
        );
        saveImage(foodImg, new File(dir, "default_food.png"));
    }

    private static void generateProductAssets(File dir) {
        List<MenuItem> items = MenuRepository.getAllItems();
        for (MenuItem item : items) {
            String slugName = ImageManager.getProductImageFileName(item);
            File targetFile = new File(dir, slugName);

            // Xác định màu sắc theo danh mục
            Color topColor;
            Color bottomColor;
            Color accentColor = Color.WHITE;
            String icon = item.getIconEmoji();

            switch (item.getCategory()) {
                case "Cà phê":
                    topColor = new Color(0x2D, 0x1B, 0x14);
                    bottomColor = new Color(0x5D, 0x40, 0x37);
                    break;
                case "Trà & Trái cây":
                    topColor = new Color(0xBF, 0x36, 0x0C);
                    bottomColor = new Color(0xF5, 0x7C, 0x00);
                    break;
                case "Đá xay & Sinh tố":
                    topColor = new Color(0x3E, 0x27, 0x23);
                    bottomColor = new Color(0x8D, 0x6E, 0x63);
                    break;
                case "Trà sữa":
                    topColor = new Color(0x4E, 0x34, 0x2E);
                    bottomColor = new Color(0xA1, 0x88, 0x7F);
                    break;
                case "Soda & Nước ép":
                    topColor = new Color(0x00, 0x4D, 0x40);
                    bottomColor = new Color(0x00, 0x89, 0x7B);
                    break;
                case "Matcha & Chocolate":
                    topColor = new Color(0x1B, 0x5E, 0x20);
                    bottomColor = new Color(0x43, 0xA0, 0x47);
                    break;
                case "Bánh ngọt":
                    topColor = new Color(0x79, 0x55, 0x48);
                    bottomColor = new Color(0xFB, 0x8C, 0x00);
                    break;
                case "Món ăn nhẹ":
                    topColor = new Color(0x88, 0x0E, 0x4F);
                    bottomColor = new Color(0xC2, 0x18, 0x5B);
                    break;
                default:
                    topColor = new Color(0x37, 0x47, 0x4F);
                    bottomColor = new Color(0x60, 0x7D, 0x8B);
                    break;
            }

            BufferedImage img = createThemedImage(
                item.getCategory().toUpperCase(),
                item.getName(),
                icon,
                topColor,
                bottomColor,
                accentColor
            );
            saveImage(img, targetFile);
        }
    }

    private static BufferedImage createThemedImage(String subtitle, String title, String emoji,
                                                  Color topColor, Color bottomColor, Color accentColor) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // Khử răng cưa tối đa cho đồ họa sắc nét
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        // 1. Nền Gradient chuyển màu nghệ thuật
        GradientPaint gp = new GradientPaint(0, 0, topColor, 0, HEIGHT, bottomColor);
        g.setPaint(gp);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 2. Họa tiết trang trí vòng tròn mờ phía sau
        g.setColor(new Color(255, 255, 255, 18));
        g.fillOval(-60, -60, 260, 260);
        g.fillOval(WIDTH - 180, HEIGHT - 180, 280, 280);

        // 3. Khung tròn trung tâm chứa Icon / Biểu tượng món
        int circleSize = 140;
        int circleX = (WIDTH - circleSize) / 2;
        int circleY = 90;

        // Vòng phát sáng mềm
        g.setColor(new Color(255, 255, 255, 35));
        g.fillOval(circleX - 8, circleY - 8, circleSize + 16, circleSize + 16);

        // Nền vòng tròn chính
        g.setColor(new Color(255, 255, 255, 55));
        g.fillOval(circleX, circleY, circleSize, circleSize);

        g.setColor(new Color(255, 255, 255, 120));
        g.setStroke(new BasicStroke(2.5f));
        g.drawOval(circleX, circleY, circleSize, circleSize);

        // Vẽ biểu tượng món
        g.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 68));
        FontMetrics fmEmoji = g.getFontMetrics();
        int emojiWidth = fmEmoji.stringWidth(emoji);
        int emojiY = circleY + (circleSize + fmEmoji.getAscent() - fmEmoji.getDescent()) / 2;
        g.setColor(Color.WHITE);
        g.drawString(emoji, (WIDTH - emojiWidth) / 2, emojiY);

        // 4. Subtitle (Tên danh mục)
        g.setFont(new Font("Segoe UI", Font.BOLD, 13));
        FontMetrics fmSub = g.getFontMetrics();
        g.setColor(new Color(255, 255, 255, 200));
        int subWidth = fmSub.stringWidth(subtitle);
        g.drawString(subtitle, (WIDTH - subWidth) / 2, 280);

        // 5. Title (Tên món)
        g.setFont(new Font("Segoe UI", Font.BOLD, 22));
        FontMetrics fmTitle = g.getFontMetrics();
        int titleWidth = fmTitle.stringWidth(title);

        // Tự động ngắt dòng nếu tên món dài
        if (titleWidth > WIDTH - 40) {
            g.setFont(new Font("Segoe UI", Font.BOLD, 17));
            fmTitle = g.getFontMetrics();
            titleWidth = fmTitle.stringWidth(title);
        }

        // Đổ bóng chữ nhẹ
        g.setColor(new Color(0, 0, 0, 100));
        g.drawString(title, (WIDTH - titleWidth) / 2 + 1, 318 + 1);

        g.setColor(Color.WHITE);
        g.drawString(title, (WIDTH - titleWidth) / 2, 318);

        // 6. Thanh thương hiệu dưới cùng
        g.setColor(new Color(255, 255, 255, 30));
        g.fill(new RoundRectangle2D.Float(50, 345, WIDTH - 100, 26, 14, 14));

        g.setFont(new Font("Segoe UI", Font.BOLD, 10));
        String brand = "L'AMOUR ARTISAN CAFE";
        FontMetrics fmBrand = g.getFontMetrics();
        g.setColor(new Color(255, 255, 255, 220));
        g.drawString(brand, (WIDTH - fmBrand.stringWidth(brand)) / 2, 362);

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
