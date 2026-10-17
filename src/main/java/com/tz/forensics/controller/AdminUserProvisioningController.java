package com.tz.forensics.controller;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.AuditService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;\nimport org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Set;

@Controller
@RequestMapping("/admin/users")
public class AdminUserProvisioningController {
    private static final Set<String> ROLES = Set.of("CYBER_PRO", "ANALYST", "FORENSICS", "INDIVIDUAL");
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final SecureRandom random = new SecureRandom();

    public AdminUserProvisioningController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                           AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @GetMapping("/new")
    public String newUserForm(Authentication auth, Model model) {
        if (!isAdmin(auth)) return "redirect:/access-denied";
        model.addAttribute("allowedRoles", ROLES);
        return "admin-add-user";
    }

    @PostMapping("/create")
    public String createUser(@RequestParam String username,
                             @RequestParam String email,
                             @RequestParam String fullName,
                             @RequestParam String role,
                             @RequestParam(required = false) String organization,
                             @RequestParam(required = false) String phone,
                             @RequestParam(required = false) String whatsappNumber,
                             Authentication auth, Model model) {
        if (!isAdmin(auth)) return "redirect:/access-denied";

        String cleanUsername = username == null ? "" : username.trim();
        String cleanEmail = email == null ? "" : email.trim().toLowerCase();
        String cleanName = fullName == null ? "" : fullName.trim();
        String cleanRole = role == null ? "" : role.trim().toUpperCase();
        String cleanPhone = clean(phone);
        String cleanWhatsApp = clean(whatsappNumber);

        model.addAttribute("allowedRoles", ROLES);
        model.addAttribute("formUsername", cleanUsername);
        model.addAttribute("formEmail", cleanEmail);
        model.addAttribute("formFullName", cleanName);
        model.addAttribute("formRole", cleanRole);
        model.addAttribute("formOrganization", clean(organization));
        model.addAttribute("formPhone", cleanPhone);
        model.addAttribute("formWhatsApp", cleanWhatsApp);

        if (!cleanUsername.matches("[A-Za-z0-9._-]{3,50}") || cleanName.isBlank()
                || !cleanEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || !ROLES.contains(cleanRole)) {
            model.addAttribute("error", "Jaza taarifa sahihi. Username iwe herufi/tarakimu/._- (angalau 3), email sahihi, na role inayoruhusiwa.");
            return "admin-add-user";
        }
        if (userRepository.existsByUsername(cleanUsername)) {
            model.addAttribute("error", "Username hiyo tayari ipo.");
            return "admin-add-user";
        }
        if (userRepository.existsByEmail(cleanEmail)) {
            model.addAttribute("error", "Email hiyo tayari imesajiliwa.");
            return "admin-add-user";
        }

        String temporaryPassword = generateTemporaryPassword();
        User user = new User();
        user.setUsername(cleanUsername);
        user.setEmail(cleanEmail);
        user.setFullName(cleanName);
        user.setRole(cleanRole);
        user.setOrganization(blankToNull(organization));
        user.setPhone(cleanPhone);
        user.setWhatsappNumber(cleanWhatsApp);
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        user.setEnabled(true);
        user.setAccountLocked(false);
        user.setFailedAttempts(0);
        user.setEmailVerified(true);
        user.setApprovalStatus("APPROVED");
        user.setMustChangePassword(true);
        user.setPasswordSetupToken(null);
        user.setPasswordSetupExpiresAt(null);
        user.setPasswordSetupChannel(null);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        userRepository.save(user);

        auditService.log("ADMIN_CREATE_USER", "User", user.getUsername(),
                "Provisioned role=" + cleanRole + "; forced password change enabled");

        String message = "Cyber Forensics TZ - Akaunti yako imeundwa.\n"
                + "Username: " + cleanUsername + "\n"
                + "Temporary password: " + temporaryPassword + "\n"
                + "Login: " + "https://cyber-forensics-tz.cyber.blitz.cloud/login" + "\n"
                + "Kwa usalama, badilisha password mara tu utakapoingia. Password hii ni ya matumizi ya kwanza tu.";
        String digits = cleanWhatsApp == null ? "" : cleanWhatsApp.replaceAll("[^0-9]", "");
        String whatsappLink = digits.isBlank() ? "" :
                "https://wa.me/" + digits + "?text=" + URLEncoder.encode(message, StandardCharsets.UTF_8);

        redirectAttributes.addFlashAttribute("created", true);
        redirectAttributes.addFlashAttribute("createdUsername", cleanUsername);
        redirectAttributes.addFlashAttribute("temporaryPassword", temporaryPassword);
        redirectAttributes.addFlashAttribute("whatsappLink", whatsappLink);
        redirectAttributes.addFlashAttribute("whatsappNumberProvided", !digits.isBlank());
        redirectAttributes.addFlashAttribute("success", "Akaunti imeundwa. Hifadhi au share password hii sasa; haitahifadhiwa wazi kwenye database.");
        return "redirect:/admin/users/new";
    }
min-add-user";
    }

    private boolean isAdmin(Authentication auth) {
        return auth != null && userRepository.findByUsername(auth.getName())
                .map(User::isAdmin).orElse(false);
    }

    private String generateTemporaryPassword() {
        StringBuilder value = new StringBuilder(16);
        for (int i = 0; i < 16; i++) value.append(PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length())));
        return value.toString();
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String blankToNull(String value) {
        return clean(value);
    }
}
