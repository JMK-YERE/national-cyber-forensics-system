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
        if (phone != null && !phone.isBlank() && otp != null && !otp.isBlank()) {
            user = userRepository.findAll().stream()
                    .filter(u -> phone.equals(u.getPhone()) && otp.equals(u.getPasswordSetupToken()))
                    .findFirst().orElse(null);
        } else if (token != null && !token.isBlank()) {
            user = userRepository.findAll().stream()
                    .filter(u -> token.equals(u.getPasswordSetupToken()))
                    .findFirst().orElse(null);
        }

        if (user == null || user.getPasswordSetupExpiresAt() == null ||
                user.getPasswordSetupExpiresAt().isBefore(LocalDateTime.now())) {
            model.addAttribute("error", "Link/OTP si sahihi au ime-expire.");
            model.addAttribute("token", token);
            model.addAttribute("phone", phone);
            model.addAttribute("smsMode", phone != null && !phone.isBlank());
            return "set-password";
        }

        user.setPassword(passwordEncoder.encode(password));
        user.setEmailVerified(true);
        user.setApprovalStatus("PENDING");
        user.setPasswordSetupToken(null);
        user.setPasswordSetupExpiresAt(null);
        user.setPasswordSetupChannel(null);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        userRepository.save(user);

        return "redirect:/login?registered=true";
    }
}
