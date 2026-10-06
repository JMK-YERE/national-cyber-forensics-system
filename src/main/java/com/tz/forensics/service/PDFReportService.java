package com.tz.forensics.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.tz.forensics.entity.CaseFile;
import com.tz.forensics.entity.ChainOfCustody;
import com.tz.forensics.entity.Evidence;
import com.tz.forensics.entity.Incident;
import com.tz.forensics.entity.CaseIoc;
import com.tz.forensics.entity.CaseTimeline;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class PDFReportService {

    private static final DeviceRgb CYBER_GREEN = new DeviceRgb(0, 200, 120);
    private static final DeviceRgb CYBER_DARK = new DeviceRgb(20, 30, 45);
    private static final DeviceRgb CYBER_RED = new DeviceRgb(220, 50, 50);

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ===== INCIDENT REPORT PDF =====
    public byte[] generateIncidentReport(Incident incident, List<Evidence> evidenceList) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document doc = new Document(pdfDoc);

            // ===== HEADER =====
            Paragraph header = new Paragraph("CYBER FORENSICS TZ")
                    .setFontSize(18).setBold()
                    .setFontColor(CYBER_DARK)
                    .setTextAlignment(TextAlignment.CENTER);
            doc.add(header);

            Paragraph subtitle = new Paragraph("Tanzania Cyber Incident Report")
                    .setFontSize(11).setItalic()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            doc.add(subtitle);

            doc.add(new Paragraph("━".repeat(60)).setFontColor(CYBER_GREEN));

            // ===== INCIDENT DETAILS =====
            doc.add(new Paragraph("INCIDENT DETAILS").setBold().setFontSize(14).setFontColor(CYBER_GREEN));
            doc.add(new Paragraph(""));

            Table table = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                    .setWidth(UnitValue.createPercentValue(100));

            addRow(table, "Incident ID", incident.getIncidentId());
            addRow(table, "Title", incident.getTitle());
            addRow(table, "Reporter", incident.getReporter());
            addRow(table, "Severity", incident.getSeverity());
            addRow(table, "Status", incident.getStatus());
            addRow(table, "Category", incident.getCategory() != null ? incident.getCategory() : "N/A");
            addRow(table, "Region", incident.getRegion() != null ? incident.getRegion() : "N/A");
            addRow(table, "Organization", incident.getOrganization() != null ? incident.getOrganization() : "N/A");
            addRow(table, "Date Reported", incident.getDateReported() != null ? incident.getDateReported().format(FMT) : "N/A");

            doc.add(table);

            // ===== DESCRIPTION =====
            doc.add(new Paragraph("DESCRIPTION").setBold().setFontSize(12).setFontColor(CYBER_GREEN));
            doc.add(new Paragraph(incident.getDescription() != null ? incident.getDescription() : "N/A").setFontSize(10));

            // ===== FINANCIAL LOSS =====
            if (incident.getTotalLossTzs() != null && incident.getTotalLossTzs().doubleValue() > 0) {
                doc.add(new Paragraph(""));
                doc.add(new Paragraph("FINANCIAL LOSS").setBold().setFontSize(12).setFontColor(CYBER_GREEN));

                Table finTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                        .setWidth(UnitValue.createPercentValue(100));

                addRow(finTable, "Direct Loss (TZS)", formatTzs(incident.getDirectLossTzs()));
                addRow(finTable, "Recovery Cost (TZS)", formatTzs(incident.getRecoveryCostTzs()));
                addRow(finTable, "Downtime Cost (TZS)", formatTzs(incident.getDowntimeCostTzs()));
                addRow(finTable, "Legal Fees (TZS)", formatTzs(incident.getLegalFeesTzs()));
                addRow(finTable, "Reputation Damage (TZS)", formatTzs(incident.getReputationDamageTzs()));
                addRow(finTable, "TOTAL LOSS (TZS)", formatTzs(incident.getTotalLossTzs()));

                doc.add(finTable);
            }

            // ===== EVIDENCE =====
            doc.add(new Paragraph(""));
            doc.add(new Paragraph("DIGITAL EVIDENCE (" + (evidenceList != null ? evidenceList.size() : 0) + ")").setBold().setFontSize(12).setFontColor(CYBER_GREEN));

            if (evidenceList != null && !evidenceList.isEmpty()) {
                Table evTable = new Table(UnitValue.createPercentArray(new float[]{25, 15, 15, 45}))
                        .setWidth(UnitValue.createPercentValue(100));

                evTable.addHeaderCell(new Cell().add(new Paragraph("File").setBold().setFontColor(ColorConstants.WHITE)).setBackgroundColor(CYBER_DARK));
                evTable.addHeaderCell(new Cell().add(new Paragraph("Size").setBold().setFontColor(ColorConstants.WHITE)).setBackgroundColor(CYBER_DARK));
                evTable.addHeaderCell(new Cell().add(new Paragraph("Verified").setBold().setFontColor(ColorConstants.WHITE)).setBackgroundColor(CYBER_DARK));
                evTable.addHeaderCell(new Cell().add(new Paragraph("SHA-256").setBold().setFontColor(ColorConstants.WHITE)).setBackgroundColor(CYBER_DARK));

                for (Evidence ev : evidenceList) {
                    evTable.addCell(new Cell().add(new Paragraph(ev.getOriginalFilename()).setFontSize(9)));
                    evTable.addCell(new Cell().add(new Paragraph(ev.getReadableSize()).setFontSize(9)));
                    evTable.addCell(new Cell().add(new Paragraph(Boolean.TRUE.equals(ev.getVerified()) ? "Yes" : "Pending").setFontSize(9)));
                    String hash = ev.getSha256Hash();
                    evTable.addCell(new Cell().add(new Paragraph(hash != null ? hash : "N/A").setFontSize(7)));
                }
                doc.add(evTable);
            } else {
                doc.add(new Paragraph("Hakuna evidence iliyopakiwa.").setFontSize(10).setItalic());
            }

            // ===== FOOTER =====
            doc.add(new Paragraph(""));
            doc.add(new Paragraph("━".repeat(60)).setFontColor(CYBER_GREEN));
            doc.add(new Paragraph("Generated: " + java.time.LocalDateTime.now().format(FMT))
                    .setFontSize(9).setItalic());
            doc.add(new Paragraph("Cyber Forensics TZ")
                    .setFontSize(9).setItalic().setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("Confidential — For Official Use Only")
                    .setFontSize(8).setItalic().setFontColor(CYBER_RED).setTextAlignment(TextAlignment.CENTER));

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed", e);
        }
    }


    public byte[] generateComplianceReport(com.tz.forensics.entity.ComplianceReport report) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfDocument pdfDoc = new PdfDocument(new PdfWriter(baos));
            Document doc = new Document(pdfDoc);
            doc.add(new Paragraph("CYBER FORENSICS TZ").setFontSize(18).setBold().setFontColor(CYBER_DARK).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph(report.getTitle()).setFontSize(14).setBold().setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("Report ID: " + report.getReportId()).setFontSize(10).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph(" "));
            Table table = new Table(UnitValue.createPercentArray(new float[]{45,55})).setWidth(UnitValue.createPercentValue(100));
            addRow(table, "Type", report.getReportTypeLabel());
            addRow(table, "Compliance Score", String.valueOf(report.getComplianceScore()) + "%");
            addRow(table, "Total Controls", String.valueOf(report.getTotalControls()));
            addRow(table, "Passed Controls", String.valueOf(report.getPassedControls()));
            addRow(table, "Failed Controls", String.valueOf(report.getFailedControls()));
            addRow(table, "Status", report.getStatus());
            doc.add(table);
            doc.add(new Paragraph("SUMMARY").setBold().setFontColor(CYBER_GREEN).setMarginTop(18));
            doc.add(new Paragraph(report.getSummary() == null ? "N/A" : report.getSummary()));
            doc.add(new Paragraph("FINDINGS").setBold().setFontColor(CYBER_GREEN).setMarginTop(14));
            doc.add(new Paragraph(report.getFindings() == null ? "N/A" : report.getFindings()));
            doc.add(new Paragraph("RECOMMENDATIONS").setBold().setFontColor(CYBER_GREEN).setMarginTop(14));
            doc.add(new Paragraph(report.getRecommendations() == null ? "N/A" : report.getRecommendations()));
            doc.add(new Paragraph("Generated: " + java.time.LocalDateTime.now().format(FMT)).setFontSize(8).setItalic().setMarginTop(20));
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Compliance PDF generation failed", e);
        }
    }

    public byte[] generateCaseReport(CaseFile caseFile) {
        return generateDetailedCaseReport(caseFile, List.of(), List.of(), List.of(), Map.of(), "Case File Report");
    }

    // ===== CASE FILE PDF =====
    public byte[] generateExecutiveCaseReport(CaseFile caseFile, List<CaseTimeline> timeline, List<CaseIoc> iocs, List<Evidence> evidence, Map<Long, List<ChainOfCustody>> custody) {
        return generateDetailedCaseReport(caseFile, timeline, iocs, evidence, custody, "Executive Case Report");
    }

    public byte[] generateTechnicalCaseReport(CaseFile caseFile, List<CaseTimeline> timeline, List<CaseIoc> iocs, List<Evidence> evidence, Map<Long, List<ChainOfCustody>> custody) {
        return generateDetailedCaseReport(caseFile, timeline, iocs, evidence, custody, "Technical Investigation Report");
    }

    public byte[] generateForensicCaseReport(CaseFile caseFile, List<CaseTimeline> timeline, List<CaseIoc> iocs, List<Evidence> evidence, Map<Long, List<ChainOfCustody>> custody) {
        return generateDetailedCaseReport(caseFile, timeline, iocs, evidence, custody, "Forensic Report");
    }

    private byte[] generateDetailedCaseReport(CaseFile caseFile, List<CaseTimeline> timeline, List<CaseIoc> iocs, List<Evidence> evidence, Map<Long, List<ChainOfCustody>> custody, String reportTitle) {

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfDocument pdfDoc = new PdfDocument(new PdfWriter(baos));
            Document doc = new Document(pdfDoc);

            doc.add(new Paragraph("CYBER FORENSICS TZ").setFontSize(18).setBold().setFontColor(CYBER_DARK).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph(reportTitle).setFontSize(14).setBold().setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("Case Investigation Report").setFontSize(10).setItalic().setTextAlignment(TextAlignment.CENTER).setMarginBottom(16));

            doc.add(new Paragraph("CASE OVERVIEW").setBold().setFontSize(13).setFontColor(CYBER_GREEN));
            Table overview = new Table(UnitValue.createPercentArray(new float[]{30,70})).setWidth(UnitValue.createPercentValue(100));
            addRow(overview, "Case Number", caseFile.getCaseNumber());
            addRow(overview, "Title", caseFile.getTitle());
            addRow(overview, "Status", caseFile.getStatus());
            addRow(overview, "Priority", caseFile.getPriority());
            addRow(overview, "Created By", caseFile.getCreatedByName());
            addRow(overview, "Assigned Investigator", caseFile.getAssignedToName());
            addRow(overview, "Lead Investigator", caseFile.getLeadInvestigatorName());
            addRow(overview, "Due Date", caseFile.getDueDate() != null ? caseFile.getDueDate().format(FMT) : "N/A");
            addRow(overview, "Created At", caseFile.getCreatedAt() != null ? caseFile.getCreatedAt().format(FMT) : "N/A");
            addRow(overview, "Closed At", caseFile.getClosedAt() != null ? caseFile.getClosedAt().format(FMT) : "Pending");
            doc.add(overview);

            doc.add(new Paragraph("DESCRIPTION").setBold().setFontSize(12).setFontColor(CYBER_GREEN).setMarginTop(14));
            doc.add(new Paragraph(caseFile.getDescription() != null ? caseFile.getDescription() : "N/A").setFontSize(10));
            if (caseFile.getClosureReason() != null && !caseFile.getClosureReason().isBlank()) {
                doc.add(new Paragraph("CLOSURE REASON").setBold().setFontSize(12).setFontColor(CYBER_GREEN).setMarginTop(14));
                doc.add(new Paragraph(caseFile.getClosureReason()).setFontSize(10));
            }

            doc.add(new Paragraph("INVESTIGATION TIMELINE").setBold().setFontSize(12).setFontColor(CYBER_GREEN).setMarginTop(16));
            if (timeline != null && !timeline.isEmpty()) {
                Table tt = new Table(UnitValue.createPercentArray(new float[]{22,23,55})).setWidth(UnitValue.createPercentValue(100));
                tt.addHeaderCell(new Cell().add(new Paragraph("Time").setBold().setFontColor(ColorConstants.WHITE)).setBackgroundColor(CYBER_DARK));
                tt.addHeaderCell(new Cell().add(new Paragraph("Event").setBold().setFontColor(ColorConstants.WHITE)).setBackgroundColor(CYBER_DARK));
                tt.addHeaderCell(new Cell().add(new Paragraph("Details").setBold().setFontColor(ColorConstants.WHITE)).setBackgroundColor(CYBER_DARK));
                for (CaseTimeline item : timeline) {
                    tt.addCell(new Paragraph(item.getCreatedAt() != null ? item.getCreatedAt().format(FMT) : "N/A"));
                    tt.addCell(new Paragraph(item.getTitle() != null ? item.getTitle() : "N/A"));
                    tt.addCell(new Paragraph(item.getDetails() != null ? item.getDetails() : "N/A"));
                }
                doc.add(tt);
            } else doc.add(new Paragraph("No timeline events recorded."));

            doc.add(new Paragraph("INDICATORS OF COMPROMISE (IOCs)").setBold().setFontSize(12).setFontColor(CYBER_GREEN).setMarginTop(16));
            if (iocs != null && !iocs.isEmpty()) {
                Table it = new Table(UnitValue.createPercentArray(new float[]{14,42,18,26})).setWidth(UnitValue.createPercentValue(100));
                it.addHeaderCell(new Cell().add(new Paragraph("Type").setBold().setFontColor(ColorConstants.WHITE)).setBackgroundColor(CYBER_DARK));
                it.addHeaderCell(new Cell().add(new Paragraph("Value").setBold().setFontColor(ColorConstants.WHITE)).setBackgroundColor(CYBER_DARK));
                it.addHeaderCell(new Cell().add(new Paragraph("Confidence").setBold().setFontColor(ColorConstants.WHITE)).setBackgroundColor(CYBER_DARK));
                it.addHeaderCell(new Cell().add(new Paragraph("Source").setBold().setFontColor(ColorConstants.WHITE)).setBackgroundColor(CYBER_DARK));
                for (CaseIoc item : iocs) {
                    it.addCell(new Paragraph(item.getIocType() != null ? item.getIocType() : "N/A"));
                    it.addCell(new Paragraph(item.getValue() != null ? item.getValue() : "N/A"));
                    it.addCell(new Paragraph(item.getConfidence() != null ? item.getConfidence() : "N/A"));
                    it.addCell(new Paragraph(item.getSource() != null ? item.getSource() : "N/A"));
                }
                doc.add(it);
            } else doc.add(new Paragraph("No IOCs recorded."));

            doc.add(new Paragraph("DIGITAL EVIDENCE & INTEGRITY").setBold().setFontSize(12).setFontColor(CYBER_GREEN).setMarginTop(16));
            if (evidence != null && !evidence.isEmpty()) {
                Table et = new Table(UnitValue.createPercentArray(new float[]{25,15,16,44})).setWidth(UnitValue.createPercentValue(100));
                et.addHeaderCell(new Cell().add(new Paragraph("File").setBold().setFontColor(ColorConstants.WHITE)).setBackgroundColor(CYBER_DARK));
                et.addHeaderCell(new Cell().add(new Paragraph("Custody").setBold().setFontColor(ColorConstants.WHITE)).setBackgroundColor(CYBER_DARK));
                et.addHeaderCell(new Cell().add(new Paragraph("Integrity").setBold().setFontColor(ColorConstants.WHITE)).setBackgroundColor(CYBER_DARK));
                et.addHeaderCell(new Cell().add(new Paragraph("SHA-256").setBold().setFontColor(ColorConstants.WHITE)).setBackgroundColor(CYBER_DARK));
                for (Evidence item : evidence) {
                    et.addCell(new Paragraph(item.getOriginalFilename() != null ? item.getOriginalFilename() : "N/A"));
                    et.addCell(new Paragraph(item.getCustodyStatus() != null ? item.getCustodyStatus() : "N/A"));
                    et.addCell(new Paragraph(Boolean.TRUE.equals(item.getVerified()) ? "VERIFIED" : "PENDING"));
                    et.addCell(new Paragraph(item.getSha256Hash() != null ? item.getSha256Hash() : "N/A"));
                }
                doc.add(et);
            } else doc.add(new Paragraph("No evidence attached to this incident."));

            doc.add(new Paragraph("CHAIN OF CUSTODY").setBold().setFontSize(12).setFontColor(CYBER_GREEN).setMarginTop(16));
            if (evidence != null && !evidence.isEmpty()) {
                Table ct = new Table(UnitValue.createPercentArray(new float[]{22,18,24,36})).setWidth(UnitValue.createPercentValue(100));
                ct.addHeaderCell(new Cell().add(new Paragraph("Evidence").setBold().setFontColor(ColorConstants.WHITE)).setBackgroundColor(CYBER_DARK));
                ct.addHeaderCell(new Cell().add(new Paragraph("Action").setBold().setFontColor(ColorConstants.WHITE)).setBackgroundColor(CYBER_DARK));
                ct.addHeaderCell(new Cell().add(new Paragraph("Performed By").setBold().setFontColor(ColorConstants.WHITE)).setBackgroundColor(CYBER_DARK));
                ct.addHeaderCell(new Cell().add(new Paragraph("Time / Hash").setBold().setFontColor(ColorConstants.WHITE)).setBackgroundColor(CYBER_DARK));
                for (Evidence item : evidence) {
                    List<ChainOfCustody> events = custody == null ? List.of() : custody.getOrDefault(item.getId(), List.of());
                    if (events.isEmpty()) {
                        ct.addCell(new Paragraph(item.getOriginalFilename() != null ? item.getOriginalFilename() : "N/A"));
                        ct.addCell(new Paragraph(item.getCustodyStatus()));
                        ct.addCell(new Paragraph(item.getCustodianName() != null ? item.getCustodianName() : "N/A"));
                        ct.addCell(new Paragraph("No custody events"));
                    } else {
                        for (ChainOfCustody event : events) {
                            ct.addCell(new Paragraph(item.getOriginalFilename() != null ? item.getOriginalFilename() : "N/A"));
                            ct.addCell(new Paragraph(event.getAction() != null ? event.getAction() : "N/A"));
                            ct.addCell(new Paragraph(event.getPerformedByName() != null ? event.getPerformedByName() : "N/A"));
                            String stamp = event.getTimestamp() != null ? event.getTimestamp().format(FMT) : "N/A";
                            String hash = event.getHashAtAction() != null ? " | " + event.getHashAtAction() : "";
                            ct.addCell(new Paragraph(stamp + hash));
                        }
                    }
                }
                doc.add(ct);
            } else {
                doc.add(new Paragraph("No evidence custody events recorded."));
            }
            doc.add(new Paragraph("Generated: " + java.time.LocalDateTime.now().format(FMT)).setFontSize(9).setItalic().setMarginTop(14));
            doc.add(new Paragraph("Confidential — For Official Use Only").setFontSize(8).setItalic().setFontColor(CYBER_RED).setTextAlignment(TextAlignment.CENTER));
            doc.close();
            return baos.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("PDF generation failed", ex);
        }
    }

    private void addRow(Table table, String label, String value) {
        table.addCell(new Cell()
                .add(new Paragraph(label).setBold().setFontSize(10))
                .setBackgroundColor(new DeviceRgb(240, 240, 240))
                .setBorder(Border.NO_BORDER));
        table.addCell(new Cell()
                .add(new Paragraph(value != null ? value : "N/A").setFontSize(10))
                .setBorder(Border.NO_BORDER));
    }

    private String formatTzs(java.math.BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%,.2f", amount);
    }
}
