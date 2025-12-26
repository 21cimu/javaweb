package com.carrental.dao;

import com.carrental.model.FundsFlowLog;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FundsFlowLogDao extends BaseDao<FundsFlowLog> {

    @Override
    protected FundsFlowLog mapRow(ResultSet rs) throws SQLException {
        FundsFlowLog log = new FundsFlowLog();
        log.setId(getLong(rs, "id"));
        log.setFlowNo(getString(rs, "flow_no"));
        log.setOrderId(getLong(rs, "order_id"));
        log.setOrderNo(getString(rs, "order_no"));
        log.setType(getString(rs, "type"));
        log.setAmount(rs.getBigDecimal("amount"));
        log.setChannel(getString(rs, "channel"));
        log.setOperatorId(getLong(rs, "operator_id"));
        log.setOperatorName(getString(rs, "operator_name"));
        log.setRemark(getString(rs, "remark"));
        log.setCreatedAt(getLocalDateTime(rs, "created_at"));
        return log;
    }

    public Long create(FundsFlowLog log) {
        String sql = """
            INSERT INTO funds_flow_logs
                (flow_no, order_id, order_no, type, amount, channel,
                 operator_id, operator_name, remark, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
            """;
        return executeInsert(sql, log.getFlowNo(), log.getOrderId(), log.getOrderNo(),
            log.getType(), log.getAmount(), log.getChannel(), log.getOperatorId(),
            log.getOperatorName(), log.getRemark());
    }

    public List<FundsFlowLog> findAll(String keyword, String type, String channel,
            LocalDateTime startDate, LocalDateTime endDate, int page, int pageSize) {
        StringBuilder sql = new StringBuilder("SELECT * FROM funds_flow_logs WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (flow_no LIKE ? OR order_no LIKE ?)");
            String like = "%" + keyword + "%";
            params.add(like);
            params.add(like);
        }
        if (type != null && !type.isBlank()) {
            sql.append(" AND type = ?");
            params.add(type);
        }
        if (channel != null && !channel.isBlank()) {
            sql.append(" AND channel = ?");
            params.add(channel);
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

    public long countAll(String keyword, String type, String channel,
            LocalDateTime startDate, LocalDateTime endDate) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM funds_flow_logs WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (flow_no LIKE ? OR order_no LIKE ?)");
            String like = "%" + keyword + "%";
            params.add(like);
            params.add(like);
        }
        if (type != null && !type.isBlank()) {
            sql.append(" AND type = ?");
            params.add(type);
        }
        if (channel != null && !channel.isBlank()) {
            sql.append(" AND channel = ?");
            params.add(channel);
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
