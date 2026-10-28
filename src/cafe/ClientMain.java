package cafe;

import cafe.controllers.LoginController;
import cafe.services.ClientSocketService;
import cafe.utils.FontLoader;
import cafe.utils.WarningSuppressor;
import cafe.utils.WindowChromeHelper;
import cafe.views.LoginView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientMain {

    public static void main(String[] args) {
        try {
            WarningSuppressor.suppress();
        } catch (Throwable ignored) {}
        Application.launch(ClientApp.class, args);
    }

    public static class ClientApp extends Application {
        private final ClientSocketService socketService = new ClientSocketService();

        @Override
        public void start(Stage primaryStage) {
            // Giai đoạn 1: Nạp font chữ nhận diện thương hiệu (Playfair Display & Inter)
            FontLoader.loadFonts();

            LoginView loginView = new LoginView();
            new LoginController(primaryStage, loginView, socketService);

            javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();

            // Tạo Scene hoàn chỉnh toàn màn hình (Maximized). Cửa sổ chuẩn OS cho phép đa nhiệm mượt mà
            Scene scene = WindowChromeHelper.createWindowScene(
                primaryStage,
                loginView,
                "Đăng Nhập • Brew & Bean CAFE",
                bounds.getWidth(),
                bounds.getHeight(),
                true,
                () -> {
                    socketService.disconnect();
                    System.exit(0);
                }
            );

            primaryStage.setTitle("Đăng Nhập • Brew & Bean CAFE");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(740);
            primaryStage.setMinHeight(660);
            primaryStage.setX(bounds.getMinX());
            primaryStage.setY(bounds.getMinY());
            primaryStage.setWidth(bounds.getWidth());
            primaryStage.setHeight(bounds.getHeight());
            primaryStage.setMaximized(true);

            // Lắng nghe phóng to form (Maximize) để chuyển sang login_hero2
            primaryStage.maximizedProperty().addListener((obs, wasMax, isNowMax) -> {
                loginView.setMaximizedMode(isNowMax);
            });

            primaryStage.setOnCloseRequest(e -> {
                socketService.disconnect();
                System.exit(0);
            });

            primaryStage.show();
            WindowChromeHelper.applyMaximized(primaryStage);
            loginView.setMaximizedMode(true);

            // Khởi động ngầm kết nối Socket, bộ nhớ đệm hình ảnh 69 món và kết nối DB ngay tại màn hình Login
            socketService.warmupConnectAsync();
            cafe.utils.ImageManager.warmupCacheAsync();
            cafe.services.DatabaseManager.warmupAsync();
        }
    }
}
