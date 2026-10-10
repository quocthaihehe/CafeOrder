package cafe.controllers;

import cafe.services.ClientSocketService;
import cafe.views.ClientView;
import cafe.views.FloorPlanView;
import cafe.views.LoginView;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LoginController {
    private final Stage stage;
    private final LoginView view;
    private final ClientSocketService socketService;

    private int activeTableNumber = 5;

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
            String lower = text.toLowerCase();
            if (lower.contains("nhanvien") || lower.contains("staff")) {
                tableNumber = 5;
            } else if (lower.contains("quanly") || lower.contains("admin") || lower.contains("manager")) {
                tableNumber = 1;
            } else {
                view.showError("Vui lòng nhập số bàn (1..12) hoặc tài khoản nhân viên!");
                return;
            }
        }

        // Kết nối tới Server
        boolean connected = socketService.connect(tableNumber);
        if (connected) {
            this.activeTableNumber = tableNumber;
            // Bước 2 theo chuẩn Báo cáo Audit: Chuyển sang Sơ đồ bàn (FloorPlanView)
            openFloorPlan(tableNumber);
        } else {
            view.showError("Không thể kết nối đến quầy pha chế! Vui lòng kiểm tra lại Server.");
        }
    }

    private void openFloorPlan(int initialTable) {
        this.activeTableNumber = initialTable;

        FloorPlanView floorPlanView = new FloorPlanView(
            activeTableNumber,
            selectedTable -> {
                this.activeTableNumber = selectedTable;
            },
            () -> {
                // Chuyển sang Bước 3: Màn hình Thực đơn Kiosk
                openClientMenu(activeTableNumber);
            },
            () -> {
                // Đăng xuất: ngắt kết nối và quay lại LoginView
                socketService.disconnect();
                returnToLoginView();
            }
        );

        Scene scene = cafe.utils.WindowChromeHelper.createWindowScene(
            stage,
            floorPlanView,
            "Sơ Đồ Bàn • Brew & Bean CAFE",
            1100,
            720,
            true,
            () -> {
                socketService.disconnect();
                System.exit(0);
            }
        );

        stage.setScene(scene);
        stage.setTitle("Sơ Đồ Bàn • Brew & Bean CAFE");
        stage.centerOnScreen();
    }

    private void openClientMenu(int tableNumber) {
        this.activeTableNumber = tableNumber;

        ClientView clientView = new ClientView();
        new ClientController(
            stage,
            clientView,
            socketService,
            tableNumber,
            () -> openFloorPlan(activeTableNumber) // Quay lại sơ đồ bàn
        );

        String menuTitle = "Brew & Bean CAFE • Bàn " + (tableNumber < 10 ? "0" + tableNumber : tableNumber);
        Scene scene = cafe.utils.WindowChromeHelper.createWindowScene(
            stage,
            clientView,
            menuTitle,
            1100,
            720,
            true,
            () -> {
                socketService.disconnect();
                System.exit(0);
            }
        );

        stage.setScene(scene);
        stage.setTitle(menuTitle);
        stage.centerOnScreen();
    }

    private void returnToLoginView() {
        Scene scene = cafe.utils.WindowChromeHelper.createWindowScene(
            stage,
            view,
            "Đăng Nhập • Brew & Bean CAFE",
            740,
            700,
            true,
            () -> {
                socketService.disconnect();
                System.exit(0);
            }
        );

        stage.setScene(scene);
        stage.setTitle("Đăng Nhập • Brew & Bean CAFE");
        stage.setMinWidth(720);
        stage.setMinHeight(650);
        view.setMaximizedMode(stage.isMaximized());
        stage.centerOnScreen();
    }
}
