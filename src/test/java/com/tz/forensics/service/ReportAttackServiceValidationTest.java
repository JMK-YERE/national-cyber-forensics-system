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
class ReportAttackServiceValidationTest {

    @Mock
    private ReportAttackRepository repository;

    private ReportAttackService service;

    @BeforeEach
    void setUp() {
        service = new ReportAttackService(repository);
    }

    @Test
    void rejectsBlankOrTooShortTitleBeforePersistence() {
        ReportAttack report = validReport();
        report.setTitle("  ");

        assertThrows(IllegalArgumentException.class, () -> service.create(report));
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsDescriptionThatExceedsLimit() {
        ReportAttack report = validReport();
        report.setDescription("x".repeat(10001));

        assertThrows(IllegalArgumentException.class, () -> service.create(report));
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsUnsupportedPriority() {
        ReportAttack report = validReport();
        report.setPriority("URGENT-OVERRIDE");

        assertThrows(IllegalArgumentException.class, () -> service.create(report));
        verify(repository, never()).save(any());
    }

    @Test
    void updateRequiresAuthenticatedActor() {
        ReportAttack report = new ReportAttack();
        report.setId(31L);
        report.setStatus("NEW");
        when(repository.findById(31L)).thenReturn(Optional.of(report));

        assertThrows(IllegalArgumentException.class, () -> service.updateStatus(
                31L, "UNDER_REVIEW", null, null, null, null, null, "ADMIN"));
        verify(repository, never()).save(any());
    }

    @Test
    void analystCannotReassignAnAssignedReport() {
        ReportAttack report = new ReportAttack();
        report.setId(32L);
        report.setStatus("NEW");
        report.setAssignedTo(7L);
        when(repository.findById(32L)).thenReturn(Optional.of(report));

        assertThrows(SecurityException.class, () -> service.updateStatus(
                32L, "UNDER_REVIEW", null, 8L, "other-analyst", null, 7L, "ANALYST"));
        verify(repository, never()).save(any());
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
