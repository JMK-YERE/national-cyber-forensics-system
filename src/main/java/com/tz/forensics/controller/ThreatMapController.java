package com.tz.forensics.controller;

import com.tz.forensics.repository.ReportAttackRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.*;

@Controller
public class ThreatMapController {
    private final ReportAttackRepository reportRepository;
    public ThreatMapController(ReportAttackRepository reportRepository){this.reportRepository=reportRepository;}

    @GetMapping("/threat-map")
    public String threatMap(Model model) {
        List<Map<String,Object>> points = new ArrayList<>();
        reportRepository.findAll().forEach(r -> {
            if (r.getRegion() == null || r.getRegion().isBlank()) return;
            Map<String,Object> p = new LinkedHashMap<>();
            p.put("region", r.getRegion());
            p.put("priority", r.getPriority() == null ? "MEDIUM" : r.getPriority());
            p.put("type", r.getAttackTypeLabel());
            points.add(p);
        });
        model.addAttribute("mapPoints", points);
        return "threat-map";
    }
}
