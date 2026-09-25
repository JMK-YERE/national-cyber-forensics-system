package com.tz.forensics.service;

import com.tz.forensics.entity.ReportAttack;
import com.tz.forensics.repository.ReportAttackRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
public class ReportAttackService {

    private final ReportAttackRepository repo;

    public ReportAttackService(ReportAttackRepository repo) {
        this.repo = repo;
    }

    public ReportAttack create(ReportAttack report) {
        if (report.getReportId() == null) {
            report.setReportId(generateReportId());
        }
        if (report.getCreatedAt() == null) {
            report.setCreatedAt(LocalDateTime.now());
        }
        return repo.save(report);
    }

    public ReportAttack save(ReportAttack report) {
        return repo.save(report);
    }

    public ReportAttack getById(Long id) {
        return repo.findById(id).orElse(null);
    }

    public List<ReportAttack> getAll() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    public List<ReportAttack> getMine(Long userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public long countNew() {
        return repo.countByStatus("NEW");
    }

    public long countToday() {
        return repo.countByCreatedAtAfter(LocalDateTime.now().withHour(0).withMinute(0));
    }

    public long countTotal() {
        return repo.count();
    }

    public void updateStatus(Long id, String status, String adminResponse, Long assignedTo, String assignedName) {
        ReportAttack r = repo.findById(id).orElse(null);
        if (r != null) {
            r.setStatus(status);
            if (adminResponse != null && !adminResponse.isEmpty()) {
                r.setAdminResponse(adminResponse);
            }
            if (assignedTo != null) {
                r.setAssignedTo(assignedTo);
                r.setAssignedToName(assignedName);
            }
            r.setUpdatedAt(LocalDateTime.now());
            repo.save(r);
        }
    }

    private String generateReportId() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int rand = 1000 + new Random().nextInt(9000);
        return "SEC-" + date + "-" + rand;
    }
}
