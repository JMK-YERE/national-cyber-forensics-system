package com.tz.forensics.controller;

import com.tz.forensics.entity.CaseFile;
import com.tz.forensics.entity.Evidence;
import com.tz.forensics.entity.Incident;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.CaseFileService;
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

@Controller
@RequestMapping("/reports")
public class PDFController {

    private final IncidentService incidentService;
    private final EvidenceService evidenceService;
    private final CaseFileService caseFileService;
    private final PDFReportService pdfReportService;
    private final UserRepository userRepository;

    public PDFController(IncidentService incidentService,
                         EvidenceService evidenceService,
                         CaseFileService caseFileService,
                         PDFReportService pdfReportService,
                         UserRepository userRepository) {
        this.incidentService = incidentService;
        this.evidenceService = evidenceService;
        this.caseFileService = caseFileService;
        this.pdfReportService = pdfReportService;
        this.userRepository = userRepository;
    }

    @GetMapping("/incident/{id}/pdf")
    public ResponseEntity<byte[]> incidentPdf(@PathVariable Long id, Authentication auth) {
        User user = authenticatedUser(auth);
        Incident incident = incidentService.getById(id);

        if (user == null || incident == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (!canAccessIncident(incident, user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<Evidence> evidenceList = evidenceService.getEvidenceByIncident(id);
        byte[] pdf = pdfReportService.generateIncidentReport(incident, evidenceList);
        String filename = "incident-" + incident.getIncidentId() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
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

        byte[] pdf = pdfReportService.generateCaseReport(caseFile);
        String filename = "case-" + caseFile.getCaseNumber() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
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
        return "ANALYST".equalsIgnoreCase(user.getRole())
                && (user.getId().equals(caseFile.getCreatedBy())
                    || user.getId().equals(caseFile.getAssignedTo())
                    || user.getId().equals(caseFile.getLeadInvestigator()));
    }

    private boolean canAccessIncident(Incident incident, User user) {
        return user.isAdmin()
                || (incident.getReporterUserId() != null && user.getId().equals(incident.getReporterUserId()))
                || (incident.getAssignedTo() != null && user.getId().equals(incident.getAssignedTo()));
    }
}
