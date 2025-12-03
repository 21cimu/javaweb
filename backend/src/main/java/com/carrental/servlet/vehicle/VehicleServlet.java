package com.carrental.servlet.vehicle;

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
import java.util.*;

/**
 * Vehicle Servlet for vehicle browsing and searching
 */
@WebServlet(name = "VehicleServlet", urlPatterns = {"/api/vehicles/*"})
public class VehicleServlet extends HttpServlet {

    private final VehicleDao vehicleDao = new VehicleDao();
    private final StoreDao storeDao = new StoreDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        if (pathInfo == null || "/".equals(pathInfo) || "/list".equals(pathInfo)) {
            handleList(request, response);
        } else if ("/search".equals(pathInfo)) {
            handleSearch(request, response);
        } else if ("/categories".equals(pathInfo)) {
            handleCategories(request, response);
        } else if ("/brands".equals(pathInfo)) {
            handleBrands(request, response);
        } else if ("/hot".equals(pathInfo)) {
            handleHot(request, response);
        } else if ("/new".equals(pathInfo)) {
            handleNew(request, response);
        } else if (pathInfo.startsWith("/detail/")) {
            handleDetail(request, response, pathInfo.substring(8));
        } else {
            JsonUtil.writeError(response, 404, "接口不存在");
        }
    }

    private void handleList(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // Parse query parameters
        String category = request.getParameter("category");
        String brand = request.getParameter("brand");
        String storeIdStr = request.getParameter("storeId");
        String fuelType = request.getParameter("fuelType");
        String minSeatsStr = request.getParameter("minSeats");
        String minPriceStr = request.getParameter("minPrice");
        String maxPriceStr = request.getParameter("maxPrice");
        String sortBy = request.getParameter("sortBy");
        String pageStr = request.getParameter("page");
        String pageSizeStr = request.getParameter("pageSize");

        Long storeId = storeIdStr != null ? Long.parseLong(storeIdStr) : null;
        Integer minSeats = minSeatsStr != null ? Integer.parseInt(minSeatsStr) : null;
        BigDecimal minPrice = minPriceStr != null ? new BigDecimal(minPriceStr) : null;
        BigDecimal maxPrice = maxPriceStr != null ? new BigDecimal(maxPriceStr) : null;
        int page = pageStr != null ? Integer.parseInt(pageStr) : 1;
        int pageSize = pageSizeStr != null ? Integer.parseInt(pageSizeStr) : 10;

        List<Vehicle> vehicles = vehicleDao.findAvailable(category, brand, storeId,
            fuelType, minSeats, minPrice, maxPrice, sortBy, page, pageSize);
        long total = vehicleDao.countAvailable(category, brand, storeId,
            fuelType, minSeats, minPrice, maxPrice);

        JsonUtil.writePaginated(response, vehicles, page, pageSize, total);
    }

    private void handleSearch(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String keyword = request.getParameter("keyword");
        String pageStr = request.getParameter("page");
        String pageSizeStr = request.getParameter("pageSize");

        if (keyword == null || keyword.isEmpty()) {
            JsonUtil.writeError(response, 400, "请输入搜索关键词");
            return;
        }

        int page = pageStr != null ? Integer.parseInt(pageStr) : 1;
        int pageSize = pageSizeStr != null ? Integer.parseInt(pageSizeStr) : 10;

        List<Vehicle> vehicles = vehicleDao.search(keyword, page, pageSize);
        JsonUtil.writeSuccess(response, vehicles);
    }

    private void handleCategories(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        List<Map<String, Object>> categories = new ArrayList<>();
        
        // Vehicle categories
        String[] cats = {"economy", "compact", "midsize", "suv", "luxury", "minivan"};
        String[] names = {"经济型", "紧凑型", "中型", "SUV", "豪华型", "MPV"};
        String[] icons = {"🚗", "🚙", "🚐", "🚎", "🏎️", "🚌"};
        
        for (int i = 0; i < cats.length; i++) {
            Map<String, Object> cat = new HashMap<>();
            cat.put("code", cats[i]);
            cat.put("name", names[i]);
            cat.put("icon", icons[i]);
            cat.put("count", vehicleDao.countAvailable(cats[i], null, null, null, null, null, null));
            categories.add(cat);
        }

        JsonUtil.writeSuccess(response, categories);
    }

    private void handleBrands(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        List<String> brands = vehicleDao.findAllBrands();
        JsonUtil.writeSuccess(response, brands);
    }

    private void handleHot(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String limitStr = request.getParameter("limit");
        int limit = limitStr != null ? Integer.parseInt(limitStr) : 6;
        
        List<Vehicle> vehicles = vehicleDao.findHotVehicles(limit);
        JsonUtil.writeSuccess(response, vehicles);
    }

    private void handleNew(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String limitStr = request.getParameter("limit");
        int limit = limitStr != null ? Integer.parseInt(limitStr) : 6;
        
        List<Vehicle> vehicles = vehicleDao.findNewVehicles(limit);
        JsonUtil.writeSuccess(response, vehicles);
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

            // Increment view count
            vehicleDao.incrementViewCount(id);

            // Get store info
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
}
