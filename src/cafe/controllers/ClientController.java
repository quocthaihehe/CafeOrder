package cafe.controllers;

import cafe.models.*;
import cafe.services.ClientSocketService;
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
            currentSearchKeyword = newVal != null ? newVal.trim().toLowerCase() : "";
            refreshMenuCatalog();
        });

        // Submit order button
        view.getSubmitOrderButton().setOnAction(e -> submitOrder());

        // Socket Service Callbacks
        socketService.setOnConnectionStateChanged(connected -> {
            view.setConnectionState(connected);
            if (!connected) {
                view.showToast("⚠️ Mất kết nối tới máy chủ pha chế!");
            }
        });

        socketService.setOnOrderAckReceived(ack -> {
            cartModel.clear();
            refreshCartView();
            view.updateActiveOrderStatus(ack.orderId, MessageProtocol.translateStatus(ack.status));
            view.showToast("✅ Đã đặt đơn #" + ack.orderId + " thành công! Quầy đang chuẩn bị.");
        });

        socketService.setOnStatusUpdateReceived(update -> {
            String translated = MessageProtocol.translateStatus(update.status);
            view.updateActiveOrderStatus(update.orderId, translated);
            view.showToast("🔔 Đơn #" + update.orderId + " đã cập nhật: " + translated + "!");
        });

        socketService.setOnErrorOccurred(errMsg -> {
            view.showToast("❌ " + errMsg);
        });

        // Render initial data
        refreshMenuCatalog();
        refreshCartView();
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
        ItemDetailDialog dialog = new ItemDetailDialog(primaryStage, item, (qty, note) -> {
            cartModel.addItem(item, qty, note);
            refreshCartView();
            view.showToast("Đã thêm: " + qty + "x " + item.getName());
        });
        dialog.show();
    }

    private void refreshCartView() {
        view.renderCart(
            cartModel.getItems(),
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
            view.showToast("⚠️ Giỏ hàng đang trống!");
            return;
        }

        if (!socketService.isConnected()) {
            view.showToast("❌ Chưa kết nối được với quầy pha chế!");
            return;
        }

        boolean sent = socketService.sendOrder(tableNumber, new ArrayList<>(cartModel.getItems()));
        if (sent) {
            view.showToast("⏳ Đang gửi đơn tới quầy pha chế...");
        }
    }
}
