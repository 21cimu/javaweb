package com.carrental.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * User entity representing a car rental customer
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String password;
    private String email;
    private String phone;
    private String realName;
    private String idCard;
    private String driverLicense;
    private String driverLicenseImage;
    private String idCardFront;
    private String idCardBack;
    private Integer verificationStatus; // 0-未认证, 1-待审核, 2-已认证, 3-认证失败
    private String avatar;
    private Integer gender; // 0-未知, 1-男, 2-女
    private LocalDateTime birthday;
    private String role; // user, admin, superadmin
    private Integer status; // 0-禁用, 1-正常
    private BigDecimal balance;
    private Integer points;
    private String inviteCode;
    private Long inviterId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public User() {}

    public User(String username, String password, String phone) {
        this.username = username;
        this.password = password;
        this.phone = phone;
        this.role = "user";
        this.status = 1;
        this.verificationStatus = 0;
        this.balance = BigDecimal.ZERO;
        this.points = 0;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }

    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }

    public String getDriverLicense() { return driverLicense; }
    public void setDriverLicense(String driverLicense) { this.driverLicense = driverLicense; }

    public String getDriverLicenseImage() { return driverLicenseImage; }
    public void setDriverLicenseImage(String driverLicenseImage) { this.driverLicenseImage = driverLicenseImage; }

    public String getIdCardFront() { return idCardFront; }
    public void setIdCardFront(String idCardFront) { this.idCardFront = idCardFront; }

    public String getIdCardBack() { return idCardBack; }
    public void setIdCardBack(String idCardBack) { this.idCardBack = idCardBack; }

    public Integer getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(Integer verificationStatus) { this.verificationStatus = verificationStatus; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }

    public LocalDateTime getBirthday() { return birthday; }
    public void setBirthday(LocalDateTime birthday) { this.birthday = birthday; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }

    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }

    public Long getInviterId() { return inviterId; }
    public void setInviterId(Long inviterId) { this.inviterId = inviterId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
