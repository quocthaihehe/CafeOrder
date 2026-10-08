package cafe.utils;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * AnimationHelper - Utility class providing hardware-accelerated micro-interactions
 * for CafeOrder JavaFX UI.
 * Follows Apple / Linear POS animation curves:
 * - 80ms - 240ms duration
 * - Cubic-Bezier Spline Interpolation
 * - Debounced stop-and-play transitions
 */
public class AnimationHelper {

    // Cubic-Bezier Spline Curves
    public static final Interpolator EASE_OUT_QUART = Interpolator.SPLINE(0.25, 1.0, 0.5, 1.0);
    public static final Interpolator TACTILE_DECEL = Interpolator.SPLINE(0.16, 1.0, 0.3, 1.0);
    public static final Interpolator SMOOTH_SNAP   = Interpolator.SPLINE(0.4, 0.0, 0.2, 1.0);

    /**
     * Micro-lift hover effect for product cards (ItemCard).
     */
    public static void applyCardHover(Node node) {
        ScaleTransition st = new ScaleTransition(Duration.millis(160), node);
        TranslateTransition tt = new TranslateTransition(Duration.millis(160), node);
        st.setInterpolator(EASE_OUT_QUART);
        tt.setInterpolator(EASE_OUT_QUART);

        node.setOnMouseEntered(e -> {
            st.stop();
            tt.stop();
            st.setToX(1.025);
            st.setToY(1.025);
            tt.setToY(-3.0);
            st.play();
            tt.play();
        });

        node.setOnMouseExited(e -> {
            st.stop();
            tt.stop();
            st.setToX(1.0);
            st.setToY(1.0);
            tt.setToY(0.0);
            st.play();
            tt.play();
        });
    }

    /**
     * Tactile Press micro-interaction for buttons (like mechanical keycap press).
     */
    public static void applyButtonPress(Node node) {
        ScaleTransition pressSt = new ScaleTransition(Duration.millis(80), node);
        pressSt.setInterpolator(TACTILE_DECEL);

        node.setOnMousePressed(e -> {
            pressSt.stop();
            pressSt.setToX(0.965);
            pressSt.setToY(0.965);
            pressSt.play();
        });

        node.setOnMouseReleased(e -> {
            pressSt.stop();
            pressSt.setToX(1.0);
            pressSt.setToY(1.0);
            pressSt.play();
        });
    }

    /**
     * Badge Bump animation when an item is added to the cart.
     */
    public static void bumpBadge(Node badge) {
        if (badge == null) return;
        ScaleTransition bump = new ScaleTransition(Duration.millis(180), badge);
        bump.setFromX(1.0);
        bump.setFromY(1.0);
        bump.setToX(1.35);
        bump.setToY(1.35);
        bump.setAutoReverse(true);
        bump.setCycleCount(2);
        bump.setInterpolator(TACTILE_DECEL);
        bump.play();
    }

    /**
     * Modal Dialog Sheet Entrance animation.
     */
    public static void playDialogEntrance(Node rootPane) {
        if (rootPane == null) return;
        rootPane.setOpacity(0.0);
        rootPane.setScaleX(0.95);
        rootPane.setScaleY(0.95);
        rootPane.setTranslateY(18.0);

        FadeTransition ft = new FadeTransition(Duration.millis(200), rootPane);
        ft.setToValue(1.0);

        ScaleTransition st = new ScaleTransition(Duration.millis(220), rootPane);
        st.setToX(1.0);
        st.setToY(1.0);
        st.setInterpolator(TACTILE_DECEL);

        TranslateTransition tt = new TranslateTransition(Duration.millis(220), rootPane);
        tt.setToY(0.0);
        tt.setInterpolator(TACTILE_DECEL);

        new ParallelTransition(ft, st, tt).play();
    }

    /**
     * Quick-add (+) button subtle rotation animation.
     */
    public static void rotateQuickAdd(Node node) {
        if (node == null) return;
        RotateTransition rt = new RotateTransition(Duration.millis(180), node);
        rt.setByAngle(90);
        rt.setInterpolator(TACTILE_DECEL);
        rt.play();
    }
}
