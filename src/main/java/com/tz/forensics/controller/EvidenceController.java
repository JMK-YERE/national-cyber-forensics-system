package com.tz.forensics.controller;

import com.tz.forensics.dto.EvidenceDto;
import com.tz.forensics.entity.Evidence;
import com.tz.forensics.service.AuditService;
import com.tz.forensics.service.EvidenceService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/evidence")
public class EvidenceController {

    private final EvidenceService evidenceService;
    private final AuditService auditService;

    public EvidenceController(EvidenceService evidenceService, AuditService auditService) {
        this.evidenceService = evidenceService;
        this.auditService = auditService;
    }

    @GetMapping("/upload/{incidentId}")
    public String uploadForm(@PathVariable Long incidentId, Model model) {
        model.addAttribute("incidentId", incidentId);
        model.addAttribute("evidence", new EvidenceDto());
        model.addAttribute("evidenceList", evidenceService.getEvidenceByIncident(incidentId));
        return "evidence-upload";
    }

    @PostMapping("/upload/{incidentId}")
    public String uploadEvidence(@PathVariable Long incidentId,
                                 @RequestParam("file") MultipartFile file,
                                 @ModelAttribute("evidence") EvidenceDto dto,
                                 Authentication auth,
                                 Model model) {
        try {
            Evidence saved = evidenceService.uploadEvidence(incidentId, file, dto, auth.getName());
            auditService.log("UPLOAD_EVIDENCE", "Evidence", String.valueOf(saved.getId()),
                    "Uploaded: " + saved.getOriginalFilename());
            model.addAttribute("success", "✅ Evidence imepakiwa! SHA-256: " + saved.getSha256Hash());
        } catch (Exception e) {
            model.addAttribute("error", "❌ Hitilafu: " + e.getMessage());
        }
        model.addAttribute("incidentId", incidentId);
        model.addAttribute("evidence", new EvidenceDto());
        model.addAttribute("evidenceList", evidenceService.getEvidenceByIncident(incidentId));
        return "evidence-upload";
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadEvidence(@PathVariable Long id,
                                                    Authentication auth) throws IOException {
        Evidence evidence = evidenceService.getEvidenceByIncident(id).stream()
                .filter(e -> e.getId().equals(id)).findFirst().orElse(null);

        byte[] data = evidenceService.downloadEvidence(id);
        auditService.log("DOWNLOAD_EVIDENCE", "Evidence", String.valueOf(id),
                "Downloaded evidence");

        String filename = (evidence != null) ? evidence.getOriginalFilename() : "evidence.bin";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @PostMapping("/verify/{id}")
    public String verifyEvidence(@PathVariable Long id, Authentication auth) {
        evidenceService.verifyEvidence(id, auth.getName());
        auditService.log("VERIFY_EVIDENCE", "Evidence", String.valueOf(id), "Verified");
        return "redirect:/incidents";
    }
}
