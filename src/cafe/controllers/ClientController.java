package cafe.controllers;

import cafe.models.*;
import cafe.models.MenuItem;
import cafe.services.ClientSocketService;
import cafe.utils.CurrencyFormatter;
import cafe.views.CartModalDialog;
import cafe.views.ClientView;
import cafe.views.ItemDetailDialog;
import cafe.views.OrderSuccessDialog;
import cafe.views.PaymentDialog;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import java.util.ArrayList;
import java.util.List;

public class ClientController {
    private final ClientView view;
    private final CartModel cartModel;
    private final ClientSocketService socketService;
    private final Stage primaryStage;

    private int tableNumber;
    private String currentCategory = "Tất cả";
    private String currentSearchKeyword = "";
    private final javafx.animation.PauseTransition searchDebounce = new javafx.animation.PauseTransition(javafx.util.Duration.millis(160));

    // Stored transaction state for OrderSuccess confirmation slip (Editorial Receipt Slip)
    private String lastPaymentMethod = "TIỀN MẶT";
    private double lastOrderSubtotal = 0.0;
    private double lastOrderDiscount = 0.0;
    private String lastVoucherCode = null;
    private double lastOrderTotal = 0.0;
    private int lastOrderQuantity = 0;
    private List<OrderItem> lastOrderItems = new ArrayList<>();
    private CartModalDialog activeCartDialog = null;
    private final Runnable onBackToFloorPlan;
    private final Runnable onLogout;

    public ClientController(Stage primaryStage, ClientView view, ClientSocketService socketService, int tableNumber) {
        this(primaryStage, view, socketService, tableNumber, null, null);
    }

    public ClientController(Stage primaryStage, ClientView view, ClientSocketService socketService, int tableNumber, Runnable onBackToFloorPlan) {
        this(primaryStage, view, socketService, tableNumber, onBackToFloorPlan, null);
    }

    public ClientController(Stage primaryStage, ClientView view, ClientSocketService socketService, int tableNumber, Runnable onBackToFloorPlan, Runnable onLogout) {
        this.primaryStage = primaryStage;
        this.view = view;
        this.socketService = socketService;
        this.tableNumber = tableNumber;
        this.onBackToFloorPlan = onBackToFloorPlan;
        this.onLogout = onLogout;
        this.cartModel = new CartModel();

        init();
    }

    private void init() {
        view.setTableNumber(tableNumber);
        view.setConnectionState(socketService.isConnected());
        view.updateCartBadge(cartModel.getTotalQuantity());

        if (onBackToFloorPlan != null) {
            view.getFloorPlanButton().setOnAction(e -> onBackToFloorPlan.run());
        }
        if (onLogout != null) {
            view.getLogoutButton().setOnAction(e -> onLogout.run());
        } else if (onBackToFloorPlan != null) {
            view.getLogoutButton().setOnAction(e -> onBackToFloorPlan.run());
        }

        // Category & Search bindings
        view.renderCategories(MenuRepository.getCategories(), currentCategory, this::onCategorySelected);

        view.getSearchField().textProperty().addListener((obs, oldVal, newVal) -> {
            searchDebounce.setOnFinished(e -> {
                currentSearchKeyword = newVal != null ? newVal.trim().toLowerCase() : "";
                refreshMenuCatalog();
            });
            searchDebounce.playFromStart();
        });

        // Floating Cart Button click -> Opens CartModalDialog
        view.getCartButton().setOnAction(e -> openCartModal());

        // Socket Service Callbacks
        socketService.setOnConnectionStateChanged(connected -> {
            view.setConnectionState(connected);
            if (!connected) {
                view.showToast("Mất kết nối tới máy chủ pha chế!");
            }
        });

        socketService.setOnOrderAckReceived(ack -> {
            int confirmedOrderId = ack.orderId;
            String translatedStatus = MessageProtocol.translateStatus(ack.status);

            List<OrderItem> snapshotItems = !lastOrderItems.isEmpty()
                ? new ArrayList<>(lastOrderItems)
                : new ArrayList<>(cartModel.getItems());
            int snapshotQty = lastOrderQuantity > 0 ? lastOrderQuantity : cartModel.getTotalQuantity();
            double snapshotSubtotal = lastOrderSubtotal > 0 ? lastOrderSubtotal : cartModel.getSubtotalAmount();
            double snapshotDiscount = lastOrderDiscount > 0 ? lastOrderDiscount : cartModel.getDiscountAmount();
            String snapshotVoucher = lastVoucherCode != null ? lastVoucherCode
                : (cartModel.getAppliedVoucher() != null ? cartModel.getAppliedVoucher().getCode() : null);
            double finalTotal = lastOrderTotal > 0 ? lastOrderTotal : cartModel.getTotalAmount();
            String finalMethod = lastPaymentMethod;

            cartModel.clear();
            view.updateCartBadge(0);
            view.updateActiveOrderStatus(confirmedOrderId, translatedStatus);

            if (activeCartDialog != null && activeCartDialog.isShowing()) {
                activeCartDialog.close();
            }

            OrderSuccessDialog.ReceiptData receiptData = new OrderSuccessDialog.ReceiptData(
                confirmedOrderId,
                tableNumber,
                java.time.LocalDateTime.now(),
                snapshotItems,
                snapshotQty,
                snapshotSubtotal,
                snapshotDiscount,
                snapshotVoucher,
                finalTotal,
                finalMethod,
                "ĐÃ THANH TOÁN",
                "Kiosk Tự Phục Vụ #01"
            );

            // Hiển thị Phiếu Thanh Toán (Editorial Receipt Slip)
            OrderSuccessDialog successDialog = new OrderSuccessDialog(
                primaryStage,
                receiptData,
                () -> view.showToast("Đơn #" + confirmedOrderId + " đang được pha chế!"),
                () -> {
                    view.highlightActiveOrderBox();
                    view.showToast("⏱ Đang theo dõi Đơn #" + confirmedOrderId + " • Trạng thái: " + translatedStatus);
                }
            );
            successDialog.show();
        });

        socketService.setOnStatusUpdateReceived(update -> {
            if (update == null) return;
            if (update.table > 0 && this.tableNumber > 0 && update.table != this.tableNumber) {
                return;
            }
            String translated = MessageProtocol.translateStatus(update.status);
            view.updateActiveOrderStatus(update.orderId, translated);
            view.showToast("Đơn #" + update.orderId + " đã cập nhật: " + translated + "!");
        });

        socketService.setOnStockStatusReceived(stockMsg -> {
            MenuItem repoItem = cafe.models.MenuRepository.findById(stockMsg.itemId);
            if (repoItem != null) {
                repoItem.setInStock(stockMsg.inStock);
            }
            view.updateItemStockStatus(stockMsg.itemId, stockMsg.inStock);
            String statusText = stockMsg.inStock ? "đã mở bán lại" : "tạm hết nguyên liệu";
            view.showToast("Món \"" + stockMsg.itemName + "\" hiện " + statusText + "!");
        });

        socketService.setOnSessionKickedReceived(kickMsg -> {
            cartModel.clear();
            view.updateCartBadge(0);
            String reason = (kickMsg != null && kickMsg.reason != null)
                ? kickMsg.reason
                : "Phiên làm việc của bạn đã bị ngắt bởi Quản trị viên hệ thống!";
            showCustomKickDialog(reason);
        });

        socketService.setOnErrorOccurred(errMsg -> {
            view.showToast(errMsg);
        });

        // Render initial catalog
        refreshMenuCatalog();
    }

    private void openCartModal() {
        activeCartDialog = new CartModalDialog(
            primaryStage,
            tableNumber,
            cartModel,
            this::onCartQtyChanged,
            this::onCartItemDeleted,
            this::applyVoucherCodeFromDialog,
            this::openPaymentFlow,
            () -> view.updateCartBadge(cartModel.getTotalQuantity())
        );
        activeCartDialog.show();
    }

    private void applyVoucherCodeFromDialog(String code) {
        if (code == null || code.trim().isEmpty()) {
            if (activeCartDialog != null) {
                activeCartDialog.setVoucherFeedback("Vui lòng nhập mã giảm giá!", false);
            }
            return;
        }

        String normalizedCode = code.trim().toUpperCase();
        Voucher v = MenuRepository.getVoucher(normalizedCode);
        if (v == null) {
            if (activeCartDialog != null) {
                activeCartDialog.setVoucherFeedback("Mã " + normalizedCode + " không hợp lệ!", false);
            }
            return;
        }

        if (cartModel.getSubtotalAmount() < v.getMinOrderAmount()) {
            if (activeCartDialog != null) {
                activeCartDialog.setVoucherFeedback("Đơn tối thiểu " + CurrencyFormatter.format(v.getMinOrderAmount()) + " mới được dùng mã!", false);
            }
            return;
        }

        boolean applied = cartModel.applyVoucher(v);
        if (applied) {
            if (activeCartDialog != null) {
                activeCartDialog.refreshView();
                activeCartDialog.setVoucherFeedback("Đã áp dụng: " + v.getDescription(), true);
            }
            view.showToast("Áp dụng thành công voucher " + normalizedCode + "!");
        }
    }

    private void onCategorySelected(String category) {
        this.currentCategory = category;
        view.renderCategories(MenuRepository.getCategories(), currentCategory, this::onCategorySelected);
        refreshMenuCatalog();
    }

    private void refreshMenuCatalog() {
        List<MenuItem> all = MenuRepository.getAllItems();
        List<MenuItem> filtered = new ArrayList<>();

        for (MenuItem item : all) {
            boolean matchCategory = currentCategory.equalsIgnoreCase("Tất cả") || item.getCategory().equalsIgnoreCase(currentCategory);
            boolean matchSearch = currentSearchKeyword.isEmpty() ||
                                  item.getName().toLowerCase().contains(currentSearchKeyword) ||
                                  item.getDescription().toLowerCase().contains(currentSearchKeyword);

            if (matchCategory && matchSearch) {
                filtered.add(item);
            }
        }

        // Đảm bảo món Bán chạy nhất (isBestseller) luôn luôn đứng ở vị trí số 1 của mỗi danh mục
        filtered.sort((a, b) -> {
            if (!currentCategory.equalsIgnoreCase("Tất cả")) {
                if (a.isBestseller() != b.isBestseller()) {
                    return a.isBestseller() ? -1 : 1;
                }
                return Integer.compare(a.getId(), b.getId());
            } else {
                int catA = getCategoryPriority(a.getCategory());
                int catB = getCategoryPriority(b.getCategory());
                if (catA != catB) {
                    return Integer.compare(catA, catB);
                }
                if (a.isBestseller() != b.isBestseller()) {
                    return a.isBestseller() ? -1 : 1;
                }
                return Integer.compare(a.getId(), b.getId());
            }
        });

        view.renderMenuItems(filtered, this::openItemCustomization, this::onQuickAdd);
    }

    private int getCategoryPriority(String category) {
        if (category == null) return 99;
        String lower = category.toLowerCase();
        if (lower.contains("cà phê")) return 1;
        if (lower.contains("trà &") || lower.contains("trái cây")) return 2;
        if (lower.contains("đá xay")) return 3;
        if (lower.contains("trà sữa")) return 4;
        if (lower.contains("nước ép") || lower.contains("soda")) return 5;
        if (lower.contains("matcha") || lower.contains("chocolate")) return 6;
        if (lower.contains("bánh ngọt")) return 7;
        if (lower.contains("món ăn nhẹ")) return 8;
        return 50;
    }

    private void onQuickAdd(MenuItem item) {
        String nameLower = item.getName() != null ? item.getName().toLowerCase() : "";
        boolean isHotOnly = nameLower.contains("nóng") || nameLower.equals("espresso") || nameLower.contains("cà phê trứng");
        OrderItem oi = new OrderItem(
            item.getId(),
            item.getName(),
            1,
            item.getPrice(),
            item.isAllowSize() ? "M" : "",
            0.0,
            100,
            isHotOnly ? 0 : 100,
            isHotOnly ? "HOT" : "COLD",
            new ArrayList<>(),
            ""
        );
        cartModel.addItem(oi);
        view.updateCartBadge(cartModel.getTotalQuantity());
        view.showToast("Đã thêm nhanh 1x " + item.getName());

        if (activeCartDialog != null && activeCartDialog.isShowing()) {
            activeCartDialog.refreshView();
        }
    }

    private void openItemCustomization(MenuItem item) {
        ItemDetailDialog dialog = new ItemDetailDialog(primaryStage, item, orderItem -> {
            cartModel.addItem(orderItem);
            view.updateCartBadge(cartModel.getTotalQuantity());
            view.showToast("Đã thêm: " + orderItem.getQty() + "x " + orderItem.getName());

            if (activeCartDialog != null && activeCartDialog.isShowing()) {
                activeCartDialog.refreshView();
            }
        });
        dialog.show();
    }

    private void onCartQtyChanged(OrderItem item, int delta) {
        cartModel.updateQuantity(item, delta);
        view.updateCartBadge(cartModel.getTotalQuantity());
    }

    private void onCartItemDeleted(OrderItem item) {
        cartModel.removeItem(item);
        view.updateCartBadge(cartModel.getTotalQuantity());
    }

    private void openPaymentFlow() {
        if (cartModel.isEmpty()) {
            view.showToast("Giỏ hàng đang trống! Vui lòng chọn món trước.");
            return;
        }

        // Bảo vệ giỏ hàng: Chặn thanh toán nếu có món tạm hết (86-ed)
        for (OrderItem oi : cartModel.getItems()) {
            MenuItem mi = oi.getProductId() > 0
                ? cafe.models.MenuRepository.findById(oi.getProductId())
                : cafe.models.MenuRepository.findByName(oi.getName());
            if (mi != null && !mi.isInStock()) {
                view.showToast("Món \"" + oi.getName() + "\" hiện đã tạm hết! Vui lòng xóa khỏi giỏ để tiếp tục.");
                return;
            }
        }

        if (!socketService.isConnected()) {
            socketService.connect(tableNumber);
        }
        if (!socketService.isConnected()) {
            view.showToast("Chưa kết nối được với quầy pha chế!");
            return;
        }

        PaymentDialog paymentDialog = new PaymentDialog(primaryStage, tableNumber, cartModel, selectedMethod -> {
            this.lastPaymentMethod = selectedMethod;
            this.lastOrderItems = new ArrayList<>(cartModel.getItems());
            this.lastOrderQuantity = cartModel.getTotalQuantity();
            this.lastOrderSubtotal = cartModel.getSubtotalAmount();
            this.lastOrderDiscount = cartModel.getDiscountAmount();
            this.lastVoucherCode = cartModel.getAppliedVoucher() != null ? cartModel.getAppliedVoucher().getCode() : null;
            this.lastOrderTotal = cartModel.getTotalAmount();
            submitOrder(selectedMethod);
        });
        paymentDialog.show();
    }

    private void submitOrder(String paymentMethod) {
        String customNote = cartModel.getOrderNote() != null ? cartModel.getOrderNote().trim() : "";
        String fullNote = customNote.isEmpty()
            ? "Thanh toán: " + paymentMethod
            : customNote + " • TT: " + paymentMethod;

        boolean sent = socketService.sendOrder(
            tableNumber,
            cartModel.getOrderType(),
            cartModel.getDiscountAmount(),
            cartModel.getAppliedVoucher() != null ? cartModel.getAppliedVoucher().getCode() : null,
            fullNote,
            new ArrayList<>(cartModel.getItems())
        );

        if (sent) {
            view.showToast("Đang gửi đơn tới quầy pha chế...");
        }
    }

    public void setTableNumber(int newTable) {
        this.tableNumber = newTable;
        view.setTableNumber(newTable);
        if (newTable <= 0) {
            cartModel.setOrderType("TAKEAWAY");
        } else {
            cartModel.setOrderType("DINE_IN");
        }
    }

    public int getTableNumber() {
        return tableNumber;
    }

    public void refreshCatalog() {
        refreshMenuCatalog();
    }
    private void showCustomKickDialog(String reason) {
        Stage modal = new Stage();
        modal.initOwner(primaryStage);
        modal.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        modal.initStyle(javafx.stage.StageStyle.TRANSPARENT);

        VBox root = new VBox(16);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(28, 24, 24, 24));
        root.setPrefWidth(460);
        root.setMaxWidth(460);
        root.setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-background-radius: 18px; " +
            "-fx-border-color: #DFD7CA; " +
            "-fx-border-radius: 18px; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 32, 0.2, 0, 10);"
        );

        Circle circle = new Circle(30, Color.web("#FEE2E2"));
        circle.setStroke(Color.web("#FECACA"));
        circle.setStrokeWidth(1.5);
        SVGPath iconShield = cafe.utils.SvgIcons.createOutlineIcon(cafe.utils.SvgIcons.LOCK, 1.0, Color.web("#B91C1C"));
        StackPane iconPane = new StackPane(circle, iconShield);

        Label lblTitle = new Label("Phiên Làm Việc Đã Bị Ngắt");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        VBox msgBox = new VBox(6);
        msgBox.setPadding(new Insets(12, 14, 12, 14));
        msgBox.setStyle("-fx-background-color: #FBF9F5; -fx-border-color: #EFEAE2; -fx-border-radius: 10px; -fx-background-radius: 10px;");

        Label lblMsgHeader = new Label("LÝ DO TỪ MÁY CHỦ QUẢN TRỊ:");
        lblMsgHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #92400E; -fx-letter-spacing: 0.5px;");

        Label lblReason = new Label(reason != null && !reason.trim().isEmpty() ? reason : "Phiên làm việc của bạn đã bị ngắt bởi Quản trị viên hệ thống!");
        lblReason.setWrapText(true);
        lblReason.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: #57534E; -fx-line-spacing: 2px;");
        msgBox.getChildren().addAll(lblMsgHeader, lblReason);

        String nowStr = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        Label lblMeta = new Label("Thời gian: " + nowStr + " • Thiết bị: Bàn " + (tableNumber < 10 ? "0" + tableNumber : tableNumber) + " • Trạng thái: Ngắt kết nối an toàn");
        lblMeta.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #8C827A; -fx-alignment: center;");

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER);

        Button btnBack = new Button("🚪 Trở Về Đăng Nhập");
        btnBack.setStyle("-fx-background-color: #EAE4DA; -fx-text-fill: #1C1917; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 10 16 10 16; -fx-cursor: hand;");
        cafe.utils.AnimationHelper.applyButtonPress(btnBack);

        Button btnReAuth = new Button("⟳ Gửi Yêu Cầu Nối Lại");
        btnReAuth.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 800; -fx-background-radius: 8px; -fx-padding: 10 18 10 18; -fx-cursor: hand;");
        cafe.utils.AnimationHelper.applyButtonPress(btnReAuth);

        Runnable performExit = () -> {
            modal.close();
            if (onLogout != null) {
                onLogout.run();
            } else if (onBackToFloorPlan != null) {
                onBackToFloorPlan.run();
            } else {
                primaryStage.close();
                Stage loginStage = new Stage();
                cafe.views.LoginView loginView = new cafe.views.LoginView();
                new LoginController(loginStage, loginView, socketService);
                javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
                javafx.scene.Scene scene = cafe.utils.WindowChromeHelper.createWindowScene(
                    loginStage,
                    loginView,
                    "Đăng Nhập • Brew & Bean CAFE",
                    bounds.getWidth(),
                    bounds.getHeight(),
                    true,
                    () -> {
                        socketService.disconnect();
                        System.exit(0);
                    }
                );
                loginStage.setScene(scene);
                loginStage.show();
            }
        };

        btnBack.setOnAction(e -> performExit.run());

        btnReAuth.setOnAction(e -> {
            btnReAuth.setDisable(true);
            btnReAuth.setText("⏳ Đang gửi yêu cầu...");
            new Thread(() -> {
                socketService.sendReconnectRequest(
                    "DEVICE-" + (tableNumber < 10 ? "0" + tableNumber : tableNumber),
                    tableNumber,
                    "KIOSK",
                    "Thiết bị Bàn " + tableNumber + " yêu cầu cấp lại phiên sau khi bị ngắt"
                );
                javafx.application.Platform.runLater(performExit);
            }, "Kick-Reauth-Thread").start();
        });

        actions.getChildren().addAll(btnBack, btnReAuth);
        root.getChildren().addAll(iconPane, lblTitle, msgBox, lblMeta, actions);

        cafe.utils.WindowChromeHelper.makeDraggable(modal, root);
        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.setFill(Color.TRANSPARENT);
        modal.setScene(scene);
        modal.showAndWait();
    }
}
