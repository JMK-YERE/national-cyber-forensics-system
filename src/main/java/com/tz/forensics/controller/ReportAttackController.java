package com.tz.forensics.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.tz.forensics.config.CountryConfig;
import com.tz.forensics.config.DynamicFieldConfig;
import com.tz.forensics.entity.ReportAttack;
import com.tz.forensics.entity.ReportMessage;
import com.tz.forensics.entity.User;
import com.tz.forensics.enums.IncidentType;
import com.tz.forensics.repository.ReportMessageRepository;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.apache.tika.Tika;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

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
    private final AdvancedSecurityService advancedSecurityService;
    private final EncryptionService encryptionService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Tika tika = new Tika();

    @Value("${app.upload.dir:uploads/evidence}")
    private String uploadDir;

    public ReportAttackController(ReportAttackService service,
                                   UserRepository userRepository,
                                   AuditService auditService,
                                   NotificationService notificationService,
                                   ReportMessageRepository messageRepo,
                                   AIChatService aiChatService,
                                   AdvancedSecurityService advancedSecurityService,
                                   EncryptionService encryptionService) {
        this.service = service;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.messageRepo = messageRepo;
        this.aiChatService = aiChatService;
        this.advancedSecurityService = advancedSecurityService;
        this.encryptionService = encryptionService;
    }

    private boolean canSeeAllReports(User user) {
        if (user == null) return false;
        String role = user.getRole();
        if (role == null) return false;
        return "ADMIN".equalsIgnoreCase(role) || "CYBER_PRO".equalsIgnoreCase(role) || "FORENSICS".equalsIgnoreCase(role);
    }

    private boolean canInteractWithReport(User user, ReportAttack report) {
        if (user == null || report == null) return false;
        return canSeeAllReports(user) || (report.getUserId() != null && report.getUserId().equals(user.getId())) || (report.getAssignedTo() != null && report.getAssignedTo().equals(user.getId()));
    }

    private void notifyAdmins(String title, String message, String type, String linkUrl) {
        try {
            List<User> admins = userRepository.findAll().stream()
                    .filter(u -> u.isAdmin() || u.isProfessional() || u.isForensics()).toList();
            for (User admin : admins) {
                notificationService.createNotification(admin.getId(), title, message, type, linkUrl);
            }
        } catch (Exception e) { log.error("Notify admins failed: {}", e.getMessage()); }
    }

    @GetMapping("/my-reports")
    public String myReports(Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        model.addAttribute("reports", service.getMine(user.getId()));
        return "my-reports";
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
        model.addAttribute("incidentTypes", IncidentType.values());
        model.addAttribute("allFields", DynamicFieldConfig.getAllFields());

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
                                @RequestParam(required = false) String attackType,
                                @RequestParam(required = false) Map<String, String> allParams,
                                @RequestParam(required = false) MultipartFile evidenceFile,
                                Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        report.setUserId(user.getId());
        report.setDateOccurred(dateOccurred);
        report.setAttackType(attackType);
        if (report.getSeverity() != null && !report.getSeverity().isBlank()) report.setPriority(report.getSeverity());

        if (country == null || country.isEmpty()) country = "TZ";
        report.setCountry(country);
        report.setCountryName(CountryConfig.getCountry(country).name);

        if (report.getReporterName() == null || report.getReporterName().isBlank())
            report.setReporterName(user.getFullName() != null ? user.getFullName() : user.getUsername());
        if (report.getReporterEmail() == null || report.getReporterEmail().isBlank())
            report.setReporterEmail(user.getEmail());
        if (report.getReporterPhone() == null || report.getReporterPhone().isBlank())
            report.setReporterPhone(user.getPhone());

        // ===== DYNAMIC DETAILS — collect all dynamic fields =====
        Map<String, Object> dynamicDetails = new LinkedHashMap<>();
        if (attackType != null && allParams != null) {
            try {
                IncidentType type = IncidentType.valueOf(attackType);
                List<DynamicFieldConfig.Field> fields = DynamicFieldConfig.getFields(type);
                for (DynamicFieldConfig.Field f : fields) {
                    String value = allParams.get(f.name);
                    if (value != null && !value.isBlank()) {
                        if ("boolean".equals(f.type)) {
                            dynamicDetails.put(f.name, Boolean.parseBoolean(value));
                        } else {
                            dynamicDetails.put(f.name, value);
                        }
                    }
                }
            } catch (Exception e) { log.error("Dynamic fields parse error: {}", e.getMessage()); }
        }

        // Security-first intake: verify a phishing URL on the server as well as in the browser.
        if ("PHISHING".equalsIgnoreCase(attackType)) {
            String phishingUrl = allParams != null ? allParams.get("phishing_url") : null;
            if (phishingUrl != null && !phishingUrl.isBlank()) {
                try {
                    dynamicDetails.put("url_security_check", advancedSecurityService.checkUrlReputation(phishingUrl));
                } catch (Exception e) {
                    log.warn("URL security check failed: {}", e.getMessage());
                }
            }
        }

        Path writtenEvidencePath = null;

        try {
            report.setDynamicDetails(objectMapper.writeValueAsString(dynamicDetails));
        } catch (Exception e) { log.error("JSON serialize: {}", e.getMessage()); }

        // ===== EVIDENCE =====
        if (evidenceFile != null && !evidenceFile.isEmpty()) {
            try {
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
                if (evidenceFile.getSize() > 50L * 1024L * 1024L) throw new IllegalArgumentException("Evidence file exceeds 50 MB.");
                byte[] evidenceBytes = evidenceFile.getBytes();
                String originalName = evidenceFile.getOriginalFilename() == null ? "evidence.bin" : Paths.get(evidenceFile.getOriginalFilename()).getFileName().toString();
                originalName = originalName.replaceAll("[^A-Za-z0-9._-]", "_");
                if (originalName.length() > 120) originalName = originalName.substring(originalName.length() - 120);
                String storedName = UUID.randomUUID() + "_" + originalName;
                Path targetPath = uploadPath.resolve(storedName).normalize();
                if (!targetPath.getParent().equals(uploadPath.toAbsolutePath().normalize())) throw new IllegalArgumentException("Invalid evidence filename.");
                Files.write(targetPath, encryptionService.encrypt(evidenceBytes));
                writtenEvidencePath = targetPath;

                report.setEvidenceFilePath(storedName);
                String detectedMime = tika.detect(evidenceBytes, originalName);
                report.setEvidenceFileType(detectFileType(detectedMime));
                report.setEvidenceFileSize(evidenceFile.getSize());
                report.setEvidenceSha256(sha256(evidenceBytes));
                report.setHasEvidence(true);
            } catch (IOException | IllegalArgumentException e) { log.error("File: {}", e.getMessage()); } catch (Exception e) { log.error("Evidence encryption failed: {}", e.getMessage()); }
        }

        ReportAttack saved;
        try {
            saved = service.create(report);
        } catch (Exception e) {
            if (writtenEvidencePath != null) {
                try { Files.deleteIfExists(writtenEvidencePath); }
                catch (Exception cleanup) { log.warn("Failed to clean orphan evidence file: {}", cleanup.getMessage()); }
            }
            log.error("Report creation failed: {}", e.getMessage(), e);
            return "redirect:/report-attack?error=save";
        }
        auditService.log("CREATE_REPORT", "ReportAttack", String.valueOf(saved.getId()),
                "Created report " + saved.getReportId() + " | type=" + saved.getAttackType() + " | severity=" + saved.getSeverity());
        if (saved.getHasEvidence() != null && saved.getHasEvidence()) {
            auditService.log("ATTACH_REPORT_EVIDENCE", "ReportAttack", String.valueOf(saved.getId()),
                    "Evidence attached | SHA-256: " + saved.getEvidenceSha256() + " | size=" + saved.getEvidenceFileSize());
        }

        try {
            String welcome = "✅ Report Received\n\nAsante kwa kuripoti. Timu yetu itaangalia taarifa yako. Utapata jibu hivi karibuni.";
            messageRepo.save(new ReportMessage(saved.getId(), null, "System", "SYSTEM", welcome));

            notifyAdmins(
                "🚨 New Report: " + saved.getAttackTypeLabel(),
                saved.getTitle() + " | " + saved.getReporterName(),
                "CRITICAL", "/report-attack/view/" + saved.getId()
            );
        } catch (Exception e) { log.error("Notif: {}", e.getMessage()); }

        return "redirect:/report-attack/view/" + saved.getId();
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable Long id, Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        ReportAttack r = service.getById(id);
        if (user == null || r == null) return "redirect:/report-attack";

        boolean isAdmin = user.isAdmin();
        boolean canSeeAll = canSeeAllReports(user);
        boolean isOwner = r.getUserId() != null && r.getUserId().equals(user.getId());
        boolean isAssignee = r.getAssignedTo() != null && r.getAssignedTo().equals(user.getId());

        if (!canSeeAll && !isOwner && !isAssignee) return "redirect:/access-denied";

        List<ReportMessage> messages = messageRepo.findByReportIdOrderByCreatedAtAsc(id);
        auditService.log("VIEW_REPORT", "ReportAttack", String.valueOf(id), "Viewed report " + r.getReportId());

        // ===== Parse dynamic details for display =====
        Map<String, Object> dynMap = new LinkedHashMap<>();
        if (r.getDynamicDetails() != null) {
            try {
                dynMap = objectMapper.readValue(r.getDynamicDetails(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) { log.error("Parse dyn: {}", e.getMessage()); }
        }

        // ===== Get field labels =====
        List<Map<String, String>> dynFields = new ArrayList<>();
        if (r.getAttackType() != null) {
            try {
                IncidentType type = IncidentType.valueOf(r.getAttackType());
                for (DynamicFieldConfig.Field f : DynamicFieldConfig.getFields(type)) {
                    if (dynMap.containsKey(f.name)) {
                        Map<String, String> item = new HashMap<>();
                        item.put("label", f.label);
                        item.put("value", String.valueOf(dynMap.get(f.name)));
                        dynFields.add(item);
                    }
                }
            } catch (Exception e) { log.error("Fields: {}", e.getMessage()); }
        }

        model.addAttribute("report", r);
        model.addAttribute("user", user);
        model.addAttribute("canSeeAll", canSeeAll);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("isAssignee", isAssignee);
        model.addAttribute("messages", messages);
        model.addAttribute("dynFields", dynFields);
        return "report-attack-detail";
    }

    @GetMapping("/evidence/{id}")
    public ResponseEntity<byte[]> downloadEvidence(@PathVariable Long id, Authentication auth) {
        try {
            if (auth == null || auth.getName() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            User user = userRepository.findByUsername(auth.getName()).orElse(null);
            ReportAttack r = service.getById(id);
            if (user == null || r == null || r.getEvidenceFilePath() == null || r.getEvidenceFilePath().isBlank())
                return ResponseEntity.notFound().build();

            boolean staff = canSeeAllReports(user);
            boolean owner = r.getUserId() != null && r.getUserId().equals(user.getId());
            boolean assignee = r.getAssignedTo() != null && r.getAssignedTo().equals(user.getId());
            if (!staff && !owner && !assignee) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

            Path basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = basePath.resolve(r.getEvidenceFilePath()).normalize();
            if (!filePath.startsWith(basePath) || !Files.isRegularFile(filePath)) {
                log.warn("Evidence file not found for report {}: {}", id, filePath);
                return ResponseEntity.notFound().build();
            }

            byte[] encrypted = Files.readAllBytes(filePath);
            byte[] data = encryptionService.decrypt(encrypted);
            String fileName = Paths.get(r.getEvidenceFilePath()).getFileName().toString();
            int separator = fileName.indexOf('_');
            if (separator >= 0 && separator + 1 < fileName.length()) fileName = fileName.substring(separator + 1);
            fileName = fileName.replaceAll("[^A-Za-z0-9._-]", "_");

            auditService.log("DOWNLOAD_REPORT_EVIDENCE", "ReportAttack", String.valueOf(id),
                    "Downloaded evidence | SHA-256: " + r.getEvidenceSha256());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(data.length)
                    .body(data);
        } catch (java.nio.file.NoSuchFileException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Evidence download failed for report {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/reply")
    public String reply(@PathVariable Long id, @RequestParam String message, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        ReportAttack r = service.getById(id);
        if (!canInteractWithReport(user, r)) return "redirect:/access-denied";
        if (message == null || message.isBlank() || message.length() > 4000) return "redirect:/report-attack/view/" + id;

        String senderType = user.isAdmin() ? "ADMIN" : (canSeeAllReports(user) ? "STAFF" : (r.getAssignedTo() != null && r.getAssignedTo().equals(user.getId()) ? "STAFF" : "USER"));
        messageRepo.save(new ReportMessage(id, user.getId(), user.getUsername(), senderType, message));
        auditService.log("REPORT_REPLY", "ReportAttack", String.valueOf(id), "Reply sent by " + user.getUsername());

        if ("ADMIN".equals(senderType)) {
            if (r.getUserId() != null) {
                notificationService.createNotification(
                    r.getUserId(),
                    "💬 Admin amejibu Report " + r.getReportId(),
                    message.substring(0, Math.min(80, message.length())),
                    "INFO", "/report-attack/view/" + id
                );
            }
        } else {
            notifyAdmins(
                "💬 User amejibu Report " + r.getReportId(),
                message.substring(0, Math.min(80, message.length())),
                "INFO", "/report-attack/view/" + id
            );
        }
        return "redirect:/report-attack/view/" + id;
    }

    @PostMapping("/{id}/ai-reply")
    public String triggerAiReply(@PathVariable Long id, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        ReportAttack r = service.getById(id);
        if (!canInteractWithReport(user, r)) return "redirect:/access-denied";

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

            if (r.getUserId() != null) {
                notificationService.createNotification(
                    r.getUserId(),
                    "🤖 AI imejibu Report " + r.getReportId(),
                    aiResponse.substring(0, Math.min(80, aiResponse.length())),
                    "INFO", "/report-attack/view/" + id
                );
            }
        } catch (Exception e) { log.error("AI: {}", e.getMessage()); }

        return "redirect:/report-attack/view/" + id;
    }

    @GetMapping("/admin")
    public String adminList(Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        // Examination workspace: privileged staff review all reports; analysts only see reports assigned to them.
        if (user == null || !(user.isAdmin() || user.isProfessional() || user.isForensics() || user.isAnalyst())) return "redirect:/access-denied";

        boolean privileged = user.isAdmin() || user.isProfessional() || user.isForensics();
        model.addAttribute("reports", privileged ? service.getAll() : service.getAssignedTo(user.getId()));
        model.addAttribute("user", user);
        model.addAttribute("newCount", privileged ? service.countNew() : service.countNewAssignedTo(user.getId()));
        model.addAttribute("todayCount", privileged ? service.countToday() : service.countTodayAssignedTo(user.getId()));
        model.addAttribute("totalCount", privileged ? service.countTotal() : service.countTotalAssignedTo(user.getId()));
        model.addAttribute("assignableUsers", privileged ? userRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getEnabled()) && u.isApproved()
                        && (u.isProfessional() || u.isForensics() || u.isAnalyst())).toList()
                : List.of());
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
        // Privileged staff may manage any report. Analysts may update only reports assigned to them.
        if (user == null || !(user.isAdmin() || user.isProfessional() || user.isForensics() || user.isAnalyst())) return "redirect:/access-denied";
        ReportAttack target = service.getById(id);
        if (target == null) return "redirect:/report-attack/admin";
        boolean privileged = user.isAdmin() || user.isProfessional() || user.isForensics();
        if (!privileged && (target.getAssignedTo() == null || !target.getAssignedTo().equals(user.getId()))) {
            auditService.log("REJECT_UPDATE_REPORT", "ReportAttack", String.valueOf(id), "Analyst attempted to update an unassigned report.");
            return "redirect:/access-denied";
        }

        // Analysts cannot reassign reports; assignment is a privileged operation.
        if (!privileged && assignedTo != null && !assignedTo.equals(target.getAssignedTo())) {
            auditService.log("REJECT_REASSIGN_REPORT", "ReportAttack", String.valueOf(id), "Analyst attempted to reassign a report.");
            return "redirect:/access-denied";
        }

        String assignedName = null;
        if (assignedTo != null) {
            User assignee = userRepository.findById(assignedTo).orElse(null);
            if (assignee == null
                    || !Boolean.TRUE.equals(assignee.getEnabled())
                    || !assignee.isApproved()
                    || !(assignee.isProfessional() || assignee.isForensics() || assignee.isAnalyst())) {
                auditService.log("REJECT_ASSIGN_REPORT", "ReportAttack", String.valueOf(id),
                        "Attempted assignment to inactive, unapproved, or unauthorized user.");
                return "redirect:/report-attack/admin";
            }
            assignedName = assignee.getUsername();
        }

        try {
            service.updateStatus(id, status, adminResponse, assignedTo, assignedName);
        } catch (IllegalArgumentException | IllegalStateException e) {
            auditService.log("REJECT_UPDATE_REPORT", "ReportAttack", String.valueOf(id), e.getMessage());
            return "redirect:/report-attack/admin";
        }
        auditService.log("UPDATE_REPORT", "ReportAttack", String.valueOf(id),
                "Status=" + status + " | assignedTo=" + assignedTo);

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
                    r.getUserId(),
                    "👤 Admin amejibu Report " + r.getReportId(),
                    adminResponse.substring(0, Math.min(80, adminResponse.length())),
                    "INFO", "/report-attack/view/" + id
                );
            }
        }

        return "redirect:/report-attack/admin";
    }

    private String sha256(byte[] data) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
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
