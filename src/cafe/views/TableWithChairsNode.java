package cafe.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * TableWithChairsNode - Component sơ đồ bàn Composite 2 tầng (Chairs Layer + Table Surface)
 * Phân tách rõ 5 trạng thái thị giác (Sprint 4.1):
 * 1. EMPTY (Trống): Nền trắng #FFFFFF, viền #D8CEBE, chấm xanh #16A34A.
 * 2. SELECTED + EMPTY (Đang chọn bàn trống): Giữ nền sáng #FCFBF8, viền đậm #112823 (2.5px) + vòng sáng focus, không bị đổi sang màu đen của bàn có khách.
 * 3. SERVING / OCCUPIED (Đang phục vụ): Nền xanh rừng đậm #172E27, chữ trắng, chấm vàng đồng #C8965A, hiển thị tổng tiền / thời gian.
 * 4. RESERVED (Đặt trước): Nền kem ấm #FFFBEB, viền cam hổ phách #F59E0B, chữ nâu #92400E.
 * 5. CLEANING (Chờ dọn bàn): Nền hồng nhạt #FEF2F2, viền đỏ #EF4444.
 */
public class TableWithChairsNode extends StackPane {

    private final int tableNumber;
    private final boolean isLarge;
    private final String labelText;
    private final StackPane tableSurface = new StackPane();
    private final Label lblNumber = new Label();
    private final Label lblStatusBadge = new Label();
    private final Label lblMeta = new Label();
    private final Circle statusDot = new Circle(3.5);

    private boolean currentSelected = false;
    private String currentStatus = "EMPTY";
    private String customMetaText = "";
    private String orderProgressStatus = "";

    public TableWithChairsNode(int tableNumber, String labelText, boolean isSelected, String status, boolean isLarge, Runnable onSelect, Runnable onDoubleClick) {
        this.tableNumber = tableNumber;
        this.labelText = labelText;
        this.isLarge = isLarge;
        this.currentSelected = isSelected;
        this.currentStatus = status != null ? status : "EMPTY";

        initComponent(onSelect, onDoubleClick);
    }

    private void initComponent(Runnable onSelect, Runnable onDoubleClick) {
        double tableW = isLarge ? 210 : 115;
        double tableH = isLarge ? 95 : 85;
        double totalW = tableW + 24;
        double totalH = tableH + 24;

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
            Region chairTop = createPillChair(50, 16);
            chairTop.setLayoutX((totalW - 50) / 2.0);
            chairTop.setLayoutY(2);

            Region chairBottom = createPillChair(50, 16);
            chairBottom.setLayoutX((totalW - 50) / 2.0);
            chairBottom.setLayoutY(totalH - 16 - 2);

            Region chairLeft = createPillChair(16, 44);
            chairLeft.setLayoutX(2);
            chairLeft.setLayoutY((totalH - 44) / 2.0);

            Region chairRight = createPillChair(16, 44);
            chairRight.setLayoutX(totalW - 16 - 2);
            chairRight.setLayoutY((totalH - 44) / 2.0);

            chairsLayer.getChildren().addAll(chairTop, chairBottom, chairLeft, chairRight);
        } else {
            double gapX = (tableW - 100) / 3.0;

            Region chairTop1 = createPillChair(50, 16);
            chairTop1.setLayoutX(12 + gapX);
            chairTop1.setLayoutY(2);

            Region chairTop2 = createPillChair(50, 16);
            chairTop2.setLayoutX(12 + gapX * 2 + 50);
            chairTop2.setLayoutY(2);

            Region chairBottom1 = createPillChair(50, 16);
            chairBottom1.setLayoutX(12 + gapX);
            chairBottom1.setLayoutY(totalH - 16 - 2);

            Region chairBottom2 = createPillChair(50, 16);
            chairBottom2.setLayoutX(12 + gapX * 2 + 50);
            chairBottom2.setLayoutY(totalH - 16 - 2);

            Region chairLeft = createPillChair(16, 48);
            chairLeft.setLayoutX(2);
            chairLeft.setLayoutY((totalH - 48) / 2.0);

            Region chairRight = createPillChair(16, 48);
            chairRight.setLayoutX(totalW - 16 - 2);
            chairRight.setLayoutY((totalH - 48) / 2.0);

            chairsLayer.getChildren().addAll(chairTop1, chairTop2, chairBottom1, chairBottom2, chairLeft, chairRight);
        }

        // =====================================================================
        // TẦNG 2: TABLE SURFACE (Mặt bàn chính giữa, bo tròn 14px, 5 trạng thái)
        // =====================================================================
        tableSurface.setPrefSize(tableW, tableH);
        tableSurface.setMinSize(tableW, tableH);
        tableSurface.setMaxSize(tableW, tableH);
        tableSurface.setAlignment(Pos.CENTER);
        tableSurface.setCursor(javafx.scene.Cursor.HAND);

        lblNumber.setText(labelText);
        lblNumber.setAlignment(Pos.CENTER);

        lblStatusBadge.setAlignment(Pos.CENTER);
        lblMeta.setAlignment(Pos.CENTER);

        VBox centerLabels = new VBox(1, lblNumber, lblStatusBadge, lblMeta);
        centerLabels.setAlignment(Pos.CENTER);
        centerLabels.setMouseTransparent(true);

        StackPane.setAlignment(statusDot, Pos.TOP_LEFT);
        StackPane.setMargin(statusDot, new Insets(7, 0, 0, 8));

        tableSurface.getChildren().addAll(centerLabels, statusDot);

        applySurfaceStyle(false);

        tableSurface.setOnMouseEntered(e -> applySurfaceStyle(true));
        tableSurface.setOnMouseExited(e -> applySurfaceStyle(false));

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
            "-fx-border-color: rgba(200, 190, 178, 0.55); " +
            "-fx-border-radius: 999px; " +
            "-fx-border-width: 0.5px;"
        );
        return chair;
    }

    private void applySurfaceStyle(boolean isHovered) {
        boolean isServing = "SERVING".equalsIgnoreCase(currentStatus) || "OCCUPIED".equalsIgnoreCase(currentStatus);
        boolean isReserved = "RESERVED".equalsIgnoreCase(currentStatus);
        boolean isCleaning = "CLEANING".equalsIgnoreCase(currentStatus);

        if (isServing) {
            // TRẠNG THÁI 3: ĐANG PHỤC VỤ (SERVING / OCCUPIED) - Nền xanh đậm #172E27
            String borderColor = currentSelected ? "#C8965A" : "#172E27";
            String borderWidth = currentSelected ? "2.5px" : "1.2px";
            tableSurface.setStyle(
                "-fx-background-color: #172E27; " +
                "-fx-background-radius: 16px; " +
                "-fx-border-color: " + borderColor + "; " +
                "-fx-border-radius: 16px; " +
                "-fx-border-width: " + borderWidth + ";"
            );
            lblNumber.setStyle(
                "-fx-font-family: 'Inter', sans-serif; " +
                "-fx-font-size: " + (isLarge ? "14.5px" : "15px") + "; " +
                "-fx-font-weight: 800; " +
                "-fx-text-fill: #FFFFFF;"
            );
            String kdsSt = orderProgressStatus != null ? orderProgressStatus.toUpperCase() : "";
            String statusText;
            String badgeColor;
            String dotHex;
            if ("QUEUED".equals(kdsSt) || "CREATED".equals(kdsSt) || "CONFIRMED".equals(kdsSt)) {
                statusText = "Chờ pha";
                badgeColor = "#FCD34D";
                dotHex = "#F59E0B";
            } else if ("PREPARING".equals(kdsSt)) {
                statusText = "Đang pha";
                badgeColor = "#93C5FD";
                dotHex = "#38BDF8";
            } else if ("DONE".equals(kdsSt) || "READY".equals(kdsSt) || "SERVED".equals(kdsSt)) {
                statusText = "Đang dùng";
                badgeColor = "#86EFAC";
                dotHex = "#22C55E";
            } else {
                statusText = "Đang dùng";
                badgeColor = "#E8C798";
                dotHex = "#E2943B";
            }
            lblStatusBadge.setText(statusText);
            lblStatusBadge.setStyle(
                "-fx-font-family: 'Inter', sans-serif; " +
                "-fx-font-size: 10.5px; " +
                "-fx-font-weight: 800; " +
                "-fx-text-fill: " + badgeColor + ";"
            );
            String meta = (customMetaText != null && !customMetaText.isEmpty()) ? customMetaText : "Có khách";
            lblMeta.setText(meta);
            lblMeta.setVisible(true);
            lblMeta.setManaged(true);
            lblMeta.setStyle(
                "-fx-font-family: 'Inter', sans-serif; " +
                "-fx-font-size: 10px; " +
                "-fx-font-weight: 600; " +
                "-fx-text-fill: #E2D9CC;"
            );
            statusDot.setFill(Color.web(dotHex));
            statusDot.setVisible(true);

            if (isHovered || currentSelected) {
                tableSurface.setTranslateY(isHovered ? -2 : 0);
                tableSurface.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.rgb(23, 46, 39, 0.45), 18, 0.22, 0, 6));
            } else {
                tableSurface.setTranslateY(0);
                tableSurface.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.rgb(23, 46, 39, 0.30), 14, 0.16, 0, 5));
            }
        } else if (isReserved) {
            // TRẠNG THÁI 4: ĐẶT TRƯỚC SẮP ĐẾN GIỜ (RESERVED <= 60 PHÚT) - Nền kem hổ phách #FFFBEB
            String borderColor = currentSelected ? "#B45309" : "#F59E0B";
            String borderWidth = currentSelected ? "2.5px" : "1.5px";
            tableSurface.setStyle(
                "-fx-background-color: #FFFBEB; " +
                "-fx-background-radius: 16px; " +
                "-fx-border-color: " + borderColor + "; " +
                "-fx-border-radius: 16px; " +
                "-fx-border-width: " + borderWidth + ";"
            );
            lblNumber.setStyle(
                "-fx-font-family: 'Inter', sans-serif; " +
                "-fx-font-size: " + (isLarge ? "14.5px" : "15px") + "; " +
                "-fx-font-weight: 800; " +
                "-fx-text-fill: #92400E;"
            );
            lblStatusBadge.setText("Giữ bàn hẹn");
            lblStatusBadge.setStyle(
                "-fx-font-family: 'Inter', sans-serif; " +
                "-fx-font-size: 10.5px; " +
                "-fx-font-weight: 800; " +
                "-fx-text-fill: #B45309;"
            );
            String meta = (customMetaText != null && !customMetaText.isEmpty()) ? customMetaText : "Sắp đến giờ";
            lblMeta.setText(meta);
            lblMeta.setVisible(true);
            lblMeta.setManaged(true);
            lblMeta.setStyle(
                "-fx-font-family: 'Inter', sans-serif; " +
                "-fx-font-size: 10px; " +
                "-fx-font-weight: 700; " +
                "-fx-text-fill: #92400E;"
            );
            statusDot.setFill(Color.web("#F59E0B"));
            statusDot.setVisible(true);

            tableSurface.setTranslateY(isHovered ? -2 : 0);
            tableSurface.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.rgb(180, 83, 9, currentSelected ? 0.22 : 0.10), 14, 0.14, 0, 4));
        } else if (isCleaning) {
            // TRẠNG THÁI 5: CHỜ DỌN BÀN (CLEANING)
            tableSurface.setStyle(
                "-fx-background-color: #FEF2F2; " +
                "-fx-background-radius: 16px; " +
                "-fx-border-color: " + (currentSelected ? "#B91C1C" : "#EF4444") + "; " +
                "-fx-border-radius: 16px; " +
                "-fx-border-width: " + (currentSelected ? "2.5px" : "1.5px") + ";"
            );
            lblNumber.setStyle(
                "-fx-font-family: 'Inter', sans-serif; " +
                "-fx-font-size: " + (isLarge ? "14.5px" : "15px") + "; " +
                "-fx-font-weight: 800; " +
                "-fx-text-fill: #991B1B;"
            );
            lblStatusBadge.setText("Chờ dọn bàn");
            lblStatusBadge.setStyle(
                "-fx-font-family: 'Inter', sans-serif; " +
                "-fx-font-size: 10.5px; " +
                "-fx-font-weight: 800; " +
                "-fx-text-fill: #DC2626;"
            );
            lblMeta.setVisible(false);
            lblMeta.setManaged(false);
            statusDot.setFill(Color.web("#EF4444"));
            statusDot.setVisible(true);
            tableSurface.setTranslateY(isHovered ? -2 : 0);
            tableSurface.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.rgb(220, 38, 38, 0.14), 12, 0.12, 0, 4));
        } else if (currentSelected) {
            // TRẠNG THÁI 2: ĐANG CHỌN BÀN TRỐNG (SELECTED + EMPTY)
            tableSurface.setStyle(
                "-fx-background-color: #FCFBF8; " +
                "-fx-background-radius: 16px; " +
                "-fx-border-color: #112823; " +
                "-fx-border-radius: 16px; " +
                "-fx-border-width: 2.5px;"
            );
            lblNumber.setStyle(
                "-fx-font-family: 'Inter', sans-serif; " +
                "-fx-font-size: " + (isLarge ? "14.5px" : "15px") + "; " +
                "-fx-font-weight: 800; " +
                "-fx-text-fill: #112823;"
            );
            lblStatusBadge.setText("● Sẵn sàng");
            lblStatusBadge.setStyle(
                "-fx-font-family: 'Inter', sans-serif; " +
                "-fx-font-size: 10.5px; " +
                "-fx-font-weight: 800; " +
                "-fx-text-fill: #16A34A;"
            );
            if (customMetaText != null && !customMetaText.isEmpty()) {
                lblMeta.setText(customMetaText);
                lblMeta.setVisible(true);
                lblMeta.setManaged(true);
                lblMeta.setStyle(
                    "-fx-font-family: 'Inter', sans-serif; " +
                    "-fx-font-size: 9.5px; " +
                    "-fx-font-weight: 700; " +
                    "-fx-text-fill: #B45309;"
                );
            } else {
                lblMeta.setVisible(false);
                lblMeta.setManaged(false);
            }
            statusDot.setFill(Color.web("#16A34A"));
            statusDot.setVisible(true);

            tableSurface.setTranslateY(isHovered ? -2 : 0);
            tableSurface.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.rgb(17, 40, 35, 0.22), 16, 0.20, 0, 5));
        } else {
            // TRẠNG THÁI 1: BÀN TRỐNG (EMPTY) - Nền trắng #FFFFFF, chữ #2C2623
            tableSurface.setStyle(
                "-fx-background-color: #FFFFFF; " +
                "-fx-background-radius: 16px; " +
                "-fx-border-color: #D8CEBE; " +
                "-fx-border-radius: 16px; " +
                "-fx-border-width: 1.2px;"
            );
            lblNumber.setStyle(
                "-fx-font-family: 'Inter', sans-serif; " +
                "-fx-font-size: " + (isLarge ? "14.5px" : "15px") + "; " +
                "-fx-font-weight: 800; " +
                "-fx-text-fill: #2C2623;"
            );
            lblStatusBadge.setText("● Sẵn sàng");
            lblStatusBadge.setStyle(
                "-fx-font-family: 'Inter', sans-serif; " +
                "-fx-font-size: 10.5px; " +
                "-fx-font-weight: 700; " +
                "-fx-text-fill: #16A34A;"
            );
            if (customMetaText != null && !customMetaText.isEmpty()) {
                lblMeta.setText(customMetaText);
                lblMeta.setVisible(true);
                lblMeta.setManaged(true);
                lblMeta.setStyle(
                    "-fx-font-family: 'Inter', sans-serif; " +
                    "-fx-font-size: 9.5px; " +
                    "-fx-font-weight: 700; " +
                    "-fx-text-fill: #B45309;"
                );
            } else {
                lblMeta.setVisible(false);
                lblMeta.setManaged(false);
            }
            statusDot.setFill(Color.web("#16A34A"));
            statusDot.setVisible(true);

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

    public void setLiveSummary(String summaryText) {
        this.customMetaText = summaryText != null ? summaryText : "";
        applySurfaceStyle(false);
    }

    public void setOrderProgressStatus(String orderProgressStatus) {
        this.orderProgressStatus = orderProgressStatus != null ? orderProgressStatus : "";
        applySurfaceStyle(false);
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public int getTableNumber() {
        return tableNumber;
    }

    public StackPane getTableSurface() {
        return tableSurface;
    }
}
