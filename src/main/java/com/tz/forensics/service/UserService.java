package com.tz.forensics.service;

import com.tz.forensics.dto.UserRegistrationDto;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final WhatsAppService whatsAppService;
    private final NotificationService notificationService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService,
                       WhatsAppService whatsAppService,
                       NotificationService notificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.whatsAppService = whatsAppService;
        this.notificationService = notificationService;
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
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setFullName(dto.getFullName());
        user.setOrganization(dto.getOrganization());
        user.setPhone(dto.getPhone());
        user.setWhatsappNumber(dto.getWhatsappNumber());
        user.setRole("REPORTER");
        user.setEnabled(true);

        userRepository.save(user);

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

        return "SUCCESS";
    }
}
