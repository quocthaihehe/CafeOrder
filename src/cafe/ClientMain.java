package cafe;

import cafe.controllers.LoginController;
import cafe.services.ClientSocketService;
import cafe.utils.FontLoader;
import cafe.utils.WindowChromeHelper;
import cafe.views.LoginView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientMain {

    public static void main(String[] args) {
        cafe.utils.WarningSuppressor.suppress();
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

            // Tạo Scene hoàn chỉnh. Cửa sổ chuẩn OS cho phép thu nhỏ, phóng to, đa nhiệm Taskbar mượt mà
            Scene scene = WindowChromeHelper.createWindowScene(
                primaryStage,
                loginView,
                "Đăng Nhập • Brew & Bean CAFE",
                760,
                700,
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
            primaryStage.setWidth(760);
            primaryStage.setHeight(700);
            primaryStage.centerOnScreen();

            // Lắng nghe phóng to form (Maximize) để chuyển sang login_hero2
            primaryStage.maximizedProperty().addListener((obs, wasMax, isNowMax) -> {
                loginView.setMaximizedMode(isNowMax);
            });

            primaryStage.setOnCloseRequest(e -> {
                socketService.disconnect();
                System.exit(0);
            });

            primaryStage.show();

            // Khởi động ngầm kết nối Socket, bộ nhớ đệm hình ảnh 69 món và kết nối DB ngay tại màn hình Login
            socketService.warmupConnectAsync();
            cafe.utils.ImageManager.warmupCacheAsync();
            cafe.services.DatabaseManager.warmupAsync();
        }
    }
}
