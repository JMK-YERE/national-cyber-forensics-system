package com.tz.forensics.controller;

import com.tz.forensics.entity.CaseFile;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.AuditService;
import com.tz.forensics.service.CaseFileService;
import com.tz.forensics.service.CaseIocService;
import com.tz.forensics.service.NotificationService;
import com.tz.forensics.service.CaseTaskService;
import com.tz.forensics.service.IncidentService;
import com.tz.forensics.service.EvidenceService;
import com.tz.forensics.entity.Incident;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/cases")
public class CaseFileController {

    private final CaseFileService caseFileService;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final CaseIocService caseIocService;
    private final NotificationService notificationService;
    private final CaseTaskService caseTaskService;
    private final IncidentService incidentService;
    private final EvidenceService evidenceService;

    public CaseFileController(CaseFileService caseFileService,
                              UserRepository userRepository,
                              AuditService auditService, CaseIocService caseIocService,
                              NotificationService notificationService, CaseTaskService caseTaskService,
                              IncidentService incidentService, EvidenceService evidenceService) {
        this.caseFileService = caseFileService;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.caseIocService = caseIocService;
        this.notificationService = notificationService;
        this.caseTaskService = caseTaskService;
        this.incidentService = incidentService;
        this.evidenceService = evidenceService;
    }

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }

    private boolean canManageCases(User user) {
        return user != null && (user.isAdmin() || user.isProfessional() || user.isForensics()
                || "ANALYST".equalsIgnoreCase(user.getRole()));
    }

    private boolean canAssignCases(User user) {
        return user != null && (user.isAdmin() || user.isProfessional() || user.isForensics());
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
        long openCount = visibleCases.stream().filter(cf -> "OPEN".equalsIgnoreCase(cf.getStatus()) || "TRIAGED".equalsIgnoreCase(cf.getStatus()) || "ASSIGNED".equalsIgnoreCase(cf.getStatus())).count();
        long investigatingCount = visibleCases.stream().filter(cf -> "INVESTIGATING".equalsIgnoreCase(cf.getStatus()) || "EXAMINATION".equalsIgnoreCase(cf.getStatus()) || "REVIEW".equalsIgnoreCase(cf.getStatus())).count();
        long closedCount = visibleCases.stream().filter(cf -> "CLOSED".equalsIgnoreCase(cf.getStatus()) || "ARCHIVED".equalsIgnoreCase(cf.getStatus())).count();
        model.addAttribute("openCount", openCount);
        model.addAttribute("investigatingCount", investigatingCount);
        model.addAttribute("closedCount", closedCount);
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

        if (caseFile.getIncidentId() == null) return "redirect:/cases";
        Incident incident = incidentService.getById(caseFile.getIncidentId());
        if (incident == null) return "redirect:/access-denied";

        if ("ANALYST".equalsIgnoreCase(user.getRole())) {
            boolean ownsOrAssigned =
                    (incident.getReporterUserId() != null && user.getId().equals(incident.getReporterUserId()))
                    || (incident.getAssignedTo() != null && user.getId().equals(incident.getAssignedTo()));
            if (!ownsOrAssigned) return "redirect:/access-denied";
        }

        try {
            CaseFile saved = caseFileService.createCase(
                    caseFile.getIncidentId(),
                    caseFile.getTitle(),
                    caseFile.getDescription(),
                    caseFile.getPriority(),
                    user.getId(),
                    auth.getName(),
                    user.getRole()
            );
            auditService.log("CREATE_CASE", "CaseFile", saved.getCaseNumber(), "Created case");
            return "redirect:/cases";
        } catch (IllegalArgumentException e) {
            auditService.log("REJECT_CREATE_CASE", "CaseFile", String.valueOf(caseFile.getIncidentId()),
                    e.getMessage() == null ? "Invalid case submission" : e.getMessage());
            return "redirect:/cases/new?incidentId=" + caseFile.getIncidentId() + "&error=invalid";
        }
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
        model.addAttribute("caseTasks", caseTaskService.findByCaseId(cf.getId()));
        model.addAttribute("caseEvidence", evidenceService.getEvidenceByCase(cf.getId()));
        if (canAssignCases(user)) {
            model.addAttribute("eligibleInvestigators", userRepository.findByRoleInAndEnabledTrueAndApprovalStatusIgnoreCase(
                    List.of("ANALYST", "FORENSICS", "CYBER_PRO"), "APPROVED"));
        }
        return "case-detail";
    }

    @PostMapping("/{id}/assign")
    public String assignCase(@PathVariable Long id, @RequestParam Long investigatorId,
                             @RequestParam(required = false) Long leadInvestigatorId,
                             @RequestParam(required = false) String dueDate, Authentication auth) {
        User actor = getCurrentUser(auth);
        if (!canAssignCases(actor)) return "redirect:/access-denied";
        CaseFile cf = caseFileService.getById(id);
        if (!canViewCase(cf, actor)) return "redirect:/access-denied";
        String current = cf.getStatus() == null ? "OPEN" : cf.getStatus().trim().toUpperCase();
        if ("CLOSED".equals(current) || "ARCHIVED".equals(current)) return "redirect:/cases/" + id;
        if (!"TRIAGED".equals(current) && !"ASSIGNED".equals(current) && !"INVESTIGATING".equals(current)
                && !"EXAMINATION".equals(current) && !"REVIEW".equals(current)) return "redirect:/cases/" + id;
        User investigator = userRepository.findById(investigatorId).orElse(null);
        if (investigator == null || !Boolean.TRUE.equals(investigator.getEnabled()) || !investigator.isApproved()
                || !(investigator.isAnalyst() || investigator.isForensics() || investigator.isProfessional())) {
            auditService.log("REJECT_CASE_ASSIGNMENT", "CaseFile", cf.getCaseNumber(), "Invalid investigator: " + investigatorId);
            return "redirect:/cases/" + id;
        }
        User lead = leadInvestigatorId == null ? investigator : userRepository.findById(leadInvestigatorId).orElse(null);
        if (lead == null || !Boolean.TRUE.equals(lead.getEnabled()) || !lead.isApproved()
                || !(lead.isAnalyst() || lead.isForensics() || lead.isProfessional())) {
            auditService.log("REJECT_CASE_ASSIGNMENT", "CaseFile", cf.getCaseNumber(), "Invalid lead investigator: " + leadInvestigatorId);
            return "redirect:/cases/" + id;
        }
        LocalDateTime deadline = null;
        try { if (dueDate != null && !dueDate.isBlank()) deadline = LocalDateTime.parse(dueDate); }
        catch (Exception e) { auditService.log("REJECT_CASE_ASSIGNMENT", "CaseFile", cf.getCaseNumber(), "Invalid due date"); return "redirect:/cases/" + id; }
        if (deadline != null && deadline.isBefore(LocalDateTime.now())) {
            auditService.log("REJECT_CASE_ASSIGNMENT", "CaseFile", cf.getCaseNumber(), "Due date is in the past");
            return "redirect:/cases/" + id;
        }
        String previousAssignee = cf.getAssignedToName();
        String investigatorName = investigator.getFullName() != null ? investigator.getFullName() : investigator.getUsername();
        String leadName = lead.getFullName() != null ? lead.getFullName() : lead.getUsername();
        try {
            if (!caseFileService.assignCase(id, investigator.getId(), investigatorName,
                    lead.getId(), leadName, deadline, actor.getId(), auth.getName(), actor.getRole())) {
                auditService.log("REJECT_CASE_ASSIGNMENT", "CaseFile", cf.getCaseNumber(), "Case assignment was rejected by the service.");
                return "redirect:/cases/" + id;
            }
        } catch (IllegalStateException e) {
            auditService.log("REJECT_CASE_ASSIGNMENT", "CaseFile", cf.getCaseNumber(), e.getMessage());
            return "redirect:/cases/" + id;
        }
        cf = caseFileService.getById(id);
        auditService.log(previousAssignee == null ? "ASSIGN_CASE" : "REASSIGN_CASE", "CaseFile", cf.getCaseNumber(),
                "Assigned to " + cf.getAssignedToName() + "; lead " + cf.getLeadInvestigatorName()
                        + (deadline != null ? "; due " + deadline : "") + " by " + auth.getName());
        caseFileService.addTimeline(id, previousAssignee == null ? "CASE_ASSIGNED" : "CASE_REASSIGNED",
                previousAssignee == null ? "Case assigned" : "Case reassigned",
                "Investigator: " + cf.getAssignedToName() + " | Lead: " + cf.getLeadInvestigatorName()
                        + (deadline != null ? " | Due: " + deadline : ""), actor.getId(), auth.getName(), actor.getRole());

        String notificationTitle = previousAssignee == null ? "Case assigned to you" : "Case assignment updated";
        String notificationMessage = "Case " + cf.getCaseNumber() + " — " + cf.getTitle()
                + " | Priority: " + cf.getPriority()
                + (deadline != null ? " | Due: " + deadline : "");
        notificationService.createNotification(investigator.getId(), notificationTitle, notificationMessage,
                "CASE_ASSIGNMENT", "/cases/" + id);
        if (!investigator.getId().equals(lead.getId())) {
            notificationService.createNotification(lead.getId(), "You are lead investigator",
                    "Case " + cf.getCaseNumber() + " — " + cf.getTitle()
                            + " | Investigator: " + cf.getAssignedToName()
                            + (deadline != null ? " | Due: " + deadline : ""),
                    "CASE_LEAD", "/cases/" + id);
        }
        return "redirect:/cases/" + id;
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
        if ("CLOSED".equals(to)) {
            notifyCaseClosed(caseFileService.getById(id), user);
        }
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

    private void notifyCaseClosed(CaseFile cf, User actor) {
        java.util.LinkedHashSet<Long> recipientIds = new java.util.LinkedHashSet<>();
        if (cf.getAssignedTo() != null) recipientIds.add(cf.getAssignedTo());
        if (cf.getLeadInvestigator() != null) recipientIds.add(cf.getLeadInvestigator());
        if (cf.getCreatedBy() != null) recipientIds.add(cf.getCreatedBy());
        recipientIds.remove(actor.getId());

        String message = "Case " + cf.getCaseNumber() + " — " + cf.getTitle()
                + " has been closed by " + actor.getUsername() + ".";
        for (Long recipientId : recipientIds) {
            notificationService.createNotification(
                    recipientId,
                    "Case closed",
                    message,
                    "CASE_CLOSED",
                    "/cases/" + cf.getId()
            );
        }
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

        notifyCaseClosed(cf, user);
        return "redirect:/cases/" + id;
    }
}
