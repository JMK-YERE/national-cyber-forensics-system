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

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        if (auth == null || auth.getName() == null) return "redirect:/login";

        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        model.addAttribute("user", user);
        model.addAttribute("username", user.getUsername());
        model.addAttribute("role", user.getRole());

        String role = user.getRole() != null ? user.getRole() : "INDIVIDUAL";
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        boolean isPro = "CYBER_PRO".equalsIgnoreCase(role);
        boolean isForensics = "FORENSICS".equalsIgnoreCase(role);
        boolean isAnalyst = "ANALYST".equalsIgnoreCase(role);
        boolean isStaff = isAdmin || isPro || isForensics || isAnalyst;

        try {
            if (isStaff) {
                long totalReports = reportRepo.count();
                long newReports = reportRepo.countByStatus("NEW");
                long totalWb = wbRepo.count();
                long totalUsers = userRepository.count();

                model.addAttribute("totalReports", totalReports);
                model.addAttribute("newReports", newReports);
                model.addAttribute("totalWb", totalWb);
                model.addAttribute("totalUsers", totalUsers);

                List<ReportAttack> recent = reportRepo.findAllByOrderByCreatedAtDesc();
                if (recent.size() > 5) recent = recent.subList(0, 5);
                model.addAttribute("recentReports", recent);
            } else {
                List<ReportAttack> my = reportRepo.findByUserIdOrderByCreatedAtDesc(user.getId());
                model.addAttribute("myReports", my);
            }
        } catch (Exception e) {
            log.error("Dashboard error: {}", e.getMessage());
            model.addAttribute("recentReports", List.of());
            model.addAttribute("myReports", List.of());
        }

        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isCyberPro", isPro);
        model.addAttribute("isForensics", isForensics);
        model.addAttribute("isAnalyst", isAnalyst);
        model.addAttribute("isStaff", isStaff);
        return "dashboard";
    }

    @GetMapping("/access-denied")
    public String accessDenied() { return "access-denied"; }
}
