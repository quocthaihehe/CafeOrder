package cafe.controllers;

import cafe.models.Order;
import cafe.services.CafeServerService;
import cafe.views.ServerKDSView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ServerController {
    private final ServerKDSView view;
    private final CafeServerService serverService;
    private final ObservableList<Order> orderList = FXCollections.observableArrayList();

    private final List<String> filterList = Arrays.asList("Tất cả đơn", "Đang chờ", "Đang pha chế", "Đã xong");
    private String currentFilter = "Tất cả đơn";
    private String searchKeyword = "";
    private final javafx.animation.PauseTransition searchDebounce = new javafx.animation.PauseTransition(javafx.util.Duration.millis(160));

    public ServerController(ServerKDSView view, CafeServerService serverService) {
        this.view = view;
        this.serverService = serverService;

        init();
    }

    private void init() {
        // Render initial filters
        view.renderFilters(filterList, currentFilter, this::onFilterChanged);

        // Search listener with debounce
        view.getSearchField().textProperty().addListener((obs, oldVal, newVal) -> {
            searchDebounce.setOnFinished(e -> {
                searchKeyword = newVal != null ? newVal.trim().toLowerCase() : "";
                refreshTicketBoard();
            });
            searchDebounce.playFromStart();
        });

        // Clear done orders button
        view.getClearDoneButton().setOnAction(e -> {
            orderList.removeIf(o -> "DONE".equalsIgnoreCase(o.getStatus()));
            refreshTicketBoard();
        });

        // Preload existing active orders from CafeServerService
        for (Order existing : serverService.getActiveOrders().values()) {
            String st = existing.getStatus() != null ? existing.getStatus().toUpperCase() : "QUEUED";
            if (!"PAID".equals(st) && !"CANCELLED".equals(st)) {
                orderList.add(existing);
            }
        }
        orderList.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

        // Server service callbacks
        serverService.setOnOrderReceived(newOrder -> {
            orderList.removeIf(o -> o.getOrderId() == newOrder.getOrderId());
            orderList.add(0, newOrder); // newest on top
            refreshTicketBoard();
        });

        serverService.setOnOrderStatusChanged((orderId, newStatus) -> {
            for (Order o : orderList) {
                if (o.getOrderId() == orderId) {
                    o.setStatus(newStatus);
                }
            }
            refreshTicketBoard();
        });

        serverService.setOnServerLog(logMsg -> {
            view.setLatestLog(logMsg);
        });

        serverService.setOnTableConnectionChanged((tbl, connected) -> {
            javafx.application.Platform.runLater(() -> {
                view.updateNetworkInfo(serverService.getActiveServerPort(), serverService.getActiveSessions().size());
            });
        });

        // Auto-refresh KDS elapsed wait time SLA badges & live session count
        javafx.animation.Timeline slaTimer = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(3), e -> {
                view.updateNetworkInfo(serverService.getActiveServerPort(), serverService.getActiveSessions().size());
                refreshTicketBoard();
            })
        );
        slaTimer.setCycleCount(javafx.animation.Animation.INDEFINITE);
        slaTimer.play();

        view.updateNetworkInfo(serverService.getActiveServerPort(), serverService.getActiveSessions().size());
        refreshTicketBoard();
    }

    private void onFilterChanged(String newFilter) {
        this.currentFilter = newFilter;
        view.renderFilters(filterList, currentFilter, this::onFilterChanged);
        refreshTicketBoard();
    }

    private void handleOrderStatusChange(Order order, String newStatus) {
        boolean updated = serverService.updateOrderStatus(order.getOrderId(), newStatus);
        if (updated) {
            order.setStatus(newStatus);
            refreshTicketBoard();
        }
    }

    // ========================================================================
    // BỘ LỌC TRẠM PHA CHẾ (KDS STATION): ALL | ESPRESSO | TEA | BAKERY
    // ========================================================================
    private static volatile String stationFilter = "ALL";
    private static ServerController activeInstance;

    public static void setStationFilter(String station) {
        stationFilter = station != null ? station : "ALL";
        if (activeInstance != null) {
            activeInstance.refreshTicketBoard();
        }
    }

    private static String stationOfCategory(String category) {
        String c = category != null ? category.toLowerCase() : "";
        if (c.contains("bánh") || c.contains("ăn") || c.contains("snack")) return "BAKERY";
        if (c.contains("cà phê") || c.contains("espresso")) return "ESPRESSO";
        return "TEA";
    }

    private boolean orderMatchesStation(Order o) {
        if ("ALL".equals(stationFilter)) return true;
        if (o.getItems() == null) return false;
        for (cafe.models.OrderItem it : o.getItems()) {
            cafe.models.MenuItem mi = cafe.models.MenuRepository.findByName(it.getName());
            String station = mi != null ? stationOfCategory(mi.getCategory()) : "ESPRESSO";
            if (station.equals(stationFilter)) return true;
        }
        return false;
    }

    private void refreshTicketBoard() {
        activeInstance = this;
        List<Order> filtered = new ArrayList<>();
        int countQueued = 0;
        int countPreparing = 0;
        int countDone = 0;

        for (Order o : orderList) {
            String st = o.getStatus() != null ? o.getStatus().toUpperCase() : "QUEUED";
            if (!orderMatchesStation(o)) continue;
            if ("QUEUED".equals(st) || "WAITING_PAYMENT".equals(st) || "CREATED".equals(st) || "CONFIRMED".equals(st)) countQueued++;
            else if ("PREPARING".equals(st)) countPreparing++;
            else if ("DONE".equals(st) || "READY".equals(st) || "SERVED".equals(st)) countDone++;

            // Check filter
            boolean matchFilter = true;
            if ("Đang chờ".equalsIgnoreCase(currentFilter)) {
                matchFilter = "QUEUED".equalsIgnoreCase(st) || "WAITING_PAYMENT".equalsIgnoreCase(st) || "CREATED".equalsIgnoreCase(st) || "CONFIRMED".equalsIgnoreCase(st);
            } else if ("Đang pha chế".equalsIgnoreCase(currentFilter)) {
                matchFilter = "PREPARING".equalsIgnoreCase(st);
            } else if ("Đã xong".equalsIgnoreCase(currentFilter)) {
                matchFilter = "DONE".equalsIgnoreCase(st) || "READY".equalsIgnoreCase(st) || "SERVED".equalsIgnoreCase(st);
            }

            // Check search
            boolean matchSearch = true;
            if (!searchKeyword.isEmpty()) {
                String idStr = String.valueOf(o.getOrderId());
                String tableStr = String.valueOf(o.getTable());
                matchSearch = idStr.contains(searchKeyword) || tableStr.contains(searchKeyword);
            }

            if (matchFilter && matchSearch) {
                filtered.add(o);
            }
        }

        view.updateStats(countQueued, countPreparing, countDone);
        view.renderOrders(filtered, this::handleOrderStatusChange);
    }
}
