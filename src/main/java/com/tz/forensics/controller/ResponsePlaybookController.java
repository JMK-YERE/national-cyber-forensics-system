package com.tz.forensics.controller;

import com.tz.forensics.service.ResponsePlaybookService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Read-only playbook catalog. Starting or executing containment actions is deliberately
 * separate and must require case authorization and an explicit human approval.
 */
@RestController
@RequestMapping("/api/response-playbooks")
public class ResponsePlaybookController {
    private final ResponsePlaybookService playbooks;

    public ResponsePlaybookController(ResponsePlaybookService playbooks) {
        this.playbooks = playbooks;
    }

    @GetMapping
    public List<ResponsePlaybookService.Playbook> list() {
        return playbooks.list();
    }

    @GetMapping("/{slug}")
    public ResponsePlaybookService.Playbook get(@PathVariable String slug) {
        return playbooks.findBySlug(slug)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Response playbook not found."));
    }
}
