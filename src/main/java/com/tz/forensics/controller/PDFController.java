package com.tz.forensics.controller;

import com.tz.forensics.entity.CaseFile;
import com.tz.forensics.entity.Evidence;
import com.tz.forensics.entity.Incident;
import com.tz.forensics.service.CaseFileService;
import com.tz.forensics.service.EvidenceService;
import com.tz.forensics.service.IncidentService;
import com.tz.forensics.service.PDFReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    public PDFController(IncidentService incidentService,
                         EvidenceService evidenceService,
                         CaseFileService caseFileService,
                         PDFReportService pdfReportService) {
        this.incidentService = incidentService;
        this.evidenceService = evidenceService;
        this.caseFileService = caseFileService;
        this.pdfReportService = pdfReportService;
    }

    @GetMapping("/incident/{id}/pdf")
    public ResponseEntity<byte[]> incidentPdf(@PathVariable Long id) {
        Incident incident = incidentService.getById(id);
        if (incident == null) return ResponseEntity.notFound().build();

        List<Evidence> evidenceList = evidenceService.getEvidenceByIncident(id);
        byte[] pdf = pdfReportService.generateIncidentReport(incident, evidenceList);

        String filename = "incident-" + incident.getIncidentId() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/case/{id}/pdf")
    public ResponseEntity<byte[]> casePdf(@PathVariable Long id) {
        CaseFile caseFile = caseFileService.getById(id);
        if (caseFile == null) return ResponseEntity.notFound().build();

        byte[] pdf = pdfReportService.generateCaseReport(caseFile);
        String filename = "case-" + caseFile.getCaseNumber() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
