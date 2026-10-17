package com.tz.forensics.repository;

import com.tz.forensics.entity.TenantAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TenantAuditEventRepository extends JpaRepository<TenantAuditEvent, Long> {
    List<TenantAuditEvent> findTop100ByTenantIdOrderByOccurredAtDesc(Long tenantId);
    java.util.Optional<TenantAuditEvent> findTopByTenantIdOrderByIdDesc(Long tenantId);
    List<TenantAuditEvent> findAllByTenantIdOrderByIdAsc(Long tenantId);
}
