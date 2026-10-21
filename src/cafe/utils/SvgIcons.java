package cafe.utils;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.SVGPath;

/**
 * SvgIcons - Lightweight vector icon provider using Lucide / Feather SVG paths.
 * Renders resolution-independent icons at any DPI without external dependencies.
 */
public class SvgIcons {

    // General UI Icons
    public static final String SHOPPING_BAG = "M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z M3 6h18 M16 10a4 4 0 0 1-8 0";
    public static final String SEARCH = "M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16z M21 21l-4.35-4.35";
    public static final String PLUS = "M12 5v14 M5 12h14";
    public static final String MINUS = "M5 12h14";
    public static final String TRASH = "M3 6h18 M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2 M10 11v6 M14 11v6";
    public static final String CLOSE = "M18 6L6 18 M6 6l12 12";
    public static final String CHECK = "M20 6L9 17l-5-5";
    public static final String ARROW_RIGHT = "M5 12h14 M12 5l7 7-7 7";
    public static final String ARROW_LEFT = "M19 12H5 M12 19l-7-7 7-7";
    public static final String CASH = "M2 6h20v12H2z M12 12a3 3 0 1 0 0-6 3 3 0 0 0 0 6z M6 12h.01 M18 12h.01";
    public static final String CREDIT_CARD = "M2 5h20v14H2z M2 10h20 M6 15h4";
    public static final String WALLET = "M19 7V4a1 1 0 0 0-1-1H5a2 2 0 0 0 0 4h15a1 1 0 0 1 1 1v4h-3a2 2 0 0 0 0 4h3a1 1 0 0 0 1-1v-2a1 1 0 0 0-1-1 M3 5v14a2 2 0 0 0 2 2h15a1 1 0 0 0 1-1v-4";
    public static final String EDIT = "M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7 M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z";
    public static final String CLOCK = "M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z M12 6v6l4 2";
    public static final String USER = "M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2 M12 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8z";
    public static final String LOCK = "M19 11H5a2 2 0 0 0-2 2v7a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7a2 2 0 0 0-2-2z M7 11V7a5 5 0 0 1 10 0v4";
    public static final String EYE = "M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6z";
    public static final String EYE_OFF = "M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24 M1 1l22 22";
    public static final String LOG_OUT = "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4 M16 17l5-5-5-5 M21 12H9";

    // Category Icons (per Section 4.4 of Bao_Cao_Chuyen_Gia)
    public static final String COFFEE = "M18 8h1a4 4 0 0 1 0 8h-1 M2 8h16v9a4 4 0 0 1-4 4H6a4 4 0 0 1-4-4V8z M6 1v3 M10 1v3 M14 1v3";
    public static final String TEA = "M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10Z M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12";
    public static final String SMOOTHIE = "M7 21h10 M12 21v-4 M17 3H7l1 14h8l1-14z M10 3V1 M14 3V1";
    public static final String JUICE = "M5 2h14l-2 18H7L5 2z M9 2v3 M12 2v4 M15 2v3";
    public static final String CAKE = "M12 2a4 4 0 0 0-4 4v1H6a2 2 0 0 0-2 2v3h16V9a2 2 0 0 0-2-2h-2V6a4 4 0 0 0-4-4z M4 15l1.5 6h13L20 15H4z";
    public static final String SNACK = "M18 2v20 M14 2v6a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V2 M10 2v6 M14 10v12";
    public static final String MILK_TEA = "M17 8h1a4 4 0 0 1 0 8h-1 M3 8h14v11a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V8z M6 2l3 6 M11 2l-1 6";

    // Navigation & Floor Plan Icons
    public static final String TABLE = "M3 5h18v4H3z M4 9v10 M20 9v10 M10 9v10 M14 9v10";
    public static final String MENU = "M4 6h16 M4 12h16 M4 18h16";
    public static final String ORDERS = "M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2 M9 2h6a1 1 0 0 1 1 1v2a1 1 0 0 1-1 1H9a1 1 0 0 1-1-1V3a1 1 0 0 1 1-1z M9 12h6 M9 16h6";
    public static final String SETTINGS = "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z";
    public static final String SPROUT = "M7 20h10 M10 20c0-4.4 3.6-8 8-8V8c-4.4 0-8 3.6-8 8 M14 20c0-6.6-5.4-12-12-12v4c4.4 0 8 3.6 8 8";
    public static final String FLOWER = "M12 7.5a4.5 4.5 0 1 1 4.5 4.5M12 7.5A4.5 4.5 0 1 0 7.5 12M12 7.5V9m4.5 3a4.5 4.5 0 1 1-4.5 4.5M16.5 12H15m-4.5 4.5a4.5 4.5 0 1 1-4.5-4.5M10.5 16.5V15m-3-4.5H9m3 3a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z";
    public static final String LEAF = "M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10Z M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12";

    /**
     * Create an SVGPath node from SVG data string.
     */
    public static SVGPath createIcon(String pathData, double scale, Paint fill, Paint stroke, double strokeWidth) {
        SVGPath path = new SVGPath();
        path.setContent(pathData);
        path.setScaleX(scale);
        path.setScaleY(scale);
        path.setFill(fill);
        path.setStroke(stroke);
        path.setStrokeWidth(strokeWidth);
        return path;
    }

    /**
     * Create a stroked outline icon (standard 1.8px line stroke, transparent fill).
     */
    public static SVGPath createOutlineIcon(String pathData, double scale, Paint color) {
        return createIcon(pathData, scale, Color.TRANSPARENT, color, 1.8);
    }

    /**
     * Create a filled icon (e.g. checkmark or bean).
     */
    public static SVGPath createFilledIcon(String pathData, double scale, Paint color) {
        return createIcon(pathData, scale, color, Color.TRANSPARENT, 0);
    }

    /**
     * Get matching icon path for category name.
     */
    public static String getIconForCategory(String categoryName) {
        if (categoryName == null) return SHOPPING_BAG;
        String cat = categoryName.trim().toLowerCase();
        if (cat.contains("tất cả")) return SHOPPING_BAG;
        if (cat.contains("cà phê")) return COFFEE;
        if (cat.contains("trà sữa")) return MILK_TEA;
        if (cat.contains("trà")) return TEA;
        if (cat.contains("đá xay") || cat.contains("sinh tố")) return SMOOTHIE;
        if (cat.contains("soda") || cat.contains("nước ép")) return JUICE;
        if (cat.contains("matcha") || cat.contains("chocolate")) return COFFEE;
        if (cat.contains("bánh ngọt")) return CAKE;
        if (cat.contains("món ăn") || cat.contains("ăn nhẹ")) return SNACK;
        return SHOPPING_BAG;
    }
}
