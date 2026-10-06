package com.tz.forensics.controller.admin;

import com.tz.forensics.entity.AuditLog;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.AuditService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin/audit")
public class AdminAuditController {

    private final UserRepository userRepository;
    private final AuditService auditService;

    public AdminAuditController(UserRepository userRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    private boolean isAuthorized(Authentication auth) {
        if (auth == null) return false;
        User u = userRepository.findByUsername(auth.getName()).orElse(null);
        return u != null && (u.isAdmin() || u.isProfessional() || u.isForensics());
    }

    @GetMapping
    public String list(Authentication auth, Model model) {
        if (!isAuthorized(auth)) return "redirect:/access-denied";
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        List<AuditLog> logs = auditService.getAllLogs();
        model.addAttribute("logs", logs);
        model.addAttribute("currentUser", user);
        return "admin/audit";
    }
}
