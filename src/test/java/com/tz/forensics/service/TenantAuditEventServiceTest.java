package com.tz.forensics.service;

import com.tz.forensics.entity.Tenant;
import com.tz.forensics.entity.TenantAuditEvent;
import com.tz.forensics.repository.TenantAuditEventRepository;
import com.tz.forensics.repository.TenantRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantAuditEventServiceTest {
    @Test
    void appendsHashLinkedAuditEventAfterLockingTenant() {
        TenantRepository tenants = mock(TenantRepository.class);
        TenantAuditEventRepository events = mock(TenantAuditEventRepository.class);
        Tenant tenant = new Tenant();
        tenant.setId(9L);
        TenantAuditEvent previous = new TenantAuditEvent();
        previous.setEventHash("a".repeat(64));
        when(tenants.findByIdForUpdate(9L)).thenReturn(Optional.of(tenant));
        when(events.findTopByTenantIdOrderByIdDesc(9L)).thenReturn(Optional.of(previous));
        when(events.save(any(TenantAuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TenantAuditEventService service = new TenantAuditEventService(tenants, events);
        TenantAuditEvent saved = service.record(9L, 7L, "TENANT_MEMBER_ADDED",
            "tenant_membership", "101", "SUCCESS", "Added analyst membership.");

        assertEquals("a".repeat(64), saved.getPreviousEventHash());
        assertNotNull(saved.getEventHash());
        assertTrue(saved.getEventHash().matches("[0-9a-f]{64}"));
        assertEquals(9L, saved.getTenantId());
        assertEquals("TENANT_MEMBER_ADDED", saved.getAction());
        verify(tenants).findByIdForUpdate(9L);
        verify(events).findTopByTenantIdOrderByIdDesc(9L);
        verify(events).save(saved);
    }

    @Test
    void verifiesValidChainAndDetectsChangedEvent() {
        TenantRepository tenants = mock(TenantRepository.class);
        TenantAuditEventRepository events = mock(TenantAuditEventRepository.class);
        Tenant tenant = new Tenant();
        tenant.setId(9L);
        java.util.List<TenantAuditEvent> stored = new java.util.ArrayList<>();
        when(tenants.findByIdForUpdate(9L)).thenReturn(Optional.of(tenant));
        when(tenants.existsById(9L)).thenReturn(true);
        when(events.findTopByTenantIdOrderByIdDesc(9L)).thenAnswer(invocation ->
            stored.isEmpty() ? Optional.empty() : Optional.of(stored.get(stored.size() - 1)));
        when(events.save(any(TenantAuditEvent.class))).thenAnswer(invocation -> {
            TenantAuditEvent event = invocation.getArgument(0);
            stored.add(event);
            return event;
        });
        when(events.findAllByTenantIdOrderByIdAsc(9L)).thenAnswer(invocation -> new java.util.ArrayList<>(stored));

        TenantAuditEventService service = new TenantAuditEventService(tenants, events);
        service.record(9L, 7L, "TENANT_CREATED", "tenant", "9", "SUCCESS", "created");
        service.record(9L, 7L, "TENANT_MEMBER_ADDED", "membership", "10", "SUCCESS", "member added");

        var verified = service.verifyChain(9L);
        assertTrue(verified.valid());
        assertEquals(2, verified.checkedEvents());

        stored.get(0).setReason("altered after recording");
        var tampered = service.verifyChain(9L);
        assertFalse(tampered.valid());
        assertTrue(tampered.message().contains("hash mismatch"));
    }

    @Test
    void refusesAuditEventWithoutTenantScope() {
        TenantAuditEventService service = new TenantAuditEventService(
            mock(TenantRepository.class), mock(TenantAuditEventRepository.class));

        assertThrows(IllegalArgumentException.class,
            () -> service.record(null, 7L, "TEST", "resource", "1", "SUCCESS", "test"));
    }
}
