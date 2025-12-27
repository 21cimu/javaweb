package com.carrental.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order entity representing a car rental order
 */
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String orderNo;
    private Long userId;
    private String userName;
    private String userPhone;
    private Long vehicleId;
    private String vehicleName;
    private String vehiclePlate;
    private Long pickupStoreId;
    private String pickupStoreName;
    private Long returnStoreId;
    private String returnStoreName;
    private String deliveryAddress;
    private String deliveryCity;
    private String deliveryDistrict;
    private BigDecimal deliveryLng;
    private BigDecimal deliveryLat;
    private LocalDateTime pickupTime;
    private LocalDateTime returnTime;
    private LocalDateTime actualPickupTime;
    private LocalDateTime actualReturnTime;
    private Integer rentalDays;
    private BigDecimal dailyPrice;
    private BigDecimal rentalAmount;
    private BigDecimal deposit;
    private BigDecimal insuranceAmount;
    private BigDecimal serviceAmount;
    private BigDecimal extraAmount; // 超时、超里程等额外费用
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal refundAmount;
    private Long couponId;
    private String couponCode;
    
    /**
     * 订单状态:
     * 0-已取消, 1-待审核, 2-审核失败, 3-待支付, 4-待取车, 
     * 5-用车中, 6-待还车, 7-待结算, 8-已完成, 9-退款中, 10-已退款
     */
    private Integer status;
    private String pickupCode;
    private Integer pickupMileage;
    private Integer returnMileage;
    private Integer pickupFuel;
    private Integer returnFuel;
    private String pickupImages;
    private String returnImages;
    private String pickupNote;
    private String returnNote;
    private String contractUrl;
    private Integer paymentMethod; // 1-微信, 2-支付宝, 3-银行卡, 4-余额
    private String paymentNo;
    private LocalDateTime paymentTime;
    private String insuranceType;
    private String addServices; // JSON array of additional services
    private String cancelReason;
    private LocalDateTime cancelTime;
    private Integer rating;
    private String review;
    private String reviewImages;
    private LocalDateTime reviewTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Whether this order's spending has been accounted into user's cumulative spending
    private Integer spendingAccounted; // 0-not accounted, 1-accounted

    // Latest after-sales summary (optional, not persisted)
    private Long afterSalesId;
    private Integer afterSalesType;
    private Integer afterSalesStatus;
    private BigDecimal afterSalesRefundAmount;
    private BigDecimal afterSalesApprovedRefundAmount;
    private String afterSalesAuditRemark;
    private LocalDateTime afterSalesAuditTime;

    // Constructors
    public Order() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }

    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }

    public String getVehicleName() { return vehicleName; }
    public void setVehicleName(String vehicleName) { this.vehicleName = vehicleName; }

    public String getVehiclePlate() { return vehiclePlate; }
    public void setVehiclePlate(String vehiclePlate) { this.vehiclePlate = vehiclePlate; }

    public Long getPickupStoreId() { return pickupStoreId; }
    public void setPickupStoreId(Long pickupStoreId) { this.pickupStoreId = pickupStoreId; }

    public String getPickupStoreName() { return pickupStoreName; }
    public void setPickupStoreName(String pickupStoreName) { this.pickupStoreName = pickupStoreName; }

    public Long getReturnStoreId() { return returnStoreId; }
    public void setReturnStoreId(Long returnStoreId) { this.returnStoreId = returnStoreId; }

    public String getReturnStoreName() { return returnStoreName; }
    public void setReturnStoreName(String returnStoreName) { this.returnStoreName = returnStoreName; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getDeliveryCity() { return deliveryCity; }
    public void setDeliveryCity(String deliveryCity) { this.deliveryCity = deliveryCity; }

    public String getDeliveryDistrict() { return deliveryDistrict; }
    public void setDeliveryDistrict(String deliveryDistrict) { this.deliveryDistrict = deliveryDistrict; }

    public BigDecimal getDeliveryLng() { return deliveryLng; }
    public void setDeliveryLng(BigDecimal deliveryLng) { this.deliveryLng = deliveryLng; }

    public BigDecimal getDeliveryLat() { return deliveryLat; }
    public void setDeliveryLat(BigDecimal deliveryLat) { this.deliveryLat = deliveryLat; }

    public LocalDateTime getPickupTime() { return pickupTime; }
    public void setPickupTime(LocalDateTime pickupTime) { this.pickupTime = pickupTime; }

    public LocalDateTime getReturnTime() { return returnTime; }
    public void setReturnTime(LocalDateTime returnTime) { this.returnTime = returnTime; }

    public LocalDateTime getActualPickupTime() { return actualPickupTime; }
    public void setActualPickupTime(LocalDateTime actualPickupTime) { this.actualPickupTime = actualPickupTime; }

    public LocalDateTime getActualReturnTime() { return actualReturnTime; }
    public void setActualReturnTime(LocalDateTime actualReturnTime) { this.actualReturnTime = actualReturnTime; }

    public Integer getRentalDays() { return rentalDays; }
    public void setRentalDays(Integer rentalDays) { this.rentalDays = rentalDays; }

    public BigDecimal getDailyPrice() { return dailyPrice; }
    public void setDailyPrice(BigDecimal dailyPrice) { this.dailyPrice = dailyPrice; }

    public BigDecimal getRentalAmount() { return rentalAmount; }
    public void setRentalAmount(BigDecimal rentalAmount) { this.rentalAmount = rentalAmount; }

    public BigDecimal getDeposit() { return deposit; }
    public void setDeposit(BigDecimal deposit) { this.deposit = deposit; }

    public BigDecimal getInsuranceAmount() { return insuranceAmount; }
    public void setInsuranceAmount(BigDecimal insuranceAmount) { this.insuranceAmount = insuranceAmount; }

    public BigDecimal getServiceAmount() { return serviceAmount; }
    public void setServiceAmount(BigDecimal serviceAmount) { this.serviceAmount = serviceAmount; }

    public BigDecimal getExtraAmount() { return extraAmount; }
    public void setExtraAmount(BigDecimal extraAmount) { this.extraAmount = extraAmount; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }

    public Long getCouponId() { return couponId; }
    public void setCouponId(Long couponId) { this.couponId = couponId; }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getPickupCode() { return pickupCode; }
    public void setPickupCode(String pickupCode) { this.pickupCode = pickupCode; }

    public Integer getPickupMileage() { return pickupMileage; }
    public void setPickupMileage(Integer pickupMileage) { this.pickupMileage = pickupMileage; }

    public Integer getReturnMileage() { return returnMileage; }
    public void setReturnMileage(Integer returnMileage) { this.returnMileage = returnMileage; }

    public Integer getPickupFuel() { return pickupFuel; }
    public void setPickupFuel(Integer pickupFuel) { this.pickupFuel = pickupFuel; }

    public Integer getReturnFuel() { return returnFuel; }
    public void setReturnFuel(Integer returnFuel) { this.returnFuel = returnFuel; }

    public String getPickupImages() { return pickupImages; }
    public void setPickupImages(String pickupImages) { this.pickupImages = pickupImages; }

    public String getReturnImages() { return returnImages; }
    public void setReturnImages(String returnImages) { this.returnImages = returnImages; }

    public String getPickupNote() { return pickupNote; }
    public void setPickupNote(String pickupNote) { this.pickupNote = pickupNote; }

    public String getReturnNote() { return returnNote; }
    public void setReturnNote(String returnNote) { this.returnNote = returnNote; }

    public String getContractUrl() { return contractUrl; }
    public void setContractUrl(String contractUrl) { this.contractUrl = contractUrl; }

    public Integer getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(Integer paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentNo() { return paymentNo; }
    public void setPaymentNo(String paymentNo) { this.paymentNo = paymentNo; }

    public LocalDateTime getPaymentTime() { return paymentTime; }
    public void setPaymentTime(LocalDateTime paymentTime) { this.paymentTime = paymentTime; }

    public String getInsuranceType() { return insuranceType; }
    public void setInsuranceType(String insuranceType) { this.insuranceType = insuranceType; }

    public String getAddServices() { return addServices; }
    public void setAddServices(String addServices) { this.addServices = addServices; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    public LocalDateTime getCancelTime() { return cancelTime; }
    public void setCancelTime(LocalDateTime cancelTime) { this.cancelTime = cancelTime; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getReview() { return review; }
    public void setReview(String review) { this.review = review; }

    public String getReviewImages() { return reviewImages; }
    public void setReviewImages(String reviewImages) { this.reviewImages = reviewImages; }

    public LocalDateTime getReviewTime() { return reviewTime; }
    public void setReviewTime(LocalDateTime reviewTime) { this.reviewTime = reviewTime; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getSpendingAccounted() { return spendingAccounted; }
    public void setSpendingAccounted(Integer spendingAccounted) { this.spendingAccounted = spendingAccounted; }

    public Long getAfterSalesId() { return afterSalesId; }
    public void setAfterSalesId(Long afterSalesId) { this.afterSalesId = afterSalesId; }

    public Integer getAfterSalesType() { return afterSalesType; }
    public void setAfterSalesType(Integer afterSalesType) { this.afterSalesType = afterSalesType; }

    public Integer getAfterSalesStatus() { return afterSalesStatus; }
    public void setAfterSalesStatus(Integer afterSalesStatus) { this.afterSalesStatus = afterSalesStatus; }

    public BigDecimal getAfterSalesRefundAmount() { return afterSalesRefundAmount; }
    public void setAfterSalesRefundAmount(BigDecimal afterSalesRefundAmount) { this.afterSalesRefundAmount = afterSalesRefundAmount; }

    public BigDecimal getAfterSalesApprovedRefundAmount() { return afterSalesApprovedRefundAmount; }
    public void setAfterSalesApprovedRefundAmount(BigDecimal afterSalesApprovedRefundAmount) { this.afterSalesApprovedRefundAmount = afterSalesApprovedRefundAmount; }

    public String getAfterSalesAuditRemark() { return afterSalesAuditRemark; }
    public void setAfterSalesAuditRemark(String afterSalesAuditRemark) { this.afterSalesAuditRemark = afterSalesAuditRemark; }

    public LocalDateTime getAfterSalesAuditTime() { return afterSalesAuditTime; }
    public void setAfterSalesAuditTime(LocalDateTime afterSalesAuditTime) { this.afterSalesAuditTime = afterSalesAuditTime; }

}
