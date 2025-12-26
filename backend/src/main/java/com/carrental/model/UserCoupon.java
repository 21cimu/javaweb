package com.carrental.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * UserCoupon entity for user-owned coupons
 */
public class UserCoupon implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private Long couponId;
    private Long orderId;
    private Integer status; // 0-未使用, 1-已使用, 2-已过期
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;
    
    // Coupon info for display
    private String couponName;
    private String couponCode;
    private LocalDateTime couponEndTime;

    // Constructors
    public UserCoupon() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getCouponId() { return couponId; }
    public void setCouponId(Long couponId) { this.couponId = couponId; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCouponName() { return couponName; }
    public void setCouponName(String couponName) { this.couponName = couponName; }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }

    public LocalDateTime getCouponEndTime() { return couponEndTime; }
    public void setCouponEndTime(LocalDateTime couponEndTime) { this.couponEndTime = couponEndTime; }
}
