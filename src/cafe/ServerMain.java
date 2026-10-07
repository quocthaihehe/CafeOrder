package cafe;

import cafe.controllers.ServerController;
import cafe.services.CafeServerService;
import cafe.views.ServerKDSView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ServerMain {

    public static void main(String[] args) {
        cafe.utils.WarningSuppressor.suppress();
        Application.launch(ServerApp.class, args);
    }

    public static class ServerApp extends Application {
        private final CafeServerService serverService = new CafeServerService();

        @Override
        public void start(Stage primaryStage) {
            // Khởi động Server socket ngầm
            serverService.start();

            ServerKDSView kdsView = new ServerKDSView();
            new ServerController(kdsView, serverService);

            Scene scene = new Scene(kdsView, 1180, 750);
            try {
                scene.getStylesheets().add(getClass().getResource("/cafe/views/server.css").toExternalForm());
            } catch (Exception ignored) {}

            primaryStage.setTitle("Quầy Điều Phối Pha Chế (KDS) • L'Amour Artisan Cafe");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(960);
            primaryStage.setMinHeight(600);
            primaryStage.centerOnScreen();

            primaryStage.setOnCloseRequest(e -> {
                serverService.stop();
                System.exit(0);
            });

            primaryStage.show();
        }
    }
}
