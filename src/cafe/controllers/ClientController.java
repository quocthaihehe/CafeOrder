package cafe.controllers;

import cafe.models.*;
import cafe.services.ClientSocketService;
import cafe.utils.CurrencyFormatter;
import cafe.views.ClientView;
import cafe.views.ItemDetailDialog;
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

        // Category & Search bindings
        view.renderCategories(MenuRepository.getCategories(), currentCategory, this::onCategorySelected);

        view.getSearchField().textProperty().addListener((obs, oldVal, newVal) -> {
            searchDebounce.setOnFinished(e -> {
                currentSearchKeyword = newVal != null ? newVal.trim().toLowerCase() : "";
                refreshMenuCatalog();
            });
            searchDebounce.playFromStart();
        });

        // Order Type Selector
        view.getOrderTypeGroup().selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == view.getBtnTakeaway()) {
                cartModel.setOrderType("TAKEAWAY");
                view.showToast("Đã chọn hình thức: Mang về");
            } else {
                cartModel.setOrderType("DINE_IN");
                view.showToast("Đã chọn hình thức: Dùng tại bàn");
            }
        });

        // Voucher Apply Button
        view.getApplyVoucherButton().setOnAction(e -> applyVoucherCode());

        // Submit order button
        view.getSubmitOrderButton().setOnAction(e -> submitOrder());

        // Socket Service Callbacks
        socketService.setOnConnectionStateChanged(connected -> {
            view.setConnectionState(connected);
            if (!connected) {
                view.showToast("Mất kết nối tới máy chủ pha chế!");
            }
        });

        socketService.setOnOrderAckReceived(ack -> {
            cartModel.clear();
            view.getVoucherField().clear();
            view.getVoucherFeedbackLabel().setVisible(false);
            view.getVoucherFeedbackLabel().setManaged(false);
            refreshCartView();
            view.updateActiveOrderStatus(ack.orderId, MessageProtocol.translateStatus(ack.status));
            view.showToast("Đã gửi đơn #" + ack.orderId + " thành công! Bếp đang chuẩn bị.");
        });

        socketService.setOnStatusUpdateReceived(update -> {
            String translated = MessageProtocol.translateStatus(update.status);
            view.updateActiveOrderStatus(update.orderId, translated);
            view.showToast("Đơn #" + update.orderId + " đã cập nhật: " + translated + "!");
        });

        socketService.setOnErrorOccurred(errMsg -> {
            view.showToast(errMsg);
        });

        // Render initial data
        refreshMenuCatalog();
        refreshCartView();
    }

    private void applyVoucherCode() {
        String code = view.getVoucherField().getText().trim().toUpperCase();
        if (code.isEmpty()) {
            view.showToast("Vui lòng nhập mã giảm giá!");
            return;
        }
        Voucher v = MenuRepository.getVoucher(code);
        if (v == null) {
            view.getVoucherFeedbackLabel().setText("Mã " + code + " không hợp lệ!");
            view.getVoucherFeedbackLabel().setStyle("-fx-text-fill: #D32F2F; -fx-font-size: 10.5px; -fx-font-weight: 700;");
            view.getVoucherFeedbackLabel().setVisible(true);
            view.getVoucherFeedbackLabel().setManaged(true);
            return;
        }
        if (cartModel.getSubtotalAmount() < v.getMinOrderAmount()) {
            view.getVoucherFeedbackLabel().setText("Đơn tối thiểu " + CurrencyFormatter.format(v.getMinOrderAmount()) + " mới được dùng mã!");
            view.getVoucherFeedbackLabel().setStyle("-fx-text-fill: #E65100; -fx-font-size: 10.5px; -fx-font-weight: 700;");
            view.getVoucherFeedbackLabel().setVisible(true);
            view.getVoucherFeedbackLabel().setManaged(true);
            return;
        }

        boolean applied = cartModel.applyVoucher(v);
        if (applied) {
            refreshCartView();
            view.getVoucherFeedbackLabel().setText(v.getDescription());
            view.getVoucherFeedbackLabel().setStyle("-fx-text-fill: #2E7D32; -fx-font-size: 10.5px; -fx-font-weight: 700;");
            view.getVoucherFeedbackLabel().setVisible(true);
            view.getVoucherFeedbackLabel().setManaged(true);
            view.showToast("Áp dụng thành công voucher " + code + "!");
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

        view.renderMenuItems(filtered, this::openItemCustomization);
    }

    private void openItemCustomization(MenuItem item) {
        ItemDetailDialog dialog = new ItemDetailDialog(primaryStage, item, orderItem -> {
            cartModel.addItem(orderItem);
            refreshCartView();
            view.showToast("Đã thêm: " + orderItem.getQty() + "x " + orderItem.getName());
        });
        dialog.show();
    }

    private void refreshCartView() {
        view.renderCart(
            cartModel.getItems(),
            cartModel.getSubtotalAmount(),
            cartModel.getDiscountAmount(),
            cartModel.getTotalAmount(),
            cartModel.getTotalQuantity(),
            this::onCartQtyChanged,
            this::onCartItemDeleted
        );
    }

    private void onCartQtyChanged(OrderItem item, int delta) {
        cartModel.updateQuantity(item, delta);
        refreshCartView();
    }

    private void onCartItemDeleted(OrderItem item) {
        cartModel.removeItem(item);
        refreshCartView();
    }

    private void submitOrder() {
        if (cartModel.isEmpty()) {
            view.showToast("Giỏ hàng đang trống!");
            return;
        }

        if (!socketService.isConnected()) {
            view.showToast("Chưa kết nối được với quầy pha chế!");
            return;
        }

        boolean sent = socketService.sendOrder(
            tableNumber,
            cartModel.getOrderType(),
            cartModel.getDiscountAmount(),
            cartModel.getAppliedVoucher() != null ? cartModel.getAppliedVoucher().getCode() : null,
            null,
            new ArrayList<>(cartModel.getItems())
        );

        if (sent) {
            view.showToast("Đang gửi đơn tới quầy pha chế...");
        }
    }
}
