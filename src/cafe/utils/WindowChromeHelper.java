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

        Scene scene = new Scene(root, width, height);
        applyTheme(scene);
        return scene;
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
