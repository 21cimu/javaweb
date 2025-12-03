package com.carrental.servlet;

import com.carrental.dao.OrderDao;
import com.carrental.dao.VehicleDao;
import com.carrental.model.Order;
import com.carrental.model.Vehicle;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Servlet for order management operations.
 */
@WebServlet("/api/orders/*")
public class OrderServlet extends HttpServlet {
    
    private final OrderDao orderDao = new OrderDao();
    private final VehicleDao vehicleDao = new VehicleDao();
    private final Gson gson = new Gson();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        String pathInfo = request.getPathInfo();
        String userId = request.getParameter("userId");
        String status = request.getParameter("status");
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                List<Order> orders;
                
                if (userId != null && !userId.isEmpty()) {
                    // Get orders by user ID
                    orders = orderDao.findByUserId(Long.parseLong(userId));
                } else if (status != null && !status.isEmpty()) {
                    // Filter by status
                    orders = orderDao.findByStatus(status);
                } else {
                    // Get all orders
                    orders = orderDao.findAll();
                }
                
                // Populate vehicle info for each order
                for (Order order : orders) {
                    if (order.getVehicleId() != null) {
                        Vehicle vehicle = vehicleDao.findById(order.getVehicleId());
                        order.setVehicle(vehicle);
                    }
                }
                
                out.print(gson.toJson(orders));
            } else {
                // Get order by ID or order number
                String idStr = pathInfo.substring(1);
                Order order;
                
                try {
                    Long id = Long.parseLong(idStr);
                    order = orderDao.findById(id);
                } catch (NumberFormatException e) {
                    // Try to find by order number
                    order = orderDao.findByOrderNo(idStr);
                }
                
                if (order != null) {
                    // Populate vehicle info
                    if (order.getVehicleId() != null) {
                        Vehicle vehicle = vehicleDao.findById(order.getVehicleId());
                        order.setVehicle(vehicle);
                    }
                    out.print(gson.toJson(order));
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print(createErrorResponse("Order not found"));
                }
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(createErrorResponse("Server error: " + e.getMessage()));
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        // Check if user is logged in
        HttpSession session = request.getSession(false);
        Long sessionUserId = null;
        if (session != null) {
            sessionUserId = (Long) session.getAttribute("userId");
        }
        
        try {
            JsonObject body = parseRequestBody(request);
            
            Order order = new Order();
            
            // Use session user ID or provided user ID
            if (body.has("userId")) {
                order.setUserId(body.get("userId").getAsLong());
            } else if (sessionUserId != null) {
                order.setUserId(sessionUserId);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(createErrorResponse("User ID is required"));
                return;
            }
            
            if (!body.has("vehicleId")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(createErrorResponse("Vehicle ID is required"));
                return;
            }
            
            Long vehicleId = body.get("vehicleId").getAsLong();
            Vehicle vehicle = vehicleDao.findById(vehicleId);
            
            if (vehicle == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print(createErrorResponse("Vehicle not found"));
                return;
            }
            
            if (!"AVAILABLE".equals(vehicle.getStatus())) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                out.print(createErrorResponse("Vehicle is not available"));
                return;
            }
            
            order.setVehicleId(vehicleId);
            order.setStoreId(vehicle.getStoreId());
            
            // Parse dates
            if (body.has("pickupTime")) {
                order.setPickupTime(LocalDateTime.parse(body.get("pickupTime").getAsString(), formatter));
            }
            if (body.has("returnTime")) {
                order.setReturnTime(LocalDateTime.parse(body.get("returnTime").getAsString(), formatter));
            }
            
            order.setPickupMethod(getStringOrDefault(body, "pickupMethod", "STORE"));
            order.setReturnMethod(getStringOrDefault(body, "returnMethod", "STORE"));
            
            // Calculate price
            order.setDailyPrice(vehicle.getDailyPrice());
            int rentalDays = body.has("rentalDays") ? body.get("rentalDays").getAsInt() : 1;
            order.setRentalDays(rentalDays);
            
            BigDecimal totalPrice = vehicle.getDailyPrice().multiply(BigDecimal.valueOf(rentalDays));
            order.setTotalPrice(totalPrice);
            order.setDeposit(vehicle.getDeposit());
            order.setInsuranceFee(body.has("insuranceFee") ? new BigDecimal(body.get("insuranceFee").getAsString()) : BigDecimal.ZERO);
            order.setServiceFee(body.has("serviceFee") ? new BigDecimal(body.get("serviceFee").getAsString()) : BigDecimal.ZERO);
            order.setDiscountAmount(body.has("discountAmount") ? new BigDecimal(body.get("discountAmount").getAsString()) : BigDecimal.ZERO);
            order.setCouponId(getStringOrNull(body, "couponId"));
            order.setRemark(getStringOrNull(body, "remark"));
            
            Long orderId = orderDao.create(order);
            
            if (orderId != null) {
                // Update vehicle status to RENTED
                vehicleDao.updateStatus(vehicleId, "RENTED");
                
                order.setId(orderId);
                order.setVehicle(vehicle);
                
                JsonObject result = new JsonObject();
                result.addProperty("success", true);
                result.addProperty("message", "Order created successfully");
                result.add("order", gson.toJsonTree(order));
                out.print(result);
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print(createErrorResponse("Failed to create order"));
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(createErrorResponse("Server error: " + e.getMessage()));
        }
    }
    
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        String pathInfo = request.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(createErrorResponse("Order ID is required"));
            return;
        }
        
        try {
            String[] parts = pathInfo.substring(1).split("/");
            Long orderId = Long.parseLong(parts[0]);
            
            Order existingOrder = orderDao.findById(orderId);
            if (existingOrder == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print(createErrorResponse("Order not found"));
                return;
            }
            
            // Handle action-based updates
            if (parts.length > 1) {
                String action = parts[1];
                boolean success = false;
                String message = "";
                
                switch (action) {
                    case "pay":
                        JsonObject payBody = parseRequestBody(request);
                        String paymentMethod = payBody.has("paymentMethod") ? payBody.get("paymentMethod").getAsString() : "ALIPAY";
                        success = orderDao.updatePaymentStatus(orderId, "PAID", paymentMethod);
                        orderDao.updateStatus(orderId, "CONFIRMED");
                        message = "Payment successful";
                        break;
                    case "pickup":
                        success = orderDao.updateActualPickupTime(orderId);
                        message = "Vehicle picked up";
                        break;
                    case "return":
                        success = orderDao.updateActualReturnTime(orderId);
                        // Release vehicle
                        vehicleDao.updateStatus(existingOrder.getVehicleId(), "AVAILABLE");
                        message = "Vehicle returned";
                        break;
                    case "complete":
                        success = orderDao.updateStatus(orderId, "COMPLETED");
                        message = "Order completed";
                        break;
                    case "cancel":
                        success = orderDao.updateStatus(orderId, "CANCELLED");
                        // Release vehicle if not already returned
                        if (!"RETURNED".equals(existingOrder.getStatus()) && !"COMPLETED".equals(existingOrder.getStatus())) {
                            vehicleDao.updateStatus(existingOrder.getVehicleId(), "AVAILABLE");
                        }
                        message = "Order cancelled";
                        break;
                    default:
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        out.print(createErrorResponse("Unknown action: " + action));
                        return;
                }
                
                if (success) {
                    Order updatedOrder = orderDao.findById(orderId);
                    if (updatedOrder.getVehicleId() != null) {
                        updatedOrder.setVehicle(vehicleDao.findById(updatedOrder.getVehicleId()));
                    }
                    JsonObject result = new JsonObject();
                    result.addProperty("success", true);
                    result.addProperty("message", message);
                    result.add("order", gson.toJsonTree(updatedOrder));
                    out.print(result);
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.print(createErrorResponse("Failed to update order"));
                }
            } else {
                // General status update
                JsonObject body = parseRequestBody(request);
                if (body.has("status")) {
                    boolean success = orderDao.updateStatus(orderId, body.get("status").getAsString());
                    if (success) {
                        Order updatedOrder = orderDao.findById(orderId);
                        JsonObject result = new JsonObject();
                        result.addProperty("success", true);
                        result.addProperty("message", "Order status updated");
                        result.add("order", gson.toJsonTree(updatedOrder));
                        out.print(result);
                    } else {
                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        out.print(createErrorResponse("Failed to update order"));
                    }
                } else {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print(createErrorResponse("No update fields provided"));
                }
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(createErrorResponse("Invalid order ID"));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(createErrorResponse("Server error: " + e.getMessage()));
        }
    }
    
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        String pathInfo = request.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(createErrorResponse("Order ID is required"));
            return;
        }
        
        try {
            String idStr = pathInfo.substring(1);
            Long id = Long.parseLong(idStr);
            
            Order order = orderDao.findById(id);
            if (order != null) {
                // Release vehicle if necessary
                if (order.getVehicleId() != null && 
                    !"RETURNED".equals(order.getStatus()) && 
                    !"COMPLETED".equals(order.getStatus()) &&
                    !"CANCELLED".equals(order.getStatus())) {
                    vehicleDao.updateStatus(order.getVehicleId(), "AVAILABLE");
                }
            }
            
            boolean success = orderDao.delete(id);
            
            if (success) {
                JsonObject result = new JsonObject();
                result.addProperty("success", true);
                result.addProperty("message", "Order deleted successfully");
                out.print(result);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print(createErrorResponse("Order not found"));
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(createErrorResponse("Invalid order ID"));
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(createErrorResponse("Server error: " + e.getMessage()));
        }
    }
    
    private JsonObject parseRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        String body = sb.toString();
        if (body.isEmpty()) {
            return new JsonObject();
        }
        return gson.fromJson(body, JsonObject.class);
    }
    
    private String getStringOrNull(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }
    
    private String getStringOrDefault(JsonObject obj, String key, String defaultValue) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : defaultValue;
    }
    
    private String createErrorResponse(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("success", false);
        error.addProperty("error", message);
        return error.toString();
    }
}
