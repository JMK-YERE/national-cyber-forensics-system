package com.tz.forensics.service;

import com.tz.forensics.entity.Tenant;
import com.tz.forensics.entity.TenantAuditEvent;
import com.tz.forensics.repository.TenantAuditEventRepository;
import com.tz.forensics.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

/**
 * Appends tenant audit records in a per-tenant serialized SHA-256 hash chain.
 * This is tamper-evident, not tamper-proof against a privileged database administrator.
 */
@Service
public class TenantAuditEventService {
    private final TenantRepository tenants;
    private final TenantAuditEventRepository events;

    public TenantAuditEventService(TenantRepository tenants, TenantAuditEventRepository events) {
        this.tenants = tenants;
        this.events = events;
    }

    @Transactional
    public TenantAuditEvent record(Long tenantId, Long actorUserId, String action,
                                   String resourceType, String resourceId,
                                   String outcome, String reason) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID is required for a tenant audit event.");
        }
        Tenant tenant = tenants.findByIdForUpdate(tenantId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found for audit event."));

        TenantAuditEvent previous = events.findTopByTenantIdOrderByIdDesc(tenant.getId()).orElse(null);
        TenantAuditEvent event = new TenantAuditEvent();
        event.setTenantId(tenant.getId());
        // PostgreSQL TIMESTAMPTZ stores microsecond precision; normalize before hashing.
        event.setOccurredAt(OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS));
        // PostgreSQL timestamptz stores microsecond precision; normalize before hashing.
        event.setOccurredAt(OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS));
        event.setActorUserId(actorUserId);
        event.setAction(required(action, "action"));
        event.setResourceType(resourceType);
        event.setResourceId(resourceId);
        event.setOutcome(outcome == null ? "SUCCESS" : outcome);
        event.setReason(reason);
        event.setPreviousEventHash(previous == null ? null : previous.getEventHash());
        event.setEventHash(hash(event));
        return events.save(event);
    }

    public record ChainVerification(boolean valid, long checkedEvents, Long firstInvalidEventId, String message) {}

    @Transactional(readOnly = true)
    public java.util.List<TenantAuditEvent> listRecent(Long tenantId) {
        if (!tenants.existsById(tenantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found.");
        }
        return events.findTop100ByTenantIdOrderByOccurredAtDesc(tenantId);
    }

    @Transactional(readOnly = true)
    public ChainVerification verifyChain(Long tenantId) {
        if (!tenants.existsById(tenantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found.");
        }
        java.util.List<TenantAuditEvent> ordered = events.findAllByTenantIdOrderByIdAsc(tenantId);
        String expectedPrevious = null;
        long checked = 0;
        for (TenantAuditEvent event : ordered) {
            checked++;
            if (!java.util.Objects.equals(expectedPrevious, event.getPreviousEventHash())) {
                return new ChainVerification(false, checked, event.getId(),
                    "Previous-event link mismatch; review this event and subsequent chain.");
            }
            String calculated = hash(event);
            if (event.getEventHash() == null || !MessageDigest.isEqual(
                    calculated.getBytes(StandardCharsets.US_ASCII),
                    event.getEventHash().getBytes(StandardCharsets.US_ASCII))) {
                return new ChainVerification(false, checked, event.getId(),
                    "Event hash mismatch; the stored event may have changed.");
            }
            expectedPrevious = event.getEventHash();
        }
        return new ChainVerification(true, checked, null, "Audit hash chain verified.");
    }

    private String hash(TenantAuditEvent event) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            add(digest, event.getPreviousEventHash());
            add(digest, event.getTenantId() == null ? null : event.getTenantId().toString());
            add(digest, event.getActorUserId() == null ? null : event.getActorUserId().toString());
            add(digest, event.getAction());
            add(digest, event.getResourceType());
            add(digest, event.getResourceId());
            add(digest, event.getOutcome());
            add(digest, event.getReason());
            add(digest, event.getCorrelationId());
            add(digest, event.getSourceIp());
            add(digest, event.getOccurredAt() == null ? null : event.getOccurredAt().toInstant().truncatedTo(ChronoUnit.MICROS).toString());
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is not available in this runtime.", impossible);
        }
    }

    private void add(MessageDigest digest, String value) {
        byte[] bytes = value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required.");
        return value.trim();
    }
}
