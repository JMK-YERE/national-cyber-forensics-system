package com.tz.forensics.repository;

import com.tz.forensics.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findAllByOrderByDateReportedDesc();
    List<Incident> findByIncidentIdContainingIgnoreCase(String incidentId);
    List<Incident> findByAssignedToOrderByDateReportedDesc(Long assignedTo);
    List<Incident> findByWorkflowStatusOrderByDateReportedDesc(String workflowStatus);
    long countByAssignedTo(Long assignedTo);
    long countByWorkflowStatus(String workflowStatus);
    List<Incident> findByAssignedToAndWorkflowStatusNotOrderByDateReportedDesc(Long assignedTo, String workflowStatus);
}
