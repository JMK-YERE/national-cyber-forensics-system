package com.tz.forensics.controller.admin;

import com.tz.forensics.entity.Tenant;
import com.tz.forensics.entity.TenantMembership;
import com.tz.forensics.entity.TenantAuditEvent;
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

    public TenantAdminApiController(TenantAdminService service, TenantAuditEventService auditEvents) {
        this.service = service;
        this.auditEvents = auditEvents;
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
