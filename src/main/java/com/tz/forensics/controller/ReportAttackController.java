package com.tz.forensics.controller;

import com.tz.forensics.config.CountryConfig;
import com.tz.forensics.entity.ReportAttack;
import com.tz.forensics.entity.ReportMessage;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.ReportMessageRepository;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/report-attack")
public class ReportAttackController {

    private static final Logger log = LoggerFactory.getLogger(ReportAttackController.class);

    private final ReportAttackService service;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final ReportMessageRepository messageRepo;
    private final AIChatService aiChatService;

    @Value("${app.upload.dir:uploads/evidence}")
    private String uploadDir;

    public ReportAttackController(ReportAttackService service,
                                   UserRepository userRepository,
                                   AuditService auditService,
                                   NotificationService notificationService,
                                   ReportMessageRepository messageRepo,
                                   AIChatService aiChatService) {
        this.service = service;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.messageRepo = messageRepo;
        this.aiChatService = aiChatService;
    }

    private boolean canSeeAllReports(User user) {
        if (user == null) return false;
        String role = user.getRole();
        if (role == null) return false;
        return "ADMIN".equalsIgnoreCase(role) || "CYBER_PRO".equalsIgnoreCase(role)
                || "FORENSICS".equalsIgnoreCase(role) || "ANALYST".equalsIgnoreCase(role);
    }

    @GetMapping
    public String showForm(Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        model.addAttribute("user", user);
        model.addAttribute("myReports", service.getMine(user.getId()));
        model.addAttribute("report", new ReportAttack());
        model.addAttribute("canSeeAll", canSeeAllReports(user));
        model.addAttribute("countries", CountryConfig.COUNTRIES.values());
        model.addAttribute("defaultCountry", CountryConfig.getCountry("TZ"));

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
                                @RequestParam(required = false) String specificDetails,
                                @RequestParam(required = false) MultipartFile evidenceFile,
                                Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        report.setUserId(user.getId());
        report.setDateOccurred(dateOccurred);
        report.setSpecificDetails(specificDetails);

        if (country == null || country.isEmpty()) country = "TZ";
        report.setCountry(country);
        report.setCountryName(CountryConfig.getCountry(country).name);

        if (report.getReporterName() == null || report.getReporterName().isBlank())
            report.setReporterName(user.getFullName() != null ? user.getFullName() : user.getUsername());
        if (report.getReporterEmail() == null || report.getReporterEmail().isBlank())
            report.setReporterEmail(user.getEmail());
        if (report.getReporterPhone() == null || report.getReporterPhone().isBlank())
            report.setReporterPhone(user.getPhone());

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
                report.setHasEvidence(true);
            } catch (IOException e) { log.error("File: {}", e.getMessage()); }
        }

        ReportAttack saved = service.create(report);

        try {
            String welcome = "✅ Report Received\n\nAsante kwa kuripoti. Timu yetu itaangalia taarifa yako. Utapata jibu hivi karibuni.";
            messageRepo.save(new ReportMessage(saved.getId(), null, "System", "SYSTEM", welcome));
            
            // Notify admins
            List<User> admins = userRepository.findAll().stream()
                    .filter(u -> u.isAdmin() || u.isProfessional() || u.isForensics()).toList();
            for (User admin : admins) {
                notificationService.createNotification(
                    "🚨 New Report: " + saved.getAttackTypeLabel(),
                    saved.getTitle() + " | " + saved.getReporterName(),
                    "CRITICAL", "/report-attack/view/" + saved.getId()
                );
            }
        } catch (Exception e) { log.error("Notif: {}", e.getMessage()); }

        return "redirect:/report-attack/view/" + saved.getId();
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable Long id, Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        ReportAttack r = service.getById(id);
        if (user == null || r == null) return "redirect:/report-attack";

        boolean isAdmin = canSeeAllReports(user);
        boolean isOwner = r.getUserId() != null && r.getUserId().equals(user.getId());

        if (!isAdmin && !isOwner) return "redirect:/access-denied";

        List<ReportMessage> messages = messageRepo.findByReportIdOrderByCreatedAtAsc(id);
        model.addAttribute("report", r);
        model.addAttribute("user", user);
        model.addAttribute("canSeeAll", isAdmin);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("messages", messages);
        return "report-attack-detail";
    }

    @GetMapping("/evidence/{id}")
    public ResponseEntity<byte[]> downloadEvidence(@PathVariable Long id) throws IOException {
        ReportAttack r = service.getById(id);
        if (r == null || r.getEvidenceFilePath() == null) return ResponseEntity.notFound().build();
        Path filePath = Paths.get(uploadDir, r.getEvidenceFilePath());
        if (!Files.exists(filePath)) return ResponseEntity.notFound().build();
        byte[] data = Files.readAllBytes(filePath);
        String fileName = r.getEvidenceFilePath();
        int idx = fileName.indexOf("_");
        if (idx > 0) fileName = fileName.substring(idx + 1);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @PostMapping("/{id}/reply")
    public String reply(@PathVariable Long id, @RequestParam String message, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        ReportAttack r = service.getById(id);
        if (user == null || r == null) return "redirect:/report-attack";

        String senderType = canSeeAllReports(user) ? "ADMIN" : "USER";
        messageRepo.save(new ReportMessage(id, user.getId(), user.getUsername(), senderType, message));

        // Notify the other party
        if ("ADMIN".equals(senderType)) {
            // Admin replied -> notify user
            if (r.getUserId() != null) {
                notificationService.createNotification(
                    "💬 Admin amejibu Report " + r.getReportId(),
                    message.substring(0, Math.min(80, message.length())),
                    "INFO", "/report-attack/view/" + id
                );
            }
        } else {
            // User replied -> notify admins
            try {
                List<User> admins = userRepository.findAll().stream()
                        .filter(u -> u.isAdmin() || u.isProfessional() || u.isForensics()).toList();
                for (User admin : admins) {
                    notificationService.createNotification(
                        "💬 User amejibu Report " + r.getReportId(),
                        message.substring(0, Math.min(80, message.length())),
                        "INFO", "/report-attack/view/" + id
                    );
                }
            } catch (Exception e) { log.error("Notif: {}", e.getMessage()); }
        }
        return "redirect:/report-attack/view/" + id;
    }

    @PostMapping("/{id}/ai-reply")
    public String triggerAiReply(@PathVariable Long id, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        ReportAttack r = service.getById(id);
        if (user == null || r == null) return "redirect:/report-attack";

        try {
            String prompt = "Report Type: " + r.getAttackTypeLabel() + "\n"
                    + "Title: " + r.getTitle() + "\n"
                    + "Description: " + r.getDescription() + "\n\n"
                    + "Jibu kwa Kiswahili kwa mtindo huu:\n"
                    + "1. Asante kwa kuripoti\n"
                    + "2. Case yako inafanyiwa kazi\n"
                    + "3. Hatua 3-4 za haraka\n"
                    + "4. Namba ya Polisi (112/999)\n"
                    + "Kuwa mfupi, wa kitaalamu, tumia emoji.";

            String aiResponse = aiChatService.chat(prompt, "UserTrigger", "sw");
            messageRepo.save(new ReportMessage(id, null, "AI Assistant", "AI", aiResponse));

            // Notify user
            if (r.getUserId() != null) {
                notificationService.createNotification(
                    "🤖 AI imejibu Report " + r.getReportId(),
                    aiResponse.substring(0, Math.min(80, aiResponse.length())),
                    "INFO", "/report-attack/view/" + id
                );
            }
            log.info("AI reply triggered by {} for report {}", user.getUsername(), r.getReportId());
        } catch (Exception e) {
            log.error("AI trigger failed: {}", e.getMessage());
        }

        return "redirect:/report-attack/view/" + id;
    }

    @GetMapping("/admin")
    public String adminList(Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null || !canSeeAllReports(user)) return "redirect:/access-denied";

        model.addAttribute("reports", service.getAll());
        model.addAttribute("user", user);
        model.addAttribute("newCount", service.countNew());
        model.addAttribute("todayCount", service.countToday());
        model.addAttribute("totalCount", service.countTotal());
        model.addAttribute("assignableUsers", userRepository.findAll().stream()
                .filter(u -> u.isAdmin() || u.isProfessional() || u.isForensics()).toList());
        return "report-attack-admin";
    }

    @PostMapping("/admin/{id}/update")
    public String updateStatus(@PathVariable Long id,
                                @RequestParam String status,
                                @RequestParam(required = false) String adminResponse,
                                @RequestParam(required = false) Long assignedTo,
                                @RequestParam(required = false) String policeCaseNumber,
                                Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null || !canSeeAllReports(user)) return "redirect:/access-denied";

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

        if (adminResponse != null && !adminResponse.isEmpty()) {
            messageRepo.save(new ReportMessage(id, user.getId(), user.getUsername(), "ADMIN", adminResponse));
            ReportAttack r = service.getById(id);
            if (r != null && r.getUserId() != null) {
                notificationService.createNotification(
                    "👤 Admin amejibu Report " + r.getReportId(),
                    adminResponse.substring(0, Math.min(80, adminResponse.length())),
                    "INFO", "/report-attack/view/" + id
                );
            }
        }

        return "redirect:/report-attack/admin";
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
