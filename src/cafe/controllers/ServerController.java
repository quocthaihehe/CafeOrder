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

    public ServerController(ServerKDSView view, CafeServerService serverService) {
        this.view = view;
        this.serverService = serverService;

        init();
    }

    private void init() {
        // Render initial filters
        view.renderFilters(filterList, currentFilter, this::onFilterChanged);

        // Search listener
        view.getSearchField().textProperty().addListener((obs, oldVal, newVal) -> {
            searchKeyword = newVal != null ? newVal.trim().toLowerCase() : "";
            refreshTicketBoard();
        });

        // Clear done orders button
        view.getClearDoneButton().setOnAction(e -> {
            orderList.removeIf(o -> "DONE".equalsIgnoreCase(o.getStatus()));
            refreshTicketBoard();
        });

        // Server service callbacks
        serverService.setOnOrderReceived(newOrder -> {
            orderList.add(0, newOrder); // newest on top
            refreshTicketBoard();
        });

        serverService.setOnServerLog(logMsg -> {
            view.setLatestLog(logMsg);
        });

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

    private void refreshTicketBoard() {
        List<Order> filtered = new ArrayList<>();
        int countQueued = 0;
        int countPreparing = 0;
        int countDone = 0;

        for (Order o : orderList) {
            String st = o.getStatus() != null ? o.getStatus().toUpperCase() : "QUEUED";
            if ("QUEUED".equals(st)) countQueued++;
            else if ("PREPARING".equals(st)) countPreparing++;
            else if ("DONE".equals(st)) countDone++;

            // Check filter
            boolean matchFilter = true;
            if ("Đang chờ".equalsIgnoreCase(currentFilter)) {
                matchFilter = "QUEUED".equalsIgnoreCase(st);
            } else if ("Đang pha chế".equalsIgnoreCase(currentFilter)) {
                matchFilter = "PREPARING".equalsIgnoreCase(st);
            } else if ("Đã xong".equalsIgnoreCase(currentFilter)) {
                matchFilter = "DONE".equalsIgnoreCase(st);
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
