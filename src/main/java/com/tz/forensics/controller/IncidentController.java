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

    public IncidentController(IncidentService incidentService, EvidenceService evidenceService,
                              AuditService auditService, NotificationService notificationService,
                              EmailService emailService, WhatsAppService whatsAppService,
                              UserRepository userRepository) {
        this.incidentService = incidentService;
        this.evidenceService = evidenceService;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.whatsAppService = whatsAppService;
        this.userRepository = userRepository;
    }

    private boolean isAdmin(User u) { return u != null && u.isAdmin(); }
    private boolean isCyberPro(User u) { return u != null && u.isProfessional(); }
    private boolean isForensics(User u) { return u != null && u.isForensics(); }
    private boolean isAnalyst(User u) { return u != null && u.isAnalyst(); }

    private boolean canViewAll(User u) { return isAdmin(u) || isCyberPro(u); }
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
    public String myTasks(Model model, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) return "redirect:/login";
        model.addAttribute("tasks", incidentService.getMyActiveIncidents(user.getId()));
        model.addAttribute("user", user);
        return "my-tasks";
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
        Incident saved = incidentService.createIncident(dto, auth.getName());
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
        model.addAttribute("evidenceList", evidenceService.getEvidenceByIncident(id));
        model.addAttribute("canAssign", canAssign(currentUser));
        model.addAttribute("canManageWorkflow", canManageWorkflow(currentUser));

        if (canAssign(currentUser)) {
            List<User> assignableUsers = userRepository.findAll().stream()
                    .filter(u -> u.isProfessional() || u.isForensics() || u.isAdmin() || u.isAnalyst())
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

        incidentService.assignIncident(id, assignedTo, assignedUser.getUsername(), currentUser.getId(), priority, dueDate);
        auditService.log("ASSIGN_INCIDENT", "Incident", String.valueOf(id), "Assigned to: " + assignedUser.getUsername());
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
        incidentService.updateWorkflowStatus(id, normalizedStatus);
        auditService.log("UPDATE_WORKFLOW", "Incident", String.valueOf(id), "Workflow: " + normalizedStatus);
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