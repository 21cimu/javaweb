package com.carrental.dao;

import com.carrental.model.AdminOperationLog;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AdminOperationLogDao extends BaseDao<AdminOperationLog> {

    @Override
    protected AdminOperationLog mapRow(ResultSet rs) throws SQLException {
        AdminOperationLog log = new AdminOperationLog();
        log.setId(getLong(rs, "id"));
        log.setOperatorId(getLong(rs, "operator_id"));
        log.setOperatorName(getString(rs, "operator_name"));
        log.setOperatorRole(getString(rs, "operator_role"));
        log.setModule(getString(rs, "module"));
        log.setAction(getString(rs, "action"));
        log.setTarget(getString(rs, "target"));
        log.setResult(getString(rs, "result"));
        log.setIp(getString(rs, "ip"));
        log.setRemark(getString(rs, "remark"));
        log.setCreatedAt(getLocalDateTime(rs, "created_at"));
        return log;
    }

    public Long create(AdminOperationLog log) {
        String sql = """
            INSERT INTO admin_operation_logs
                (operator_id, operator_name, operator_role, module, action, target,
                 result, ip, remark, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
            """;
        return executeInsert(sql, log.getOperatorId(), log.getOperatorName(),
            log.getOperatorRole(), log.getModule(), log.getAction(), log.getTarget(),
            log.getResult(), log.getIp(), log.getRemark());
    }

    public List<AdminOperationLog> findAll(String operator, String module, String result,
            LocalDateTime startDate, LocalDateTime endDate, int page, int pageSize) {
        StringBuilder sql = new StringBuilder("SELECT * FROM admin_operation_logs WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (operator != null && !operator.isBlank()) {
            sql.append(" AND operator_name LIKE ?");
            params.add("%" + operator + "%");
        }
        if (module != null && !module.isBlank()) {
            sql.append(" AND module = ?");
            params.add(module);
        }
        if (result != null && !result.isBlank()) {
            sql.append(" AND result = ?");
            params.add(result);
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

    public long countAll(String operator, String module, String result,
            LocalDateTime startDate, LocalDateTime endDate) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM admin_operation_logs WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (operator != null && !operator.isBlank()) {
            sql.append(" AND operator_name LIKE ?");
            params.add("%" + operator + "%");
        }
        if (module != null && !module.isBlank()) {
            sql.append(" AND module = ?");
            params.add(module);
        }
        if (result != null && !result.isBlank()) {
            sql.append(" AND result = ?");
            params.add(result);
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
