package com.tz.forensics.controller;

import com.tz.forensics.config.CountryConfig;
import com.tz.forensics.entity.User;
import com.tz.forensics.entity.WhistleblowerMessage;
import com.tz.forensics.entity.WhistleblowerReport;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.AuditService;
import com.tz.forensics.service.NotificationService;
import com.tz.forensics.service.WhistleblowerService;
import com.tz.forensics.service.EncryptionService;
import com.tz.forensics.service.EvidenceStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Locale;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/whistleblower")
public class WhistleblowerController {

    private static final Logger log = LoggerFactory.getLogger(WhistleblowerController.class);
    private final Tika tika = new Tika();

    private final WhistleblowerService service;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final EncryptionService encryptionService;
    private final EvidenceStorage evidenceStorage;

    public WhistleblowerController(WhistleblowerService service,
                                    UserRepository userRepository,
                                    AuditService auditService,
                                    NotificationService notificationService,
                                   EncryptionService encryptionService,
                                   EvidenceStorage evidenceStorage) {
        this.service = service;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.encryptionService = encryptionService;
        this.evidenceStorage = evidenceStorage;
    }

    @GetMapping
    public String home(Model model) {
        model.addAttribute("countries", CountryConfig.COUNTRIES.values());
        return "whistleblower-home";
    }

    @GetMapping("/report")
    public String reportForm(@RequestParam(required = false) String category, Model model) {
        WhistleblowerReport report = new WhistleblowerReport();
        if (category != null && !category.isEmpty()) report.setCategory(category);
        model.addAttribute("report", report);
        model.addAttribute("countries", CountryConfig.COUNTRIES.values());
        model.addAttribute("defaultCountry", CountryConfig.getCountry("TZ"));
        return "whistleblower-form";
    }

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

        if (evidenceFile != null && !evidenceFile.isEmpty()) {
            try {
                if (evidenceFile.getSize() > 10 * 1024 * 1024) throw new IOException("File exceeds 10 MB limit.");
                String original = evidenceFile.getOriginalFilename() == null ? "evidence.bin" : evidenceFile.getOriginalFilename();
                String extension = "";
                int dot = original.lastIndexOf(".");
                if (dot >= 0 && dot < original.length() - 1) extension = original.substring(dot).replaceAll("[^A-Za-z0-9.]", "").toLowerCase(Locale.ROOT);
                String storedName = UUID.randomUUID() + extension + ".enc";
                byte[] originalBytes = evidenceFile.getBytes();
                evidenceStorage.write(storedName, encryptionService.encrypt(originalBytes));
                report.setEvidenceFilePath(storedName);
                report.setEvidenceFileType(detectFileType(tika.detect(originalBytes, original)));
                report.setEvidenceFileSize((long) originalBytes.length);
            } catch (IOException e) { log.error("File: {}", e.getMessage()); }
        }

        WhistleblowerReport saved = service.createReport(report);

        // ===== NOTIFY ADMINS =====
        try {
            List<User> admins = userRepository.findAll().stream()
                    .filter(u -> u.isAdmin() || u.isProfessional() || u.isForensics()).toList();
            for (User admin : admins) {
                notificationService.createNotification(
                    admin.getId(),
                    "🕵️ Whistleblower Report: " + saved.getCategoryLabel(),
                    saved.getTitle() + " | Tracking: " + saved.getTrackingCode(),
                    "CRITICAL",
                    "/whistleblower/admin/" + saved.getId()
                );
            }
            log.info("Notified {} admins for WB {}", admins.size(), saved.getTrackingCode());
        } catch (Exception e) { log.error("Notify: {}", e.getMessage()); }

        auditService.log("WHISTLEBLOWER_CREATED", "Whistleblower", saved.getTrackingCode(), "New anonymous report");

        ra.addFlashAttribute("successCode", saved.getTrackingCode());
        return "redirect:/whistleblower/success";
    }

    @GetMapping("/success")
    public String success() { return "whistleblower-success"; }

    @GetMapping("/track")
    public String trackForm() { return "whistleblower-track"; }

    @PostMapping("/track")
    public String trackReport(@RequestParam String code, Model model, HttpSession session) {
        Integer attempts = (Integer) session.getAttribute("WB_TRACK_ATTEMPTS");
        Long windowStart = (Long) session.getAttribute("WB_TRACK_WINDOW_START");
        long now = System.currentTimeMillis();
        if (windowStart == null || now - windowStart > 600_000L) {
            attempts = 0;
            session.setAttribute("WB_TRACK_WINDOW_START", now);
        }
        attempts = attempts == null ? 0 : attempts;
        if (attempts >= 5) {
            model.addAttribute("error", "Too many attempts. Please wait 10 minutes before trying again.");
            return "whistleblower-track";
        }
        session.setAttribute("WB_TRACK_ATTEMPTS", attempts + 1);
        WhistleblowerReport report = service.findByTrackingCode(code);
        if (report == null) {
            model.addAttribute("error", "Tracking code haipo au si sahihi.");
            return "whistleblower-track";
        }
        session.removeAttribute("WB_TRACK_ATTEMPTS");
        session.removeAttribute("WB_TRACK_WINDOW_START");
        // Bind subsequent reporter replies to this verified tracking session and exact report.
        session.setAttribute("WB_VERIFIED_TRACKING_CODE", report.getTrackingCode().trim().toUpperCase(Locale.ROOT));
        session.setAttribute("WB_VERIFIED_REPORT_ID", report.getId());
        List<WhistleblowerMessage> messages = service.getMessages(report.getId());
        model.addAttribute("report", report);
        model.addAttribute("messages", messages);
        return "whistleblower-detail";
    }

    @PostMapping("/track/{id}/reply")
    public String reporterReply(@PathVariable Long id,
                                 @RequestParam String message,
                                 @RequestParam String code,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        // The verified session is the authorization boundary. Do not look up the
        // report or compare the supplied code before this check; that would turn
        // the endpoint into a tracking-code oracle and would permit unnecessary
        // access to report metadata.
        String normalizedCode = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        String sessionCode = (String) session.getAttribute("WB_VERIFIED_TRACKING_CODE");
        Long sessionReportId = (Long) session.getAttribute("WB_VERIFIED_REPORT_ID");
        if (sessionCode == null || !normalizedCode.equals(sessionCode)
                || sessionReportId == null || !id.equals(sessionReportId)) {
            ra.addFlashAttribute("error", "Tracking session imekwisha. Tafadhali thibitisha tracking code tena.");
            return "redirect:/whistleblower/track";
        }

        WhistleblowerReport target = service.getById(id);
        if (target == null) {
            ra.addFlashAttribute("error", "Taarifa haipatikani.");
            return "redirect:/whistleblower/track";
        }
        if ("CLOSED".equalsIgnoreCase(target.getStatus())) {
            ra.addFlashAttribute("error", "Taarifa iliyofungwa haiwezi kupokea ujumbe mpya.");
            return "redirect:/whistleblower/track";
        }
        if (message == null || message.isBlank() || message.length() > 4000) {
            ra.addFlashAttribute("error", "Ujumbe lazima uwe na herufi 1 hadi 4000.");
            return "redirect:/whistleblower/track";
        }
        service.addMessage(id, "REPORTER", message.trim());
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
        return "redirect:/whistleblower/track";
    }

    @GetMapping("/admin")
    public String adminList(Authentication auth, Model model) {
        if (auth == null) return "redirect:/login";
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null || (!user.isAdmin() && !user.isProfessional() && !user.isForensics() && !"ANALYST".equalsIgnoreCase(user.getRole())))
            return "redirect:/access-denied";
        boolean privileged = user.isAdmin() || user.isProfessional() || user.isForensics();
        model.addAttribute("reports", privileged ? service.getAll() : service.getAssignedTo(user.getId()));
        model.addAttribute("newCount", privileged ? service.countNew() : service.countNewAssignedTo(user.getId()));
        model.addAttribute("todayCount", privileged ? service.countToday() : service.countTodayAssignedTo(user.getId()));
        model.addAttribute("user", user);
        return "whistleblower-admin";
    }

    @GetMapping("/admin/{id}")
    public String adminDetail(@PathVariable Long id, Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null || (!user.isAdmin() && !user.isProfessional() && !user.isForensics() && !user.isAnalyst()))
            return "redirect:/access-denied";
        WhistleblowerReport report = service.getById(id);
        if (report == null) return "redirect:/whistleblower/admin";
        if (user.isAnalyst() && (report.getAssignedTo() == null || !user.getId().equals(report.getAssignedTo())))
            return "redirect:/access-denied";
        model.addAttribute("report", report);
        model.addAttribute("messages", service.getMessages(id));
        model.addAttribute("user", user);
        return "whistleblower-admin-detail";
    }

    @GetMapping("/admin/{id}/evidence")
    public ResponseEntity<byte[]> downloadEvidence(@PathVariable Long id, Authentication auth) {
        User user = auth == null ? null : userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null || (!user.isAdmin() && !user.isProfessional() && !user.isForensics() && !user.isAnalyst())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        WhistleblowerReport report = service.getById(id);
        if (report != null && user.isAnalyst() && (report.getAssignedTo() == null || !user.getId().equals(report.getAssignedTo()))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (report == null || report.getEvidenceFilePath() == null || report.getEvidenceFilePath().isBlank()) {
            return ResponseEntity.notFound().build();
        }
        try {
            byte[] stored = evidenceStorage.read(report.getEvidenceFilePath());
            byte[] data = report.getEvidenceFilePath().endsWith(".enc") ? encryptionService.decrypt(stored) : stored;
            auditService.log("DOWNLOAD_WHISTLEBLOWER_EVIDENCE", "Whistleblower", String.valueOf(id), "Evidence downloaded by authorized staff");
            String filename = "whistleblower-evidence-" + id;
            if (report.getEvidenceFileType() != null && report.getEvidenceFileType().equalsIgnoreCase("PHOTO")) filename += ".bin";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(data.length)
                    .body(data);
        } catch (Exception e) {
            log.error("Whistleblower evidence download failed for {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/admin/{id}/update")
    public String adminUpdate(@PathVariable Long id,
                               @RequestParam String status,
                               @RequestParam(required = false) String adminResponse,
                               @RequestParam(required = false) String internalNotes,
                               Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null || (!user.isAdmin() && !user.isProfessional() && !user.isForensics() && !user.isAnalyst()))
            return "redirect:/access-denied";
        WhistleblowerReport target = service.getById(id);
        if (target == null) return "redirect:/whistleblower/admin";
        if (user.isAnalyst() && (target.getAssignedTo() == null || !user.getId().equals(target.getAssignedTo())))
            return "redirect:/access-denied";
        try {
            service.updateStatus(id, status, adminResponse, internalNotes);
        } catch (IllegalArgumentException | IllegalStateException e) {
            auditService.log("REJECT_WHISTLEBLOWER_UPDATE", "Whistleblower", String.valueOf(id), e.getMessage());
            return "redirect:/whistleblower/admin/" + id;
        }
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
