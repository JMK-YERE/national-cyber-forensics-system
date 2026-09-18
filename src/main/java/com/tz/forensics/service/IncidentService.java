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
}
