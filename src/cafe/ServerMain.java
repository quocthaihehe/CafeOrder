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

            // Giao diện Quản trị & Điều phối Quầy Trung tâm (CoffeePOS theo ảnh nhaminh-coffee.vn/quay)
            cafe.views.AdminShellView adminShell = new cafe.views.AdminShellView(serverService, kdsView);

            // Đồng bộ live: khi có đơn hàng mới từ Kiosk/Staff, đẩy vào Dashboard Admin
            serverService.setOnOrderReceived(order -> {
                adminShell.notifyNewOrderArrived(order);
            });

            Scene scene = WindowChromeHelper.createWindowScene(
                primaryStage,
                adminShell,
                "CoffeePOS • Quản Trị & Quầy Điều Phối Trung Tâm (Brew & Bean)",
                1200,
                760,
                true,
                () -> {
                    serverService.stop();
                    System.exit(0);
                }
            );

            primaryStage.setTitle("CoffeePOS • Quản Trị & Quầy Điều Phối Trung Tâm");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(650);
            primaryStage.centerOnScreen();

            primaryStage.setOnCloseRequest(e -> {
                serverService.stop();
                System.exit(0);
            });

            primaryStage.setMaximized(true);
            primaryStage.show();
            primaryStage.setMaximized(true);
        }
    }
}
