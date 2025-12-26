package com.carrental.servlet;

import com.carrental.dao.AfterSalesOrderDao;
import com.carrental.dao.OrderDao;
import com.carrental.model.AfterSalesOrder;
import com.carrental.model.Order;
import com.carrental.util.JsonUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "AfterSalesServlet", urlPatterns = {"/api/after-sales"})
public class AfterSalesServlet extends HttpServlet {

    private final OrderDao orderDao = new OrderDao();
    private final AfterSalesOrderDao afterSalesOrderDao = new AfterSalesOrderDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");

        // 从 AuthFilter 中获取登录用户
        Long userId = (Long) req.getAttribute("userId");
        if (userId == null) {
            JsonUtil.writeError(resp, 401, "未登录或登录已过期");
            return;
        }

        Map<String, Object> body = JsonUtil.fromJson(req, Map.class);
        if (body == null) {
            JsonUtil.writeError(resp, 400, "请求体不能为空");
            return;
        }

        Long orderId = parseLong(body.get("orderId"));
        Integer type = parseInteger(body.get("type"));
        String reasonCode = parseString(body.get("reasonCode"));
        String description = parseString(body.get("description"));
        String expectedSolution = parseString(body.get("expectedSolution"));
        String contactPhone = parseString(body.get("contactPhone"));
        BigDecimal refundAmount = parseBigDecimal(body.get("refundAmount"));

        if (orderId == null || type == null || description == null || description.trim().length() < 10) {
            JsonUtil.writeError(resp, 400, "参数不完整或描述过短");
            return;
        }

        try {
            Order order = orderDao.findById(orderId);
            if (order == null) {
                JsonUtil.writeError(resp, 404, "订单不存在");
                return;
            }
            if (!order.getUserId().equals(userId)) {
                JsonUtil.writeError(resp, 403, "无权对该订单发起售后");
                return;
            }

            // 校验订单状态是否允许售后
            int status = order.getStatus() == null ? 0 : order.getStatus();
            if (!(status == 5 || status == 6 || status == 7 || status == 8 || status == 9 || status == 10)) {
                JsonUtil.writeError(resp, 400, "当前订单状态不支持售后");
                return;
            }

            // 校验支付金额
            BigDecimal paid = order.getPaidAmount() != null ? order.getPaidAmount() : order.getTotalAmount();
            if (paid == null || paid.compareTo(BigDecimal.ZERO) <= 0) {
                JsonUtil.writeError(resp, 400, "未支付订单无法发起售后");
                return;
            }

            // 检查是否已有进行中的售后单
            if (afterSalesOrderDao.hasActiveByOrderId(orderId)) {
                JsonUtil.writeError(resp, 400, "该订单已有正在处理的售后单");
                return;
            }

            // 退款类售后金额校验
            if (type == 2) {
                if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    JsonUtil.writeError(resp, 400, "退款金额必须大于 0");
                    return;
                }
                BigDecimal refunded = order.getRefundAmount() == null ? BigDecimal.ZERO : order.getRefundAmount();
                BigDecimal maxRefund = paid.subtract(refunded);
                if (refundAmount.compareTo(maxRefund) > 0) {
                    JsonUtil.writeError(resp, 400, "退款金额不能超过可退金额");
                    return;
                }
            }

            AfterSalesOrder aso = new AfterSalesOrder();
            aso.setOrderId(order.getId());
            aso.setOrderNo(order.getOrderNo());
            aso.setUserId(order.getUserId());
            aso.setUserName(order.getUserName());
            aso.setUserPhone(contactPhone != null && !contactPhone.isEmpty() ? contactPhone : order.getUserPhone());
            aso.setType(type);
            aso.setReasonCode(reasonCode);
            aso.setDescription(description);
            aso.setExpectedSolution(expectedSolution);
            aso.setRefundAmount(refundAmount);
            aso.setStatus(1); // 待审核
            aso.setAfterNo(generateAfterNo());
            aso.setCreatedAt(LocalDateTime.now());
            aso.setUpdatedAt(LocalDateTime.now());

            Long id = afterSalesOrderDao.create(aso);
            aso.setId(id);

            Map<String, Object> data = new HashMap<>();
            data.put("id", aso.getId());
            data.put("afterNo", aso.getAfterNo());
            data.put("status", aso.getStatus());

            JsonUtil.writeSuccess(resp, data);
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.writeError(resp, 500, "服务器内部错误");
        }
    }

    private String generateAfterNo() {
        return "AS" + System.currentTimeMillis();
    }

    private Long parseLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        String s = value.toString().trim();
        if (s.isEmpty()) return null;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException ignored) {
            try {
                return new BigDecimal(s).longValue();
            } catch (NumberFormatException ignored2) {
                return null;
            }
        }
    }

    private Integer parseInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        String s = value.toString().trim();
        if (s.isEmpty()) return null;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ignored) {
            try {
                return new BigDecimal(s).intValue();
            } catch (NumberFormatException ignored2) {
                return null;
            }
        }
    }

    private BigDecimal parseBigDecimal(Object value) {
        if (value == null) return null;
        String s = value.toString().trim();
        if (s.isEmpty()) return null;
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String parseString(Object value) {
        if (value == null) return null;
        String s = value.toString();
        return s == null ? null : s.trim();
    }
}
