package com.carrental.servlet.admin;

import com.carrental.dao.VehicleDao;
import com.carrental.dao.StoreDao;
import com.carrental.model.Vehicle;
import com.carrental.model.Store;
import com.carrental.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Admin Vehicle Management Servlet
 */
@WebServlet(name = "AdminVehicleServlet", urlPatterns = {"/api/admin/vehicles/*"})
public class AdminVehicleServlet extends HttpServlet {

    private final VehicleDao vehicleDao = new VehicleDao();
    private final StoreDao storeDao = new StoreDao();
    private final com.carrental.dao.OrderDao orderDao = new com.carrental.dao.OrderDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        if (pathInfo == null || "/".equals(pathInfo) || "/list".equals(pathInfo)) {
            handleList(request, response);
        } else if (pathInfo.startsWith("/detail/")) {
            handleDetail(request, response, pathInfo.substring(8));
        } else if ("/stats".equals(pathInfo)) {
            handleStats(request, response);
        } else {
            JsonUtil.writeError(response, 404, "接口不存在");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        if ("/create".equals(pathInfo)) {
            handleCreate(request, response);
        } else if ("/update".equals(pathInfo)) {
            handleUpdate(request, response);
        } else if ("/status".equals(pathInfo)) {
            handleUpdateStatus(request, response);
        } else if ("/delete".equals(pathInfo)) {
            // Support deletion via POST with JSON body { id: 123 }
            handleDeleteFromBody(request, response);
        } else {
            JsonUtil.writeError(response, 404, "接口不存在");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        
        if (pathInfo != null && pathInfo.startsWith("/")) {
            handleDelete(request, response, pathInfo.substring(1));
        } else {
            JsonUtil.writeError(response, 404, "接口不存在");
        }
    }

    private void handleList(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String pageStr = request.getParameter("page");
        String pageSizeStr = request.getParameter("pageSize");
        String storeIdStr = request.getParameter("storeId");
        String statusStr = request.getParameter("status");

        int page = pageStr != null ? Integer.parseInt(pageStr) : 1;
        int pageSize = pageSizeStr != null ? Integer.parseInt(pageSizeStr) : 10;

        List<Vehicle> vehicles;
        long total;

        Integer statusFilter = null;
        if (statusStr != null && !statusStr.isEmpty()) {
            try {
                statusFilter = Integer.parseInt(statusStr);
            } catch (NumberFormatException e) {
                // Invalid status parameter, return 400 error
                JsonUtil.writeError(response, 400, "无效的车辆状态参数");
                return;
            }
        }

        if (storeIdStr != null) {
            Long storeId = Long.parseLong(storeIdStr);
            if (statusFilter != null) {
                vehicles = vehicleDao.findByStoreAndStatus(storeId, statusFilter, page, pageSize);
                total = vehicleDao.countByStoreAndStatus(storeId, statusFilter);
            } else {
                vehicles = vehicleDao.findByStore(storeId, page, pageSize);
                // Count all vehicles for that store (not only available)
                total = vehicleDao.countByStore(storeId);
            }
        } else {
            if (statusFilter != null) {
                vehicles = vehicleDao.findByStatus(statusFilter, page, pageSize);
                total = vehicleDao.countByStatus(statusFilter);
            } else {
                // Return all vehicles (including offline/soft-deleted)
                vehicles = vehicleDao.findAll(page, pageSize);
                total = vehicleDao.count();
            }
        }

        // Convert vehicles to maps and parse images JSON string into actual array for frontend
        List<Map<String, Object>> mapped = new ArrayList<>();
        for (Vehicle v : vehicles) {
            mapped.add(vehicleToMap(v));
        }
        JsonUtil.writePaginated(response, mapped, page, pageSize, total);
    }

    private void handleDetail(HttpServletRequest request, HttpServletResponse response,
            String idStr) throws IOException {
        try {
            Long id = Long.parseLong(idStr);
            Vehicle vehicle = vehicleDao.findById(id);
            
            if (vehicle == null) {
                JsonUtil.writeError(response, 404, "车辆不存在");
                return;
            }

            Store store = null;
            if (vehicle.getStoreId() != null) {
                store = storeDao.findById(vehicle.getStoreId());
            }

            Map<String, Object> result = new HashMap<>();
            result.put("vehicle", vehicleToMap(vehicle));
            result.put("store", store);

            JsonUtil.writeSuccess(response, result);
        } catch (NumberFormatException e) {
            JsonUtil.writeError(response, 400, "无效的车辆ID");
        }
    }

    private void handleStats(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Map<String, Object> stats = new HashMap<>();

        stats.put("total", vehicleDao.count());
        stats.put("available", vehicleDao.countByStatus(1));
        stats.put("booked", vehicleDao.countByStatus(2));
        stats.put("rented", vehicleDao.countByStatus(3));
        stats.put("maintenance", vehicleDao.countByStatus(4));
        stats.put("cleaning", vehicleDao.countByStatus(5));
        stats.put("accident", vehicleDao.countByStatus(6));
        stats.put("offline", vehicleDao.countByStatus(0));

        JsonUtil.writeSuccess(response, stats);
    }

    private void handleCreate(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Map<String, Object> body = JsonUtil.fromJson(request, Map.class);

        Vehicle vehicle = new Vehicle();
        populateVehicle(vehicle, body);
        // Ensure default status is set to 1 (可租) when not provided
        if (vehicle.getStatus() == null) {
            vehicle.setStatus(1);
        }
        vehicle.setViewCount(0);
        vehicle.setOrderCount(0);
        vehicle.setRating(BigDecimal.valueOf(5.0));

        Long id = vehicleDao.create(vehicle);
        if (id != null) {
            vehicle.setId(id);
            
            // Update store vehicle count
            if (vehicle.getStoreId() != null) {
                updateStoreVehicleCount(vehicle.getStoreId());
            }
            
            JsonUtil.writeSuccess(response, "车辆添加成功", vehicleToMap(vehicle));
        } else {
            JsonUtil.writeError(response, 500, "添加失败");
        }
    }

    private void handleUpdate(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Map<String, Object> body = JsonUtil.fromJson(request, Map.class);
        Long id = ((Number) body.get("id")).longValue();

        Vehicle existing = vehicleDao.findById(id);
        if (existing == null) {
            JsonUtil.writeError(response, 404, "车辆不存在");
            return;
        }

        Long oldStoreId = existing.getStoreId();
        populateVehicle(existing, body);

        int result = vehicleDao.update(existing);
        if (result > 0) {
            // Update store vehicle counts if store changed
            if (oldStoreId != null && !oldStoreId.equals(existing.getStoreId())) {
                updateStoreVehicleCount(oldStoreId);
            }
            if (existing.getStoreId() != null) {
                updateStoreVehicleCount(existing.getStoreId());
            }
            
            JsonUtil.writeSuccess(response, "更新成功", vehicleToMap(existing));
        } else {
            JsonUtil.writeError(response, 500, "更新失败");
        }
    }

    private void handleUpdateStatus(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Map<String, Object> body = JsonUtil.fromJson(request, Map.class);
        Long id = ((Number) body.get("id")).longValue();
        Integer status = ((Number) body.get("status")).intValue();

        Vehicle vehicle = vehicleDao.findById(id);
        if (vehicle == null) {
            JsonUtil.writeError(response, 404, "车辆不存在");
            return;
        }

        int result = vehicleDao.updateStatus(id, status);
        if (result > 0) {
            // Update store available count
            if (vehicle.getStoreId() != null) {
                updateStoreVehicleCount(vehicle.getStoreId());
            }
            JsonUtil.writeSuccess(response, "状态更新成功", null);
        } else {
            JsonUtil.writeError(response, 500, "更新失败");
        }
    }

    private void handleDelete(HttpServletRequest request, HttpServletResponse response,
            String idStr) throws IOException {
        try {
            Long id = Long.parseLong(idStr);
            Vehicle vehicle = vehicleDao.findById(id);
            
            if (vehicle == null) {
                JsonUtil.writeError(response, 404, "车辆不存在");
                return;
            }

            // Determine hard vs soft: default to hard delete; allow soft via param hard=false in body or query
            boolean hardDelete = true;
            String hardParam = request.getParameter("hard");
            if (hardParam != null) {
                hardDelete = !("false".equalsIgnoreCase(hardParam) || "0".equals(hardParam));
            }

            // If request body contains JSON with hard flag, prefer that
            try {
                Map<String, Object> body = JsonUtil.fromJson(request, Map.class);
                if (body != null && body.containsKey("hard")) {
                    Object hv = body.get("hard");
                    if (hv instanceof Boolean) hardDelete = (Boolean) hv;
                    else if (hv instanceof String) hardDelete = !"false".equalsIgnoreCase((String) hv);
                }
            } catch (Exception e) {
                // ignore - body optional
            }

            if (!hardDelete) {
                // Soft delete path: same as before
                if (vehicle.getStatus() != null && vehicle.getStatus() == 0) {
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("code", 400);
                    resp.put("message", "车辆已下架或已删除");
                    resp.put("data", Map.of("beforeStatus", vehicle.getStatus()));
                    JsonUtil.writeJson(response, resp);
                    return;
                }
                int result = vehicleDao.updateStatus(id, 0);
                Vehicle after = vehicleDao.findById(id);
                if (result > 0) {
                    if (vehicle.getStoreId() != null) {
                        // update store counts: get current counts and decrement available
                        long total = vehicleDao.countByStore(vehicle.getStoreId());
                        storeDao.updateVehicleCounts(vehicle.getStoreId(), (int) total, (int) vehicleDao.countByStatus(1));
                    }
                    Map<String, Object> debug = new HashMap<>();
                    debug.put("updateResult", result);
                    debug.put("afterStatus", after != null ? after.getStatus() : null);
                    JsonUtil.writeSuccess(response, "删除成功(软删除)", debug);
                } else {
                    Map<String, Object> debug = new HashMap<>();
                    debug.put("updateResult", result);
                    debug.put("afterStatus", after != null ? after.getStatus() : null);
                    JsonUtil.writeError(response, 500, "删除失败: " + debug.toString());
                }
                return;
            }

            // Hard delete path: check for related orders
            long relatedOrders = orderDao.countByVehicleId(id);
            if (relatedOrders > 0) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("code", 400);
                resp.put("message", "无法删除：存在关联订单");
                resp.put("data", Map.of("orderCount", relatedOrders));
                JsonUtil.writeJson(response, resp);
                return;
            }

            // perform physical delete
            int deleted = vehicleDao.deleteById(id);
            if (deleted > 0) {
                if (vehicle.getStoreId() != null) {
                    long total = vehicleDao.countByStore(vehicle.getStoreId());
                    int available = (int) vehicleDao.countByStatus(1);
                    storeDao.updateVehicleCounts(vehicle.getStoreId(), (int) total, available);
                }
                JsonUtil.writeSuccess(response, "删除成功(物理删除)", null);
            } else {
                JsonUtil.writeError(response, 500, "删除失败");
            }

        } catch (NumberFormatException e) {
            JsonUtil.writeError(response, 400, "无效的车辆ID");
        }
    }

    // New helper to support delete via POST (JSON body)
    private void handleDeleteFromBody(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Map<String, Object> body = JsonUtil.fromJson(request, Map.class);
            if (body == null || !body.containsKey("id")) {
                JsonUtil.writeError(response, 400, "缺少车辆ID");
                return;
            }
            Object idObj = body.get("id");
            Long id;
            try {
                if (idObj instanceof Number) {
                    id = ((Number) idObj).longValue();
                } else {
                    id = Long.parseLong(String.valueOf(idObj));
                }
            } catch (NumberFormatException e) {
                JsonUtil.writeError(response, 400, "无效的车辆ID");
                return;
            }

            // simple log for debugging
            System.out.println("AdminVehicleServlet: delete request from user=" + request.getAttribute("userId") + " vehicleId=" + id);

            // reuse existing delete handler
            handleDelete(request, response, String.valueOf(id));
        } catch (IOException e) {
            JsonUtil.writeError(response, 400, "解析请求体失败");
        }
    }

    private void populateVehicle(Vehicle vehicle, Map<String, Object> body) {
        if (body.containsKey("vin")) vehicle.setVin((String) body.get("vin"));
        if (body.containsKey("plateNumber")) vehicle.setPlateNumber((String) body.get("plateNumber"));
        if (body.containsKey("brand")) vehicle.setBrand((String) body.get("brand"));
        if (body.containsKey("model")) vehicle.setModel((String) body.get("model"));
        if (body.containsKey("series")) vehicle.setSeries((String) body.get("series"));
        if (body.containsKey("year")) vehicle.setYear(((Number) body.get("year")).intValue());
        if (body.containsKey("color")) vehicle.setColor((String) body.get("color"));
        if (body.containsKey("seats")) vehicle.setSeats(((Number) body.get("seats")).intValue());
        if (body.containsKey("fuelType")) vehicle.setFuelType((String) body.get("fuelType"));
        if (body.containsKey("transmission")) vehicle.setTransmission((String) body.get("transmission"));
        if (body.containsKey("category")) vehicle.setCategory((String) body.get("category"));
        if (body.containsKey("storeId")) vehicle.setStoreId(((Number) body.get("storeId")).longValue());
        if (body.containsKey("dailyPrice")) vehicle.setDailyPrice(new BigDecimal(body.get("dailyPrice").toString()));
        if (body.containsKey("weeklyPrice")) vehicle.setWeeklyPrice(new BigDecimal(body.get("weeklyPrice").toString()));
        if (body.containsKey("monthlyPrice")) vehicle.setMonthlyPrice(new BigDecimal(body.get("monthlyPrice").toString()));
        if (body.containsKey("deposit")) vehicle.setDeposit(new BigDecimal(body.get("deposit").toString()));
        if (body.containsKey("mileage")) vehicle.setMileage(((Number) body.get("mileage")).intValue());
        if (body.containsKey("status")) vehicle.setStatus(((Number) body.get("status")).intValue());
        if (body.containsKey("mainImage")) vehicle.setMainImage((String) body.get("mainImage"));
        if (body.containsKey("images")) vehicle.setImages(JsonUtil.toJson(body.get("images")));
        if (body.containsKey("features")) vehicle.setFeatures(JsonUtil.toJson(body.get("features")));
        if (body.containsKey("description")) vehicle.setDescription((String) body.get("description"));
        if (body.containsKey("isHot")) vehicle.setIsHot((Boolean) body.get("isHot"));
        if (body.containsKey("isNew")) vehicle.setIsNew((Boolean) body.get("isNew"));
        if (body.containsKey("noDeposit")) vehicle.setNoDeposit((Boolean) body.get("noDeposit"));
        
        if (body.containsKey("purchaseDate")) {
            vehicle.setPurchaseDate(LocalDate.parse((String) body.get("purchaseDate")));
        }
        if (body.containsKey("purchasePrice")) {
            vehicle.setPurchasePrice(new BigDecimal(body.get("purchasePrice").toString()));
        }
        if (body.containsKey("insuranceExpiry")) {
            vehicle.setInsuranceExpiry(LocalDate.parse((String) body.get("insuranceExpiry")));
        }
        if (body.containsKey("inspectionExpiry")) {
            vehicle.setInspectionExpiry(LocalDate.parse((String) body.get("inspectionExpiry")));
        }
        if (body.containsKey("registrationExpiry")) {
            vehicle.setRegistrationExpiry(LocalDate.parse((String) body.get("registrationExpiry")));
        }
        if (body.containsKey("lastMaintenanceDate")) {
            Object value = body.get("lastMaintenanceDate");
            if (value instanceof String && !((String) value).isBlank()) {
                vehicle.setLastMaintenanceDate(LocalDate.parse((String) value));
            } else {
                vehicle.setLastMaintenanceDate(null);
            }
        }
    }

    private void updateStoreVehicleCount(Long storeId) {
        // This would update the store's vehicle_count and available_count
        // In a production system, this would be done with proper queries
    }

    // Convert a Vehicle object into a Map and parse images JSON string into List<String>
    private Map<String, Object> vehicleToMap(Vehicle v) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", v.getId());
        m.put("vin", v.getVin());
        m.put("plateNumber", v.getPlateNumber());
        m.put("brand", v.getBrand());
        m.put("model", v.getModel());
        m.put("series", v.getSeries());
        m.put("year", v.getYear());
        m.put("color", v.getColor());
        m.put("seats", v.getSeats());
        m.put("fuelType", v.getFuelType());
        m.put("transmission", v.getTransmission());
        m.put("category", v.getCategory());
        m.put("storeId", v.getStoreId());
        m.put("storeName", v.getStoreName());
        m.put("storeCity", v.getStoreCity());
        m.put("dailyPrice", v.getDailyPrice());
        m.put("weeklyPrice", v.getWeeklyPrice());
        m.put("monthlyPrice", v.getMonthlyPrice());
        m.put("deposit", v.getDeposit());
        m.put("mileage", v.getMileage());
        m.put("status", v.getStatus());
        m.put("mainImage", v.getMainImage());
        // parse images JSON string
        Object imagesObj = null;
        String imagesStr = v.getImages();
        if (imagesStr != null && !imagesStr.isEmpty()) {
            try {
                imagesObj = JsonUtil.fromJson(imagesStr, Object.class);
            } catch (Exception e) {
                imagesObj = null;
            }
        }
        m.put("images", imagesObj != null ? imagesObj : new ArrayList<>());
        m.put("features", v.getFeatures());
        m.put("description", v.getDescription());
        m.put("purchaseDate", v.getPurchaseDate());
        m.put("purchasePrice", v.getPurchasePrice());
        m.put("insuranceExpiry", v.getInsuranceExpiry());
        m.put("inspectionExpiry", v.getInspectionExpiry());
        m.put("registrationExpiry", v.getRegistrationExpiry());
        m.put("lastMaintenanceDate", v.getLastMaintenanceDate());
        m.put("viewCount", v.getViewCount());
        m.put("orderCount", v.getOrderCount());
        m.put("rating", v.getRating());
        m.put("isHot", v.getIsHot());
        m.put("isNew", v.getIsNew());
        m.put("noDeposit", v.getNoDeposit());
        m.put("createdAt", v.getCreatedAt());
        m.put("updatedAt", v.getUpdatedAt());
        return m;
    }
}
