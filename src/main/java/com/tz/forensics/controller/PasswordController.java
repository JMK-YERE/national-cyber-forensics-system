package com.tz.forensics.controller;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.EmailService;
import com.tz.forensics.service.SmsService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.UUID;
import java.security.SecureRandom;

@Controller
public class PasswordController {
    private static final long RESET_OTP_RESEND_COOLDOWN_MS = 30_000L;
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
    public String forgotSubmit(@RequestParam String email, HttpSession session, RedirectAttributes ra) {
        User user = userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);

        Object lastSent = session.getAttribute("PASSWORD_RESET_OTP_SENT_AT");
        Object previousUser = session.getAttribute("PASSWORD_RESET_USER_ID");
        if (user != null && lastSent instanceof Long && previousUser instanceof Long
                && user.getId().equals((Long) previousUser)
                && System.currentTimeMillis() - (Long) lastSent < RESET_OTP_RESEND_COOLDOWN_MS) {
            ra.addFlashAttribute("sent", true);
            ra.addFlashAttribute("error", "Subiri sekunde chache kabla ya kuomba OTP nyingine.");
            return "redirect:/reset-password?emailOtp=true";
        }

        // Enforce the OTP cooldown from the account state as well as the browser session.
        // This prevents attackers from bypassing the resend limit by creating new sessions.
        if (user != null && user.getPasswordResetToken() != null
                && user.getPasswordResetExpiresAt() != null
                && user.getPasswordResetExpiresAt().isAfter(LocalDateTime.now())) {
            ra.addFlashAttribute("sent", true);
            return "redirect:/reset-password?emailOtp=true";
        }

        // Do not reveal whether an account exists.
        if (user != null && user.getEmail() != null && !user.getEmail().isBlank()) {
            String otp = String.valueOf(100000 + new SecureRandom().nextInt(900000));
            user.setPasswordResetToken(passwordEncoder.encode(otp));
            user.setPasswordResetExpiresAt(LocalDateTime.now().plusSeconds(60));
            user.setPasswordResetAttempts(0);
            userRepository.save(user);
            session.setAttribute("PASSWORD_RESET_USER_ID", user.getId());
            session.setAttribute("PASSWORD_RESET_MODE", "EMAIL");
            session.setAttribute("PASSWORD_RESET_OTP_SENT_AT", System.currentTimeMillis());

            boolean sent = emailService.sendPasswordResetOtp(user.getEmail(), user.getUsername(), otp);
            if (!sent) {
                user.setPasswordResetToken(null);
                user.setPasswordResetExpiresAt(null);
                user.setPasswordResetAttempts(0);
                userRepository.save(user);
                session.removeAttribute("PASSWORD_RESET_USER_ID");
                session.removeAttribute("PASSWORD_RESET_MODE");
                session.removeAttribute("PASSWORD_RESET_OTP_SENT_AT");
                ra.addFlashAttribute("error", "Email ya uthibitisho haikutumwa. Tafadhali jaribu tena.");
                return "redirect:/forgot-password";
            }
        }

        ra.addFlashAttribute("sent", true);
        return "redirect:/reset-password?emailOtp=true";
    }

    @PostMapping("/forgot-password/sms")
    public String forgotSms(@RequestParam String phone, HttpSession session, RedirectAttributes ra) {
        String cleanPhone = phone == null ? "" : phone.trim();
        User user = userRepository.findByPhone(cleanPhone).orElse(null);

        Object lastSent = session.getAttribute("PASSWORD_RESET_OTP_SENT_AT");
        Object previousUser = session.getAttribute("PASSWORD_RESET_USER_ID");
        if (user != null && lastSent instanceof Long && previousUser instanceof Long
                && user.getId().equals((Long) previousUser)
                && System.currentTimeMillis() - (Long) lastSent < RESET_OTP_RESEND_COOLDOWN_MS) {
            ra.addFlashAttribute("sent", true);
            ra.addFlashAttribute("error", "Subiri sekunde chache kabla ya kuomba OTP nyingine.");
            return "redirect:/reset-password?sms=true";
        }

        // Enforce the OTP cooldown from the account state as well as the browser session.
        if (user != null && user.getPasswordResetToken() != null
                && user.getPasswordResetExpiresAt() != null
                && user.getPasswordResetExpiresAt().isAfter(LocalDateTime.now())) {
            ra.addFlashAttribute("sent", true);
            return "redirect:/reset-password?sms=true";
        }

        // Do not reveal whether a phone number belongs to an account.
        if (user != null && Boolean.TRUE.equals(user.getEnabled()) && user.isApproved() && user.isEmailVerified()) {
            String otp = String.valueOf(100000 + new SecureRandom().nextInt(900000));
            user.setPasswordResetToken(passwordEncoder.encode(otp));
            user.setPasswordResetExpiresAt(LocalDateTime.now().plusSeconds(60));
            user.setPasswordResetAttempts(0);
            userRepository.save(user);

            boolean sent = smsService.sendSms(user.getPhone(),
                    "Cyber Forensics TZ: OTP ya reset password ni " + otp + ". Inaisha ndani ya sekunde 60.");
            if (sent) {
                session.setAttribute("PASSWORD_RESET_USER_ID", user.getId());
                session.setAttribute("PASSWORD_RESET_MODE", "SMS");
                session.setAttribute("PASSWORD_RESET_OTP_SENT_AT", System.currentTimeMillis());
            } else {
                user.setPasswordResetToken(null);
                user.setPasswordResetExpiresAt(null);
                user.setPasswordResetAttempts(0);
                userRepository.save(user);
            }
        }
        ra.addFlashAttribute("sent", true);
        return "redirect:/reset-password?sms=true";
    }

    @GetMapping("/reset-password")
    public String resetForm(@RequestParam(required = false) String token,
                            @RequestParam(required = false) String sms,
                            @RequestParam(required = false) String emailOtp,
                            @RequestParam(required = false) String sent,
                            Model model) {
        model.addAttribute("token", token == null ? "" : token);
        model.addAttribute("smsMode", sms != null);
        model.addAttribute("emailOtp", emailOtp != null);
        model.addAttribute("sent", sent != null);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetSubmit(@RequestParam(required = false, defaultValue = "") String token,
                              @RequestParam String password,
                              @RequestParam String confirmPassword,
                              Model model,
                              HttpServletRequest request,
                              HttpServletResponse response) {
        String cleanToken = token.trim();
        HttpSession session = request.getSession(false);
        Long resetUserId = session == null ? null : (Long) session.getAttribute("PASSWORD_RESET_USER_ID");
        User user = resetUserId == null ? null : userRepository.findById(resetUserId).orElse(null);

        if (user == null || !Boolean.TRUE.equals(user.getEnabled()) || !user.isApproved() || !user.isEmailVerified() ||
                user.getPasswordResetToken() == null ||
                user.getPasswordResetExpiresAt() == null ||
                user.getPasswordResetExpiresAt().isBefore(LocalDateTime.now())) {
            model.addAttribute("error", "OTP si sahihi, ime-expire, au session ya reset haipo. Omba OTP mpya.");
            model.addAttribute("emailOtp", true);
            return "reset-password";
        }

        if (user.getPasswordResetAttempts() >= 5) {
            clearPasswordResetOtp(user);
            model.addAttribute("error", "Umefikia kikomo cha majaribio. Omba OTP mpya.");
            model.addAttribute("emailOtp", true);
            return "reset-password";
        }

        // Count every OTP verification attempt, including incorrect OTPs.
        // The previous flow incremented only after a successful token match,
        // which allowed unlimited guesses during the 60-second validity window.
        String normalizedOtp = cleanToken;
        if (!normalizedOtp.matches("\\d{6}")) {
            user.setPasswordResetAttempts(user.getPasswordResetAttempts() + 1);
            if (user.getPasswordResetAttempts() >= 5) {
                clearPasswordResetOtp(user);
            } else {
                userRepository.save(user);
            }
            model.addAttribute("error", "OTP lazima iwe na tarakimu 6.");
            model.addAttribute("emailOtp", true);
            return "reset-password";
        }

        user.setPasswordResetAttempts(user.getPasswordResetAttempts() + 1);
        boolean otpValid = passwordEncoder.matches(normalizedOtp, user.getPasswordResetToken());
        if (!otpValid) {
            if (user.getPasswordResetAttempts() >= 5) {
                clearPasswordResetOtp(user);
                model.addAttribute("error", "OTP si sahihi. Umefikia kikomo cha majaribio; omba OTP mpya.");
            } else {
                userRepository.save(user);
                model.addAttribute("error", "OTP si sahihi.");
            }
            model.addAttribute("emailOtp", true);
            return "reset-password";
        }

        if (password.length() < 6 || !password.equals(confirmPassword)) {
            userRepository.save(user);
            model.addAttribute("error", "Password lazima iwe na angalau herufi 6 na zifanane.");
            model.addAttribute("emailOtp", true);
            return "reset-password";
        }

        user.setPassword(passwordEncoder.encode(password));
        request.changeSessionId();
        user.setFailedAttempts(0);
        user.setAccountLocked(false);
        user.setLastLogin(LocalDateTime.now());
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        user.setPasswordResetAttempts(0);
        userRepository.save(user);
        if (session != null) {
            session.removeAttribute("PASSWORD_RESET_USER_ID");
            session.removeAttribute("PASSWORD_RESET_MODE");
            session.removeAttribute("PASSWORD_RESET_OTP_SENT_AT");
        }

        // The password-reset flow is already a verified identity flow.
        // Establish a normal authenticated session so the user goes straight
        // to the role-aware dashboard instead of being forced through login again.
        String role = user.getRole() == null || user.getRole().isBlank()
                ? "INDIVIDUAL" : user.getRole().trim().toUpperCase();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getUsername(),
                        null,
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_" + role)));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);

        return "redirect:/dashboard";
    }

    private void clearPasswordResetOtp(User user) {
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        user.setPasswordResetAttempts(0);
        userRepository.save(user);
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
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            ra.addFlashAttribute("error", "Password mpya lazima iwe tofauti na password ya sasa.");
            return "redirect:/change-password";
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        ra.addFlashAttribute("success", "✅ Neno la siri limebadilishwa!");
        return "redirect:/change-password";
    }
}
