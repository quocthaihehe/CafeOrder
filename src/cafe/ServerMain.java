package cafe;

import cafe.controllers.ServerController;
import cafe.services.CafeServerService;
import cafe.utils.AnimationHelper;
import cafe.utils.FontLoader;
import cafe.utils.IconHelper;
import cafe.utils.SvgIcons;
import cafe.utils.WindowChromeHelper;
import cafe.views.ServerKDSView;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
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
            // 1. Nạp font nhận diện thương hiệu
            FontLoader.loadFonts();

            // 2. Khởi động Server Socket trung tâm
            serverService.start();

            // 3. Giao diện Quầy Điều Phối Pha Chế Trung Tâm (Server KDS toàn màn hình)
            ServerKDSView kdsView = new ServerKDSView();
            new ServerController(kdsView, serverService);

            javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();

            Runnable confirmExitAction = () -> showExitServerConfirmModal(primaryStage);

            Scene scene = WindowChromeHelper.createWindowScene(
                primaryStage,
                kdsView,
                "Brew & Bean CAFE • Quầy Điều Phối Pha Chế Trung Tâm (Server KDS)",
                bounds.getWidth(),
                bounds.getHeight(),
                true,
                confirmExitAction
            );

            primaryStage.setTitle("Brew & Bean CAFE • Quầy Điều Phối Pha Chế Trung Tâm (Server KDS)");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1020);
            primaryStage.setMinHeight(680);
            primaryStage.setX(bounds.getMinX());
            primaryStage.setY(bounds.getMinY());
            primaryStage.setWidth(bounds.getWidth());
            primaryStage.setHeight(bounds.getHeight());
            primaryStage.setMaximized(true);

            primaryStage.setOnCloseRequest(e -> {
                e.consume();
                confirmExitAction.run();
            });

            primaryStage.show();
            WindowChromeHelper.applyMaximized(primaryStage);
        }

        private void showExitServerConfirmModal(Stage ownerStage) {
            Stage modal = new Stage();
            if (ownerStage != null) modal.initOwner(ownerStage);
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.initStyle(StageStyle.TRANSPARENT);
            modal.setTitle("Xác nhận đóng Máy chủ KDS");

            VBox card = new VBox(16);
            card.setPrefWidth(440);
            card.setPadding(new Insets(24));
            card.setStyle(
                "-fx-background-color: #FFFFFF; " +
                "-fx-background-radius: 16px; " +
                "-fx-border-color: #DFD7CA; " +
                "-fx-border-radius: 16px; " +
                "-fx-border-width: 1px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 24, 0.15, 0, 8);"
            );

            HBox topRow = new HBox(12);
            topRow.setAlignment(Pos.CENTER_LEFT);

            StackPane iconBadge = new StackPane();
            iconBadge.setPrefSize(44, 44);
            iconBadge.setMinSize(44, 44);
            iconBadge.setStyle("-fx-background-color: #FEF2F2; -fx-background-radius: 12px; -fx-border-color: #FECACA; -fx-border-radius: 12px;");
            Node kdsIcon = IconHelper.getIcon("admin", "man-pha-che.png", SvgIcons.COFFEE, "", 22, Color.web("#B91C1C"));
            iconBadge.getChildren().add(kdsIcon);

            VBox titleBox = new VBox(3);
            HBox.setHgrow(titleBox, Priority.ALWAYS);
            Label lblTitle = new Label("Xác nhận dừng Máy chủ Pha Chế KDS?");
            lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #112823;");
            Label lblSub = new Label("Trạm Máy chủ Socket Trung tâm • Port " + serverService.getActiveServerPort());
            lblSub.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #B91C1C;");
            titleBox.getChildren().addAll(lblTitle, lblSub);

            topRow.getChildren().addAll(iconBadge, titleBox);

            Label lblDesc = new Label(
                "Đóng cửa sổ này sẽ dừng Máy chủ Socket trung tâm và ngắt kết nối toàn bộ thiết bị Client (Kiosk Khách, Sơ đồ bàn Nhân viên và Quản trị viên) đang kết nối."
            );
            lblDesc.setWrapText(true);
            lblDesc.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #57534E; -fx-line-spacing: 2px;");

            HBox actions = new HBox(10);
            actions.setAlignment(Pos.CENTER_RIGHT);

            Button btnStay = new Button("Tiếp tục vận hành");
            btnStay.setStyle("-fx-background-color: #F4EFE6; -fx-text-fill: #112823; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 9 16 9 16; -fx-cursor: hand;");
            AnimationHelper.applyButtonPress(btnStay);
            btnStay.setOnAction(e -> modal.close());

            Button btnStop = new Button("Dừng Máy Chủ & Thoát");
            btnStop.setStyle("-fx-background-color: #B91C1C; -fx-text-fill: #FFFFFF; -fx-font-size: 12.5px; -fx-font-weight: 800; -fx-background-radius: 8px; -fx-padding: 9 18 9 18; -fx-cursor: hand;");
            AnimationHelper.applyButtonPress(btnStop);
            btnStop.setOnAction(e -> {
                modal.close();
                serverService.stop();
                if (ownerStage != null) ownerStage.close();
                System.exit(0);
            });

            actions.getChildren().addAll(btnStay, btnStop);
            card.getChildren().addAll(topRow, lblDesc, actions);

            WindowChromeHelper.makeDraggable(modal, card);
            StackPane wrap = new StackPane(card);
            wrap.setPadding(new Insets(16));
            wrap.setStyle("-fx-background-color: transparent;");
            Scene modalScene = new Scene(wrap);
            modalScene.setFill(Color.TRANSPARENT);
            WindowChromeHelper.applyTheme(modalScene);
            modal.setScene(modalScene);
            modal.show();
            AnimationHelper.playDialogEntrance(card);
        }
    }
}
