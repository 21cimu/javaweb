package com.carrental.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Coupon entity representing discount coupons
 */
public class Coupon implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String code;
    private String name;
    private String description;
    private Integer type; // 1-满减, 2-折扣, 3-固定金额
    private BigDecimal minAmount; // 最低消费金额
    private BigDecimal discountAmount; // 减免金额(满减/固定)
    private BigDecimal discountRate; // 折扣率(0.1-1.0)
    private BigDecimal maxDiscount; // 最大折扣金额
    private Integer totalCount; // 发放总量
    private Integer usedCount; // 已使用数量
    private Integer perUserLimit; // 每人限领数量
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String applicableCategories; // 适用车型分类
    private String applicableVehicles; // 适用车辆ID
    private Integer status; // 0-禁用, 1-启用
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // VIP requirement: 0-none,1-gold,2-diamond
    private Integer vipLevelRequired;

    // Constructors
    public Coupon() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }

    public BigDecimal getMinAmount() { return minAmount; }
    public void setMinAmount(BigDecimal minAmount) { this.minAmount = minAmount; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public BigDecimal getDiscountRate() { return discountRate; }
    public void setDiscountRate(BigDecimal discountRate) { this.discountRate = discountRate; }

    public BigDecimal getMaxDiscount() { return maxDiscount; }
    public void setMaxDiscount(BigDecimal maxDiscount) { this.maxDiscount = maxDiscount; }

    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }

    public Integer getUsedCount() { return usedCount; }
    public void setUsedCount(Integer usedCount) { this.usedCount = usedCount; }

    public Integer getPerUserLimit() { return perUserLimit; }
    public void setPerUserLimit(Integer perUserLimit) { this.perUserLimit = perUserLimit; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getApplicableCategories() { return applicableCategories; }
    public void setApplicableCategories(String applicableCategories) { this.applicableCategories = applicableCategories; }

    public String getApplicableVehicles() { return applicableVehicles; }
    public void setApplicableVehicles(String applicableVehicles) { this.applicableVehicles = applicableVehicles; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getVipLevelRequired() { return vipLevelRequired; }
    public void setVipLevelRequired(Integer vipLevelRequired) { this.vipLevelRequired = vipLevelRequired; }
}
