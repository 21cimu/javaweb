package com.carrental.servlet.order;

import com.carrental.dao.FundsFlowLogDao;
import com.carrental.dao.OrderDao;
import com.carrental.dao.OrderEventLogDao;
import com.carrental.dao.AfterSalesOrderDao;
import com.carrental.dao.VehicleDao;
import com.carrental.dao.UserDao;
import com.carrental.dao.CouponDao;
import com.carrental.dao.StoreDao;
import com.carrental.model.AfterSalesOrder;
import com.carrental.model.FundsFlowLog;
import com.carrental.model.Order;
import com.carrental.model.OrderEventLog;
import com.carrental.model.Vehicle;
import com.carrental.model.User;
import com.carrental.model.Coupon;
import com.carrental.model.Store;
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
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Order Servlet for order management
 */
@WebServlet(name = "OrderServlet", urlPatterns = {"/api/orders/*"})
public class OrderServlet extends HttpServlet {

    private final OrderDao orderDao = new OrderDao();
    private final AfterSalesOrderDao afterSalesOrderDao = new AfterSalesOrderDao();
    private final VehicleDao vehicleDao = new VehicleDao();
    private final UserDao userDao = new UserDao();
    private final CouponDao couponDao = new CouponDao();
    private final StoreDao storeDao = new StoreDao();
    private final OrderEventLogDao orderEventLogDao = new OrderEventLogDao();
    private final FundsFlowLogDao fundsFlowLogDao = new FundsFlowLogDao();

    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        Long userId = (Long) request.getAttribute("userId");

        if (userId == null) {
            JsonUtil.writeError(response, 401, "请先登录");
            return;
        }

        if (pathInfo == null || "/".equals(pathInfo) || "/list".equals(pathInfo)) {
            handleList(request, response, userId);
        } else if (pathInfo.startsWith("/detail/")) {
            handleDetail(request, response, userId, pathInfo.substring(8));
        } else {
            JsonUtil.writeError(response, 404, "接口不存在");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        Long userId = (Long) request.getAttribute("userId");

        if (userId == null) {
            JsonUtil.writeError(response, 401, "请先登录");
            return;
        }

        if ("/create".equals(pathInfo)) {
            handleCreate(request, response, userId);
        } else if ("/pay".equals(pathInfo)) {
            handlePay(request, response, userId);
        } else if ("/cancel".equals(pathInfo)) {
            handleCancel(request, response, userId);
        } else if ("/review".equals(pathInfo)) {
            handleReview(request, response, userId);
        } else if ("/calculate".equals(pathInfo)) {
            handleCalculate(request, response, userId);
        } else {
            JsonUtil.writeError(response, 404, "接口不存在");
        }
    }

    private void handleList(HttpServletRequest request, HttpServletResponse response,
            Long userId) throws IOException {
        String statusStr = request.getParameter("status");
        String pageStr = request.getParameter("page");
        String pageSizeStr = request.getParameter("pageSize");

        Integer status = statusStr != null ? Integer.parseInt(statusStr) : null;
        int page = pageStr != null ? Integer.parseInt(pageStr) : 1;
        int pageSize = pageSizeStr != null ? Integer.parseInt(pageSizeStr) : 10;

        List<Order> orders = orderDao.findByUser(userId, status, page, pageSize);
        long total = orderDao.countByUser(userId, status);
        for (Order order : orders) {
            attachAfterSalesSummary(order);
        }

        JsonUtil.writePaginated(response, orders, page, pageSize, total);
    }

    private void handleDetail(HttpServletRequest request, HttpServletResponse response,
            Long userId, String orderNoOrId) throws IOException {
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

        if (!order.getUserId().equals(userId)) {
            JsonUtil.writeError(response, 403, "无权查看此订单");
            return;
        }

        attachAfterSalesSummary(order);

        // Get vehicle and store details
        Vehicle vehicle = vehicleDao.findById(order.getVehicleId());
        Store pickupStore = storeDao.findById(order.getPickupStoreId());
        Store returnStore = storeDao.findById(order.getReturnStoreId());

        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("vehicle", vehicle);
        result.put("pickupStore", pickupStore);
        result.put("returnStore", returnStore);

        JsonUtil.writeSuccess(response, result);
    }

    private void handleCreate(HttpServletRequest request, HttpServletResponse response,
            Long userId) throws IOException {
        Map<String, Object> body = JsonUtil.fromJson(request, Map.class);

        // Get user
        User user = userDao.findById(userId);
        if (user == null) {
            JsonUtil.writeError(response, 404, "用户不存在");
            return;
        }

        // Check verification status
        if (user.getVerificationStatus() == null || user.getVerificationStatus() != 2) {
            JsonUtil.writeError(response, 403, "请先完成实名认证和驾驶资质验证");
            return;
        }

        // Parse order details
        Long vehicleId = ((Number) body.get("vehicleId")).longValue();
        Long pickupStoreId = ((Number) body.get("pickupStoreId")).longValue();
        Long returnStoreId = body.get("returnStoreId") != null ? 
            ((Number) body.get("returnStoreId")).longValue() : pickupStoreId;
        String pickupTimeStr = (String) body.get("pickupTime");
        String returnTimeStr = (String) body.get("returnTime");
        String insuranceType = (String) body.get("insuranceType");
        String addServices = body.get("addServices") != null ? 
            JsonUtil.toJson(body.get("addServices")) : null;
        Long couponId = body.get("couponId") != null ? 
            ((Number) body.get("couponId")).longValue() : null;
        String deliveryAddress = (String) body.get("deliveryAddress");
        String deliveryCity = (String) body.get("deliveryCity");
        String deliveryDistrict = (String) body.get("deliveryDistrict");
        BigDecimal deliveryLng = body.get("deliveryLng") != null ?
            new BigDecimal(body.get("deliveryLng").toString()) : null;
        BigDecimal deliveryLat = body.get("deliveryLat") != null ?
            new BigDecimal(body.get("deliveryLat").toString()) : null;

        // Validate vehicle
        Vehicle vehicle = vehicleDao.findById(vehicleId);
        if (vehicle == null) {
            JsonUtil.writeError(response, 404, "车辆不存在");
            return;
        }
        if (vehicle.getStatus() != 1) {
            JsonUtil.writeError(response, 400, "车辆当前不可租用");
            return;
        }

        // Validate stores
        Store pickupStore = storeDao.findById(pickupStoreId);
        Store returnStore = storeDao.findById(returnStoreId);
        if (pickupStore == null || returnStore == null) {
            JsonUtil.writeError(response, 404, "门店不存在");
            return;
        }

        // Parse times
        LocalDateTime pickupTime = LocalDateTime.parse(pickupTimeStr, DATE_FORMATTER);
        LocalDateTime returnTime = LocalDateTime.parse(returnTimeStr, DATE_FORMATTER);

        if (pickupTime.isBefore(LocalDateTime.now())) {
            JsonUtil.writeError(response, 400, "取车时间不能早于当前时间");
            return;
        }
        if (returnTime.isBefore(pickupTime)) {
            JsonUtil.writeError(response, 400, "还车时间不能早于取车时间");
            return;
        }

        // Calculate rental days and amounts
        long hours = ChronoUnit.HOURS.between(pickupTime, returnTime);
        int rentalDays = (int) Math.ceil(hours / 24.0);
        if (rentalDays < 1) rentalDays = 1;

        BigDecimal dailyPrice = vehicle.getDailyPrice();
        BigDecimal rentalAmount = dailyPrice.multiply(BigDecimal.valueOf(rentalDays));
        BigDecimal deposit = vehicle.getDeposit();
        BigDecimal insuranceAmount = BigDecimal.ZERO;
        BigDecimal serviceAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;

        // Calculate insurance
        if ("basic".equals(insuranceType)) {
            insuranceAmount = BigDecimal.valueOf(30).multiply(BigDecimal.valueOf(rentalDays));
        } else if ("premium".equals(insuranceType)) {
            insuranceAmount = BigDecimal.valueOf(60).multiply(BigDecimal.valueOf(rentalDays));
        }

        // Calculate discount from coupon
        Coupon coupon = null;
        if (couponId != null) {
            coupon = couponDao.findById(couponId);
            if (coupon != null && coupon.getStatus() == 1) {
                if (coupon.getType() == 1 && rentalAmount.compareTo(coupon.getMinAmount()) >= 0) {
                    discountAmount = coupon.getDiscountAmount();
                } else if (coupon.getType() == 2) {
                    discountAmount = rentalAmount.multiply(
                        BigDecimal.ONE.subtract(coupon.getDiscountRate()));
                    if (coupon.getMaxDiscount() != null && 
                        discountAmount.compareTo(coupon.getMaxDiscount()) > 0) {
                        discountAmount = coupon.getMaxDiscount();
                    }
                } else if (coupon.getType() == 3) {
                    discountAmount = coupon.getDiscountAmount();
                }
            }
        }

        //异地还车费
        if (!pickupStoreId.equals(returnStoreId)) {
            serviceAmount = serviceAmount.add(BigDecimal.valueOf(200));
        }

        BigDecimal totalAmount = rentalAmount
            .add(deposit)
            .add(insuranceAmount)
            .add(serviceAmount)
            .subtract(discountAmount);

        // Create order
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setUserName(user.getRealName() != null ? user.getRealName() : user.getUsername());
        order.setUserPhone(user.getPhone());
        order.setVehicleId(vehicleId);
        order.setVehicleName(vehicle.getBrand() + " " + vehicle.getModel());
        order.setVehiclePlate(vehicle.getPlateNumber());
        order.setPickupStoreId(pickupStoreId);
        order.setPickupStoreName(pickupStore.getName());
        order.setReturnStoreId(returnStoreId);
        order.setReturnStoreName(returnStore.getName());
        order.setPickupTime(pickupTime);
        order.setReturnTime(returnTime);
        order.setRentalDays(rentalDays);
        order.setDailyPrice(dailyPrice);
        order.setRentalAmount(rentalAmount);
        order.setDeposit(deposit);
        order.setInsuranceAmount(insuranceAmount);
        order.setServiceAmount(serviceAmount);
        order.setExtraAmount(BigDecimal.ZERO);
        order.setDiscountAmount(discountAmount);
        order.setTotalAmount(totalAmount);
        order.setPaidAmount(BigDecimal.ZERO);
        order.setRefundAmount(BigDecimal.ZERO);
        order.setCouponId(couponId);
        order.setCouponCode(coupon != null ? coupon.getCode() : null);
        order.setStatus(1); // 待审核
        order.setPickupCode(generatePickupCode());
        order.setInsuranceType(insuranceType);
        order.setAddServices(addServices);
        order.setDeliveryAddress(deliveryAddress);
        order.setDeliveryCity(deliveryCity);
        order.setDeliveryDistrict(deliveryDistrict);
        order.setDeliveryLng(deliveryLng);
        order.setDeliveryLat(deliveryLat);

        Long orderId = orderDao.create(order);
        if (orderId == null) {
            JsonUtil.writeError(response, 500, "创建订单失败");
            return;
        }

        // Update vehicle status
        vehicleDao.updateStatus(vehicleId, 2); // 预订中
        vehicleDao.incrementOrderCount(vehicleId);

        // Update coupon if used
        if (coupon != null) {
            couponDao.incrementUsedCount(couponId);
            // Mark user's claimed coupon as used so it's no longer available
            couponDao.markUserCouponUsed(userId, couponId);
        }

        order.setId(orderId);
        logOrderEvent(order, request, "created", "order", "order_created");
        JsonUtil.writeSuccess(response, "订单创建成功", order);
    }

    private void handlePay(HttpServletRequest request, HttpServletResponse response,
            Long userId) throws IOException {
        Map<String, Object> body = JsonUtil.fromJson(request, Map.class);
        Long orderId = ((Number) body.get("orderId")).longValue();
        Integer paymentMethod = ((Number) body.get("paymentMethod")).intValue();

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

        // Simulate payment
        String paymentNo = "PAY" + System.currentTimeMillis();
        int result = orderDao.updatePayment(orderId, paymentMethod, paymentNo, order.getTotalAmount());

        if (result > 0) {
            // Update vehicle status
            vehicleDao.updateStatus(order.getVehicleId(), 2);
            logOrderEvent(order, request, "paid", "payment", "payment_success");
            logFundsFlow(order, request, "income", mapPaymentChannel(paymentMethod),
                order.getTotalAmount(), "order_payment");
            JsonUtil.writeSuccess(response, "支付成功", null);
        } else {
            JsonUtil.writeError(response, 500, "支付处理失败");
        }
    }

    private void handleCancel(HttpServletRequest request, HttpServletResponse response,
            Long userId) throws IOException {
        Map<String, Object> body = JsonUtil.fromJson(request, Map.class);
        Long orderId = ((Number) body.get("orderId")).longValue();
        String reason = (String) body.get("reason");

        Order order = orderDao.findById(orderId);
        if (order == null) {
            JsonUtil.writeError(response, 404, "订单不存在");
            return;
        }
        if (!order.getUserId().equals(userId)) {
            JsonUtil.writeError(response, 403, "无权操作此订单");
            return;
        }
        if (order.getStatus() >= 5) {
            JsonUtil.writeError(response, 400, "当前订单状态不可取消");
            return;
        }

        int result = orderDao.cancel(orderId, reason);
        if (result > 0) {
            // Release vehicle
            vehicleDao.updateStatus(order.getVehicleId(), 1);
            logOrderEvent(order, request, "cancel", "order", "order_cancelled");
            JsonUtil.writeSuccess(response, "订单已取消", null);
        } else {
            JsonUtil.writeError(response, 500, "取消失败");
        }
    }

    private void handleReview(HttpServletRequest request, HttpServletResponse response,
            Long userId) throws IOException {
        Map<String, Object> body = JsonUtil.fromJson(request, Map.class);
        Long orderId = ((Number) body.get("orderId")).longValue();
        Integer rating = ((Number) body.get("rating")).intValue();
        String review = (String) body.get("review");
        String reviewImages = body.get("reviewImages") != null ?
            JsonUtil.toJson(body.get("reviewImages")) : null;

        Order order = orderDao.findById(orderId);
        if (order == null) {
            JsonUtil.writeError(response, 404, "订单不存在");
            return;
        }
        if (!order.getUserId().equals(userId)) {
            JsonUtil.writeError(response, 403, "无权操作此订单");
            return;
        }
        if (order.getStatus() != 8) {
            JsonUtil.writeError(response, 400, "只能评价已完成的订单");
            return;
        }
        if (order.getRating() != null) {
            JsonUtil.writeError(response, 400, "订单已评价");
            return;
        }

        int result = orderDao.updateReview(orderId, rating, review, reviewImages);
        if (result > 0) {
            // Award points
            userDao.updatePoints(userId, 10);
            logOrderEvent(order, request, "review", "service", "order_reviewed");
            JsonUtil.writeSuccess(response, "评价成功", null);
        } else {
            JsonUtil.writeError(response, 500, "评价失败");
        }
    }

    private void handleCalculate(HttpServletRequest request, HttpServletResponse response,
            Long userId) throws IOException {
        Map<String, Object> body = JsonUtil.fromJson(request, Map.class);

        Long vehicleId = ((Number) body.get("vehicleId")).longValue();
        String pickupTimeStr = (String) body.get("pickupTime");
        String returnTimeStr = (String) body.get("returnTime");
        Long pickupStoreId = ((Number) body.get("pickupStoreId")).longValue();
        Long returnStoreId = body.get("returnStoreId") != null ?
            ((Number) body.get("returnStoreId")).longValue() : pickupStoreId;
        String insuranceType = (String) body.get("insuranceType");
        Long couponId = body.get("couponId") != null ?
            ((Number) body.get("couponId")).longValue() : null;

        Vehicle vehicle = vehicleDao.findById(vehicleId);
        if (vehicle == null) {
            JsonUtil.writeError(response, 404, "车辆不存在");
            return;
        }

        LocalDateTime pickupTime = LocalDateTime.parse(pickupTimeStr, DATE_FORMATTER);
        LocalDateTime returnTime = LocalDateTime.parse(returnTimeStr, DATE_FORMATTER);

        long hours = ChronoUnit.HOURS.between(pickupTime, returnTime);
        int rentalDays = (int) Math.ceil(hours / 24.0);
        if (rentalDays < 1) rentalDays = 1;

        BigDecimal dailyPrice = vehicle.getDailyPrice();
        BigDecimal rentalAmount = dailyPrice.multiply(BigDecimal.valueOf(rentalDays));
        BigDecimal deposit = vehicle.getDeposit();
        BigDecimal insuranceAmount = BigDecimal.ZERO;
        BigDecimal serviceAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;

        if ("basic".equals(insuranceType)) {
            insuranceAmount = BigDecimal.valueOf(30).multiply(BigDecimal.valueOf(rentalDays));
        } else if ("premium".equals(insuranceType)) {
            insuranceAmount = BigDecimal.valueOf(60).multiply(BigDecimal.valueOf(rentalDays));
        }

        if (couponId != null) {
            Coupon coupon = couponDao.findById(couponId);
            if (coupon != null && coupon.getStatus() == 1) {
                if (coupon.getType() == 1 && rentalAmount.compareTo(coupon.getMinAmount()) >= 0) {
                    discountAmount = coupon.getDiscountAmount();
                } else if (coupon.getType() == 2) {
                    discountAmount = rentalAmount.multiply(
                        BigDecimal.ONE.subtract(coupon.getDiscountRate()));
                    if (coupon.getMaxDiscount() != null &&
                        discountAmount.compareTo(coupon.getMaxDiscount()) > 0) {
                        discountAmount = coupon.getMaxDiscount();
                    }
                } else if (coupon.getType() == 3) {
                    discountAmount = coupon.getDiscountAmount();
                }
            }
        }

        if (!pickupStoreId.equals(returnStoreId)) {
            serviceAmount = serviceAmount.add(BigDecimal.valueOf(200));
        }

        BigDecimal totalAmount = rentalAmount
            .add(deposit)
            .add(insuranceAmount)
            .add(serviceAmount)
            .subtract(discountAmount);

        Map<String, Object> result = new HashMap<>();
        result.put("rentalDays", rentalDays);
        result.put("dailyPrice", dailyPrice);
        result.put("rentalAmount", rentalAmount);
        result.put("deposit", deposit);
        result.put("insuranceAmount", insuranceAmount);
        result.put("serviceAmount", serviceAmount);
        result.put("discountAmount", discountAmount);
        result.put("totalAmount", totalAmount);

        JsonUtil.writeSuccess(response, result);
    }

    private String generateOrderNo() {
        return "CR" + System.currentTimeMillis() + 
            String.format("%04d", new Random().nextInt(10000));
    }

    private String generatePickupCode() {
        return String.format("%06d", new Random().nextInt(1000000));
    }

    private void attachAfterSalesSummary(Order order) {
        if (order == null || order.getId() == null) {
            return;
        }
        try {
            AfterSalesOrder aso = afterSalesOrderDao.findLatestByOrderId(order.getId());
            if (aso == null) {
                return;
            }
            order.setAfterSalesId(aso.getId());
            order.setAfterSalesType(aso.getType());
            order.setAfterSalesStatus(aso.getStatus());
            order.setAfterSalesRefundAmount(aso.getRefundAmount());
            order.setAfterSalesApprovedRefundAmount(aso.getApprovedRefundAmount());
            order.setAfterSalesAuditRemark(aso.getAuditRemark());
            order.setAfterSalesAuditTime(aso.getAuditTime());
        } catch (Exception e) {
            e.printStackTrace();
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

    private String mapPaymentChannel(Integer paymentMethod) {
        if (paymentMethod == null) return "unknown";
        switch (paymentMethod) {
            case 1:
                return "wechat";
            case 2:
                return "alipay";
            case 3:
                return "bank";
            case 4:
                return "balance";
            default:
                return "unknown";
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
            String.format("%04d", new Random().nextInt(10000));
    }
}
