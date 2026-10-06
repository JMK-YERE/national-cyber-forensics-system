package com.tz.forensics.controller;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Second factor for Google sign-in.
 *
 * Flow:
 * Google account selection -> security consent -> email OTP -> dashboard.
 * OTP validity is enforced on the server for exactly 60 seconds and is
 * single-use with a maximum of 5 verification attempts.
 */
@Controller
@RequestMapping("/oauth2/verify")
public class OAuth2VerificationController {

    private static final String USER_ID = "OAUTH_VERIFY_USER_ID";
    private static final String EMAIL = "OAUTH_VERIFY_EMAIL";
    private static final String NAME = "OAUTH_VERIFY_NAME";
    private static final String CHALLENGE_EXPIRES = "OAUTH_VERIFY_EXPIRES";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long RESEND_COOLDOWN_MS = 30_000L;

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public OAuth2VerificationController(UserRepository userRepository,
                                        EmailService emailService,
                                        PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String consent(HttpSession session, Model model) {
        if (!challengeExists(session)) {
            return "redirect:/login";
        }
        model.addAttribute("emailMasked", maskEmail((String) session.getAttribute(EMAIL)));
        model.addAttribute("name", session.getAttribute(NAME));
        model.addAttribute("sent", false);
        model.addAttribute("expired", false);
        return "oauth2-verify";
    }

    @PostMapping("/send")
    public String sendOtp(HttpSession session, Model model) {
        if (!challengeExists(session)) return "redirect:/login";

        Object sentAt = session.getAttribute("OAUTH_OTP_SENT_AT");
        if (sentAt instanceof Long && System.currentTimeMillis() - (Long) sentAt < RESEND_COOLDOWN_MS) {
            model.addAttribute("error", "Subiri sekunde chache kabla ya kuomba OTP nyingine.");
            model.addAttribute("emailMasked", maskEmail((String) session.getAttribute(EMAIL)));
            model.addAttribute("name", session.getAttribute(NAME));
            model.addAttribute("sent", true);
            model.addAttribute("expired", false);
            return "oauth2-verify";
        }

        User user = userRepository.findById((Long) session.getAttribute(USER_ID)).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getEnabled()) || !user.isApproved() || !user.isEmailVerified()) {
            clearChallenge(session);
            return "redirect:/login?error=oauth_account_unavailable";
        }

        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        user.setOauthOtpHash(passwordEncoder.encode(otp));
        user.setOauthOtpExpiresAt(LocalDateTime.now().plusSeconds(60));
        user.setOauthOtpAttempts(0);
        userRepository.save(user);

        if (!emailService.sendOAuthLoginOtp(user.getEmail(), user.getUsername(), otp)) {
            user.setOauthOtpHash(null);
            user.setOauthOtpExpiresAt(null);
            user.setOauthOtpAttempts(0);
            userRepository.save(user);
            model.addAttribute("error", "Email ya uthibitisho haikutumwa. Tafadhali jaribu tena.");
            model.addAttribute("emailMasked", maskEmail(user.getEmail()));
            model.addAttribute("name", user.getFullName());
            model.addAttribute("sent", false);
            model.addAttribute("expired", false);
            return "oauth2-verify";
        }

        session.setAttribute("OAUTH_OTP_SENT_AT", System.currentTimeMillis());
        model.addAttribute("sent", true);
        model.addAttribute("emailMasked", maskEmail(user.getEmail()));
        model.addAttribute("name", user.getFullName());
        return "oauth2-verify";
    }

    @PostMapping("/check")
    public String verifyOtp(@RequestParam String otp, HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        if (!challengeExists(session)) return "redirect:/login";

        User user = userRepository.findById((Long) session.getAttribute(USER_ID)).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())
                || !user.isApproved() || !user.isEmailVerified()) {
            clearChallenge(session);
            return "redirect:/login?error=oauth_account_unavailable";
        }

        if (user.getOauthOtpHash() == null || user.getOauthOtpExpiresAt() == null) {
            model.addAttribute("error", "Omba OTP mpya kwanza.");
            model.addAttribute("emailMasked", maskEmail(user.getEmail()));
            model.addAttribute("name", user.getFullName());
            model.addAttribute("sent", false);
            model.addAttribute("expired", false);
            return "oauth2-verify";
        }

        if (user.getOauthOtpExpiresAt().isBefore(LocalDateTime.now())) {
            clearOtp(user);
            model.addAttribute("expired", true);
            model.addAttribute("emailMasked", maskEmail(user.getEmail()));
            model.addAttribute("name", user.getFullName());
            return "oauth2-verify";
        }

        if (user.getOauthOtpAttempts() >= 5) {
            clearOtp(user);
            model.addAttribute("error", "Umefikia idadi ya juu ya majaribio. Omba OTP mpya.");
            model.addAttribute("emailMasked", maskEmail(user.getEmail()));
            model.addAttribute("name", user.getFullName());
            return "oauth2-verify";
        }

        user.setOauthOtpAttempts(user.getOauthOtpAttempts() + 1);

        if (!passwordEncoder.matches(otp == null ? "" : otp.trim(), user.getOauthOtpHash())) {
            userRepository.save(user);
            model.addAttribute("error", "OTP si sahihi.");
            model.addAttribute("emailMasked", maskEmail(user.getEmail()));
            model.addAttribute("name", user.getFullName());
            model.addAttribute("sent", true);
            return "oauth2-verify";
        }

        clearOtp(user);
        request.changeSessionId();
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String role = user.getRole() == null || user.getRole().isBlank() ? "INDIVIDUAL" : user.getRole().toUpperCase();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getUsername(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        request.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext());

        clearChallenge(request.getSession(false));
        return "redirect:/dashboard";
    }

    @PostMapping("/resend")
    public String resend(HttpSession session, Model model) {
        if (!challengeExists(session)) return "redirect:/login";

        Object sentAt = session.getAttribute("OAUTH_OTP_SENT_AT");
        if (sentAt instanceof Long && System.currentTimeMillis() - (Long) sentAt < RESEND_COOLDOWN_MS) {
            model.addAttribute("error", "Subiri sekunde chache kabla ya kuomba OTP nyingine.");
            model.addAttribute("emailMasked", maskEmail((String) session.getAttribute(EMAIL)));
            model.addAttribute("name", session.getAttribute(NAME));
            model.addAttribute("sent", true);
            model.addAttribute("expired", false);
            return "oauth2-verify";
        }

        User user = userRepository.findById((Long) session.getAttribute(USER_ID)).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())
                || !user.isApproved() || !user.isEmailVerified()) {
            clearChallenge(session);
            return "redirect:/login?error=oauth_account_unavailable";
        }

        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        user.setOauthOtpHash(passwordEncoder.encode(otp));
        user.setOauthOtpExpiresAt(LocalDateTime.now().plusSeconds(60));
        user.setOauthOtpAttempts(0);
        userRepository.save(user);

        if (!emailService.sendOAuthLoginOtp(user.getEmail(), user.getUsername(), otp)) {
            clearOtp(user);
            model.addAttribute("error", "Email ya uthibitisho haikutumwa. Tafadhali jaribu tena.");
        } else {
            session.setAttribute("OAUTH_OTP_SENT_AT", System.currentTimeMillis());
            model.addAttribute("sent", true);
        }

        model.addAttribute("emailMasked", maskEmail(user.getEmail()));
        model.addAttribute("name", user.getFullName());
        return "oauth2-verify";
    }

    @PostMapping("/decline")
    public String decline(HttpSession session) {
        clearChallenge(session);
        SecurityContextHolder.clearContext();
        return "redirect:/login";
    }

    private boolean challengeExists(HttpSession session) {
        if (session == null || session.getAttribute(USER_ID) == null) return false;
        Object expiry = session.getAttribute(CHALLENGE_EXPIRES);
        return expiry instanceof Long && System.currentTimeMillis() < (Long) expiry;
    }

    private void clearOtp(User user) {
        user.setOauthOtpHash(null);
        user.setOauthOtpExpiresAt(null);
        user.setOauthOtpAttempts(0);
        userRepository.save(user);
    }

    private void clearChallenge(HttpSession session) {
        if (session == null) return;
        session.removeAttribute(USER_ID);
        session.removeAttribute(EMAIL);
        session.removeAttribute(NAME);
        session.removeAttribute("OAUTH_VERIFY_PICTURE");
        session.removeAttribute(CHALLENGE_EXPIRES);
        session.removeAttribute("OAUTH_OTP_SENT_AT");
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "email yako";
        String[] p = email.split("@", 2);
        String local = p[0];
        String visible = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
        return visible + "••••@" + p[1];
    }
}
