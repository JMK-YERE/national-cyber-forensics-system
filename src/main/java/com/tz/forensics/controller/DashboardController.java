package com.tz.forensics.controller;

import com.tz.forensics.entity.Incident;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.CaseFileService;
import com.tz.forensics.service.IncidentService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class DashboardController {

    private final IncidentService incidentService;
    private final UserRepository userRepository;
    private final CaseFileService caseFileService;

    public DashboardController(IncidentService incidentService,
                                UserRepository userRepository,
                                CaseFileService caseFileService) {
        this.incidentService = incidentService;
        this.userRepository = userRepository;
        this.caseFileService = caseFileService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        model.addAttribute("user", user);
        model.addAttribute("username", user.getUsername());
        model.addAttribute("role", user.getRole());

        // ===== DIFFERENT DATA KWA KILA ROLE =====
        if (user.isIndividual()) {
            // Individual: Only their own social media stats
            model.addAttribute("totalIncidents", 0);
        } else {
            // Professionals: All incidents
            List<Incident> allIncidents = incidentService.getAllIncidents();
            model.addAttribute("totalIncidents", allIncidents.size());

            long openCases = caseFileService.countOpen();
            long investigatingCases = caseFileService.countInvestigating();
            long closedCases = caseFileService.countClosed();

            model.addAttribute("openCases", openCases);
            model.addAttribute("investigatingCases", investigatingCases);
            model.addAttribute("closedCases", closedCases);

            // Kwa Cyber Pro / Forensics — my tasks
            if (user.isProfessional() || user.isForensics()) {
                model.addAttribute("myTasks", incidentService.getMyActiveIncidents(user.getId()));
            }
        }

        // ===== ROUTE KWA ROLE =====
        return switch (user.getRole() != null ? user.getRole() : "INDIVIDUAL") {
            case "ADMIN" -> "dashboard-admin";
            case "CYBER_PRO" -> "dashboard-professional";
            case "FORENSICS" -> "dashboard-forensics";
            default -> "dashboard-individual";
        };
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
}
