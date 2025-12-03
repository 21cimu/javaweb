package com.carrental.dao;

import com.carrental.model.Store;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Store Data Access Object
 */
public class StoreDao extends BaseDao<Store> {

    @Override
    protected Store mapRow(ResultSet rs) throws SQLException {
        Store store = new Store();
        store.setId(getLong(rs, "id"));
        store.setName(getString(rs, "name"));
        store.setAddress(getString(rs, "address"));
        store.setCity(getString(rs, "city"));
        store.setDistrict(getString(rs, "district"));
        store.setLatitude(rs.getBigDecimal("latitude"));
        store.setLongitude(rs.getBigDecimal("longitude"));
        store.setPhone(getString(rs, "phone"));
        store.setBusinessHours(getString(rs, "business_hours"));
        store.setImage(getString(rs, "image"));
        store.setStatus(getInt(rs, "status"));
        store.setVehicleCount(getInt(rs, "vehicle_count"));
        store.setAvailableCount(getInt(rs, "available_count"));
        store.setCreatedAt(getLocalDateTime(rs, "created_at"));
        store.setUpdatedAt(getLocalDateTime(rs, "updated_at"));
        return store;
    }

    /**
     * Find store by ID
     */
    public Store findById(Long id) {
        String sql = "SELECT * FROM stores WHERE id = ?";
        return executeQuerySingle(sql, id);
    }

    /**
     * Create new store
     */
    public Long create(Store store) {
        String sql = """
            INSERT INTO stores (name, address, city, district, latitude, longitude,
                phone, business_hours, image, status, vehicle_count, available_count,
                created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
            """;
        return executeInsert(sql, store.getName(), store.getAddress(),
            store.getCity(), store.getDistrict(), store.getLatitude(),
            store.getLongitude(), store.getPhone(), store.getBusinessHours(),
            store.getImage(), store.getStatus(), store.getVehicleCount(),
            store.getAvailableCount());
    }

    /**
     * Update store
     */
    public int update(Store store) {
        String sql = """
            UPDATE stores SET 
                name = ?, address = ?, city = ?, district = ?,
                latitude = ?, longitude = ?, phone = ?, business_hours = ?,
                image = ?, status = ?, updated_at = NOW()
            WHERE id = ?
            """;
        return executeUpdate(sql, store.getName(), store.getAddress(),
            store.getCity(), store.getDistrict(), store.getLatitude(),
            store.getLongitude(), store.getPhone(), store.getBusinessHours(),
            store.getImage(), store.getStatus(), store.getId());
    }

    /**
     * Update vehicle counts
     */
    public int updateVehicleCounts(Long id, Integer vehicleCount, Integer availableCount) {
        String sql = """
            UPDATE stores SET 
                vehicle_count = ?, available_count = ?, updated_at = NOW()
            WHERE id = ?
            """;
        return executeUpdate(sql, vehicleCount, availableCount, id);
    }

    /**
     * Find all active stores
     */
    public List<Store> findActive() {
        String sql = "SELECT * FROM stores WHERE status = 1 ORDER BY name ASC";
        return executeQuery(sql);
    }

    /**
     * Find stores by city
     */
    public List<Store> findByCity(String city) {
        String sql = "SELECT * FROM stores WHERE city = ? AND status = 1 ORDER BY name ASC";
        return executeQuery(sql, city);
    }

    /**
     * Find all stores (admin)
     */
    public List<Store> findAll(int page, int pageSize) {
        String sql = "SELECT * FROM stores ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return executeQuery(sql, pageSize, (page - 1) * pageSize);
    }

    /**
     * Count all stores
     */
    public long count() {
        String sql = "SELECT COUNT(*) FROM stores";
        return executeCount(sql);
    }

    /**
     * Get all cities with stores
     */
    public List<String> findAllCities() {
        String sql = "SELECT DISTINCT city FROM stores WHERE status = 1 ORDER BY city";
        List<Store> stores = executeQuery(sql);
        return stores.stream().map(Store::getCity).toList();
    }
}
