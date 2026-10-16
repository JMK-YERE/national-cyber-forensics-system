package com.tz.forensics.service;

import com.tz.forensics.entity.Tenant;
import com.tz.forensics.entity.TenantMembership;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.TenantMembershipRepository;
import com.tz.forensics.repository.TenantRepository;
import com.tz.forensics.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantAccessServiceTest {
    private UserRepository users;
    private TenantRepository tenants;
    private TenantMembershipRepository memberships;
    private TenantAccessService access;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        tenants = mock(TenantRepository.class);
        memberships = mock(TenantMembershipRepository.class);
        access = new TenantAccessService(users, tenants, memberships);
    }

    @Test
    void deniesUserWithoutMembershipEvenWhenTenantIdIsKnown() {
        User user = user(12L, "analyst");
        Tenant tenant = tenant(7L, "ACTIVE");
        when(users.findByUsername("analyst")).thenReturn(Optional.of(user));
        when(tenants.findById(7L)).thenReturn(Optional.of(tenant));
        when(memberships.findByTenantIdAndUserId(7L, 12L)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> access.requireMembership("analyst", 7L, Set.of("TENANT_ADMIN")));

        assertEquals(403, error.getStatusCode().value());
    }

    @Test
    void deniesActiveMemberWithInsufficientTenantRole() {
        User user = user(12L, "analyst");
        Tenant tenant = tenant(7L, "ACTIVE");
        TenantMembership membership = membership(7L, 12L, "ANALYST", "ACTIVE");
        when(users.findByUsername("analyst")).thenReturn(Optional.of(user));
        when(tenants.findById(7L)).thenReturn(Optional.of(tenant));
        when(memberships.findByTenantIdAndUserId(7L, 12L)).thenReturn(Optional.of(membership));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> access.requireMembership("analyst", 7L, Set.of("TENANT_ADMIN")));

        assertEquals(403, error.getStatusCode().value());
    }

    @Test
    void permitsOnlyActiveTenantAdministratorForAdministration() {
        User user = user(12L, "tenant-admin");
        Tenant tenant = tenant(7L, "ACTIVE");
        TenantMembership membership = membership(7L, 12L, "TENANT_ADMIN", "ACTIVE");
        when(users.findByUsername("tenant-admin")).thenReturn(Optional.of(user));
        when(tenants.findById(7L)).thenReturn(Optional.of(tenant));
        when(memberships.findByTenantIdAndUserId(7L, 12L)).thenReturn(Optional.of(membership));

        TenantMembership result = access.requireMembership("tenant-admin", 7L, Set.of("TENANT_ADMIN"));

        assertSame(membership, result);
    }

    @Test
    void deniesSuspendedTenantEvenWhenMembershipIsActive() {
        User user = user(12L, "tenant-admin");
        when(users.findByUsername("tenant-admin")).thenReturn(Optional.of(user));
        when(tenants.findById(7L)).thenReturn(Optional.of(tenant(7L, "SUSPENDED")));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> access.requireMembership("tenant-admin", 7L, Set.of("TENANT_ADMIN")));

        assertEquals(403, error.getStatusCode().value());
        verifyNoInteractions(memberships);
    }

    private User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private Tenant tenant(Long id, String status) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setStatus(status);
        return tenant;
    }

    private TenantMembership membership(Long tenantId, Long userId, String role, String status) {
        TenantMembership membership = new TenantMembership();
        membership.setTenantId(tenantId);
        membership.setUserId(userId);
        membership.setRole(role);
        membership.setStatus(status);
        return membership;
    }
}
