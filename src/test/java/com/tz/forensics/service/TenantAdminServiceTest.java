package com.tz.forensics.service;

import com.tz.forensics.repository.TenantAuditEventRepository;
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
    private TenantAuditEventRepository auditEvents;
    private UserRepository users;
    private TenantAdminService service;

    @BeforeEach
    void setUp() {
        tenants = mock(TenantRepository.class);
        memberships = mock(TenantMembershipRepository.class);
        auditEvents = mock(TenantAuditEventRepository.class);
        users = mock(UserRepository.class);
        service = new TenantAdminService(tenants, memberships, auditEvents, users);
    }

    @Test
    void rejectsBlankTenantName() {
        assertThrows(ResponseStatusException.class,
            () -> service.createTenant("  ", "valid-tenant", "Finance", "admin"));
        verifyNoInteractions(tenants, memberships, auditEvents, users);
    }

    @Test
    void rejectsInvalidTenantSlug() {
        assertThrows(ResponseStatusException.class,
            () -> service.createTenant("Finance Org", "Finance Org!", "Finance", "admin"));
        verifyNoInteractions(tenants, memberships, auditEvents, users);
    }

    @Test
    void rejectsDuplicateTenantSlug() {
        when(tenants.existsBySlugIgnoreCase("finance-org")).thenReturn(true);
        assertThrows(ResponseStatusException.class,
            () -> service.createTenant("Finance Org", "finance-org", "Finance", "admin"));
        verify(tenants).existsBySlugIgnoreCase("finance-org");
        verify(tenants, never()).save(any());
        verifyNoInteractions(memberships, auditEvents, users);
    }
}
