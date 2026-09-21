package com.tz.forensics.repository;

import com.tz.forensics.entity.PersonalIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PersonalIncidentRepository extends JpaRepository<PersonalIncident, Long> {
    List<PersonalIncident> findAllByOrderByCreatedAtDesc();
    List<PersonalIncident> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<PersonalIncident> findByStatusOrderByCreatedAtDesc(String status);
    long countByStatus(String status);
}
