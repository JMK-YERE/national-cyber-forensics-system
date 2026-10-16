package com.tz.forensics.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.tz.forensics.entity.Tenant;
import com.tz.forensics.entity.TenantMembership;
import com.tz.forensics.entity.TenantPolicy;
import com.tz.forensics.service.TenantAccessService;
import com.tz.forensics.service.TenantAdminService;
import com.tz.forensics.service.TenantAuditEventService;
import com.tz.forensics.service.TenantPolicyAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * Tenant self-service endpoints. A caller-supplied tenant ID is never trusted by itself:
 * every operation verifies an active membership and, for administration, a tenant role.
 */
@RestController
@RequestMapping("/api/tenants")
public class TenantWorkspaceController {
    private static final Set<String> TENANT_ADMINS = Set.of("TENANT_ADMIN");

    private final TenantAccessService access;
    private final TenantAdminService tenantAdmin;
    private final TenantPolicyAdminService policies;
    private final TenantAuditEventService audit;

    public TenantWorkspaceController(TenantAccessService access, TenantAdminService tenantAdmin,
            TenantPolicyAdminService policies, TenantAuditEventService audit) {
        this.access = access;
        this.tenantAdmin = tenantAdmin;
        this.policies = policies;
        this.audit = audit;
    }

    @GetMapping
    public List<Tenant> myTenants(Authentication auth) {
        return access.listMyActiveTenants(auth == null ? null : auth.getName());
    }

    @GetMapping("/{tenantId}/members")
    public List<TenantMembership> members(@PathVariable Long tenantId, Authentication auth) {
        access.requireMembership(auth.getName(), tenantId, TENANT_ADMINS);
        return tenantAdmin.listActiveMemberships(tenantId);
    }

    @PostMapping("/{tenantId}/members")
    public TenantMembership addMember(@PathVariable Long tenantId, @Valid @RequestBody AddMember request,
            Authentication auth) {
        access.requireMembership(auth.getName(), tenantId, TENANT_ADMINS);
        return tenantAdmin.addMembership(tenantId, request.userId(), request.role(), auth.getName());
    }

    @PutMapping("/{tenantId}/members/{userId}")
    public TenantMembership updateMember(@PathVariable Long tenantId, @PathVariable Long userId,
            @Valid @RequestBody UpdateRole request, Authentication auth) {
        access.requireMembership(auth.getName(), tenantId, TENANT_ADMINS);
        return tenantAdmin.updateMembershipRole(tenantId, userId, request.role(), auth.getName());
    }

    @DeleteMapping("/{tenantId}/members/{userId}")
    public TenantMembership revokeMember(@PathVariable Long tenantId, @PathVariable Long userId,
            Authentication auth) {
        access.requireMembership(auth.getName(), tenantId, TENANT_ADMINS);
        return tenantAdmin.revokeMembership(tenantId, userId, auth.getName());
    }

    @GetMapping("/{tenantId}/policies")
    public List<TenantPolicy> policies(@PathVariable Long tenantId, Authentication auth) {
        access.requireMembership(auth.getName(), tenantId, TENANT_ADMINS);
        return policies.list(tenantId);
    }

    @PutMapping("/{tenantId}/policies/{type}")
    public TenantPolicy updatePolicy(@PathVariable Long tenantId, @PathVariable String type,
            @RequestBody JsonNode document, Authentication auth) {
        access.requireMembership(auth.getName(), tenantId, TENANT_ADMINS);
        return policies.upsert(tenantId, type, document, auth.getName());
    }

    @GetMapping("/{tenantId}/audit-events")
    public List<com.tz.forensics.entity.TenantAuditEvent> auditEvents(
            @PathVariable Long tenantId, Authentication auth) {
        access.requireMembership(auth.getName(), tenantId, TENANT_ADMINS);
        return audit.listRecent(tenantId);
    }

    @GetMapping("/{tenantId}/audit-events/verify")
    public TenantAuditEventService.ChainVerification verifyAuditChain(
            @PathVariable Long tenantId, Authentication auth) {
        access.requireMembership(auth.getName(), tenantId, TENANT_ADMINS);
        return audit.verifyChain(tenantId);
    }

    public record AddMember(@NotNull Long userId, @NotBlank @Size(max = 40) String role) {}
    public record UpdateRole(@NotBlank @Size(max = 40) String role) {}
}
