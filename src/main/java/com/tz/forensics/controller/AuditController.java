package com.tz.forensics.controller;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.AuditService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuditController {

    private final AuditService auditService;
    private final UserRepository userRepository;

    public AuditController(AuditService auditService, UserRepository userRepository) {
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    @GetMapping("/audit")
    public String viewLogs(Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        // Only Admin, Cyber Pro, Forensics can see audit logs
        if (!user.isAdmin() && !user.isProfessional() && !user.isForensics()) {
            return "redirect:/access-denied";
        }

        model.addAttribute("logs", auditService.getAllLogs());
        model.addAttribute("currentUser", user);
        return "audit-log";
    }
}
