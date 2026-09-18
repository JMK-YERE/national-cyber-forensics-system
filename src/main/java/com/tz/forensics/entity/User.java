package com.tz.forensics.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name", length = 100)
    private String fullName;

    private String phone;

    @Column(name = "whatsapp_number", length = 30)
    private String whatsappNumber;

    @Column(name = "whatsapp_api_key", length = 100)
    private String whatsappApiKey;

    private String organization;

    @Column(length = 30)
    private String role = "REPORTER";

    @Column(name = "notify_email")
    private Boolean notifyEmail = true;

    @Column(name = "notify_whatsapp")
    private Boolean notifyWhatsapp = false;

    @Column(name = "notify_sms")
    private Boolean notifySms = false;

    private Boolean enabled = true;

    @Column(name = "account_locked")
    private Boolean accountLocked = false;

    @Column(name = "failed_attempts")
    private Integer failedAttempts = 0;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public User() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getWhatsappNumber() { return whatsappNumber; }
    public void setWhatsappNumber(String whatsappNumber) { this.whatsappNumber = whatsappNumber; }
    public String getWhatsappApiKey() { return whatsappApiKey; }
    public void setWhatsappApiKey(String whatsappApiKey) { this.whatsappApiKey = whatsappApiKey; }
    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Boolean getNotifyEmail() { return notifyEmail; }
    public void setNotifyEmail(Boolean notifyEmail) { this.notifyEmail = notifyEmail; }
    public Boolean getNotifyWhatsapp() { return notifyWhatsapp; }
    public void setNotifyWhatsapp(Boolean notifyWhatsapp) { this.notifyWhatsapp = notifyWhatsapp; }
    public Boolean getNotifySms() { return notifySms; }
    public void setNotifySms(Boolean notifySms) { this.notifySms = notifySms; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Boolean getAccountLocked() { return accountLocked; }
    public void setAccountLocked(Boolean accountLocked) { this.accountLocked = accountLocked; }
    public Integer getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(Integer failedAttempts) { this.failedAttempts = failedAttempts; }
    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
