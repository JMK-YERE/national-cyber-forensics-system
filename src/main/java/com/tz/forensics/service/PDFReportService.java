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
import com.tz.forensics.entity.Evidence;
import com.tz.forensics.entity.Incident;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
            Paragraph header = new Paragraph("🇹🇿 NATIONAL CYBER FORENSICS SYSTEM")
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
            doc.add(new Paragraph("📋 INCIDENT DETAILS").setBold().setFontSize(14).setFontColor(CYBER_GREEN));
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
            doc.add(new Paragraph(""));
            doc.add(new Paragraph("📝 DESCRIPTION").setBold().setFontSize(12).setFontColor(CYBER_GREEN));
            doc.add(new Paragraph(incident.getDescription() != null ? incident.getDescription() : "N/A")
                    .setFontSize(10));

            // ===== FINANCIAL LOSS =====
            if (incident.getTotalLossTzs() != null && incident.getTotalLossTzs().doubleValue() > 0) {
                doc.add(new Paragraph(""));
                doc.add(new Paragraph("💰 FINANCIAL LOSS").setBold().setFontSize(12).setFontColor(CYBER_GREEN));

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
            doc.add(new Paragraph("🔬 DIGITAL EVIDENCE (" + (evidenceList != null ? evidenceList.size() : 0) + ")").setBold().setFontSize(12).setFontColor(CYBER_GREEN));

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
                    evTable.addCell(new Cell().add(new Paragraph(Boolean.TRUE.equals(ev.getVerified()) ? "✅ Yes" : "⏳ Pending").setFontSize(9)));
                    String hash = ev.getSha256Hash() != null && ev.getSha256Hash().length() > 30
                            ? ev.getSha256Hash().substring(0, 30) + "..."
                            : ev.getSha256Hash();
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
            doc.add(new Paragraph("National Cyber Forensics System - Tanzania 🇹🇿")
                    .setFontSize(9).setItalic().setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("Confidential — For Official Use Only")
                    .setFontSize(8).setItalic().setFontColor(CYBER_RED).setTextAlignment(TextAlignment.CENTER));

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    // ===== CASE FILE PDF =====
    public byte[] generateCaseReport(CaseFile caseFile) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document doc = new Document(pdfDoc);

            doc.add(new Paragraph("🇹🇿 NATIONAL CYBER FORENSICS SYSTEM")
                    .setFontSize(18).setBold().setFontColor(CYBER_DARK).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("Case File Report").setFontSize(11).setItalic().setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));
            doc.add(new Paragraph("━".repeat(60)).setFontColor(CYBER_GREEN));

            doc.add(new Paragraph("📁 CASE DETAILS").setBold().setFontSize(14).setFontColor(CYBER_GREEN));

            Table table = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                    .setWidth(UnitValue.createPercentValue(100));
            addRow(table, "Case Number", caseFile.getCaseNumber());
            addRow(table, "Title", caseFile.getTitle());
            addRow(table, "Status", caseFile.getStatus());
            addRow(table, "Priority", caseFile.getPriority());
            addRow(table, "Created By", caseFile.getCreatedByName());
            addRow(table, "Created At", caseFile.getCreatedAt() != null ? caseFile.getCreatedAt().format(FMT) : "N/A");
            addRow(table, "Closed At", caseFile.getClosedAt() != null ? caseFile.getClosedAt().format(FMT) : "Pending");
            doc.add(table);

            doc.add(new Paragraph(""));
            doc.add(new Paragraph("📝 DESCRIPTION").setBold().setFontSize(12).setFontColor(CYBER_GREEN));
            doc.add(new Paragraph(caseFile.getDescription() != null ? caseFile.getDescription() : "N/A").setFontSize(10));

            if (caseFile.getClosureReason() != null) {
                doc.add(new Paragraph(""));
                doc.add(new Paragraph("🔒 CLOSURE REASON").setBold().setFontSize(12).setFontColor(CYBER_GREEN));
                doc.add(new Paragraph(caseFile.getClosureReason()).setFontSize(10));
            }

            doc.add(new Paragraph(""));
            doc.add(new Paragraph("━".repeat(60)).setFontColor(CYBER_GREEN));
            doc.add(new Paragraph("Generated: " + java.time.LocalDateTime.now().format(FMT)).setFontSize(9).setItalic());
            doc.add(new Paragraph("Confidential — For Official Use Only").setFontSize(8).setItalic().setFontColor(CYBER_RED).setTextAlignment(TextAlignment.CENTER));

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed", e);
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
