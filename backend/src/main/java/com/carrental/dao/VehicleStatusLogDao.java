package com.carrental.dao;

import com.carrental.model.VehicleStatusLog;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VehicleStatusLogDao extends BaseDao<VehicleStatusLog> {

    @Override
    protected VehicleStatusLog mapRow(ResultSet rs) throws SQLException {
        VehicleStatusLog log = new VehicleStatusLog();
        log.setId(getLong(rs, "id"));
        log.setVehicleId(getLong(rs, "vehicle_id"));
        log.setVehicleName(getString(rs, "vehicle_name"));
        log.setPlateNumber(getString(rs, "plate_number"));
        log.setFromStatus(getInt(rs, "from_status"));
        log.setToStatus(getInt(rs, "to_status"));
        log.setOperatorId(getLong(rs, "operator_id"));
        log.setOperatorName(getString(rs, "operator_name"));
        log.setOperatorRole(getString(rs, "operator_role"));
        log.setRemark(getString(rs, "remark"));
        log.setCreatedAt(getLocalDateTime(rs, "created_at"));
        return log;
    }

    public Long create(VehicleStatusLog log) {
        String sql = """
            INSERT INTO vehicle_status_logs
                (vehicle_id, vehicle_name, plate_number, from_status, to_status,
                 operator_id, operator_name, operator_role, remark, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
            """;
        return executeInsert(sql, log.getVehicleId(), log.getVehicleName(),
            log.getPlateNumber(), log.getFromStatus(), log.getToStatus(),
            log.getOperatorId(), log.getOperatorName(), log.getOperatorRole(),
            log.getRemark());
    }

    public List<VehicleStatusLog> findAll(String vehicle, Integer status,
            LocalDateTime startDate, LocalDateTime endDate, int page, int pageSize) {
        StringBuilder sql = new StringBuilder("SELECT * FROM vehicle_status_logs WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (vehicle != null && !vehicle.isBlank()) {
            sql.append(" AND (vehicle_name LIKE ? OR plate_number LIKE ?)");
            String like = "%" + vehicle + "%";
            params.add(like);
            params.add(like);
        }
        if (status != null) {
            sql.append(" AND to_status = ?");
            params.add(status);
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

    public long countAll(String vehicle, Integer status,
            LocalDateTime startDate, LocalDateTime endDate) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM vehicle_status_logs WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (vehicle != null && !vehicle.isBlank()) {
            sql.append(" AND (vehicle_name LIKE ? OR plate_number LIKE ?)");
            String like = "%" + vehicle + "%";
            params.add(like);
            params.add(like);
        }
        if (status != null) {
            sql.append(" AND to_status = ?");
            params.add(status);
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
