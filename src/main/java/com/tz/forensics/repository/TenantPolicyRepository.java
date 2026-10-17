package com.tz.forensics.repository;

import com.tz.forensics.entity.TenantPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantPolicyRepository extends JpaRepository<TenantPolicy, Long> {
    Optional<TenantPolicy> findByTenantIdAndPolicyType(Long tenantId, String policyType);
    java.util.List<TenantPolicy> findAllByTenantIdOrderByPolicyTypeAsc(Long tenantId);
}
