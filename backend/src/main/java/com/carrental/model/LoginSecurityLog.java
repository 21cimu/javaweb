package com.carrental.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class LoginSecurityLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String account;
    private String ip;
    private String location;
    private String device;
    private String result;
    private String message;
    private LocalDateTime createdAt;

    public LoginSecurityLog() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDevice() { return device; }
    public void setDevice(String device) { this.device = device; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
