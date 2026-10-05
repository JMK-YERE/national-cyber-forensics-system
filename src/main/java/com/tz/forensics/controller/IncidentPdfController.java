package com.tz.forensics.controller;

import com.tz.forensics.entity.Incident;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.IncidentRepository;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.EvidenceService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.nio.charset.StandardCharsets;

@Controller
public class IncidentPdfController {
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final EvidenceService evidenceService;

    public IncidentPdfController(IncidentRepository incidentRepository, UserRepository userRepository,
                                 EvidenceService evidenceService) {
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.evidenceService = evidenceService;
    }

    @GetMapping("/reports/incident/{id}/pdf")
    public ResponseEntity<ByteArrayResource> download(@PathVariable Long id, Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        Incident incident = incidentRepository.findById(id).orElse(null);
        if (user == null || incident == null || !canAccess(incident, user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String text = "CYBER FORENSICS TZ\n\n"
                + "INCIDENT REPORT\n"
                + "Incident ID: " + safe(incident.getIncidentId()) + "\n"
                + "Title: " + safe(incident.getTitle()) + "\n"
                + "Severity: " + safe(incident.getSeverity()) + "\n"
                + "Workflow Status: " + safe(incident.getWorkflowStatus()) + "\n"
                + "Reporter: " + safe(incident.getReporter()) + "\n"
                + "Reported: " + safe(String.valueOf(incident.getDateReported())) + "\n\n"
                + "Description\n" + safe(incident.getDescription()) + "\n\n"
                + "Evidence items: " + evidenceService.getEvidenceByIncident(id).size();

        byte[] pdf = buildPdf(text);
        ByteArrayResource resource = new ByteArrayResource(pdf);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"incident-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(resource);
    }

    private boolean canAccess(Incident incident, User user) {
        return user.isAdmin()
                || (incident.getReporterUserId() != null && user.getId().equals(incident.getReporterUserId()))
                || (incident.getAssignedTo() != null && user.getId().equals(incident.getAssignedTo()));
    }

    private String safe(String value) {
        if (value == null) return "-";
        return value.replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("\r", " ")
                .replace("\n", " ");
    }

    private byte[] buildPdf(String text) {
        String[] lines = text.split("\\R");
        StringBuilder stream = new StringBuilder("BT\n/F1 10 Tf\n50 780 Td\n");
        for (String line : lines) {
            stream.append("(").append(safe(line)).append(") Tj\n0 -16 Td\n");
        }
        stream.append("ET");

        String body = stream.toString();
        String[] objects = new String[] {
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
                "<< /Length " + body.getBytes(StandardCharsets.US_ASCII).length + " >>\nstream\n" + body + "\nendstream"
        };

        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        int[] offsets = new int[objects.length + 1];

        for (int i = 0; i < objects.length; i++) {
            offsets[i + 1] = pdf.toString().getBytes(StandardCharsets.US_ASCII).length;
            pdf.append(i + 1).append(" 0 obj\n")
                    .append(objects[i]).append("\nendobj\n");
        }

        int xref = pdf.toString().getBytes(StandardCharsets.US_ASCII).length;
        pdf.append("xref\n0 ").append(objects.length + 1)
                .append("\n0000000000 65535 f \n");

        for (int i = 1; i <= objects.length; i++) {
            pdf.append(String.format("%010d 00000 n \n", offsets[i]));
        }

        pdf.append("trailer\n<< /Size ").append(objects.length + 1)
                .append(" /Root 1 0 R >>\nstartxref\n")
                .append(xref).append("\n%%EOF");

        return pdf.toString().getBytes(StandardCharsets.US_ASCII);
    }
}
