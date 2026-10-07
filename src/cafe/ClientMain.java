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

            Scene scene = new Scene(loginView, 540, 520);
            try {
                scene.getStylesheets().add(getClass().getResource("/cafe/views/client.css").toExternalForm());
            } catch (Exception ignored) {}

            primaryStage.setTitle("Đăng Nhập Bàn • L'Amour Artisan Cafe");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(480);
            primaryStage.setMinHeight(480);
            primaryStage.centerOnScreen();

            primaryStage.setOnCloseRequest(e -> {
                socketService.disconnect();
                System.exit(0);
            });

            primaryStage.show();
        }
    }
}
