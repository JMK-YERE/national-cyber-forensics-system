package com.tz.forensics.repository;

import com.tz.forensics.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByOauthProviderAndOauthId(String provider, String oauthId);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findByRoleInAndEnabledTrueAndApprovalStatusIgnoreCase(List<String> roles, String approvalStatus);
}
