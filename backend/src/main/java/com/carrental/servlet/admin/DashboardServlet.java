package com.carrental.servlet.admin;

import com.carrental.dao.OrderDao;
import com.carrental.dao.VehicleDao;
import com.carrental.dao.UserDao;
import com.carrental.util.JsonUtil;
import com.carrental.util.DatabaseUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Dashboard Servlet for admin dashboard data
 */
@WebServlet(name = "DashboardServlet", urlPatterns = {"/api/admin/dashboard/*"})
public class DashboardServlet extends HttpServlet {

    private final OrderDao orderDao = new OrderDao();
    private final VehicleDao vehicleDao = new VehicleDao();
    private final UserDao userDao = new UserDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        if (pathInfo == null || "/".equals(pathInfo) || "/overview".equals(pathInfo)) {
            handleOverview(request, response);
        } else if ("/stats".equals(pathInfo)) {
            handleStats(request, response);
        } else if ("/trends".equals(pathInfo)) {
            handleTrends(request, response);
        } else if ("/alerts".equals(pathInfo)) {
            handleAlerts(request, response);
        } else if ("/rankings".equals(pathInfo)) {
            handleRankings(request, response);
        } else {
            JsonUtil.writeError(response, 404, "接口不存在");
        }
    }

    private void handleOverview(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Map<String, Object> overview = new HashMap<>();

        // Today's key metrics
        Map<String, Object> today = getTodayMetrics();
        overview.put("today", today);

        // Vehicle stats
        Map<String, Object> vehicles = new HashMap<>();
        vehicles.put("total", vehicleDao.count());
        vehicles.put("available", vehicleDao.countByStatus(1));
        vehicles.put("rented", vehicleDao.countByStatus(3));
        vehicles.put("maintenance", vehicleDao.countByStatus(4));
        overview.put("vehicles", vehicles);

        // Order stats
        Map<String, Object> orders = new HashMap<>();
        orders.put("pending", orderDao.countAll(1, null, null, null, null));
        orders.put("inProgress", orderDao.countAll(5, null, null, null, null));
        orders.put("completed", orderDao.countAll(8, null, null, null, null));
        overview.put("orders", orders);

        // User stats
        Map<String, Object> users = new HashMap<>();
        users.put("total", userDao.count());
        overview.put("users", users);

        JsonUtil.writeSuccess(response, overview);
    }

    private void handleStats(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String period = request.getParameter("period");
        if (period == null) period = "week";

        Map<String, Object> stats = new HashMap<>();

        // Get statistics based on period
        if ("day".equals(period)) {
            stats.put("orderCount", getDayOrderCount());
            stats.put("revenue", getDayRevenue());
        } else if ("week".equals(period)) {
            stats.put("orderCount", getWeekOrderCount());
            stats.put("revenue", getWeekRevenue());
        } else {
            stats.put("orderCount", getMonthOrderCount());
            stats.put("revenue", getMonthRevenue());
        }

        JsonUtil.writeSuccess(response, stats);
    }

    private void handleTrends(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String days = request.getParameter("days");
        int numDays = days != null ? Integer.parseInt(days) : 7;

        List<Map<String, Object>> trends = new ArrayList<>();

        // Generate trend data for the last N days
        String sql = """
            SELECT DATE(created_at) as date, 
                   COUNT(*) as orders,
                   COALESCE(SUM(paid_amount), 0) as revenue
            FROM orders
            WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL ? DAY)
            GROUP BY DATE(created_at)
            ORDER BY date ASC
            """;

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, numDays);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> day = new HashMap<>();
                    day.put("date", rs.getDate("date").toString());
                    day.put("orders", rs.getInt("orders"));
                    day.put("revenue", rs.getBigDecimal("revenue"));
                    trends.add(day);
                }
            }
        } catch (SQLException e) {
            // Log error
        }

        JsonUtil.writeSuccess(response, trends);
    }

    private void handleAlerts(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        List<Map<String, Object>> alerts = new ArrayList<>();

        // Low inventory alert
        long availableVehicles = vehicleDao.countByStatus(1);
        if (availableVehicles < 5) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "inventory");
            alert.put("level", "warning");
            alert.put("message", "可租车辆不足，当前仅剩 " + availableVehicles + " 辆");
            alerts.add(alert);
        }

        // Pending review orders
        long pendingOrders = orderDao.countPendingReview();
        if (pendingOrders > 0) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "order");
            alert.put("level", "info");
            alert.put("message", "有 " + pendingOrders + " 个订单待审核");
            alerts.add(alert);
        }

        // Overdue returns
        List<com.carrental.model.Order> dueOrders = orderDao.findDueForReturn();
        if (!dueOrders.isEmpty()) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "return");
            alert.put("level", "warning");
            alert.put("message", "今日有 " + dueOrders.size() + " 个订单需还车");
            alerts.add(alert);
        }

        // Check for vehicles needing maintenance (insurance/inspection expiry)
        long maintenanceNeeded = getVehiclesNeedingMaintenance();
        if (maintenanceNeeded > 0) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "maintenance");
            alert.put("level", "danger");
            alert.put("message", maintenanceNeeded + " 辆车辆证照/保险即将到期");
            alerts.add(alert);
        }

        JsonUtil.writeSuccess(response, alerts);
    }

    private void handleRankings(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Map<String, Object> rankings = new HashMap<>();

        // Top vehicles by rental count
        rankings.put("topVehicles", getTopVehicles());

        // Top stores by revenue
        rankings.put("topStores", getTopStores());

        JsonUtil.writeSuccess(response, rankings);
    }

    private Map<String, Object> getTodayMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        String sql = """
            SELECT 
                COUNT(*) as order_count,
                COALESCE(SUM(CASE WHEN status >= 4 THEN paid_amount ELSE 0 END), 0) as gmv,
                COALESCE(AVG(CASE WHEN status >= 4 THEN paid_amount ELSE NULL END), 0) as avg_order
            FROM orders
            WHERE DATE(created_at) = CURDATE()
            """;

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                metrics.put("orderCount", rs.getInt("order_count"));
                metrics.put("gmv", rs.getBigDecimal("gmv"));
                metrics.put("avgOrderValue", rs.getBigDecimal("avg_order"));
            }
        } catch (SQLException e) {
            metrics.put("orderCount", 0);
            metrics.put("gmv", BigDecimal.ZERO);
            metrics.put("avgOrderValue", BigDecimal.ZERO);
        }

        // Calculate rental rate
        long totalVehicles = vehicleDao.count();
        long rentedVehicles = vehicleDao.countByStatus(3) + vehicleDao.countByStatus(2);
        if (totalVehicles > 0) {
            double rentalRate = (double) rentedVehicles / totalVehicles * 100;
            metrics.put("rentalRate", String.format("%.1f%%", rentalRate));
        } else {
            metrics.put("rentalRate", "0%");
        }

        return metrics;
    }

    private int getDayOrderCount() {
        String sql = "SELECT COUNT(*) FROM orders WHERE DATE(created_at) = CURDATE()";
        return executeCountQuery(sql);
    }

    private int getWeekOrderCount() {
        String sql = "SELECT COUNT(*) FROM orders WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)";
        return executeCountQuery(sql);
    }

    private int getMonthOrderCount() {
        String sql = "SELECT COUNT(*) FROM orders WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)";
        return executeCountQuery(sql);
    }

    private BigDecimal getDayRevenue() {
        String sql = "SELECT COALESCE(SUM(paid_amount), 0) FROM orders WHERE DATE(created_at) = CURDATE() AND status >= 4";
        return executeSumQuery(sql);
    }

    private BigDecimal getWeekRevenue() {
        String sql = "SELECT COALESCE(SUM(paid_amount), 0) FROM orders WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) AND status >= 4";
        return executeSumQuery(sql);
    }

    private BigDecimal getMonthRevenue() {
        String sql = "SELECT COALESCE(SUM(paid_amount), 0) FROM orders WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) AND status >= 4";
        return executeSumQuery(sql);
    }

    private int executeCountQuery(String sql) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            // Log error
        }
        return 0;
    }

    private BigDecimal executeSumQuery(String sql) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            // Log error
        }
        return BigDecimal.ZERO;
    }

    private long getVehiclesNeedingMaintenance() {
        String sql = """
            SELECT COUNT(*) FROM vehicles 
            WHERE insurance_expiry <= DATE_ADD(CURDATE(), INTERVAL 30 DAY)
               OR inspection_expiry <= DATE_ADD(CURDATE(), INTERVAL 30 DAY)
               OR registration_expiry <= DATE_ADD(CURDATE(), INTERVAL 30 DAY)
            """;
        return executeCountQuery(sql);
    }

    private List<Map<String, Object>> getTopVehicles() {
        List<Map<String, Object>> vehicles = new ArrayList<>();
        String sql = """
            SELECT id, brand, model, plate_number, order_count, rating
            FROM vehicles
            ORDER BY order_count DESC
            LIMIT 5
            """;

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> v = new HashMap<>();
                v.put("id", rs.getLong("id"));
                v.put("name", rs.getString("brand") + " " + rs.getString("model"));
                v.put("plateNumber", rs.getString("plate_number"));
                v.put("orderCount", rs.getInt("order_count"));
                v.put("rating", rs.getBigDecimal("rating"));
                vehicles.add(v);
            }
        } catch (SQLException e) {
            // Log error
        }
        return vehicles;
    }

    private List<Map<String, Object>> getTopStores() {
        List<Map<String, Object>> stores = new ArrayList<>();
        String sql = """
            SELECT s.id, s.name, COUNT(o.id) as order_count, 
                   COALESCE(SUM(o.paid_amount), 0) as revenue
            FROM stores s
            LEFT JOIN orders o ON o.pickup_store_id = s.id AND o.status >= 4
            GROUP BY s.id, s.name
            ORDER BY revenue DESC
            LIMIT 5
            """;

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> store = new HashMap<>();
                store.put("id", rs.getLong("id"));
                store.put("name", rs.getString("name"));
                store.put("orderCount", rs.getInt("order_count"));
                store.put("revenue", rs.getBigDecimal("revenue"));
                stores.add(store);
            }
        } catch (SQLException e) {
            // Log error
        }
        return stores;
    }
}
