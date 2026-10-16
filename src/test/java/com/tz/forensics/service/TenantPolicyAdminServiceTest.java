package com.tz.forensics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tz.forensics.entity.Tenant;
import com.tz.forensics.entity.TenantPolicy;
import com.tz.forensics.repository.TenantPolicyRepository;
import com.tz.forensics.repository.TenantRepository;
import com.tz.forensics.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantPolicyAdminServiceTest {
    @Test
    void savesValidatedPolicyAndWritesAuditEvent() throws Exception {
        TenantRepository tenants = mock(TenantRepository.class);
        TenantPolicyRepository policies = mock(TenantPolicyRepository.class);
        TenantAuditEventService audit = mock(TenantAuditEventService.class);
        UserRepository users = mock(UserRepository.class);
        Tenant tenant = new Tenant();
        tenant.setId(3L);
        tenant.setStatus("ACTIVE");
        when(tenants.findById(3L)).thenReturn(Optional.of(tenant));
        when(policies.findByTenantIdAndPolicyType(3L, "EVIDENCE_RETENTION")).thenReturn(Optional.empty());
        when(policies.save(any(TenantPolicy.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(users.findByUsername("root")).thenReturn(Optional.empty());

        TenantPolicyAdminService service = new TenantPolicyAdminService(
            tenants, policies, audit, users, new ObjectMapper());
        TenantPolicy saved = service.upsert(3L, "evidence_retention",
            new ObjectMapper().readTree("{\"retentionDays\":90,\"legalHoldEnabled\":true}"), "root");

        assertEquals("EVIDENCE_RETENTION", saved.getPolicyType());
        assertEquals(1, saved.getVersion());
        assertTrue(saved.getPolicyJson().contains("\"retentionDays\":90"));
        verify(audit).record(3L, null, "TENANT_POLICY_CREATED", "tenant_policy",
            "EVIDENCE_RETENTION", "SUCCESS", "Policy version 1 saved.");
    }

    @Test
    void rejectsSecretsInPolicyDocuments() throws Exception {
        TenantRepository tenants = mock(TenantRepository.class);
        TenantPolicyRepository policies = mock(TenantPolicyRepository.class);
        TenantAuditEventService audit = mock(TenantAuditEventService.class);
        UserRepository users = mock(UserRepository.class);
        Tenant tenant = new Tenant();
        tenant.setId(3L);
        tenant.setStatus("ACTIVE");
        when(tenants.findById(3L)).thenReturn(Optional.of(tenant));

        TenantPolicyAdminService service = new TenantPolicyAdminService(
            tenants, policies, audit, users, new ObjectMapper());
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.upsert(3L, "NOTIFICATIONS",
                new ObjectMapper().readTree("{\"emailEnabled\":true,\"apiKey\":\"do-not-store\"}"), "root"));

        assertEquals(400, error.getStatusCode().value());
        verify(policies, never()).save(any());
        verifyNoInteractions(audit);
    }

    @Test
    void rejectsInvalidRetentionRange() throws Exception {
        TenantRepository tenants = mock(TenantRepository.class);
        TenantPolicyRepository policies = mock(TenantPolicyRepository.class);
        Tenant tenant = new Tenant();
        tenant.setId(3L);
        tenant.setStatus("ACTIVE");
        when(tenants.findById(3L)).thenReturn(Optional.of(tenant));

        TenantPolicyAdminService service = new TenantPolicyAdminService(
            tenants, policies, mock(TenantAuditEventService.class), mock(UserRepository.class), new ObjectMapper());
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.upsert(3L, "EVIDENCE_RETENTION",
                new ObjectMapper().readTree("{\"retentionDays\":0}"), "root"));

        assertEquals(400, error.getStatusCode().value());
        verify(policies, never()).save(any());
    }
}
