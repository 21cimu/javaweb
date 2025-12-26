package com.carrental.dao;

import com.carrental.model.OrderEventLog;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderEventLogDao extends BaseDao<OrderEventLog> {

    @Override
    protected OrderEventLog mapRow(ResultSet rs) throws SQLException {
        OrderEventLog log = new OrderEventLog();
        log.setId(getLong(rs, "id"));
        log.setOrderId(getLong(rs, "order_id"));
        log.setOrderNo(getString(rs, "order_no"));
        log.setEventType(getString(rs, "event_type"));
        log.setStage(getString(rs, "stage"));
        log.setOperatorId(getLong(rs, "operator_id"));
        log.setOperatorName(getString(rs, "operator_name"));
        log.setOperatorRole(getString(rs, "operator_role"));
        log.setMessage(getString(rs, "message"));
        log.setCreatedAt(getLocalDateTime(rs, "created_at"));
        return log;
    }

    public Long create(OrderEventLog log) {
        String sql = """
            INSERT INTO order_event_logs
                (order_id, order_no, event_type, stage, operator_id,
                 operator_name, operator_role, message, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
            """;
        return executeInsert(sql, log.getOrderId(), log.getOrderNo(), log.getEventType(),
            log.getStage(), log.getOperatorId(), log.getOperatorName(), log.getOperatorRole(),
            log.getMessage());
    }

    public List<OrderEventLog> findAll(String orderNo, String eventType, String operator,
            LocalDateTime startDate, LocalDateTime endDate, int page, int pageSize) {
        StringBuilder sql = new StringBuilder("SELECT * FROM order_event_logs WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (orderNo != null && !orderNo.isBlank()) {
            sql.append(" AND order_no LIKE ?");
            params.add("%" + orderNo + "%");
        }
        if (eventType != null && !eventType.isBlank()) {
            sql.append(" AND event_type = ?");
            params.add(eventType);
        }
        if (operator != null && !operator.isBlank()) {
            sql.append(" AND operator_name LIKE ?");
            params.add("%" + operator + "%");
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

    public long countAll(String orderNo, String eventType, String operator,
            LocalDateTime startDate, LocalDateTime endDate) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM order_event_logs WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (orderNo != null && !orderNo.isBlank()) {
            sql.append(" AND order_no LIKE ?");
            params.add("%" + orderNo + "%");
        }
        if (eventType != null && !eventType.isBlank()) {
            sql.append(" AND event_type = ?");
            params.add(eventType);
        }
        if (operator != null && !operator.isBlank()) {
            sql.append(" AND operator_name LIKE ?");
            params.add("%" + operator + "%");
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
}
