package cafe;

import cafe.controllers.ServerController;
import cafe.services.CafeServerService;
import cafe.utils.FontLoader;
import cafe.utils.WindowChromeHelper;
import cafe.views.ServerKDSView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
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
            // Giai đoạn 1: Nạp font nhận diện thương hiệu
            FontLoader.loadFonts();

            // Khởi động Server socket ngầm
            serverService.start();

            ServerKDSView kdsView = new ServerKDSView();
            new ServerController(kdsView, serverService);

            // Giao diện Quản trị & Điều phối Quầy Trung tâm (CoffeePOS)
            cafe.views.AdminShellView adminShell = new cafe.views.AdminShellView(serverService, kdsView);

            // Đồng bộ live: khi có đơn hàng mới từ Kiosk/Staff, đẩy vào Dashboard Admin
            serverService.setOnOrderReceived(order -> {
                adminShell.notifyNewOrderArrived(order);
            });

            // Xử lý sự kiện Đăng xuất / Thoát phiên làm việc Quản lý
            adminShell.setOnLogout(() -> {
                Alert alert = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    "Bạn có chắc chắn muốn đăng xuất và đóng phiên làm việc Máy chủ điều phối?\nThao tác này sẽ đóng giao diện quản trị trung tâm và dừng máy chủ an toàn.",
                    ButtonType.YES,
                    ButtonType.NO
                );
                alert.setTitle("Đăng Xuất • Server POS");
                alert.setHeaderText("Xác nhận đóng phiên làm việc Quản trị");
                alert.showAndWait().ifPresent(res -> {
                    if (res == ButtonType.YES) {
                        serverService.stop();
                        primaryStage.close();
                        System.exit(0);
                    }
                });
            });

            javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();

            Scene scene = WindowChromeHelper.createWindowScene(
                primaryStage,
                adminShell,
                "CoffeePOS • Quản Trị & Quầy Điều Phối Trung Tâm (Brew & Bean)",
                bounds.getWidth(),
                bounds.getHeight(),
                true,
                () -> {
                    serverService.stop();
                    System.exit(0);
                }
            );

            primaryStage.setTitle("CoffeePOS • Quản Trị & Quầy Điều Phối Trung Tâm (Brew & Bean)");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1080);
            primaryStage.setMinHeight(700);
            primaryStage.setX(bounds.getMinX());
            primaryStage.setY(bounds.getMinY());
            primaryStage.setWidth(bounds.getWidth());
            primaryStage.setHeight(bounds.getHeight());
            primaryStage.setMaximized(true);

            primaryStage.setOnCloseRequest(e -> {
                serverService.stop();
                System.exit(0);
            });

            primaryStage.show();
            cafe.utils.WindowChromeHelper.applyMaximized(primaryStage);
        }
    }
}
