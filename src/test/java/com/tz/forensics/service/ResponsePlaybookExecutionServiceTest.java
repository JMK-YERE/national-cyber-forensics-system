package com.tz.forensics.service;

import com.tz.forensics.entity.CaseFile;
import com.tz.forensics.entity.Incident;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.ResponsePlaybookRunRepository;
import com.tz.forensics.repository.ResponsePlaybookStepRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResponsePlaybookExecutionServiceTest {
    @Mock private ResponsePlaybookRunRepository runs;
    @Mock private ResponsePlaybookStepRunRepository steps;
    @Mock private CaseFileService cases;
    @Mock private IncidentService incidents;

    private ResponsePlaybookExecutionService service;

    @BeforeEach
    void setUp() {
        service = new ResponsePlaybookExecutionService(
            runs, steps, new ResponsePlaybookService(), cases, incidents);
    }

    @Test
    void rejectsNonStaffBeforeLoadingCase() {
        User individual = new User();
        individual.setId(12L);
        individual.setRole("INDIVIDUAL");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.listRuns(5L, individual));

        assertEquals(403, error.getStatusCode().value());
        verifyNoInteractions(cases, incidents, runs, steps);
    }

    @Test
    void deniesAnalystWhoIsNotConnectedToUnderlyingIncidentAndCase() {
        User analyst = new User();
        analyst.setId(12L);
        analyst.setRole("ANALYST");
        CaseFile caseFile = new CaseFile();
        caseFile.setIncidentId(44L);
        Incident incident = new Incident();
        when(cases.getById(5L)).thenReturn(caseFile);
        when(incidents.getById(44L)).thenReturn(incident);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.listRuns(5L, analyst));

        assertEquals(403, error.getStatusCode().value());
        verify(runs, never()).findByCaseIdOrderByStartedAtDesc(anyLong());
    }

    @Test
    void unknownPlaybookDoesNotCreateRun() {
        User admin = new User();
        admin.setId(1L);
        admin.setRole("ADMIN");
        CaseFile caseFile = new CaseFile();
        caseFile.setStatus("OPEN");
        when(cases.getById(5L)).thenReturn(caseFile);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.startRun(5L, "does-not-exist", admin));

        assertEquals(404, error.getStatusCode().value());
        verify(runs, never()).save(any());
        verify(steps, never()).save(any());
    }
}
