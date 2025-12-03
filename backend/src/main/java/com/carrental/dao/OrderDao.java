package com.carrental.dao;

import com.carrental.model.Order;
import com.carrental.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Data Access Object for Order entity.
 */
public class OrderDao {
    
    /**
     * Create a new order.
     */
    public Long create(Order order) throws SQLException {
        String sql = "INSERT INTO orders (order_no, user_id, vehicle_id, store_id, pickup_time, return_time, " +
                     "pickup_method, return_method, daily_price, rental_days, total_price, deposit, " +
                     "insurance_fee, service_fee, discount_amount, coupon_id, status, payment_status, " +
                     "payment_method, remark, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            String orderNo = order.getOrderNo() != null ? order.getOrderNo() : generateOrderNo();
            
            stmt.setString(1, orderNo);
            stmt.setLong(2, order.getUserId());
            stmt.setLong(3, order.getVehicleId());
            stmt.setObject(4, order.getStoreId());
            stmt.setTimestamp(5, order.getPickupTime() != null ? Timestamp.valueOf(order.getPickupTime()) : null);
            stmt.setTimestamp(6, order.getReturnTime() != null ? Timestamp.valueOf(order.getReturnTime()) : null);
            stmt.setString(7, order.getPickupMethod() != null ? order.getPickupMethod() : "STORE");
            stmt.setString(8, order.getReturnMethod() != null ? order.getReturnMethod() : "STORE");
            stmt.setBigDecimal(9, order.getDailyPrice());
            stmt.setInt(10, order.getRentalDays() != null ? order.getRentalDays() : 1);
            stmt.setBigDecimal(11, order.getTotalPrice());
            stmt.setBigDecimal(12, order.getDeposit());
            stmt.setBigDecimal(13, order.getInsuranceFee());
            stmt.setBigDecimal(14, order.getServiceFee());
            stmt.setBigDecimal(15, order.getDiscountAmount());
            stmt.setString(16, order.getCouponId());
            stmt.setString(17, order.getStatus() != null ? order.getStatus() : "PENDING");
            stmt.setString(18, order.getPaymentStatus() != null ? order.getPaymentStatus() : "UNPAID");
            stmt.setString(19, order.getPaymentMethod());
            stmt.setString(20, order.getRemark());
            
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
     * Find order by ID.
     */
    public Order findById(Long id) throws SQLException {
        String sql = "SELECT * FROM orders WHERE id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToOrder(rs);
                }
            }
        }
        return null;
    }
    
    /**
     * Find order by order number.
     */
    public Order findByOrderNo(String orderNo) throws SQLException {
        String sql = "SELECT * FROM orders WHERE order_no = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, orderNo);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToOrder(rs);
                }
            }
        }
        return null;
    }
    
    /**
     * Find orders by user ID.
     */
    public List<Order> findByUserId(Long userId) throws SQLException {
        String sql = "SELECT * FROM orders WHERE user_id = ? ORDER BY created_at DESC";
        List<Order> orders = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapResultSetToOrder(rs));
                }
            }
        }
        return orders;
    }
    
    /**
     * Find orders by status.
     */
    public List<Order> findByStatus(String status) throws SQLException {
        String sql = "SELECT * FROM orders WHERE status = ? ORDER BY created_at DESC";
        List<Order> orders = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapResultSetToOrder(rs));
                }
            }
        }
        return orders;
    }
    
    /**
     * Find all orders.
     */
    public List<Order> findAll() throws SQLException {
        String sql = "SELECT * FROM orders ORDER BY created_at DESC";
        List<Order> orders = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                orders.add(mapResultSetToOrder(rs));
            }
        }
        return orders;
    }
    
    /**
     * Update order status.
     */
    public boolean updateStatus(Long id, String status) throws SQLException {
        String sql = "UPDATE orders SET status = ?, updated_at = NOW() WHERE id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status);
            stmt.setLong(2, id);
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Update payment status.
     */
    public boolean updatePaymentStatus(Long id, String paymentStatus, String paymentMethod) throws SQLException {
        String sql = "UPDATE orders SET payment_status = ?, payment_method = ?, updated_at = NOW() WHERE id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, paymentStatus);
            stmt.setString(2, paymentMethod);
            stmt.setLong(3, id);
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Update actual pickup time.
     */
    public boolean updateActualPickupTime(Long id) throws SQLException {
        String sql = "UPDATE orders SET actual_pickup_time = NOW(), status = 'PICKED_UP', updated_at = NOW() WHERE id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Update actual return time.
     */
    public boolean updateActualReturnTime(Long id) throws SQLException {
        String sql = "UPDATE orders SET actual_return_time = NOW(), status = 'RETURNED', updated_at = NOW() WHERE id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Delete order.
     */
    public boolean delete(Long id) throws SQLException {
        String sql = "DELETE FROM orders WHERE id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Generate unique order number.
     */
    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
    
    private Order mapResultSetToOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setId(rs.getLong("id"));
        order.setOrderNo(rs.getString("order_no"));
        order.setUserId(rs.getLong("user_id"));
        order.setVehicleId(rs.getLong("vehicle_id"));
        order.setStoreId(rs.getLong("store_id"));
        
        Timestamp pickupTime = rs.getTimestamp("pickup_time");
        if (pickupTime != null) {
            order.setPickupTime(pickupTime.toLocalDateTime());
        }
        Timestamp returnTime = rs.getTimestamp("return_time");
        if (returnTime != null) {
            order.setReturnTime(returnTime.toLocalDateTime());
        }
        Timestamp actualPickupTime = rs.getTimestamp("actual_pickup_time");
        if (actualPickupTime != null) {
            order.setActualPickupTime(actualPickupTime.toLocalDateTime());
        }
        Timestamp actualReturnTime = rs.getTimestamp("actual_return_time");
        if (actualReturnTime != null) {
            order.setActualReturnTime(actualReturnTime.toLocalDateTime());
        }
        
        order.setPickupMethod(rs.getString("pickup_method"));
        order.setReturnMethod(rs.getString("return_method"));
        order.setDailyPrice(rs.getBigDecimal("daily_price"));
        order.setRentalDays(rs.getInt("rental_days"));
        order.setTotalPrice(rs.getBigDecimal("total_price"));
        order.setDeposit(rs.getBigDecimal("deposit"));
        order.setInsuranceFee(rs.getBigDecimal("insurance_fee"));
        order.setServiceFee(rs.getBigDecimal("service_fee"));
        order.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        order.setCouponId(rs.getString("coupon_id"));
        order.setStatus(rs.getString("status"));
        order.setPaymentStatus(rs.getString("payment_status"));
        order.setPaymentMethod(rs.getString("payment_method"));
        order.setRemark(rs.getString("remark"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            order.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            order.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return order;
    }
}
