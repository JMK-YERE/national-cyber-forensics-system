package com.tz.forensics.service;

import com.tz.forensics.entity.CaseFile;
import com.tz.forensics.entity.Incident;
import com.tz.forensics.entity.ResponsePlaybookRun;
import com.tz.forensics.entity.ResponsePlaybookStepRun;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.ResponsePlaybookRunRepository;
import com.tz.forensics.repository.ResponsePlaybookStepRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;

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

    @Test
    void skipRequiresReasonAndDoesNotFinalizeStepWithoutIt() {
        User admin = new User();
        admin.setId(1L);
        admin.setRole("ADMIN");
        CaseFile cf = new CaseFile();
        cf.setId(5L);
        cf.setStatus("INVESTIGATING");
        ResponsePlaybookRun run = new ResponsePlaybookRun();
        ReflectionTestUtils.setField(run, "id", 41L);
        run.setCaseId(5L);
        run.setStatus("IN_PROGRESS");
        ResponsePlaybookStepRun step = new ResponsePlaybookStepRun();
        ReflectionTestUtils.setField(step, "id", 9L);
        step.setRunId(41L);
        step.setStatus("PENDING");
        when(runs.findById(41L)).thenReturn(Optional.of(run));
        when(cases.getById(5L)).thenReturn(cf);
        when(steps.findByIdAndRunId(9L, 41L)).thenReturn(Optional.of(step));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.updateStep(41L, 9L, "SKIPPED", "  ", admin));

        assertEquals(400, error.getStatusCode().value());
        assertEquals("PENDING", step.getStatus());
        verify(steps, never()).save(any());
    }

    @Test
    void finalStepCompletionCompletesRunAndWritesTimeline() {
        User admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setFullName("Platform Admin");
        admin.setRole("ADMIN");
        CaseFile cf = new CaseFile();
        cf.setId(5L);
        cf.setStatus("INVESTIGATING");
        ResponsePlaybookRun run = new ResponsePlaybookRun();
        ReflectionTestUtils.setField(run, "id", 41L);
        run.setCaseId(5L);
        run.setPlaybookSlug("phishing");
        run.setStatus("IN_PROGRESS");
        ResponsePlaybookStepRun step = new ResponsePlaybookStepRun();
        ReflectionTestUtils.setField(step, "id", 9L);
        step.setRunId(41L);
        step.setStepOrder(7);
        step.setTitle("Document findings");
        step.setStatus("PENDING");
        when(runs.findById(41L)).thenReturn(Optional.of(run));
        when(cases.getById(5L)).thenReturn(cf);
        when(steps.findByIdAndRunId(9L, 41L)).thenReturn(Optional.of(step));
        when(steps.findByRunIdOrderByStepOrderAsc(41L)).thenReturn(List.of(step));

        var result = service.updateStep(41L, 9L, "COMPLETED", "Report linked", admin);

        assertEquals("COMPLETED", result.run().getStatus());
        assertNotNull(result.run().getCompletedAt());
        assertEquals("COMPLETED", result.steps().get(0).getStatus());
        verify(runs).save(run);
        verify(cases).addTimeline(eq(5L), eq("PLAYBOOK_STEP_UPDATED"), anyString(), contains("Report linked"),
            eq(1L), eq("Platform Admin"), eq("ADMIN"));
    }


    @Test
    void cancellationRequiresReasonAndDoesNotChangeRunOnInvalidReason() {
        User admin = new User();
        admin.setId(1L);
        admin.setRole("ADMIN");
        CaseFile cf = new CaseFile();
        cf.setId(5L);
        cf.setStatus("INVESTIGATING");
        ResponsePlaybookRun run = new ResponsePlaybookRun();
        ReflectionTestUtils.setField(run, "id", 41L);
        run.setCaseId(5L);
        run.setStatus("IN_PROGRESS");
        when(runs.findById(41L)).thenReturn(Optional.of(run));
        when(cases.getById(5L)).thenReturn(cf);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.cancelRun(41L, "no", admin));

        assertEquals(400, error.getStatusCode().value());
        assertEquals("IN_PROGRESS", run.getStatus());
        verify(runs, never()).save(any());
    }

    @Test
    void cancellationRecordsReasonAndClosesActiveRun() {
        User admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setFullName("Platform Admin");
        admin.setRole("ADMIN");
        CaseFile cf = new CaseFile();
        cf.setId(5L);
        cf.setStatus("INVESTIGATING");
        ResponsePlaybookRun run = new ResponsePlaybookRun();
        ReflectionTestUtils.setField(run, "id", 41L);
        run.setCaseId(5L);
        run.setPlaybookSlug("phishing");
        run.setStatus("IN_PROGRESS");
        when(runs.findById(41L)).thenReturn(Optional.of(run));
        when(cases.getById(5L)).thenReturn(cf);
        when(steps.findByRunIdOrderByStepOrderAsc(41L)).thenReturn(List.of());

        var result = service.cancelRun(41L, "Duplicate incident", admin);

        assertEquals("CANCELLED", result.run().getStatus());
        assertNotNull(result.run().getCompletedAt());
        verify(runs).save(run);
        verify(cases).addTimeline(eq(5L), eq("PLAYBOOK_CANCELLED"), anyString(),
            contains("Duplicate incident"), eq(1L), eq("Platform Admin"), eq("ADMIN"));
    }


    @Test
    void cannotFinalizeLaterStepWhileEarlierStepIsPending() {
        User admin = new User();
        admin.setId(1L);
        admin.setRole("ADMIN");
        CaseFile cf = new CaseFile();
        cf.setId(5L);
        cf.setStatus("INVESTIGATING");
        ResponsePlaybookRun run = new ResponsePlaybookRun();
        ReflectionTestUtils.setField(run, "id", 41L);
        run.setCaseId(5L);
        run.setStatus("IN_PROGRESS");
        ResponsePlaybookStepRun first = new ResponsePlaybookStepRun();
        ReflectionTestUtils.setField(first, "id", 8L);
        first.setRunId(41L);
        first.setStepOrder(1);
        first.setStatus("PENDING");
        ResponsePlaybookStepRun second = new ResponsePlaybookStepRun();
        ReflectionTestUtils.setField(second, "id", 9L);
        second.setRunId(41L);
        second.setStepOrder(2);
        second.setStatus("PENDING");
        when(runs.findById(41L)).thenReturn(Optional.of(run));
        when(cases.getById(5L)).thenReturn(cf);
        when(steps.findByIdAndRunId(9L, 41L)).thenReturn(Optional.of(second));
        when(steps.findByRunIdOrderByStepOrderAsc(41L)).thenReturn(List.of(first, second));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.updateStep(41L, 9L, "COMPLETED", "Later step", admin));

        assertEquals(409, error.getStatusCode().value());
        assertEquals("PENDING", second.getStatus());
        verify(steps, never()).save(any());
    }

}
