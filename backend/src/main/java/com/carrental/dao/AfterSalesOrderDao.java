package com.carrental.dao;

import com.carrental.model.AfterSalesOrder;
import com.carrental.util.DatabaseUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AfterSalesOrderDao extends BaseDao<AfterSalesOrder> {

    @Override
    protected AfterSalesOrder mapRow(ResultSet rs) throws SQLException {
        AfterSalesOrder a = new AfterSalesOrder();
        a.setId(rs.getLong("id"));
        a.setAfterNo(rs.getString("after_no"));
        a.setOrderId(rs.getLong("order_id"));
        a.setOrderNo(rs.getString("order_no"));
        a.setUserId(rs.getLong("user_id"));
        a.setUserName(rs.getString("user_name"));
        a.setUserPhone(rs.getString("user_phone"));
        a.setType(rs.getInt("type"));
        a.setReasonCode(rs.getString("reason_code"));
        a.setDescription(rs.getString("description"));
        a.setExpectedSolution(rs.getString("expected_solution"));
        a.setRefundAmount(rs.getBigDecimal("refund_amount"));
        a.setApprovedRefundAmount(rs.getBigDecimal("approved_refund_amount"));
        a.setEvidenceImages(rs.getString("evidence_images"));
        a.setStatus(rs.getInt("status"));
        a.setAuditAdminId(rs.getLong("audit_admin_id"));
        a.setAuditAdminName(rs.getString("audit_admin_name"));
        Timestamp auditTime = rs.getTimestamp("audit_time");
        if (auditTime != null) {
            a.setAuditTime(auditTime.toLocalDateTime());
        }
        a.setAuditRemark(rs.getString("audit_remark"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            a.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            a.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return a;
    }

    public Long create(AfterSalesOrder aso) throws SQLException {
        String sql = "INSERT INTO after_sales_orders (after_no, order_id, order_no, user_id, user_name, user_phone, type, reason_code, description, expected_solution, refund_amount, evidence_images, status, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, aso.getAfterNo());
            ps.setLong(2, aso.getOrderId());
            ps.setString(3, aso.getOrderNo());
            ps.setLong(4, aso.getUserId());
            ps.setString(5, aso.getUserName());
            ps.setString(6, aso.getUserPhone());
            ps.setInt(7, aso.getType());
            ps.setString(8, aso.getReasonCode());
            ps.setString(9, aso.getDescription());
            ps.setString(10, aso.getExpectedSolution());
            ps.setBigDecimal(11, aso.getRefundAmount() == null ? BigDecimal.ZERO : aso.getRefundAmount());
            ps.setString(12, aso.getEvidenceImages());
            ps.setInt(13, aso.getStatus());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return null;
    }

    public AfterSalesOrder findLatestByOrderId(Long orderId) throws SQLException {
        String sql = "SELECT * FROM after_sales_orders WHERE order_id = ? ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public boolean hasActiveByOrderId(Long orderId) throws SQLException {
        String sql = "SELECT COUNT(1) FROM after_sales_orders WHERE order_id = ? AND status IN (1,2,5)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1) > 0;
                }
            }
        }
        return false;
    }

    public List<AfterSalesOrder> findAll(Integer status, String orderNo, Long userId,
                                         java.time.LocalDateTime startDate, java.time.LocalDateTime endDate,
                                         int page, int pageSize) {
        StringBuilder sql = new StringBuilder("SELECT * FROM after_sales_orders WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (status != null) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        if (orderNo != null && !orderNo.isEmpty()) {
            sql.append(" AND order_no LIKE ?");
            params.add("%" + orderNo + "%");
        }
        if (userId != null) {
            sql.append(" AND user_id = ?");
            params.add(userId);
        }
        if (startDate != null) {
            sql.append(" AND created_at >= ?");
            params.add(startDate);
        }
        if (endDate != null) {
            sql.append(" AND created_at <= ?");
            params.add(endDate);
        }

        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add((page - 1) * pageSize);

        return executeQuery(sql.toString(), params.toArray());
    }

    public long countAll(Integer status, String orderNo, Long userId,
                         java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM after_sales_orders WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (status != null) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        if (orderNo != null && !orderNo.isEmpty()) {
            sql.append(" AND order_no LIKE ?");
            params.add("%" + orderNo + "%");
        }
        if (userId != null) {
            sql.append(" AND user_id = ?");
            params.add(userId);
        }
        if (startDate != null) {
            sql.append(" AND created_at >= ?");
            params.add(startDate);
        }
        if (endDate != null) {
            sql.append(" AND created_at <= ?");
            params.add(endDate);
        }

        return executeCount(sql.toString(), params.toArray());
    }

    public int updateStatus(Long id, Integer status, java.math.BigDecimal approvedRefundAmount,
                            Long adminId, String adminName, String auditRemark) {
        String sql = "UPDATE after_sales_orders SET status = ?, approved_refund_amount = ?, " +
                "audit_admin_id = ?, audit_admin_name = ?, audit_remark = ?, audit_time = NOW(), updated_at = NOW() " +
                "WHERE id = ?";
        return executeUpdate(sql, status, approvedRefundAmount, adminId, adminName, auditRemark, id);
    }

    // 单条查询，供后台审核等使用
    public AfterSalesOrder findById(Long id) throws SQLException {
        String sql = "SELECT * FROM after_sales_orders WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }
}
