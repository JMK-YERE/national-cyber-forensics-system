package com.tz.forensics.controller;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.security.MessageDigest;

@Controller
public class EmailVerificationController {
    private final UserRepository userRepository;

    public EmailVerificationController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/verify-email")
    public String verify(@RequestParam String token, Model model) {
        String hashedToken = sha256(token == null ? "" : token.trim());
        User user = userRepository.findByVerificationToken(hashedToken).orElse(null);
        if (user == null) {
            // Backward-compatible support for pending pre-hardening records.
            user = userRepository.findAll().stream()
                    .filter(u -> token != null && token.equals(u.getVerificationToken()))
                    .findFirst().orElse(null);
        }

        if (user == null) {
            model.addAttribute("success", false);
            model.addAttribute("message", "Verification link si sahihi au imekwisha.");
            return "verify-email";
        }

        if (user.getVerificationTokenExpiresAt() != null &&
                user.getVerificationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            model.addAttribute("success", false);
            model.addAttribute("message", "Verification link ime-expire. Wasiliana na support.");
            return "verify-email";
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        userRepository.save(user);

        model.addAttribute("success", true);
        model.addAttribute("message", "Email yako imethibitishwa. Sasa unaweza kuingia.");
        return "verify-email";
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