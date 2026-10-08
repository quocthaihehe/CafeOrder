package cafe.views;

import cafe.utils.AnimationHelper;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * TableWithChairsNode - Component bàn ghế chuẩn bản vẽ mặt bằng kiến trúc 2.5D (Top-down Layout)
 * theo đúng Screen 2 trong UI_mong_muon.png:
 * - Mặt bàn trung tâm: 52x42px (bàn vuông/nhỏ) hoặc 120x42px (bàn dài), bo góc 8px
 * - Bốn phía xung quanh là các khối ghế (Chair Blocks): màu #E8DED0, bo góc 3px
 * - Bàn được chọn (Bàn 05): Đổi màu sang Deep Green #112823, chữ trắng, góc trên bên trái có chấm cam #E5A04D
 */
public class TableWithChairsNode extends StackPane {

    private final Button btnSurface = new Button();
    private final int tableNumber;
    private final boolean isLarge;

    public TableWithChairsNode(int tableNumber, String labelText, boolean isSelected, String status, boolean isLarge, Runnable onSelect, Runnable onDoubleClick) {
        this.tableNumber = tableNumber;
        this.isLarge = isLarge;

        double tableW = isLarge ? 120 : 52;
        double tableH = 42;
        double totalW = tableW + 24;
        double totalH = tableH + 20;

        setPrefSize(totalW, totalH);
        setMinSize(totalW, totalH);
        setMaxSize(totalW, totalH);

        // 1. Tạo các khối ghế bao quanh (#E8DED0, radius 3px)
        if (!isLarge) {
            // 4 ghế: Trên, Dưới, Trái, Phải
            Region chairTop = createChairBlock(28, 6);
            StackPane.setAlignment(chairTop, Pos.TOP_CENTER);

            Region chairBottom = createChairBlock(28, 6);
            StackPane.setAlignment(chairBottom, Pos.BOTTOM_CENTER);

            Region chairLeft = createChairBlock(6, 22);
            StackPane.setAlignment(chairLeft, Pos.CENTER_LEFT);

            Region chairRight = createChairBlock(6, 22);
            StackPane.setAlignment(chairRight, Pos.CENTER_RIGHT);

            getChildren().addAll(chairTop, chairBottom, chairLeft, chairRight);
        } else {
            // Bàn dài: Ghế đôi trên và dưới, ghế 2 bên
            Region chairTopLeft = createChairBlock(36, 6);
            StackPane.setAlignment(chairTopLeft, Pos.TOP_LEFT);
            chairTopLeft.setTranslateX(18);

            Region chairTopRight = createChairBlock(36, 6);
            StackPane.setAlignment(chairTopRight, Pos.TOP_RIGHT);
            chairTopRight.setTranslateX(-18);

            Region chairBottomLeft = createChairBlock(36, 6);
            StackPane.setAlignment(chairBottomLeft, Pos.BOTTOM_LEFT);
            chairBottomLeft.setTranslateX(18);

            Region chairBottomRight = createChairBlock(36, 6);
            StackPane.setAlignment(chairBottomRight, Pos.BOTTOM_RIGHT);
            chairBottomRight.setTranslateX(-18);

            Region chairLeft = createChairBlock(6, 24);
            StackPane.setAlignment(chairLeft, Pos.CENTER_LEFT);

            Region chairRight = createChairBlock(6, 24);
            StackPane.setAlignment(chairRight, Pos.CENTER_RIGHT);

            getChildren().addAll(chairTopLeft, chairTopRight, chairBottomLeft, chairBottomRight, chairLeft, chairRight);
        }

        // 2. Mặt bàn trung tâm
        btnSurface.setPrefSize(tableW, tableH);
        btnSurface.setMinSize(tableW, tableH);
        btnSurface.setMaxSize(tableW, tableH);
        btnSurface.setText(labelText);
        StackPane.setAlignment(btnSurface, Pos.CENTER);
        AnimationHelper.applyButtonPress(btnSurface);

        updateStyle(isSelected, status);

        // 3. Chấm cam phục vụ góc trên bên trái nếu bàn đang phục vụ hoặc đang chọn
        if ("SERVING".equalsIgnoreCase(status) || isSelected) {
            Circle orangeDot = new Circle(3.2, Color.web("#E5A04D"));
            StackPane.setAlignment(orangeDot, Pos.CENTER);
            orangeDot.setTranslateX(-(tableW / 2) + 9);
            orangeDot.setTranslateY(-(tableH / 2) + 9);
            getChildren().addAll(btnSurface, orangeDot);
        } else if ("RESERVED".equalsIgnoreCase(status)) {
            Circle greenDot = new Circle(3.2, Color.web("#86A88A"));
            StackPane.setAlignment(greenDot, Pos.CENTER);
            greenDot.setTranslateX(-(tableW / 2) + 9);
            greenDot.setTranslateY(-(tableH / 2) + 9);
            getChildren().addAll(btnSurface, greenDot);
        } else {
            getChildren().add(btnSurface);
        }

        // Click events
        btnSurface.setOnAction(e -> {
            if (onSelect != null) onSelect.run();
        });

        btnSurface.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && onDoubleClick != null) {
                onDoubleClick.run();
            }
        });
    }

    private Region createChairBlock(double w, double h) {
        Region r = new Region();
        r.setPrefSize(w, h);
        r.setMinSize(w, h);
        r.setMaxSize(w, h);
        r.setStyle("-fx-background-color: #E8DED0; -fx-background-radius: 3px;");
        return r;
    }

    public void updateStyle(boolean isSelected, String status) {
        if (isSelected) {
            btnSurface.setStyle(
                "-fx-background-color: #112823; " +
                "-fx-text-fill: #FFFFFF; " +
                "-fx-font-size: " + (isLarge ? "11.5px" : "13px") + "; " +
                "-fx-font-weight: 700; " +
                "-fx-background-radius: 8px; " +
                "-fx-border-color: #112823; " +
                "-fx-border-radius: 8px; " +
                "-fx-border-width: 1px; " +
                "-fx-cursor: hand;"
            );
        } else {
            btnSurface.setStyle(
                "-fx-background-color: #FBF9F5; " +
                "-fx-text-fill: #292722; " +
                "-fx-font-size: " + (isLarge ? "11.5px" : "13px") + "; " +
                "-fx-font-weight: 600; " +
                "-fx-background-radius: 8px; " +
                "-fx-border-color: #DED7CD; " +
                "-fx-border-radius: 8px; " +
                "-fx-border-width: 1px; " +
                "-fx-cursor: hand;"
            );
        }
    }

    public int getTableNumber() {
        return tableNumber;
    }

    public Button getSurfaceButton() {
        return btnSurface;
    }
}
