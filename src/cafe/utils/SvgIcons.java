package cafe.utils;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.SVGPath;

/**
 * SvgIcons - Lightweight vector icon provider using Lucide / Feather SVG paths.
 * Renders resolution-independent icons at any DPI without external dependencies.
 */
public class SvgIcons {

    // SVG Path Data (from Lucide icons)
    public static final String SHOPPING_BAG = "M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z M3 6h18 M16 10a4 4 0 0 1-8 0";
    public static final String SEARCH = "M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16z M21 21l-4.35-4.35";
    public static final String COFFEE = "M18 8h1a4 4 0 0 1 0 8h-1 M2 8h16v9a4 4 0 0 1-4 4H6a4 4 0 0 1-4-4V8z M6 1v3 M10 1v3 M14 1v3";
    public static final String BEANS = "M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10 10-4.5 10-10S17.5 2 12 2zm1 14.5c-2.5 0-4-2-4-4.5s1.5-4.5 4-4.5 4 2 4 4.5-1.5 4.5-4 4.5z";
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
    public static final String CLOCK = "M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z M12 6v6l4 2";
    public static final String USER = "M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2 M12 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8z";

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
}
