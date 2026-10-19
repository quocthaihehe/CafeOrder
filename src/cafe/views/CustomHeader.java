package cafe.views;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

/**
 * CustomHeader - Thanh tiêu đề tùy biến thay thế Title Bar mặc định của OS (Giai đoạn 2):
 * - Chiều cao 36px, màu nền đồng bộ với nền ứng dụng (#F3EFEA hoặc #FFFFFF).
 * - Hỗ trợ kéo thả cửa sổ (Drag & Drop), nhấp đúp để phóng to/thu nhỏ (Maximize/Restore).
 * - Cụm nút điều khiển góc phải: Nút Ẩn (-), Phóng to (□), Đóng (✕) với hiệu ứng hover tinh tế.
 */
public class CustomHeader extends HBox {

    private double xOffset = 0;
    private double yOffset = 0;

    private final Label lblTitle = new Label();
    private final Button btnMinimize = new Button("−");
    private final Button btnMaximize = new Button("□");
    private final Button btnClose = new Button("✕");

    public CustomHeader(Stage stage, String title) {
        this(stage, title, false, null);
    }

    public CustomHeader(Stage stage, String title, boolean isWhiteBackground, Runnable customCloseAction) {
        initUI(stage, title, isWhiteBackground, customCloseAction);
    }

    private void initUI(Stage stage, String title, boolean isWhiteBackground, Runnable customCloseAction) {
        getStyleClass().add("custom-header");
        if (isWhiteBackground) {
            getStyleClass().add("custom-header-white");
        } else {
            getStyleClass().add("custom-header-light");
        }

        setPrefHeight(36);
        setMinHeight(36);
        setMaxHeight(36);
        setAlignment(Pos.CENTER_LEFT);

        // Logo & Tiêu đề cửa sổ
        lblTitle.setText(title != null ? title : "Brew & Bean POS");
        lblTitle.getStyleClass().add("header-title");
        lblTitle.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 600; -fx-text-fill: #78716C;");

        // Khoảng trống co giãn giữa tiêu đề và cụm nút
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Cụm nút điều khiển cửa sổ góc phải
        HBox controlsBox = new HBox(4);
        controlsBox.setAlignment(Pos.CENTER_RIGHT);

        btnMinimize.getStyleClass().addAll("header-btn", "header-btn-min");
        btnMaximize.getStyleClass().addAll("header-btn", "header-btn-max");
        btnClose.getStyleClass().addAll("header-btn", "header-btn-close");

        // Nút Ẩn (-)
        btnMinimize.setOnAction(e -> {
            if (stage != null) {
                stage.setIconified(true);
            }
        });

        // Nút Phóng to / Khôi phục (□ / ❐)
        btnMaximize.setOnAction(e -> {
            if (stage != null) {
                toggleMaximize(stage);
            }
        });

        // Tự động đồng bộ icon phóng to / khôi phục theo trạng thái maximized của Stage
        if (stage != null) {
            stage.maximizedProperty().addListener((obs, wasMax, isNowMax) -> {
                btnMaximize.setText(isNowMax ? "❐" : "□");
            });
            btnMaximize.setText(stage.isMaximized() ? "❐" : "□");
        }

        // Nút Đóng (✕)
        btnClose.setOnAction(e -> {
            if (customCloseAction != null) {
                customCloseAction.run();
            } else if (stage != null) {
                stage.fireEvent(new javafx.stage.WindowEvent(stage, javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST));
            }
        });

        controlsBox.getChildren().addAll(btnMinimize, btnMaximize, btnClose);

        getChildren().addAll(lblTitle, spacer, controlsBox);

        // Bắt sự kiện kéo thả cửa sổ trên toàn bộ thanh header
        setOnMousePressed(event -> {
            if (stage != null && !stage.isMaximized()) {
                xOffset = event.getScreenX() - stage.getX();
                yOffset = event.getScreenY() - stage.getY();
            }
        });

        setOnMouseDragged(event -> {
            if (stage != null && !stage.isMaximized()) {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            }
        });

        // Nhấp đúp chuột vào header để phóng to / thu nhỏ
        setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && stage != null) {
                toggleMaximize(stage);
            }
        });
    }

    private void toggleMaximize(Stage stage) {
        stage.setMaximized(!stage.isMaximized());
    }

    public void setTitle(String title) {
        lblTitle.setText(title);
    }

    public Label getTitleLabel() {
        return lblTitle;
    }

    public Button getMinimizeButton() {
        return btnMinimize;
    }

    public Button getMaximizeButton() {
        return btnMaximize;
    }

    public Button getCloseButton() {
        return btnClose;
    }
}
