package com.tz.forensics.controller;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final UserRepository userRepository;

    public DashboardController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        if (auth == null || auth.getName() == null) {
            return "redirect:/login";
        }

        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        model.addAttribute("user", user);
        model.addAttribute("username", user.getUsername());
        model.addAttribute("role", user.getRole());

        String role = user.getRole() != null ? user.getRole() : "INDIVIDUAL";
        log.info("Dashboard access: user={}, role={}", user.getUsername(), role);

        // Route kwa role
        return switch (role) {
            case "ADMIN" -> "dashboard-admin";
            case "CYBER_PRO" -> "dashboard-professional";
            case "FORENSICS" -> "dashboard-forensics";
            default -> "dashboard-individual";
        };
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
}
