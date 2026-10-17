package com.tz.forensics.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class ResponsePlaybookServiceTest {
    private final ResponsePlaybookService service = new ResponsePlaybookService();

    @Test
    void catalogContainsCoreIncidentTypes() {
        var slugs = service.list().stream()
            .map(ResponsePlaybookService.Playbook::slug)
            .collect(java.util.stream.Collectors.toSet());

        assertTrue(slugs.containsAll(java.util.Set.of(
            "phishing", "ransomware", "account-compromise", "data-breach", "malware")));
        assertEquals(5, slugs.size());
    }

    @Test
    void everyPlaybookHasOrderedRequiredStepsAndVersion() {
        for (var playbook : service.list()) {
            assertFalse(playbook.title().isBlank());
            assertFalse(playbook.version().isBlank());
            assertFalse(playbook.steps().isEmpty());
            assertEquals(playbook.steps().size(),
                new HashSet<>(playbook.steps().stream().map(ResponsePlaybookService.Step::order).toList()).size());
            for (int i = 0; i < playbook.steps().size(); i++) {
                var step = playbook.steps().get(i);
                assertEquals(i + 1, step.order());
                assertFalse(step.title().isBlank());
                assertFalse(step.guidance().isBlank());
                assertTrue(step.required());
            }
        }
    }

    @Test
    void lookupIsCaseInsensitiveAndUnknownPlaybookIsAbsent() {
        assertTrue(service.findBySlug("PHISHING").isPresent());
        assertTrue(service.findBySlug("missing-playbook").isEmpty());
        assertTrue(service.findBySlug(null).isEmpty());
    }
}
