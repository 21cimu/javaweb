package com.carrental.servlet.admin;

import com.carrental.dao.FundsFlowLogDao;
import com.carrental.dao.OrderDao;
import com.carrental.dao.OrderEventLogDao;
import com.carrental.dao.VehicleDao;
import com.carrental.dao.UserDao;
import com.carrental.model.FundsFlowLog;
import com.carrental.model.Order;
import com.carrental.model.OrderEventLog;
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
    private final OrderEventLogDao orderEventLogDao = new OrderEventLogDao();
    private final FundsFlowLogDao fundsFlowLogDao = new FundsFlowLogDao();

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
        } else if ("/delete".equals(pathInfo)) {
            handleDelete(request, response);
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
        // Parse filters
        String statusStr = request.getParameter("status");
        String startDateStr = request.getParameter("startDate");
        String endDateStr = request.getParameter("endDate");

        Integer status = statusStr != null ? Integer.parseInt(statusStr) : null;
        LocalDateTime startDate = startDateStr != null ?
            LocalDateTime.parse(startDateStr + " 00:00:00", DATE_FORMATTER) : null;
        LocalDateTime endDate = endDateStr != null ?
            LocalDateTime.parse(endDateStr + " 23:59:59", DATE_FORMATTER) : null;

        List<Order> orders = orderDao.findAll(status, null, null, startDate, endDate, 1, 10000);

        // Prepare CSV
        // Define headers
        String[] headers = new String[] {
            "订单ID", "订单号", "用户ID", "用户姓名", "手机号", "车辆ID", "车辆名", "车牌",
            "取车门店", "还车门店", "取车时间", "还车时间", "租期(天)", "日租金", "租金金额",
            "押金", "保险费", "服务费", "优惠金额", "额外费用", "总金额", "已支付", "退款金额",
            "状态", "创建时间"
        };

        // Build CSV content using StringBuilder
        StringBuilder sb = new StringBuilder();
        // header line
        for (int i = 0; i < headers.length; i++) {
            sb.append(escapeCsv(headers[i]));
            if (i < headers.length - 1) sb.append(',');
        }
        sb.append('\n');

        // data lines
        for (Order o : orders) {
            List<String> cols = new ArrayList<>();
            cols.add(o.getId() != null ? o.getId().toString() : "");
            cols.add(o.getOrderNo());
            cols.add(o.getUserId() != null ? o.getUserId().toString() : "");
            cols.add(o.getUserName());
            cols.add(o.getUserPhone());
            cols.add(o.getVehicleId() != null ? o.getVehicleId().toString() : "");
            cols.add(o.getVehicleName());
            cols.add(o.getVehiclePlate());
            cols.add(o.getPickupStoreName());
            cols.add(o.getReturnStoreName());
            cols.add(formatDateTime(o.getPickupTime()));
            cols.add(formatDateTime(o.getReturnTime()));
            cols.add(o.getRentalDays() != null ? o.getRentalDays().toString() : "");
            cols.add(o.getDailyPrice() != null ? o.getDailyPrice().toString() : "");
            cols.add(o.getRentalAmount() != null ? o.getRentalAmount().toString() : "");
            cols.add(o.getDeposit() != null ? o.getDeposit().toString() : "");
            cols.add(o.getInsuranceAmount() != null ? o.getInsuranceAmount().toString() : "");
            cols.add(o.getServiceAmount() != null ? o.getServiceAmount().toString() : "");
            cols.add(o.getDiscountAmount() != null ? o.getDiscountAmount().toString() : "");
            cols.add(o.getExtraAmount() != null ? o.getExtraAmount().toString() : "");
            cols.add(o.getTotalAmount() != null ? o.getTotalAmount().toString() : "");
            cols.add(o.getPaidAmount() != null ? o.getPaidAmount().toString() : "");
            cols.add(o.getRefundAmount() != null ? o.getRefundAmount().toString() : "");
            cols.add(o.getStatus() != null ? o.getStatus().toString() : "");
            cols.add(formatDateTime(o.getCreatedAt()));

            for (int i = 0; i < cols.size(); i++) {
                sb.append(escapeCsv(cols.get(i)));
                if (i < cols.size() - 1) sb.append(',');
            }
            sb.append('\n');
        }

        // Set response headers for file download
        String filename = "orders_export_" + java.time.LocalDate.now().toString() + ".csv";
        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        // For browsers to handle utf-8 bom, write BOM
        byte[] bom = new byte[] {(byte)0xEF, (byte)0xBB, (byte)0xBF};
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        // Write BOM + content
        try {
            response.getOutputStream().write(bom);
            response.getOutputStream().write(sb.toString().getBytes("UTF-8"));
            response.getOutputStream().flush();
        } catch (IOException e) {
            // Fallback to JSON error
            JsonUtil.writeError(response, 500, "导出失败");
        }
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        String v = value.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\n") || v.contains("\r") || v.contains("\"") ) {
            return "\"" + v + "\"";
        }
        return v;
    }

    private static String formatDateTime(java.time.LocalDateTime dt) {
        if (dt == null) return "";
        return dt.format(DATE_FORMATTER);
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
            logOrderEvent(order, request, "reviewed", "review", "approved");
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
            String msg = reason != null && !reason.isBlank() ? "rejected:" + reason : "rejected";
            logOrderEvent(order, request, "reviewed", "review", msg);
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
            logOrderEvent(order, request, "pickup", "fulfillment", "pickup_confirmed");
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
            logOrderEvent(order, request, "return", "fulfillment", "return_confirmed");
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
            logOrderEvent(order, request, "complete", "settlement", "order_completed");
            
            // Refund deposit to user if there's a deposit
            try {
                if (order.getDeposit() != null && order.getDeposit().compareTo(BigDecimal.ZERO) > 0) {
                    int r1 = orderDao.refundDeposit(orderId, order.getDeposit());
                    int r2 = userDao.updateBalance(order.getUserId(), order.getDeposit());
                    if (r1 <= 0 || r2 <= 0) {
                        JsonUtil.writeError(response, 500, "退还押金失败");
                        return;
                    }
                    logFundsFlow(order, request, "refund", "balance", order.getDeposit(), "deposit_refund");
                }
            } catch (Exception e) {
                JsonUtil.writeError(response, 500, "退还押金过程中发生错误");
                return;
            }

            // Award points to user based on total amount (not including refunded deposit)
            BigDecimal pointsBase = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
            // If deposit was refunded, subtract it from points base
            if (order.getDeposit() != null) {
                pointsBase = pointsBase.subtract(order.getDeposit());
            }
            int points = pointsBase.intValue() / 10;
            userDao.updatePoints(order.getUserId(), points);

            // Update user's cumulative spending and VIP level only once per order
            try {
                // Re-fetch order to get up-to-date paid/refund values and spending_accounted flag
                Order refreshed = orderDao.findById(orderId);
                java.math.BigDecimal paid = refreshed.getPaidAmount() != null ? refreshed.getPaidAmount() : java.math.BigDecimal.ZERO;
                java.math.BigDecimal refund = refreshed.getRefundAmount() != null ? refreshed.getRefundAmount() : java.math.BigDecimal.ZERO;
                java.math.BigDecimal netSpent = paid.subtract(refund);
                if (netSpent.compareTo(java.math.BigDecimal.ZERO) > 0 && (refreshed.getSpendingAccounted() == null || refreshed.getSpendingAccounted() == 0)) {
                    int inc = userDao.incrementCumulativeSpending(refreshed.getUserId(), netSpent);
                    if (inc > 0) {
                        orderDao.markSpendingAccounted(orderId);
                    }
                }
            } catch (Exception e) {
                // non-fatal: log and continue
            }

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
            logOrderEvent(order, request, "refund", "settlement", "order_refunded");
            logFundsFlow(order, request, "refund", "offline", refundAmount, "manual_refund");
            JsonUtil.writeSuccess(response, "退款成功", null);
        } else {
            JsonUtil.writeError(response, 500, "操作失败");
        }
    }

    private void handleDelete(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Map<String, Object> body = JsonUtil.fromJson(request, Map.class);
        if (body == null || !body.containsKey("orderId")) {
            JsonUtil.writeError(response, 400, "缺少订单ID");
            return;
        }

        Long orderId;
        try {
            Object idObj = body.get("orderId");
            if (idObj instanceof Number) {
                orderId = ((Number) idObj).longValue();
            } else {
                orderId = Long.parseLong(String.valueOf(idObj));
            }
        } catch (NumberFormatException e) {
            JsonUtil.writeError(response, 400, "无效的订单ID");
            return;
        }

        Order order = orderDao.findById(orderId);
        if (order == null) {
            JsonUtil.writeError(response, 404, "订单不存在");
            return;
        }

        Integer status = order.getStatus();
        if (status == null || !(status == 0 || status == 2 || status == 8 || status == 10)) {
            JsonUtil.writeError(response, 400, "当前状态不允许删除");
            return;
        }

        int result = orderDao.deleteById(orderId);
        if (result > 0) {
            JsonUtil.writeSuccess(response, "删除成功", null);
        } else {
            JsonUtil.writeError(response, 500, "删除失败");
        }
    }

    private void logOrderEvent(Order order, HttpServletRequest request,
            String eventType, String stage, String message) {
        if (order == null) return;
        try {
            OrderEventLog log = new OrderEventLog();
            log.setOrderId(order.getId());
            log.setOrderNo(order.getOrderNo());
            log.setEventType(eventType);
            log.setStage(stage);
            log.setOperatorId((Long) request.getAttribute("userId"));
            log.setOperatorName(resolveOperatorName(request, order));
            log.setOperatorRole((String) request.getAttribute("role"));
            log.setMessage(message);
            orderEventLogDao.create(log);
        } catch (Exception ignored) {
            // Avoid breaking main flow if logging fails.
        }
    }

    private void logFundsFlow(Order order, HttpServletRequest request,
            String type, String channel, BigDecimal amount, String remark) {
        if (order == null) return;
        try {
            FundsFlowLog log = new FundsFlowLog();
            log.setFlowNo(generateFlowNo());
            log.setOrderId(order.getId());
            log.setOrderNo(order.getOrderNo());
            log.setType(type);
            log.setAmount(amount != null ? amount : BigDecimal.ZERO);
            log.setChannel(channel);
            log.setOperatorId((Long) request.getAttribute("userId"));
            log.setOperatorName(resolveOperatorName(request, order));
            log.setRemark(remark);
            fundsFlowLogDao.create(log);
        } catch (Exception ignored) {
            // Avoid breaking main flow if logging fails.
        }
    }

    private String resolveOperatorName(HttpServletRequest request, Order order) {
        String name = (String) request.getAttribute("username");
        if (name != null && !name.isBlank()) {
            return name;
        }
        if (order != null && order.getUserName() != null) {
            return order.getUserName();
        }
        Long userId = (Long) request.getAttribute("userId");
        return userId != null ? String.valueOf(userId) : "";
    }

    private String generateFlowNo() {
        return "FF" + System.currentTimeMillis() +
            String.format("%04d", new java.util.Random().nextInt(10000));
    }
}
