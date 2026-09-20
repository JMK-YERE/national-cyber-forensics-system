package com.tz.forensics.controller;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.IncidentService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final IncidentService incidentService;
    private final UserRepository userRepository;

    public DashboardController(IncidentService incidentService, UserRepository userRepository) {
        this.incidentService = incidentService;
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    // ===== ROUTER — Inaamua dashboard ipi =====
    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        model.addAttribute("user", user);
        model.addAttribute("username", user.getUsername());
        model.addAttribute("role", user.getRole());
        model.addAttribute("totalIncidents", incidentService.getAllIncidents().size());

        // ===== ROUTE KWA ROLE =====
        return switch (user.getRole() != null ? user.getRole() : "INDIVIDUAL") {
            case "ADMIN" -> "dashboard-admin";
            case "CYBER_PRO" -> "dashboard-professional";
            case "FORENSICS" -> "dashboard-forensics";
            default -> "dashboard-individual";
        };
    }

    // ===== ACCESS DENIED =====
    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
}
