package com.tz.forensics.controller;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.AuditService;
import com.tz.forensics.service.EmailService;
import com.tz.forensics.service.SmsService;
import com.tz.forensics.service.WhatsAppService;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final AuditService auditService;
    private final EmailService emailService;
    private final SmsService smsService;
    private final WhatsAppService whatsAppService;
    private final PasswordEncoder passwordEncoder;

    public AdminController(UserRepository userRepository, AuditService auditService, EmailService emailService, SmsService smsService, WhatsAppService whatsAppService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.emailService = emailService;
        this.smsService = smsService;
        this.whatsAppService = whatsAppService;
        this.passwordEncoder = passwordEncoder;
    }

    // ===== LIST USERS =====
    @GetMapping("/users")
    public String listUsers(Model model, Authentication auth) {
        User currentUser = auth == null ? null : userRepository.findByUsername(auth.getName()).orElse(null);
        if (currentUser == null || !currentUser.isAdmin()) {
            return "redirect:/access-denied";
        }

        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        model.addAttribute("totalUsers", users.size());
        model.addAttribute("adminCount", users.stream().filter(User::isAdmin).count());
        model.addAttribute("proCount", users.stream().filter(User::isProfessional).count());
        model.addAttribute("forensicsCount", users.stream().filter(User::isForensics).count());
        model.addAttribute("individualCount", users.stream().filter(User::isIndividual).count());
        return "admin-users";
    }

    // ===== VIEW USER =====
    @GetMapping("/users/{id}")
    public String viewUser(@PathVariable Long id, Model model, Authentication auth) {
        User currentUser = userRepository.findByUsername(auth.getName()).orElse(null);
        if (currentUser == null || !currentUser.isAdmin()) {
            return "redirect:/access-denied";
        }

        User user = userRepository.findById(id).orElse(null);
        if (user == null) return "redirect:/admin/users";

        model.addAttribute("user", user);
        return "admin-user-detail";
    }

    // ===== CHANGE ROLE =====
    @PostMapping("/users/{id}/role")
    public String changeRole(@PathVariable Long id,
                             @RequestParam String role,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
        User currentUser = userRepository.findByUsername(auth.getName()).orElse(null);
        if (currentUser == null || !currentUser.isAdmin()) {
            return "redirect:/access-denied";
        }

        User user = userRepository.findById(id).orElse(null);
        String normalizedRole = role == null ? "" : role.trim().toUpperCase();
        java.util.Set<String> allowedRoles = java.util.Set.of("ADMIN", "CYBER_PRO", "FORENSICS", "ANALYST", "INDIVIDUAL");
        if (user != null && allowedRoles.contains(normalizedRole)) {
            if (user.getId().equals(currentUser.getId()) && !"ADMIN".equals(normalizedRole)) {
                auditService.log("REJECT_CHANGE_ROLE", "User", user.getUsername(), "Self-demotion rejected");
                redirectAttributes.addFlashAttribute("error", "❌ Huwezi kujiondoa ADMIN mwenyewe.");
                return "redirect:/admin/users";
            }
            String oldRole = user.getRole();
            if ("ADMIN".equalsIgnoreCase(oldRole) && !"ADMIN".equals(normalizedRole)
                    && userRepository.findAll().stream().filter(User::isAdmin).count() <= 1) {
                auditService.log("REJECT_CHANGE_ROLE", "User", user.getUsername(), "Last admin protection");
                redirectAttributes.addFlashAttribute("error", "❌ Mfumo lazima ubaki na angalau ADMIN mmoja.");
                return "redirect:/admin/users";
            }
            user.setRole(normalizedRole);
            userRepository.save(user);
            auditService.log("CHANGE_ROLE", "User", user.getUsername(),
                    "Role: " + oldRole + " → " + normalizedRole);
            redirectAttributes.addFlashAttribute("success",
                    "✅ Role ya " + user.getUsername() + " imebadilishwa kuwa " + normalizedRole);
        }
        return "redirect:/admin/users";
    }

    // ===== APPROVE USER =====
    @PostMapping("/users/{id}/approve")
    public String approveUser(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        User currentUser = userRepository.findByUsername(auth.getName()).orElse(null);
        if (currentUser == null || !currentUser.isAdmin()) return "redirect:/access-denied";
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            user.setApprovalStatus("APPROVED");
            user.setEnabled(true);
            userRepository.save(user);
            auditService.log("APPROVE_USER", "User", user.getUsername(), "User approved");
            ra.addFlashAttribute("success", "✅ " + user.getUsername() + " ameidhinishwa");
        }
        return "redirect:/admin/users";
    }

    // ===== ENABLE/DISABLE USER =====
    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
        User currentUser = userRepository.findByUsername(auth.getName()).orElse(null);
        if (currentUser == null || !currentUser.isAdmin()) {
            return "redirect:/access-denied";
        }

        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            if (user.getId().equals(currentUser.getId()) && Boolean.TRUE.equals(user.getEnabled())) {
                auditService.log("REJECT_TOGGLE_USER", "User", user.getUsername(), "Self-disable rejected");
                redirectAttributes.addFlashAttribute("error", "❌ Huwezi kujizima mwenyewe.");
                return "redirect:/admin/users";
            }
            if (user.isAdmin() && Boolean.TRUE.equals(user.getEnabled())
                    && userRepository.findAll().stream().filter(u -> u.isAdmin() && Boolean.TRUE.equals(u.getEnabled())).count() <= 1) {
                auditService.log("REJECT_TOGGLE_USER", "User", user.getUsername(), "Last enabled admin protection");
                redirectAttributes.addFlashAttribute("error", "❌ Lazima kuwe na ADMIN mmoja aliyewezeshwa.");
                return "redirect:/admin/users";
            }
            user.setEnabled(!Boolean.TRUE.equals(user.getEnabled()));
            userRepository.save(user);
            String status = user.getEnabled() ? "amewashwa" : "amezimwa";
            auditService.log("TOGGLE_USER", "User", user.getUsername(),
                    "User " + status);
            redirectAttributes.addFlashAttribute("success",
                    "✅ " + user.getUsername() + " " + status);
        }
        return "redirect:/admin/users";
    }

    // ===== UNLOCK ACCOUNT =====
    @PostMapping("/users/{id}/unlock")
    public String unlockUser(@PathVariable Long id,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
        User currentUser = userRepository.findByUsername(auth.getName()).orElse(null);
        if (currentUser == null || !currentUser.isAdmin()) {
            return "redirect:/access-denied";
        }

        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            user.setAccountLocked(false);
            user.setFailedAttempts(0);
            userRepository.save(user);
            auditService.log("UNLOCK_USER", "User", user.getUsername(), "Account unlocked");
            redirectAttributes.addFlashAttribute("success",
                    "✅ " + user.getUsername() + " amefunguliwa");
        }
        return "redirect:/admin/users";
    }

    // ===== DELETE USER =====
    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
        User currentUser = userRepository.findByUsername(auth.getName()).orElse(null);
        if (currentUser == null || !currentUser.isAdmin()) {
            return "redirect:/access-denied";
        }

        User user = userRepository.findById(id).orElse(null);
        if (user != null && !user.getId().equals(currentUser.getId())
                && !"admin".equalsIgnoreCase(user.getUsername())) {
            String username = user.getUsername();
            user.setEnabled(false);
            user.setApprovalStatus("REVOKED");
            userRepository.save(user);
            auditService.log("DEACTIVATE_USER", "User", username,
                    "Account deactivated instead of hard deletion to preserve forensic attribution and audit history");
            redirectAttributes.addFlashAttribute("success",
                    "✅ " + username + " amezimwa na kuwekwa REVOKED; rekodi za audit zimehifadhiwa.");
        } else {
            redirectAttributes.addFlashAttribute("error",
                    "❌ Hauwezi kufuta admin mkuu");
        }
        return "redirect:/admin/users";
    }
    @PostMapping("/users/{id}/temporary-password")
    public String resetTemporaryPassword(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        User admin = auth == null ? null : userRepository.findByUsername(auth.getName()).orElse(null);
        if (admin == null || !admin.isAdmin()) return "redirect:/access-denied";
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            ra.addFlashAttribute("error", "Akaunti haijapatikana.");
            return "redirect:/admin/users";
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            ra.addFlashAttribute("error", "Akaunti imezimwa. Washa akaunti kwanza kabla ya kutoa temporary password.");
            return "redirect:/admin/users";
        }
        String temporaryPassword = generateTemporaryPassword();
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        user.setPasswordSetupChannel("INIT");
        user.setPasswordSetupExpiresAt(LocalDateTime.now().plusHours(24));
        user.setPasswordSetupToken(null);
        user.setPasswordSetupAttempts(0);
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        user.setPasswordResetAttempts(0);
        user.setFailedAttempts(0);
        user.setAccountLocked(false);
        user.setApprovalStatus("APPROVED");
        user.setEmailVerified(true);
        userRepository.save(user);
        auditService.log(admin.getId(), admin.getUsername(), "ADMIN_RESET_TEMPORARY_PASSWORD", "User", user.getUsername(), "Temporary password reissued; expiry=24h; secret omitted");
        ra.addFlashAttribute("success", "Temporary password mpya imetengenezwa kwa " + user.getUsername() + ". Mtumiaji lazima abadilishe kabla ya Dashboard.");
        ra.addFlashAttribute("temporaryPassword", temporaryPassword);
        ra.addFlashAttribute("temporaryUsername", user.getUsername());
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/temporary-access")
    public String issueTemporaryAccess(@PathVariable Long id, @RequestParam String channel, Authentication auth, RedirectAttributes ra) {
        User admin = auth == null ? null : userRepository.findByUsername(auth.getName()).orElse(null);
        if (admin == null || !admin.isAdmin()) return "redirect:/access-denied";
        User user = userRepository.findById(id).orElse(null);
        if (user != null && "INIT".equalsIgnoreCase(user.getPasswordSetupChannel())) {
            ra.addFlashAttribute("error", "Mtumiaji huyu lazima aingie kwa temporary password na abadilishe password kwanza kabla ya kutoa reset link/OTP mpya.");
            return "redirect:/admin/users";
        }
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            ra.addFlashAttribute("error", "Akaunti haipo au imezimwa.");
            return "redirect:/admin/users";
        }

        String selected = channel == null ? "" : channel.trim().toUpperCase();
        if ("EMAIL".equals(selected)) {
            if (user.getEmail() == null || user.getEmail().isBlank()) {
                ra.addFlashAttribute("error", "Akaunti hii haina email ya kutuma setup link.");
                return "redirect:/admin/users";
            }
            String token = java.util.UUID.randomUUID().toString() + java.util.UUID.randomUUID();
            user.setPasswordSetupToken(sha256(token));
            user.setPasswordSetupAttempts(0);
            user.setPasswordSetupExpiresAt(LocalDateTime.now().plusHours(24));
            user.setPasswordSetupChannel("EMAIL");
            userRepository.save(user);
            boolean sent = emailService.sendPasswordSetupEmail(user.getEmail(), user.getUsername(), token);
            if (!sent) {
                user.setPasswordSetupToken(null);
                user.setPasswordSetupExpiresAt(null);
                user.setPasswordSetupChannel(null);
                userRepository.save(user);
                auditService.log(admin.getId(), admin.getUsername(), "TEMP_ACCESS_SEND_FAILED", "User", user.getUsername(), "Channel=EMAIL; setup token invalidated; secret omitted");
                ra.addFlashAttribute("error", "Email haikutumwa. Hakuna link iliyoamilishwa. Kagua email provider na ujaribu tena.");
                return "redirect:/admin/users";
            }
            auditService.log(admin.getId(), admin.getUsername(), "TEMP_ACCESS_ISSUED", "User", user.getUsername(), "Channel=EMAIL; expiry=24h; secret omitted");
            ra.addFlashAttribute("success", "Setup link imetumwa kwa email ya " + user.getUsername() + ". Link inaisha ndani ya saa 24.");
            return "redirect:/admin/users";
        }

        String destination = "SMS".equals(selected) ? user.getPhone() : ("WHATSAPP".equals(selected) ? (user.getWhatsappNumber() != null && !user.getWhatsappNumber().isBlank() ? user.getWhatsappNumber() : user.getPhone()) : null);
        if (!"SMS".equals(selected) && !"WHATSAPP".equals(selected)) {
            ra.addFlashAttribute("error", "Chagua Email reset link au SMS OTP.");
            return "redirect:/admin/users";
        }
        if (destination == null || destination.isBlank()) {
            ra.addFlashAttribute("error", "Namba ya simu/WhatsApp haipo.");
            return "redirect:/admin/users";
        }
        String code = String.valueOf(100000 + new SecureRandom().nextInt(900000));
        user.setPasswordSetupToken(sha256(code));
        user.setPasswordSetupAttempts(0);
        user.setPasswordSetupExpiresAt(LocalDateTime.now().plusMinutes(10));
        user.setPasswordSetupChannel(selected);
        userRepository.save(user);
        String message = "Cyber Forensics TZ: code ya muda ni " + code + ". Inaisha ndani ya dakika 10. Fungua /set-password, weka namba yako na code, kisha tengeneza password mpya. Usimshirikishe mtu mwingine.";
        boolean sent = "SMS".equals(selected) ? smsService.sendSms(destination, message) : whatsAppService.sendWhatsApp(destination, user.getWhatsappApiKey(), message);
        if (!sent) {
            auditService.log(admin.getId(), admin.getUsername(), "TEMP_ACCESS_SEND_FAILED", "User", user.getUsername(), "Channel=" + selected + "; manual share fallback; secret omitted");
            ra.addFlashAttribute("warning", "Kutuma kupitia " + selected + " kumeshindikana (huduma haijasanidiwa au namba si sahihi). Unaweza kunakili code hii na kuituma mwenyewe. Code inaisha ndani ya dakika 10.");
            ra.addFlashAttribute("manualAccessCode", code);
            ra.addFlashAttribute("manualAccessPhone", destination);
            ra.addFlashAttribute("manualAccessUsername", user.getUsername());
            ra.addFlashAttribute("manualAccessChannel", selected);
            return "redirect:/admin/users";
        }
        auditService.log(admin.getId(), admin.getUsername(), "TEMP_ACCESS_ISSUED", "User", user.getUsername(), "Channel=" + selected + "; expiry=10m; secret omitted");
        ra.addFlashAttribute("success", "Code ya muda imetumwa kwa " + user.getUsername() + " kupitia " + selected + ". Inaisha ndani ya dakika 10.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/create")
    public String createUser(@RequestParam String username, @RequestParam String email, @RequestParam(required = false) String fullName, @RequestParam(required = false) String phone, @RequestParam(required = false) String whatsappNumber, @RequestParam(required = false) String organization, @RequestParam(defaultValue = "INDIVIDUAL") String role, Authentication auth, RedirectAttributes ra) {
        User admin = auth == null ? null : userRepository.findByUsername(auth.getName()).orElse(null);
        if (admin == null || !admin.isAdmin()) return "redirect:/access-denied";
        String name = username == null ? "" : username.trim(); String mail = email == null ? "" : email.trim().toLowerCase(); String selectedRole = role == null ? "" : role.trim().toUpperCase();
        if (name.isBlank() || mail.isBlank() || userRepository.existsByUsername(name) || userRepository.existsByEmail(mail)) { ra.addFlashAttribute("error", "Username/email haijajazwa au tayari inatumika."); return "redirect:/admin/users"; }
        if ((phone == null || phone.isBlank()) && (whatsappNumber == null || whatsappNumber.isBlank())) { ra.addFlashAttribute("error", "Weka namba ya simu au WhatsApp ili kutuma code."); return "redirect:/admin/users"; }
        if (!java.util.Set.of("INDIVIDUAL", "CYBER_PRO", "FORENSICS", "ANALYST", "ADMIN").contains(selectedRole)) { ra.addFlashAttribute("error", "Role si sahihi."); return "redirect:/admin/users"; }
        String temporaryPassword = generateTemporaryPassword(); User user = new User(); user.setUsername(name); user.setEmail(mail); user.setFullName(fullName == null ? "" : fullName.trim()); user.setPhone(phone == null ? "" : phone.trim()); user.setWhatsappNumber(whatsappNumber == null ? "" : whatsappNumber.trim()); user.setOrganization(organization == null ? "" : organization.trim()); user.setRole(selectedRole); user.setPassword(passwordEncoder.encode(temporaryPassword)); user.setPasswordSetupChannel("INIT"); user.setPasswordSetupExpiresAt(LocalDateTime.now().plusHours(24)); user.setPasswordSetupAttempts(0); user.setEnabled(true); user.setEmailVerified(true); user.setApprovalStatus("APPROVED"); user.setCreatedAt(LocalDateTime.now()); userRepository.save(user);
        auditService.log(admin.getId(), admin.getUsername(), "ADMIN_CREATE_USER", "User", user.getUsername(), "Role=" + selectedRole + "; initial setup code not yet sent");
        ra.addFlashAttribute("success", "Akaunti imetengenezwa. Tumia temporary password iliyoonyeshwa hapa chini kuingia; mfumo utakulazimisha kuweka password yako ndani ya saa 24."); ra.addFlashAttribute("temporaryPassword", temporaryPassword); ra.addFlashAttribute("temporaryUsername", user.getUsername()); return "redirect:/admin/users";
    }

    private String generateTemporaryPassword() {
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$%";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(14);
        for (int i = 0; i < 14; i++) password.append(alphabet.charAt(random.nextInt(alphabet.length())));
        return password.toString();
    }

    private String sha256(String value) { try { byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)); StringBuilder out = new StringBuilder(64); for (byte b : digest) out.append(String.format("%02x", b)); return out.toString(); } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 unavailable", e); } }
}
