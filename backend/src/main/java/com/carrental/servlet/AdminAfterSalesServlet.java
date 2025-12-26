package com.carrental.servlet;

import com.carrental.dao.AfterSalesOrderDao;
import com.carrental.dao.FundsFlowLogDao;
import com.carrental.dao.OrderDao;
import com.carrental.dao.OrderEventLogDao;
import com.carrental.model.AfterSalesOrder;
import com.carrental.model.FundsFlowLog;
import com.carrental.model.Order;
import com.carrental.model.OrderEventLog;
import com.carrental.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin after-sales management servlet
 */
@WebServlet(name = "AdminAfterSalesServlet", urlPatterns = {"/api/admin/after-sales", "/api/admin/after-sales/audit"})
public class AdminAfterSalesServlet extends HttpServlet {

    private final AfterSalesOrderDao afterSalesOrderDao = new AfterSalesOrderDao();
    private final OrderDao orderDao = new OrderDao();
    private final OrderEventLogDao orderEventLogDao = new OrderEventLogDao();
    private final FundsFlowLogDao fundsFlowLogDao = new FundsFlowLogDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 列表查询
        int page = parseInt(req.getParameter("page"), 1);
        int pageSize = parseInt(req.getParameter("pageSize"), 10);
        Integer status = parseInteger(req.getParameter("status"));
        String orderNo = req.getParameter("orderNo");
        Long userId = parseLong(req.getParameter("userId"));
        LocalDateTime startDate = parseDateTime(req.getParameter("startDate"));
        LocalDateTime endDate = parseDateTime(req.getParameter("endDate"));

        List<AfterSalesOrder> list = afterSalesOrderDao.findAll(status, orderNo, userId, startDate, endDate, page, pageSize);
        long total = afterSalesOrderDao.countAll(status, orderNo, userId, startDate, endDate);

        JsonUtil.writePaginated(resp, list, page, pageSize, (int) total);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String uri = req.getRequestURI();
        if (uri.endsWith("/audit")) {
            handleAudit(req, resp);
        } else {
            JsonUtil.writeError(resp, 404, "Not Found");
        }
    }

    private void handleAudit(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> body = JsonUtil.fromJson(req, Map.class);
        if (body == null) {
            JsonUtil.writeError(resp, 400, "请求体不能为空");
            return;
        }

        Object idObj = body.get("id");
        Long id = idObj instanceof Number ? ((Number) idObj).longValue() : parseLong(idObj == null ? null : idObj.toString());
        String decision = (String) body.get("decision"); // approve / reject
        BigDecimal approvedRefundAmount = null;
        if (body.get("approvedRefundAmount") != null) {
            approvedRefundAmount = new BigDecimal(body.get("approvedRefundAmount").toString());
        }
        String remark = (String) body.get("remark");

        if (id == null || decision == null) {
            JsonUtil.writeError(resp, 400, "缺少必要参数");
            return;
        }

        // 管理员信息从 AuthFilter 注入
        Long adminId = (Long) req.getAttribute("userId");
        String adminName = (String) req.getAttribute("username");

        AfterSalesOrder aso;
        try {
            aso = afterSalesOrderDao.findById(id);
        } catch (Exception e) {
            JsonUtil.writeError(resp, 500, "读取售后单失败");
            return;
        }
        if (aso == null) {
            JsonUtil.writeError(resp, 404, "售后单不存在");
            return;
        }
        if (aso.getStatus() == null || aso.getStatus() != 1) {
            JsonUtil.writeError(resp, 400, "当前状态不允许审核");
            return;
        }

        try {
            if ("approve".equalsIgnoreCase(decision)) {
                int newStatus = 2; // 已同意
                if (aso.getType() != null && aso.getType() == 2) { // 退款类
                    if (approvedRefundAmount == null || approvedRefundAmount.compareTo(BigDecimal.ZERO) <= 0) {
                        JsonUtil.writeError(resp, 400, "请填写有效的退款金额");
                        return;
                    }
                    // 更新订单退款金额
                    Order order = orderDao.findById(aso.getOrderId());
                    if (order == null) {
                        JsonUtil.writeError(resp, 404, "关联订单不存在");
                        return;
                    }
                    // 使用已有的 refundDeposit 方法叠加退款金额
                    orderDao.refundDeposit(order.getId(), approvedRefundAmount);
                    // 订单状态标记为退款中或已退款，这里简单标记为已退款(10)
                    orderDao.updateStatus(order.getId(), 10);
                    logOrderEvent(order, req, "refund", "after-sales", "after_sales_refund");
                    logFundsFlow(order, req, "refund", "offline", approvedRefundAmount, "after_sales_refund");
                    newStatus = 4; // 售后单状态置为已完成
                }
                afterSalesOrderDao.updateStatus(id, newStatus, approvedRefundAmount, adminId, adminName, remark);
            } else if ("reject".equalsIgnoreCase(decision)) {
                afterSalesOrderDao.updateStatus(id, 3, approvedRefundAmount, adminId, adminName, remark);
            } else {
                JsonUtil.writeError(resp, 400, "不支持的决策类型");
                return;
            }
        } catch (Exception e) {
            JsonUtil.writeError(resp, 500, "处理失败");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("decision", decision);
        JsonUtil.writeSuccess(resp, data);
    }

    private int parseInt(String s, int defaultValue) {
        try {
            return s == null ? defaultValue : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Integer parseInteger(String s) {
        try {
            return s == null || s.isBlank() ? null : Integer.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long parseLong(String s) {
        try {
            return s == null || s.isBlank() ? null : Long.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(String s) {
        if (s == null || s.isBlank()) return null;
        // 兼容仅日期字符串
        if (s.length() == 10) {
            LocalDate d = LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return d.atStartOfDay();
        }
        return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
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
