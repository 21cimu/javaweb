package com.carrental.dao;

import com.carrental.model.Order;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Order Data Access Object
 */
public class OrderDao extends BaseDao<Order> {

    @Override
    protected Order mapRow(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setId(getLong(rs, "id"));
        order.setOrderNo(getString(rs, "order_no"));
        order.setUserId(getLong(rs, "user_id"));
        order.setUserName(getString(rs, "user_name"));
        order.setUserPhone(getString(rs, "user_phone"));
        order.setVehicleId(getLong(rs, "vehicle_id"));
        order.setVehicleName(getString(rs, "vehicle_name"));
        order.setVehiclePlate(getString(rs, "vehicle_plate"));
        order.setPickupStoreId(getLong(rs, "pickup_store_id"));
        order.setPickupStoreName(getString(rs, "pickup_store_name"));
        order.setReturnStoreId(getLong(rs, "return_store_id"));
        order.setReturnStoreName(getString(rs, "return_store_name"));
        order.setPickupTime(getLocalDateTime(rs, "pickup_time"));
        order.setReturnTime(getLocalDateTime(rs, "return_time"));
        order.setActualPickupTime(getLocalDateTime(rs, "actual_pickup_time"));
        order.setActualReturnTime(getLocalDateTime(rs, "actual_return_time"));
        order.setRentalDays(getInt(rs, "rental_days"));
        order.setDailyPrice(rs.getBigDecimal("daily_price"));
        order.setRentalAmount(rs.getBigDecimal("rental_amount"));
        order.setDeposit(rs.getBigDecimal("deposit"));
        order.setInsuranceAmount(rs.getBigDecimal("insurance_amount"));
        order.setServiceAmount(rs.getBigDecimal("service_amount"));
        order.setExtraAmount(rs.getBigDecimal("extra_amount"));
        order.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        order.setTotalAmount(rs.getBigDecimal("total_amount"));
        order.setPaidAmount(rs.getBigDecimal("paid_amount"));
        order.setRefundAmount(rs.getBigDecimal("refund_amount"));
        order.setCouponId(getLong(rs, "coupon_id"));
        order.setCouponCode(getString(rs, "coupon_code"));
        order.setStatus(getInt(rs, "status"));
        order.setPickupCode(getString(rs, "pickup_code"));
        order.setPickupMileage(getInt(rs, "pickup_mileage"));
        order.setReturnMileage(getInt(rs, "return_mileage"));
        order.setPickupFuel(getInt(rs, "pickup_fuel"));
        order.setReturnFuel(getInt(rs, "return_fuel"));
        order.setPickupImages(getString(rs, "pickup_images"));
        order.setReturnImages(getString(rs, "return_images"));
        order.setPickupNote(getString(rs, "pickup_note"));
        order.setReturnNote(getString(rs, "return_note"));
        order.setContractUrl(getString(rs, "contract_url"));
        order.setPaymentMethod(getInt(rs, "payment_method"));
        order.setPaymentNo(getString(rs, "payment_no"));
        order.setPaymentTime(getLocalDateTime(rs, "payment_time"));
        order.setInsuranceType(getString(rs, "insurance_type"));
        order.setAddServices(getString(rs, "add_services"));
        order.setCancelReason(getString(rs, "cancel_reason"));
        order.setCancelTime(getLocalDateTime(rs, "cancel_time"));
        order.setRating(getInt(rs, "rating"));
        order.setReview(getString(rs, "review"));
        order.setReviewImages(getString(rs, "review_images"));
        order.setReviewTime(getLocalDateTime(rs, "review_time"));
        order.setCreatedAt(getLocalDateTime(rs, "created_at"));
        order.setUpdatedAt(getLocalDateTime(rs, "updated_at"));
        return order;
    }

    /**
     * Find order by ID
     */
    public Order findById(Long id) {
        String sql = "SELECT * FROM orders WHERE id = ?";
        return executeQuerySingle(sql, id);
    }

    /**
     * Find order by order number
     */
    public Order findByOrderNo(String orderNo) {
        String sql = "SELECT * FROM orders WHERE order_no = ?";
        return executeQuerySingle(sql, orderNo);
    }

    /**
     * Create new order
     */
    public Long create(Order order) {
        String sql = """
            INSERT INTO orders (order_no, user_id, user_name, user_phone, vehicle_id,
                vehicle_name, vehicle_plate, pickup_store_id, pickup_store_name,
                return_store_id, return_store_name, pickup_time, return_time,
                rental_days, daily_price, rental_amount, deposit, insurance_amount,
                service_amount, extra_amount, discount_amount, total_amount,
                paid_amount, refund_amount, coupon_id, coupon_code, status,
                pickup_code, insurance_type, add_services, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
            """;
        return executeInsert(sql, order.getOrderNo(), order.getUserId(),
            order.getUserName(), order.getUserPhone(), order.getVehicleId(),
            order.getVehicleName(), order.getVehiclePlate(), order.getPickupStoreId(),
            order.getPickupStoreName(), order.getReturnStoreId(), order.getReturnStoreName(),
            order.getPickupTime(), order.getReturnTime(), order.getRentalDays(),
            order.getDailyPrice(), order.getRentalAmount(), order.getDeposit(),
            order.getInsuranceAmount(), order.getServiceAmount(), order.getExtraAmount(),
            order.getDiscountAmount(), order.getTotalAmount(), order.getPaidAmount(),
            order.getRefundAmount(), order.getCouponId(), order.getCouponCode(),
            order.getStatus(), order.getPickupCode(), order.getInsuranceType(),
            order.getAddServices());
    }

    /**
     * Update order status
     */
    public int updateStatus(Long id, Integer status) {
        String sql = "UPDATE orders SET status = ?, updated_at = NOW() WHERE id = ?";
        return executeUpdate(sql, status, id);
    }

    /**
     * Update payment info
     */
    public int updatePayment(Long id, Integer paymentMethod, String paymentNo, 
            java.math.BigDecimal paidAmount) {
        String sql = """
            UPDATE orders SET 
                payment_method = ?, payment_no = ?, paid_amount = ?,
                payment_time = NOW(), status = 4, updated_at = NOW()
            WHERE id = ?
            """;
        return executeUpdate(sql, paymentMethod, paymentNo, paidAmount, id);
    }

    /**
     * Update pickup info
     */
    public int updatePickup(Long id, LocalDateTime actualPickupTime, 
            Integer pickupMileage, Integer pickupFuel, String pickupImages, String pickupNote) {
        String sql = """
            UPDATE orders SET 
                actual_pickup_time = ?, pickup_mileage = ?, pickup_fuel = ?,
                pickup_images = ?, pickup_note = ?, status = 5, updated_at = NOW()
            WHERE id = ?
            """;
        return executeUpdate(sql, actualPickupTime, pickupMileage, pickupFuel, 
            pickupImages, pickupNote, id);
    }

    /**
     * Update return info
     */
    public int updateReturn(Long id, LocalDateTime actualReturnTime,
            Integer returnMileage, Integer returnFuel, String returnImages, 
            String returnNote, java.math.BigDecimal extraAmount) {
        String sql = """
            UPDATE orders SET 
                actual_return_time = ?, return_mileage = ?, return_fuel = ?,
                return_images = ?, return_note = ?, extra_amount = ?,
                status = 7, updated_at = NOW()
            WHERE id = ?
            """;
        return executeUpdate(sql, actualReturnTime, returnMileage, returnFuel,
            returnImages, returnNote, extraAmount, id);
    }

    /**
     * Update review
     */
    public int updateReview(Long id, Integer rating, String review, String reviewImages) {
        String sql = """
            UPDATE orders SET 
                rating = ?, review = ?, review_images = ?,
                review_time = NOW(), updated_at = NOW()
            WHERE id = ?
            """;
        return executeUpdate(sql, rating, review, reviewImages, id);
    }

    /**
     * Cancel order
     */
    public int cancel(Long id, String reason) {
        String sql = """
            UPDATE orders SET 
                status = 0, cancel_reason = ?, cancel_time = NOW(), updated_at = NOW()
            WHERE id = ?
            """;
        return executeUpdate(sql, reason, id);
    }

    /**
     * Find orders by user
     */
    public List<Order> findByUser(Long userId, Integer status, int page, int pageSize) {
        String sql;
        if (status != null) {
            sql = """
                SELECT * FROM orders 
                WHERE user_id = ? AND status = ?
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """;
            return executeQuery(sql, userId, status, pageSize, (page - 1) * pageSize);
        } else {
            sql = """
                SELECT * FROM orders 
                WHERE user_id = ?
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """;
            return executeQuery(sql, userId, pageSize, (page - 1) * pageSize);
        }
    }

    /**
     * Count orders by user
     */
    public long countByUser(Long userId, Integer status) {
        if (status != null) {
            String sql = "SELECT COUNT(*) FROM orders WHERE user_id = ? AND status = ?";
            return executeCount(sql, userId, status);
        } else {
            String sql = "SELECT COUNT(*) FROM orders WHERE user_id = ?";
            return executeCount(sql, userId);
        }
    }

    /**
     * Find all orders with filters (admin)
     */
    public List<Order> findAll(Integer status, Long storeId, String orderNo,
            LocalDateTime startDate, LocalDateTime endDate, int page, int pageSize) {
        StringBuilder sql = new StringBuilder("SELECT * FROM orders WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (status != null) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        if (storeId != null) {
            sql.append(" AND (pickup_store_id = ? OR return_store_id = ?)");
            params.add(storeId);
            params.add(storeId);
        }
        if (orderNo != null && !orderNo.isEmpty()) {
            sql.append(" AND order_no LIKE ?");
            params.add("%" + orderNo + "%");
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

    /**
     * Count all orders with filters
     */
    public long countAll(Integer status, Long storeId, String orderNo,
            LocalDateTime startDate, LocalDateTime endDate) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM orders WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (status != null) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        if (storeId != null) {
            sql.append(" AND (pickup_store_id = ? OR return_store_id = ?)");
            params.add(storeId);
            params.add(storeId);
        }
        if (orderNo != null && !orderNo.isEmpty()) {
            sql.append(" AND order_no LIKE ?");
            params.add("%" + orderNo + "%");
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

    /**
     * Get today's order statistics
     */
    public java.util.Map<String, Object> getTodayStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        
        // Today's order count
        String countSql = "SELECT COUNT(*) FROM orders WHERE DATE(created_at) = CURDATE()";
        stats.put("orderCount", executeCount(countSql));
        
        // Today's GMV
        String gmvSql = """
            SELECT COALESCE(SUM(paid_amount), 0) as gmv 
            FROM orders 
            WHERE DATE(created_at) = CURDATE() AND status >= 4
            """;
        // This would need a different query method to get scalar value
        
        return stats;
    }

    /**
     * Get pending review orders count
     */
    public long countPendingReview() {
        String sql = "SELECT COUNT(*) FROM orders WHERE status = 1";
        return executeCount(sql);
    }

    /**
     * Get orders due for return today
     */
    public List<Order> findDueForReturn() {
        String sql = """
            SELECT * FROM orders 
            WHERE status = 5 AND DATE(return_time) = CURDATE()
            ORDER BY return_time ASC
            """;
        return executeQuery(sql);
    }
}
