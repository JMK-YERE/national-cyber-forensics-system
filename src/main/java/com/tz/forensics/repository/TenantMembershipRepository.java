package com.tz.forensics.repository;

import com.tz.forensics.entity.TenantMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantMembershipRepository extends JpaRepository<TenantMembership, Long> {
    List<TenantMembership> findAllByUserIdAndStatus(Long userId, String status);
    List<TenantMembership> findAllByTenantIdAndStatus(Long tenantId, String status);
    Optional<TenantMembership> findByTenantIdAndUserIdAndStatus(Long tenantId, Long userId, String status);
    boolean existsByTenantIdAndUserIdAndStatus(Long tenantId, Long userId, String status);
}
