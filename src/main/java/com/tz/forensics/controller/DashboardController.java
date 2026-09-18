package com.tz.forensics.controller;

import com.tz.forensics.service.IncidentService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final IncidentService incidentService;

    public DashboardController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        model.addAttribute("username", auth.getName());
        model.addAttribute("role", auth.getAuthorities().iterator().next().getAuthority());
        model.addAttribute("totalIncidents", incidentService.getAllIncidents().size());
        return "dashboard";
    }
}
