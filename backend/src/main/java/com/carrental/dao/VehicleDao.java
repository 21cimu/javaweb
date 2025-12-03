package com.carrental.dao;

import com.carrental.model.Vehicle;
import com.carrental.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Vehicle entity.
 */
public class VehicleDao {
    
    /**
     * Create a new vehicle.
     */
    public Long create(Vehicle vehicle) throws SQLException {
        String sql = "INSERT INTO vehicles (plate_number, vin, brand, model, category, color, seats, " +
                     "transmission, fuel_type, daily_price, deposit, deposit_free, status, store_id, " +
                     "image_url, description, mileage, purchase_date, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, vehicle.getPlateNumber());
            stmt.setString(2, vehicle.getVin());
            stmt.setString(3, vehicle.getBrand());
            stmt.setString(4, vehicle.getModel());
            stmt.setString(5, vehicle.getCategory());
            stmt.setString(6, vehicle.getColor());
            stmt.setInt(7, vehicle.getSeats() != null ? vehicle.getSeats() : 5);
            stmt.setString(8, vehicle.getTransmission());
            stmt.setString(9, vehicle.getFuelType());
            stmt.setBigDecimal(10, vehicle.getDailyPrice());
            stmt.setBigDecimal(11, vehicle.getDeposit());
            stmt.setBoolean(12, vehicle.getDepositFree() != null ? vehicle.getDepositFree() : false);
            stmt.setString(13, vehicle.getStatus() != null ? vehicle.getStatus() : "AVAILABLE");
            stmt.setObject(14, vehicle.getStoreId());
            stmt.setString(15, vehicle.getImageUrl());
            stmt.setString(16, vehicle.getDescription());
            stmt.setInt(17, vehicle.getMileage() != null ? vehicle.getMileage() : 0);
            stmt.setObject(18, vehicle.getPurchaseDate() != null ? Timestamp.valueOf(vehicle.getPurchaseDate()) : null);
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return null;
    }
    
    /**
     * Find vehicle by ID.
     */
    public Vehicle findById(Long id) throws SQLException {
        String sql = "SELECT * FROM vehicles WHERE id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToVehicle(rs);
                }
            }
        }
        return null;
    }
    
    /**
     * Find all vehicles.
     */
    public List<Vehicle> findAll() throws SQLException {
        String sql = "SELECT * FROM vehicles ORDER BY created_at DESC";
        List<Vehicle> vehicles = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                vehicles.add(mapResultSetToVehicle(rs));
            }
        }
        return vehicles;
    }
    
    /**
     * Find available vehicles.
     */
    public List<Vehicle> findAvailable() throws SQLException {
        String sql = "SELECT * FROM vehicles WHERE status = 'AVAILABLE' ORDER BY daily_price ASC";
        List<Vehicle> vehicles = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                vehicles.add(mapResultSetToVehicle(rs));
            }
        }
        return vehicles;
    }
    
    /**
     * Find vehicles by category.
     */
    public List<Vehicle> findByCategory(String category) throws SQLException {
        String sql = "SELECT * FROM vehicles WHERE category = ? AND status = 'AVAILABLE' ORDER BY daily_price ASC";
        List<Vehicle> vehicles = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, category);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    vehicles.add(mapResultSetToVehicle(rs));
                }
            }
        }
        return vehicles;
    }
    
    /**
     * Search vehicles by keyword (brand, model, category).
     */
    public List<Vehicle> search(String keyword) throws SQLException {
        String sql = "SELECT * FROM vehicles WHERE (brand LIKE ? OR model LIKE ? OR category LIKE ?) " +
                     "AND status = 'AVAILABLE' ORDER BY daily_price ASC";
        List<Vehicle> vehicles = new ArrayList<>();
        String pattern = "%" + keyword + "%";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            stmt.setString(3, pattern);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    vehicles.add(mapResultSetToVehicle(rs));
                }
            }
        }
        return vehicles;
    }
    
    /**
     * Update vehicle.
     */
    public boolean update(Vehicle vehicle) throws SQLException {
        String sql = "UPDATE vehicles SET plate_number=?, vin=?, brand=?, model=?, category=?, " +
                     "color=?, seats=?, transmission=?, fuel_type=?, daily_price=?, deposit=?, " +
                     "deposit_free=?, status=?, store_id=?, image_url=?, description=?, mileage=?, " +
                     "updated_at=NOW() WHERE id=?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, vehicle.getPlateNumber());
            stmt.setString(2, vehicle.getVin());
            stmt.setString(3, vehicle.getBrand());
            stmt.setString(4, vehicle.getModel());
            stmt.setString(5, vehicle.getCategory());
            stmt.setString(6, vehicle.getColor());
            stmt.setInt(7, vehicle.getSeats());
            stmt.setString(8, vehicle.getTransmission());
            stmt.setString(9, vehicle.getFuelType());
            stmt.setBigDecimal(10, vehicle.getDailyPrice());
            stmt.setBigDecimal(11, vehicle.getDeposit());
            stmt.setBoolean(12, vehicle.getDepositFree());
            stmt.setString(13, vehicle.getStatus());
            stmt.setObject(14, vehicle.getStoreId());
            stmt.setString(15, vehicle.getImageUrl());
            stmt.setString(16, vehicle.getDescription());
            stmt.setInt(17, vehicle.getMileage());
            stmt.setLong(18, vehicle.getId());
            
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Update vehicle status.
     */
    public boolean updateStatus(Long id, String status) throws SQLException {
        String sql = "UPDATE vehicles SET status = ?, updated_at = NOW() WHERE id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status);
            stmt.setLong(2, id);
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Delete vehicle.
     */
    public boolean delete(Long id) throws SQLException {
        String sql = "DELETE FROM vehicles WHERE id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        }
    }
    
    private Vehicle mapResultSetToVehicle(ResultSet rs) throws SQLException {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(rs.getLong("id"));
        vehicle.setPlateNumber(rs.getString("plate_number"));
        vehicle.setVin(rs.getString("vin"));
        vehicle.setBrand(rs.getString("brand"));
        vehicle.setModel(rs.getString("model"));
        vehicle.setCategory(rs.getString("category"));
        vehicle.setColor(rs.getString("color"));
        vehicle.setSeats(rs.getInt("seats"));
        vehicle.setTransmission(rs.getString("transmission"));
        vehicle.setFuelType(rs.getString("fuel_type"));
        vehicle.setDailyPrice(rs.getBigDecimal("daily_price"));
        vehicle.setDeposit(rs.getBigDecimal("deposit"));
        vehicle.setDepositFree(rs.getBoolean("deposit_free"));
        vehicle.setStatus(rs.getString("status"));
        vehicle.setStoreId(rs.getLong("store_id"));
        vehicle.setImageUrl(rs.getString("image_url"));
        vehicle.setDescription(rs.getString("description"));
        vehicle.setMileage(rs.getInt("mileage"));
        Timestamp purchaseDate = rs.getTimestamp("purchase_date");
        if (purchaseDate != null) {
            vehicle.setPurchaseDate(purchaseDate.toLocalDateTime());
        }
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            vehicle.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            vehicle.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return vehicle;
    }
}
