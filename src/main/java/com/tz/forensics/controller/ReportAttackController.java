package com.tz.forensics.controller;

import com.tz.forensics.entity.ReportAttack;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.AuditService;
import com.tz.forensics.service.EmailService;
import com.tz.forensics.service.NotificationService;
import com.tz.forensics.service.ReportAttackService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/report-attack")
public class ReportAttackController {

    private final ReportAttackService service;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final EmailService emailService;

    public ReportAttackController(ReportAttackService service,
                                   UserRepository userRepository,
                                   AuditService auditService,
                                   NotificationService notificationService,
                                   EmailService emailService) {
        this.service = service;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    @GetMapping
    public String showForm(Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        model.addAttribute("user", user);
        model.addAttribute("myReports", service.getMine(user.getId()));
        model.addAttribute("report", new ReportAttack());
        model.addAttribute("recentAttacks", service.getRecent24h());
        model.addAttribute("totalReports", service.countTotal());
        model.addAttribute("todayReports", service.countToday());
        return "report-attack-form";
    }

    @PostMapping("/new")
    public String createReport(@ModelAttribute ReportAttack report,
                                @RequestParam(required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateOccurred,
                                Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        report.setUserId(user.getId());
        report.setDateOccurred(dateOccurred);

        if (report.getReporterName() == null || report.getReporterName().isBlank()) {
            report.setReporterName(user.getFullName() != null ? user.getFullName() : user.getUsername());
        }
        if (report.getReporterEmail() == null || report.getReporterEmail().isBlank()) {
            report.setReporterEmail(user.getEmail());
        }

        ReportAttack saved = service.create(report);
        auditService.log("REPORT_ATTACK", "ReportAttack", saved.getReportId(),
                "Attack type: " + saved.getAttackType() + " | Region: " + saved.getRegion());

        // Notification kwa admin
        try {
            notificationService.createNotification(
                "🚨 Attack Mpya: " + saved.getAttackTypeLabel(),
                saved.getTitle() + " (" + saved.getRegion() + ")",
                "CRITICAL", "/report-attack/admin"
            );
        } catch (Exception e) { System.err.println("Notif failed: " + e.getMessage()); }

        // Email kwa admin
        try {
            emailService.sendIncidentAlert(saved.getReportId(), saved.getTitle(), saved.getPriority());
        } catch (Exception e) { System.err.println("Email failed: " + e.getMessage()); }

        return "redirect:/report-attack/success?id=" + saved.getId();
    }

    @GetMapping("/success")
    public String success(@RequestParam Long id, Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        ReportAttack r = service.getById(id);
        if (user == null || r == null) return "redirect:/report-attack";

        model.addAttribute("report", r);
        model.addAttribute("user", user);
        return "report-attack-success";
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable Long id, Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        ReportAttack r = service.getById(id);
        if (user == null || r == null) return "redirect:/report-attack";

        model.addAttribute("report", r);
        model.addAttribute("user", user);
        return "report-attack-detail";
    }

    // ===== ADMIN =====
    @GetMapping("/admin")
    public String adminList(Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null || (!user.isAdmin() && !user.isProfessional() && !user.isForensics())) {
            return "redirect:/access-denied";
        }

        List<ReportAttack> all = service.getAll();
        model.addAttribute("reports", all);
        model.addAttribute("user", user);
        model.addAttribute("newCount", service.countNew());
        model.addAttribute("todayCount", service.countToday());
        model.addAttribute("totalCount", service.countTotal());

        // Assignable users
        List<User> assignable = userRepository.findAll().stream()
                .filter(u -> u.isAdmin() || u.isProfessional() || u.isForensics())
                .toList();
        model.addAttribute("assignableUsers", assignable);

        return "report-attack-admin";
    }

    @PostMapping("/admin/{id}/update")
    public String updateStatus(@PathVariable Long id,
                                @RequestParam String status,
                                @RequestParam(required = false) String adminResponse,
                                @RequestParam(required = false) Long assignedTo,
                                Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null || (!user.isAdmin() && !user.isProfessional())) {
            return "redirect:/access-denied";
        }

        String assignedName = null;
        if (assignedTo != null) {
            User assignee = userRepository.findById(assignedTo).orElse(null);
            if (assignee != null) assignedName = assignee.getUsername();
        }

        service.updateStatus(id, status, adminResponse, assignedTo, assignedName);
        auditService.log("ATTACK_UPDATE", "ReportAttack", String.valueOf(id), "Status: " + status);
        return "redirect:/report-attack/admin";
    }
}
