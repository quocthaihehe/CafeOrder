package cafe.controllers;

import cafe.services.ClientSocketService;
import cafe.views.ClientView;
import cafe.views.LoginView;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LoginController {
    private final Stage stage;
    private final LoginView view;
    private final ClientSocketService socketService;

    public LoginController(Stage stage, LoginView view, ClientSocketService socketService) {
        this.stage = stage;
        this.view = view;
        this.socketService = socketService;

        init();
    }

    private void init() {
        view.getLoginButton().setOnAction(e -> handleLogin());
        view.getTableField().setOnAction(e -> handleLogin());
    }

    private void handleLogin() {
        view.clearError();
        String text = view.getTableField().getText().trim();
        if (text.isEmpty()) {
            view.showError("Vui lòng nhập số bàn của bạn!");
            return;
        }

        int tableNumber;
        try {
            tableNumber = Integer.parseInt(text);
            if (tableNumber <= 0) {
                view.showError("Số bàn phải lớn hơn 0!");
                return;
            }
        } catch (NumberFormatException e) {
            view.showError("Số bàn không hợp lệ! Vui lòng chỉ nhập số.");
            return;
        }

        // Kết nối tới Server
        boolean connected = socketService.connect(tableNumber);
        if (connected) {
            // Mở màn hình Kiosk đặt món chính
            ClientView clientView = new ClientView();
            new ClientController(stage, clientView, socketService, tableNumber);

            Scene scene = new Scene(clientView, 1100, 720);
            try {
                scene.getStylesheets().add(getClass().getResource("/cafe/views/client.css").toExternalForm());
            } catch (Exception ignored) {}

            stage.setScene(scene);
            stage.setTitle("L'Amour Artisan Cafe • Bàn " + tableNumber);
            stage.centerOnScreen();
        } else {
            view.showError("Không thể kết nối đến quầy pha chế! Vui lòng kiểm tra lại Server.");
        }
    }
}
