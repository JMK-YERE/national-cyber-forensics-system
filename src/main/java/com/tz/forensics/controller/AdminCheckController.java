package com.tz.forensics.controller;

import com.tz.forensics.entity.ReportAttack;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.ReportAttackRepository;
import com.tz.forensics.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class AdminCheckController {

    private final UserRepository userRepository;
    private final ReportAttackRepository reportRepo;

    public AdminCheckController(UserRepository userRepository, ReportAttackRepository reportRepo) {
        this.userRepository = userRepository;
        this.reportRepo = reportRepo;
    }

    @GetMapping("/admin/check")
    @ResponseBody
    public Map<String, Object> check(Authentication auth) {
        Map<String, Object> result = new HashMap<>();

        // Current user
        User me = userRepository.findByUsername(auth.getName()).orElse(null);
        if (me != null) {
            result.put("username", me.getUsername());
            result.put("role", me.getRole());
            result.put("isAdmin", me.isAdmin());
            result.put("isProfessional", me.isProfessional());
            result.put("isForensics", me.isForensics());
            result.put("isIndividual", me.isIndividual());
        } else {
            result.put("error", "User haipo");
        }

        // All users count
        List<User> allUsers = userRepository.findAll();
        result.put("totalUsers", allUsers.size());

        // Count admins
        long adminCount = allUsers.stream().filter(User::isAdmin).count();
        result.put("adminCount", adminCount);

        // All reports
        List<ReportAttack> allReports = reportRepo.findAll();
        result.put("totalReports", allReports.size());

        // Recent 5 reports
        result.put("recentReports", allReports.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .map(r -> Map.of(
                    "id", r.getId(),
                    "reportId", r.getReportId(),
                    "title", r.getTitle(),
                    "status", r.getStatus(),
                    "country", r.getCountry() != null ? r.getCountry() : "N/A",
                    "region", r.getRegion() != null ? r.getRegion() : "N/A",
                    "reporter", r.getReporterName() != null ? r.getReporterName() : "N/A",
                    "phone", r.getReporterPhone() != null ? r.getReporterPhone() : "N/A",
                    "createdAt", r.getCreatedAt().toString()
                ))
                .toList());

        return result;
    }
}
