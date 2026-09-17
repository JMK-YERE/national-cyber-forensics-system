package com.tz.forensics.controller;

import com.tz.forensics.service.AuditService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/audit")
    public String viewLogs(Model model) {
        model.addAttribute("logs", auditService.getAllLogs());
        return "audit-log";
    }
}
