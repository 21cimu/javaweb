package com.carrental.servlet;

import com.carrental.dao.VehicleDao;
import com.carrental.model.Vehicle;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.List;

/**
 * Servlet for vehicle management operations.
 */
@WebServlet("/api/vehicles/*")
public class VehicleServlet extends HttpServlet {
    
    private final VehicleDao vehicleDao = new VehicleDao();
    private final Gson gson = new Gson();
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        String pathInfo = request.getPathInfo();
        String category = request.getParameter("category");
        String keyword = request.getParameter("keyword");
        String available = request.getParameter("available");
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                List<Vehicle> vehicles;
                
                if (keyword != null && !keyword.isEmpty()) {
                    // Search by keyword
                    vehicles = vehicleDao.search(keyword);
                } else if (category != null && !category.isEmpty()) {
                    // Filter by category
                    vehicles = vehicleDao.findByCategory(category);
                } else if ("true".equals(available)) {
                    // Get available only
                    vehicles = vehicleDao.findAvailable();
                } else {
                    // Get all
                    vehicles = vehicleDao.findAll();
                }
                
                out.print(gson.toJson(vehicles));
            } else {
                // Get vehicle by ID
                String idStr = pathInfo.substring(1);
                Long id = Long.parseLong(idStr);
                Vehicle vehicle = vehicleDao.findById(id);
                
                if (vehicle != null) {
                    out.print(gson.toJson(vehicle));
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print(createErrorResponse("Vehicle not found"));
                }
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(createErrorResponse("Invalid vehicle ID"));
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
        
        try {
            JsonObject body = parseRequestBody(request);
            
            Vehicle vehicle = new Vehicle();
            vehicle.setPlateNumber(getStringOrNull(body, "plateNumber"));
            vehicle.setVin(getStringOrNull(body, "vin"));
            vehicle.setBrand(getStringOrNull(body, "brand"));
            vehicle.setModel(getStringOrNull(body, "model"));
            vehicle.setCategory(getStringOrNull(body, "category"));
            vehicle.setColor(getStringOrNull(body, "color"));
            vehicle.setSeats(body.has("seats") ? body.get("seats").getAsInt() : 5);
            vehicle.setTransmission(getStringOrNull(body, "transmission"));
            vehicle.setFuelType(getStringOrNull(body, "fuelType"));
            vehicle.setDailyPrice(getBigDecimalOrDefault(body, "dailyPrice", BigDecimal.ZERO));
            vehicle.setDeposit(getBigDecimalOrDefault(body, "deposit", BigDecimal.ZERO));
            vehicle.setDepositFree(body.has("depositFree") && body.get("depositFree").getAsBoolean());
            vehicle.setStatus("AVAILABLE");
            vehicle.setStoreId(body.has("storeId") ? body.get("storeId").getAsLong() : null);
            vehicle.setImageUrl(getStringOrNull(body, "imageUrl"));
            vehicle.setDescription(getStringOrNull(body, "description"));
            vehicle.setMileage(body.has("mileage") ? body.get("mileage").getAsInt() : 0);
            
            Long vehicleId = vehicleDao.create(vehicle);
            
            if (vehicleId != null) {
                vehicle.setId(vehicleId);
                JsonObject result = new JsonObject();
                result.addProperty("success", true);
                result.addProperty("message", "Vehicle created successfully");
                result.add("vehicle", gson.toJsonTree(vehicle));
                out.print(result);
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print(createErrorResponse("Failed to create vehicle"));
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
            out.print(createErrorResponse("Vehicle ID is required"));
            return;
        }
        
        try {
            String idStr = pathInfo.substring(1);
            Long id = Long.parseLong(idStr);
            
            Vehicle existingVehicle = vehicleDao.findById(id);
            if (existingVehicle == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print(createErrorResponse("Vehicle not found"));
                return;
            }
            
            JsonObject body = parseRequestBody(request);
            
            // Update fields if provided
            if (body.has("plateNumber")) existingVehicle.setPlateNumber(body.get("plateNumber").getAsString());
            if (body.has("brand")) existingVehicle.setBrand(body.get("brand").getAsString());
            if (body.has("model")) existingVehicle.setModel(body.get("model").getAsString());
            if (body.has("category")) existingVehicle.setCategory(body.get("category").getAsString());
            if (body.has("color")) existingVehicle.setColor(body.get("color").getAsString());
            if (body.has("seats")) existingVehicle.setSeats(body.get("seats").getAsInt());
            if (body.has("transmission")) existingVehicle.setTransmission(body.get("transmission").getAsString());
            if (body.has("fuelType")) existingVehicle.setFuelType(body.get("fuelType").getAsString());
            if (body.has("dailyPrice")) existingVehicle.setDailyPrice(getBigDecimalOrDefault(body, "dailyPrice", existingVehicle.getDailyPrice()));
            if (body.has("deposit")) existingVehicle.setDeposit(getBigDecimalOrDefault(body, "deposit", existingVehicle.getDeposit()));
            if (body.has("depositFree")) existingVehicle.setDepositFree(body.get("depositFree").getAsBoolean());
            if (body.has("status")) existingVehicle.setStatus(body.get("status").getAsString());
            if (body.has("imageUrl")) existingVehicle.setImageUrl(body.get("imageUrl").getAsString());
            if (body.has("description")) existingVehicle.setDescription(body.get("description").getAsString());
            if (body.has("mileage")) existingVehicle.setMileage(body.get("mileage").getAsInt());
            
            boolean success = vehicleDao.update(existingVehicle);
            
            if (success) {
                JsonObject result = new JsonObject();
                result.addProperty("success", true);
                result.addProperty("message", "Vehicle updated successfully");
                result.add("vehicle", gson.toJsonTree(existingVehicle));
                out.print(result);
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print(createErrorResponse("Failed to update vehicle"));
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(createErrorResponse("Invalid vehicle ID"));
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
            out.print(createErrorResponse("Vehicle ID is required"));
            return;
        }
        
        try {
            String idStr = pathInfo.substring(1);
            Long id = Long.parseLong(idStr);
            
            boolean success = vehicleDao.delete(id);
            
            if (success) {
                JsonObject result = new JsonObject();
                result.addProperty("success", true);
                result.addProperty("message", "Vehicle deleted successfully");
                out.print(result);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.print(createErrorResponse("Vehicle not found"));
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(createErrorResponse("Invalid vehicle ID"));
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
        return gson.fromJson(sb.toString(), JsonObject.class);
    }
    
    private String getStringOrNull(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }
    
    private BigDecimal getBigDecimalOrDefault(JsonObject obj, String key, BigDecimal defaultValue) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return defaultValue;
        }
        try {
            return new BigDecimal(obj.get(key).getAsString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    private String createErrorResponse(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("success", false);
        error.addProperty("error", message);
        return error.toString();
    }
}
