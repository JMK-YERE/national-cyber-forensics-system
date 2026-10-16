package com.tz.forensics.service;

import com.tz.forensics.entity.Tenant;
import com.tz.forensics.entity.TenantAuditEvent;
import com.tz.forensics.entity.TenantMembership;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.TenantAuditEventRepository;
import com.tz.forensics.repository.TenantMembershipRepository;
import com.tz.forensics.repository.TenantRepository;
import com.tz.forensics.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class TenantAdminService {
    private static final Set<String> ALLOWED_TENANT_ROLES = Set.of(
        "TENANT_ADMIN", "INCIDENT_MANAGER", "ANALYST", "FORENSICS", "RESPONDER", "VIEWER", "REPORTER"
    );

    private final TenantRepository tenants;
    private final TenantMembershipRepository memberships;
    private final TenantAuditEventRepository auditEvents;
    private final UserRepository users;

    public TenantAdminService(TenantRepository tenants,
                              TenantMembershipRepository memberships,
                              TenantAuditEventRepository auditEvents,
                              UserRepository users) {
        this.tenants = tenants;
        this.memberships = memberships;
        this.auditEvents = auditEvents;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<Tenant> listTenants() {
        return tenants.findAll();
    }

    @Transactional
    public Tenant createTenant(String name, String slug, String sector, String actorName) {
        if (name == null || name.isBlank() || name.trim().length() > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant name is required (max 180 characters).");
        }
        String normalizedSlug = normalizeSlug(slug);
        if (normalizedSlug.length() > 100 || !normalizedSlug.matches("[a-z0-9]+(?:-[a-z0-9]+)*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slug must contain lowercase letters, numbers and single hyphens.");
        }
        if (tenants.existsBySlugIgnoreCase(normalizedSlug)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant slug already exists.");
        }

        Tenant tenant = new Tenant();
        tenant.setName(name.trim());
        tenant.setSlug(normalizedSlug);
        tenant.setSector(sector == null || sector.isBlank() ? null : sector.trim());
        tenant.setStatus("ACTIVE");
        Tenant saved = tenants.save(tenant);
        recordAudit(saved.getId(), actorName, "TENANT_CREATED", "tenant", saved.getId().toString(), "Tenant created.");
        return saved;
    }

    @Transactional
    public TenantMembership addMembership(Long tenantId, Long userId, String role, String actorName) {
        Tenant tenant = tenants.findById(tenantId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found."));
        if (!"ACTIVE".equals(tenant.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant is not active.");
        }
        User user = users.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
        String normalizedRole = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_TENANT_ROLES.contains(normalizedRole)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported tenant role.");
        }
        if (memberships.existsByTenantIdAndUserIdAndStatus(tenantId, userId, "ACTIVE")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already an active member of this tenant.");
        }

        TenantMembership membership = memberships.findByTenantIdAndUserIdAndStatus(tenantId, userId, "REVOKED")
            .orElseGet(TenantMembership::new);
        membership.setTenantId(tenantId);
        membership.setUserId(userId);
        membership.setRole(normalizedRole);
        membership.setStatus("ACTIVE");
        User actor = users.findByUsername(actorName).orElse(null);
        membership.setCreatedBy(actor == null ? null : actor.getId());
        TenantMembership saved = memberships.save(membership);
        recordAudit(tenantId, actorName, "TENANT_MEMBER_ADDED", "tenant_membership",
            saved.getId().toString(), "userId=" + userId + "; role=" + normalizedRole);
        return saved;
    }

    private void recordAudit(Long tenantId, String actorName, String action, String resourceType,
                             String resourceId, String reason) {
        TenantAuditEvent event = new TenantAuditEvent();
        event.setTenantId(tenantId);
        users.findByUsername(actorName).ifPresent(user -> event.setActorUserId(user.getId()));
        event.setAction(action);
        event.setResourceType(resourceType);
        event.setResourceId(resourceId);
        event.setOutcome("SUCCESS");
        event.setReason(reason);
        auditEvents.save(event);
    }

    private String normalizeSlug(String slug) {
        return slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT);
    }
}
