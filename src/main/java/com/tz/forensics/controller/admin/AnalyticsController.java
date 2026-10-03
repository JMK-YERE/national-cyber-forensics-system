package com.tz.forensics.controller.admin;

import com.tz.forensics.entity.ReportAttack;
import com.tz.forensics.repository.ReportAttackRepository;
import com.tz.forensics.repository.UserRepository;
import com.tz.forensics.repository.WhistleblowerReportRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/analytics")
public class AnalyticsController {
    private final ReportAttackRepository reports;
    private final UserRepository users;
    private final WhistleblowerReportRepository whistleblowers;

    public AnalyticsController(ReportAttackRepository reports, UserRepository users,
                               WhistleblowerReportRepository whistleblowers) {
        this.reports = reports;
        this.users = users;
        this.whistleblowers = whistleblowers;
    }

    @GetMapping
    public String dashboard(Model model) {
        var all = reports.findAll();
        Map<String, Long> status = new LinkedHashMap<>();
        Map<String, Long> category = new LinkedHashMap<>();
        for (ReportAttack r : all) {
            status.merge(r.getStatus() == null ? "UNKNOWN" : r.getStatus(), 1L, Long::sum);
            category.merge(r.getAttackTypeLabel() == null ? "OTHER" : r.getAttackTypeLabel(), 1L, Long::sum);
        }
        model.addAttribute("totalReports", all.size());
        model.addAttribute("totalUsers", users.count());
        model.addAttribute("totalWhistleblowers", whistleblowers.count());
        model.addAttribute("statusLabels", status.keySet());
        model.addAttribute("statusValues", status.values());
        model.addAttribute("categoryLabels", category.keySet());
        model.addAttribute("categoryValues", category.values());
        return "admin/analytics";
    }
}