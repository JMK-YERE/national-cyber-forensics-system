package com.tz.forensics.controller.admin;

import com.tz.forensics.entity.Tenant;
import com.tz.forensics.entity.TenantMembership;
import com.tz.forensics.entity.TenantAuditEvent;
import com.tz.forensics.entity.TenantPolicy;
import com.tz.forensics.service.TenantPolicyAdminService;
import com.fasterxml.jackson.databind.JsonNode;
import com.tz.forensics.service.TenantAuditEventService;
import com.tz.forensics.service.TenantAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/api/tenants")
public class TenantAdminApiController {
    private final TenantAdminService service;
    private final TenantAuditEventService auditEvents;
    private final TenantPolicyAdminService policies;

    public TenantAdminApiController(TenantAdminService service, TenantAuditEventService auditEvents,
                                    TenantPolicyAdminService policies) {
        this.service = service;
        this.auditEvents = auditEvents;
        this.policies = policies;
    }

    @GetMapping
    public List<Tenant> listTenants() {
        return service.listTenants();
    }

    @GetMapping("/{tenantId}/audit-events")
    public List<TenantAuditEvent> listAuditEvents(@PathVariable Long tenantId) {
        return auditEvents.listRecent(tenantId);
    }

    @GetMapping("/{tenantId}/audit-events/verify")
    public TenantAuditEventService.ChainVerification verifyAuditChain(@PathVariable Long tenantId) {
        return auditEvents.verifyChain(tenantId);
    }

    @GetMapping("/{tenantId}/policies")
    public List<TenantPolicy> listPolicies(@PathVariable Long tenantId) {
        return policies.list(tenantId);
    }

    @PutMapping("/{tenantId}/policies/{policyType}")
    public TenantPolicy upsertPolicy(@PathVariable Long tenantId, @PathVariable String policyType,
                                     @RequestBody JsonNode document,
                                     org.springframework.security.core.Authentication authentication) {
        return policies.upsert(tenantId, policyType, document, authentication.getName());
    }

    @PostMapping
    public Tenant createTenant(@Valid @RequestBody CreateTenantRequest request,
                               org.springframework.security.core.Authentication authentication) {
        return service.createTenant(request.name(), request.slug(), request.sector(), authentication.getName());
    }

    @PostMapping("/{tenantId}/members")
    public TenantMembership addMember(@PathVariable Long tenantId,
                                      @Valid @RequestBody AddMemberRequest request,
                                      org.springframework.security.core.Authentication authentication) {
        return service.addMembership(tenantId, request.userId(), request.role(), authentication.getName());
    }

    public record CreateTenantRequest(
        @NotBlank @Size(max = 180) String name,
        @NotBlank @Size(max = 100) String slug,
        @Size(max = 80) String sector
    ) {}

    public record AddMemberRequest(@NotNull Long userId, @NotBlank @Size(max = 40) String role) {}
}
