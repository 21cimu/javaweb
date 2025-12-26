package com.carrental.servlet.admin;

import com.carrental.dao.CouponDao;
import com.carrental.dao.UserDao;
import com.carrental.model.Coupon;
import com.carrental.model.User;
import com.carrental.util.DatabaseUtil;
import com.carrental.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Admin servlet for managing coupons
 */
@WebServlet(name = "AdminCouponServlet", urlPatterns = {"/api/admin/coupons/*"})
public class AdminCouponServlet extends HttpServlet {

    private final CouponDao couponDao = new CouponDao();
    private final UserDao userDao = new UserDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request)) {
            JsonUtil.writeError(response, 403, "无权限");
            return;
        }

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || "/".equals(pathInfo)) {
            handleList(request, response);
        } else {
            // /{id}
            String[] parts = pathInfo.split("/");
            if (parts.length >= 2) {
                try {
                    Long id = Long.parseLong(parts[1]);
                    handleGet(request, response, id);
                } catch (NumberFormatException e) {
                    JsonUtil.writeError(response, 400, "无效的ID");
                }
            } else {
                JsonUtil.writeError(response, 404, "接口不存在");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request)) {
            JsonUtil.writeError(response, 403, "无权限");
            return;
        }

        String pathInfo = request.getPathInfo();
        if ("/issue".equals(pathInfo)) {
            handleIssue(request, response);
        } else if (pathInfo == null || "/".equals(pathInfo)) {
            handleCreate(request, response);
        } else {
            JsonUtil.writeError(response, 404, "接口不存在");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request)) {
            JsonUtil.writeError(response, 403, "无权限");
            return;
        }

        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.startsWith("/")) {
            String[] parts = pathInfo.split("/");
            if (parts.length >= 2) {
                try {
                    Long id = Long.parseLong(parts[1]);
                    handleUpdate(request, response, id);
                } catch (NumberFormatException e) {
                    JsonUtil.writeError(response, 400, "无效的ID");
                }
            } else {
                JsonUtil.writeError(response, 404, "接口不存在");
            }
        } else {
            JsonUtil.writeError(response, 404, "接口不存在");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!isAdmin(request)) {
            JsonUtil.writeError(response, 403, "无权限");
            return;
        }

        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.startsWith("/")) {
            String[] parts = pathInfo.split("/");
            if (parts.length >= 2) {
                try {
                    Long id = Long.parseLong(parts[1]);
                    handleDelete(request, response, id);
                } catch (NumberFormatException e) {
                    JsonUtil.writeError(response, 400, "无效的ID");
                }
            } else {
                JsonUtil.writeError(response, 404, "接口不存在");
            }
        } else {
            JsonUtil.writeError(response, 404, "接口不存在");
        }
    }

    private boolean isAdmin(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (role != null) {
            return "admin".equals(role) || "superadmin".equals(role);
        }
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return false;
        User user = userDao.findById(userId);
        return user != null && ("admin".equals(user.getRole()) || "superadmin".equals(user.getRole()));
    }

    private void handleList(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int page = parseIntOrDefault(request.getParameter("page"), 1);
        int pageSize = parseIntOrDefault(request.getParameter("pageSize"), 20);
        String keyword = request.getParameter("keyword");
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

        List<Coupon> items = new ArrayList<>();
        long total = 0;

        StringBuilder sql = new StringBuilder("SELECT * FROM coupons WHERE 1=1");
        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM coupons WHERE 1=1");
        List<Object> params = new ArrayList<>();
        List<Object> countParams = new ArrayList<>();

        if (keyword != null && !keyword.isEmpty()) {
            sql.append(" AND (name LIKE ? OR code LIKE ?)");
            countSql.append(" AND (name LIKE ? OR code LIKE ?)");
            String like = "%" + keyword + "%";
            params.add(like);
            params.add(like);
            countParams.add(like);
            countParams.add(like);
        }
        if (status != null) {
            sql.append(" AND status = ?");
            countSql.append(" AND status = ?");
            params.add(status);
            countParams.add(status);
        }

        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add((page - 1) * pageSize);

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString());
             PreparedStatement countStmt = conn.prepareStatement(countSql.toString())) {

            // fill query params
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Coupon c = new Coupon();
                    c.setId(rs.getLong("id"));
                    c.setCode(rs.getString("code"));
                    c.setName(rs.getString("name"));
                    c.setDescription(rs.getString("description"));
                    c.setType(rs.getInt("type"));
                    c.setMinAmount(rs.getBigDecimal("min_amount"));
                    c.setDiscountAmount(rs.getBigDecimal("discount_amount"));
                    c.setDiscountRate(rs.getBigDecimal("discount_rate"));
                    c.setMaxDiscount(rs.getBigDecimal("max_discount"));
                    c.setTotalCount(rs.getInt("total_count"));
                    c.setUsedCount(rs.getInt("used_count"));
                    c.setPerUserLimit(rs.getInt("per_user_limit"));
                    c.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
                    c.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
                    c.setApplicableCategories(rs.getString("applicable_categories"));
                    c.setApplicableVehicles(rs.getString("applicable_vehicles"));
                    c.setStatus(rs.getInt("status"));
                    c.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    c.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                    c.setVipLevelRequired(rs.getInt("vip_level_required"));
                    items.add(c);
                }
            }

            // fill count params
            for (int i = 0; i < countParams.size(); i++) {
                countStmt.setObject(i + 1, countParams.get(i));
            }
            try (ResultSet crs = countStmt.executeQuery()) {
                if (crs.next()) {
                    total = crs.getLong(1);
                }
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

    private void handleGet(HttpServletRequest request, HttpServletResponse response, Long id) throws IOException {
        Coupon coupon = couponDao.findById(id);
        if (coupon == null) {
            JsonUtil.writeError(response, 404, "优惠券不存在");
            return;
        }
        JsonUtil.writeSuccess(response, coupon);
    }

    private void handleCreate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> body = JsonUtil.fromJson(request, Map.class);
        try {
            Coupon coupon = parseCouponFromMap(body);
            // Basic validation
            if (coupon.getCode() == null || coupon.getCode().isEmpty()) {
                JsonUtil.writeError(response, 400, "code 不能为空");
                return;
            }
            Long id = couponDao.create(coupon);
            Map<String, Object> result = new HashMap<>();
            result.put("id", id);
            JsonUtil.writeSuccess(response, result);
        } catch (Exception e) {
            JsonUtil.writeError(response, 400, "参数错误: " + e.getMessage());
        }
    }

    private void handleUpdate(HttpServletRequest request, HttpServletResponse response, Long id) throws IOException {
        Map<String, Object> body = JsonUtil.fromJson(request, Map.class);
        Coupon existing = couponDao.findById(id);
        if (existing == null) {
            JsonUtil.writeError(response, 404, "优惠券不存在");
            return;
        }
        try {
            Coupon coupon = parseCouponFromMap(body);
            coupon.setId(id);
            couponDao.update(coupon);
            JsonUtil.writeSuccess(response, "更新成功", null);
        } catch (Exception e) {
            JsonUtil.writeError(response, 400, "参数错误: " + e.getMessage());
        }
    }

    private void handleDelete(HttpServletRequest request, HttpServletResponse response, Long id) throws IOException {
        Coupon existing = couponDao.findById(id);
        if (existing == null) {
            JsonUtil.writeError(response, 404, "优惠券不存在");
            return;
        }
        couponDao.updateStatus(id, 0); // disable instead of hard delete
        JsonUtil.writeSuccess(response, "删除成功", null);
    }

    private void handleIssue(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> body = JsonUtil.fromJson(request, Map.class);
        Long couponId = body.get("couponId") == null ? null : ((Number) body.get("couponId")).longValue();
        List<Object> userIdsObj = (List<Object>) body.get("userIds");
        String note = (String) body.get("note");

        if (couponId == null || userIdsObj == null || userIdsObj.isEmpty()) {
            JsonUtil.writeError(response, 400, "couponId 和 userIds 为必填");
            return;
        }

        Long adminId = (Long) request.getAttribute("userId");
        if (adminId == null) {
            JsonUtil.writeError(response, 401, "管理员未登录");
            return;
        }

        Coupon coupon = couponDao.findById(couponId);
        if (coupon == null) {
            JsonUtil.writeError(response, 404, "优惠券不存在");
            return;
        }

        List<Long> success = new ArrayList<>();
        List<Map<String, Object>> failed = new ArrayList<>();

        for (Object uidObj : userIdsObj) {
            Long userId = ((Number) uidObj).longValue();
            User user = userDao.findById(userId);
            if (user == null) {
                Map<String, Object> f = new HashMap<>();
                f.put("userId", userId);
                f.put("reason", "用户不存在");
                failed.add(f);
                continue;
            }

            // Validate coupon status and expiry
            if (coupon.getStatus() != 1) {
                Map<String, Object> f = new HashMap<>();
                f.put("userId", userId);
                f.put("reason", "优惠券已被禁用");
                failed.add(f);
                continue;
            }
            if (coupon.getEndTime().isBefore(LocalDateTime.now())) {
                Map<String, Object> f = new HashMap<>();
                f.put("userId", userId);
                f.put("reason", "优惠券已过期");
                failed.add(f);
                continue;
            }
            Integer totalCount = coupon.getTotalCount();
            int usedCount = coupon.getUsedCount() == null ? 0 : coupon.getUsedCount();
            if (totalCount != null && totalCount > 0 && usedCount >= totalCount) {
                Map<String, Object> f = new HashMap<>();
                f.put("userId", userId);
                f.put("reason", "优惠券已领完");
                failed.add(f);
                continue;
            }

            int userCount = getUserCouponCount(userId, couponId);
            if (coupon.getPerUserLimit() != null && userCount >= coupon.getPerUserLimit()) {
                Map<String, Object> f = new HashMap<>();
                f.put("userId", userId);
                f.put("reason", "已达到领取上限");
                failed.add(f);
                continue;
            }

            boolean issued = issueCouponToUser(userId, couponId, adminId);
            if (issued) {
                success.add(userId);
                // increment used_count in coupon
                couponDao.incrementUsedCount(couponId);
            } else {
                Map<String, Object> f = new HashMap<>();
                f.put("userId", userId);
                f.put("reason", "数据库错误");
                failed.add(f);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("failed", failed);
        JsonUtil.writeSuccess(response, result);
    }

    private boolean issueCouponToUser(Long userId, Long couponId, Long adminId) {
        // Use existing columns to insert a user coupon. The issued admin and assigned time
        // can be recorded in a separate update if the DB schema supports it. To keep
        // compatibility with environments where new columns may not yet be recognized,
        // perform a basic insert only.
        String sql = "INSERT INTO user_coupons (user_id, coupon_id, status, created_at) VALUES (?, ?, 0, NOW())";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, couponId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    private int getUserCouponCount(Long userId, Long couponId) {
        String sql = "SELECT COUNT(*) FROM user_coupons WHERE user_id = ? AND coupon_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, couponId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            // log
        }
        return 0;
    }

    private Coupon parseCouponFromMap(Map<String, Object> body) {
        Coupon coupon = new Coupon();
        if (body.get("code") != null) coupon.setCode((String) body.get("code"));
        if (body.get("name") != null) coupon.setName((String) body.get("name"));
        if (body.get("description") != null) coupon.setDescription((String) body.get("description"));

        // 兼容前端字段既可能是数字也可能是字符串的情况
        coupon.setType(parseIntegerField(body.get("type")));

        if (body.get("minAmount") != null) coupon.setMinAmount(new java.math.BigDecimal(body.get("minAmount").toString()));
        if (body.get("discountAmount") != null) coupon.setDiscountAmount(new java.math.BigDecimal(body.get("discountAmount").toString()));
        if (body.get("discountRate") != null) coupon.setDiscountRate(new java.math.BigDecimal(body.get("discountRate").toString()));
        if (body.get("maxDiscount") != null) coupon.setMaxDiscount(new java.math.BigDecimal(body.get("maxDiscount").toString()));

        coupon.setTotalCount(parseIntegerField(body.get("totalCount")));
        coupon.setPerUserLimit(parseIntegerField(body.get("perUserLimit")));

        Object startTimeObj = body.get("startTime");
        Object endTimeObj = body.get("endTime");
        if (startTimeObj != null) {
            LocalDate startDate = LocalDate.parse(startTimeObj.toString());
            coupon.setStartTime(startDate.atStartOfDay());
        }
        if (endTimeObj != null) {
            LocalDate endDate = LocalDate.parse(endTimeObj.toString());
            coupon.setEndTime(endDate.atTime(LocalTime.of(23, 59, 59)));
        }
        if (body.get("applicableCategories") != null) coupon.setApplicableCategories((String) body.get("applicableCategories"));
        if (body.get("applicableVehicles") != null) coupon.setApplicableVehicles((String) body.get("applicableVehicles"));

        coupon.setStatus(parseIntegerField(body.get("status")));

        // 新增：最低会员等级门槛 vipLevelRequired，0/1/2
        coupon.setVipLevelRequired(parseIntegerField(body.get("vipLevelRequired")));

        return coupon;
    }

    /**
     * 将可能为 Number 或 String 的字段安全转换为 Integer，null/空返回 null。
     */
    private Integer parseIntegerField(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            String s = ((String) value).trim();
            if (s.isEmpty()) return null;
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("数值字段格式错误: " + s);
            }
        }
        throw new IllegalArgumentException("不支持的数值类型: " + value.getClass());
    }

    private int parseIntOrDefault(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }
}
