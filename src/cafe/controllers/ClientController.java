package cafe.controllers;

import cafe.models.*;
import cafe.services.ClientSocketService;
import cafe.utils.CurrencyFormatter;
import cafe.views.CartModalDialog;
import cafe.views.ClientView;
import cafe.views.ItemDetailDialog;
import cafe.views.OrderSuccessDialog;
import cafe.views.PaymentDialog;
import javafx.stage.Stage;
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

    // Stored transaction state for OrderSuccess confirmation slip
    private String lastPaymentMethod = "TIỀN MẶT";
    private double lastOrderTotal = 0.0;
    private CartModalDialog activeCartDialog = null;

    public ClientController(Stage primaryStage, ClientView view, ClientSocketService socketService, int tableNumber) {
        this.primaryStage = primaryStage;
        this.view = view;
        this.socketService = socketService;
        this.tableNumber = tableNumber;
        this.cartModel = new CartModel();

        init();
    }

    private void init() {
        view.setTableNumber(tableNumber);
        view.setConnectionState(socketService.isConnected());
        view.updateCartBadge(cartModel.getTotalQuantity());

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
            double finalTotal = lastOrderTotal > 0 ? lastOrderTotal : cartModel.getTotalAmount();
            String finalMethod = lastPaymentMethod;

            cartModel.clear();
            view.updateCartBadge(0);
            view.updateActiveOrderStatus(confirmedOrderId, MessageProtocol.translateStatus(ack.status));

            if (activeCartDialog != null && activeCartDialog.isShowing()) {
                activeCartDialog.close();
            }

            // Hiển thị Order Confirmation Screen
            OrderSuccessDialog successDialog = new OrderSuccessDialog(
                primaryStage,
                confirmedOrderId,
                tableNumber,
                finalTotal,
                finalMethod,
                () -> view.showToast("Đơn #" + confirmedOrderId + " đang được pha chế!")
            );
            successDialog.show();
        });

        socketService.setOnStatusUpdateReceived(update -> {
            String translated = MessageProtocol.translateStatus(update.status);
            view.updateActiveOrderStatus(update.orderId, translated);
            view.showToast("Đơn #" + update.orderId + " đã cập nhật: " + translated + "!");
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
            cartModel,
            this::onCartQtyChanged,
            this::onCartItemDeleted,
            this::applyVoucherCodeFromDialog,
            this::openPaymentFlow
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

        view.renderMenuItems(filtered, this::openItemCustomization, this::onQuickAdd);
    }

    private void onQuickAdd(MenuItem item) {
        // Quick add default configuration (Size M, base price)
        OrderItem oi = new OrderItem(
            item.getId(),
            item.getName(),
            1,
            item.getPrice(),
            "M",
            0.0,
            100,
            100,
            "COLD",
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

        if (!socketService.isConnected()) {
            view.showToast("Chưa kết nối được với quầy pha chế!");
            return;
        }

        PaymentDialog paymentDialog = new PaymentDialog(primaryStage, tableNumber, cartModel, selectedMethod -> {
            this.lastPaymentMethod = selectedMethod;
            this.lastOrderTotal = cartModel.getTotalAmount();
            submitOrder(selectedMethod);
        });
        paymentDialog.show();
    }

    private void submitOrder(String paymentMethod) {
        boolean sent = socketService.sendOrder(
            tableNumber,
            cartModel.getOrderType(),
            cartModel.getDiscountAmount(),
            cartModel.getAppliedVoucher() != null ? cartModel.getAppliedVoucher().getCode() : null,
            "Thanh toán: " + paymentMethod,
            new ArrayList<>(cartModel.getItems())
        );

        if (sent) {
            view.showToast("Đang gửi đơn tới quầy pha chế...");
        }
    }
}
