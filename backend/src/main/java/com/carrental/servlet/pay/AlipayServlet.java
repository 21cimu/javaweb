package com.carrental.servlet.pay;

import com.carrental.dao.FundsFlowLogDao;
import com.carrental.dao.OrderDao;
import com.carrental.dao.OrderEventLogDao;
import com.carrental.dao.VehicleDao;
import com.carrental.model.Order;
import com.carrental.model.FundsFlowLog;
import com.carrental.model.OrderEventLog;
import com.carrental.util.AlipayConfig;
import com.carrental.util.AlipaySignUtil;
import com.carrental.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Alipay Sandbox integration:
 * - POST /api/pay/alipay/create  (JWT required)
 * - POST /api/pay/alipay/notify  (public, Alipay callback)
 *
 * We return an auto-submit HTML form to the frontend so it can open a new tab and submit.
 */
@WebServlet(name = "AlipayServlet", urlPatterns = {"/api/pay/alipay/*"})
public class AlipayServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(AlipayServlet.class);

    private static final DateTimeFormatter ALIPAY_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OrderDao orderDao = new OrderDao();
    private final VehicleDao vehicleDao = new VehicleDao();
    private final OrderEventLogDao orderEventLogDao = new OrderEventLogDao();
    private final FundsFlowLogDao fundsFlowLogDao = new FundsFlowLogDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        if ("/return".equals(pathInfo)) {
            handleReturn(request, response);
        } else {
            JsonUtil.writeError(response, 404, "接口不存在");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        if ("/create".equals(pathInfo)) {
            handleCreate(request, response);
        } else if ("/notify".equals(pathInfo)) {
            handleNotify(request, response);
        } else if ("/return".equals(pathInfo)) {
            handleReturn(request, response);
        } else {
            JsonUtil.writeError(response, 404, "接口不存在");
        }
    }

    private void handleCreate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            JsonUtil.writeError(response, 401, "请先登录");
            return;
        }

        Map<String, Object> body = JsonUtil.fromJson(request, Map.class);
        Long orderId = body.get("orderId") == null ? null : ((Number) body.get("orderId")).longValue();
        if (orderId == null) {
            JsonUtil.writeError(response, 400, "缺少 orderId");
            return;
        }

        Order order = orderDao.findById(orderId);
        if (order == null) {
            JsonUtil.writeError(response, 404, "订单不存在");
            return;
        }
        if (!order.getUserId().equals(userId)) {
            JsonUtil.writeError(response, 403, "无权操作此订单");
            return;
        }
        if (order.getStatus() != 3) {
            JsonUtil.writeError(response, 400, "当前订单状态不可支付");
            return;
        }

        String outTradeNo = order.getOrderNo();
        String totalAmount = order.getTotalAmount() == null
                ? "0.00"
                : order.getTotalAmount().setScale(2, RoundingMode.HALF_UP).toPlainString();
        String subject = AlipayConfig.getSubjectPrefix() + outTradeNo;

        Map<String, String> bizContent = new HashMap<>();
        bizContent.put("out_trade_no", outTradeNo);
        bizContent.put("product_code", AlipayConfig.getProductCode());
        bizContent.put("total_amount", totalAmount);
        bizContent.put("subject", subject);

        Map<String, String> params = new HashMap<>();
        params.put("app_id", AlipayConfig.getAppId());
        params.put("method", "alipay.trade.page.pay");
        params.put("format", "JSON");
        params.put("charset", AlipayConfig.getCharset());
        params.put("sign_type", AlipayConfig.getSignType());
        params.put("timestamp", LocalDateTime.now().format(ALIPAY_TS));
        params.put("version", "1.0");
        params.put("notify_url", AlipayConfig.getNotifyUrl());
        params.put("return_url", AlipayConfig.getReturnUrl());
        params.put("biz_content", JsonUtil.toJson(bizContent));

        String sign = AlipaySignUtil.sign(params, AlipayConfig.getPrivateKey(), AlipayConfig.getCharset());
        params.put("sign", sign);

        String charset = AlipayConfig.getCharset();
        String actionUrl = buildGatewayActionUrl(AlipayConfig.getGatewayUrl(), charset);
        String form = buildAutoSubmitForm(actionUrl, params, charset);

        Map<String, Object> data = new HashMap<>();
        data.put("gateway", actionUrl);
        data.put("outTradeNo", outTradeNo);
        data.put("htmlForm", form);

        JsonUtil.writeSuccess(response, data);
    }

    private void handleNotify(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Alipay will POST form-urlencoded. We must use getParameterMap().
        Map<String, String> params = flattenParams(request.getParameterMap());

        // Verify sign first
        boolean verified = AlipaySignUtil.verify(params, AlipayConfig.getAlipayPublicKey(), AlipayConfig.getCharset());
        if (!verified) {
            logger.warn("Alipay notify sign verify failed. params={}", params);
            writePlain(response, "failure");
            return;
        }

        // Basic checks
        String appId = params.get("app_id");
        if (appId != null && !appId.equals(AlipayConfig.getAppId())) {
            logger.warn("Alipay notify app_id mismatch: {}", appId);
            writePlain(response, "failure");
            return;
        }

        String outTradeNo = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");
        String tradeStatus = params.get("trade_status");
        String totalAmountStr = params.get("total_amount");

        if (outTradeNo == null || outTradeNo.isBlank()) {
            writePlain(response, "failure");
            return;
        }

        if (!("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus))) {
            // Not a success status, acknowledge success to stop retries, but do nothing.
            logger.info("Alipay notify non-success status: out_trade_no={}, status={}", outTradeNo, tradeStatus);
            writePlain(response, "success");
            return;
        }

        Order order = orderDao.findByOrderNo(outTradeNo);
        if (order == null) {
            logger.warn("Alipay notify order not found: {}", outTradeNo);
            writePlain(response, "failure");
            return;
        }

        // Idempotency: if already paid, just ack.
        if (order.getStatus() != null && order.getStatus() >= 4) {
            writePlain(response, "success");
            return;
        }

        // Amount check
        BigDecimal notifyAmount;
        try {
            notifyAmount = new BigDecimal(totalAmountStr);
        } catch (Exception e) {
            logger.warn("Alipay notify invalid total_amount: {}", totalAmountStr);
            writePlain(response, "failure");
            return;
        }

        BigDecimal orderAmount = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
        if (orderAmount.compareTo(notifyAmount) != 0) {
            logger.warn("Alipay notify amount mismatch: out_trade_no={}, orderAmount={}, notifyAmount={}", outTradeNo, orderAmount, notifyAmount);
            writePlain(response, "failure");
            return;
        }

        // Update payment to paid. payment_method=2 means alipay (new convention).
        int updated = orderDao.updatePayment(order.getId(), 2, tradeNo == null ? "" : tradeNo, notifyAmount);
        if (updated > 0) {
            vehicleDao.updateStatus(order.getVehicleId(), 2);
            logOrderEvent(order, "paid", "payment", "alipay_paid");
            logFundsFlow(order, "income", "alipay", notifyAmount, "alipay_payment");
            writePlain(response, "success");
        } else {
            writePlain(response, "failure");
        }
    }

    private void handleReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, String> params = flattenParams(request.getParameterMap());
        String redirectUrl = AlipayConfig.getReturnRedirectUrl();
        if (redirectUrl == null || redirectUrl.isBlank()) {
            redirectUrl = "/";
        }

        if (params.isEmpty()) {
            response.sendRedirect(redirectUrl);
            return;
        }

        logger.info("Alipay return received. params={}", params);
        boolean verified = AlipaySignUtil.verify(params, AlipayConfig.getAlipayPublicKey(), AlipayConfig.getCharset());
        if (!verified) {
            logger.warn("Alipay return sign verify failed. params={}", params);
            response.sendRedirect(redirectUrl);
            return;
        }

        String appId = params.get("app_id");
        if (appId != null && !appId.equals(AlipayConfig.getAppId())) {
            logger.warn("Alipay return app_id mismatch: {}", appId);
            response.sendRedirect(redirectUrl);
            return;
        }

        String tradeStatus = params.get("trade_status");
        if (tradeStatus != null && !("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus))) {
            logger.info("Alipay return non-success status: {}", tradeStatus);
            response.sendRedirect(redirectUrl);
            return;
        }

        String outTradeNo = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");
        String totalAmountStr = params.get("total_amount");
        if (outTradeNo == null || outTradeNo.isBlank() || totalAmountStr == null || totalAmountStr.isBlank()) {
            if (outTradeNo == null || outTradeNo.isBlank()) {
                response.sendRedirect(redirectUrl);
                return;
            }
        }

        Order order = orderDao.findByOrderNo(outTradeNo);
        if (order == null) {
            logger.warn("Alipay return order not found: {}", outTradeNo);
            response.sendRedirect(redirectUrl);
            return;
        }

        // Idempotency: if already paid, just redirect.
        if (order.getStatus() != null && order.getStatus() >= 4) {
            response.sendRedirect(redirectUrl);
            return;
        }

        BigDecimal orderAmount = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
        BigDecimal returnAmount = orderAmount;
        if (totalAmountStr != null && !totalAmountStr.isBlank()) {
            try {
                returnAmount = new BigDecimal(totalAmountStr);
            } catch (Exception e) {
                logger.warn("Alipay return invalid total_amount: {}", totalAmountStr);
                response.sendRedirect(redirectUrl);
                return;
            }

            if (orderAmount.compareTo(returnAmount) != 0) {
                logger.warn("Alipay return amount mismatch: out_trade_no={}, orderAmount={}, returnAmount={}", outTradeNo, orderAmount, returnAmount);
                response.sendRedirect(redirectUrl);
                return;
            }
        }

        int updated = orderDao.updatePayment(order.getId(), 2, tradeNo == null ? "" : tradeNo, returnAmount);
        if (updated > 0) {
            vehicleDao.updateStatus(order.getVehicleId(), 2);
            logOrderEvent(order, "paid", "payment", "alipay_paid");
            logFundsFlow(order, "income", "alipay", returnAmount, "alipay_payment");
            logger.info("Alipay return updated order: out_trade_no={}, id={}", outTradeNo, order.getId());
        } else {
            logger.warn("Alipay return failed to update order: out_trade_no={}, id={}", outTradeNo, order.getId());
        }

        response.sendRedirect(redirectUrl);
    }

    private void logOrderEvent(Order order, String eventType, String stage, String message) {
        if (order == null) return;
        try {
            OrderEventLog log = new OrderEventLog();
            log.setOrderId(order.getId());
            log.setOrderNo(order.getOrderNo());
            log.setEventType(eventType);
            log.setStage(stage);
            log.setOperatorId(order.getUserId());
            log.setOperatorName(order.getUserName());
            log.setOperatorRole("user");
            log.setMessage(message);
            orderEventLogDao.create(log);
        } catch (Exception ignored) {
            // Avoid breaking payment flow if logging fails.
        }
    }

    private void logFundsFlow(Order order, String type, String channel,
            BigDecimal amount, String remark) {
        if (order == null) return;
        try {
            FundsFlowLog log = new FundsFlowLog();
            log.setFlowNo(generateFlowNo());
            log.setOrderId(order.getId());
            log.setOrderNo(order.getOrderNo());
            log.setType(type);
            log.setAmount(amount != null ? amount : BigDecimal.ZERO);
            log.setChannel(channel);
            log.setOperatorId(order.getUserId());
            log.setOperatorName(order.getUserName());
            log.setRemark(remark);
            fundsFlowLogDao.create(log);
        } catch (Exception ignored) {
            // Avoid breaking payment flow if logging fails.
        }
    }

    private String generateFlowNo() {
        return "FF" + System.currentTimeMillis() +
            String.format("%04d", new java.util.Random().nextInt(10000));
    }

    private static Map<String, String> flattenParams(Map<String, String[]> parameterMap) {
        Map<String, String> params = new HashMap<>();
        if (parameterMap == null) return params;
        for (Map.Entry<String, String[]> e : parameterMap.entrySet()) {
            String key = e.getKey();
            String[] arr = e.getValue();
            if (arr == null) continue;
            if (arr.length == 1) {
                params.put(key, arr[0]);
            } else {
                // join with comma (Alipay signature uses the raw value from request; multi-values rarely happen)
                params.put(key, String.join(",", arr));
            }
        }
        return params;
    }

    private static void writePlain(HttpServletResponse response, String text) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(text);
    }

    private static String buildAutoSubmitForm(String action, Map<String, String> params, String charset) {
        String cs = (charset == null || charset.isBlank()) ? "utf-8" : charset;

        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html><html><head><meta charset='").append(escapeHtml(cs)).append("'><title>Redirecting...</title></head><body>");
        sb.append("<form id='alipayForm' name='alipayForm' action='")
                .append(escapeHtml(action))
                .append("' method='post' accept-charset='")
                .append(escapeHtml(cs))
                .append("'>");
        for (Map.Entry<String, String> e : params.entrySet()) {
            String k = e.getKey();
            String v = e.getValue();
            if (k == null || v == null) continue;
            sb.append("<input type='hidden' name='").append(escapeHtml(k)).append("' value='").append(escapeHtml(v)).append("' />");
        }
        sb.append("</form>");
        sb.append("<script>document.getElementById('alipayForm').submit();</script>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private static String buildGatewayActionUrl(String gatewayUrl, String charset) {
        if (gatewayUrl == null || gatewayUrl.isBlank()) return "";
        String cs = (charset == null || charset.isBlank()) ? null : charset.trim();
        if (cs == null || cs.isEmpty()) return gatewayUrl;

        // Alipay gateway error "invalid-signature" often happens when charset isn't specified in the URL.
        String lower = gatewayUrl.toLowerCase();
        if (lower.contains("charset=")) return gatewayUrl;

        return gatewayUrl + (gatewayUrl.contains("?") ? "&" : "?") + "charset=" + cs;
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
