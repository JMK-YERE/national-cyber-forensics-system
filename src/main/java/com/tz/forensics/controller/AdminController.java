package com.tz.forensics.controller;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.AuditService;
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

    public AdminController(UserRepository userRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
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
}
