package com.tz.forensics.controller;

import com.tz.forensics.entity.CaseFile;
import com.tz.forensics.entity.CaseIoc;
import com.tz.forensics.entity.ChainOfCustody;
import com.tz.forensics.entity.CaseTimeline;
import com.tz.forensics.entity.Evidence;
import com.tz.forensics.entity.Incident;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.repository.ChainOfCustodyRepository;
import com.tz.forensics.service.CaseFileService;
import com.tz.forensics.service.CaseIocService;
import com.tz.forensics.service.AuditService;
import com.tz.forensics.service.EvidenceService;
import com.tz.forensics.service.IncidentService;
import com.tz.forensics.service.PDFReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/reports")
public class PDFController {

    private final IncidentService incidentService;
    private final EvidenceService evidenceService;
    private final CaseFileService caseFileService;
    private final PDFReportService pdfReportService;
    private final UserRepository userRepository;
    private final CaseIocService caseIocService;
    private final AuditService auditService;
    private final ChainOfCustodyRepository custodyRepository;

    public PDFController(IncidentService incidentService,
                         EvidenceService evidenceService,
                         CaseFileService caseFileService,
                         PDFReportService pdfReportService,
                         UserRepository userRepository,
                         CaseIocService caseIocService,
                         AuditService auditService,
                         ChainOfCustodyRepository custodyRepository) {
        this.incidentService = incidentService;
        this.evidenceService = evidenceService;
        this.caseFileService = caseFileService;
        this.pdfReportService = pdfReportService;
        this.userRepository = userRepository;
        this.caseIocService = caseIocService;
        this.auditService = auditService;
        this.custodyRepository = custodyRepository;
    }

    @GetMapping("/incident/{id}/pdf")
    public ResponseEntity<byte[]> incidentPdf(@PathVariable Long id, Authentication auth) {
        User user = authenticatedUser(auth);
        Incident incident = incidentService.getById(id);

        if (user == null || incident == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (!isStaff(user) || !canAccessIncident(incident, user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<Evidence> evidenceList = evidenceService.getEvidenceByIncident(id);
        byte[] pdf = pdfReportService.generateIncidentReport(incident, evidenceList);
        auditService.log("EXPORT_INCIDENT_REPORT", "Incident", String.valueOf(id),
                "Incident report exported: " + incident.getIncidentId());
        String filename = safeFilename("incident-" + incident.getIncidentId()) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
                .header("Pragma", "no-cache")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    @GetMapping("/case/{id}/executive.pdf")
    public ResponseEntity<byte[]> executiveCasePdf(@PathVariable Long id, Authentication auth) {
        return caseReport(id, auth, "executive");
    }

    @GetMapping("/case/{id}/technical.pdf")
    public ResponseEntity<byte[]> technicalCasePdf(@PathVariable Long id, Authentication auth) {
        return caseReport(id, auth, "technical");
    }

    @GetMapping("/case/{id}/forensic.pdf")
    public ResponseEntity<byte[]> forensicCasePdf(@PathVariable Long id, Authentication auth) {
        return caseReport(id, auth, "forensic");
    }

    @GetMapping("/case/{id}/pdf")
    public ResponseEntity<byte[]> casePdf(@PathVariable Long id, Authentication auth) {
        User user = authenticatedUser(auth);
        CaseFile caseFile = caseFileService.getById(id);

        if (user == null || caseFile == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Case reports require object-level authorization, not only a staff role.
        if (!canAccessCase(caseFile, user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<CaseTimeline> timeline = caseFileService.getTimeline(id);
        List<CaseIoc> iocs = caseIocService.findByCaseId(id);
        List<Evidence> evidence = evidenceService.getEvidenceByCase(id);
        Map<Long, List<ChainOfCustody>> custody = new HashMap<>();
        for (Evidence item : evidence) {
            if (item.getId() != null) {
                custody.put(item.getId(), custodyRepository.findByEvidenceIdOrderByTimestampDesc(item.getId()));
            }
        }
        byte[] pdf = pdfReportService.generateForensicCaseReport(caseFile, timeline, iocs, evidence, custody);
        auditService.log("EXPORT_CASE_REPORT", "CASE", String.valueOf(id),
                "Report type=standard | Case=" + caseFile.getCaseNumber());
        String filename = safeFilename("case-" + caseFile.getCaseNumber()) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    private ResponseEntity<byte[]> caseReport(Long id, Authentication auth, String type) {
        User user = authenticatedUser(auth);
        CaseFile cf = caseFileService.getById(id);
        if (user == null || cf == null || !canAccessCase(cf, user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<CaseTimeline> timeline = caseFileService.getTimeline(id);
        List<CaseIoc> iocs = caseIocService.findByCaseId(id);
        List<Evidence> evidence = evidenceService.getEvidenceByCase(id);
        Map<Long, List<ChainOfCustody>> custody = new HashMap<>();
        for (Evidence item : evidence) {
            if (item.getId() != null) {
                custody.put(item.getId(), custodyRepository.findByEvidenceIdOrderByTimestampDesc(item.getId()));
            }
        }

        byte[] pdf = switch (type) {
            case "executive" -> pdfReportService.generateExecutiveCaseReport(cf, timeline, iocs, evidence, custody);
            case "technical" -> pdfReportService.generateTechnicalCaseReport(cf, timeline, iocs, evidence, custody);
            default -> pdfReportService.generateForensicCaseReport(cf, timeline, iocs, evidence, custody);
        };

        auditService.log("EXPORT_CASE_REPORT", "CASE",
                String.valueOf(id), "Report type=" + type + " | Case=" + cf.getCaseNumber());

        String filename = safeFilename(type + "-" + cf.getCaseNumber()) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    private String safeFilename(String value) {
        if (value == null || value.isBlank()) return "report";
        return value.replaceAll("[\\r\\n\\\\/\":*?<>|]+", "_").replaceAll("\\s+", " ").trim();
    }

    private User authenticatedUser(Authentication auth) {
        if (auth == null || auth.getName() == null) return null;
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }

    private boolean isStaff(User user) {
        if (user == null || user.getRole() == null) return false;
        String role = user.getRole();
        return "ADMIN".equalsIgnoreCase(role)
                || "CYBER_PRO".equalsIgnoreCase(role)
                || "FORENSICS".equalsIgnoreCase(role)
                || "ANALYST".equalsIgnoreCase(role);
    }

    private boolean canAccessCase(CaseFile caseFile, User user) {
        if (caseFile == null || user == null) return false;
        if (user.isAdmin() || user.isProfessional() || user.isForensics()) return true;
        if (!"ANALYST".equalsIgnoreCase(user.getRole()) || user.getId() == null) return false;

        // Analysts must have access to both the case and its underlying incident.
        Incident incident = caseFile.getIncidentId() == null ? null : incidentService.getById(caseFile.getIncidentId());
        if (incident == null) return false;
        boolean incidentAccess =
                (incident.getReporterUserId() != null && user.getId().equals(incident.getReporterUserId()))
                || (incident.getAssignedTo() != null && user.getId().equals(incident.getAssignedTo()));
        if (!incidentAccess) return false;

        return (caseFile.getCreatedBy() != null && user.getId().equals(caseFile.getCreatedBy()))
                || (caseFile.getAssignedTo() != null && user.getId().equals(caseFile.getAssignedTo()))
                || (caseFile.getLeadInvestigator() != null && user.getId().equals(caseFile.getLeadInvestigator()));
    }

    private boolean canAccessIncident(Incident incident, User user) {
        if (incident == null || user == null || user.getId() == null) return false;
        return user.isAdmin() || user.isProfessional() || user.isForensics()
                || (incident.getReporterUserId() != null && user.getId().equals(incident.getReporterUserId()))
                || (incident.getAssignedTo() != null && user.getId().equals(incident.getAssignedTo()));
    }
}
