package cafe.utils;

import cafe.views.CustomHeader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * WindowChromeHelper - Tiện ích chuẩn hóa khung cửa sổ không viền (StageStyle.UNDECORATED):
 * - Tự động gắn CustomHeader vào đỉnh cửa sổ.
 * - Hỗ trợ kéo thả cửa sổ và nhấp đúp để phóng to/thu nhỏ.
 * - Tự động nạp bộ stylesheet toàn cục (theme.css, cafe-theme.css, client.css).
 */
public class WindowChromeHelper {

    private static double dragX = 0;
    private static double dragY = 0;

    /**
     * Tạo Scene hoàn chỉnh bao gồm CustomHeader ở trên và nội dung view ở giữa.
     */
    public static Scene createWindowScene(Stage stage, Parent content, String title, double width, double height, boolean showMaximize, Runnable customClose) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #F3EFEA;");

        CustomHeader header = new CustomHeader(stage, title, false, customClose);
        if (!showMaximize) {
            header.getMaximizeButton().setVisible(false);
            header.getMaximizeButton().setManaged(false);
        }

        root.setTop(header);
        root.setCenter(content);

        double actualWidth = width;
        double actualHeight = height;
        if (stage != null && stage.isMaximized()) {
            try {
                javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
                actualWidth = bounds.getWidth();
                actualHeight = bounds.getHeight();
            } catch (Exception ignored) {}
        }

        Scene scene = new Scene(root, actualWidth, actualHeight);
        applyTheme(scene);
        return scene;
    }

    /**
     * Cập nhật nội dung cửa sổ mượt mà không làm mất trạng thái Maximized (Toàn màn hình):
     * - Nếu Stage đã có Scene với root là BorderPane, chỉ thay thế Header và Content.
     * - Giữ nguyên Native Window Handle của OS, triệt tiêu hoàn toàn hiện tượng co giật kích thước.
     * - Đảm bảo cửa sổ luôn ở trạng thái Toàn màn hình (Maximized).
     */
    public static void setWindowContent(Stage stage, Parent content, String title, boolean showMaximize, Runnable customClose) {
        if (stage == null) return;

        Scene existingScene = stage.getScene();
        if (existingScene != null && existingScene.getRoot() instanceof BorderPane) {
            BorderPane root = (BorderPane) existingScene.getRoot();
            CustomHeader header = new CustomHeader(stage, title, false, customClose);
            if (!showMaximize) {
                header.getMaximizeButton().setVisible(false);
                header.getMaximizeButton().setManaged(false);
            }
            root.setTop(header);
            root.setCenter(content);
            stage.setTitle(title);
            applyTheme(existingScene);

            applyMaximized(stage);
            return;
        }

        // Nếu chưa có Scene hoặc root không phải BorderPane, tạo mới Scene toàn màn hình
        javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        Scene newScene = createWindowScene(stage, content, title, bounds.getWidth(), bounds.getHeight(), showMaximize, customClose);
        stage.setScene(newScene);
        stage.setTitle(title);
        applyMaximized(stage);
    }

    /**
     * Áp dụng trạng thái Toàn màn hình (Maximized) chuẩn xác trên môi trường Windows không viền (UNDECORATED).
     */
    public static void applyMaximized(Stage stage) {
        if (stage == null) return;
        javafx.application.Platform.runLater(() -> {
            try {
                javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
                stage.setX(bounds.getMinX());
                stage.setY(bounds.getMinY());
                stage.setWidth(bounds.getWidth());
                stage.setHeight(bounds.getHeight());
                stage.setMaximized(true);
            } catch (Exception ignored) {
                stage.setMaximized(true);
            }
        });
    }

    /**
     * Giúp bất kỳ Node hoặc Hộp thoại Dialog nào có thể kéo thả di chuyển trên màn hình.
     */
    public static void makeDraggable(Stage stage, Node dragNode) {
        if (stage == null || dragNode == null) return;

        dragNode.setOnMousePressed(event -> {
            if (!stage.isMaximized()) {
                dragX = event.getScreenX() - stage.getX();
                dragY = event.getScreenY() - stage.getY();
            }
        });

        dragNode.setOnMouseDragged(event -> {
            if (!stage.isMaximized()) {
                stage.setX(event.getScreenX() - dragX);
                stage.setY(event.getScreenY() - dragY);
            }
        });
    }

    /**
     * Nạp toàn bộ CSS phong cách Brew & Bean vào Scene.
     */
    public static void applyTheme(Scene scene) {
        if (scene == null) return;

        String[] stylesheets = {
            "/styles/theme.css",
            "/main/resources/styles/theme.css",
            "/cafe/resources/css/theme.css",
            "/cafe/resources/css/cafe-theme.css",
            "/cafe/views/client.css"
        };

        for (String cssPath : stylesheets) {
            try {
                java.net.URL url = WindowChromeHelper.class.getResource(cssPath);
                if (url != null) {
                    String externalForm = url.toExternalForm();
                    if (!scene.getStylesheets().contains(externalForm)) {
                        scene.getStylesheets().add(externalForm);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }
}
