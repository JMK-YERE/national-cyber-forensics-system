package com.tz.forensics.service;

import com.tz.forensics.entity.ComplianceReport;
import com.tz.forensics.repository.ComplianceReportRepository;
import com.tz.forensics.repository.EvidenceRepository;
import com.tz.forensics.repository.IncidentRepository;
import com.tz.forensics.repository.ReportAttackRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ComplianceServiceTest {

    @Test
    void rejectsUnsupportedReportType() {
        ComplianceReportRepository reports = mock(ComplianceReportRepository.class);
        ComplianceService service = new ComplianceService(
                reports, mock(IncidentRepository.class),
                mock(ReportAttackRepository.class), mock(EvidenceRepository.class));

        assertThrows(IllegalArgumentException.class,
                () -> service.generateReport("FAKE", 1L, "admin"));
        verifyNoInteractions(reports);
    }

    @Test
    void rejectsUnsupportedStatus() {
        ComplianceReportRepository reports = mock(ComplianceReportRepository.class);
        ComplianceService service = new ComplianceService(
                reports, mock(IncidentRepository.class),
                mock(ReportAttackRepository.class), mock(EvidenceRepository.class));

        assertThrows(IllegalArgumentException.class,
                () -> service.updateStatus(1L, "COMPROMISED"));
        verifyNoInteractions(reports);
    }

    @Test
    void generatesAllowedReportType() {
        ComplianceReportRepository reports = mock(ComplianceReportRepository.class);
        when(reports.count()).thenReturn(0L);
        when(reports.save(any(ComplianceReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ComplianceService service = new ComplianceService(
                reports, mock(IncidentRepository.class),
                mock(ReportAttackRepository.class), mock(EvidenceRepository.class));

        ComplianceReport result = service.generateReport("iso27001", 1L, "admin");

        org.junit.jupiter.api.Assertions.assertEquals("ISO27001", result.getReportType());
        org.junit.jupiter.api.Assertions.assertEquals("DRAFT", result.getStatus());
        verify(reports).save(any(ComplianceReport.class));
    }
}
