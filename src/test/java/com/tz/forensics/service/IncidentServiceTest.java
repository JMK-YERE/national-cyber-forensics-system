package com.tz.forensics.service;

import com.tz.forensics.entity.Incident;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.CaseFileRepository;
import com.tz.forensics.repository.EvidenceRepository;
import com.tz.forensics.repository.IncidentRepository;
import com.tz.forensics.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {
    @Mock IncidentRepository incidents;
    @Mock UserRepository users;
    @Mock CaseFileRepository cases;
    @Mock EvidenceRepository evidence;
    @Mock AuditService auditService;

    @InjectMocks IncidentService service;

    @Test void rejectsSkippingWorkflowStages() {
        Incident i = new Incident();
        i.setId(1L);
        i.setIncidentId("SEC-20261006-101");
        i.setWorkflowStatus("NEW");
        when(incidents.findById(1L)).thenReturn(Optional.of(i));

        assertThrows(IllegalStateException.class,
                () -> service.updateWorkflowStatus(1L, "CLOSED"));
        verify(incidents, never()).save(any());
    }

    @Test void requiresAssignmentBeforeAssignedStage() {
        Incident i = new Incident();
        i.setId(1L);
        i.setIncidentId("SEC-20261006-102");
        i.setWorkflowStatus("TRIAGED");
        when(incidents.findById(1L)).thenReturn(Optional.of(i));

        assertThrows(IllegalStateException.class,
                () -> service.updateWorkflowStatus(1L, "ASSIGNED"));
        verify(incidents, never()).save(any());
    }

    @Test void allowsNormalLifecycleProgression() {
        Incident i = new Incident();
        i.setId(1L);
        i.setIncidentId("SEC-20261006-103");
        i.setWorkflowStatus("NEW");
        when(incidents.findById(1L)).thenReturn(Optional.of(i));

        service.updateWorkflowStatus(1L, "TRIAGED");

        assertEquals("TRIAGED", i.getWorkflowStatus());
        verify(incidents).save(i);
        verify(auditService).log(eq("UPDATE_WORKFLOW"), eq("Incident"),
                eq("SEC-20261006-103"), contains("NEW -> TRIAGED"));
    }

    @Test void assignmentDoesNotDowngradeInvestigatingIncident() {
        Incident i = new Incident();
        i.setId(1L);
        i.setIncidentId("SEC-20261006-104");
        i.setWorkflowStatus("INVESTIGATING");
        when(incidents.findById(1L)).thenReturn(Optional.of(i));

        User assignee = mock(User.class);
        when(assignee.getId()).thenReturn(9L);
        when(assignee.getUsername()).thenReturn("forensics");
        when(assignee.getEnabled()).thenReturn(true);
        when(assignee.isApproved()).thenReturn(true);
        when(assignee.isAdmin()).thenReturn(false);
        when(assignee.isProfessional()).thenReturn(false);
        when(assignee.isForensics()).thenReturn(true);
        when(assignee.isAnalyst()).thenReturn(false);
        when(users.findById(9L)).thenReturn(Optional.of(assignee));

        service.assignIncident(1L, 9L, "forensics", 7L, "HIGH", null);

        assertEquals("INVESTIGATING", i.getWorkflowStatus());
        assertEquals(9L, i.getAssignedTo());
        verify(incidents).save(i);
    }
}
