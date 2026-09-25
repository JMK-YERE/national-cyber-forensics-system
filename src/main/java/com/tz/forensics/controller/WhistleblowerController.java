package com.tz.forensics.controller;

import com.tz.forensics.config.CountryConfig;
import com.tz.forensics.entity.WhistleblowerMessage;
import com.tz.forensics.entity.WhistleblowerReport;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.AuditService;
import com.tz.forensics.service.NotificationService;
import com.tz.forensics.service.WhistleblowerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/whistleblower")
public class WhistleblowerController {

    private static final Logger log = LoggerFactory.getLogger(WhistleblowerController.class);

    private final WhistleblowerService service;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Value("${app.upload.dir:uploads/whistleblower}")
    private String uploadDir;

    public WhistleblowerController(WhistleblowerService service,
                                    UserRepository userRepository,
                                    AuditService auditService,
                                    NotificationService notificationService) {
        this.service = service;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    // ===== PUBLIC: HOME =====
    @GetMapping
    public String home(Model model) {
        model.addAttribute("countries", CountryConfig.COUNTRIES.values());
        return "whistleblower-home";
    }

    // ===== PUBLIC: REPORT FORM =====
    @GetMapping("/report")
    public String reportForm(@RequestParam(required = false) String category, Model model) {
        WhistleblowerReport report = new WhistleblowerReport();
        if (category != null && !category.isEmpty()) {
            report.setCategory(category);
        }
        model.addAttribute("report", report);
        model.addAttribute("countries", CountryConfig.COUNTRIES.values());
        model.addAttribute("defaultCountry", CountryConfig.getCountry("TZ"));
        return "whistleblower-form";
    }

    // ===== PUBLIC: SUBMIT REPORT (anonymous) =====
    @PostMapping("/submit")
    public String submitReport(@ModelAttribute WhistleblowerReport report,
                                @RequestParam(required = false) String country,
                                @RequestParam(required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateOccurred,
                                @RequestParam(required = false) MultipartFile evidenceFile,
                                RedirectAttributes ra) {

        if (country == null || country.isEmpty()) country = "TZ";
        report.setCountry(country);
        report.setCountryName(CountryConfig.getCountry(country).name);
        report.setDateOccurred(dateOccurred);

        // Evidence file
        if (evidenceFile != null && !evidenceFile.isEmpty()) {
            try {
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
                String storedName = UUID.randomUUID() + "_" + evidenceFile.getOriginalFilename();
                Path targetPath = uploadPath.resolve(storedName);
                Files.write(targetPath, evidenceFile.getBytes());

                report.setEvidenceFilePath(storedName);
                report.setEvidenceFileType(detectFileType(evidenceFile.getContentType()));
                report.setEvidenceFileSize(evidenceFile.getSize());
            } catch (IOException e) {
                log.error("File upload: {}", e.getMessage());
            }
        }

        WhistleblowerReport saved = service.createReport(report);

        // ===== NOTIFY ALL ADMINS =====
        try {
            List<User> admins = userRepository.findAll().stream()
                    .filter(u -> u.isAdmin() || u.isProfessional() || u.isForensics()).toList();
            String title = "🕵️ Whistleblower Report: " + saved.getCategoryLabel();
            String msg = saved.getTitle() + " | Tracking: " + saved.getTrackingCode();
            for (User admin : admins) {
                notificationService.createNotification(
                    admin.getId(), title, msg, "CRITICAL",
                    "/whistleblower/admin/" + saved.getId()
                );
            }
            log.info("Notified {} admins for whistleblower report {}", admins.size(), saved.getTrackingCode());
        } catch (Exception e) {
            log.error("Notify admins: {}", e.getMessage());
        }

        auditService.log("WHISTLEBLOWER_CREATED", "Whistleblower",
                saved.getTrackingCode(), "New anonymous report");

        ra.addFlashAttribute("successCode", saved.getTrackingCode());
        return "redirect:/whistleblower/success";
    }

    // ===== PUBLIC: SUCCESS PAGE =====
    @GetMapping("/success")
    public String success() {
        return "whistleblower-success";
    }

    // ===== PUBLIC: TRACK FORM =====
    @GetMapping("/track")
    public String trackForm() {
        return "whistleblower-track";
    }

    // ===== PUBLIC: TRACK REPORT =====
    @PostMapping("/track")
    public String trackReport(@RequestParam String code, Model model) {
        WhistleblowerReport report = service.findByTrackingCode(code);
        if (report == null) {
            model.addAttribute("error", "Tracking code haipo au si sahihi.");
            return "whistleblower-track";
        }
        List<WhistleblowerMessage> messages = service.getMessages(report.getId());
        model.addAttribute("report", report);
        model.addAttribute("messages", messages);
        return "whistleblower-detail";
    }

    // ===== PUBLIC: REPORTER REPLY =====
    @PostMapping("/track/{id}/reply")
    public String reporterReply(@PathVariable Long id,
                                 @RequestParam String message,
                                 @RequestParam String code,
                                 RedirectAttributes ra) {
        service.addMessage(id, "REPORTER", message);

        // Notify admins of reply
        try {
            WhistleblowerReport r = service.getById(id);
            List<User> admins = userRepository.findAll().stream()
                    .filter(u -> u.isAdmin() || u.isProfessional() || u.isForensics()).toList();
            for (User admin : admins) {
                notificationService.createNotification(
                    admin.getId(),
                    "💬 Whistleblower Reply: " + r.getTrackingCode(),
                    message.substring(0, Math.min(80, message.length())),
                    "INFO", "/whistleblower/admin/" + id
                );
            }
        } catch (Exception e) { log.error("Notif: {}", e.getMessage()); }

        ra.addFlashAttribute("success", "✅ Ujumbe wako umetumwa.");
        return "redirect:/whistleblower/track?code=" + code;
    }

    // ===== ADMIN: LIST =====
    @GetMapping("/admin")
    public String adminList(Authentication auth, Model model) {
        if (auth == null) return "redirect:/login";
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null || (!user.isAdmin() && !user.isProfessional() && !user.isForensics())) {
            return "redirect:/access-denied";
        }
        model.addAttribute("reports", service.getAll());
        model.addAttribute("newCount", service.countNew());
        model.addAttribute("todayCount", service.countToday());
        model.addAttribute("user", user);
        return "whistleblower-admin";
    }

    // ===== ADMIN: DETAIL =====
    @GetMapping("/admin/{id}")
    public String adminDetail(@PathVariable Long id, Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null || (!user.isAdmin() && !user.isProfessional() && !user.isForensics())) {
            return "redirect:/access-denied";
        }
        WhistleblowerReport report = service.getById(id);
        if (report == null) return "redirect:/whistleblower/admin";
        model.addAttribute("report", report);
        model.addAttribute("messages", service.getMessages(id));
        model.addAttribute("user", user);
        return "whistleblower-admin-detail";
    }

    // ===== ADMIN: UPDATE =====
    @PostMapping("/admin/{id}/update")
    public String adminUpdate(@PathVariable Long id,
                               @RequestParam String status,
                               @RequestParam(required = false) String adminResponse,
                               @RequestParam(required = false) String internalNotes,
                               Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null || (!user.isAdmin() && !user.isProfessional() && !user.isForensics())) {
            return "redirect:/access-denied";
        }
        service.updateStatus(id, status, adminResponse, internalNotes);
        auditService.log("WHISTLEBLOWER_UPDATE", "Whistleblower", String.valueOf(id), "Status: " + status);
        return "redirect:/whistleblower/admin/" + id;
    }

    private String detectFileType(String contentType) {
        if (contentType == null) return "FILE";
        if (contentType.startsWith("image/")) return "PHOTO";
        if (contentType.startsWith("video/")) return "VIDEO";
        if (contentType.startsWith("audio/")) return "AUDIO";
        if (contentType.contains("pdf") || contentType.contains("word") ||
            contentType.contains("document") || contentType.contains("text")) return "DOCUMENT";
        return "FILE";
    }
}
