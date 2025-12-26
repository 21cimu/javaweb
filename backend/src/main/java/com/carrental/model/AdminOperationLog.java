package com.carrental.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class AdminOperationLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long operatorId;
    private String operatorName;
    private String operatorRole;
    private String module;
    private String action;
    private String target;
    private String result;
    private String ip;
    private String remark;
    private LocalDateTime createdAt;

    public AdminOperationLog() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }

    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }

    public String getOperatorRole() { return operatorRole; }
    public void setOperatorRole(String operatorRole) { this.operatorRole = operatorRole; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
