package com.tz.forensics.controller;

import com.tz.forensics.entity.User;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.service.ResponsePlaybookExecutionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Case-linked playbook execution API. Authorization is rechecked in the service for every operation. */
@RestController
@RequestMapping("/api/response-playbooks")
public class ResponsePlaybookExecutionController {
    private final ResponsePlaybookExecutionService execution;
    private final UserRepository users;

    public ResponsePlaybookExecutionController(ResponsePlaybookExecutionService execution, UserRepository users) {
        this.execution = execution;
        this.users = users;
    }

    @GetMapping("/cases/{caseId}/runs")
    public List<ResponsePlaybookExecutionService.RunDetails> listRuns(@PathVariable Long caseId, Authentication auth) {
        return execution.listRuns(caseId, currentUser(auth));
    }

    @PostMapping("/cases/{caseId}/runs")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponsePlaybookExecutionService.RunDetails startRun(@PathVariable Long caseId,
            @Valid @RequestBody StartRunRequest request, Authentication auth) {
        return execution.startRun(caseId, request.playbookSlug(), currentUser(auth));
    }

    @GetMapping("/runs/{runId}")
    public ResponsePlaybookExecutionService.RunDetails getRun(@PathVariable Long runId, Authentication auth) {
        return execution.getRun(runId, currentUser(auth));
    }

    @PatchMapping("/runs/{runId}/steps/{stepId}")
    public ResponsePlaybookExecutionService.RunDetails updateStep(@PathVariable Long runId,
            @PathVariable Long stepId, @Valid @RequestBody UpdateStepRequest request, Authentication auth) {
        return execution.updateStep(runId, stepId, request.status(), request.notes(), currentUser(auth));
    }

    @PatchMapping("/runs/{runId}/cancel")
    public ResponsePlaybookExecutionService.RunDetails cancelRun(@PathVariable Long runId,
            @Valid @RequestBody CancelRunRequest request, Authentication auth) {
        return execution.cancelRun(runId, request.reason(), currentUser(auth));
    }

    private User currentUser(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }
        return users.findByUsername(auth.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user record not found."));
    }

    public record StartRunRequest(@NotBlank @Size(max = 80) String playbookSlug) {}
    public record UpdateStepRequest(@NotBlank @Size(max = 20) String status, @Size(max = 5000) String notes) {}
    public record CancelRunRequest(@NotBlank @Size(min = 5, max = 1000) String reason) {}
}
