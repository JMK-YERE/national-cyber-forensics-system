package com.tz.forensics.controller;

import com.tz.forensics.dto.IncidentDto;
import com.tz.forensics.entity.Incident;
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
import java.util.Set;

@Controller
@RequestMapping("/incidents")
public class IncidentController {
    private final IncidentService incidentService;
    private final EvidenceService evidenceService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final WhatsAppService whatsAppService;
    private final UserRepository userRepository;
    private final CaseFileService caseFileService;

    public IncidentController(IncidentService incidentService, EvidenceService evidenceService,
                              AuditService auditService, NotificationService notificationService,
                              EmailService emailService, WhatsAppService whatsAppService,
                              UserRepository userRepository,
                              CaseFileService caseFileService) {
        this.incidentService = incidentService;
        this.evidenceService = evidenceService;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.whatsAppService = whatsAppService;
        this.userRepository = userRepository;
        this.caseFileService = caseFileService;
    }

    private boolean isAdmin(User u) { return u != null && u.isAdmin(); }
    private boolean isCyberPro(User u) { return u != null && u.isProfessional(); }
    private boolean isForensics(User u) { return u != null && u.isForensics(); }
    private boolean isAnalyst(User u) { return u != null && u.isAnalyst(); }

    private boolean canViewAll(User u) { return isAdmin(u) || isCyberPro(u) || isForensics(u); }
    private boolean canManageWorkflow(User u) { return isAdmin(u) || isCyberPro(u) || isForensics(u); }
    private boolean canAssign(User u) { return isAdmin(u) || isCyberPro(u); }

    private boolean canAccess(Incident incident, User user) {
        if (incident == null || user == null) return false;
        if (canViewAll(user)) return true;
        return (incident.getReporterUserId() != null && user.getId().equals(incident.getReporterUserId()))
                || (incident.getAssignedTo() != null && user.getId().equals(incident.getAssignedTo()));
    }

    @GetMapping
    public String listIncidents(Model model, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) return "redirect:/login";
        model.addAttribute("incidents", canViewAll(user)
                ? incidentService.getAllIncidents()
                : incidentService.getMyIncidents(user.getId()));
        model.addAttribute("user", user);
        model.addAttribute("isAdmin", isAdmin(user));
        model.addAttribute("isCyberPro", isCyberPro(user));
        model.addAttribute("isForensics", isForensics(user));
        model.addAttribute("isAnalyst", isAnalyst(user));
        return "incidents";
    }

    @GetMapping("/my-tasks")
    public String myTasks(Authentication auth) {
        if (auth == null || auth.getName() == null) return "redirect:/login";
        return "redirect:/case-tasks";
    }

    @GetMapping("/new")
    public String showForm(Model model, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) return "redirect:/login";
        model.addAttribute("incident", new IncidentDto());
        model.addAttribute("user", user);
        return "incident-form";
    }

    @PostMapping("/new")
    public String createIncident(@ModelAttribute("incident") IncidentDto dto, Authentication auth, Model model) {
        Incident saved;
        try {
            saved = incidentService.createIncident(dto, auth.getName());
        } catch (IllegalArgumentException e) {
            auditService.log("REJECT_CREATE_INCIDENT", "Incident", "NEW", e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("incident", dto);
            model.addAttribute("user", userRepository.findByUsername(auth.getName()).orElse(null));
            return "incident-form";
        }
        auditService.log("CREATE_INCIDENT", "Incident", saved.getIncidentId(), "Created incident: " + saved.getTitle());
        try { notificationService.createIncidentNotification(saved.getIncidentId(), saved.getTitle(), saved.getSeverity()); } catch (Exception ignored) {}
        try { emailService.sendIncidentAlert(saved.getIncidentId(), saved.getTitle(), saved.getSeverity()); } catch (Exception ignored) {}
        try { whatsAppService.sendIncidentAlert(saved.getIncidentId(), saved.getTitle(), saved.getSeverity()); } catch (Exception ignored) {}
        model.addAttribute("success", "Incident imehifadhiwa! Incident ID: " + saved.getIncidentId());
        model.addAttribute("incident", new IncidentDto());
        return "incident-form";
    }

    @GetMapping("/{id}")
    public String viewIncident(@PathVariable Long id, Model model, Authentication auth) {
        User currentUser = userRepository.findByUsername(auth.getName()).orElse(null);
        Incident incident = incidentService.getById(id);
        if (!canAccess(incident, currentUser)) return "redirect:/access-denied";

        model.addAttribute("incident", incident);

        // Do not expose forensic evidence from the incident page to an individual reporter.
        // Evidence access must remain case-scoped/staff-scoped through EvidenceController.
        if (isAdmin(currentUser) || isCyberPro(currentUser) || isForensics(currentUser) || isAnalyst(currentUser)) {
            model.addAttribute("evidenceList", evidenceService.getEvidenceByIncident(id));
            model.addAttribute("linkedCases", caseFileService.getCasesByIncident(id));
        } else {
            model.addAttribute("evidenceList", java.util.List.of());
            model.addAttribute("linkedCases", java.util.List.of());
        }

        model.addAttribute("canAssign", canAssign(currentUser));
        model.addAttribute("canManageWorkflow", canManageWorkflow(currentUser));

        if (canAssign(currentUser)) {
            List<User> assignableUsers = userRepository.findAll().stream()
                    .filter(u -> Boolean.TRUE.equals(u.getEnabled()) && u.isApproved()
                            && (u.isProfessional() || u.isForensics() || u.isAdmin() || u.isAnalyst()))
                    .toList();
            model.addAttribute("assignableUsers", assignableUsers);
        }
        return "incident-detail";
    }

    @PostMapping("/{id}/assign")
    public String assignIncident(@PathVariable Long id, @RequestParam Long assignedTo,
                                 @RequestParam(required = false) String priority,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueDate,
                                 Authentication auth) {
        User currentUser = userRepository.findByUsername(auth.getName()).orElse(null);
        User assignedUser = userRepository.findById(assignedTo).orElse(null);
        Incident incident = incidentService.getById(id);
        if (!canAssign(currentUser) || incident == null || assignedUser == null) return "redirect:/access-denied";

        try {
            incidentService.assignIncident(id, assignedTo, assignedUser.getUsername(), currentUser.getId(), priority, dueDate);
        } catch (IllegalArgumentException | IllegalStateException e) {
            auditService.log("REJECT_ASSIGN_INCIDENT", "Incident", String.valueOf(id), e.getMessage());
            return "redirect:/incidents/" + id;
        }

        try {
            notificationService.createNotification(
                    assignedUser.getId(),
                    "📌 Incident assigned to you",
                    incident.getIncidentId() + " — " + incident.getTitle()
                            + (priority != null ? " | Priority: " + priority : ""),
                    "ASSIGNMENT",
                    "/incidents/" + id
            );
        } catch (Exception ignored) {}
        return "redirect:/incidents/" + id;
    }

    @PostMapping("/{id}/workflow")
    public String updateWorkflow(@PathVariable Long id, @RequestParam String status, Authentication auth) {
        User currentUser = userRepository.findByUsername(auth.getName()).orElse(null);
        Incident incident = incidentService.getById(id);
        if (!canManageWorkflow(currentUser) || !canAccess(incident, currentUser)) return "redirect:/access-denied";

        String normalizedStatus = status == null ? "" : status.trim().toUpperCase();
        Set<String> allowedStatuses = Set.of("NEW","TRIAGED","ASSIGNED","INVESTIGATING","CONTAINMENT","ERADICATION","RECOVERY","CLOSED");
        if (!allowedStatuses.contains(normalizedStatus)) {
            auditService.log("REJECT_WORKFLOW", "Incident", String.valueOf(id), "Rejected invalid workflow status: " + status);
            return "redirect:/incidents/" + id;
        }
        try {
            incidentService.updateWorkflowStatus(id, normalizedStatus, currentUser.getId(), currentUser.getUsername(), currentUser.getRole());
        } catch (IllegalArgumentException | IllegalStateException e) {
            auditService.log("REJECT_WORKFLOW", "Incident", String.valueOf(id), e.getMessage());
            return "redirect:/incidents/" + id;
        }

        try {
            String message = incident.getIncidentId() + " — workflow changed to " + normalizedStatus;
            if (incident.getReporterUserId() != null) {
                notificationService.createNotification(
                        incident.getReporterUserId(),
                        "🔄 Incident status updated",
                        message,
                        "WORKFLOW",
                        "/incidents/" + id
                );
            }
            if (incident.getAssignedTo() != null
                    && !incident.getAssignedTo().equals(incident.getReporterUserId())) {
                notificationService.createNotification(
                        incident.getAssignedTo(),
                        "🔄 Assigned incident updated",
                        message,
                        "WORKFLOW",
                        "/incidents/" + id
                );
            }
        } catch (Exception ignored) {}
        return "redirect:/incidents/" + id;
    }

    @GetMapping("/search")
    public String searchPage(@RequestParam(required = false) String incidentId, Model model, Authentication auth) {
        User currentUser = userRepository.findByUsername(auth.getName()).orElse(null);
        if (!(isAdmin(currentUser) || isCyberPro(currentUser) || isForensics(currentUser))) return "redirect:/access-denied";
        if (incidentId != null && !incidentId.isBlank()) {
            model.addAttribute("results", incidentService.searchByIncidentId(incidentId));
            model.addAttribute("searchId", incidentId);
        }
        model.addAttribute("user", currentUser);
        return "search";
    }
}