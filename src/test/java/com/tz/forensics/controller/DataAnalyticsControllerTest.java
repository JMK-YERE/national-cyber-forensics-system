package com.tz.forensics.controller;

import com.tz.forensics.entity.ReportAttack;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.ReportAttackRepository;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.ReportAttackService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataAnalyticsControllerTest {
    @Mock ReportAttackRepository reports;
    @Mock UserRepository users;
    @Mock ReportAttackService reportService;
    @Mock Authentication auth;

    @Test
    void analyticsIncludesOnlyOwnedAndAssignedReportsAndDeduplicates() {
        User analyst = new User();
        analyst.setId(7L);
        analyst.setUsername("analyst");
        analyst.setRole("ANALYST");

        ReportAttack owned = report(1L, 7L, null);
        ReportAttack assigned = report(2L, 99L, 7L);

        when(auth.getName()).thenReturn("analyst");
        when(auth.getAuthorities()).thenReturn(List.of(new SimpleGrantedAuthority("ROLE_ANALYST")));
        when(users.findByUsername("analyst")).thenReturn(Optional.of(analyst));
        when(reports.findByUserIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(owned));
        // A report appearing in both query results must only be counted once.
        when(reports.findByAssignedToOrderByCreatedAtDesc(7L)).thenReturn(List.of(owned, assigned));

        Model model = new ExtendedModelMap();
        DataAnalyticsController controller = new DataAnalyticsController(reports, users, reportService);

        assertEquals("analytics", controller.analytics(auth, model));
        assertEquals(2, model.getAttribute("total"));
        assertEquals(0L, model.getAttribute("users"));
        assertEquals(false, model.getAttribute("tenantMetricsAvailable"));
        verify(reports, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void analyticsRejectsRolesOutsideAnalyticsAllowList() {
        when(auth.getAuthorities()).thenReturn(List.of(new SimpleGrantedAuthority("ROLE_INDIVIDUAL")));
        DataAnalyticsController controller = new DataAnalyticsController(reports, users, reportService);

        assertEquals("redirect:/access-denied", controller.analytics(auth, new ExtendedModelMap()));
        verifyNoInteractions(users, reports);
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
