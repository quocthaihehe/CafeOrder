package cafe;

import cafe.controllers.ServerController;
import cafe.services.CafeServerService;
import cafe.utils.FontLoader;
import cafe.utils.WindowChromeHelper;
import cafe.views.ServerKDSView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ServerMain {

    public static void main(String[] args) {
        cafe.utils.WarningSuppressor.suppress();
        Application.launch(ServerApp.class, args);
    }

    public static class ServerApp extends Application {
        private final CafeServerService serverService = new CafeServerService();

        @Override
        public void start(Stage primaryStage) {
            // Giai đoạn 1: Nạp font nhận diện thương hiệu
            FontLoader.loadFonts();

            // Giai đoạn 2: Loại bỏ thanh Title Bar mặc định của OS (StageStyle.UNDECORATED)
            primaryStage.initStyle(StageStyle.UNDECORATED);

            // Khởi động Server socket ngầm
            serverService.start();

            ServerKDSView kdsView = new ServerKDSView();
            new ServerController(kdsView, serverService);

            Scene scene = WindowChromeHelper.createWindowScene(
                primaryStage,
                kdsView,
                "Quầy Điều Phối Pha Chế (KDS) • Brew & Bean CAFE",
                1180,
                750,
                true,
                () -> {
                    serverService.stop();
                    System.exit(0);
                }
            );

            primaryStage.setTitle("Quầy Điều Phối Pha Chế (KDS) • Brew & Bean CAFE");
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
