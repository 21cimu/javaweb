package com.carrental.servlet.admin;

import com.carrental.dao.OrderDao;
import com.carrental.dao.VehicleDao;
import com.carrental.dao.UserDao;
import com.carrental.model.Order;
import com.carrental.model.Vehicle;
import com.carrental.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Admin Order Management Servlet
 */
@WebServlet(name = "AdminOrderServlet", urlPatterns = {"/api/admin/orders/*"})
public class AdminOrderServlet extends HttpServlet {

    private final OrderDao orderDao = new OrderDao();
    private final VehicleDao vehicleDao = new VehicleDao();
    private final UserDao userDao = new UserDao();

    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        if (pathInfo == null || "/".equals(pathInfo) || "/list".equals(pathInfo)) {
            handleList(request, response);
        } else if (pathInfo.startsWith("/detail/")) {
            handleDetail(request, response, pathInfo.substring(8));
        } else if ("/pending".equals(pathInfo)) {
            handlePending(request, response);
        } else if ("/stats".equals(pathInfo)) {
            handleStats(request, response);
        } else if ("/export".equals(pathInfo)) {
            handleExport(request, response);
        } else {
            JsonUtil.writeError(response, 404, "接口不存在");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        if ("/approve".equals(pathInfo)) {
            handleApprove(request, response);
        } else if ("/reject".equals(pathInfo)) {
            handleReject(request, response);
        } else if ("/pickup".equals(pathInfo)) {
            handlePickup(request, response);
        } else if ("/return".equals(pathInfo)) {
            handleReturn(request, response);
        } else if ("/complete".equals(pathInfo)) {
            handleComplete(request, response);
        } else if ("/refund".equals(pathInfo)) {
            handleRefund(request, response);
        } else {
            JsonUtil.writeError(response, 404, "接口不存在");
        }
    }

    private void handleList(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String pageStr = request.getParameter("page");
        String pageSizeStr = request.getParameter("pageSize");
        String statusStr = request.getParameter("status");
        String storeIdStr = request.getParameter("storeId");
        String orderNo = request.getParameter("orderNo");
        String startDateStr = request.getParameter("startDate");
        String endDateStr = request.getParameter("endDate");

        int page = pageStr != null ? Integer.parseInt(pageStr) : 1;
        int pageSize = pageSizeStr != null ? Integer.parseInt(pageSizeStr) : 10;
        Integer status = statusStr != null ? Integer.parseInt(statusStr) : null;
        Long storeId = storeIdStr != null ? Long.parseLong(storeIdStr) : null;
        LocalDateTime startDate = startDateStr != null ? 
            LocalDateTime.parse(startDateStr + " 00:00:00", DATE_FORMATTER) : null;
        LocalDateTime endDate = endDateStr != null ? 
            LocalDateTime.parse(endDateStr + " 23:59:59", DATE_FORMATTER) : null;

        List<Order> orders = orderDao.findAll(status, storeId, orderNo, startDate, endDate, page, pageSize);
        long total = orderDao.countAll(status, storeId, orderNo, startDate, endDate);

        JsonUtil.writePaginated(response, orders, page, pageSize, total);
    }

    private void handleDetail(HttpServletRequest request, HttpServletResponse response,
            String orderNoOrId) throws IOException {
        Order order;
        try {
            Long orderId = Long.parseLong(orderNoOrId);
            order = orderDao.findById(orderId);
        } catch (NumberFormatException e) {
            order = orderDao.findByOrderNo(orderNoOrId);
        }

        if (order == null) {
            JsonUtil.writeError(response, 404, "订单不存在");
            return;
        }

        Vehicle vehicle = vehicleDao.findById(order.getVehicleId());

        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("vehicle", vehicle);

        JsonUtil.writeSuccess(response, result);
    }

    private void handlePending(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        List<Order> orders = orderDao.findAll(1, null, null, null, null, 1, 100);
        JsonUtil.writeSuccess(response, orders);
    }

    private void handleStats(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Map<String, Object> stats = new HashMap<>();

        stats.put("total", orderDao.countAll(null, null, null, null, null));
        stats.put("pending", orderDao.countAll(1, null, null, null, null));
        stats.put("waitingPayment", orderDao.countAll(3, null, null, null, null));
        stats.put("waitingPickup", orderDao.countAll(4, null, null, null, null));
        stats.put("inUse", orderDao.countAll(5, null, null, null, null));
        stats.put("waitingReturn", orderDao.countAll(6, null, null, null, null));
        stats.put("waitingSettle", orderDao.countAll(7, null, null, null, null));
        stats.put("completed", orderDao.countAll(8, null, null, null, null));
        stats.put("cancelled", orderDao.countAll(0, null, null, null, null));

        JsonUtil.writeSuccess(response, stats);
    }

    private void handleExport(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // In production, this would generate CSV/Excel
        String statusStr = request.getParameter("status");
        String startDateStr = request.getParameter("startDate");
        String endDateStr = request.getParameter("endDate");

        Integer status = statusStr != null ? Integer.parseInt(statusStr) : null;
        LocalDateTime startDate = startDateStr != null ? 
            LocalDateTime.parse(startDateStr + " 00:00:00", DATE_FORMATTER) : null;
        LocalDateTime endDate = endDateStr != null ? 
            LocalDateTime.parse(endDateStr + " 23:59:59", DATE_FORMATTER) : null;

        List<Order> orders = orderDao.findAll(status, null, null, startDate, endDate, 1, 10000);

        // Return as JSON for now, in production convert to CSV
        response.setContentType("application/json");
        JsonUtil.writeSuccess(response, orders);
    }

    private void handleApprove(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Map<String, Object> body = JsonUtil.fromJson(request, Map.class);
        Long orderId = ((Number) body.get("orderId")).longValue();

        Order order = orderDao.findById(orderId);
        if (order == null) {
            JsonUtil.writeError(response, 404, "订单不存在");
            return;
        }

        if (order.getStatus() != 1) {
            JsonUtil.writeError(response, 400, "订单状态不正确");
            return;
        }

        int result = orderDao.updateStatus(orderId, 3); // 待支付
        if (result > 0) {
            JsonUtil.writeSuccess(response, "审核通过", null);
        } else {
            JsonUtil.writeError(response, 500, "操作失败");
        }
    }

    private void handleReject(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Map<String, Object> body = JsonUtil.fromJson(request, Map.class);
        Long orderId = ((Number) body.get("orderId")).longValue();
        String reason = (String) body.get("reason");

        Order order = orderDao.findById(orderId);
        if (order == null) {
            JsonUtil.writeError(response, 404, "订单不存在");
            return;
        }

        if (order.getStatus() != 1) {
            JsonUtil.writeError(response, 400, "订单状态不正确");
            return;
        }

        int result = orderDao.updateStatus(orderId, 2); // 审核失败
        if (result > 0) {
            // Release vehicle
            vehicleDao.updateStatus(order.getVehicleId(), 1);
            JsonUtil.writeSuccess(response, "已拒绝", null);
        } else {
            JsonUtil.writeError(response, 500, "操作失败");
        }
    }

    private void handlePickup(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Map<String, Object> body = JsonUtil.fromJson(request, Map.class);
        Long orderId = ((Number) body.get("orderId")).longValue();
        Integer mileage = ((Number) body.get("mileage")).intValue();
        Integer fuel = ((Number) body.get("fuel")).intValue();
        String images = body.get("images") != null ? JsonUtil.toJson(body.get("images")) : null;
        String note = (String) body.get("note");

        Order order = orderDao.findById(orderId);
        if (order == null) {
            JsonUtil.writeError(response, 404, "订单不存在");
            return;
        }

        if (order.getStatus() != 4) {
            JsonUtil.writeError(response, 400, "订单状态不正确");
            return;
        }

        int result = orderDao.updatePickup(orderId, LocalDateTime.now(), mileage, fuel, images, note);
        if (result > 0) {
            // Update vehicle status to rented
            vehicleDao.updateStatus(order.getVehicleId(), 3);
            JsonUtil.writeSuccess(response, "取车成功", null);
        } else {
            JsonUtil.writeError(response, 500, "操作失败");
        }
    }

    private void handleReturn(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Map<String, Object> body = JsonUtil.fromJson(request, Map.class);
        Long orderId = ((Number) body.get("orderId")).longValue();
        Integer mileage = ((Number) body.get("mileage")).intValue();
        Integer fuel = ((Number) body.get("fuel")).intValue();
        String images = body.get("images") != null ? JsonUtil.toJson(body.get("images")) : null;
        String note = (String) body.get("note");

        Order order = orderDao.findById(orderId);
        if (order == null) {
            JsonUtil.writeError(response, 404, "订单不存在");
            return;
        }

        if (order.getStatus() != 5 && order.getStatus() != 6) {
            JsonUtil.writeError(response, 400, "订单状态不正确");
            return;
        }

        // Calculate extra charges
        BigDecimal extraAmount = BigDecimal.ZERO;
        
        // Check for overtime
        if (LocalDateTime.now().isAfter(order.getReturnTime())) {
            long extraHours = java.time.temporal.ChronoUnit.HOURS.between(
                order.getReturnTime(), LocalDateTime.now());
            if (extraHours > 0) {
                BigDecimal hourlyRate = order.getDailyPrice().divide(
                    BigDecimal.valueOf(24), 2, java.math.RoundingMode.HALF_UP);
                extraAmount = extraAmount.add(hourlyRate.multiply(BigDecimal.valueOf(extraHours)));
            }
        }

        // Check for extra mileage (if applicable)
        // This would need mileage limit configuration

        int result = orderDao.updateReturn(orderId, LocalDateTime.now(), mileage, fuel, images, note, extraAmount);
        if (result > 0) {
            // Update vehicle status to cleaning
            vehicleDao.updateStatus(order.getVehicleId(), 5);
            JsonUtil.writeSuccess(response, "还车成功", Map.of("extraAmount", extraAmount));
        } else {
            JsonUtil.writeError(response, 500, "操作失败");
        }
    }

    private void handleComplete(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Map<String, Object> body = JsonUtil.fromJson(request, Map.class);
        Long orderId = ((Number) body.get("orderId")).longValue();

        Order order = orderDao.findById(orderId);
        if (order == null) {
            JsonUtil.writeError(response, 404, "订单不存在");
            return;
        }

        if (order.getStatus() != 7) {
            JsonUtil.writeError(response, 400, "订单状态不正确");
            return;
        }

        int result = orderDao.updateStatus(orderId, 8); // 已完成
        if (result > 0) {
            // Update vehicle status to available
            vehicleDao.updateStatus(order.getVehicleId(), 1);
            
            // Award points to user
            int points = order.getTotalAmount().intValue() / 10;
            userDao.updatePoints(order.getUserId(), points);
            
            JsonUtil.writeSuccess(response, "订单已完成", null);
        } else {
            JsonUtil.writeError(response, 500, "操作失败");
        }
    }

    private void handleRefund(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Map<String, Object> body = JsonUtil.fromJson(request, Map.class);
        Long orderId = ((Number) body.get("orderId")).longValue();
        BigDecimal refundAmount = new BigDecimal(body.get("refundAmount").toString());
        String reason = (String) body.get("reason");

        Order order = orderDao.findById(orderId);
        if (order == null) {
            JsonUtil.writeError(response, 404, "订单不存在");
            return;
        }

        // Process refund
        // In production, this would call payment gateway
        int result = orderDao.updateStatus(orderId, 10); // 已退款
        if (result > 0) {
            // Release vehicle if applicable
            if (order.getStatus() < 5) {
                vehicleDao.updateStatus(order.getVehicleId(), 1);
            }
            JsonUtil.writeSuccess(response, "退款成功", null);
        } else {
            JsonUtil.writeError(response, 500, "操作失败");
        }
    }
}
