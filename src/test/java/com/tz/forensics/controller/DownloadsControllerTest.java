package com.tz.forensics.controller;

import com.tz.forensics.entity.ReportAttack;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.ReportAttackRepository;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DownloadsControllerTest {

    @Mock UserRepository userRepository;
    @Mock ReportAttackRepository reportRepository;
    @Mock AuditService auditService;
    @Mock Authentication authentication;

    private DownloadsController controller;

    @BeforeEach
    void setUp() {
        controller = new DownloadsController(userRepository, reportRepository, auditService);
    }

    @Test
    void unauthenticatedExportIsRejected() {
        ResponseEntity<ByteArrayResource> response = controller.myReportsCsv(null);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(userRepository, reportRepository, auditService);
    }

    @Test
    void missingAccountIsRejected() {
        when(authentication.getName()).thenReturn("missing-user");
        when(userRepository.findByUsername("missing-user")).thenReturn(Optional.empty());

        ResponseEntity<ByteArrayResource> response = controller.myReportsCsv(authentication);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(reportRepository, auditService);
    }

    @Test
    void exportContainsOnlyReportsCreatedByOrAssignedToCurrentUser() {
        User user = new User();
        user.setId(7L);
        when(authentication.getName()).thenReturn("analyst");
        when(userRepository.findByUsername("analyst")).thenReturn(Optional.of(user));

        ReportAttack owned = report(1L, 7L, null, "My report");
        ReportAttack assigned = report(2L, 99L, 7L, "Assigned report");
        when(reportRepository.findByUserIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(owned));
        when(reportRepository.findByAssignedToOrderByCreatedAtDesc(7L)).thenReturn(List.of(assigned));

        ResponseEntity<ByteArrayResource> response = controller.myReportsCsv(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        String csv = new String(response.getBody().getByteArray(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("My report"));
        assertTrue(csv.contains("Assigned report"));
        assertFalse(csv.contains("Other tenant secret"));
        assertTrue(response.getHeaders().getCacheControl().contains("no-cache"));
        verify(auditService).log(eq("EXPORT_REPORTS_CSV"), eq("ReportAttack"),
                eq("user:7"), contains("2"));
    }

    @Test
    void csvEscapesQuotesAndNeutralizesSpreadsheetFormulaPrefixes() {
        User user = new User();
        user.setId(7L);
        when(authentication.getName()).thenReturn("analyst");
        when(userRepository.findByUsername("analyst")).thenReturn(Optional.of(user));
        when(reportRepository.findByUserIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(report(1L, 7L, null, "=SUM(1,1)")));
        when(reportRepository.findByAssignedToOrderByCreatedAtDesc(7L)).thenReturn(List.of());

        ResponseEntity<ByteArrayResource> response = controller.myReportsCsv(authentication);

        String csv = new String(response.getBody().getByteArray(), StandardCharsets.UTF_8);
        assertTrue(csv.contains("\"'=SUM(1,1)\""));
    }

    private ReportAttack report(Long id, Long owner, Long assignee, String title) {
        ReportAttack report = new ReportAttack();
        report.setId(id);
        report.setUserId(owner);
        report.setAssignedTo(assignee);
        report.setReportId("R-" + id);
        report.setTitle(title);
        report.setDescription("Test report");
        report.setReporterName("Test user");
        report.setAttackType("PHISHING");
        report.setStatus("NEW");
        report.setPriority("MEDIUM");
        return report;
    }
}
