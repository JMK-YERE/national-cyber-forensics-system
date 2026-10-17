package com.tz.forensics.service;

import com.tz.forensics.repository.TenantMembershipRepository;
import com.tz.forensics.repository.TenantRepository;
import com.tz.forensics.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class TenantAdminServiceTest {
    private TenantRepository tenants;
    private TenantMembershipRepository memberships;
    private TenantAuditEventService auditEventService;
    private UserRepository users;
    private TenantAdminService service;

    @BeforeEach
    void setUp() {
        tenants = mock(TenantRepository.class);
        memberships = mock(TenantMembershipRepository.class);
        auditEventService = mock(TenantAuditEventService.class);
        users = mock(UserRepository.class);
        service = new TenantAdminService(tenants, memberships, auditEventService, users);
    }

    @Test
    void rejectsBlankTenantName() {
        assertThrows(ResponseStatusException.class,
            () -> service.createTenant("  ", "valid-tenant", "Finance", "admin"));
        verifyNoInteractions(tenants, memberships, auditEventService, users);
    }

    @Test
    void rejectsInvalidTenantSlug() {
        assertThrows(ResponseStatusException.class,
            () -> service.createTenant("Finance Org", "Finance Org!", "Finance", "admin"));
        verifyNoInteractions(tenants, memberships, auditEventService, users);
    }

    @Test
    void creatorIsGrantedActiveTenantAdminMembership() {
        com.tz.forensics.entity.User creator = new com.tz.forensics.entity.User();
        creator.setId(9L);
        creator.setUsername("root");
        com.tz.forensics.entity.Tenant savedTenant = new com.tz.forensics.entity.Tenant();
        savedTenant.setId(22L);
        when(tenants.existsBySlugIgnoreCase("finance-org")).thenReturn(false);
        when(tenants.save(any(com.tz.forensics.entity.Tenant.class))).thenReturn(savedTenant);
        when(users.findByUsername("root")).thenReturn(java.util.Optional.of(creator));

        service.createTenant("Finance Org", "finance-org", "Finance", "root");

        org.mockito.ArgumentCaptor<com.tz.forensics.entity.TenantMembership> captor =
            org.mockito.ArgumentCaptor.forClass(com.tz.forensics.entity.TenantMembership.class);
        verify(memberships).save(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(22L, captor.getValue().getTenantId());
        org.junit.jupiter.api.Assertions.assertEquals(9L, captor.getValue().getUserId());
        org.junit.jupiter.api.Assertions.assertEquals("TENANT_ADMIN", captor.getValue().getRole());
        org.junit.jupiter.api.Assertions.assertEquals("ACTIVE", captor.getValue().getStatus());
    }

    @Test
    void preventsRemovingTheLastTenantAdministrator() {
        when(tenants.existsById(3L)).thenReturn(true);
        com.tz.forensics.entity.Tenant lockedTenant = new com.tz.forensics.entity.Tenant();
        lockedTenant.setId(3L);
        when(tenants.findByIdForUpdate(3L)).thenReturn(java.util.Optional.of(lockedTenant));
        com.tz.forensics.entity.TenantMembership admin = new com.tz.forensics.entity.TenantMembership();
        admin.setTenantId(3L);
        admin.setUserId(4L);
        admin.setRole("TENANT_ADMIN");
        admin.setStatus("ACTIVE");
        when(memberships.findByTenantIdAndUserId(3L, 4L)).thenReturn(java.util.Optional.of(admin));
        when(memberships.findAllByTenantIdAndStatus(3L, "ACTIVE")).thenReturn(java.util.List.of(admin));

        ResponseStatusException error = org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
            () -> service.updateMembershipRole(3L, 4L, "ANALYST", "root"));

        org.junit.jupiter.api.Assertions.assertEquals(409, error.getStatusCode().value());
        verify(memberships, never()).save(any());
        verifyNoInteractions(auditEventService, users);
    }

    @Test
    void rejectsDuplicateTenantSlug() {
        when(tenants.existsBySlugIgnoreCase("finance-org")).thenReturn(true);
        assertThrows(ResponseStatusException.class,
            () -> service.createTenant("Finance Org", "finance-org", "Finance", "admin"));
        verify(tenants).existsBySlugIgnoreCase("finance-org");
        verify(tenants, never()).save(any());
        verifyNoInteractions(memberships, auditEventService, users);
    }
}
