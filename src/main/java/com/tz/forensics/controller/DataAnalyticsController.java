package com.tz.forensics.controller;

import com.tz.forensics.entity.ReportAttack;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.ReportAttackRepository;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.ReportAttackService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class DataAnalyticsController {
    private final ReportAttackRepository reports;
    private final UserRepository users;
    private final ReportAttackService reportService;

    public DataAnalyticsController(ReportAttackRepository reports, UserRepository users, ReportAttackService reportService) {
        this.reports = reports;
        this.users = users;
        this.reportService = reportService;
    }

    @GetMapping("/analytics")
    public String analytics(Authentication auth, Model model) {
        if (auth == null) return "redirect:/login";
        boolean allowed = auth.getAuthorities().stream()
                .anyMatch(a -> java.util.Set.of("ROLE_ADMIN","ROLE_CYBER_PRO","ROLE_FORENSICS","ROLE_ANALYST").contains(a.getAuthority()));
        if (!allowed) return "redirect:/access-denied";

        User user = users.findByUsername(auth.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        boolean privileged = user.isAdmin() || user.isProfessional() || user.isForensics();
        var all = privileged ? reports.findAllByOrderByCreatedAtDesc()
                : user.isAnalyst() ? reportService.getAssignedTo(user.getId())
                : reportService.getMine(user.getId());
        Map<String,Long> categories = new LinkedHashMap<>();
        Map<String,Long> regions = new LinkedHashMap<>();
        Map<String,Long> statuses = new LinkedHashMap<>();
        for (ReportAttack r : all) {
            categories.merge(r.getAttackTypeLabel() == null ? "Other" : r.getAttackTypeLabel(), 1L, Long::sum);
            if (r.getRegion() != null && !r.getRegion().isBlank()) regions.merge(r.getRegion().trim(), 1L, Long::sum);
            statuses.merge(r.getStatus() == null ? "NEW" : r.getStatus(), 1L, Long::sum);
        }
        model.addAttribute("total", all.size());
        model.addAttribute("users", users.count());
        model.addAttribute("open", all.stream().filter(r -> r.getStatus() == null || !"CLOSED".equalsIgnoreCase(r.getStatus())).count());
        model.addAttribute("high", all.stream().filter(r -> "HIGH".equalsIgnoreCase(r.getPriority()) || "CRITICAL".equalsIgnoreCase(r.getPriority())).count());
        model.addAttribute("categoryLabels", categories.keySet());
        model.addAttribute("categoryValues", categories.values());
        model.addAttribute("regionLabels", regions.keySet());
        model.addAttribute("regionValues", regions.values());
        model.addAttribute("statusLabels", statuses.keySet());
        model.addAttribute("statusValues", statuses.values());
        return "analytics";
    }
}