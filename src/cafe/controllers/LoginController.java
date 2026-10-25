package cafe.controllers;

import cafe.models.StaffModel;
import cafe.services.ClientSocketService;
import cafe.services.DatabaseManager;
import cafe.views.ClientView;
import cafe.views.FloorPlanView;
import cafe.views.LoginView;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

public class LoginController {
    private final Stage stage;
    private final LoginView view;
    private final ClientSocketService socketService;

    private int activeTableNumber = 5;
    private Timeline serverPollTimeline;

    public LoginController(Stage stage, LoginView view, ClientSocketService socketService) {
        this.stage = stage;
        this.view = view;
        this.socketService = socketService;

        init();
        initServerStatus();
    }

    private void init() {
        view.getLoginButton().setOnAction(e -> handleLogin());
        view.getTableField().setOnAction(e -> handleLogin());
        view.getPasswordField().setOnAction(e -> handleLogin());
    }

    private void initServerStatus() {
        socketService.setOnConnectionStateChanged(connected -> {
            Platform.runLater(() -> view.setServerStatus(connected, cafe.utils.NetworkConfig.getServerHost(), cafe.utils.NetworkConfig.getServerPort()));
        });

        socketService.setOnReconnectApproved(appMsg -> {
            Platform.runLater(() -> {
                view.clearError();
                view.showInfo("Phiên làm việc đã được Quản trị viên phê duyệt thành công!");
                socketService.resetKickedState();
                socketService.warmupConnectAsync();
                view.setServerStatus(true, cafe.utils.NetworkConfig.getServerHost(), cafe.utils.NetworkConfig.getServerPort());
                view.setRetryButtonText("Thử lại");
                view.getBtnRetryConnection().setDisable(false);
            });
        });

        socketService.setOnReconnectRejected(rejMsg -> {
            Platform.runLater(() -> {
                view.showError("Yêu cầu cấp lại phiên bị từ chối: " + (rejMsg != null && rejMsg.reason != null ? rejMsg.reason : "Không được cấp phép"));
                view.setServerStatus(false, cafe.utils.NetworkConfig.getServerHost(), cafe.utils.NetworkConfig.getServerPort());
                view.setRetryButtonText("Xin cấp lại");
                view.getBtnRetryConnection().setDisable(false);
            });
        });

        view.getBtnRetryConnection().setOnAction(e -> {
            if (socketService.isKickedByAdmin()) {
                view.setRetryButtonText("Đang gửi...");
                view.getBtnRetryConnection().setDisable(true);
                new Thread(() -> {
                    boolean sent = socketService.sendReconnectRequest(
                        "DEVICE-" + (activeTableNumber < 10 ? "0" + activeTableNumber : activeTableNumber),
                        activeTableNumber,
                        "KIOSK",
                        "Thiết bị Bàn " + activeTableNumber + " xin cấp lại phiên làm việc"
                    );
                    Platform.runLater(() -> {
                        if (sent) {
                            view.showInfo("Đã gửi yêu cầu cấp lại phiên! Đang chờ Quản trị viên phê duyệt...");
                            view.setServerStatus(false, cafe.utils.NetworkConfig.getServerHost(), cafe.utils.NetworkConfig.getServerPort());
                            view.setRetryButtonText("Chờ duyệt...");
                        } else {
                            view.showError("Không thể kết nối máy chủ để gửi yêu cầu! Vui lòng thử lại sau.");
                            view.getBtnRetryConnection().setDisable(false);
                            view.setRetryButtonText("Thử lại");
                        }
                    });
                }, "Reconnect-Request-Thread").start();
                return;
            }

            view.getBtnRetryConnection().setDisable(true);
            new Thread(() -> {
                boolean ok = socketService.tryConnectToServer();
                Platform.runLater(() -> {
                    view.setServerStatus(ok, cafe.utils.NetworkConfig.getServerHost(), cafe.utils.NetworkConfig.getServerPort());
                    view.getBtnRetryConnection().setDisable(false);
                });
            }, "Retry-Connect-Thread").start();
        });

        view.getBtnConfigServer().setOnAction(e -> showServerConfigModal());

        view.setServerStatus(socketService.isConnected(), cafe.utils.NetworkConfig.getServerHost(), cafe.utils.NetworkConfig.getServerPort());
        startServerPoll();
    }

    private void showServerConfigModal() {
        Stage modal = new Stage();
        if (stage != null) modal.initOwner(stage);
        modal.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        modal.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        modal.setTitle("Cấu Hình Kết Nối Máy Chủ (Server IP & Port)");

        javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(14);
        card.setPrefWidth(460);
        card.setPadding(new javafx.geometry.Insets(22));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16px; -fx-border-color: #DFD7CA; -fx-border-radius: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 24, 0.15, 0, 8);");

        javafx.scene.layout.HBox headerRow = new javafx.scene.layout.HBox(10);
        headerRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.Node netIcon = cafe.utils.IconHelper.getIcon("admin", "ha-tang-mang.png", cafe.utils.SvgIcons.SETTINGS, "", 22, javafx.scene.paint.Color.web("#112823"));
        javafx.scene.layout.VBox titleCol = new javafx.scene.layout.VBox(2);
        javafx.scene.control.Label lblTitle = new javafx.scene.control.Label("Cấu Hình Kết Nối Máy Chủ (TCP Socket)");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        javafx.scene.control.Label lblSub = new javafx.scene.control.Label("Hỗ trợ kết nối trên cùng 1 máy (127.0.0.1) hoặc qua mạng LAN 2 máy");
        lblSub.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #6F695F;");
        titleCol.getChildren().addAll(lblTitle, lblSub);
        headerRow.getChildren().addAll(netIcon, titleCol);

        javafx.scene.control.Label lblHostPrompt = new javafx.scene.control.Label("Địa chỉ IP Máy chủ (Server Host):");
        lblHostPrompt.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #112823;");

        javafx.scene.control.TextField txtHost = new javafx.scene.control.TextField(cafe.utils.NetworkConfig.getServerHost());
        txtHost.setPromptText("VD: 127.0.0.1 hoặc 192.168.1.15");
        txtHost.setStyle("-fx-background-color: #FAF8F5; -fx-border-color: #DFD8CE; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 9 12 9 12; -fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #112823;");

        javafx.scene.layout.HBox quickHosts = new javafx.scene.layout.HBox(8);
        quickHosts.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.control.Button btnLocalhost = new javafx.scene.control.Button("Đặt về Localhost 127.0.0.1");
        btnLocalhost.setStyle("-fx-background-color: #F4EFE6; -fx-text-fill: #112823; -fx-font-size: 11px; -fx-font-weight: 700; -fx-background-radius: 6px; -fx-padding: 4 10 4 10; -fx-cursor: hand;");
        btnLocalhost.setOnAction(ev -> txtHost.setText("127.0.0.1"));

        String localLan = cafe.utils.NetworkConfig.getLocalLanIp();
        javafx.scene.control.Button btnLanHost = new javafx.scene.control.Button("IP LAN máy này (" + localLan + ")");
        btnLanHost.setStyle("-fx-background-color: #ECFDF5; -fx-text-fill: #065F46; -fx-font-size: 11px; -fx-font-weight: 700; -fx-background-radius: 6px; -fx-padding: 4 10 4 10; -fx-cursor: hand;");
        btnLanHost.setOnAction(ev -> txtHost.setText(localLan));
        quickHosts.getChildren().addAll(btnLocalhost, btnLanHost);

        javafx.scene.control.Label lblPortPrompt = new javafx.scene.control.Label("Cổng kết nối TCP (Server Port, mặc định 8888):");
        lblPortPrompt.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #112823;");

        javafx.scene.control.TextField txtPort = new javafx.scene.control.TextField(String.valueOf(cafe.utils.NetworkConfig.getServerPort()));
        txtPort.setPromptText("VD: 8888");
        txtPort.setStyle("-fx-background-color: #FAF8F5; -fx-border-color: #DFD8CE; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 9 12 9 12; -fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #112823;");

        javafx.scene.control.Label lblStatusHint = new javafx.scene.control.Label("Xem địa chỉ IP LAN và Port đang hiển thị trên thanh Header của màn hình ServerMain (Quầy Pha Chế KDS).");
        lblStatusHint.setWrapText(true);
        lblStatusHint.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #57534E; -fx-background-color: #FAF8F5; -fx-padding: 8 10 8 10; -fx-background-radius: 8px;");

        javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(10);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        javafx.scene.control.Button btnCancel = new javafx.scene.control.Button("Đóng");
        btnCancel.setStyle("-fx-background-color: #F4EFE6; -fx-text-fill: #4A453E; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
        btnCancel.setOnAction(ev -> modal.close());

        javafx.scene.control.Button btnSaveConnect = new javafx.scene.control.Button("Lưu & Kết nối ngay");
        btnSaveConnect.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-size: 12.5px; -fx-font-weight: 800; -fx-background-radius: 8px; -fx-padding: 8 18 8 18; -fx-cursor: hand;");
        btnSaveConnect.setOnAction(ev -> {
            String host = txtHost.getText() != null ? txtHost.getText().trim() : "";
            if (host.isEmpty()) host = "127.0.0.1";
            int port;
            try {
                port = Integer.parseInt(txtPort.getText().trim());
                if (!cafe.utils.NetworkConfig.isValidPort(port)) {
                    lblStatusHint.setText("Cổng mạng phải nằm trong dải 1024 - 65535!");
                    lblStatusHint.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #B91C1C; -fx-background-color: #FEE2E2; -fx-padding: 8 10 8 10; -fx-background-radius: 8px;");
                    return;
                }
            } catch (NumberFormatException ex) {
                lblStatusHint.setText("Cổng TCP phải là một số nguyên hợp lệ!");
                lblStatusHint.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #B91C1C; -fx-background-color: #FEE2E2; -fx-padding: 8 10 8 10; -fx-background-radius: 8px;");
                return;
            }

            final String targetHost = host;
            final int targetPort = port;
            cafe.utils.NetworkConfig.saveConfig(targetHost, targetPort);
            btnSaveConnect.setDisable(true);
            btnSaveConnect.setText("Đang kết nối...");

            new Thread(() -> {
                socketService.disconnect();
                socketService.resetKickedState();
                long t0 = System.currentTimeMillis();
                boolean ok = socketService.tryConnectToServer();
                long rtt = Math.max(1, System.currentTimeMillis() - t0);
                Platform.runLater(() -> {
                    btnSaveConnect.setDisable(false);
                    btnSaveConnect.setText("Lưu & Kết nối ngay");
                    view.setServerStatus(ok, targetHost, targetPort, ok ? rtt : -1);
                    if (ok) {
                        view.clearError();
                        view.showInfo("Đã kết nối thành công tới Máy chủ " + targetHost + ":" + targetPort + " (RTT: " + rtt + "ms)");
                        modal.close();
                    } else {
                        lblStatusHint.setText("Đã lưu cấu hình (" + targetHost + ":" + targetPort + "), nhưng chưa kết nối được Server. Hãy kiểm tra ServerMain đã mở đúng cổng này chưa.");
                        lblStatusHint.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #B91C1C; -fx-background-color: #FEE2E2; -fx-padding: 8 10 8 10; -fx-background-radius: 8px;");
                    }
                });
            }, "Config-Reconnect-Thread").start();
        });

        actions.getChildren().addAll(btnCancel, btnSaveConnect);
        card.getChildren().addAll(headerRow, lblHostPrompt, txtHost, quickHosts, lblPortPrompt, txtPort, lblStatusHint, actions);

        cafe.utils.WindowChromeHelper.makeDraggable(modal, card);
        javafx.scene.layout.StackPane wrap = new javafx.scene.layout.StackPane(card);
        wrap.setPadding(new javafx.geometry.Insets(16));
        wrap.setStyle("-fx-background-color: transparent;");
        Scene scene = new Scene(wrap);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        cafe.utils.WindowChromeHelper.applyTheme(scene);
        modal.setScene(scene);
        modal.show();
        cafe.utils.AnimationHelper.playDialogEntrance(card);
    }

    private void startServerPoll() {
        stopServerPoll();
        serverPollTimeline = new Timeline(
            new KeyFrame(Duration.seconds(3), ev -> {
                if (!socketService.isConnected()) {
                    new Thread(() -> {
                        boolean ok = socketService.tryConnectToServer();
                        Platform.runLater(() -> {
                            view.setServerStatus(ok, cafe.utils.NetworkConfig.getServerHost(), cafe.utils.NetworkConfig.getServerPort());
                        });
                    }, "Poll-Connect-Thread").start();
                } else {
                    view.setServerStatus(true, cafe.utils.NetworkConfig.getServerHost(), cafe.utils.NetworkConfig.getServerPort());
                }
            })
        );
        serverPollTimeline.setCycleCount(Animation.INDEFINITE);
        serverPollTimeline.play();
    }

    private void stopServerPoll() {
        if (serverPollTimeline != null) {
            serverPollTimeline.stop();
            serverPollTimeline = null;
        }
    }

    private void handleLogin() {
        view.clearError();

        // 1. Kiểm tra kết nối Máy chủ trung tâm (Port " + cafe.utils.NetworkConfig.getServerPort() + ")
        if (!socketService.isConnected()) {
            boolean ok = socketService.tryConnectToServer();
            if (!ok) {
                view.setServerStatus(false, cafe.utils.NetworkConfig.getServerHost(), cafe.utils.NetworkConfig.getServerPort());
                view.showError("Chưa kết nối Máy chủ trung tâm (Port " + cafe.utils.NetworkConfig.getServerPort() + ")!\nVui lòng khởi động ServerMain trước rồi bấm [Thử lại].");
                return;
            }
            view.setServerStatus(true, cafe.utils.NetworkConfig.getServerHost(), cafe.utils.NetworkConfig.getServerPort());
        }

        String account = view.getAccountField().getText().trim();
        String password = view.getPasswordField().getText().trim();

        if (account.isEmpty()) {
            view.showError("Vui lòng nhập số bàn (1..12) hoặc tài khoản nhân viên!");
            return;
        }

        StaffModel staffLookup = DatabaseManager.findStaffByUsername(account);
        if (staffLookup != null) {
            if (!"ACTIVE".equalsIgnoreCase(staffLookup.getStatus())) {
                view.showError("Tài khoản '" + staffLookup.getUsername() + "' (" + staffLookup.getFullName() + ") hiện đang bị KHÓA bởi Quản trị viên!\nVui lòng liên hệ Quản lý để mở khóa.");
                return;
            }
        }

        int potentialTable = parseTableNumber(account);

        // =====================================================================
        // NHÁNH 1: KHÁCH HÀNG (CUSTOMER KIOSK TẠI BÀN)
        // Điều kiện: Nhập số bàn (VD: "5", "Bàn 5", "1") và mật khẩu để trống hoặc trùng tên -> Vào thẳng Kiosk
        // =====================================================================
        if (potentialTable > 0 && (password.isEmpty() || password.equalsIgnoreCase(account))) {
            boolean connected = socketService.connect(potentialTable);
            if (connected) {
                stopServerPoll();
                this.activeTableNumber = potentialTable;
                openCustomerMenu(potentialTable);
            } else {
                view.showError("Không thể kết nối đến quầy pha chế! Vui lòng kiểm tra lại Server.");
            }
            return;
        }

        // =====================================================================
        // NHÁNH 2: NHÂN VIÊN / QUẢN TRỊ (STAFF / MANAGER POS)
        // =====================================================================
        if (staffLookup != null) {
            StaffModel staff = DatabaseManager.authenticateStaff(account, password);
            if (staff != null) {
                int table = (potentialTable > 0) ? potentialTable : 5;
                boolean connected = socketService.connectStaff(table, staff.getUsername());
                if (connected) {
                    stopServerPoll();
                    this.activeTableNumber = table;
                    if (staff.isManager() || "admin".equalsIgnoreCase(staff.getUsername())) {
                        openAdminDashboard(staff);
                    } else {
                        openFloorPlan(table, staff);
                    }
                } else {
                    view.showError("Không thể kết nối đến quầy pha chế! Vui lòng kiểm tra lại Server.");
                }
                return;
            } else {
                view.showError("Mật khẩu không chính xác! Vui lòng kiểm tra lại.");
                return;
            }
        }

        // Nếu người dùng nhập số bàn nhưng có mật khẩu không khớp
        if (potentialTable > 0) {
            boolean connected = socketService.connect(potentialTable);
            if (connected) {
                stopServerPoll();
                this.activeTableNumber = potentialTable;
                openCustomerMenu(potentialTable);
                return;
            }
        }

        view.showError("Tài khoản '" + account + "' không tồn tại trong hệ thống!");
    }

    private int parseTableNumber(String input) {
        if (input == null) return -1;
        String clean = input.replaceAll("(?i)(bàn|ban|table|#)", "").trim();
        try {
            int t = Integer.parseInt(clean);
            if (t > 0 && t <= 350) return t;
        } catch (NumberFormatException ignored) {}
        return -1;
    }

    /**
     * MỞ MÀN HÌNH QUẢN TRỊ TRUNG TÂM (ADMIN / MANAGER POS - 10 PHÂN HỆ):
     */
    private void openAdminDashboard(StaffModel staff) {
        cafe.services.CafeServerService serverService = cafe.services.CafeServerService.getInstance();
        cafe.views.ServerKDSView kdsView = new cafe.views.ServerKDSView();
        new ServerController(kdsView, serverService);

        cafe.views.AdminShellView adminShell = new cafe.views.AdminShellView(serverService, kdsView);
        adminShell.attachClientContext(stage, socketService);
        adminShell.setOnLogout(() -> {
            socketService.disconnect();
            socketService.warmupConnectAsync();
            returnToLoginView();
        });

        serverService.setOnOrderReceived(order -> {
            adminShell.notifyNewOrderArrived(order);
        });

        String title = "CoffeePOS • Trung Tâm Quản Trị & Điều Phối (" + (staff != null ? staff.getFullName() : "Quản lý") + ")";
        cafe.utils.WindowChromeHelper.setWindowContent(
            stage,
            adminShell,
            title,
            true,
            () -> {
                socketService.disconnect();
                System.exit(0);
            }
        );
        stage.setMaximized(true);
        cafe.utils.WindowChromeHelper.applyMaximized(stage);
    }

    /**
     * MỞ MÀN HÌNH KHÁCH HÀNG (CUSTOMER KIOSK TẠI BÀN):
     * - Khóa cứng số bàn của khách.
     * - ẨN NÚT "SƠ ĐỒ BÀN" (Khách không được xem sơ đồ quán và không thể đổi sang bàn khác).
     * - Mở Live Order Tracker tại góc màn hình để theo dõi tiến độ Barista pha chế.
     */
    private void openCustomerMenu(int tableNumber) {
        this.activeTableNumber = tableNumber;

        ClientView clientView = new ClientView();
        new ClientController(
            stage,
            clientView,
            socketService,
            tableNumber,
            null,
            () -> {
                socketService.disconnect();
                socketService.warmupConnectAsync();
                returnToLoginView();
            }
        );

        if (clientView.getFloorPlanButton() != null) {
            clientView.getFloorPlanButton().setVisible(false);
            clientView.getFloorPlanButton().setManaged(false);
        }

        // Đăng xuất từ màn hình Kiosk Khách (Role 1)
        clientView.setOnLogout(() -> {
            socketService.disconnect();
            socketService.warmupConnectAsync();
            returnToLoginView();
        });

        String menuTitle = "Brew & Bean CAFE • Bàn " + (tableNumber < 10 ? "0" + tableNumber : tableNumber);
        cafe.utils.WindowChromeHelper.setWindowContent(
            stage,
            clientView,
            menuTitle,
            true,
            () -> {
                socketService.disconnect();
                System.exit(0);
            }
        );
        stage.setMaximized(true);
        cafe.utils.WindowChromeHelper.applyMaximized(stage);
    }

    /**
     * MỞ MÀN HÌNH NHÂN VIÊN (UNIFIED STAFF POS SHELL / SƠ ĐỒ BÀN 3 TẦNG):
     * - Giữ cố định thanh điều hướng 235px bên trái xuyên suốt cả 5 phân hệ (Bàn, Thực đơn, Đơn hàng, Thanh toán, Cài đặt).
     * - Lắng nghe thay đổi trạng thái bàn từ Server để đổi màu bàn thời gian thực.
     */
    private void openFloorPlan(int initialTable, StaffModel staff) {
        this.activeTableNumber = initialTable;

        FloorPlanView floorPlanView = new FloorPlanView(
            activeTableNumber,
            selectedTable -> {
                this.activeTableNumber = selectedTable;
                socketService.switchTable(selectedTable);
            },
            () -> {
                openStaffMenu(activeTableNumber, staff);
            },
            () -> {
                socketService.disconnect();
                socketService.warmupConnectAsync();
                returnToLoginView();
            }
        );

        floorPlanView.attachEmbeddedOrdering(
            stage,
            socketService,
            (staff != null && staff.isManager()) ? () -> openAdminDashboard(staff) : null
        );

        if (staff != null) {
            floorPlanView.setStaffProfile(staff);
        }

        socketService.setOnTableStatusReceived((tableNum, status) -> {
            floorPlanView.setTableStatus(tableNum, status);
        });

        socketService.setOnSessionKickedReceived(kickMsg -> {
            javafx.application.Platform.runLater(() -> {
                String reason = (kickMsg != null && kickMsg.reason != null)
                    ? kickMsg.reason
                    : "Phiên làm việc đã bị ngắt bởi Quản trị viên!";
                socketService.disconnect();
                socketService.warmupConnectAsync();
                returnToLoginView();
                view.showError(reason);
            });
        });

        String title = "Sơ Đồ Bàn & Phục Vụ • Brew & Bean CAFE (" + (staff != null ? staff.getFullName() : "Nhân viên") + ")";
        cafe.utils.WindowChromeHelper.setWindowContent(
            stage,
            floorPlanView,
            title,
            true,
            () -> {
                socketService.disconnect();
                System.exit(0);
            }
        );
        stage.setMaximized(true);
        cafe.utils.WindowChromeHelper.applyMaximized(stage);
    }

    /**
     * MÀN HÌNH THỰC ĐƠN ĐỘC LẬP (FALLBACK):
     */
    private void openStaffMenu(int tableNumber, StaffModel staff) {
        this.activeTableNumber = tableNumber;

        ClientView clientView = new ClientView();
        new ClientController(
            stage,
            clientView,
            socketService,
            tableNumber,
            () -> openFloorPlan(activeTableNumber, staff),
            () -> openFloorPlan(activeTableNumber, staff)
        );

        if (clientView.getFloorPlanButton() != null) {
            clientView.getFloorPlanButton().setVisible(true);
            clientView.getFloorPlanButton().setManaged(true);
        }

        clientView.setOnLogout(() -> {
            socketService.disconnect();
            socketService.warmupConnectAsync();
            returnToLoginView();
        });

        String menuTitle = "Brew & Bean CAFE • Đặt món Bàn " + (tableNumber < 10 ? "0" + tableNumber : tableNumber);
        cafe.utils.WindowChromeHelper.setWindowContent(
            stage,
            clientView,
            menuTitle,
            true,
            () -> {
                socketService.disconnect();
                System.exit(0);
            }
        );
        stage.setMaximized(true);
        cafe.utils.WindowChromeHelper.applyMaximized(stage);
    }

    private void returnToLoginView() {
        startServerPoll();
        view.setServerStatus(socketService.isConnected(), cafe.utils.NetworkConfig.getServerHost(), cafe.utils.NetworkConfig.getServerPort());

        cafe.utils.WindowChromeHelper.setWindowContent(
            stage,
            view,
            "Đăng Nhập • Brew & Bean CAFE",
            true,
            () -> {
                socketService.disconnect();
                System.exit(0);
            }
        );

        stage.setMinWidth(720);
        stage.setMinHeight(650);
        stage.setMaximized(true);
        cafe.utils.WindowChromeHelper.applyMaximized(stage);
        view.setMaximizedMode(true);
    }
}

