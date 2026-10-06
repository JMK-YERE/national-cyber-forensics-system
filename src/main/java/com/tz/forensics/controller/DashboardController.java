package com.tz.forensics.controller;

import com.tz.forensics.entity.ReportAttack;
import com.tz.forensics.entity.User;
import com.tz.forensics.repository.ReportAttackRepository;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.repository.WhistleblowerReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;

@Controller
public class DashboardController {
    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
    private final UserRepository userRepository;
    private final ReportAttackRepository reportRepo;
    private final WhistleblowerReportRepository wbRepo;
    private final com.tz.forensics.service.CaseTaskService caseTaskService;

    public DashboardController(UserRepository userRepository, ReportAttackRepository reportRepo,
                               WhistleblowerReportRepository wbRepo,
                               com.tz.forensics.service.CaseTaskService caseTaskService) {
        this.userRepository = userRepository;
        this.reportRepo = reportRepo;
        this.wbRepo = wbRepo;
        this.caseTaskService = caseTaskService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        if (auth == null || auth.getName() == null) return "redirect:/login";
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        model.addAttribute("user", user);
        model.addAttribute("username", user.getUsername());
        model.addAttribute("role", user.getRole());

        String role = user.getRole() != null ? user.getRole() : "INDIVIDUAL";
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        boolean isPro = "CYBER_PRO".equalsIgnoreCase(role);
        boolean isForensics = "FORENSICS".equalsIgnoreCase(role);
        boolean isAnalyst = "ANALYST".equalsIgnoreCase(role);
        boolean isStaff = isAdmin || isPro || isForensics || isAnalyst;

        try {
            if (isStaff) {
                long totalReports = reportRepo.count();
                long newReports = reportRepo.countByStatus("NEW");
                model.addAttribute("totalReports", totalReports);
                model.addAttribute("newReports", newReports);
                model.addAttribute("totalWb", wbRepo.count());
                model.addAttribute("totalUsers", userRepository.count());
                List<com.tz.forensics.entity.CaseTask> myTasks = caseTaskService.findMyTasks(user.getId());
                model.addAttribute("myTaskCount", myTasks.size());
                model.addAttribute("myOverdueTaskCount", myTasks.stream().filter(com.tz.forensics.entity.CaseTask::isOverdue).count());
                model.addAttribute("myOpenTaskCount", myTasks.stream().filter(t -> t.getStatus() != null && !Set.of("COMPLETED","CANCELLED").contains(t.getStatus().toUpperCase())).count());

                model.addAttribute("newCount", newReports);
                model.addAttribute("triagedCount", reportRepo.countByStatus("TRIAGED"));
                model.addAttribute("assignedCount", reportRepo.countByStatus("ASSIGNED"));
                model.addAttribute("investigatingCount", reportRepo.countByStatus("INVESTIGATING"));
                model.addAttribute("containmentCount", reportRepo.countByStatus("CONTAINMENT"));
                model.addAttribute("eradicationCount", reportRepo.countByStatus("ERADICATION"));
                model.addAttribute("recoveryCount", reportRepo.countByStatus("RECOVERY"));
                model.addAttribute("closedCount", reportRepo.countByStatus("CLOSED"));

                List<ReportAttack> allReports = reportRepo.findAllByOrderByCreatedAtDesc();
                List<Integer> dailyCounts = new ArrayList<>();
                List<String> dailyLabels = new ArrayList<>();
                LocalDate today = LocalDate.now();
                for (int i = 6; i >= 0; i--) {
                    LocalDate day = today.minusDays(i);
                    LocalDateTime start = day.atStartOfDay();
                    LocalDateTime end = day.plusDays(1).atStartOfDay();
                    long count = allReports.stream().filter(r -> r.getCreatedAt() != null
                            && !r.getCreatedAt().isBefore(start)
                            && r.getCreatedAt().isBefore(end)).count();
                    dailyCounts.add((int) count);
                    dailyLabels.add(day.getDayOfWeek().toString().substring(0, 3));
                }
                model.addAttribute("dailyCounts", dailyCounts);
                model.addAttribute("dailyLabels", dailyLabels);

                List<ReportAttack> recent = allReports;
                if (recent.size() > 5) recent = recent.subList(0, 5);
                model.addAttribute("recentReports", recent);

                // Role-aware geographic intelligence: one map point per report, without reporter identity.
                // Reports without a region are retained as "Unknown location" instead of disappearing.
                List<Map<String,Object>> mapPoints = new ArrayList<>();
                for (ReportAttack r : allReports) {
                    Map<String,Object> point = new LinkedHashMap<>();
                    String region = r.getRegion() == null ? "" : r.getRegion().trim();
                    point.put("region", region.isBlank() ? "Unknown location" : region);
                    point.put("priority", r.getPriority() == null ? "MEDIUM" : r.getPriority());
                    point.put("type", r.getAttackTypeLabel() == null ? "Other" : r.getAttackTypeLabel());
                    point.put("status", r.getStatus() == null ? "NEW" : r.getStatus());
                    point.put("reportId", r.getReportId() == null ? "" : r.getReportId());
                    mapPoints.add(point);
                }
                model.addAttribute("mapPoints", mapPoints);
                model.addAttribute("analyticsTotal", allReports.size());
                model.addAttribute("analyticsHigh", allReports.stream().filter(r -> "HIGH".equalsIgnoreCase(r.getPriority()) || "CRITICAL".equalsIgnoreCase(r.getPriority())).count());
                model.addAttribute("analyticsOpen", allReports.stream().filter(r -> r.getStatus() == null || !"CLOSED".equalsIgnoreCase(r.getStatus())).count());
                model.addAttribute("analyticsClosed", allReports.stream().filter(r -> "CLOSED".equalsIgnoreCase(r.getStatus())).count());
                Map<String,Integer> regionCounts = new LinkedHashMap<>();
                Map<String,Integer> typeCounts = new LinkedHashMap<>();
                allReports.forEach(r -> {
                    if (r.getRegion() != null && !r.getRegion().isBlank()) regionCounts.merge(r.getRegion().trim(), 1, Integer::sum);
                    String type = r.getAttackTypeLabel() == null ? "Other" : r.getAttackTypeLabel();
                    typeCounts.merge(type, 1, Integer::sum);
                });
                model.addAttribute("regionLabels", regionCounts.keySet());
                model.addAttribute("regionValues", regionCounts.values());
                model.addAttribute("typeLabels", typeCounts.keySet());
                model.addAttribute("typeValues", typeCounts.values());
            } else {
                List<ReportAttack> my = reportRepo.findByUserIdOrderByCreatedAtDesc(user.getId());
                model.addAttribute("myReports", my);
                model.addAttribute("personalReportCount", my.size());

                // Personal map scope: only this user's reported incidents are exposed.
                List<Map<String,Object>> mapPoints = new ArrayList<>();
                for (ReportAttack r : my) {
                    Map<String,Object> point = new LinkedHashMap<>();
                    String region = r.getRegion() == null ? "" : r.getRegion().trim();
                    point.put("region", region.isBlank() ? "Unknown location" : region);
                    point.put("priority", r.getPriority() == null ? "MEDIUM" : r.getPriority());
                    point.put("type", r.getAttackTypeLabel() == null ? "Other" : r.getAttackTypeLabel());
                    point.put("status", r.getStatus() == null ? "NEW" : r.getStatus());
                    point.put("reportId", r.getReportId() == null ? "" : r.getReportId());
                    mapPoints.add(point);
                }
                model.addAttribute("mapPoints", mapPoints);
            }
        } catch (Exception e) {
            log.error("Dashboard error: {}", e.getMessage(), e);
            model.addAttribute("recentReports", List.of());
            model.addAttribute("myReports", List.of());
            model.addAttribute("dailyCounts", List.of(0,0,0,0,0,0,0));
            model.addAttribute("dailyLabels", List.of("","","","","","",""));
        }

        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isCyberPro", isPro);
        model.addAttribute("isForensics", isForensics);
        model.addAttribute("isAnalyst", isAnalyst);
        model.addAttribute("isStaff", isStaff);
        if (isAdmin) return "dashboard-admin";
         if (isPro) return "dashboard-professional";
         if (isForensics) return "dashboard-forensics";
         if (isAnalyst) return "dashboard-analyst";
         return "dashboard-individual";
    }

    @GetMapping("/access-denied")
    public String accessDenied() { return "access-denied"; }
}
