package com.tz.forensics.service;

import com.tz.forensics.dto.IncidentDto;
import com.tz.forensics.entity.Incident;
import com.tz.forensics.repository.IncidentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;

    public IncidentService(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    public Incident createIncident(IncidentDto dto, String username) {
        Incident incident = new Incident();
        String incidentId = "SEC-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + (100 + new Random().nextInt(900));
        incident.setIncidentId(incidentId);
        incident.setTitle(dto.getTitle());
        incident.setDescription(dto.getDescription());
        incident.setReporter(dto.getReporter());
        incident.setDateReported(LocalDateTime.now());
        incident.setStatus("Under Investigation");
        incident.setSeverity(dto.getSeverity() != null ? dto.getSeverity() : "MEDIUM");
        incident.setCategory(dto.getCategory());
        incident.setRegion(dto.getRegion());
        incident.setOrganization(dto.getOrganization());
        incident.setWorkflowStatus("NEW");

        incident.setDirectLossTzs(dto.getDirectLossTzs());
        incident.setRecoveryCostTzs(dto.getRecoveryCostTzs());
        incident.setDowntimeCostTzs(dto.getDowntimeCostTzs());
        incident.setLegalFeesTzs(dto.getLegalFeesTzs());
        incident.setReputationDamageTzs(dto.getReputationDamageTzs());

        BigDecimal total = BigDecimal.ZERO;
        if (dto.getDirectLossTzs() != null) total = total.add(dto.getDirectLossTzs());
        if (dto.getRecoveryCostTzs() != null) total = total.add(dto.getRecoveryCostTzs());
        if (dto.getDowntimeCostTzs() != null) total = total.add(dto.getDowntimeCostTzs());
        if (dto.getLegalFeesTzs() != null) total = total.add(dto.getLegalFeesTzs());
        if (dto.getReputationDamageTzs() != null) total = total.add(dto.getReputationDamageTzs());
        incident.setTotalLossTzs(total);

        return incidentRepository.save(incident);
    }

    public List<Incident> getAllIncidents() {
        return incidentRepository.findAllByOrderByDateReportedDesc();
    }

    public Incident getById(Long id) {
        return incidentRepository.findById(id).orElse(null);
    }

    public List<Incident> searchByIncidentId(String incidentId) {
        return incidentRepository.findByIncidentIdContainingIgnoreCase(incidentId);
    }

    public List<Incident> getMyIncidents(Long userId) {
        return incidentRepository.findByAssignedToOrderByDateReportedDesc(userId);
    }

    public List<Incident> getMyActiveIncidents(Long userId) {
        return incidentRepository.findByAssignedToAndWorkflowStatusNotOrderByDateReportedDesc(userId, "COMPLETED");
    }

    public void assignIncident(Long incidentId, Long assignedTo, String assignedToName,
                                Long assignedBy, String priority, LocalDateTime dueDate) {
        Incident incident = incidentRepository.findById(incidentId).orElse(null);
        if (incident != null) {
            incident.setAssignedTo(assignedTo);
            incident.setAssignedToName(assignedToName);
            incident.setAssignedBy(assignedBy);
            incident.setAssignedAt(LocalDateTime.now());
            incident.setPriority(priority != null ? priority : "MEDIUM");
            incident.setDueDate(dueDate);
            incident.setWorkflowStatus("ASSIGNED");
            incidentRepository.save(incident);
        }
    }

    public void updateWorkflowStatus(Long incidentId, String status) {
        Incident incident = incidentRepository.findById(incidentId).orElse(null);
        if (incident != null) {
            incident.setWorkflowStatus(status);
            if ("COMPLETED".equals(status)) {
                incident.setIsClosed(true);
                incident.setClosedAt(LocalDateTime.now());
                incident.setStatus("Resolved");
            }
            incidentRepository.save(incident);
        }
    }
}
