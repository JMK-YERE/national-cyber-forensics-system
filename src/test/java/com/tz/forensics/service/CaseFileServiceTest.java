package com.tz.forensics.service;

import com.tz.forensics.entity.CaseFile;
import com.tz.forensics.repository.CaseFileRepository;
import com.tz.forensics.repository.CaseTimelineRepository;
import com.tz.forensics.repository.IncidentRepository;
import com.tz.forensics.entity.Incident;
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
class CaseFileServiceTest {
    @Mock CaseFileRepository cases;
    @Mock CaseTimelineRepository timeline;
    @Mock IncidentRepository incidents;
    @InjectMocks CaseFileService service;

    @Test void lifecycleRejectsIllegalTransition() {
        CaseFile c = new CaseFile(); c.setId(1L); c.setStatus("OPEN");
        when(cases.findById(1L)).thenReturn(Optional.of(c));
        assertFalse(service.updateStatus(1L,"INVESTIGATING",null,7L,"analyst","ANALYST"));
        verify(cases, never()).save(any());
    }

    @Test void assignedRequiresAssignee() {
        CaseFile c = new CaseFile(); c.setId(1L); c.setStatus("TRIAGED");
        when(cases.findById(1L)).thenReturn(Optional.of(c));
        assertFalse(service.updateStatus(1L,"ASSIGNED",null,7L,"analyst","ANALYST"));
        verify(cases, never()).save(any());
    }

    @Test void closingRequiresReason() {
        CaseFile c = new CaseFile(); c.setId(1L); c.setStatus("REVIEW");
        when(cases.findById(1L)).thenReturn(Optional.of(c));
        assertFalse(service.updateStatus(1L,"CLOSED","",7L,"analyst","ANALYST"));
        verify(cases, never()).save(any());
    }
    @Test void createCaseRejectsMissingIncident() {
        when(incidents.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () ->
                service.createCase(99L, "Valid case", "A sufficiently long description.", "HIGH", 7L, "analyst", "ANALYST"));
        verify(cases, never()).save(any());
    }

    @Test void createCaseRejectsIndividualRole() {
        Incident incident = new Incident();
        when(incidents.findById(1L)).thenReturn(Optional.of(incident));
        assertThrows(IllegalArgumentException.class, () ->
                service.createCase(1L, "Valid case", "A sufficiently long description.", "HIGH", 7L, "user", "INDIVIDUAL"));
        verify(cases, never()).save(any());
    }

    @Test void analystCannotCreateCaseForUnrelatedIncident() {
        Incident incident = new Incident();
        incident.setReporterUserId(8L);
        incident.setAssignedTo(9L);
        when(incidents.findById(1L)).thenReturn(Optional.of(incident));
        assertThrows(IllegalArgumentException.class, () ->
                service.createCase(1L, "Valid case", "A sufficiently long description.", "HIGH", 7L, "analyst", "ANALYST"));
        verify(cases, never()).save(any());
    }

}
