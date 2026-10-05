package com.tz.forensics.controller;

import com.tz.forensics.entity.CaseFile;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.AuditService;
import com.tz.forensics.service.CaseFileService;
import com.tz.forensics.service.CaseIocService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/cases")
public class CaseFileController {

    private final CaseFileService caseFileService;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final CaseIocService caseIocService;

    public CaseFileController(CaseFileService caseFileService,
                              UserRepository userRepository,
                              AuditService auditService, CaseIocService caseIocService) {
        this.caseFileService = caseFileService;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.caseIocService = caseIocService;
    }

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }

    private boolean canManageCases(User user) {
        return user != null && (user.isAdmin() || user.isProfessional() || user.isForensics()
                || "ANALYST".equalsIgnoreCase(user.getRole()));
    }

    private boolean canViewCase(CaseFile cf, User user) {
        if (cf == null || user == null) return false;
        if (user.isAdmin() || user.isProfessional() || user.isForensics()) return true;
        return "ANALYST".equalsIgnoreCase(user.getRole())
                && (user.getId().equals(cf.getCreatedBy())
                    || user.getId().equals(cf.getAssignedTo())
                    || user.getId().equals(cf.getLeadInvestigator()));
    }

    @GetMapping
    public String listCases(Model model, Authentication auth) {
        User user = getCurrentUser(auth);
        if (!canManageCases(user)) return "redirect:/access-denied";

        var visibleCases = caseFileService.getAllCases().stream()
                .filter(cf -> canViewCase(cf, user))
                .toList();
        model.addAttribute("cases", visibleCases);
        model.addAttribute("openCount", caseFileService.countOpen());
        model.addAttribute("investigatingCount", caseFileService.countInvestigating());
        model.addAttribute("closedCount", caseFileService.countClosed());
        return "cases";
    }

    @GetMapping("/new")
    public String newCaseForm(@RequestParam(required = false) Long incidentId, Model model, Authentication auth) {
        User user = getCurrentUser(auth);
        if (!canManageCases(user)) return "redirect:/access-denied";

        CaseFile cf = new CaseFile();
        if (incidentId != null) cf.setIncidentId(incidentId);
        model.addAttribute("caseFile", cf);
        return "case-form";
    }

    @PostMapping("/new")
    public String createCase(@ModelAttribute CaseFile caseFile, Authentication auth) {
        User user = getCurrentUser(auth);
        if (!canManageCases(user)) return "redirect:/access-denied";

        CaseFile saved = caseFileService.createCase(
                caseFile.getIncidentId(),
                caseFile.getTitle(),
                caseFile.getDescription(),
                caseFile.getPriority(),
                user.getId(),
                auth.getName()
        );
        auditService.log("CREATE_CASE", "CaseFile", saved.getCaseNumber(), "Created case");
        return "redirect:/cases";
    }

    @GetMapping({"/{id}", "/view/{id}"})
    public String viewCase(@PathVariable Long id, Model model, Authentication auth) {
        User user = getCurrentUser(auth);
        if (!canManageCases(user)) return "redirect:/access-denied";

        CaseFile cf = caseFileService.getById(id);
        if (!canViewCase(cf, user)) return "redirect:/access-denied";
        model.addAttribute("caseFile", cf);
        model.addAttribute("timeline", caseFileService.getTimeline(cf.getId()));
        model.addAttribute("iocs", caseIocService.findByCaseId(cf.getId()));
        return "case-detail";
    }

    @PostMapping("/{id}/status")
    public String transitionStatus(@PathVariable Long id, @RequestParam String status,
                                    @RequestParam(required = false) String reason,
                                    Authentication auth) {
        User user = getCurrentUser(auth);
        if (!canManageCases(user)) return "redirect:/access-denied";
        CaseFile cf = caseFileService.getById(id);
        if (!canViewCase(cf, user)) return "redirect:/access-denied";

        String from = cf.getStatus() == null ? "OPEN" : cf.getStatus().trim().toUpperCase();
        String to = status == null ? "" : status.trim().toUpperCase();
        if ("CLOSED".equals(from) || "ARCHIVED".equals(from)) return "redirect:/cases/" + id;

        if (!caseFileService.updateStatus(id, to, reason, user.getId(), auth.getName(), user.getRole())) {
            auditService.log("REJECT_CASE_STATUS", "CaseFile", cf.getCaseNumber(),
                    "Rejected lifecycle transition " + from + " → " + to);
            return "redirect:/cases/" + id;
        }
        auditService.log("CHANGE_CASE_STATUS", "CaseFile", cf.getCaseNumber(),
                from + " → " + to + " by " + auth.getName());
        return "redirect:/cases/" + id;
    }

    @PostMapping("/{id}/archive")
    public String archiveCase(@PathVariable Long id, Authentication auth) {
        User user = getCurrentUser(auth);
        if (!canManageCases(user)) return "redirect:/access-denied";
        CaseFile cf = caseFileService.getById(id);
        if (!canViewCase(cf, user)) return "redirect:/access-denied";
        String current = cf.getStatus() == null ? "OPEN" : cf.getStatus().trim().toUpperCase();
        if (!"CLOSED".equals(current)) return "redirect:/cases/" + id;
        if (!caseFileService.updateStatus(id, "ARCHIVED", null, user.getId(), auth.getName(), user.getRole())) {
            auditService.log("REJECT_CASE_STATUS", "CaseFile", cf.getCaseNumber(), "Rejected CLOSED → ARCHIVED");
            return "redirect:/cases/" + id;
        }
        auditService.log("ARCHIVE_CASE", "CaseFile", cf.getCaseNumber(), "Case archived by " + auth.getName());
        return "redirect:/cases/" + id;
    }

    @PostMapping("/{id}/close")
    public String closeCase(@PathVariable Long id, @RequestParam String reason, Authentication auth) {
        User user = getCurrentUser(auth);
        if (!canManageCases(user)) return "redirect:/access-denied";

        CaseFile cf = caseFileService.getById(id);
        if (!canViewCase(cf, user)) return "redirect:/access-denied";

        String currentStatus = cf.getStatus() == null ? "OPEN" : cf.getStatus().toUpperCase();
        if ("CLOSED".equals(currentStatus) || "ARCHIVED".equals(currentStatus)) {
            return "redirect:/cases/" + id;
        }

        if (!caseFileService.updateStatus(id, "CLOSED", reason, user.getId(), auth.getName(), user.getRole())) {
            auditService.log("REJECT_CASE_STATUS", "CaseFile", cf.getCaseNumber(),
                    "Rejected invalid case lifecycle transition from " + currentStatus + " to CLOSED");
            return "redirect:/cases/" + id;
        }
        auditService.log("CLOSE_CASE", "CaseFile", cf.getCaseNumber(),
                "Case closed by " + auth.getName());
        return "redirect:/cases/" + id;
    }
}
