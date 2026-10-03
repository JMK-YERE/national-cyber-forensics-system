package com.tz.forensics.controller;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.EmailService;
import com.tz.forensics.service.SmsService;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.UUID;

@Controller
public class PasswordController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SmsService smsService;

    public PasswordController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                              EmailService emailService, SmsService smsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.smsService = smsService;
    }

    @GetMapping("/forgot-password")
    public String forgotForm() { return "forgot-password"; }

    @PostMapping("/forgot-password")
    public String forgotSubmit(@RequestParam String email, RedirectAttributes ra) {
        User user = userRepository.findByEmail(email.trim()).orElse(null);
        // Always use the same response to avoid revealing whether an email exists.
        if (user != null) {
            String token = UUID.randomUUID().toString();
            user.setPasswordResetToken(token);
            user.setPasswordResetExpiresAt(LocalDateTime.now().plusHours(1));
            userRepository.save(user);
            emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), token);
        }
        ra.addFlashAttribute("sent", true);
        return "redirect:/forgot-password?sent";
    }

    @PostMapping("/forgot-password/sms")
    public String forgotSms(@RequestParam String phone, RedirectAttributes ra) {
        User user = userRepository.findAll().stream()
                .filter(u -> phone.trim().equals(u.getPhone()))
                .findFirst().orElse(null);
        if (user != null) {
            String otp = String.valueOf(100000 + new java.util.Random().nextInt(900000));
            user.setPasswordResetToken(otp);
            user.setPasswordResetExpiresAt(LocalDateTime.now().plusMinutes(10));
            userRepository.save(user);
            smsService.sendSms(user.getPhone(),
                    "Cyber Forensics TZ: OTP ya reset password ni " + otp + ". Inaisha ndani ya dakika 10.");
        }
        ra.addFlashAttribute("sent", true);
        return "redirect:/forgot-password?sent=sms";
    }

    @GetMapping("/reset-password")
    public String resetForm(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("token", token == null ? "" : token);
        model.addAttribute("smsMode", false);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetSubmit(@RequestParam String token,
                              @RequestParam String password,
                              @RequestParam String confirmPassword,
                              Model model) {
        User user = userRepository.findAll().stream()
                .filter(u -> token.equals(u.getPasswordResetToken()))
                .findFirst().orElse(null);
        if (user == null || user.getPasswordResetExpiresAt() == null ||
                user.getPasswordResetExpiresAt().isBefore(LocalDateTime.now())) {
            model.addAttribute("error", "Reset link/OTP si sahihi au ime-expire.");
            model.addAttribute("token", token);
            return "reset-password";
        }
        if (password.length() < 6 || !password.equals(confirmPassword)) {
            model.addAttribute("error", "Password lazima iwe na angalau herufi 6 na zifanane.");
            model.addAttribute("token", token);
            return "reset-password";
        }
        user.setPassword(passwordEncoder.encode(password));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        userRepository.save(user);
        return "redirect:/login?registered=true";
    }

    @GetMapping("/change-password")
    public String changeForm() { return "change-password"; }

    @PostMapping("/change-password")
    public String changeSubmit(@RequestParam String currentPassword,
                               @RequestParam String newPassword,
                               @RequestParam String confirmPassword,
                               Authentication auth,
                               RedirectAttributes ra) {
        if (auth == null) return "redirect:/login";
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) return "redirect:/login";
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            ra.addFlashAttribute("error", "Neno la siri la sasa si sahihi");
            return "redirect:/change-password";
        }
        if (!newPassword.equals(confirmPassword) || newPassword.length() < 6) {
            ra.addFlashAttribute("error", "Password mpya iwe na herufi 6+ na zote zifanane.");
            return "redirect:/change-password";
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        ra.addFlashAttribute("success", "✅ Neno la siri limebadilishwa!");
        return "redirect:/change-password";
    }
}
