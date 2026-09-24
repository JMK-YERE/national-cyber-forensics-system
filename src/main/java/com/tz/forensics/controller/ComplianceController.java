package com.tz.forensics.controller;

import com.tz.forensics.entity.ComplianceReport;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.AuditService;
import com.tz.forensics.service.ComplianceService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/compliance")
public class ComplianceController {

    private final ComplianceService service;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public ComplianceController(ComplianceService service,
                                 UserRepository userRepository,
                                 AuditService auditService) {
        this.service = service;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    private boolean canAccess(User user) {
        return user != null && (user.isAdmin() || user.isProfessional() || user.isForensics());
    }

    @GetMapping
    public String dashboard(Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (!canAccess(user)) return "redirect:/access-denied";

        model.addAttribute("reports", service.getAllReports());
        model.addAttribute("stats", service.getStatistics());
        model.addAttribute("user", user);
        return "compliance-dashboard";
    }

    @PostMapping("/generate")
    public String generateReport(@RequestParam String type,
                                  Authentication auth,
                                  RedirectAttributes ra) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (!canAccess(user)) return "redirect:/access-denied";

        ComplianceReport report = service.generateReport(type, user.getId(), user.getUsername());
        auditService.log("COMPLIANCE_REPORT", "Compliance", report.getReportId(), "Generated " + type);

        ra.addFlashAttribute("success", "✅ Compliance report imeundwa: " + report.getReportId());
        return "redirect:/compliance/view/" + report.getId();
    }

    @GetMapping("/view/{id}")
    public String viewReport(@PathVariable Long id, Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (!canAccess(user)) return "redirect:/access-denied";

        ComplianceReport report = service.getById(id);
        if (report == null) return "redirect:/compliance";

        model.addAttribute("report", report);
        model.addAttribute("user", user);
        return "compliance-report";
    }

    @PostMapping("/update/{id}")
    public String updateStatus(@PathVariable Long id,
                                @RequestParam String status,
                                Authentication auth,
                                RedirectAttributes ra) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (!canAccess(user)) return "redirect:/access-denied";

        service.updateStatus(id, status);
        ra.addFlashAttribute("success", "✅ Status imebadilishwa");
        return "redirect:/compliance/view/" + id;
    }
}
