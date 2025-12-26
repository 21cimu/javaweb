package com.carrental.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * After-sales order entity for refund/repair/complaint etc.
 */
public class AfterSalesOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String afterNo;
    private Long orderId;
    private String orderNo;
    private Long userId;
    private String userName;
    private String userPhone;
    /** 1-repair,2-refund,3-complaint,4-other */
    private Integer type;
    private String reasonCode;
    private String description;
    private String expectedSolution;
    private BigDecimal refundAmount;
    private BigDecimal approvedRefundAmount;
    private String evidenceImages;
    /** 0-closed,1-pending,2-approved,3-rejected,4-finished,5-refunding */
    private Integer status;

    private Long auditAdminId;
    private String auditAdminName;
    private LocalDateTime auditTime;
    private String auditRemark;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AfterSalesOrder() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAfterNo() { return afterNo; }
    public void setAfterNo(String afterNo) { this.afterNo = afterNo; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }

    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }

    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getExpectedSolution() { return expectedSolution; }
    public void setExpectedSolution(String expectedSolution) { this.expectedSolution = expectedSolution; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }

    public BigDecimal getApprovedRefundAmount() { return approvedRefundAmount; }
    public void setApprovedRefundAmount(BigDecimal approvedRefundAmount) { this.approvedRefundAmount = approvedRefundAmount; }

    public String getEvidenceImages() { return evidenceImages; }
    public void setEvidenceImages(String evidenceImages) { this.evidenceImages = evidenceImages; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Long getAuditAdminId() { return auditAdminId; }
    public void setAuditAdminId(Long auditAdminId) { this.auditAdminId = auditAdminId; }

    public String getAuditAdminName() { return auditAdminName; }
    public void setAuditAdminName(String auditAdminName) { this.auditAdminName = auditAdminName; }

    public LocalDateTime getAuditTime() { return auditTime; }
    public void setAuditTime(LocalDateTime auditTime) { this.auditTime = auditTime; }

    public String getAuditRemark() { return auditRemark; }
    public void setAuditRemark(String auditRemark) { this.auditRemark = auditRemark; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

