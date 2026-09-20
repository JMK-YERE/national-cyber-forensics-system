package com.tz.forensics.controller;

import com.tz.forensics.entity.CaseFile;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.AuditService;
import com.tz.forensics.service.CaseFileService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/cases")
public class CaseFileController {

    private final CaseFileService caseFileService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public CaseFileController(CaseFileService caseFileService,
                              UserRepository userRepository,
                              AuditService auditService) {
        this.caseFileService = caseFileService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @GetMapping
    public String listCases(Model model) {
        model.addAttribute("cases", caseFileService.getAllCases());
        model.addAttribute("openCount", caseFileService.countOpen());
        model.addAttribute("investigatingCount", caseFileService.countInvestigating());
        model.addAttribute("closedCount", caseFileService.countClosed());
        return "cases";
    }

    @GetMapping("/new")
    public String newCaseForm(@RequestParam(required = false) Long incidentId, Model model) {
        CaseFile cf = new CaseFile();
        if (incidentId != null) cf.setIncidentId(incidentId);
        model.addAttribute("caseFile", cf);
        return "case-form";
    }

    @PostMapping("/new")
    public String createCase(@ModelAttribute CaseFile caseFile,
                             Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        CaseFile saved = caseFileService.createCase(
                caseFile.getIncidentId(),
                caseFile.getTitle(),
                caseFile.getDescription(),
                caseFile.getPriority(),
                user != null ? user.getId() : null,
                auth.getName()
        );
        auditService.log("CREATE_CASE", "CaseFile", saved.getCaseNumber(), "Created case");
        return "redirect:/cases";
    }

    @GetMapping("/{id}")
    public String viewCase(@PathVariable Long id, Model model) {
        CaseFile cf = caseFileService.getById(id);
        if (cf == null) return "redirect:/cases";
        model.addAttribute("caseFile", cf);
        return "case-detail";
    }

    @PostMapping("/{id}/close")
    public String closeCase(@PathVariable Long id, @RequestParam String reason) {
        caseFileService.updateStatus(id, "CLOSED", reason);
        return "redirect:/cases";
    }
}
