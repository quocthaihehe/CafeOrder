package cafe;

import cafe.controllers.LoginController;
import cafe.services.ClientSocketService;
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
            LoginView loginView = new LoginView();
            new LoginController(primaryStage, loginView, socketService);

            // Form login mặc định tinh chỉnh theo ảnh login_hero (821x1916 -> cột trái 300px + cột phải 440px)
            Scene scene = new Scene(loginView, 740, 700);
            try {
                scene.getStylesheets().add(getClass().getResource("/cafe/resources/css/cafe-theme.css").toExternalForm());
                scene.getStylesheets().add(getClass().getResource("/cafe/views/client.css").toExternalForm());
            } catch (Exception ignored) {}

            primaryStage.setTitle("Đăng Nhập • Brew & Bean CAFE");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(720);
            primaryStage.setMinHeight(650);
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
        }
    }
}
