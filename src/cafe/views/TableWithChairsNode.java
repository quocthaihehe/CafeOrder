package cafe.views;

import cafe.utils.AnimationHelper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * TableWithChairsNode - Component sơ đồ bàn Composite 2 tầng (Chairs Layer + Table Surface)
 * chuẩn 100% pixel-perfect theo Phan_tich_CafeOrderv3.md & Screen 2 (UI_mong_muon.png):
 *
 * 1. Tầng nền (Chairs Layer):
 *    - Các khối ghế hình viên thuốc (-fx-background-radius: 999px; -fx-background-color: #DDD4C8;)
 *    - Lùi ra sau mặt bàn một khoảng hợp lý, ôm tự nhiên vào 4 cạnh bàn vuông hoặc 6 vị trí bàn dài.
 *
 * 2. Tầng trên (Table Surface):
 *    - Mặt bàn nằm chính giữa, bo góc tròn 14px, bóng đổ kép Neumorphism / Soft UI:
 *      + Bàn trống: Nền trắng #FFFFFF, bóng mềm rgba(0,0,0,0.08), chữ số đậm màu #2C2623.
 *      + Bàn đang chọn / phục vụ: Nền xanh đen #172E27, bóng xanh sâu rgba(23,46,39,0.35),
 *        chữ trắng tinh, có chấm cam nhỏ #E2943B ở góc trên bên trái.
 *      + Bàn đặt trước (RESERVED): Nền trắng #FFFFFF, bóng mềm, chấm xanh lá #86A88A.
 *
 * 3. Hiệu ứng tương tác:
 *    - Micro-lift hover tương tác: Khi rê chuột vào bàn, mặt bàn hơi nhấc lên (translateY(-2px))
 *      và tăng nhẹ độ lan của DropShadow.
 *    - Click event & Double-click event mượt mà.
 */
public class TableWithChairsNode extends StackPane {

    private final int tableNumber;
    private final boolean isLarge;
    private final String labelText;
    private final StackPane tableSurface = new StackPane();
    private final Label lblNumber = new Label();
    private final Circle statusDot = new Circle(3.2);

    private boolean currentSelected = false;
    private String currentStatus = "EMPTY";

    public TableWithChairsNode(int tableNumber, String labelText, boolean isSelected, String status, boolean isLarge, Runnable onSelect, Runnable onDoubleClick) {
        this.tableNumber = tableNumber;
        this.labelText = labelText;
        this.isLarge = isLarge;
        this.currentSelected = isSelected;
        this.currentStatus = status != null ? status : "EMPTY";

        initComponent(onSelect, onDoubleClick);
    }

    private void initComponent(Runnable onSelect, Runnable onDoubleClick) {
        double tableW = isLarge ? 144 : 68;
        double tableH = 58;
        double totalW = tableW + 16; // 84 or 160
        double totalH = tableH + 16; // 74

        setPrefSize(totalW, totalH);
        setMinSize(totalW, totalH);
        setMaxSize(totalW, totalH);
        setAlignment(Pos.CENTER);

        // =====================================================================
        // TẦNG 1: CHAIRS LAYER (Khối ghế hình viên thuốc #DDD4C8 ôm quanh bàn)
        // =====================================================================
        Pane chairsLayer = new Pane();
        chairsLayer.setPrefSize(totalW, totalH);
        chairsLayer.setMinSize(totalW, totalH);
        chairsLayer.setMaxSize(totalW, totalH);
        chairsLayer.setPickOnBounds(false);

        if (!isLarge) {
            // 4 ghế xung quanh bàn vuông
            // Ghế Trên & Dưới (nằm ngang, 34px x 12px)
            Region chairTop = createPillChair(34, 12);
            chairTop.setLayoutX((totalW - 34) / 2.0);
            chairTop.setLayoutY(2);

            Region chairBottom = createPillChair(34, 12);
            chairBottom.setLayoutX((totalW - 34) / 2.0);
            chairBottom.setLayoutY(totalH - 12 - 2);

            // Ghế Trái & Phải (nằm dọc, 12px x 30px)
            Region chairLeft = createPillChair(12, 30);
            chairLeft.setLayoutX(2);
            chairLeft.setLayoutY((totalH - 30) / 2.0);

            Region chairRight = createPillChair(12, 30);
            chairRight.setLayoutX(totalW - 12 - 2);
            chairRight.setLayoutY((totalH - 30) / 2.0);

            chairsLayer.getChildren().addAll(chairTop, chairBottom, chairLeft, chairRight);
        } else {
            // 6 ghế xung quanh bàn dài (2 trên, 2 dưới, 1 trái, 1 phải)
            double gapX = (tableW - (36 * 2)) / 3.0;

            Region chairTop1 = createPillChair(36, 12);
            chairTop1.setLayoutX(8 + gapX);
            chairTop1.setLayoutY(2);

            Region chairTop2 = createPillChair(36, 12);
            chairTop2.setLayoutX(8 + gapX * 2 + 36);
            chairTop2.setLayoutY(2);

            Region chairBottom1 = createPillChair(36, 12);
            chairBottom1.setLayoutX(8 + gapX);
            chairBottom1.setLayoutY(totalH - 12 - 2);

            Region chairBottom2 = createPillChair(36, 12);
            chairBottom2.setLayoutX(8 + gapX * 2 + 36);
            chairBottom2.setLayoutY(totalH - 12 - 2);

            Region chairLeft = createPillChair(12, 30);
            chairLeft.setLayoutX(2);
            chairLeft.setLayoutY((totalH - 30) / 2.0);

            Region chairRight = createPillChair(12, 30);
            chairRight.setLayoutX(totalW - 12 - 2);
            chairRight.setLayoutY((totalH - 30) / 2.0);

            chairsLayer.getChildren().addAll(chairTop1, chairTop2, chairBottom1, chairBottom2, chairLeft, chairRight);
        }

        // =====================================================================
        // TẦNG 2: TABLE SURFACE (Mặt bàn chính giữa, bo tròn 14px, bóng sâu 3D)
        // =====================================================================
        tableSurface.setPrefSize(tableW, tableH);
        tableSurface.setMinSize(tableW, tableH);
        tableSurface.setMaxSize(tableW, tableH);
        tableSurface.setAlignment(Pos.CENTER);
        tableSurface.setCursor(javafx.scene.Cursor.HAND);

        lblNumber.setText(labelText);
        lblNumber.setAlignment(Pos.CENTER);

        StackPane.setAlignment(statusDot, Pos.TOP_LEFT);
        StackPane.setMargin(statusDot, new Insets(8, 0, 0, 8));

        tableSurface.getChildren().addAll(lblNumber, statusDot);

        applySurfaceStyle(false);

        // Hiệu ứng Hover Micro-Lift & Bóng tương tác
        tableSurface.setOnMouseEntered(e -> applySurfaceStyle(true));
        tableSurface.setOnMouseExited(e -> applySurfaceStyle(false));

        // Click & Double-click events
        tableSurface.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                if (onDoubleClick != null) onDoubleClick.run();
            } else {
                if (onSelect != null) onSelect.run();
            }
        });

        getChildren().addAll(chairsLayer, tableSurface);
    }

    private Region createPillChair(double width, double height) {
        Region chair = new Region();
        chair.setPrefSize(width, height);
        chair.setMinSize(width, height);
        chair.setMaxSize(width, height);
        chair.setStyle(
            "-fx-background-color: #DDD4C8; " +
            "-fx-background-radius: 999px; " +
            "-fx-border-color: rgba(200, 190, 178, 0.5); " +
            "-fx-border-radius: 999px; " +
            "-fx-border-width: 0.5px;"
        );
        return chair;
    }

    private void applySurfaceStyle(boolean isHovered) {
        boolean isDark = currentSelected || "SERVING".equalsIgnoreCase(currentStatus) || "OCCUPIED".equalsIgnoreCase(currentStatus);

        if (isDark) {
            // Bàn đang chọn / phục vụ: Nền xanh đen #172E27, chữ trắng tinh, chấm cam #E2943B
            tableSurface.setStyle(
                "-fx-background-color: #172E27; " +
                "-fx-background-radius: 14px; " +
                "-fx-border-color: #172E27; " +
                "-fx-border-radius: 14px; " +
                "-fx-border-width: 1px;"
            );
            lblNumber.setStyle(
                "-fx-font-family: 'Inter', sans-serif; " +
                "-fx-font-size: " + (isLarge ? "12px" : "14px") + "; " +
                "-fx-font-weight: 700; " +
                "-fx-text-fill: #FFFFFF;"
            );
            statusDot.setFill(Color.web("#E2943B"));
            statusDot.setVisible(true);

            if (isHovered) {
                tableSurface.setTranslateY(-2);
                tableSurface.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.rgb(23, 46, 39, 0.45), 20, 0.22, 0, 8));
            } else {
                tableSurface.setTranslateY(0);
                tableSurface.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.rgb(23, 46, 39, 0.35), 16, 0.18, 0, 6));
            }
        } else {
            // Bàn trống / Bàn khác: Nền trắng #FFFFFF, chữ than #2C2623, bóng mềm 3D
            tableSurface.setStyle(
                "-fx-background-color: #FFFFFF; " +
                "-fx-background-radius: 14px; " +
                "-fx-border-color: rgba(222, 215, 205, 0.7); " +
                "-fx-border-radius: 14px; " +
                "-fx-border-width: 1px;"
            );
            lblNumber.setStyle(
                "-fx-font-family: 'Inter', sans-serif; " +
                "-fx-font-size: " + (isLarge ? "12px" : "14px") + "; " +
                "-fx-font-weight: 700; " +
                "-fx-text-fill: #2C2623;"
            );

            if ("RESERVED".equalsIgnoreCase(currentStatus)) {
                statusDot.setFill(Color.web("#86A88A"));
                statusDot.setVisible(true);
            } else {
                statusDot.setVisible(false);
            }

            if (isHovered) {
                tableSurface.setTranslateY(-2);
                tableSurface.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.rgb(0, 0, 0, 0.14), 16, 0.15, 0, 6));
            } else {
                tableSurface.setTranslateY(0);
                tableSurface.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.rgb(0, 0, 0, 0.08), 12, 0.10, 0, 4));
            }
        }
    }

    public void updateStyle(boolean isSelected, String status) {
        this.currentSelected = isSelected;
        if (status != null) this.currentStatus = status;
        applySurfaceStyle(false);
    }

    public int getTableNumber() {
        return tableNumber;
    }

    public StackPane getTableSurface() {
        return tableSurface;
    }
}
