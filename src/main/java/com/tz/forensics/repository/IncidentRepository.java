package com.tz.forensics.repository;

import com.tz.forensics.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findAllByOrderByDateReportedDesc();
    List<Incident> findByIncidentIdContainingIgnoreCase(String incidentId);
}
