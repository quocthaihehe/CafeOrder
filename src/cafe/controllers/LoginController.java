package cafe.controllers;

import cafe.models.Order;
import cafe.models.StaffModel;
import cafe.services.CafeServerService;
import cafe.services.ClientSocketService;
import cafe.services.DatabaseManager;
import cafe.utils.CurrencyFormatter;
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

    private int activeTableNumber = 2;
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

        // Nút [ Sơ đồ bàn ] ở góc trên bên phải mở Popup Sơ đồ chọn bàn (740px)
        view.getBtnOpenTableMap().setOnAction(e -> view.showTableMapPopup(stage, this::handleLogin));

        // Xử lý khi người dùng bấm trực tiếp vào một bàn
        view.setOnTableCardClicked(tableNum -> {
            CafeServerService server = CafeServerService.getInstance();
            String liveStatus = server.getTableLiveDisplayStatus(tableNum);
            if ("Sẵn sàng".equals(liveStatus)) {
                view.getAccountField().setText(String.valueOf(tableNum));
                view.getPasswordField().setText("");
                String tCode = tableNum < 10 ? "0" + tableNum : String.valueOf(tableNum);
                view.showInfo("● Bàn " + tCode + " đang Sẵn sàng đón khách — Bấm [Đăng nhập] để mở thực đơn gọi món!");
            } else {
                Order activeOrd = server.getCombinedOrderForTable(tableNum);
                CafeServerService.ReservationInfo resInfo = server.getReservation(tableNum);
                view.showOccupiedTableNotice(tableNum, liveStatus, activeOrd, resInfo);
                showTableOccupiedModal(tableNum, liveStatus, activeOrd, resInfo);
            }
        });

        // Đồng bộ trực tiếp trạng thái bàn lên màn hình Login khi có thay đổi từ Server
        CafeServerService.getInstance().setOnTableStatusBroadcasted((tbl, st) -> {
            Platform.runLater(view::refreshTablePickerGrid);
        });
        CafeServerService.getInstance().setOnOrderStatusChanged((oid, st) -> {
            Platform.runLater(view::refreshTablePickerGrid);
        });
        socketService.addTableStatusListener((tbl, st) -> {
            Platform.runLater(view::refreshTablePickerGrid);
        });
    }

    private void initServerStatus() {
        socketService.setOnConnectionStateChanged(connected -> {
            Platform.runLater(() -> {
                view.setServerStatus(connected, cafe.utils.NetworkConfig.getServerHost(), cafe.utils.NetworkConfig.getServerPort());
                view.refreshTablePickerGrid();
            });
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
                view.refreshTablePickerGrid();
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
                    view.refreshTablePickerGrid();
                });
            }, "Retry-Connect-Thread").start();
        });

        view.getBtnConfigServer().setOnAction(e -> showServerConfigModal());

        view.setServerStatus(socketService.isConnected(), cafe.utils.NetworkConfig.getServerHost(), cafe.utils.NetworkConfig.getServerPort());
        startServerPoll();
    }

    /**
     * Hiển thị Modal thông báo "Bàn đang có khách" kế thừa 100% ngôn ngữ thiết kế Brew & Bean (#FBF9F5 / #112823).
     * Liệt kê rõ trạng thái hiện tại (Chờ pha / Đang pha / Đang dùng / Giữ bàn hẹn / Chờ dọn bàn)
     * và gợi ý danh sách các bàn đang ở trạng thái "Sẵn sàng" để chọn nhanh 1 chạm.
     */
    private void showTableOccupiedModal(int tableNumber, String liveStatus, Order activeOrder, CafeServerService.ReservationInfo resInfo) {
        Stage modal = new Stage();
        if (stage != null) modal.initOwner(stage);
        modal.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        modal.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        String tCode = tableNumber < 10 ? "0" + tableNumber : String.valueOf(tableNumber);
        modal.setTitle("Thông Báo Bàn Đang Có Khách — Bàn " + tCode);

        javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(14);
        card.setPrefWidth(480);
        card.setPadding(new javafx.geometry.Insets(22));
        card.setStyle(
            "-fx-background-color: #FBF9F5; " +
            "-fx-background-radius: 16px; " +
            "-fx-border-color: #DED7CD; " +
            "-fx-border-width: 1.5px; " +
            "-fx-border-radius: 16px; " +
            "-fx-effect: dropshadow(gaussian, rgba(17,40,35,0.24), 26, 0.18, 0, 8);"
        );

        // Header
        javafx.scene.layout.HBox headerRow = new javafx.scene.layout.HBox(12);
        headerRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane();
        iconWrap.setPrefSize(44, 44);
        iconWrap.setMinSize(44, 44);
        iconWrap.setStyle("-fx-background-color: #172E27; -fx-background-radius: 12px;");
        javafx.scene.Node tableIcon = cafe.utils.IconHelper.getIcon("staff", "so-do-ban.png", cafe.utils.SvgIcons.TABLE, "", 22, javafx.scene.paint.Color.web("#FCD34D"));
        iconWrap.getChildren().add(tableIcon);

        javafx.scene.layout.VBox titleCol = new javafx.scene.layout.VBox(3);
        javafx.scene.control.Label lblTitle = new javafx.scene.control.Label("Bàn " + tCode + " hiện đang có khách!");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', Georgia, serif; -fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        javafx.scene.control.Label lblSub = new javafx.scene.control.Label("Không thể đăng nhập mới vào bàn đang trong phiên phục vụ");
        lblSub.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-text-fill: #6E675F;");
        titleCol.getChildren().addAll(lblTitle, lblSub);
        headerRow.getChildren().addAll(iconWrap, titleCol);

        // Khối chi tiết trạng thái bàn đang bận (#172E27)
        javafx.scene.layout.VBox statusBox = new javafx.scene.layout.VBox(8);
        statusBox.setPadding(new javafx.geometry.Insets(14, 16, 14, 16));
        statusBox.setStyle("-fx-background-color: #172E27; -fx-background-radius: 12px; -fx-border-color: #C8965A; -fx-border-radius: 12px; -fx-border-width: 1px;");

        javafx.scene.layout.HBox badgeRow = new javafx.scene.layout.HBox(8);
        badgeRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(4.5);
        String badgeTextColor;
        if ("Chờ pha".equals(liveStatus)) {
            dot.setFill(javafx.scene.paint.Color.web("#F59E0B"));
            badgeTextColor = "#FCD34D";
        } else if ("Đang pha".equals(liveStatus)) {
            dot.setFill(javafx.scene.paint.Color.web("#38BDF8"));
            badgeTextColor = "#93C5FD";
        } else if ("Đang dùng".equals(liveStatus)) {
            dot.setFill(javafx.scene.paint.Color.web("#22C55E"));
            badgeTextColor = "#86EFAC";
        } else {
            dot.setFill(javafx.scene.paint.Color.web("#F59E0B"));
            badgeTextColor = "#FDE68A";
        }

        javafx.scene.control.Label lblStTitle = new javafx.scene.control.Label("Trạng thái Bàn " + tCode + ": " + liveStatus.toUpperCase() + " (Đang có khách)");
        lblStTitle.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: " + badgeTextColor + ";");
        badgeRow.getChildren().addAll(dot, lblStTitle);

        String infoDesc;
        if (activeOrder != null) {
            infoDesc = "Đơn hiện tại: #" + activeOrder.getOrderId() +
                       " • Số lượng: " + activeOrder.getTotalQuantity() + " món" +
                       " • Tạm tính: " + CurrencyFormatter.formatVND(activeOrder.getTotalAmount()) +
                       "\n(Các trạng thái Chờ pha, Đang pha, Đang dùng đều thuộc bàn đang có khách sử dụng cho đến khi hoàn tất thanh toán trả bàn).";
        } else if ("Giữ bàn hẹn".equals(liveStatus) && resInfo != null) {
            infoDesc = "Khách đặt hẹn: " + resInfo.customerName + " (" + resInfo.phone + ") • Giờ hẹn: " + resInfo.reservationTime + " (" + resInfo.guestCount + " khách).";
        } else {
            infoDesc = "Bàn hiện đang phục vụ khách tại quán, vui lòng chọn các bàn có trạng thái ● Sẵn sàng.";
        }

        javafx.scene.control.Label lblOrderSummary = new javafx.scene.control.Label(infoDesc);
        lblOrderSummary.setWrapText(true);
        lblOrderSummary.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-text-fill: #E2D9CC;");
        statusBox.getChildren().addAll(badgeRow, lblOrderSummary);

        // Danh sách gợi ý các bàn Sẵn sàng để chọn nhanh
        javafx.scene.control.Label lblSuggestPrompt = new javafx.scene.control.Label("Chọn nhanh bàn đang Sẵn sàng (Trống):");
        lblSuggestPrompt.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        javafx.scene.layout.FlowPane readyFlow = new javafx.scene.layout.FlowPane(8, 8);
        java.util.List<Integer> readyTables = CafeServerService.getInstance().getReadyTables();
        int shown = 0;
        for (int rt : readyTables) {
            if (shown >= 8) break;
            String rtCode = rt < 10 ? "0" + rt : String.valueOf(rt);
            javafx.scene.control.Button btnPick = new javafx.scene.control.Button("● Bàn " + rtCode + " (Sẵn sàng)");
            btnPick.setStyle(
                "-fx-background-color: #ECFDF5; " +
                "-fx-border-color: #16A34A; " +
                "-fx-border-radius: 8px; " +
                "-fx-background-radius: 8px; " +
                "-fx-text-fill: #15803D; " +
                "-fx-font-family: 'Inter', sans-serif; " +
                "-fx-font-size: 11.5px; " +
                "-fx-font-weight: 800; " +
                "-fx-padding: 6 11 6 11; " +
                "-fx-cursor: hand;"
            );
            cafe.utils.AnimationHelper.applyButtonPress(btnPick);
            final int chosenTable = rt;
            btnPick.setOnAction(ev -> {
                modal.close();
                view.getAccountField().setText(String.valueOf(chosenTable));
                view.getPasswordField().setText("");
                view.showInfo("Đã chuyển sang Bàn " + rtCode + " (● Sẵn sàng) — Bấm [Đăng nhập] để gọi món!");
            });
            readyFlow.getChildren().add(btnPick);
            shown++;
        }

        // Actions
        javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(10);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        javafx.scene.control.Button btnOpenMap = new javafx.scene.control.Button("Mở Sơ Đồ Bàn");
        btnOpenMap.setGraphic(cafe.utils.IconHelper.getIcon("staff", "so-do-ban.png", cafe.utils.SvgIcons.TABLE, "", 15, javafx.scene.paint.Color.web("#112823")));
        btnOpenMap.setStyle(
            "-fx-background-color: #EDE6DB; " +
            "-fx-text-fill: #112823; " +
            "-fx-font-family: 'Inter', sans-serif; " +
            "-fx-font-size: 13px; " +
            "-fx-font-weight: 800; " +
            "-fx-background-radius: 8px; " +
            "-fx-padding: 9 16 9 16; " +
            "-fx-cursor: hand;"
        );
        cafe.utils.AnimationHelper.applyButtonPress(btnOpenMap);
        btnOpenMap.setOnAction(ev -> {
            modal.close();
            view.showTableMapPopup(stage, this::handleLogin);
        });

        javafx.scene.control.Button btnClose = new javafx.scene.control.Button("Đã hiểu, chọn bàn khác");
        btnClose.setStyle(
            "-fx-background-color: #112823; " +
            "-fx-text-fill: #FFFFFF; " +
            "-fx-font-family: 'Inter', sans-serif; " +
            "-fx-font-size: 13px; " +
            "-fx-font-weight: 800; " +
            "-fx-background-radius: 8px; " +
            "-fx-padding: 9 20 9 20; " +
            "-fx-cursor: hand;"
        );
        cafe.utils.AnimationHelper.applyButtonPress(btnClose);
        btnClose.setOnAction(ev -> modal.close());
        actions.getChildren().addAll(btnOpenMap, btnClose);

        card.getChildren().addAll(headerRow, statusBox, lblSuggestPrompt, readyFlow, actions);

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
                    view.refreshTablePickerGrid();
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
                            view.refreshTablePickerGrid();
                        });
                    }, "Poll-Connect-Thread").start();
                } else {
                    view.setServerStatus(true, cafe.utils.NetworkConfig.getServerHost(), cafe.utils.NetworkConfig.getServerPort());
                    view.refreshTablePickerGrid();
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

        // 1. Kiểm tra kết nối Máy chủ trung tâm
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
            view.showError("Vui lòng chọn bàn Sẵn sàng (1..12, 201..208, 301..306) hoặc nhập tài khoản nhân viên!");
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
        // Điều kiện: Nhập số bàn và mật khẩu để trống hoặc trùng tên -> Kiểm tra bàn Sẵn sàng trước khi cho vào
        // =====================================================================
        if (potentialTable > 0 && (password.isEmpty() || password.equalsIgnoreCase(account))) {
            attemptCustomerTableLogin(potentialTable);
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

        // Nếu người dùng nhập số bàn nhưng có gõ mật khẩu bất kỳ
        if (potentialTable > 0) {
            attemptCustomerTableLogin(potentialTable);
            return;
        }

        view.showError("Tài khoản '" + account + "' không tồn tại trong hệ thống!");
    }

    /**
     * Kiểm tra nghiêm ngặt trạng thái bàn trước khi cho phép Khách hàng đăng nhập Kiosk:
     * - Chỉ cho phép đăng nhập khi bàn có trạng thái "Sẵn sàng".
     * - Nếu bàn có trạng thái "Chờ pha", "Đang pha", "Đang dùng", "Giữ bàn hẹn" hoặc "Chờ dọn bàn",
     *   hiển thị thông báo bàn đang có khách ngay trên UI đăng nhập (Card + Modal kế thừa thiết kế hệ thống).
     */
    private void attemptCustomerTableLogin(int tableNumber) {
        if (!CafeServerService.isValidFloorTable(tableNumber)) {
            view.showError("Bàn số " + tableNumber + " không tồn tại trong sơ đồ quán! (Tầng 1: 1..12, Tầng 2: 201..208, Tầng 3: 301..306)");
            return;
        }

        CafeServerService server = CafeServerService.getInstance();
        String liveStatus = server.getTableLiveDisplayStatus(tableNumber);

        if (!"Sẵn sàng".equals(liveStatus)) {
            Order activeOrd = server.getCombinedOrderForTable(tableNumber);
            CafeServerService.ReservationInfo resInfo = server.getReservation(tableNumber);
            view.showOccupiedTableNotice(tableNumber, liveStatus, activeOrd, resInfo);
            showTableOccupiedModal(tableNumber, liveStatus, activeOrd, resInfo);
            return;
        }

        boolean connected = socketService.connect(tableNumber);
        if (connected) {
            stopServerPoll();
            this.activeTableNumber = tableNumber;
            openCustomerMenu(tableNumber);
        } else {
            view.showError("Không thể kết nối đến quầy pha chế! Vui lòng kiểm tra lại Server.");
        }
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
     * MỞ MÀN HÌNH QUẢN TRỊ TRUNG TÂM (ADMIN / MANAGER POS):
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
        view.clearError();
        view.refreshTablePickerGrid();
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
