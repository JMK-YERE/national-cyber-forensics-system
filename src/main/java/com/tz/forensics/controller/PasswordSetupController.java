package com.tz.forensics.controller;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.EmailService;
import com.tz.forensics.service.SmsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.UUID;
import java.security.MessageDigest;

@Controller
public class PasswordSetupController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SmsService smsService;

    public PasswordSetupController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                   EmailService emailService, SmsService smsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.smsService = smsService;
    }

    @GetMapping("/set-password")
    public String setPasswordForm(@RequestParam(required = false) String token,
                                   @RequestParam(required = false) String phone,
                                   Model model) {
        model.addAttribute("token", token == null ? "" : token);
        model.addAttribute("phone", phone == null ? "" : phone);
        model.addAttribute("smsMode", phone != null && !phone.isBlank());
        return "set-password";
    }

    @PostMapping("/set-password")
    public String setPassword(@RequestParam(required = false) String token,
                              @RequestParam(required = false) String phone,
                              @RequestParam(required = false) String otp,
                              @RequestParam String password,
                              @RequestParam String confirmPassword,
                              Model model) {
        if (password.length() < 6 || !password.equals(confirmPassword)) {
            model.addAttribute("error", "Password lazima iwe na angalau herufi 6 na password zote zifanane.");
            model.addAttribute("token", token);
            model.addAttribute("phone", phone);
            model.addAttribute("smsMode", phone != null && !phone.isBlank());
            return "set-password";
        }

        User user = null;
        String supplied = phone != null && !phone.isBlank() ? otp : token;
        String hashed = sha256(supplied == null ? "" : supplied.trim());
        if (phone != null && !phone.isBlank() && otp != null && !otp.isBlank()) {
            user = userRepository.findByPhone(phone.trim()).orElse(null);
            if (user != null && !hashed.equals(user.getPasswordSetupToken())) {
                if (user.getPasswordSetupAttempts() >= 5) {
                    user = null;
                } else {
                    user.setPasswordSetupAttempts(user.getPasswordSetupAttempts() + 1);
                    userRepository.save(user);
                    user = null;
                }
                // Legacy plaintext SMS tokens intentionally do not bypass
                // the attempt counter.
            }
        } else if (token != null && !token.isBlank()) {
            user = userRepository.findByPasswordSetupToken(hashed).orElse(null);
            if (user == null) {
                // Backward-compatible support for pending pre-hardening records.
                user = userRepository.findAll().stream()
                        .filter(u -> token.trim().equals(u.getPasswordSetupToken()))
                        .findFirst().orElse(null);
            }
        }

        if (user != null && user.getPasswordSetupAttempts() >= 5) {
            model.addAttribute("error", "Umefikia kikomo cha majaribio. Omba OTP/link mpya.");
            model.addAttribute("token", "");
            model.addAttribute("phone", phone);
            model.addAttribute("smsMode", phone != null && !phone.isBlank());
            return "set-password";
        }

        if (user == null || user.getPasswordSetupExpiresAt() == null ||
                user.getPasswordSetupExpiresAt().isBefore(LocalDateTime.now())) {
            model.addAttribute("error", "Link/OTP si sahihi au ime-expire.");
            model.addAttribute("token", token);
            model.addAttribute("phone", phone);
            model.addAttribute("smsMode", phone != null && !phone.isBlank());
            return "set-password";
        }

        user.setPasswordSetupAttempts(user.getPasswordSetupAttempts() + 1);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmailVerified(true);
        user.setApprovalStatus("PENDING");
        user.setPasswordSetupToken(null);
        user.setPasswordSetupAttempts(0);
        user.setPasswordSetupExpiresAt(null);
        user.setPasswordSetupChannel(null);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        userRepository.save(user);

        return "redirect:/login?registered=true";
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
