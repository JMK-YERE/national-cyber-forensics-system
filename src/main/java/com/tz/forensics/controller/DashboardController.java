package com.tz.forensics.controller;

import com.tz.forensics.entity.ReportAttack;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.ReportAttackRepository;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.repository.WhistleblowerReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final UserRepository userRepository;
    private final ReportAttackRepository reportRepo;
    private final WhistleblowerReportRepository wbRepo;

    public DashboardController(UserRepository userRepository,
                                ReportAttackRepository reportRepo,
                                WhistleblowerReportRepository wbRepo) {
        this.userRepository = userRepository;
        this.reportRepo = reportRepo;
        this.wbRepo = wbRepo;
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

        boolean isAdmin = user.isAdmin() || user.isProfessional()
                || user.isForensics() || "ANALYST".equalsIgnoreCase(role);

        if (isAdmin) {
            // ===== ADMIN DASHBOARD — stats zote =====
            try {
                long totalReports = reportRepo.count();
                long newReports = reportRepo.countByStatus("NEW");
                long totalWb = wbRepo.count();
                long newWb = wbRepo.countByStatus("NEW");
                long totalUsers = userRepository.count();

                model.addAttribute("totalReports", totalReports);
                model.addAttribute("newReports", newReports);
                model.addAttribute("totalWb", totalWb);
                model.addAttribute("newWb", newWb);
                model.addAttribute("totalUsers", totalUsers);

                // Recent reports (5 za mwisho)
                List<ReportAttack> recent = reportRepo.findAllByOrderByCreatedAtDesc();
                if (recent.size() > 5) recent = recent.subList(0, 5);
                model.addAttribute("recentReports", recent);
            } catch (Exception e) {
                log.error("Admin dashboard stats failed: {}", e.getMessage());
                model.addAttribute("totalReports", 0L);
                model.addAttribute("newReports", 0L);
                model.addAttribute("totalWb", 0L);
                model.addAttribute("newWb", 0L);
                model.addAttribute("totalUsers", 0L);
                model.addAttribute("recentReports", List.of());
            }

            return switch (role) {
                case "ADMIN" -> "dashboard-admin";
                case "CYBER_PRO" -> "dashboard-professional";
                case "FORENSICS" -> "dashboard-forensics";
                default -> "dashboard-admin";
            };
        } else {
            // ===== USER DASHBOARD — simple =====
            try {
                List<ReportAttack> myReports = reportRepo.findByUserIdOrderByCreatedAtDesc(user.getId());
                model.addAttribute("myReports", myReports);
            } catch (Exception e) {
                log.error("User reports: {}", e.getMessage());
                model.addAttribute("myReports", List.of());
            }
            return "dashboard-individual";
        }
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
}
