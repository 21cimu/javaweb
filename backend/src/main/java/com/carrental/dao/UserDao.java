package com.carrental.dao;

import com.carrental.model.User;
import com.carrental.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for User entity.
 */
public class UserDao {
    
    /**
     * Create a new user.
     */
    public Long create(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password, phone, email, real_name, id_card, driving_license, " +
                     "member_level, points, balance, status, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getPhone());
            stmt.setString(4, user.getEmail());
            stmt.setString(5, user.getRealName());
            stmt.setString(6, user.getIdCard());
            stmt.setString(7, user.getDrivingLicense());
            stmt.setString(8, user.getMemberLevel() != null ? user.getMemberLevel() : "NORMAL");
            stmt.setInt(9, user.getPoints() != null ? user.getPoints() : 0);
            stmt.setBigDecimal(10, user.getBalance() != null ? user.getBalance() : new java.math.BigDecimal("0"));
            stmt.setString(11, user.getStatus() != null ? user.getStatus() : "ACTIVE");
            
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
     * Find user by ID.
     */
    public User findById(Long id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }
    
    /**
     * Find user by username.
     */
    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }
    
    /**
     * Find user by phone number.
     */
    public User findByPhone(String phone) throws SQLException {
        String sql = "SELECT * FROM users WHERE phone = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, phone);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }
    
    /**
     * Find all users.
     */
    public List<User> findAll() throws SQLException {
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        List<User> users = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        return users;
    }
    
    /**
     * Update user.
     */
    public boolean update(User user) throws SQLException {
        String sql = "UPDATE users SET username=?, phone=?, email=?, real_name=?, id_card=?, " +
                     "driving_license=?, member_level=?, points=?, balance=?, status=?, updated_at=NOW() WHERE id=?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPhone());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getRealName());
            stmt.setString(5, user.getIdCard());
            stmt.setString(6, user.getDrivingLicense());
            stmt.setString(7, user.getMemberLevel());
            stmt.setInt(8, user.getPoints());
            stmt.setBigDecimal(9, user.getBalance());
            stmt.setString(10, user.getStatus());
            stmt.setLong(11, user.getId());
            
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Delete user.
     */
    public boolean delete(Long id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Authenticate user by username and password.
     */
    public User authenticate(String username, String password) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ? AND status = 'ACTIVE'";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            stmt.setString(2, password);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }
    
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setPhone(rs.getString("phone"));
        user.setEmail(rs.getString("email"));
        user.setRealName(rs.getString("real_name"));
        user.setIdCard(rs.getString("id_card"));
        user.setDrivingLicense(rs.getString("driving_license"));
        user.setMemberLevel(rs.getString("member_level"));
        user.setPoints(rs.getInt("points"));
        user.setBalance(rs.getBigDecimal("balance"));
        user.setStatus(rs.getString("status"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return user;
    }
}
