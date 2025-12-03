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
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Admin Vehicle Management Servlet
 */
@WebServlet(name = "AdminVehicleServlet", urlPatterns = {"/api/admin/vehicles/*"})
public class AdminVehicleServlet extends HttpServlet {

    private final VehicleDao vehicleDao = new VehicleDao();
    private final StoreDao storeDao = new StoreDao();

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

        if (storeIdStr != null) {
            Long storeId = Long.parseLong(storeIdStr);
            vehicles = vehicleDao.findByStore(storeId, page, pageSize);
            total = vehicleDao.countAvailable(null, null, storeId, null, null, null, null);
        } else {
            vehicles = vehicleDao.findAll(page, pageSize);
            total = vehicleDao.count();
        }

        JsonUtil.writePaginated(response, vehicles, page, pageSize, total);
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
            result.put("vehicle", vehicle);
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
            
            JsonUtil.writeSuccess(response, "车辆添加成功", vehicle);
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
            
            JsonUtil.writeSuccess(response, "更新成功", existing);
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

            // Soft delete by setting status to 0
            int result = vehicleDao.updateStatus(id, 0);
            if (result > 0) {
                if (vehicle.getStoreId() != null) {
                    updateStoreVehicleCount(vehicle.getStoreId());
                }
                JsonUtil.writeSuccess(response, "删除成功", null);
            } else {
                JsonUtil.writeError(response, 500, "删除失败");
            }
        } catch (NumberFormatException e) {
            JsonUtil.writeError(response, 400, "无效的车辆ID");
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
    }

    private void updateStoreVehicleCount(Long storeId) {
        // This would update the store's vehicle_count and available_count
        // In a production system, this would be done with proper queries
    }
}
