package com.tz.forensics.controller;

import com.tz.forensics.entity.Incident;
import com.tz.forensics.entity.ReportAttack;
import com.tz.forensics.repository.IncidentRepository;
import com.tz.forensics.repository.ReportAttackRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class ThreatMapController {

    private final IncidentRepository incidentRepo;
    private final ReportAttackRepository attackRepo;

    private static final Map<String, double[]> REGION_COORDS = new LinkedHashMap<>();
    static {
        REGION_COORDS.put("Dar es Salaam", new double[]{-6.7924, 39.2083});
        REGION_COORDS.put("Arusha", new double[]{-3.3869, 36.6830});
        REGION_COORDS.put("Mwanza", new double[]{-2.5164, 32.9175});
        REGION_COORDS.put("Dodoma", new double[]{-6.1630, 35.7516});
        REGION_COORDS.put("Mbeya", new double[]{-8.9094, 33.4608});
        REGION_COORDS.put("Morogoro", new double[]{-6.8278, 37.6591});
        REGION_COORDS.put("Tanga", new double[]{-5.0689, 39.0988});
        REGION_COORDS.put("Zanzibar", new double[]{-6.1659, 39.2026});
        REGION_COORDS.put("Kilimanjaro", new double[]{-3.3471, 37.3422});
        REGION_COORDS.put("Tabora", new double[]{-5.0167, 32.8000});
        REGION_COORDS.put("Kigoma", new double[]{-4.8769, 29.6269});
        REGION_COORDS.put("Iringa", new double[]{-7.7700, 35.6900});
        REGION_COORDS.put("Mtwara", new double[]{-10.2667, 40.1833});
        REGION_COORDS.put("Ruvuma", new double[]{-10.6833, 35.6500});
        REGION_COORDS.put("Singida", new double[]{-4.8167, 34.7500});
        REGION_COORDS.put("Shinyanga", new double[]{-3.6619, 33.4212});
        REGION_COORDS.put("Kagera", new double[]{-1.8825, 31.3900});
        REGION_COORDS.put("Mara", new double[]{-1.7750, 34.1500});
        REGION_COORDS.put("Manyara", new double[]{-4.3167, 36.0667});
        REGION_COORDS.put("Rukwa", new double[]{-8.0000, 31.5000});
        REGION_COORDS.put("Katavi", new double[]{-6.3667, 31.0500});
        REGION_COORDS.put("Njombe", new double[]{-9.3333, 34.7667});
        REGION_COORDS.put("Simiyu", new double[]{-2.8333, 33.9833});
        REGION_COORDS.put("Geita", new double[]{-2.8667, 32.1667});
        REGION_COORDS.put("Songwe", new double[]{-8.8514, 32.8925});
        REGION_COORDS.put("Pwani", new double[]{-6.9333, 38.9167});
        REGION_COORDS.put("Lindi", new double[]{-10.0000, 39.7167});
    }

    public ThreatMapController(IncidentRepository incidentRepo,
                                 ReportAttackRepository attackRepo) {
        this.incidentRepo = incidentRepo;
        this.attackRepo = attackRepo;
    }

    @GetMapping("/threat-map")
    public String threatMap(Model model) {
        List<Incident> incidents = incidentRepo.findAll();
        List<ReportAttack> attacks = attackRepo.findAll();

        Map<String, Integer> regionCounts = new LinkedHashMap<>();
        Map<String, Integer> regionCritical = new LinkedHashMap<>();
        Map<String, Integer> regionResolved = new LinkedHashMap<>();

        for (String region : REGION_COORDS.keySet()) {
            regionCounts.put(region, 0);
            regionCritical.put(region, 0);
            regionResolved.put(region, 0);
        }

        // Incidents
        for (Incident inc : incidents) {
            String region = inc.getRegion();
            if (region != null && regionCounts.containsKey(region)) {
                regionCounts.put(region, regionCounts.get(region) + 1);
                if ("CRITICAL".equalsIgnoreCase(inc.getSeverity())) {
                    regionCritical.put(region, regionCritical.get(region) + 1);
                }
                if ("Resolved".equalsIgnoreCase(inc.getStatus())) {
                    regionResolved.put(region, regionResolved.get(region) + 1);
                }
            }
        }

        // Report Attacks
        for (ReportAttack ra : attacks) {
            String region = ra.getRegion();
            if (region != null && regionCounts.containsKey(region)) {
                regionCounts.put(region, regionCounts.get(region) + 1);
                if ("CRITICAL".equalsIgnoreCase(ra.getPriority()) || "HIGH".equalsIgnoreCase(ra.getPriority())) {
                    regionCritical.put(region, regionCritical.get(region) + 1);
                }
                if ("RESOLVED".equalsIgnoreCase(ra.getStatus()) || "CLOSED".equalsIgnoreCase(ra.getStatus())) {
                    regionResolved.put(region, regionResolved.get(region) + 1);
                }
            }
        }

        // Top 5
        List<Map.Entry<String, Integer>> top5 = regionCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toList());

        // Recent 7 days
        LocalDateTime weekAgo = LocalDateTime.now().minus(7, ChronoUnit.DAYS);
        long recentIncidents = incidents.stream()
                .filter(i -> i.getDateReported() != null && i.getDateReported().isAfter(weekAgo))
                .count();
        long recentAttacks = attacks.stream()
                .filter(a -> a.getCreatedAt() != null && a.getCreatedAt().isAfter(weekAgo))
                .count();

        model.addAttribute("totalIncidents", incidents.size() + attacks.size());
        model.addAttribute("recentIncidents", recentIncidents + recentAttacks);
        model.addAttribute("totalCritical", regionCritical.values().stream().mapToInt(Integer::intValue).sum());
        model.addAttribute("totalResolved", regionResolved.values().stream().mapToInt(Integer::intValue).sum());
        model.addAttribute("regionCounts", regionCounts);
        model.addAttribute("top5", top5);
        model.addAttribute("regionCoords", REGION_COORDS);

        // Recent attacks for map (last 24h)
        List<ReportAttack> recent24h = attacks.stream()
                .filter(a -> a.getCreatedAt() != null && a.getCreatedAt().isAfter(LocalDateTime.now().minusHours(24)))
                .collect(Collectors.toList());
        model.addAttribute("recent24hAttacks", recent24h);

        return "threat-map";
    }
}
