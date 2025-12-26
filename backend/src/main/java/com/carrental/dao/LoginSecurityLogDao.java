package com.carrental.dao;

import com.carrental.model.LoginSecurityLog;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LoginSecurityLogDao extends BaseDao<LoginSecurityLog> {

    @Override
    protected LoginSecurityLog mapRow(ResultSet rs) throws SQLException {
        LoginSecurityLog log = new LoginSecurityLog();
        log.setId(getLong(rs, "id"));
        log.setUserId(getLong(rs, "user_id"));
        log.setAccount(getString(rs, "account"));
        log.setIp(getString(rs, "ip"));
        log.setLocation(getString(rs, "location"));
        log.setDevice(getString(rs, "device"));
        log.setResult(getString(rs, "result"));
        log.setMessage(getString(rs, "message"));
        log.setCreatedAt(getLocalDateTime(rs, "created_at"));
        return log;
    }

    public Long create(LoginSecurityLog log) {
        String sql = """
            INSERT INTO login_security_logs
                (user_id, account, ip, location, device, result, message, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
            """;
        return executeInsert(sql, log.getUserId(), log.getAccount(), log.getIp(),
            log.getLocation(), log.getDevice(), log.getResult(), log.getMessage());
    }

    public List<LoginSecurityLog> findAll(String account, String result,
            LocalDateTime startDate, LocalDateTime endDate, int page, int pageSize) {
        StringBuilder sql = new StringBuilder("SELECT * FROM login_security_logs WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (account != null && !account.isBlank()) {
            sql.append(" AND account LIKE ?");
            params.add("%" + account + "%");
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

    public long countAll(String account, String result,
            LocalDateTime startDate, LocalDateTime endDate) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM login_security_logs WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (account != null && !account.isBlank()) {
            sql.append(" AND account LIKE ?");
            params.add("%" + account + "%");
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
