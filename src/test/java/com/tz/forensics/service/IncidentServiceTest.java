package com.tz.forensics.service;

import com.tz.forensics.entity.Incident;
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
    @InjectMocks IncidentService service;

    @Test void rejectsSkippingWorkflowStages() {
        Incident i = new Incident();
        i.setId(1L);
        i.setWorkflowStatus("NEW");
        when(incidents.findById(1L)).thenReturn(Optional.of(i));

        assertThrows(IllegalStateException.class,
                () -> service.updateWorkflowStatus(1L, "CLOSED"));
        verify(incidents, never()).save(any());
    }

    @Test void requiresAssignmentBeforeAssignedStage() {
        Incident i = new Incident();
        i.setId(1L);
        i.setWorkflowStatus("TRIAGED");
        when(incidents.findById(1L)).thenReturn(Optional.of(i));

        assertThrows(IllegalStateException.class,
                () -> service.updateWorkflowStatus(1L, "ASSIGNED"));
        verify(incidents, never()).save(any());
    }

    @Test void allowsNormalLifecycleProgression() {
        Incident i = new Incident();
        i.setId(1L);
        i.setWorkflowStatus("NEW");
        when(incidents.findById(1L)).thenReturn(Optional.of(i));

        service.updateWorkflowStatus(1L, "TRIAGED");

        assertEquals("TRIAGED", i.getWorkflowStatus());
        verify(incidents).save(i);
    }

    @Test void assignmentDoesNotDowngradeInvestigatingIncident() {
        Incident i = new Incident();
        i.setId(1L);
        i.setWorkflowStatus("INVESTIGATING");
        when(incidents.findById(1L)).thenReturn(Optional.of(i));

        service.assignIncident(1L, 9L, "forensics", 7L, "HIGH", null);

        assertEquals("INVESTIGATING", i.getWorkflowStatus());
        assertEquals(9L, i.getAssignedTo());
        verify(incidents).save(i);
    }
}
