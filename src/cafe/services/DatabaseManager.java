package cafe.services;

import cafe.models.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String DEFAULT_URL = "jdbc:sqlserver://localhost:1433;databaseName=CafeOrderDB;encrypt=true;trustServerCertificate=true;characterEncoding=UTF-8;";
    private static final String USER = "sa";
    private static final String PASSWORD = "cafe123";

    private static Boolean cachedAvailability = null;

    public static Connection getConnection() throws SQLException {
        DriverManager.setLoginTimeout(2);
        return DriverManager.getConnection(DEFAULT_URL, USER, PASSWORD);
    }

    public static boolean isAvailable() {
        if (cachedAvailability != null && !cachedAvailability) {
            return false;
        }
        try (Connection conn = getConnection()) {
            cachedAvailability = true;
            return true;
        } catch (Exception e) {
            cachedAvailability = false;
            return false;
        }
    }

    public static boolean saveOrder(Order order) {
        if (!isAvailable()) {
            return false;
        }

        String sqlOrder = "INSERT INTO Orders (table_id, order_type, status, total_amount, discount_amount, vat_amount, voucher_id, note) " +
                          "VALUES (?, ?, ?, ?, ?, ?, NULL, ?);";
        String sqlDetail = "INSERT INTO OrderDetails (order_id, product_id, quantity, unit_price, size_code, sugar_percent, ice_percent, temperature, customer_note) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";
        String sqlTopping = "INSERT INTO OrderDetailToppings (detail_id, topping_id, quantity, unit_price) VALUES (?, ?, ?, ?);";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            int orderId;
            try (PreparedStatement ps = conn.prepareStatement(sqlOrder, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, order.getTable() > 0 ? order.getTable() : 1);
                ps.setString(2, order.getOrderType() != null ? order.getOrderType() : "DINE_IN");
                ps.setString(3, order.getStatus() != null ? order.getStatus() : "QUEUED");
                ps.setDouble(4, order.getTotalAmount());
                ps.setDouble(5, order.getDiscountAmount());
                ps.setDouble(6, order.getVatAmount());
                ps.setString(7, order.getNote());
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        orderId = rs.getInt(1);
                        order.setOrderId(orderId);
                    } else {
                        conn.rollback();
                        return false;
                    }
                }
            }

            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    int detailId;
                    try (PreparedStatement psDetail = conn.prepareStatement(sqlDetail, Statement.RETURN_GENERATED_KEYS)) {
                        psDetail.setInt(1, orderId);
                        psDetail.setInt(2, item.getProductId() > 0 ? item.getProductId() : 1);
                        psDetail.setInt(3, item.getQty());
                        psDetail.setDouble(4, item.getUnitPrice());
                        psDetail.setString(5, item.getSizeCode());
                        psDetail.setInt(6, item.getSugarPercent());
                        psDetail.setInt(7, item.getIcePercent());
                        psDetail.setString(8, item.getTemperature());
                        psDetail.setString(9, item.getNote());
                        psDetail.executeUpdate();

                        try (ResultSet rsDetail = psDetail.getGeneratedKeys()) {
                            if (rsDetail.next()) {
                                detailId = rsDetail.getInt(1);
                            } else {
                                continue;
                            }
                        }
                    }

                    if (item.getToppings() != null && !item.getToppings().isEmpty()) {
                        try (PreparedStatement psTop = conn.prepareStatement(sqlTopping)) {
                            for (ToppingItem top : item.getToppings()) {
                                psTop.setInt(1, detailId);
                                psTop.setInt(2, top.getToppingId() > 0 ? top.getToppingId() : 1);
                                psTop.setInt(3, 1);
                                psTop.setDouble(4, top.getPrice());
                                psTop.addBatch();
                            }
                            psTop.executeBatch();
                        }
                    }
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("[DATABASE] Lỗi lưu đơn hàng vào SQL Server: " + e.getMessage());
            return false;
        }
    }
}
