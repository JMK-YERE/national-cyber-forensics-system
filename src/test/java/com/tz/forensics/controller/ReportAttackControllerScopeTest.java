package com.tz.forensics.controller;

import com.tz.forensics.entity.ReportAttack;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.ReportMessageRepository;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportAttackControllerScopeTest {
    @Mock ReportAttackService service;
    @Mock UserRepository userRepository;
    @Mock AuditService auditService;
    @Mock NotificationService notificationService;
    @Mock ReportMessageRepository messageRepo;
    @Mock AIChatService aiChatService;
    @Mock AdvancedSecurityService advancedSecurityService;
    @Mock EncryptionService encryptionService;
    @Mock EvidenceStorage evidenceStorage;
    @Mock Authentication authentication;

    private ReportAttackController controller;

    @BeforeEach
    void setUp() {
        controller = new ReportAttackController(service, userRepository, auditService,
                notificationService, messageRepo, aiChatService, advancedSecurityService,
                encryptionService, evidenceStorage);
    }

    @Test
    void privilegedRoleSeesOnlyOwnedOrAssignedReportsInExaminationQueue() {
        User admin = user(7L, "ADMIN");
        ReportAttack owned = report(1L, 7L, null);
        ReportAttack assigned = report(2L, 99L, 7L);
        when(authentication.getName()).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(service.getMine(7L)).thenReturn(List.of(owned));
        when(service.getAssignedTo(7L)).thenReturn(List.of(assigned));

        Model model = new ExtendedModelMap();
        assertEquals("report-attack-admin", controller.adminList(authentication, model));

        @SuppressWarnings("unchecked")
        List<ReportAttack> visible = (List<ReportAttack>) model.getAttribute("reports");
        assertEquals(2, visible.size());
        assertTrue(visible.stream().anyMatch(r -> r.getId().equals(1L)));
        assertTrue(visible.stream().anyMatch(r -> r.getId().equals(2L)));
        verify(service, never()).getAll();
        assertEquals(2L, model.getAttribute("totalCount") instanceof Number n ? n.longValue() : -1L);
    }

    @Test
    void privilegedRoleCannotUpdateReportOutsideOwnerOrAssignmentScope() {
        User admin = user(7L, "ADMIN");
        ReportAttack other = report(3L, 99L, 100L);
        when(authentication.getName()).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(service.getById(3L)).thenReturn(other);

        String result = controller.updateStatus(3L, "UNDER_REVIEW", null, null, null, authentication);

        assertEquals("redirect:/access-denied", result);
        verify(service, never()).updateStatus(anyLong(), anyString(), any(), any(), any(), any(), any(), anyString());
        verify(auditService).log(eq("REJECT_UPDATE_REPORT"), eq("ReportAttack"), eq("3"),
                contains("outside their owner/assignment scope"));
    }

    private User user(Long id, String role) {
        User user = new User();
        user.setId(id);
        user.setUsername("test");
        user.setRole(role);
        return user;
    }

    private ReportAttack report(Long id, Long owner, Long assignee) {
        ReportAttack report = new ReportAttack();
        report.setId(id);
        report.setUserId(owner);
        report.setAssignedTo(assignee);
        report.setStatus("NEW");
        return report;
    }
}
