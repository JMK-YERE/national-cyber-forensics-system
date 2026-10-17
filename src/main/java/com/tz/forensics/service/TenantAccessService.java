package com.tz.forensics.service;

import com.tz.forensics.entity.Tenant;
import com.tz.forensics.entity.TenantMembership;
import com.tz.forensics.entity.User;
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
public class TenantAccessService {
    private final UserRepository users;
    private final TenantRepository tenants;
    private final TenantMembershipRepository memberships;

    public TenantAccessService(UserRepository users, TenantRepository tenants, TenantMembershipRepository memberships) {
        this.users = users;
        this.tenants = tenants;
        this.memberships = memberships;
    }

    @Transactional(readOnly = true)
    public TenantMembership requireMembership(String username, Long tenantId, Set<String> allowedRoles) {
        if (username == null || username.isBlank() || tenantId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant access denied.");
        }
        User user = users.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user record not found."));
        requireActiveAccount(user);
        Tenant tenant = tenants.findById(tenantId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found."));
        if (!"ACTIVE".equalsIgnoreCase(tenant.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant is not active.");
        }
        TenantMembership membership = memberships.findByTenantIdAndUserId(tenantId, user.getId())
            .filter(m -> "ACTIVE".equalsIgnoreCase(m.getStatus()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Active tenant membership is required."));
        Set<String> normalizedRoles = allowedRoles == null ? Set.of() : allowedRoles.stream()
            .map(r -> r.toUpperCase(Locale.ROOT)).collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (!normalizedRoles.isEmpty() && !normalizedRoles.contains(membership.getRole() == null ? "" : membership.getRole().toUpperCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your tenant role does not permit this action.");
        }
        return membership;
    }

    /**
     * Re-check account state at the tenant boundary as well as at login. This prevents
     * a disabled, locked, or unapproved account with a stale session from continuing
     * to use tenant-scoped endpoints.
     */
    private void requireActiveAccount(User user) {
        if (!Boolean.TRUE.equals(user.getEnabled()) || !user.isApproved()
                || Boolean.TRUE.equals(user.getAccountLocked())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active.");
        }
    }

    @Transactional(readOnly = true)
    public List<Tenant> listMyActiveTenants(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }
        User user = users.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user record not found."));
        requireActiveAccount(user);
        return memberships.findAllByUserIdAndStatus(user.getId(), "ACTIVE").stream()
            .map(m -> tenants.findById(m.getTenantId()).orElse(null))
            .filter(t -> t != null && "ACTIVE".equalsIgnoreCase(t.getStatus()))
            .toList();
    }
}
