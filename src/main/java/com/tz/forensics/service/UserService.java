package com.tz.forensics.service;

import com.tz.forensics.dto.UserRegistrationDto;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.security.SecureRandom;
import java.security.MessageDigest;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final WhatsAppService whatsAppService;
    private final NotificationService notificationService;
    private final SmsService smsService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService,
                       WhatsAppService whatsAppService,
                       NotificationService notificationService,
                       SmsService smsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.whatsAppService = whatsAppService;
        this.notificationService = notificationService;
        this.smsService = smsService;
    }

    public String registerUser(UserRegistrationDto dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            return "Username already exists!";
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            return "Email already exists!";
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        // Password is intentionally created after registration through a one-time email link or SMS OTP.
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setFullName(dto.getFullName());
        user.setOrganization(dto.getOrganization());
        user.setPhone(dto.getPhone());
        user.setWhatsappNumber(dto.getWhatsappNumber());
        user.setRole("INDIVIDUAL");
        user.setEnabled(true);
        user.setApprovalStatus("PENDING");
        user.setEmailVerified(false);
        String channel = "SMS".equalsIgnoreCase(dto.getVerificationChannel()) ? "SMS" : "EMAIL";
        String setupToken = "SMS".equals(channel)
                ? String.valueOf(100000 + new SecureRandom().nextInt(900000))
                : UUID.randomUUID().toString();
        user.setPasswordSetupToken(sha256(setupToken));
        user.setPasswordSetupAttempts(0);
        user.setPasswordSetupExpiresAt(LocalDateTime.now().plusMinutes("SMS".equals(channel) ? 10 : 60 * 24));
        user.setPasswordSetupChannel(channel);
        user.setVerificationToken(sha256(setupToken));
        user.setVerificationTokenExpiresAt(user.getPasswordSetupExpiresAt());

        userRepository.save(user);

        try {
            if ("EMAIL".equals(channel)) {
                emailService.sendPasswordSetupEmail(dto.getEmail(), dto.getUsername(), setupToken);
            }
        } catch (Exception e) {
            System.err.println("❌ Verification email failed: " + e.getMessage());
        }

        boolean smsSent = false;
        if ("SMS".equals(channel)) {
            smsSent = smsService.sendSms(dto.getPhone(), "Cyber Forensics TZ: OTP yako ya kutengeneza password ni " + setupToken + ". Inaisha ndani ya dakika 10.");
        }

        // ===== 1. EMAIL KWA MTU MWENYEWE (BURE) =====
        try {
            emailService.sendWelcomeEmail(dto.getEmail(), dto.getUsername());
            System.out.println("✅ Welcome email sent to: " + dto.getEmail());
        } catch (Exception e) {
            System.err.println("❌ Welcome email failed: " + e.getMessage());
        }

        // ===== 2. NOTIFY ADMIN =====
        try {
            emailService.sendAdminNewUserAlert(dto.getUsername(), dto.getEmail());
        } catch (Exception e) {
            System.err.println("❌ Admin alert failed: " + e.getMessage());
        }

        // ===== 3. IN-APP NOTIFICATION =====
        try {
            notificationService.createNotification(
                "👤 Mtumiaji Mpya: " + dto.getUsername(),
                dto.getFullName() + " (" + dto.getEmail() + ") amejiunga",
                "INFO",
                "/audit"
            );
        } catch (Exception e) {
            System.err.println("❌ Notification failed: " + e.getMessage());
        }

        return "SMS".equals(channel) ? (smsSent ? "SMS_SENT" : "SMS_NOT_SENT") : "SUCCESS";
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(64);
            for (byte b : digest) out.append(String.format("%02x", b));
            return out.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
