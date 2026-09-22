package com.tz.forensics.controller;

import com.tz.forensics.config.CountryConfig;
import com.tz.forensics.entity.ReportAttack;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.*;
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

    // ===== CHECK: Admin/Pro/Forensics =====
    private boolean canSeeAllReports(User user) {
        return user != null && (user.isAdmin() || user.isProfessional() || user.isForensics());
    }

    // ===== REPORT FORM =====
    @GetMapping
    public String showForm(Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        model.addAttribute("user", user);
        model.addAttribute("myReports", service.getMine(user.getId()));
        model.addAttribute("report", new ReportAttack());
        model.addAttribute("canSeeAll", canSeeAllReports(user));
        model.addAttribute("countries", CountryConfig.COUNTRIES.values());
        model.addAttribute("defaultCountry", CountryConfig.getCountry(user.getOrganization() != null ? "TZ" : "TZ"));

        // Admin anaona REPORTS ZOTE kwenye form pia
        if (canSeeAllReports(user)) {
            model.addAttribute("allReports", service.getAll());
        }

        return "report-attack-form";
    }

    @PostMapping("/new")
    public String createReport(@ModelAttribute ReportAttack report,
                                @RequestParam(required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateOccurred,
                                @RequestParam(required = false) String country,
                                Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        report.setUserId(user.getId());
        report.setDateOccurred(dateOccurred);

        if (country == null || country.isEmpty()) country = "TZ";
        report.setCountry(country);
        CountryConfig.CountryInfo ci = CountryConfig.getCountry(country);
        report.setCountryName(ci.name);

        if (report.getReporterName() == null || report.getReporterName().isBlank()) {
            report.setReporterName(user.getFullName() != null ? user.getFullName() : user.getUsername());
        }
        if (report.getReporterEmail() == null || report.getReporterEmail().isBlank()) {
            report.setReporterEmail(user.getEmail());
        }
        if (report.getReporterPhone() == null || report.getReporterPhone().isBlank()) {
            report.setReporterPhone(user.getPhone());
        }

        ReportAttack saved = service.create(report);
        auditService.log("REPORT_ATTACK", "ReportAttack", saved.getReportId(),
                "Type: " + saved.getAttackType() + " | Region: " + saved.getRegion());

        // Notify admins
        try {
            notificationService.createNotification(
                "🚨 Attack Mpya: " + saved.getAttackTypeLabel(),
                saved.getTitle() + " (" + saved.getRegion() + ", " + saved.getCountryName() + ")",
                "CRITICAL", "/report-attack/admin"
            );
        } catch (Exception e) { System.err.println("Notif: " + e.getMessage()); }

        return "redirect:/report-attack/success?id=" + saved.getId();
    }

    @GetMapping("/success")
    public String success(@RequestParam Long id, Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        ReportAttack r = service.getById(id);
        if (user == null || r == null) return "redirect:/report-attack";

        model.addAttribute("report", r);
        model.addAttribute("user", user);
        model.addAttribute("country", CountryConfig.getCountry(r.getCountry()));
        return "report-attack-success";
    }

    // ===== VIEW — Admin anaweza kuona zote, individual anaona zake tu =====
    @GetMapping("/view/{id}")
    public String view(@PathVariable Long id, Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        ReportAttack r = service.getById(id);
        if (user == null || r == null) return "redirect:/report-attack";

        // Individual anaona zake pekee, Admin anaona zote
        if (!canSeeAllReports(user) && !r.getUserId().equals(user.getId())) {
            return "redirect:/access-denied";
        }

        model.addAttribute("report", r);
        model.addAttribute("user", user);
        model.addAttribute("canSeeAll", canSeeAllReports(user));
        model.addAttribute("country", CountryConfig.getCountry(r.getCountry()));
        return "report-attack-detail";
    }

    // ===== ADMIN VIEW — Admin/Pro/Forensics PEKEE =====
    @GetMapping("/admin")
    public String adminList(Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (!canSeeAllReports(user)) return "redirect:/access-denied";

        List<ReportAttack> all = service.getAll();
        model.addAttribute("reports", all);
        model.addAttribute("user", user);
        model.addAttribute("newCount", service.countNew());
        model.addAttribute("todayCount", service.countToday());
        model.addAttribute("totalCount", service.countTotal());

        List<User> assignable = userRepository.findAll().stream()
                .filter(u -> u.isAdmin() || u.isProfessional() || u.isForensics())
                .toList();
        model.addAttribute("assignableUsers", assignable);

        return "report-attack-admin";
    }

    // ===== ADMIN UPDATE — Kumsaidia mtu =====
    @PostMapping("/admin/{id}/update")
    public String updateStatus(@PathVariable Long id,
                                @RequestParam String status,
                                @RequestParam(required = false) String adminResponse,
                                @RequestParam(required = false) Long assignedTo,
                                @RequestParam(required = false) String policeCaseNumber,
                                Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (!canSeeAllReports(user)) return "redirect:/access-denied";

        String assignedName = null;
        if (assignedTo != null) {
            User assignee = userRepository.findById(assignedTo).orElse(null);
            if (assignee != null) assignedName = assignee.getUsername();
        }

        service.updateStatus(id, status, adminResponse, assignedTo, assignedName);

        if (policeCaseNumber != null && !policeCaseNumber.isEmpty()) {
            ReportAttack r = service.getById(id);
            if (r != null) {
                r.setPoliceCaseNumber(policeCaseNumber);
                service.save(r);
            }
        }

        auditService.log("ATTACK_HELP", "ReportAttack", String.valueOf(id),
                "Status: " + status + " | Assigned: " + assignedName);

        return "redirect:/report-attack/admin";
    }
}
