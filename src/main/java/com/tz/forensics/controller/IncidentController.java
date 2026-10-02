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

    private boolean isStaff(User user) {
        return user != null && (user.isProfessional() || user.isForensics() || user.isAdmin()
                || "ANALYST".equalsIgnoreCase(user.getRole()));
    }

    private boolean canAccess(Incident incident, User user) {
        if (incident == null || user == null) return false;
        return isStaff(user)
                || user.getId().equals(incident.getReporterUserId())
                || user.getId().equals(incident.getAssignedTo());
    }

    private boolean canManage(User user) { return isStaff(user); }

    @GetMapping
    public String listIncidents(Model model, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        model.addAttribute("incidents", user != null && isStaff(user)
                ? incidentService.getAllIncidents()
                : incidentService.getMyIncidents(user != null ? user.getId() : null));
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
    public String showForm(Model model) {
        model.addAttribute("incident", new IncidentDto());
        return "incident-form";
    }

    @PostMapping("/new")
    public String createIncident(@ModelAttribute("incident") IncidentDto dto,
                                 Authentication auth, Model model) {
        Incident saved = incidentService.createIncident(dto, auth.getName());
        auditService.log("CREATE_INCIDENT", "Incident", saved.getIncidentId(),
                "Created incident: " + saved.getTitle());
        try { notificationService.createIncidentNotification(saved.getIncidentId(), saved.getTitle(), saved.getSeverity()); }
        catch (Exception e) { System.err.println("Notif failed: " + e.getMessage()); }
        try { emailService.sendIncidentAlert(saved.getIncidentId(), saved.getTitle(), saved.getSeverity()); }
        catch (Exception e) { System.err.println("Email failed: " + e.getMessage()); }
        try { whatsAppService.sendIncidentAlert(saved.getIncidentId(), saved.getTitle(), saved.getSeverity()); }
        catch (Exception e) { System.err.println("WhatsApp failed: " + e.getMessage()); }

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

        if (canManage(currentUser)) {
            List<User> assignableUsers = userRepository.findAll().stream()
                    .filter(u -> u.isProfessional() || u.isForensics() || u.isAdmin()
                            || "ANALYST".equalsIgnoreCase(u.getRole()))
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
        if (!canManage(currentUser) || incident == null || assignedUser == null) return "redirect:/access-denied";

        incidentService.assignIncident(id, assignedTo, assignedUser.getUsername(),
                currentUser.getId(), priority, dueDate);
        auditService.log("ASSIGN_INCIDENT", "Incident", String.valueOf(id),
                "Assigned to: " + assignedUser.getUsername());
        try {
            notificationService.createNotification("Incident ime-assign kwako",
                    "Una kazi mpya: Incident ID #" + id, "INFO", "/incidents/my-tasks");
        } catch (Exception e) { }
        return "redirect:/incidents/" + id;
    }

    @PostMapping("/{id}/workflow")
    public String updateWorkflow(@PathVariable Long id, @RequestParam String status,
                                 Authentication auth) {
        User currentUser = userRepository.findByUsername(auth.getName()).orElse(null);
        Incident incident = incidentService.getById(id);
        if (!canManage(currentUser) || !canAccess(incident, currentUser)) return "redirect:/access-denied";

        incidentService.updateWorkflowStatus(id, status);
        auditService.log("UPDATE_WORKFLOW", "Incident", String.valueOf(id), "Workflow: " + status);
        return "redirect:/incidents/" + id;
    }

    @GetMapping("/search")
    public String searchPage(@RequestParam(required = false) String incidentId,
                             Model model, Authentication auth) {
        User currentUser = userRepository.findByUsername(auth.getName()).orElse(null);
        if (!isStaff(currentUser)) return "redirect:/access-denied";
        if (incidentId != null && !incidentId.isBlank()) {
            model.addAttribute("results", incidentService.searchByIncidentId(incidentId));
            model.addAttribute("searchId", incidentId);
        }
        return "search";
    }
}