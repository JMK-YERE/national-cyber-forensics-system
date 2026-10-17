package com.tz.forensics.service;

import com.tz.forensics.entity.*;
import com.tz.forensics.repository.ResponsePlaybookRunRepository;
import com.tz.forensics.repository.ResponsePlaybookStepRunRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Service
public class ResponsePlaybookExecutionService {
    private static final Set<String> STAFF_ROLES = Set.of("ADMIN", "CYBER_PRO", "FORENSICS", "ANALYST");

    private final ResponsePlaybookRunRepository runs;
    private final ResponsePlaybookStepRunRepository steps;
    private final ResponsePlaybookService catalog;
    private final CaseFileService cases;
    private final IncidentService incidents;
    private final TenantAccessService tenantAccessService;

    public ResponsePlaybookExecutionService(ResponsePlaybookRunRepository runs,
            ResponsePlaybookStepRunRepository steps, ResponsePlaybookService catalog,
            CaseFileService cases, IncidentService incidents, TenantAccessService tenantAccessService) {
        this.runs = runs;
        this.steps = steps;
        this.catalog = catalog;
        this.cases = cases;
        this.incidents = incidents;
        this.tenantAccessService = tenantAccessService;
    }

    public record RunDetails(ResponsePlaybookRun run, List<ResponsePlaybookStepRun> steps) {}

    @Transactional
    public RunDetails startRun(Long caseId, String slug, User actor) {
        CaseFile caseFile = requireAuthorizedCase(caseId, actor);
        if ("CLOSED".equalsIgnoreCase(caseFile.getStatus()) || "ARCHIVED".equalsIgnoreCase(caseFile.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A playbook cannot be started on a closed or archived case.");
        }
        ResponsePlaybookService.Playbook playbook = catalog.findBySlug(slug)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Response playbook not found."));

        ResponsePlaybookRun run = new ResponsePlaybookRun();
        run.setCaseId(caseId);
        run.setPlaybookSlug(playbook.slug());
        run.setPlaybookVersion(playbook.version());
        run.setStartedBy(actor.getId());
        run = runs.save(run);

        for (ResponsePlaybookService.Step template : playbook.steps()) {
            ResponsePlaybookStepRun step = new ResponsePlaybookStepRun();
            step.setRunId(run.getId());
            step.setStepOrder(template.order());
            step.setPhase(template.phase());
            step.setTitle(template.title());
            step.setGuidance(template.guidance());
            step.setRequired(template.required());
            steps.save(step);
        }
        cases.addTimeline(caseId, "PLAYBOOK_STARTED", "Response playbook started",
            "Playbook " + playbook.slug() + " v" + playbook.version() + " started by " + safeName(actor), actor.getId(),
            safeName(actor), actor.getRole());
        return getRun(run.getId(), actor);
    }

    @Transactional(readOnly = true)
    public List<RunDetails> listRuns(Long caseId, User actor) {
        requireAuthorizedCase(caseId, actor);
        return runs.findByCaseIdOrderByStartedAtDesc(caseId).stream()
            .map(run -> new RunDetails(run, steps.findByRunIdOrderByStepOrderAsc(run.getId())))
            .toList();
    }

    @Transactional(readOnly = true)
    public RunDetails getRun(Long runId, User actor) {
        ResponsePlaybookRun run = runs.findById(runId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Playbook run not found."));
        requireAuthorizedCase(run.getCaseId(), actor);
        return new RunDetails(run, steps.findByRunIdOrderByStepOrderAsc(runId));
    }

    @Transactional
    public RunDetails updateStep(Long runId, Long stepId, String requestedStatus, String notes, User actor) {
        ResponsePlaybookRun run = runs.findById(runId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Playbook run not found."));
        CaseFile caseFile = requireAuthorizedCase(run.getCaseId(), actor);
        if (!"IN_PROGRESS".equals(run.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This playbook run is no longer active.");
        }
        ResponsePlaybookStepRun step = steps.findByIdAndRunId(stepId, runId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Playbook step not found."));
        String status = requestedStatus == null ? "" : requestedStatus.trim().toUpperCase(java.util.Locale.ROOT);
        if (!Set.of("COMPLETED", "SKIPPED").contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Step status must be COMPLETED or SKIPPED.");
        }
        if (!"PENDING".equals(step.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This step has already been finalized.");
        }
        List<ResponsePlaybookStepRun> orderedSteps = steps.findByRunIdOrderByStepOrderAsc(runId);
        boolean earlierStepPending = orderedSteps.stream()
            .anyMatch(candidate -> candidate.getStepOrder() < step.getStepOrder() && "PENDING".equals(candidate.getStatus()));
        if (earlierStepPending) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Complete or explicitly skip earlier steps before finalizing this step.");
        }
        String cleanNotes = notes == null ? "" : notes.trim();
        if ("SKIPPED".equals(status) && cleanNotes.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A reason is required when skipping a step.");
        }
        if (cleanNotes.length() > 5000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Step notes must be 5000 characters or fewer.");
        }

        step.setStatus(status);
        step.setCompletionNotes(cleanNotes.isBlank() ? null : cleanNotes);
        step.setCompletedBy(actor.getId());
        step.setCompletedAt(OffsetDateTime.now());
        steps.save(step);

        List<ResponsePlaybookStepRun> currentSteps = steps.findByRunIdOrderByStepOrderAsc(runId);
        boolean finished = currentSteps.stream().noneMatch(s -> "PENDING".equals(s.getStatus()));
        if (finished) {
            run.setStatus("COMPLETED");
            run.setCompletedAt(OffsetDateTime.now());
            runs.save(run);
        }
        String detail = "Playbook " + run.getPlaybookSlug() + " step " + step.getStepOrder()
            + " marked " + status + (cleanNotes.isBlank() ? "" : ". Notes: " + cleanNotes);
        cases.addTimeline(caseFile.getId(), "PLAYBOOK_STEP_UPDATED", "Response playbook step updated",
            detail, actor.getId(), safeName(actor), actor.getRole());
        return new RunDetails(run, currentSteps);
    }

    @Transactional
    public RunDetails cancelRun(Long runId, String reason, User actor) {
        ResponsePlaybookRun run = runs.findById(runId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Playbook run not found."));
        CaseFile caseFile = requireAuthorizedCase(run.getCaseId(), actor);
        if (!"IN_PROGRESS".equals(run.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only an active playbook run can be cancelled.");
        }
        String cleanReason = reason == null ? "" : reason.trim();
        if (cleanReason.length() < 5 || cleanReason.length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cancellation reason must be between 5 and 1000 characters.");
        }
        run.setStatus("CANCELLED");
        run.setCompletedAt(OffsetDateTime.now());
        runs.save(run);
        cases.addTimeline(caseFile.getId(), "PLAYBOOK_CANCELLED", "Response playbook cancelled",
            "Playbook " + run.getPlaybookSlug() + " cancelled by " + safeName(actor) + ". Reason: " + cleanReason,
            actor.getId(), safeName(actor), actor.getRole());
        return new RunDetails(run, steps.findByRunIdOrderByStepOrderAsc(runId));
    }

    private CaseFile requireAuthorizedCase(Long caseId, User actor) {
        if (actor == null || actor.getId() == null || !STAFF_ROLES.contains(
                actor.getRole() == null ? "" : actor.getRole().trim().toUpperCase(java.util.Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Incident-response staff access is required.");
        }
        CaseFile caseFile = cases.getById(caseId);
        if (caseFile == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found.");

        // This service is called by both MVC and REST endpoints, so tenant authorization
        // must be enforced here rather than relying on a particular controller.
        if (caseFile.getIncidentId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Case access denied.");
        }
        Incident incident = incidents.getById(caseFile.getIncidentId());
        if (incident == null || !java.util.Objects.equals(caseFile.getTenantId(), incident.getTenantId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Case access denied.");
        }
        if (caseFile.getTenantId() != null) {
            tenantAccessService.requireMembership(actor.getUsername(), caseFile.getTenantId(), Set.of());
        }
        if (actor.isAdmin() || actor.isProfessional() || actor.isForensics()) return caseFile;

        boolean incidentAccess = (incident.getReporterUserId() != null && actor.getId().equals(incident.getReporterUserId()))
            || (incident.getAssignedTo() != null && actor.getId().equals(incident.getAssignedTo()));
        boolean caseAccess = (caseFile.getCreatedBy() != null && actor.getId().equals(caseFile.getCreatedBy()))
            || (caseFile.getAssignedTo() != null && actor.getId().equals(caseFile.getAssignedTo()))
            || (caseFile.getLeadInvestigator() != null && actor.getId().equals(caseFile.getLeadInvestigator()));
        if (!incidentAccess || !caseAccess) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Case access denied.");
        }
        return caseFile;
    }

    private String safeName(User actor) {
        return actor.getFullName() == null || actor.getFullName().isBlank() ? actor.getUsername() : actor.getFullName();
    }
}
