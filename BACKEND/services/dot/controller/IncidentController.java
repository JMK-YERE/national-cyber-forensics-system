package com.tz.forensics.controller;

import com.tz.forensics.dto.IncidentDto;
import com.tz.forensics.entity.Incident;
import com.tz.forensics.service.AuditService;
import com.tz.forensics.service.EvidenceService;
import com.tz.forensics.service.IncidentService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/incidents")
public class IncidentController {

    private final IncidentService incidentService;
    private final EvidenceService evidenceService;
    private final AuditService auditService;

    public IncidentController(IncidentService incidentService,
                              EvidenceService evidenceService,
                              AuditService auditService) {
        this.incidentService = incidentService;
        this.evidenceService = evidenceService;
        this.auditService = auditService;
    }

    @GetMapping
    public String listIncidents(Model model) {
        model.addAttribute("incidents", incidentService.getAllIncidents());
        return "incidents";
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
        model.addAttribute("success",
                "✅ Tukio limehifadhiwa! Incident ID: " + saved.getIncidentId());
        model.addAttribute("incident", new IncidentDto());
        return "incident-form";
    }

    @GetMapping("/{id}")
    public String viewIncident(@PathVariable Long id, Model model) {
        Incident incident = incidentService.getById(id);
        if (incident == null) return "redirect:/incidents";
        model.addAttribute("incident", incident);
        model.addAttribute("evidenceList", evidenceService.getEvidenceByIncident(id));
        return "incident-detail";
    }

    @GetMapping("/search")
    public String searchPage(@RequestParam(required = false) String incidentId, Model model) {
        if (incidentId != null && !incidentId.isBlank()) {
            model.addAttribute("results", incidentService.searchByIncidentId(incidentId));
            model.addAttribute("searchId", incidentId);
        }
        return "search";
    }
}
