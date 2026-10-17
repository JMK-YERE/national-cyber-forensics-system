package com.tz.forensics.service;

import com.tz.forensics.entity.ReportAttack;
import com.tz.forensics.repository.ReportAttackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportAttackServiceTest {

    @Mock
    private ReportAttackRepository repository;

    private ReportAttackService service;

    @BeforeEach
    void setUp() {
        service = new ReportAttackService(repository);
    }

    @Test
    void newReportMustStartInNewStatus() {
        ReportAttack report = validReport();
        report.setStatus("INVESTIGATING");

        assertThrows(IllegalArgumentException.class, () -> service.create(report));
        verify(repository, never()).save(any());
    }

    @Test
    void createNormalizesAndPersistsValidReport() {
        ReportAttack report = validReport();
        report.setStatus(" new ");
        report.setPriority(" high ");

        when(repository.existsByReportId(anyString())).thenReturn(false);
        when(repository.save(any(ReportAttack.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReportAttack saved = service.create(report);

        assertEquals("NEW", saved.getStatus());
        assertEquals("HIGH", saved.getPriority());
        assertNotNull(saved.getReportId());
        assertTrue(saved.getReportId().startsWith("SEC-"));
        verify(repository).save(report);
    }

    @Test
    void analystCannotUpdateReportAssignedToAnotherUser() {
        ReportAttack report = new ReportAttack();
        report.setId(10L);
        report.setStatus("NEW");
        report.setAssignedTo(42L);
        when(repository.findById(10L)).thenReturn(Optional.of(report));

        assertThrows(SecurityException.class, () -> service.updateStatus(
                10L, "UNDER_REVIEW", null, null, null, null, 99L, "ANALYST"));
        verify(repository, never()).save(any());
    }

    @Test
    void workflowRejectsSkippingRequiredStages() {
        ReportAttack report = new ReportAttack();
        report.setId(11L);
        report.setStatus("NEW");
        when(repository.findById(11L)).thenReturn(Optional.of(report));

        assertThrows(IllegalStateException.class, () -> service.updateStatus(
                11L, "INVESTIGATING", null, null, null, null, 5L, "ADMIN"));
        verify(repository, never()).save(any());
    }

    @Test
    void closedReportCannotBeReopened() {
        ReportAttack report = new ReportAttack();
        report.setId(12L);
        report.setStatus("CLOSED");
        when(repository.findById(12L)).thenReturn(Optional.of(report));

        assertThrows(IllegalStateException.class, () -> service.updateStatus(
                12L, "UNDER_REVIEW", null, null, null, null, 5L, "ADMIN"));
        verify(repository, never()).save(any());
    }

    @Test
    void validWorkflowTransitionPersistsNormalizedStatus() {
        ReportAttack report = new ReportAttack();
        report.setId(13L);
        report.setStatus("NEW");
        when(repository.findById(13L)).thenReturn(Optional.of(report));
        when(repository.save(any(ReportAttack.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateStatus(13L, " under_review ", null, null, null, null, 5L, "ADMIN");

        assertEquals("UNDER_REVIEW", report.getStatus());
        verify(repository).save(report);
    }

    private ReportAttack validReport() {
        ReportAttack report = new ReportAttack();
        report.setTitle("Suspicious login");
        report.setDescription("Repeated suspicious login attempts from an unknown location.");
        report.setReporterName("Reporter");
        report.setStatus("NEW");
        report.setPriority("MEDIUM");
        return report;
    }
}
