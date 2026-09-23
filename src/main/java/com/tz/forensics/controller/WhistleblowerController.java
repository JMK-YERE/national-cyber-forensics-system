package com.tz.forensics.controller;

import com.tz.forensics.config.CountryConfig;
import com.tz.forensics.entity.WhistleblowerMessage;
import com.tz.forensics.entity.WhistleblowerReport;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.WhistleblowerService;
import com.tz.forensics.service.AuditService;
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

    private final WhistleblowerService service;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Value("${app.upload.dir:uploads/whistleblower}")
    private String uploadDir;

    public WhistleblowerController(WhistleblowerService service,
                                    UserRepository userRepository,
                                    AuditService auditService) {
        this.service = service;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @GetMapping
    public String home(Model model) {
        model.addAttribute("countries", CountryConfig.COUNTRIES.values());
        return "whistleblower-home";
    }

    @GetMapping("/report")
    public String reportForm(Model model) {
        model.addAttribute("report", new WhistleblowerReport());
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

        // Handle evidence file
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
                System.err.println("File upload failed: " + e.getMessage());
            }
        }

        WhistleblowerReport saved = service.createReport(report);

        ra.addFlashAttribute("successCode", saved.getTrackingCode());
        return "redirect:/whistleblower/success";
    }

    @GetMapping("/success")
    public String success() {
        return "whistleblower-success";
    }

    @GetMapping("/track")
    public String trackForm() {
        return "whistleblower-track";
    }

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

    @PostMapping("/track/{id}/reply")
    public String reporterReply(@PathVariable Long id,
                                 @RequestParam String message,
                                 @RequestParam String code,
                                 RedirectAttributes ra) {
        service.addMessage(id, "REPORTER", message);
        ra.addFlashAttribute("success", "✅ Ujumbe wako umetumwa.");
        return "redirect:/whistleblower/track?code=" + code;
    }

    @GetMapping("/admin")
    public String adminList(Authentication auth, Model model) {
        if (auth == null) return "redirect:/login";
        var user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null || (!user.isAdmin() && !user.isProfessional())) {
            return "redirect:/access-denied";
        }
        model.addAttribute("reports", service.getAll());
        model.addAttribute("newCount", service.countNew());
        model.addAttribute("user", user);
        return "whistleblower-admin";
    }

    @GetMapping("/admin/{id}")
    public String adminDetail(@PathVariable Long id, Authentication auth, Model model) {
        var user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null || (!user.isAdmin() && !user.isProfessional())) {
            return "redirect:/access-denied";
        }
        WhistleblowerReport report = service.getById(id);
        if (report == null) return "redirect:/whistleblower/admin";
        model.addAttribute("report", report);
        model.addAttribute("messages", service.getMessages(id));
        model.addAttribute("user", user);
        return "whistleblower-admin-detail";
    }

    @PostMapping("/admin/{id}/update")
    public String adminUpdate(@PathVariable Long id,
                               @RequestParam String status,
                               @RequestParam(required = false) String adminResponse,
                               @RequestParam(required = false) String internalNotes,
                               Authentication auth) {
        var user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null || (!user.isAdmin() && !user.isProfessional())) {
            return "redirect:/access-denied";
        }
        service.updateStatus(id, status, adminResponse, internalNotes);
        auditService.log("WB_UPDATE", "Whistleblower", String.valueOf(id), "Status: " + status);
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
