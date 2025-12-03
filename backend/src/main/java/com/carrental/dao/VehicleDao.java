package com.carrental.dao;

import com.carrental.model.Vehicle;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Vehicle Data Access Object
 */
public class VehicleDao extends BaseDao<Vehicle> {

    @Override
    protected Vehicle mapRow(ResultSet rs) throws SQLException {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(getLong(rs, "id"));
        vehicle.setVin(getString(rs, "vin"));
        vehicle.setPlateNumber(getString(rs, "plate_number"));
        vehicle.setBrand(getString(rs, "brand"));
        vehicle.setModel(getString(rs, "model"));
        vehicle.setSeries(getString(rs, "series"));
        vehicle.setYear(getInt(rs, "year"));
        vehicle.setColor(getString(rs, "color"));
        vehicle.setSeats(getInt(rs, "seats"));
        vehicle.setFuelType(getString(rs, "fuel_type"));
        vehicle.setTransmission(getString(rs, "transmission"));
        vehicle.setCategory(getString(rs, "category"));
        vehicle.setStoreId(getLong(rs, "store_id"));
        try {
            vehicle.setStoreName(getString(rs, "store_name"));
        } catch (SQLException ignored) {}
        vehicle.setDailyPrice(rs.getBigDecimal("daily_price"));
        vehicle.setWeeklyPrice(rs.getBigDecimal("weekly_price"));
        vehicle.setMonthlyPrice(rs.getBigDecimal("monthly_price"));
        vehicle.setDeposit(rs.getBigDecimal("deposit"));
        vehicle.setMileage(getInt(rs, "mileage"));
        vehicle.setStatus(getInt(rs, "status"));
        vehicle.setMainImage(getString(rs, "main_image"));
        vehicle.setImages(getString(rs, "images"));
        vehicle.setFeatures(getString(rs, "features"));
        vehicle.setDescription(getString(rs, "description"));
        vehicle.setPurchaseDate(getLocalDate(rs, "purchase_date"));
        vehicle.setPurchasePrice(rs.getBigDecimal("purchase_price"));
        vehicle.setInsuranceExpiry(getLocalDate(rs, "insurance_expiry"));
        vehicle.setInspectionExpiry(getLocalDate(rs, "inspection_expiry"));
        vehicle.setRegistrationExpiry(getLocalDate(rs, "registration_expiry"));
        vehicle.setViewCount(getInt(rs, "view_count"));
        vehicle.setOrderCount(getInt(rs, "order_count"));
        vehicle.setRating(rs.getBigDecimal("rating"));
        vehicle.setIsHot(getBoolean(rs, "is_hot"));
        vehicle.setIsNew(getBoolean(rs, "is_new"));
        vehicle.setNoDeposit(getBoolean(rs, "no_deposit"));
        vehicle.setCreatedAt(getLocalDateTime(rs, "created_at"));
        vehicle.setUpdatedAt(getLocalDateTime(rs, "updated_at"));
        return vehicle;
    }

    /**
     * Find vehicle by ID
     */
    public Vehicle findById(Long id) {
        String sql = """
            SELECT v.*, s.name as store_name 
            FROM vehicles v 
            LEFT JOIN stores s ON v.store_id = s.id 
            WHERE v.id = ?
            """;
        return executeQuerySingle(sql, id);
    }

    /**
     * Find vehicle by plate number
     */
    public Vehicle findByPlateNumber(String plateNumber) {
        String sql = "SELECT * FROM vehicles WHERE plate_number = ?";
        return executeQuerySingle(sql, plateNumber);
    }

    /**
     * Create new vehicle
     */
    public Long create(Vehicle vehicle) {
        String sql = """
            INSERT INTO vehicles (vin, plate_number, brand, model, series, year, color,
                seats, fuel_type, transmission, category, store_id, daily_price, 
                weekly_price, monthly_price, deposit, mileage, status, main_image, 
                images, features, description, purchase_date, purchase_price,
                insurance_expiry, inspection_expiry, registration_expiry,
                view_count, order_count, rating, is_hot, is_new, no_deposit,
                created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 
                ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
            """;
        return executeInsert(sql, vehicle.getVin(), vehicle.getPlateNumber(),
            vehicle.getBrand(), vehicle.getModel(), vehicle.getSeries(),
            vehicle.getYear(), vehicle.getColor(), vehicle.getSeats(),
            vehicle.getFuelType(), vehicle.getTransmission(), vehicle.getCategory(),
            vehicle.getStoreId(), vehicle.getDailyPrice(), vehicle.getWeeklyPrice(),
            vehicle.getMonthlyPrice(), vehicle.getDeposit(), vehicle.getMileage(),
            vehicle.getStatus(), vehicle.getMainImage(), vehicle.getImages(),
            vehicle.getFeatures(), vehicle.getDescription(), vehicle.getPurchaseDate(),
            vehicle.getPurchasePrice(), vehicle.getInsuranceExpiry(),
            vehicle.getInspectionExpiry(), vehicle.getRegistrationExpiry(),
            vehicle.getViewCount(), vehicle.getOrderCount(), vehicle.getRating(),
            vehicle.getIsHot(), vehicle.getIsNew(), vehicle.getNoDeposit());
    }

    /**
     * Update vehicle
     */
    public int update(Vehicle vehicle) {
        String sql = """
            UPDATE vehicles SET 
                vin = ?, plate_number = ?, brand = ?, model = ?, series = ?,
                year = ?, color = ?, seats = ?, fuel_type = ?, transmission = ?,
                category = ?, store_id = ?, daily_price = ?, weekly_price = ?,
                monthly_price = ?, deposit = ?, mileage = ?, status = ?,
                main_image = ?, images = ?, features = ?, description = ?,
                is_hot = ?, is_new = ?, no_deposit = ?, updated_at = NOW()
            WHERE id = ?
            """;
        return executeUpdate(sql, vehicle.getVin(), vehicle.getPlateNumber(),
            vehicle.getBrand(), vehicle.getModel(), vehicle.getSeries(),
            vehicle.getYear(), vehicle.getColor(), vehicle.getSeats(),
            vehicle.getFuelType(), vehicle.getTransmission(), vehicle.getCategory(),
            vehicle.getStoreId(), vehicle.getDailyPrice(), vehicle.getWeeklyPrice(),
            vehicle.getMonthlyPrice(), vehicle.getDeposit(), vehicle.getMileage(),
            vehicle.getStatus(), vehicle.getMainImage(), vehicle.getImages(),
            vehicle.getFeatures(), vehicle.getDescription(),
            vehicle.getIsHot(), vehicle.getIsNew(), vehicle.getNoDeposit(),
            vehicle.getId());
    }

    /**
     * Update vehicle status
     */
    public int updateStatus(Long id, Integer status) {
        String sql = "UPDATE vehicles SET status = ?, updated_at = NOW() WHERE id = ?";
        return executeUpdate(sql, status, id);
    }

    /**
     * Increment view count
     */
    public int incrementViewCount(Long id) {
        String sql = "UPDATE vehicles SET view_count = view_count + 1 WHERE id = ?";
        return executeUpdate(sql, id);
    }

    /**
     * Increment order count
     */
    public int incrementOrderCount(Long id) {
        String sql = "UPDATE vehicles SET order_count = order_count + 1 WHERE id = ?";
        return executeUpdate(sql, id);
    }

    /**
     * List available vehicles with filters
     */
    public List<Vehicle> findAvailable(String category, String brand, Long storeId,
            String fuelType, Integer minSeats, java.math.BigDecimal minPrice, 
            java.math.BigDecimal maxPrice, String sortBy, int page, int pageSize) {
        StringBuilder sql = new StringBuilder("""
            SELECT v.*, s.name as store_name 
            FROM vehicles v 
            LEFT JOIN stores s ON v.store_id = s.id 
            WHERE v.status = 1
            """);
        List<Object> params = new ArrayList<>();
        
        if (category != null && !category.isEmpty()) {
            sql.append(" AND v.category = ?");
            params.add(category);
        }
        if (brand != null && !brand.isEmpty()) {
            sql.append(" AND v.brand = ?");
            params.add(brand);
        }
        if (storeId != null) {
            sql.append(" AND v.store_id = ?");
            params.add(storeId);
        }
        if (fuelType != null && !fuelType.isEmpty()) {
            sql.append(" AND v.fuel_type = ?");
            params.add(fuelType);
        }
        if (minSeats != null) {
            sql.append(" AND v.seats >= ?");
            params.add(minSeats);
        }
        if (minPrice != null) {
            sql.append(" AND v.daily_price >= ?");
            params.add(minPrice);
        }
        if (maxPrice != null) {
            sql.append(" AND v.daily_price <= ?");
            params.add(maxPrice);
        }

        // Sorting
        if ("price_asc".equals(sortBy)) {
            sql.append(" ORDER BY v.daily_price ASC");
        } else if ("price_desc".equals(sortBy)) {
            sql.append(" ORDER BY v.daily_price DESC");
        } else if ("rating".equals(sortBy)) {
            sql.append(" ORDER BY v.rating DESC");
        } else if ("popular".equals(sortBy)) {
            sql.append(" ORDER BY v.order_count DESC");
        } else {
            sql.append(" ORDER BY v.is_hot DESC, v.created_at DESC");
        }

        sql.append(" LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add((page - 1) * pageSize);

        return executeQuery(sql.toString(), params.toArray());
    }

    /**
     * Count available vehicles with filters
     */
    public long countAvailable(String category, String brand, Long storeId,
            String fuelType, Integer minSeats, java.math.BigDecimal minPrice, 
            java.math.BigDecimal maxPrice) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM vehicles v WHERE v.status = 1");
        List<Object> params = new ArrayList<>();
        
        if (category != null && !category.isEmpty()) {
            sql.append(" AND v.category = ?");
            params.add(category);
        }
        if (brand != null && !brand.isEmpty()) {
            sql.append(" AND v.brand = ?");
            params.add(brand);
        }
        if (storeId != null) {
            sql.append(" AND v.store_id = ?");
            params.add(storeId);
        }
        if (fuelType != null && !fuelType.isEmpty()) {
            sql.append(" AND v.fuel_type = ?");
            params.add(fuelType);
        }
        if (minSeats != null) {
            sql.append(" AND v.seats >= ?");
            params.add(minSeats);
        }
        if (minPrice != null) {
            sql.append(" AND v.daily_price >= ?");
            params.add(minPrice);
        }
        if (maxPrice != null) {
            sql.append(" AND v.daily_price <= ?");
            params.add(maxPrice);
        }

        return executeCount(sql.toString(), params.toArray());
    }

    /**
     * Search vehicles
     */
    public List<Vehicle> search(String keyword, int page, int pageSize) {
        String sql = """
            SELECT v.*, s.name as store_name 
            FROM vehicles v 
            LEFT JOIN stores s ON v.store_id = s.id 
            WHERE v.status = 1 
            AND (v.brand LIKE ? OR v.model LIKE ? OR v.series LIKE ? OR v.plate_number LIKE ?)
            ORDER BY v.is_hot DESC, v.order_count DESC
            LIMIT ? OFFSET ?
            """;
        String pattern = "%" + keyword + "%";
        return executeQuery(sql, pattern, pattern, pattern, pattern, pageSize, (page - 1) * pageSize);
    }

    /**
     * Get all brands
     */
    public List<String> findAllBrands() {
        String sql = "SELECT DISTINCT brand FROM vehicles WHERE status = 1 ORDER BY brand";
        List<String> brands = new ArrayList<>();
        List<Vehicle> vehicles = executeQuery(sql);
        for (Vehicle v : vehicles) {
            brands.add(v.getBrand());
        }
        return brands;
    }

    /**
     * Get hot vehicles
     */
    public List<Vehicle> findHotVehicles(int limit) {
        String sql = """
            SELECT v.*, s.name as store_name 
            FROM vehicles v 
            LEFT JOIN stores s ON v.store_id = s.id 
            WHERE v.status = 1 AND v.is_hot = 1
            ORDER BY v.order_count DESC
            LIMIT ?
            """;
        return executeQuery(sql, limit);
    }

    /**
     * Get new vehicles
     */
    public List<Vehicle> findNewVehicles(int limit) {
        String sql = """
            SELECT v.*, s.name as store_name 
            FROM vehicles v 
            LEFT JOIN stores s ON v.store_id = s.id 
            WHERE v.status = 1 AND v.is_new = 1
            ORDER BY v.created_at DESC
            LIMIT ?
            """;
        return executeQuery(sql, limit);
    }

    /**
     * List all vehicles (admin)
     */
    public List<Vehicle> findAll(int page, int pageSize) {
        String sql = """
            SELECT v.*, s.name as store_name 
            FROM vehicles v 
            LEFT JOIN stores s ON v.store_id = s.id 
            ORDER BY v.created_at DESC
            LIMIT ? OFFSET ?
            """;
        return executeQuery(sql, pageSize, (page - 1) * pageSize);
    }

    /**
     * Count all vehicles
     */
    public long count() {
        String sql = "SELECT COUNT(*) FROM vehicles";
        return executeCount(sql);
    }

    /**
     * Count vehicles by status
     */
    public long countByStatus(Integer status) {
        String sql = "SELECT COUNT(*) FROM vehicles WHERE status = ?";
        return executeCount(sql, status);
    }

    /**
     * Find vehicles by store
     */
    public List<Vehicle> findByStore(Long storeId, int page, int pageSize) {
        String sql = """
            SELECT v.*, s.name as store_name 
            FROM vehicles v 
            LEFT JOIN stores s ON v.store_id = s.id 
            WHERE v.store_id = ?
            ORDER BY v.created_at DESC
            LIMIT ? OFFSET ?
            """;
        return executeQuery(sql, storeId, pageSize, (page - 1) * pageSize);
    }
}
