package com.carrental.servlet.admin;

import com.carrental.dao.UserDao;
import com.carrental.util.DatabaseUtil;
import com.carrental.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Admin servlet to manage user-held coupons
 */
@WebServlet(name = "AdminUserCouponServlet", urlPatterns = {"/api/admin/user-coupons/*"})
public class AdminUserCouponServlet extends HttpServlet {

    private final UserDao userDao = new UserDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!isAdmin(request)) {
            JsonUtil.writeError(response, 403, "无权限");
            return;
        }

        int page = parseIntOrDefault(request.getParameter("page"), 1);
        int pageSize = parseIntOrDefault(request.getParameter("pageSize"), 20);
        String userIdStr = request.getParameter("userId");
        Long userId = userIdStr != null && !userIdStr.isEmpty() ? Long.parseLong(userIdStr) : null;
        String statusStr = request.getParameter("status");
        Integer status = null;
        try {
            if (statusStr != null && !statusStr.isEmpty()) {
                status = Integer.parseInt(statusStr);
            }
        } catch (NumberFormatException e) {
            JsonUtil.writeError(response, 400, "无效的状态参数");
            return;
        }
        String keyword = request.getParameter("keyword");

        List<Map<String, Object>> items = new ArrayList<>();
        long total = 0;

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT uc.id, uc.user_id, uc.coupon_id, uc.order_id, uc.status, uc.used_at, uc.created_at, ");
        sql.append("c.code, c.name, c.end_time, u.username, u.phone ");
        sql.append("FROM car_rental.user_coupons uc JOIN car_rental.coupons c ON uc.coupon_id = c.id ");
        sql.append("JOIN car_rental.users u ON uc.user_id = u.id WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        if (userId != null) {
            sql.append(" AND uc.user_id = ?");
            params.add(userId);
        }
        if (status != null) {
            sql.append(" AND uc.status = ?");
            params.add(status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            sql.append(" AND (u.username LIKE ? OR u.phone LIKE ? OR c.code LIKE ?)");
            String like = "%" + keyword + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        sql.append(" ORDER BY uc.created_at DESC LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add((page - 1) * pageSize);

        StringBuilder countSql = new StringBuilder();
        countSql.append("SELECT COUNT(*) FROM car_rental.user_coupons uc ");
        countSql.append("JOIN car_rental.coupons c ON uc.coupon_id = c.id ");
        countSql.append("JOIN car_rental.users u ON uc.user_id = u.id WHERE 1=1");
        List<Object> countParams = new ArrayList<>();
        if (userId != null) {
            countSql.append(" AND uc.user_id = ?");
            countParams.add(userId);
        }
        if (status != null) {
            countSql.append(" AND uc.status = ?");
            countParams.add(status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            countSql.append(" AND (u.username LIKE ? OR u.phone LIKE ? OR c.code LIKE ?)");
            String like = "%" + keyword + "%";
            countParams.add(like);
            countParams.add(like);
            countParams.add(like);
        }

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString());
             PreparedStatement countStmt = conn.prepareStatement(countSql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", rs.getLong("id"));
                    m.put("userId", rs.getLong("user_id"));
                    m.put("username", rs.getString("username"));
                    m.put("phone", rs.getString("phone"));
                    m.put("couponId", rs.getLong("coupon_id"));
                    m.put("code", rs.getString("code"));
                    m.put("name", rs.getString("name"));
                    m.put("orderId", rs.getObject("order_id"));
                    m.put("status", rs.getInt("status"));
                    m.put("usedAt", rs.getTimestamp("used_at"));
                    m.put("createdAt", rs.getTimestamp("created_at"));
                    m.put("couponEndTime", rs.getTimestamp("end_time"));
                    items.add(m);
                }
            }

            for (int i = 0; i < countParams.size(); i++) {
                countStmt.setObject(i + 1, countParams.get(i));
            }
            try (ResultSet crs = countStmt.executeQuery()) {
                if (crs.next()) total = crs.getLong(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.writeError(response, 500, "数据库错误");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("items", items);
        JsonUtil.writeSuccess(response, data);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!isAdmin(request)) {
            JsonUtil.writeError(response, 403, "无权限");
            return;
        }

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || !pathInfo.startsWith("/")) {
            JsonUtil.writeError(response, 400, "缺少 ID");
            return;
        }
        String[] parts = pathInfo.split("/");
        if (parts.length < 2) {
            JsonUtil.writeError(response, 400, "缺少 ID");
            return;
        }
        Long id;
        try {
            id = Long.parseLong(parts[1]);
        } catch (NumberFormatException ex) {
            JsonUtil.writeError(response, 400, "无效的 ID");
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) JsonUtil.fromJson(request, Map.class);
        Integer status = body.get("status") == null ? null : ((Number) body.get("status")).intValue();

        if (status == null) {
            JsonUtil.writeError(response, 400, "status 为必填");
            return;
        }

        String sql = "UPDATE car_rental.user_coupons SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, status);
            stmt.setLong(2, id);
            int affected = stmt.executeUpdate();
            if (affected > 0) {
                JsonUtil.writeSuccess(response, "更新成功", null);
            } else {
                JsonUtil.writeError(response, 404, "记录不存在");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.writeError(response, 500, "数据库错误");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!isAdmin(request)) {
            JsonUtil.writeError(response, 403, "无权限");
            return;
        }

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || !pathInfo.startsWith("/")) {
            JsonUtil.writeError(response, 400, "缺少 ID");
            return;
        }
        String[] parts = pathInfo.split("/");
        if (parts.length < 2) {
            JsonUtil.writeError(response, 400, "缺少 ID");
            return;
        }
        Long id2;
        try {
            id2 = Long.parseLong(parts[1]);
        } catch (NumberFormatException ex) {
            JsonUtil.writeError(response, 400, "无效的 ID");
            return;
        }

        // We will perform a soft-revoke: set status = 2 (已过期/回收)
        String sql = "UPDATE car_rental.user_coupons SET status = 2 WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id2);
            int affected = stmt.executeUpdate();
            if (affected > 0) {
                JsonUtil.writeSuccess(response, "已回收", null);
            } else {
                JsonUtil.writeError(response, 404, "记录不存在");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JsonUtil.writeError(response, 500, "数据库错误");
        }
    }

    private boolean isAdmin(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (role != null) {
            return "admin".equals(role) || "superadmin".equals(role);
        }
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return false;
        var user = userDao.findById(userId);
        return user != null && ("admin".equals(user.getRole()) || "superadmin".equals(user.getRole()));
    }

    private int parseIntOrDefault(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }
}
