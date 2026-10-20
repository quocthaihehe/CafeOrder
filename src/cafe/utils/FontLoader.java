package cafe.utils;

import javafx.scene.text.Font;
import java.io.InputStream;

/**
 * FontLoader - Tải font nhận diện thương hiệu cho Brew & Bean POS:
 * - Playfair Display (Bold, Regular) cho tiêu đề mang phong cách Editorial sang trọng.
 * - Inter (Regular, Medium, SemiBold) cho toàn bộ nội dung và nút bấm giao diện.
 */
public class FontLoader {

    private static boolean initialized = false;

    public static synchronized void loadFonts() {
        if (initialized) return;
        initialized = true;

        String[] fontPaths = {
            "/fonts/PlayfairDisplay-Bold.ttf",
            "/fonts/PlayfairDisplay-Regular.ttf",
            "/fonts/Inter-Regular.ttf",
            "/fonts/Inter-Medium.ttf",
            "/fonts/Inter-SemiBold.ttf",
            "/main/resources/fonts/PlayfairDisplay-Bold.ttf",
            "/main/resources/fonts/PlayfairDisplay-Regular.ttf",
            "/main/resources/fonts/Inter-Regular.ttf",
            "/main/resources/fonts/Inter-Medium.ttf",
            "/main/resources/fonts/Inter-SemiBold.ttf",
            "/cafe/resources/fonts/PlayfairDisplay-Bold.ttf",
            "/cafe/resources/fonts/PlayfairDisplay-Regular.ttf",
            "/cafe/resources/fonts/Inter-Regular.ttf",
            "/cafe/resources/fonts/Inter-Medium.ttf",
            "/cafe/resources/fonts/Inter-SemiBold.ttf"
        };

        for (String path : fontPaths) {
            try (InputStream is = FontLoader.class.getResourceAsStream(path)) {
                if (is != null) {
                    Font.loadFont(is, 14);
                }
            } catch (Exception ignored) {
            }
        }
    }
}
