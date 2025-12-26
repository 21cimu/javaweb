package com.carrental.servlet.marketing;

import com.carrental.dao.CouponDao;
import com.carrental.dao.UserDao;
import com.carrental.model.Coupon;
import com.carrental.model.User;
import com.carrental.util.JsonUtil;
import com.carrental.util.DatabaseUtil;
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
import java.time.LocalDateTime;
import java.util.*;

/**
 * Marketing Servlet for promotions and coupons
 */
@WebServlet(name = "MarketingServlet", urlPatterns = {"/api/marketing/*"})
public class MarketingServlet extends HttpServlet {

    private final CouponDao couponDao = new CouponDao();
    private final UserDao userDao = new UserDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        if ("/promotions".equals(pathInfo)) {
            handlePromotions(request, response);
        } else if ("/banners".equals(pathInfo)) {
            handleBanners(request, response);
        } else if ("/coupons".equals(pathInfo)) {
            handleCoupons(request, response);
        } else if ("/my-coupons".equals(pathInfo)) {
            handleMyCoupons(request, response);
        } else if ("/points".equals(pathInfo)) {
            handlePoints(request, response);
        } else {
            JsonUtil.writeError(response, 404, "接口不存在");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        Long userId = (Long) request.getAttribute("userId");

        if ("/claim-coupon".equals(pathInfo)) {
            handleClaimCoupon(request, response, userId);
        } else if ("/exchange-points".equals(pathInfo)) {
            handleExchangePoints(request, response, userId);
        } else {
            JsonUtil.writeError(response, 404, "接口不存在");
        }
    }

    private void handlePromotions(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // Return active promotions
        List<Map<String, Object>> promotions = new ArrayList<>();

        // 满减活动
        Map<String, Object> promo1 = new HashMap<>();
        promo1.put("id", 1);
        promo1.put("title", "首单立减50元");
        promo1.put("description", "新用户首单租车，立减50元");
        promo1.put("type", "new_user");
        // corresponds to frontend/public/images/promo/first.png
        promo1.put("image", "/images/promo/first.png");
        promotions.add(promo1);

        // 限时折扣
        Map<String, Object> promo2 = new HashMap<>();
        promo2.put("id", 2);
        promo2.put("title", "周末特惠8折");
        promo2.put("description", "每周五至周日租车享8折优惠");
        promo2.put("type", "weekend");
        // corresponds to frontend/public/images/promo/weekend.png
        promo2.put("image", "/images/promo/weekend.png");
        promotions.add(promo2);

        // 连租优惠
        Map<String, Object> promo3 = new HashMap<>();
        promo3.put("id", 3);
        promo3.put("title", "租7天送1天");
        promo3.put("description", "连续租车满7天，赠送1天免费用车");
        promo3.put("type", "long_term");
        // corresponds to frontend/public/images/promo/seven.png
        promo3.put("image", "/images/promo/seven.png");
        promotions.add(promo3);

        // 第二单半价
        Map<String, Object> promo4 = new HashMap<>();
        promo4.put("id", 4);
        promo4.put("title", "第二单半价");
        promo4.put("description", "同月内第二次租车享5折优惠");
        promo4.put("type", "second_order");
        // corresponds to frontend/public/images/promo/half.png
        promo4.put("image", "/images/promo/half.png");
        promotions.add(promo4);

        JsonUtil.writeSuccess(response, promotions);
    }

    private void handleBanners(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        List<Map<String, Object>> banners = new ArrayList<>();

        Map<String, Object> banner1 = new HashMap<>();
        banner1.put("id", 1);
        banner1.put("title", "新用户专享");
        banner1.put("image", "/images/banner/banner1.jpg");
        banner1.put("link", "/promotions/new-user");
        banners.add(banner1);

        Map<String, Object> banner2 = new HashMap<>();
        banner2.put("id", 2);
        banner2.put("title", "热门车型推荐");
        banner2.put("image", "/images/banner/banner2.jpg");
        banner2.put("link", "/vehicles?tag=hot");
        banners.add(banner2);

        Map<String, Object> banner3 = new HashMap<>();
        banner3.put("id", 3);
        banner3.put("title", "豪华车体验");
        banner3.put("image", "/images/banner/banner3.jpg");
        banner3.put("link", "/vehicles?category=luxury");
        banners.add(banner3);

        JsonUtil.writeSuccess(response, banners);
    }

    private void handleCoupons(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        List<Coupon> coupons = couponDao.findAvailable();
        JsonUtil.writeSuccess(response, coupons);
    }

    private void handleMyCoupons(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            JsonUtil.writeError(response, 401, "请先登录");
            return;
        }

        String statusStr = request.getParameter("status");
        Integer status = statusStr != null ? Integer.parseInt(statusStr) : null;

        List<Map<String, Object>> coupons = getUserCoupons(userId, status);
        JsonUtil.writeSuccess(response, coupons);
    }

    private void handlePoints(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            JsonUtil.writeError(response, 401, "请先登录");
            return;
        }

        User user = userDao.findById(userId);
        if (user == null) {
            JsonUtil.writeError(response, 404, "用户不存在");
            return;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("points", user.getPoints());
        
        // Points exchange items
        List<Map<String, Object>> items = new ArrayList<>();
        
        Map<String, Object> item1 = new HashMap<>();
        item1.put("id", 1);
        item1.put("name", "20元优惠券");
        item1.put("points", 200);
        item1.put("type", "coupon");
        items.add(item1);

        Map<String, Object> item2 = new HashMap<>();
        item2.put("id", 2);
        item2.put("name", "儿童座椅免费券");
        item2.put("points", 300);
        item2.put("type", "service");
        items.add(item2);

        Map<String, Object> item3 = new HashMap<>();
        item3.put("id", 3);
        item3.put("name", "保险升级券");
        item3.put("points", 500);
        item3.put("type", "insurance");
        items.add(item3);

        result.put("exchangeItems", items);

        JsonUtil.writeSuccess(response, result);
    }

    private void handleClaimCoupon(HttpServletRequest request, HttpServletResponse response,
            Long userId) throws IOException {
        if (userId == null) {
            JsonUtil.writeError(response, 401, "请先登录");
            return;
        }

        Map<String, Object> body = JsonUtil.fromJson(request, Map.class);
        Long couponId = ((Number) body.get("couponId")).longValue();

        Coupon coupon = couponDao.findById(couponId);
        if (coupon == null) {
            JsonUtil.writeError(response, 404, "优惠券不存在");
            return;
        }

        if (coupon.getStatus() != 1) {
            JsonUtil.writeError(response, 400, "优惠券已失效");
            return;
        }

        if (coupon.getEndTime().isBefore(LocalDateTime.now())) {
            JsonUtil.writeError(response, 400, "优惠券已过期");
            return;
        }

        Integer totalCount = coupon.getTotalCount();
        int usedCount = coupon.getUsedCount() == null ? 0 : coupon.getUsedCount();
        if (totalCount != null && totalCount > 0 &&
            usedCount >= totalCount) {
            JsonUtil.writeError(response, 400, "优惠券已领完");
            return;
        }

        // 检查用户是否已有一张未使用且未过期的同券
        int activeCount = getActiveUserCouponCount(userId, couponId);
        if (activeCount > 0) {
            JsonUtil.writeError(response, 400, "您已领取该优惠券，需使用或到期后才能再次领取");
            return;
        }

        // === 调整后的：会员等级限制逻辑 ===
        // 约定：coupon.vipLevelRequired
        // 0 或 null -> 不限（所有用户可领）
        // 1 -> 仅黄金会员可领（user.vipLevel >= 1）
        // 2 -> 仅钻石会员可领（user.vipLevel >= 2）
        Integer required = coupon.getVipLevelRequired();
        if (required != null && required > 0) {
            User user = userDao.findById(userId);
            int userVip = (user != null && user.getVipLevel() != null) ? user.getVipLevel() : 0;

            if (userVip < required) {
                String msg;
                if (required == 1) {
                    msg = "仅黄金会员及以上可领取该优惠券";
                } else if (required == 2) {
                    msg = "仅钻石会员可领取该优惠券";
                } else {
                    msg = "当前会员等级不足，无法领取该优惠券";
                }
                JsonUtil.writeError(response, 403, msg);
                return;
            }
        }

        // 领取优惠券
        boolean success = claimCoupon(userId, couponId);
        if (success) {
            JsonUtil.writeSuccess(response, "领取成功", null);
        } else {
            JsonUtil.writeError(response, 500, "领取失败");
        }
    }

    /**
     * Count user's active (未使用且未过期) coupons for a given coupon id
     */
    private int getActiveUserCouponCount(Long userId, Long couponId) {
        String sql = "SELECT COUNT(*) FROM car_rental.user_coupons uc JOIN car_rental.coupons c ON uc.coupon_id = c.id " +
                     "WHERE uc.user_id = ? AND uc.coupon_id = ? AND uc.status = 0 AND c.end_time >= NOW()";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, couponId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            // log error
        }
        return 0;
    }

    private void handleExchangePoints(HttpServletRequest request, HttpServletResponse response,
            Long userId) throws IOException {
        if (userId == null) {
            JsonUtil.writeError(response, 401, "请先登录");
            return;
        }

        Map<String, Object> body = JsonUtil.fromJson(request, Map.class);
        Integer itemId = ((Number) body.get("itemId")).intValue();
        Integer points = ((Number) body.get("points")).intValue();

        User user = userDao.findById(userId);
        if (user == null) {
            JsonUtil.writeError(response, 404, "用户不存在");
            return;
        }

        if (user.getPoints() < points) {
            JsonUtil.writeError(response, 400, "积分不足");
            return;
        }

        // Deduct points
        userDao.updatePoints(userId, -points);

        // In production, create the exchanged item (coupon, service, etc.)
        JsonUtil.writeSuccess(response, "兑换成功", null);
    }

    private List<Map<String, Object>> getUserCoupons(Long userId, Integer status) {
        List<Map<String, Object>> coupons = new ArrayList<>();
        String sql = "SELECT uc.*, c.code, c.name, c.description, c.type, c.min_amount,"
            + " c.discount_amount, c.discount_rate, c.max_discount, c.end_time"
            + " FROM car_rental.user_coupons uc"
            + " JOIN car_rental.coupons c ON uc.coupon_id = c.id"
            + " WHERE uc.user_id = ?";
        if (status != null) {
            sql += " AND uc.status = ?";
        }
        sql += " ORDER BY uc.created_at DESC";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            if (status != null) {
                stmt.setInt(2, status);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> coupon = new HashMap<>();
                    coupon.put("id", rs.getLong("id"));
                    coupon.put("couponId", rs.getLong("coupon_id"));
                    coupon.put("code", rs.getString("code"));
                    coupon.put("name", rs.getString("name"));
                    coupon.put("description", rs.getString("description"));
                    coupon.put("type", rs.getInt("type"));
                    coupon.put("minAmount", rs.getBigDecimal("min_amount"));
                    coupon.put("discountAmount", rs.getBigDecimal("discount_amount"));
                    coupon.put("discountRate", rs.getBigDecimal("discount_rate"));
                    coupon.put("maxDiscount", rs.getBigDecimal("max_discount"));
                    coupon.put("endTime", rs.getTimestamp("end_time"));
                    coupon.put("status", rs.getInt("status"));
                    coupons.add(coupon);
                }
            }
        } catch (SQLException e) {
            // Log error
        }
        return coupons;
    }

    private int getUserCouponCount(Long userId, Long couponId) {
        String sql = "SELECT COUNT(*) FROM car_rental.user_coupons WHERE user_id = ? AND coupon_id = ?";
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
            // Log error
        }
        return 0;
    }

    private boolean claimCoupon(Long userId, Long couponId) {
        String sql = "INSERT INTO car_rental.user_coupons (user_id, coupon_id, status, created_at) VALUES (?, ?, 0, NOW())";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, couponId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }
}
