package com.carrental.dao;

import com.carrental.util.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Base DAO class with common database operations
 */
public abstract class BaseDao<T> {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Map ResultSet row to entity
     */
    protected abstract T mapRow(ResultSet rs) throws SQLException;

    /**
     * Execute query and return list of results
     */
    protected List<T> executeQuery(String sql, Object... params) {
        List<T> results = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            setParameters(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error executing query: {}", sql, e);
        }
        return results;
    }

    /**
     * Execute query and return single result
     */
    protected T executeQuerySingle(String sql, Object... params) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            setParameters(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error executing query: {}", sql, e);
        }
        return null;
    }

    /**
     * Execute update (INSERT, UPDATE, DELETE)
     */
    protected int executeUpdate(String sql, Object... params) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            setParameters(stmt, params);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error executing update: {}", sql, e);
            return -1;
        }
    }

    /**
     * Execute insert and return generated key
     */
    protected Long executeInsert(String sql, Object... params) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setParameters(stmt, params);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getLong(1);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error executing insert: {}", sql, e);
        }
        return null;
    }

    /**
     * Execute count query
     */
    protected long executeCount(String sql, Object... params) {
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            setParameters(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error executing count: {}", sql, e);
        }
        return 0;
    }

    /**
     * Set parameters for prepared statement
     */
    protected void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                Object param = params[i];
                if (param == null) {
                    stmt.setNull(i + 1, Types.NULL);
                } else if (param instanceof String) {
                    stmt.setString(i + 1, (String) param);
                } else if (param instanceof Integer) {
                    stmt.setInt(i + 1, (Integer) param);
                } else if (param instanceof Long) {
                    stmt.setLong(i + 1, (Long) param);
                } else if (param instanceof Double) {
                    stmt.setDouble(i + 1, (Double) param);
                } else if (param instanceof Boolean) {
                    stmt.setBoolean(i + 1, (Boolean) param);
                } else if (param instanceof java.math.BigDecimal) {
                    stmt.setBigDecimal(i + 1, (java.math.BigDecimal) param);
                } else if (param instanceof java.time.LocalDateTime) {
                    stmt.setTimestamp(i + 1, Timestamp.valueOf((java.time.LocalDateTime) param));
                } else if (param instanceof java.time.LocalDate) {
                    stmt.setDate(i + 1, Date.valueOf((java.time.LocalDate) param));
                } else if (param instanceof Timestamp) {
                    stmt.setTimestamp(i + 1, (Timestamp) param);
                } else {
                    stmt.setObject(i + 1, param);
                }
            }
        }
    }

    /**
     * Get nullable string from ResultSet
     */
    protected String getString(ResultSet rs, String column) throws SQLException {
        return rs.getString(column);
    }

    /**
     * Get nullable integer from ResultSet
     */
    protected Integer getInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    /**
     * Get nullable long from ResultSet
     */
    protected Long getLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    /**
     * Get nullable boolean from ResultSet
     */
    protected Boolean getBoolean(ResultSet rs, String column) throws SQLException {
        boolean value = rs.getBoolean(column);
        return rs.wasNull() ? null : value;
    }

    /**
     * Get LocalDateTime from ResultSet
     */
    protected java.time.LocalDateTime getLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts != null ? ts.toLocalDateTime() : null;
    }

    /**
     * Get LocalDate from ResultSet
     */
    protected java.time.LocalDate getLocalDate(ResultSet rs, String column) throws SQLException {
        Date date = rs.getDate(column);
        return date != null ? date.toLocalDate() : null;
    }
}
