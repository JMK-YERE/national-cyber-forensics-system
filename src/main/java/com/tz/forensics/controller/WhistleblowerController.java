package com.tz.forensics.controller;

import com.tz.forensics.entity.User;
import com.tz.forensics.entity.WhistleblowerMessage;
import com.tz.forensics.entity.WhistleblowerReport;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.AuditService;
import com.tz.forensics.service.WhistleblowerService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/whistleblower")
public class WhistleblowerController {

    private final WhistleblowerService service;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public WhistleblowerController(WhistleblowerService service,
                                    UserRepository userRepository,
                                    AuditService auditService) {
        this.service = service;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    // ===== PUBLIC PAGES =====

    @GetMapping
    public String home() {
        return "whistleblower-home";
    }

    @GetMapping("/report")
    public String reportForm(Model model) {
        model.addAttribute("report", new WhistleblowerReport());
        return "whistleblower-form";
    }

    @PostMapping("/submit")
    public String submitReport(@ModelAttribute WhistleblowerReport report,
                                @RequestParam(required = false) 
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateOccurred,
                                RedirectAttributes ra) {
        report.setDateOccurred(dateOccurred);

        // NO IP tracking, NO user identification
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
            model.addAttribute("error", "Code haipo au si sahihi. Angalia herufi na namba.");
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
        ra.addFlashAttribute("success", "✅ Ujumbe wako umetumwa kwa admin.");
        return "redirect:/whistleblower/track?code=" + code;
    }

    // ===== ADMIN PAGES =====

    @GetMapping("/admin")
    public String adminList(Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
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
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
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
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null || (!user.isAdmin() && !user.isProfessional())) {
            return "redirect:/access-denied";
        }
        service.updateStatus(id, status, adminResponse, internalNotes);
        auditService.log("WB_UPDATE", "Whistleblower", String.valueOf(id), "Status: " + status);
        return "redirect:/whistleblower/admin/" + id;
    }

    @PostMapping("/admin/{id}/reply")
    public String adminReply(@PathVariable Long id,
                              @RequestParam String message,
                              Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null || (!user.isAdmin() && !user.isProfessional())) {
            return "redirect:/access-denied";
        }
        service.addMessage(id, "ADMIN", message);
        return "redirect:/whistleblower/admin/" + id;
    }
}
