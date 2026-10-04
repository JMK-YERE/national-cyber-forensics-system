package com.tz.forensics.controller;

import com.tz.forensics.dto.EvidenceDto;
import com.tz.forensics.entity.Evidence;
import com.tz.forensics.entity.Incident;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.repository.IncidentRepository;
import com.tz.forensics.service.AuditService;
import com.tz.forensics.service.EvidenceService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Controller
@RequestMapping("/evidence")
public class EvidenceController {
    private final EvidenceService evidenceService;
    private final AuditService auditService;
    private final UserRepository userRepository;
    private final IncidentRepository incidentRepository;

    public EvidenceController(EvidenceService evidenceService, AuditService auditService,
                              UserRepository userRepository, IncidentRepository incidentRepository) {
        this.evidenceService = evidenceService;
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.incidentRepository = incidentRepository;
    }

    private User currentUser(Authentication auth) {
        return auth == null ? null : userRepository.findByUsername(auth.getName()).orElse(null);
    }

    private boolean isStaff(User user) {
        return user != null && (user.isAdmin() || user.isProfessional() || user.isForensics()
                || "ANALYST".equalsIgnoreCase(user.getRole()));
    }

    private boolean canAccessIncident(Incident incident, User user) {
        if (incident == null || user == null) return false;
        return user.isAdmin()
                || user.getId().equals(incident.getReporterUserId())
                || user.getId().equals(incident.getAssignedTo());
    }

    @GetMapping("/upload/{incidentId}")
    public String uploadForm(@PathVariable Long incidentId, Authentication auth, Model model) {
        User user = currentUser(auth);
        Incident incident = incidentRepository.findById(incidentId).orElse(null);
        if (!canAccessIncident(incident, user)) return "redirect:/access-denied";
        model.addAttribute("incidentId", incidentId);
        model.addAttribute("evidence", new EvidenceDto());
        model.addAttribute("evidenceList", evidenceService.getEvidenceByIncident(incidentId));
        return "evidence-upload";
    }

    @PostMapping("/upload/{incidentId}")
    public String uploadEvidence(@PathVariable Long incidentId,
                                 @RequestParam("file") MultipartFile file,
                                 @ModelAttribute("evidence") EvidenceDto dto,
                                 Authentication auth, Model model) {
        User user = currentUser(auth);
        Incident incident = incidentRepository.findById(incidentId).orElse(null);
        if (!canAccessIncident(incident, user)) return "redirect:/access-denied";

        try {
            if (file == null || file.isEmpty()) throw new IllegalArgumentException("Chagua evidence file kwanza.");
            Evidence saved = evidenceService.uploadEvidence(incidentId, file, dto, auth.getName(), user.getId());
            auditService.log("UPLOAD_EVIDENCE", "Evidence", String.valueOf(saved.getId()),
                    "Uploaded: " + saved.getOriginalFilename() + " | SHA-256: " + saved.getSha256Hash());
            model.addAttribute("success", "Evidence imepakiwa! SHA-256: " + saved.getSha256Hash());
        } catch (Exception e) {
            model.addAttribute("error", "Hitilafu: " + e.getMessage());
        }
        model.addAttribute("incidentId", incidentId);
        model.addAttribute("evidence", new EvidenceDto());
        model.addAttribute("evidenceList", evidenceService.getEvidenceByIncident(incidentId));
        return "evidence-upload";
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadEvidence(@PathVariable Long id, Authentication auth) throws IOException {
        User user = currentUser(auth);
        Evidence evidence = evidenceService.getById(id);
        if (evidence == null) return ResponseEntity.notFound().build();

        Incident incident = incidentRepository.findById(evidence.getIncidentId()).orElse(null);
        if (!canAccessIncident(incident, user)) return ResponseEntity.status(403).build();

        byte[] data;
        try { data = evidenceService.downloadEvidence(id); }
        catch (IOException e) { return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); }
        catch (RuntimeException e) { return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); }
        auditService.log("DOWNLOAD_EVIDENCE", "Evidence", String.valueOf(id),
                "Downloaded | SHA-256: " + evidence.getSha256Hash());

        String filename = evidence.getOriginalFilename() == null ? "evidence.bin"
                : evidence.getOriginalFilename().replace("\"", "_");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @PostMapping("/verify/{id}")
    public String verifyEvidence(@PathVariable Long id, Authentication auth) {
        User user = currentUser(auth);
        Evidence evidence = evidenceService.getById(id);
        if (evidence == null) return "redirect:/incidents";
        if (!isStaff(user)) return "redirect:/access-denied";

        evidenceService.verifyEvidence(id, auth.getName(), user.getId());
        auditService.log("VERIFY_EVIDENCE", "Evidence", String.valueOf(id),
                "Verified | SHA-256: " + evidence.getSha256Hash());
        return "redirect:/incidents/" + evidence.getIncidentId();
    }
}