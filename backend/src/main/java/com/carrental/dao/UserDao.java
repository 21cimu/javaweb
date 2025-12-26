package com.carrental.dao;

import com.carrental.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * User Data Access Object
 */
public class UserDao extends BaseDao<User> {

    @Override
    protected User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(getLong(rs, "id"));
        user.setUsername(getString(rs, "username"));
        user.setPassword(getString(rs, "password"));
        user.setEmail(getString(rs, "email"));
        user.setPhone(getString(rs, "phone"));
        user.setRealName(getString(rs, "real_name"));
        user.setIdCard(getString(rs, "id_card"));
        user.setDriverLicense(getString(rs, "driver_license"));
        user.setDriverLicenseImage(getString(rs, "driver_license_image"));
        user.setIdCardFront(getString(rs, "id_card_front"));
        user.setIdCardBack(getString(rs, "id_card_back"));
        user.setVerificationStatus(getInt(rs, "verification_status"));
        user.setAvatar(getString(rs, "avatar"));
        user.setGender(getInt(rs, "gender"));
        user.setBirthday(getLocalDateTime(rs, "birthday"));
        user.setRole(getString(rs, "role"));
        user.setStatus(getInt(rs, "status"));
        user.setBalance(rs.getBigDecimal("balance"));
        user.setPoints(getInt(rs, "points"));
        user.setInviteCode(getString(rs, "invite_code"));
        user.setInviterId(getLong(rs, "inviter_id"));
        user.setCreatedAt(getLocalDateTime(rs, "created_at"));
        user.setUpdatedAt(getLocalDateTime(rs, "updated_at"));
        // VIP fields
        user.setCumulativeSpending(rs.getBigDecimal("cumulative_spending"));
        user.setVipLevel(getInt(rs, "vip_level"));
        return user;
    }

    /**
     * Find user by ID
     */
    public User findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        return executeQuerySingle(sql, id);
    }

    /**
     * Find user by username
     */
    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        return executeQuerySingle(sql, username);
    }

    /**
     * Find user by phone
     */
    public User findByPhone(String phone) {
        String sql = "SELECT * FROM users WHERE phone = ?";
        return executeQuerySingle(sql, phone);
    }

    /**
     * Find user by email
     */
    public User findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        return executeQuerySingle(sql, email);
    }

    /**
     * Find user by invite code
     */
    public User findByInviteCode(String inviteCode) {
        String sql = "SELECT * FROM users WHERE invite_code = ?";
        return executeQuerySingle(sql, inviteCode);
    }

    /**
     * Create new user
     */
    public Long create(User user) {
        String sql = """
            INSERT INTO users (username, password, email, phone, real_name, id_card, 
                driver_license, driver_license_image, id_card_front, id_card_back,
                verification_status, avatar, gender, birthday, role, status, 
                balance, points, invite_code, inviter_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
            """;
        return executeInsert(sql, 
            user.getUsername(), user.getPassword(), user.getEmail(), user.getPhone(),
            user.getRealName(), user.getIdCard(), user.getDriverLicense(), 
            user.getDriverLicenseImage(), user.getIdCardFront(), user.getIdCardBack(),
            user.getVerificationStatus(), user.getAvatar(), user.getGender(),
            user.getBirthday(), user.getRole(), user.getStatus(),
            user.getBalance(), user.getPoints(), user.getInviteCode(), user.getInviterId());
    }

    /**
     * Update user
     */
    public int update(User user) {
        String sql = """
            UPDATE users SET 
                email = ?, phone = ?, real_name = ?, avatar = ?, 
                gender = ?, birthday = ?, updated_at = NOW()
            WHERE id = ?
            """;
        return executeUpdate(sql, user.getEmail(), user.getPhone(), user.getRealName(),
            user.getAvatar(), user.getGender(), user.getBirthday(), user.getId());
    }

    /**
     * Update verification info
     */
    public int updateVerification(Long userId, String realName, String idCard, 
            String driverLicense, String driverLicenseImage, 
            String idCardFront, String idCardBack, Integer verificationStatus) {
        String sql = """
            UPDATE users SET 
                real_name = ?, id_card = ?, driver_license = ?, 
                driver_license_image = ?, id_card_front = ?, id_card_back = ?,
                verification_status = ?, updated_at = NOW()
            WHERE id = ?
            """;
        return executeUpdate(sql, realName, idCard, driverLicense, 
            driverLicenseImage, idCardFront, idCardBack, verificationStatus, userId);
    }

    /**
     * Update verification status
     */
    public int updateVerificationStatus(Long userId, Integer status) {
        String sql = "UPDATE users SET verification_status = ?, updated_at = NOW() WHERE id = ?";
        return executeUpdate(sql, status, userId);
    }

    /**
     * Update user status (enable/disable)
     */
    public int updateStatus(Long userId, Integer status) {
        String sql = "UPDATE users SET status = ?, updated_at = NOW() WHERE id = ?";
        return executeUpdate(sql, status, userId);
    }

    /**
     * Update user role
     */
    public int updateRole(Long userId, String role) {
        String sql = "UPDATE users SET role = ?, updated_at = NOW() WHERE id = ?";
        return executeUpdate(sql, role, userId);
    }

    /**
     * Update password
     */
    public int updatePassword(Long userId, String password) {
        String sql = "UPDATE users SET password = ?, updated_at = NOW() WHERE id = ?";
        return executeUpdate(sql, password, userId);
    }

    /**
     * Update balance
     */
    public int updateBalance(Long userId, java.math.BigDecimal amount) {
        String sql = "UPDATE users SET balance = balance + ?, updated_at = NOW() WHERE id = ?";
        return executeUpdate(sql, amount, userId);
    }

    /**
     * Update points
     */
    public int updatePoints(Long userId, Integer points) {
        String sql = "UPDATE users SET points = points + ?, updated_at = NOW() WHERE id = ?";
        return executeUpdate(sql, points, userId);
    }

    /**
     * List all users with pagination
     */
    public List<User> findAll(int page, int pageSize) {
        String sql = "SELECT * FROM users ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return executeQuery(sql, pageSize, (page - 1) * pageSize);
    }

    /**
     * Count all users
     */
    public long count() {
        String sql = "SELECT COUNT(*) FROM users";
        return executeCount(sql);
    }

    /**
     * Search users
     */
    public List<User> search(String keyword, int page, int pageSize) {
        String sql = """
            SELECT * FROM users 
            WHERE username LIKE ? OR phone LIKE ? OR real_name LIKE ?
            ORDER BY created_at DESC LIMIT ? OFFSET ?
            """;
        String pattern = "%" + keyword + "%";
        return executeQuery(sql, pattern, pattern, pattern, pageSize, (page - 1) * pageSize);
    }

    /**
     * Count search results
     */
    public long countSearch(String keyword) {
        String sql = """
            SELECT COUNT(*) FROM users 
            WHERE username LIKE ? OR phone LIKE ? OR real_name LIKE ?
            """;
        String pattern = "%" + keyword + "%";
        return executeCount(sql, pattern, pattern, pattern);
    }

    /**
     * Search users with filters (keyword, verificationStatus, role)
     */
    public List<User> searchByFilters(String keyword, Integer verificationStatus, String role, int page, int pageSize) {
        StringBuilder sql = new StringBuilder("SELECT * FROM users WHERE 1=1");
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (keyword != null && !keyword.isEmpty()) {
            sql.append(" AND (username LIKE ? OR phone LIKE ? OR real_name LIKE ?)");
            String pattern = "%" + keyword + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        if (verificationStatus != null) {
            sql.append(" AND verification_status = ?");
            params.add(verificationStatus);
        }
        if (role != null && !role.isEmpty()) {
            sql.append(" AND role = ?");
            params.add(role);
        }
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add((page - 1) * pageSize);
        return executeQuery(sql.toString(), params.toArray());
    }

    public long countByFilters(String keyword, Integer verificationStatus, String role) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM users WHERE 1=1");
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (keyword != null && !keyword.isEmpty()) {
            sql.append(" AND (username LIKE ? OR phone LIKE ? OR real_name LIKE ?)");
            String pattern = "%" + keyword + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        if (verificationStatus != null) {
            sql.append(" AND verification_status = ?");
            params.add(verificationStatus);
        }
        if (role != null && !role.isEmpty()) {
            sql.append(" AND role = ?");
            params.add(role);
        }
        return executeCount(sql.toString(), params.toArray());
    }

    /**
     * Atomically increment user's cumulative spending and update vip level based on thresholds
     */
    public int incrementCumulativeSpending(Long userId, java.math.BigDecimal amount) {
        String sql = "UPDATE users SET cumulative_spending = COALESCE(cumulative_spending,0) + ?, " +
                     "vip_level = CASE WHEN COALESCE(cumulative_spending,0) + ? > 5000 THEN 2 " +
                     "WHEN COALESCE(cumulative_spending,0) + ? >= 1000 THEN 1 ELSE 0 END, " +
                     "updated_at = NOW() WHERE id = ?";
        return executeUpdate(sql, amount, amount, amount, userId);
    }

    /**
     * Set vip level explicitly (admin)
     */
    public int setVipLevel(Long userId, Integer level) {
        String sql = "UPDATE users SET vip_level = ?, updated_at = NOW() WHERE id = ?";
        return executeUpdate(sql, level, userId);
    }
}
