package com.carrental.dao;

import com.carrental.model.Coupon;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Coupon Data Access Object
 */
public class CouponDao extends BaseDao<Coupon> {

    @Override
    protected Coupon mapRow(ResultSet rs) throws SQLException {
        Coupon coupon = new Coupon();
        coupon.setId(getLong(rs, "id"));
        coupon.setCode(getString(rs, "code"));
        coupon.setName(getString(rs, "name"));
        coupon.setDescription(getString(rs, "description"));
        coupon.setType(getInt(rs, "type"));
        coupon.setMinAmount(rs.getBigDecimal("min_amount"));
        coupon.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        coupon.setDiscountRate(rs.getBigDecimal("discount_rate"));
        coupon.setMaxDiscount(rs.getBigDecimal("max_discount"));
        coupon.setTotalCount(getInt(rs, "total_count"));
        coupon.setUsedCount(getInt(rs, "used_count"));
        coupon.setPerUserLimit(getInt(rs, "per_user_limit"));
        coupon.setStartTime(getLocalDateTime(rs, "start_time"));
        coupon.setEndTime(getLocalDateTime(rs, "end_time"));
        coupon.setApplicableCategories(getString(rs, "applicable_categories"));
        coupon.setApplicableVehicles(getString(rs, "applicable_vehicles"));
        coupon.setStatus(getInt(rs, "status"));
        coupon.setCreatedAt(getLocalDateTime(rs, "created_at"));
        coupon.setUpdatedAt(getLocalDateTime(rs, "updated_at"));
        return coupon;
    }

    /**
     * Find coupon by ID
     */
    public Coupon findById(Long id) {
        String sql = "SELECT * FROM coupons WHERE id = ?";
        return executeQuerySingle(sql, id);
    }

    /**
     * Find coupon by code
     */
    public Coupon findByCode(String code) {
        String sql = "SELECT * FROM coupons WHERE code = ?";
        return executeQuerySingle(sql, code);
    }

    /**
     * Create new coupon
     */
    public Long create(Coupon coupon) {
        String sql = """
            INSERT INTO coupons (code, name, description, type, min_amount,
                discount_amount, discount_rate, max_discount, total_count,
                used_count, per_user_limit, start_time, end_time,
                applicable_categories, applicable_vehicles, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
            """;
        return executeInsert(sql, coupon.getCode(), coupon.getName(),
            coupon.getDescription(), coupon.getType(), coupon.getMinAmount(),
            coupon.getDiscountAmount(), coupon.getDiscountRate(), coupon.getMaxDiscount(),
            coupon.getTotalCount(), coupon.getUsedCount(), coupon.getPerUserLimit(),
            coupon.getStartTime(), coupon.getEndTime(), coupon.getApplicableCategories(),
            coupon.getApplicableVehicles(), coupon.getStatus());
    }

    /**
     * Update coupon
     */
    public int update(Coupon coupon) {
        String sql = """
            UPDATE coupons SET 
                code = ?, name = ?, description = ?, type = ?, min_amount = ?,
                discount_amount = ?, discount_rate = ?, max_discount = ?,
                total_count = ?, per_user_limit = ?, start_time = ?, end_time = ?,
                applicable_categories = ?, applicable_vehicles = ?, status = ?,
                updated_at = NOW()
            WHERE id = ?
            """;
        return executeUpdate(sql, coupon.getCode(), coupon.getName(),
            coupon.getDescription(), coupon.getType(), coupon.getMinAmount(),
            coupon.getDiscountAmount(), coupon.getDiscountRate(), coupon.getMaxDiscount(),
            coupon.getTotalCount(), coupon.getPerUserLimit(),
            coupon.getStartTime(), coupon.getEndTime(), coupon.getApplicableCategories(),
            coupon.getApplicableVehicles(), coupon.getStatus(), coupon.getId());
    }

    /**
     * Increment used count
     */
    public int incrementUsedCount(Long id) {
        String sql = "UPDATE coupons SET used_count = used_count + 1, updated_at = NOW() WHERE id = ?";
        return executeUpdate(sql, id);
    }

    /**
     * Find available coupons
     */
    public List<Coupon> findAvailable() {
        String sql = """
            SELECT * FROM coupons 
            WHERE status = 1 
            AND start_time <= NOW() 
            AND end_time >= NOW()
            AND (total_count IS NULL OR used_count < total_count)
            ORDER BY created_at DESC
            """;
        return executeQuery(sql);
    }

    /**
     * Find all coupons (admin)
     */
    public List<Coupon> findAll(int page, int pageSize) {
        String sql = "SELECT * FROM coupons ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return executeQuery(sql, pageSize, (page - 1) * pageSize);
    }

    /**
     * Count all coupons
     */
    public long count() {
        String sql = "SELECT COUNT(*) FROM coupons";
        return executeCount(sql);
    }

    /**
     * Update status
     */
    public int updateStatus(Long id, Integer status) {
        String sql = "UPDATE coupons SET status = ?, updated_at = NOW() WHERE id = ?";
        return executeUpdate(sql, status, id);
    }
}
