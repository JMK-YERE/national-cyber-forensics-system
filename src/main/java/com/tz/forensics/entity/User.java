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
    private String role = "INDIVIDUAL";

    // ===== SOCIAL MEDIA ACCOUNTS =====
    @Column(name = "has_facebook")
    private Boolean hasFacebook = false;

    @Column(name = "has_instagram")
    private Boolean hasInstagram = false;

    @Column(name = "has_tiktok")
    private Boolean hasTiktok = false;

    @Column(name = "has_whatsapp")
    private Boolean hasWhatsapp = false;

    @Column(name = "has_gmail")
    private Boolean hasGmail = false;

    @Column(name = "has_x")
    private Boolean hasX = false;

    // ===== SECURITY SCORE =====
    @Column(name = "security_score")
    private Integer securityScore = 0;

    @Column(name = "has_2fa")
    private Boolean has2fa = false;

    @Column(name = "breach_checked")
    private Boolean breachChecked = false;

    // ===== NOTIFICATIONS =====
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

    // ===== HELPER METHODS =====
    public boolean isAdmin() { return "ADMIN".equals(role); }
    public boolean isProfessional() { return "CYBER_PRO".equals(role); }
    public boolean isForensics() { return "FORENSICS".equals(role); }
    public boolean isIndividual() { return "INDIVIDUAL".equals(role) || role == null; }

    public int countSocialAccounts() {
        int count = 0;
        if (Boolean.TRUE.equals(hasFacebook)) count++;
        if (Boolean.TRUE.equals(hasInstagram)) count++;
        if (Boolean.TRUE.equals(hasTiktok)) count++;
        if (Boolean.TRUE.equals(hasWhatsapp)) count++;
        if (Boolean.TRUE.equals(hasGmail)) count++;
        if (Boolean.TRUE.equals(hasX)) count++;
        return count;
    }

    // Getters & Setters
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
    public Boolean getHasFacebook() { return hasFacebook; }
    public void setHasFacebook(Boolean hasFacebook) { this.hasFacebook = hasFacebook; }
    public Boolean getHasInstagram() { return hasInstagram; }
    public void setHasInstagram(Boolean hasInstagram) { this.hasInstagram = hasInstagram; }
    public Boolean getHasTiktok() { return hasTiktok; }
    public void setHasTiktok(Boolean hasTiktok) { this.hasTiktok = hasTiktok; }
    public Boolean getHasWhatsapp() { return hasWhatsapp; }
    public void setHasWhatsapp(Boolean hasWhatsapp) { this.hasWhatsapp = hasWhatsapp; }
    public Boolean getHasGmail() { return hasGmail; }
    public void setHasGmail(Boolean hasGmail) { this.hasGmail = hasGmail; }
    public Boolean getHasX() { return hasX; }
    public void setHasX(Boolean hasX) { this.hasX = hasX; }
    public Integer getSecurityScore() { return securityScore; }
    public void setSecurityScore(Integer securityScore) { this.securityScore = securityScore; }
    public Boolean getHas2fa() { return has2fa; }
    public void setHas2fa(Boolean has2fa) { this.has2fa = has2fa; }
    public Boolean getBreachChecked() { return breachChecked; }
    public void setBreachChecked(Boolean breachChecked) { this.breachChecked = breachChecked; }
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
